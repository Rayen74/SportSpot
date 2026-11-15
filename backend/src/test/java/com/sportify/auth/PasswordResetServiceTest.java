package com.sportify.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class PasswordResetServiceTest {

    @Test
    public void testTokenGenerationAndHashing() {
        String token1 = PasswordResetService.generateSecureToken();
        String token2 = PasswordResetService.generateSecureToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertEquals(64, token1.length()); // 32 bytes hex = 64 hex chars
        assertNotEquals(token1, token2);

        String hash1 = PasswordResetService.hashToken(token1);
        String hash1Again = PasswordResetService.hashToken(token1);
        String hash2 = PasswordResetService.hashToken(token2);

        assertNotNull(hash1);
        assertEquals(64, hash1.length()); // SHA-256 = 64 hex chars
        assertEquals(hash1, hash1Again);
        assertNotEquals(hash1, hash2);
    }
}
