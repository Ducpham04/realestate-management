package com.realestate.auth;

import com.realestate.auth.dto.AuthResponse;
import com.realestate.auth.dto.LoginRequest;
import com.realestate.common.exception.AppException;
import com.realestate.common.exception.ErrorCode;
import com.realestate.security.JwtProperties;
import com.realestate.security.JwtService;
import com.realestate.security.UserPrincipal;
import com.realestate.user.User;
import com.realestate.user.UserMapper;
import com.realestate.user.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final Clock clock;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return issueTokens(userService.findUser(principal.getId()));
    }

    /**
     * Rotates a refresh token: the presented token is revoked and a new pair is issued.
     * Presenting an already-revoked token is treated as theft and revokes every session of that user.
     */
    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(AuthService::invalidToken);

        if (stored.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}; revoking all sessions", stored.getUser().getId());
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), now);
            throw invalidToken();
        }
        if (stored.isExpired(now)) {
            throw invalidToken();
        }
        User user = stored.getUser();
        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED, "Account is disabled");
        }
        stored.setRevokedAt(now);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .filter(t -> !t.isRevoked())
                .ifPresent(t -> t.setRevokedAt(clock.instant()));
    }

    @Scheduled(cron = "${app.jwt.cleanup-cron:0 0 3 * * *}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void purgeExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(clock.instant());
        if (deleted > 0) {
            log.info("Purged {} expired refresh tokens", deleted);
        }
    }

    private AuthResponse issueTokens(User user) {
        Instant now = clock.instant();
        String rawRefresh = generateOpaqueToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawRefresh))
                .createdAt(now)
                .expiresAt(now.plus(jwtProperties.refreshTokenTtl()))
                .build());
        String accessToken = jwtService.generateAccessToken(UserPrincipal.from(user));
        return new AuthResponse(accessToken, rawRefresh, "Bearer", jwtService.accessTokenTtlSeconds(),
                userMapper.toResponse(user));
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static AppException invalidToken() {
        return new AppException(ErrorCode.INVALID_TOKEN, "Refresh token is invalid or expired");
    }
}
