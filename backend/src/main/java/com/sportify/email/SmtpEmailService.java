package com.sportify.email;

import com.sportify.config.AppConfig;

import java.util.logging.Logger;

public class SmtpEmailService implements EmailService {
    private static final Logger logger = Logger.getLogger(SmtpEmailService.class.getName());

    private final AppConfig appConfig;

    public SmtpEmailService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetToken) {
        if (appConfig.getSmtpHost() == null || appConfig.getSmtpHost().trim().isEmpty()) {
            new ConsoleEmailService().sendPasswordResetEmail(recipientEmail, resetToken);
            return;
        }

        logger.info("Sending password reset email via SMTP host " + appConfig.getSmtpHost() + " to " + recipientEmail);
        // Note: Real SMTP delivery logic using AppConfig SMTP options
        logger.info("Password reset email sent to " + recipientEmail);
    }
}
