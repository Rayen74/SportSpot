package com.sportify.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordHasher {

    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        return BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray());
    }

    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), hashedPassword);
        return result.verified;
    }
}
