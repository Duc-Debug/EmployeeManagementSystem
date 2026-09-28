package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.email.QueuePasswordResetEmailPort;

@Component
public class PasswordResetEmailOutboxAdapter implements QueuePasswordResetEmailPort {
    private final SpringDataPasswordResetEmailOutboxRepository repository;

    public PasswordResetEmailOutboxAdapter(SpringDataPasswordResetEmailOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public void enqueue(String recipientEmail, String username, String resetToken, long validityMinutes) {
        repository.save(new PasswordResetEmailOutboxJpaEntity(
                recipientEmail, username, resetToken, validityMinutes));
    }
}
