package com.hrm.employeemanagement.application.dto.employee;

import java.time.LocalDate;
import java.util.List;

import com.hrm.employeemanagement.domain.employee.Employee;

public record OutsourcedEmployeeResult(
    Long id,
    Long orgUnitId,
    String orgUnitName,
    String employeeCode,
    String fullName,
    String providerName,
    String professionalRole,
    LocalDate startDate,
    LocalDate contractEndDate,
    Boolean isOutsourced,
    Integer standardHoursPerWeek,
    String status,
    List<String> skillNames,
    Long version
) {
    public static OutsourcedEmployeeResult fromDomain(Employee employee, String orgUnitName, List<String> skillNames) {
        return new OutsourcedEmployeeResult(
            employee.getIdValue(),
            employee.getOrgUnitId(),
            orgUnitName,
            employee.getEmployeeCode(),
            employee.getFullName(),
            employee.getProviderName(),
            employee.getProfessionalRole(),
            employee.getStartDate(),
            employee.getContractEndDate(),
            employee.getIsOutsourced(),
            employee.getStandardHoursPerWeek(),
            employee.getStatusValue(),
            skillNames != null ? skillNames : List.of(),
            employee.getVersion()
        );
    }
}
