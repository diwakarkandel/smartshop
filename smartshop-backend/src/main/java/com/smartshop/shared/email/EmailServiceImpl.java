package com.smartshop.shared.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Override
    public void sendShopRejectionEmail(String toEmail, String userName, String shopName, String rejectionReason) {
        String subject = "Your SmartShop shop application requires re-verification";
        String body = """
                <p>Dear %s,</p>
                <p>Thank you for applying to register <strong>%s</strong> on SmartShop.</p>
                <p>Your shop application requires re-verification before it can be approved.</p>
                <div class="reason-box">
                    <strong>Reason:</strong> %s
                </div>
                <p>Please address the concerns above and submit a new application. Your PAN certificate
                will be reviewed again once you reapply.</p>
                """
                .formatted(escape(userName), escape(shopName), escape(rejectionReason));
        sendHtmlEmail(toEmail, subject, buildHtml("Application Requires Re-verification", body, "Login to reapply"));
    }

    @Override
    public void sendShopApprovalEmail(String toEmail, String userName, String shopName) {
        String subject = "Your SmartShop shop has been approved!";
        String body = """
                <p>Dear %s,</p>
                <p>Congratulations! Your application to register <strong>%s</strong> has been approved.</p>
                <p>Your shop is now live on SmartShop and you have been assigned the
                <strong>SHOP_ADMIN</strong> role.</p>
                <div class="highlight-box">
                    Log in to your account to view your unique staff invite code and start inviting
                    cashiers, managers and inventory staff.
                </div>
                """
                .formatted(escape(userName), escape(shopName));
        sendHtmlEmail(toEmail, subject, buildHtml("Shop Application Approved", body, "Login to your account"));
    }

    @Override
    public void sendStaffRejectionEmail(String toEmail, String userName, String shopName, String rejectionReason) {
        String subject = "Update on your SmartShop join request";
        String body = """
                <p>Dear %s,</p>
                <p>Thank you for your interest in joining <strong>%s</strong> on SmartShop.</p>
                <p>Unfortunately, the shop administrator has declined your join request.</p>
                <div class="reason-box">
                    <strong>Reason:</strong> %s
                </div>
                <p>You may contact the shop administrator directly or submit a new join request in the future.</p>
                """
                .formatted(escape(userName), escape(shopName), escape(rejectionReason));
        sendHtmlEmail(toEmail, subject, buildHtml("Join Request Rejected", body, "Login to reapply"));
    }

    @Override
    public void sendStaffApprovalEmail(String toEmail, String userName, String shopName, String roleName) {
        String subject = "Welcome to %s on SmartShop!".formatted(shopName);
        String body = """
                <p>Dear %s,</p>
                <p>Great news! Your join request for <strong>%s</strong> has been approved.</p>
                <p>You have been assigned the <strong>%s</strong> role.</p>
                <div class="highlight-box">
                    Log in to your account to see your new shop and branch assignments.
                </div>
                """
                .formatted(escape(userName), escape(shopName), escape(roleName));
        sendHtmlEmail(toEmail, subject, buildHtml("Join Request Approved", body, "Login to your account"));
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String userName) {
        String subject = "Welcome to SmartShop!";
        String body = """
                <p>Dear %s,</p>
                <p>Welcome to <strong>SmartShop</strong>! We are thrilled to have you on board.</p>
                <p>SmartShop is your all-in-one platform for managing retail operations, from inventory tracking to point-of-sale transactions.</p>
                <div class="highlight-box">
                    As a new user, you can either register your own shop, or accept an invitation to join an existing one using an invite code.
                </div>
                """
                .formatted(escape(userName));
        sendHtmlEmail(toEmail, subject, buildHtml("Welcome to SmartShop", body, "Get Started Now"));
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        String subject = "Reset your SmartShop password";
        String body = """
                <p>Dear %s,</p>
                <p>We received a request to reset the password for your SmartShop account.</p>
                <p>Please use the following password reset token to choose a new password:</p>
                <div class="highlight-box" style="font-size: 18px; font-weight: bold; letter-spacing: 2px; text-align: center;">
                    %s
                </div>
                <p>This token will expire in 2 hours. If you did not request a password reset, you can safely ignore this email.</p>
                """
                .formatted(escape(userName), escape(resetToken));
        sendHtmlEmail(toEmail, subject, buildHtml("Password Reset Request", body, "Reset Password"));
    }

    @Override
    public void sendEmailVerificationEmail(String toEmail, String userName, String verificationToken) {
        String subject = "Verify your email address for SmartShop";
        String body = """
                <p>Dear %s,</p>
                <p>Thank you for creating an account with SmartShop!</p>
                <p>Please verify your email address using the following verification token:</p>
                <div class="highlight-box" style="font-size: 18px; font-weight: bold; letter-spacing: 2px; text-align: center;">
                    %s
                </div>
                <p>This token will expire in 24 hours.</p>
                """
                .formatted(escape(userName), escape(verificationToken));
        sendHtmlEmail(toEmail, subject, buildHtml("Verify Your Email", body, "Verify Email"));
    }

    @Override
    public void sendCustomerChangeNotification(List<String> recipientEmails, String shopName,
                                               String customerName, String action, String actorName) {
        if (recipientEmails == null || recipientEmails.isEmpty()) {
            return;
        }
        String subject = "Customer " + action + ": " + customerName;
        String body = """
                <p>Hello team,</p>
                <p>A customer record was <strong>%s</strong> in <strong>%s</strong>.</p>
                <div class="highlight-box">
                    <strong>Customer:</strong> %s<br/>
                    <strong>Action:</strong> %s<br/>
                    <strong>By:</strong> %s
                </div>
                <p>This is an automated notification sent to the shop's staff.</p>
                """
                .formatted(escape(action), escape(shopName), escape(customerName),
                        escape(action), escape(actorName));
        sendBccEmail(recipientEmails, subject, buildHtml("Customer Notification", body, "Open SmartShop"));
    }

    /** Sends one HTML message with the recipients as BCC (keeps staff addresses private). */
    private void sendBccEmail(List<String> recipients, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(fromAddress);
            helper.setBcc(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send BCC notification to {} recipient(s): {}", recipients.size(), e.getMessage());
        }
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildHtml(String heading, String bodyContent, String ctaText) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f5f7;font-family:Arial,Helvetica,sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7;padding:24px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="600" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,0.08);">
                                    <tr>
                                        <td style="background-color:#1a73e8;padding:28px 40px;">
                                            <span style="color:#ffffff;font-size:26px;font-weight:bold;letter-spacing:0.5px;">SmartShop</span><br>
                                            <span style="color:#d6e4ff;font-size:13px;">Smart retail management platform</span>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:32px 40px;color:#333333;font-size:15px;line-height:1.6;">
                                            <h2 style="margin:0 0 16px 0;font-size:20px;color:#1a1a1a;">%s</h2>
                                            %s
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:0 40px 36px 40px;" align="center">
                                            <table role="presentation" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center" bgcolor="#1a73e8"
                                                        style="border-radius:6px;padding:12px 32px;color:#ffffff;font-size:15px;font-weight:bold;">
                                                        %s
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#f8f9fa;padding:18px 40px;text-align:center;color:#888888;font-size:12px;">
                                            This is an automated message from SmartShop. Please do not reply to this email.
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(escape(heading), escape(heading), bodyContent, ctaText);
    }

    private String escape(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
