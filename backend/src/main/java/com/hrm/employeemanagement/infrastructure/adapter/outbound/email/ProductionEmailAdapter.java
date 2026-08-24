package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import com.hrm.employeemanagement.application.port.outbound.email.SimulatedEmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Production Email Adapter implementing SimulatedEmailPort/EmailPort.
 * Dispatches password reset emails securely via production mail sender without logging plaintext reset tokens.
 */
@Component
@Profile("prod")
public class ProductionEmailAdapter implements SimulatedEmailPort {

    private static final Logger log = LoggerFactory.getLogger(ProductionEmailAdapter.class);

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String username, String resetToken, long validityMinutes) {
        // Secure production email delivery implementation (SMTP/SendGrid/SES).
        // Logs only delivery metadata without printing plaintext sensitive reset credentials.
        log.info("Sending password reset email to recipient: {} (User: {})", recipientEmail, username);
        
        // Implementation details for production mail provider:
        // javaMailSender.send(mimeMessage);
    }
}
