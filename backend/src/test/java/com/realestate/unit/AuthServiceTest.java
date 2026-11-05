package com.realestate.unit;

import static com.realestate.support.TestData.AGENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realestate.auth.AuthService;
import com.realestate.auth.RefreshToken;
import com.realestate.auth.RefreshTokenRepository;
import com.realestate.auth.dto.LoginRequest;
import com.realestate.common.exception.AppException;
import com.realestate.common.exception.ErrorCode;
import com.realestate.security.JwtProperties;
import com.realestate.security.JwtService;
import com.realestate.security.UserPrincipal;
import com.realestate.support.Mappers;
import com.realestate.support.TestData;
import com.realestate.user.Role;
import com.realestate.user.User;
import com.realestate.user.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T08:00:00Z");
    private static final JwtProperties PROPS = new JwtProperties(
            "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNDU2Nzg=", "test",
            Duration.ofMinutes(15), Duration.ofDays(7));

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserService userService;

    private AuthService service;
    private JwtService jwtService;
    private final User user = TestData.user(AGENT_ID, Role.AGENT);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        jwtService = new JwtService(PROPS, clock);
        service = new AuthService(authenticationManager, jwtService, PROPS, refreshTokenRepository, userService,
                Mappers.USER, clock);
    }

    @Test
    void loginIssuesAccessAndHashedRefreshToken() {
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(UserPrincipal.from(user), null));
        when(userService.findUser(AGENT_ID)).thenReturn(user);

        var response = service.login(new LoginRequest("user2@test.com", "secret"));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken stored = captor.getValue();
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(response.refreshToken());
        assertThat(stored.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(jwtService.parseAccessToken(response.accessToken()))
                .hasValueSatisfying(p -> {
                    assertThat(p.getId()).isEqualTo(AGENT_ID);
                    assertThat(p.getRole()).isEqualTo(Role.AGENT);
                });
    }

    @Test
    void loginPropagatesBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.login(new LoginRequest("x@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshRotatesToken() {
        RefreshToken stored = RefreshToken.builder().user(user).tokenHash("h")
                .createdAt(NOW.minusSeconds(60)).expiresAt(NOW.plusSeconds(3600)).build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        var response = service.refresh("raw-token");

        assertThat(stored.getRevokedAt()).isEqualTo(NOW);
        assertThat(response.refreshToken()).isNotEqualTo("raw-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void reusingRevokedTokenRevokesAllSessions() {
        RefreshToken stored = RefreshToken.builder().user(user).tokenHash("h")
                .createdAt(NOW.minusSeconds(60)).expiresAt(NOW.plusSeconds(3600))
                .revokedAt(NOW.minusSeconds(10)).build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh("raw-token"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
        verify(refreshTokenRepository).revokeAllForUser(AGENT_ID, NOW);
    }

    @Test
    void expiredRefreshTokenIsRejected() {
        RefreshToken stored = RefreshToken.builder().user(user).tokenHash("h")
                .createdAt(NOW.minusSeconds(7200)).expiresAt(NOW).build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh("raw-token")).isInstanceOf(AppException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void disabledUserCannotRefresh() {
        user.setEnabled(false);
        RefreshToken stored = RefreshToken.builder().user(user).tokenHash("h")
                .createdAt(NOW).expiresAt(NOW.plusSeconds(3600)).build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.refresh("raw-token"))
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    void unknownRefreshTokenIsRejected() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("nope")).isInstanceOf(AppException.class);
    }

    @Test
    void logoutRevokesToken() {
        RefreshToken stored = RefreshToken.builder().user(user).tokenHash("h")
                .createdAt(NOW).expiresAt(NOW.plusSeconds(3600)).build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        service.logout("raw");

        assertThat(stored.isRevoked()).isTrue();
    }
}
