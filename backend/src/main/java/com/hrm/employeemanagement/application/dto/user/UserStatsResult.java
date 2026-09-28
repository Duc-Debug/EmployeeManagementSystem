package com.hrm.employeemanagement.application.dto.user;

public record UserStatsResult(
        long totalUsers,
        long activeUsers,
        long lockedUsers
) {}
