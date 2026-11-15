package com.sportify.auth;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {

    @Test
    public void testPasswordHashingAndVerification() {
        String rawPassword = "StrongPassword123";
        String hash = PasswordHasher.hashPassword(rawPassword);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
        assertTrue(PasswordHasher.verifyPassword(rawPassword, hash));
        assertFalse(PasswordHasher.verifyPassword("WrongPassword123", hash));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHashEmptyPasswordThrowsException() {
        PasswordHasher.hashPassword("   ");
    }

    @Test
    public void testNullPasswordVerification() {
        assertFalse(PasswordHasher.verifyPassword(null, "$2a$12$somehash"));
        assertFalse(PasswordHasher.verifyPassword("pass", null));
    }
}
