package com.hrm.employeemanagement.application.port.inbound.user;

import com.hrm.employeemanagement.application.dto.user.ResetPasswordCommand;

public interface ResetPasswordUseCase {
    void resetPassword(ResetPasswordCommand command);
}
