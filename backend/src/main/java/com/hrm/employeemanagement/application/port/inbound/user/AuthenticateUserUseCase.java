package com.hrm.employeemanagement.application.port.inbound.user;

import com.hrm.employeemanagement.application.dto.user.AuthTokenResult;
import com.hrm.employeemanagement.application.dto.user.LoginCommand;

public interface AuthenticateUserUseCase {
    AuthTokenResult login(LoginCommand command);
}
