package com.hrm.employeemanagement.application.dto.employee;

import java.time.LocalDate;

import com.hrm.employeemanagement.domain.employee.Employee;

public record EmployeeProfileResult(
    Long id,
    Long userId,
    Long orgUnitId,
    String employeeCode,
    String fullName,
    String professionalRole,
    LocalDate startDate,
    LocalDate contractEndDate,
    Boolean isOutsourced,
    Integer standardHoursPerWeek,
    String status,
    Long version
) {
    public EmployeeProfileResult(Long id, Long userId, Long orgUnitId, String employeeCode,
                                 String fullName, String professionalRole, LocalDate startDate,
                                 LocalDate contractEndDate, Boolean isOutsourced,
                                 Integer standardHoursPerWeek, String status) {
        this(id, userId, orgUnitId, employeeCode, fullName, professionalRole, startDate,
                contractEndDate, isOutsourced, standardHoursPerWeek, status, null);
    }

    public static EmployeeProfileResult fromDomain(Employee employee) {
        return new EmployeeProfileResult(
            employee.getIdValue(),
            employee.getUserIdValue(),
            employee.getOrgUnitId(),
            employee.getEmployeeCode(),
            employee.getFullName(),
            employee.getProfessionalRole(),
            employee.getStartDate(),
            employee.getContractEndDate(),
            employee.getIsOutsourced(),
            employee.getStandardHoursPerWeek(),
            employee.getStatusValue(),
            employee.getVersion()
        );
    }
}
