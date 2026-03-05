package com.hope.master_service.service;

public interface EmailService {

    void sendInvitationEmail(String toEmail, String firstName, String role, String activationLink);

    void sendPasswordResetEmail(String toEmail, String firstName, String resetLink);

    void sendOtpEmail(String toEmail, String firstName, String otp);
}
