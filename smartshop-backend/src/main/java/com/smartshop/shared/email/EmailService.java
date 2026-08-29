package com.smartshop.shared.email;

public interface EmailService {

    void sendShopRejectionEmail(String toEmail, String userName, String shopName, String rejectionReason);

    void sendShopApprovalEmail(String toEmail, String userName, String shopName);

    void sendStaffRejectionEmail(String toEmail, String userName, String shopName, String rejectionReason);

    void sendStaffApprovalEmail(String toEmail, String userName, String shopName, String roleName);

    void sendWelcomeEmail(String toEmail, String userName);
}
