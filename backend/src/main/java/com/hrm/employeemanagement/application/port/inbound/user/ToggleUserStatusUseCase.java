package com.hrm.employeemanagement.application.port.inbound.user;

import com.hrm.employeemanagement.application.dto.user.UserResult;

public interface ToggleUserStatusUseCase {
    UserResult toggleUserStatus(Long userId, boolean lock);
}
