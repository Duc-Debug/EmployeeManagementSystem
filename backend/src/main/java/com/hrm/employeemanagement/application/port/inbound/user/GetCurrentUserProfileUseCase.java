package com.hrm.employeemanagement.application.port.inbound.user;

import com.hrm.employeemanagement.application.dto.user.UserResult;

public interface GetCurrentUserProfileUseCase {
    UserResult getCurrentUserProfile(Long userId);
}
