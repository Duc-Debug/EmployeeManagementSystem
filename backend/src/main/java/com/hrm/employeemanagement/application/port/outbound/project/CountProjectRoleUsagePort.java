package com.hrm.employeemanagement.application.port.outbound.project;

public interface CountProjectRoleUsagePort {
    long countDemandsByRoleId(Long roleId);
    long countEmployeesByProfessionalRole(String roleName);
}
