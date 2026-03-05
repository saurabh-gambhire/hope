package com.hope.master_service.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SendGridEmailService implements EmailService {

    private final SendGrid sendGrid;

    @Value("${sendgrid.from-email}")
    private String fromEmail;

    @Value("${sendgrid.template.invitation}")
    private String invitationTemplate;

    @Value("${sendgrid.template.otp:#{null}}")
    private String otpTemplate;

    @Override
    @Async
    public void sendInvitationEmail(String toEmail, String firstName, String role, String activationLink) {
        Personalization personalization = new Personalization();
        personalization.addDynamicTemplateData("FIRST_NAME", firstName);
        personalization.addDynamicTemplateData("ROLE", role);
        personalization.addDynamicTemplateData("ACTIVATION_LINK", activationLink);
        personalization.addDynamicTemplateData("EXPIRY_HOURS", "72");
        personalization.setSubject("You've been invited to Hope EHR");

        try {
            send(toEmail, invitationTemplate, personalization);
            log.info("Invitation email sent to {}", toEmail);
        } catch (IOException e) {
            log.error("Failed to send invitation email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetLink) {
        Personalization personalization = new Personalization();
        personalization.addDynamicTemplateData("FIRST_NAME", firstName);
        personalization.addDynamicTemplateData("RESET_LINK", resetLink);
        personalization.setSubject("Hope EHR - Password Reset Request");

        try {
            send(toEmail, invitationTemplate, personalization);
            log.info("Password reset email sent to {}", toEmail);
        } catch (IOException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendOtpEmail(String toEmail, String firstName, String otp) {
        Personalization personalization = new Personalization();
        personalization.addDynamicTemplateData("FIRST_NAME", firstName);
        personalization.addDynamicTemplateData("OTP", otp);
        personalization.addDynamicTemplateData("EXPIRY_MINUTES", "15");
        personalization.setSubject("Hope EHR - Password Reset Verification Code");

        String templateId = otpTemplate != null ? otpTemplate : invitationTemplate;

        try {
            send(toEmail, templateId, personalization);
            log.info("OTP email sent to {}", toEmail);
        } catch (IOException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    private void send(String toEmail, String templateId, Personalization personalization) throws IOException {
        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        personalization.addTo(to);

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setTemplateId(templateId);
        mail.addPersonalization(personalization);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGrid.api(request);

        if (response.getStatusCode() >= 400) {
            log.error("SendGrid error: status={}, body={}", response.getStatusCode(), response.getBody());
            throw new IOException("SendGrid API error: " + response.getStatusCode());
        }

        log.debug("SendGrid response: status={}", response.getStatusCode());
    }
}
