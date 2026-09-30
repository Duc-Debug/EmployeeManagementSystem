package com.hrm.employeemanagement.application.port.outbound.email;

public interface EmailSenderPort {
    void sendPasswordResetEmail(String recipientEmail, String username, String resetToken, long validityMinutes);
}
