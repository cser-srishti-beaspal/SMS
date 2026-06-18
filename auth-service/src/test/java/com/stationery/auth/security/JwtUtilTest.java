package com.stationery.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "mySuperSecretKeyForJwtSigningMySuperSecretKeyForJwtSigning";
    private final Long expiration = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", expiration);
    }

    @Test
    void generateAndValidateToken_Success() {
        String username = "testuser";
        String role = "STUDENT";

        String token = jwtUtil.generateToken(username, role);
        assertNotNull(token);

        assertTrue(jwtUtil.validateToken(token));
        assertEquals(username, jwtUtil.extractUsername(token));
        assertEquals(role, jwtUtil.extractRole(token));
    }

    @Test
    void validateToken_Malformed() {
        String invalidToken = "not.a.valid.jwt.token";
        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    void validateToken_Expired() {
        // Set short expiration
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // expired 1 sec ago
        String token = jwtUtil.generateToken("expiredUser", "ADMIN");

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_EmptyOrNull() {
        assertFalse(jwtUtil.validateToken(""));
        assertFalse(jwtUtil.validateToken(null));
    }
}
