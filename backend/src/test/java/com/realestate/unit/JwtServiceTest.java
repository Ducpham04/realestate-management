package com.realestate.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.realestate.security.JwtProperties;
import com.realestate.security.JwtService;
import com.realestate.security.UserPrincipal;
import com.realestate.support.TestData;
import com.realestate.user.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNDU2Nzg=";
    private static final Instant NOW = Instant.parse("2026-06-15T08:00:00Z");

    private JwtService service(String secret, String issuer, Instant now) {
        return new JwtService(new JwtProperties(secret, issuer, Duration.ofMinutes(15), Duration.ofDays(7)),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void roundTripsPrincipalClaims() {
        JwtService jwt = service(SECRET, "test", NOW);
        UserPrincipal principal = UserPrincipal.from(TestData.user(7L, Role.ADMIN));

        var parsed = jwt.parseAccessToken(jwt.generateAccessToken(principal)).orElseThrow();

        assertThat(parsed.getId()).isEqualTo(7L);
        assertThat(parsed.getEmail()).isEqualTo("user7@test.com");
        assertThat(parsed.getFullName()).isEqualTo("User 7");
        assertThat(parsed.isAdmin()).isTrue();
        assertThat(parsed.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void rejectsExpiredToken() {
        String token = service(SECRET, "test", NOW).generateAccessToken(TestData.agent());

        assertThat(service(SECRET, "test", NOW.plus(Duration.ofMinutes(16))).parseAccessToken(token)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithDifferentKeyOrIssuer() {
        String token = service(SECRET, "test", NOW).generateAccessToken(TestData.agent());
        String otherSecret = "b3RoZXItc2VjcmV0LW90aGVyLXNlY3JldC1vdGhlci1zZWNyZXQtMTIzNDU=";

        assertThat(service(otherSecret, "test", NOW).parseAccessToken(token)).isEmpty();
        assertThat(service(SECRET, "other", NOW).parseAccessToken(token)).isEmpty();
        assertThat(service(SECRET, "test", NOW).parseAccessToken("garbage")).isEmpty();
    }

    @Test
    void refusesWeakSecret() {
        assertThatThrownBy(() -> service("c2hvcnQ=", "test", NOW)).isInstanceOf(IllegalStateException.class);
    }
}
