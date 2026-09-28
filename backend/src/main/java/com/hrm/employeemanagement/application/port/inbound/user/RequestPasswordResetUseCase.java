package com.hrm.employeemanagement.application.port.inbound.user;

import com.hrm.employeemanagement.application.dto.user.RequestPasswordResetCommand;

public interface RequestPasswordResetUseCase {
    void requestPasswordReset(RequestPasswordResetCommand command);
}
