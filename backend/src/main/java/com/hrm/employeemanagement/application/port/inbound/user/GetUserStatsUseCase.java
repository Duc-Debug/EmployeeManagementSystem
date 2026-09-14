package com.hrm.employeemanagement.application.port.inbound.user;

import com.hrm.employeemanagement.application.dto.user.UserStatsResult;

public interface GetUserStatsUseCase {
    UserStatsResult getUserStats();
}
