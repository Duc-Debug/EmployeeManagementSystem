package com.hrm.employeemanagement.port.in.user;

import com.hrm.employeemanagement.application.dto.user.UserResult;

public interface ToggleUserStatusUseCase {
    UserResult toggleUserStatus(Long userId, boolean lock, Long currentAdminId);
}
