package com.hrm.employeemanagement.application.port.outbound.email;

public interface QueuePasswordResetEmailPort {
    void enqueue(String recipientEmail, String username, String resetToken, long validityMinutes);
}
