package com.hrm.employeemanagement.port.in.user;

import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;

public interface UpdateUserRoleUseCase {
    UserResult updateUserRole(UpdateUserRoleCommand command, Long currentAdminId);
}
