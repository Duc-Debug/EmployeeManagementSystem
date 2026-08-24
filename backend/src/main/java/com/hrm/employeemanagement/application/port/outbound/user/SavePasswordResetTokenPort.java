package com.hrm.employeemanagement.application.port.outbound.user;

import com.hrm.employeemanagement.domain.user.PasswordResetToken;
import com.hrm.employeemanagement.domain.user.UserId;

public interface SavePasswordResetTokenPort {
    PasswordResetToken save(PasswordResetToken token);
    void invalidateActiveTokensByUserId(UserId userId);
}
