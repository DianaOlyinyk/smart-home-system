package server.auth.internal;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-test-secret-key-12345";

    private final JwtValidatorService validator = new JwtValidatorService(SECRET);

    private final UserDetails owner = org.springframework.security.core.userdetails.User
            .withUsername("owner@example.com")
            .password("x")
            .authorities(List.of())
            .build();

    private final UserDetails other = org.springframework.security.core.userdetails.User
            .withUsername("guest@example.com")
            .password("x")
            .authorities(List.of())
            .build();

    @Test
    void validTokenGivesBackUsername() {
        String token = new JwtService(SECRET, 15).generateToken(owner);

        assertEquals(3, token.split("\\.").length);
        assertEquals("owner@example.com", validator.extractUsername(token));
        assertTrue(validator.isTokenValid(token, owner));
    }

    @Test
    void tokenOfAnotherUserIsNotValidForThisUser() {
        String token = new JwtService(SECRET, 15).generateToken(owner);

        assertFalse(validator.isTokenValid(token, other));
    }

    @Test
    void tamperedPayloadIsRejected() {
        String[] parts = new JwtService(SECRET, 15).generateToken(owner).split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"hacker@example.com\"}".getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThrows(JwtException.class, () -> validator.extractUsername(forged));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String foreign = new JwtService("another-secret-key-another-secret-key-99", 15).generateToken(owner);

        assertThrows(JwtException.class, () -> validator.extractUsername(foreign));
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = new JwtService(SECRET, -1).generateToken(owner);

        assertThrows(ExpiredJwtException.class, () -> validator.extractUsername(expired));
    }

    @Test
    void secretShorterThan32BytesIsRejected() {
        assertThrows(WeakKeyException.class, () -> new JwtService("short-secret", 15));
    }
}
