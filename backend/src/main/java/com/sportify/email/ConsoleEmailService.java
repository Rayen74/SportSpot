package com.sportify.email;

import java.util.logging.Logger;

public class ConsoleEmailService implements EmailService {
    private static final Logger logger = Logger.getLogger(ConsoleEmailService.class.getName());

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetToken) {
        logger.info("==================================================================");
        logger.info("EMAIL SERVICE (Console Mode - No SMTP Host Configured)");
        logger.info("To: " + recipientEmail);
        logger.info("Subject: Sportify Password Reset Instructions");
        logger.info("Reset Token: " + resetToken);
        logger.info("Reset Password API Endpoint: POST /auth/reset-password");
        logger.info("Payload Example: {\"token\": \"" + resetToken + "\", \"newPassword\": \"NewPassword123\"}");
        logger.info("==================================================================");
    }
}
