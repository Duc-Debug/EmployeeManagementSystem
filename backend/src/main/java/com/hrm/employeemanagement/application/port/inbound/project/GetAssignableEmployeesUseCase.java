package com.hrm.employeemanagement.application.port.inbound.project;

import java.time.LocalDate;
import java.util.List;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;

public interface GetAssignableEmployeesUseCase {

    List<ProjectMemberResult> getAssignableEmployees();

    default List<ProjectMemberResult> getAssignableEmployees(LocalDate startDate) {
        return getAssignableEmployees();
    }
}

