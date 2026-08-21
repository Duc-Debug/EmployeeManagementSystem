package com.hrm.employeemanagement.application.port.inbound.user;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;

public interface CreateUserUseCase {
    UserResult createUser(CreateUserCommand command, Long currentAdminId);
}
