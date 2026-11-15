package com.sportify.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.sportify.config.AppConfig;
import com.sportify.users.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JwtServiceTest {

    private JwtService jwtService;

    @Before
    public void setUp() {
        System.setProperty("JWT_SECRET", "test_secret_key_1234567890_super_secret");
        System.setProperty("JWT_EXPIRATION_MINUTES", "60");
        AppConfig appConfig = new AppConfig();
        jwtService = new JwtService(appConfig);
    }

    @Test
    public void testGenerateAndVerifyToken() {
        User user = new User();
        user.setId(101L);
        user.setEmail("john@example.com");
        user.setRole("USER");

        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertTrue(token.length() > 20);

        Optional<DecodedJWT> jwtOpt = jwtService.verifyToken(token);
        assertTrue(jwtOpt.isPresent());

        DecodedJWT jwt = jwtOpt.get();
        assertEquals(Long.valueOf(101L), jwtService.extractUserId(jwt));
        assertEquals("john@example.com", jwt.getClaim("email").asString());
        assertEquals("USER", jwt.getClaim("role").asString());
    }

    @Test
    public void testVerifyInvalidToken() {
        Optional<DecodedJWT> jwtOpt = jwtService.verifyToken("invalid.jwt.token");
        assertFalse(jwtOpt.isPresent());
    }

    @Test
    public void testVerifyNullOrEmptyToken() {
        assertFalse(jwtService.verifyToken(null).isPresent());
        assertFalse(jwtService.verifyToken("  ").isPresent());
    }
}
