package com.smartshop.shared.email;

import java.util.List;

public interface EmailService {

    /**
     * Notifies a shop's staff (BCC) that a customer record was created or updated.
     * A best-effort notification: implementations must not throw on send failure.
     *
     * @param recipientEmails staff email addresses to BCC
     * @param shopName        the shop the customer belongs to
     * @param customerName    the affected customer
     * @param action          "created" or "updated"
     * @param actorName       who made the change
     */
    void sendCustomerChangeNotification(List<String> recipientEmails, String shopName,
                                        String customerName, String action, String actorName);


    void sendShopRejectionEmail(String toEmail, String userName, String shopName, String rejectionReason);

    void sendShopApprovalEmail(String toEmail, String userName, String shopName);

    void sendStaffRejectionEmail(String toEmail, String userName, String shopName, String rejectionReason);

    void sendStaffApprovalEmail(String toEmail, String userName, String shopName, String roleName);

    void sendWelcomeEmail(String toEmail, String userName);

    void sendPasswordResetEmail(String toEmail, String userName, String resetToken);

    void sendEmailVerificationEmail(String toEmail, String userName, String verificationToken);
}
