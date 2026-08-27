package com.hrm.employeemanagement.application.port.inbound.user;

import com.hrm.employeemanagement.application.dto.user.LogoutCommand;

public interface LogoutUseCase {
    void logout(LogoutCommand command);
}
