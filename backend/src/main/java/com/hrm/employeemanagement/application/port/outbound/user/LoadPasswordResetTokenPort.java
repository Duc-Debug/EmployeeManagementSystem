package com.hrm.employeemanagement.application.port.outbound.user;

import com.hrm.employeemanagement.domain.user.PasswordResetToken;

import java.util.Optional;

public interface LoadPasswordResetTokenPort {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
