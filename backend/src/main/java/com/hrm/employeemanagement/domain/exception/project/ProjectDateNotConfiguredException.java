package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectDateNotConfiguredException extends DomainException {

    public ProjectDateNotConfiguredException(String message) {
        super(message);
    }

    public static ProjectDateNotConfiguredException missingDates(Long projectId) {
        return new ProjectDateNotConfiguredException(
                "Dự án (ID: " + projectId + ") chưa có ngày bắt đầu hoặc ngày kết thúc dự kiến để tính tuần");
    }
}