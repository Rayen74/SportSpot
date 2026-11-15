package com.sportify.email;

public interface EmailService {
    void sendPasswordResetEmail(String recipientEmail, String resetToken);
}
