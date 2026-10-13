package com.lms.system.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtTokenServiceTest {

    @Test
    void issuedTokenContainsUserIdentityRolesAndExpiration() {
        SecretKey key = new SecretKeySpec(
                "test-only-signing-key-with-at-least-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        JwtConfig jwtConfig = new JwtConfig();
        JwtTokenService service = new JwtTokenService(jwtConfig.jwtEncoder(key), 900);
        JwtDecoder decoder = jwtConfig.jwtDecoder(key);
        AuthenticatedUser user = new AuthenticatedUser(
                42L,
                "student@example.com",
                "API Student",
                "unused-hash",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        JwtTokenService.IssuedToken issuedToken = service.issue(user);
        var decodedToken = decoder.decode(issuedToken.accessToken());

        assertEquals(900, issuedToken.expiresIn());
        assertEquals("student@example.com", decodedToken.getSubject());
        assertEquals(42L, ((Number) decodedToken.getClaim("userId")).longValue());
        assertEquals(List.of("ROLE_USER", "ROLE_STUDENT"), decodedToken.getClaimAsStringList("roles"));
        assertNotNull(decodedToken.getIssuedAt());
        assertEquals(Duration.ofSeconds(900), Duration.between(decodedToken.getIssuedAt(), decodedToken.getExpiresAt()));
    }
}
