package com.hrm.employeemanagement.application.port.inbound.user;

import com.hrm.employeemanagement.application.dto.user.UpdateUserCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;

public interface UpdateUserUseCase {
    UserResult updateUser(UpdateUserCommand command);
}
