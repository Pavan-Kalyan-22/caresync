package com.caresync.service.impl;

import com.caresync.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.from}")
    private String fromEmail;

    @Value("${spring.application.name}")
    private String appName;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String email, String otp) {
        try {
            String subject = "CareSync - Email Verification OTP";
            String htmlContent = buildOtpEmailTemplate(otp);
            sendHtmlEmail(email, subject, htmlContent);
            log.info("OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    @Override
    public void sendVerificationEmail(String email, String userName) {
        try {
            String subject = "Welcome to " + appName + " - Verify Your Email";
            String htmlContent = buildVerificationEmailTemplate(userName);
            sendHtmlEmail(email, subject, htmlContent);
            log.info("Verification email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String email, String otp) {
        try {
            String subject = "CareSync - Password Reset Request";
            String htmlContent = buildPasswordResetEmailTemplate(otp);
            sendHtmlEmail(email, subject, htmlContent);
            log.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Override
    public void sendPasswordResetSuccessEmail(String email) {
        try {
            String subject = "CareSync - Your Password Has Been Successfully Reset";
            String htmlContent = buildPasswordResetSuccessEmailTemplate();
            sendHtmlEmail(email, subject, htmlContent);
            log.info("Password reset success email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset success email to: {}", email, e);
            throw new RuntimeException("Failed to send password reset success email", e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    private String buildOtpEmailTemplate(String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 5px 5px 0 0; }" +
                ".content { padding: 20px; background: #f9f9f9; }" +
                ".otp-box { background: white; border: 2px solid #667eea; padding: 15px; border-radius: 5px; text-align: center; margin: 20px 0; }" +
                ".otp-box .otp { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }" +
                ".footer { padding: 15px; background: #f5f5f5; text-align: center; font-size: 12px; color: #999; border-radius: 0 0 5px 5px; }" +
                ".warning { color: #d32f2f; font-size: 14px; margin: 10px 0; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>CareSync - Email Verification</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Hello,</p>" +
                "<p>Thank you for signing up with CareSync! To complete your registration and verify your email address, please use the following One-Time Password (OTP):</p>" +
                "<div class='otp-box'>" +
                "<div class='otp'>" + otp + "</div>" +
                "</div>" +
                "<p>This OTP is valid for 5 minutes only. If you did not request this OTP, please ignore this email.</p>" +
                "<p class='warning'><strong>⚠️ Security Note:</strong> Never share your OTP with anyone, including CareSync support staff.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>&copy; 2026 CareSync. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildVerificationEmailTemplate(String userName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 5px 5px 0 0; }" +
                ".content { padding: 20px; background: #f9f9f9; }" +
                ".button { display: inline-block; background: #667eea; color: white; padding: 10px 20px; border-radius: 5px; text-decoration: none; margin: 20px 0; }" +
                ".footer { padding: 15px; background: #f5f5f5; text-align: center; font-size: 12px; color: #999; border-radius: 0 0 5px 5px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>Welcome to CareSync!</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Hello " + userName + ",</p>" +
                "<p>Your email has been successfully verified! You are now ready to use CareSync to manage your health and wellness.</p>" +
                "<p>Start your journey towards better health by exploring our features:</p>" +
                "<ul><li>Track your hydration levels</li><li>Get weather-based health recommendations</li><li>Receive personalized wellness insights</li><li>Connect with healthcare providers</li></ul>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>&copy; 2026 CareSync. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildPasswordResetEmailTemplate(String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 5px 5px 0 0; }" +
                ".content { padding: 20px; background: #f9f9f9; }" +
                ".otp-box { background: white; border: 2px solid #667eea; padding: 15px; border-radius: 5px; text-align: center; margin: 20px 0; }" +
                ".otp-box .otp { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }" +
                ".footer { padding: 15px; background: #f5f5f5; text-align: center; font-size: 12px; color: #999; border-radius: 0 0 5px 5px; }" +
                ".warning { color: #d32f2f; font-size: 14px; margin: 10px 0; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>CareSync - Password Reset</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Hello,</p>" +
                "<p>We received a request to reset your password. Use the following OTP to proceed with resetting your password:</p>" +
                "<div class='otp-box'>" +
                "<div class='otp'>" + otp + "</div>" +
                "</div>" +
                "<p>This OTP is valid for 5 minutes only. If you did not request a password reset, please ignore this email and your account will remain secure.</p>" +
                "<p class='warning'><strong>⚠️ Security Note:</strong> Never share your OTP with anyone.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>&copy; 2026 CareSync. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildPasswordResetSuccessEmailTemplate() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 5px 5px 0 0; }" +
                ".content { padding: 20px; background: #f9f9f9; }" +
                ".success { color: #2e7d32; font-size: 16px; font-weight: bold; }" +
                ".footer { padding: 15px; background: #f5f5f5; text-align: center; font-size: 12px; color: #999; border-radius: 0 0 5px 5px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>CareSync - Password Reset Successful</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Hello,</p>" +
                "<p class='success'>✓ Your password has been successfully reset!</p>" +
                "<p>You can now log in to your CareSync account using your new password.</p>" +
                "<p>If you did not make this change or believe your account is compromised, please contact our support team immediately.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>&copy; 2026 CareSync. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
