package com.hrm.employeemanagement.application.port.outbound.project;

public interface CountProjectRoleUsagePort {
    long countDemandsByRoleId(Long roleId);
    long countEmployeesByProfessionalRole(String roleName, String roleCode);

    default long countEmployeesByProfessionalRole(String roleName) {
        return countEmployeesByProfessionalRole(roleName, roleName);
    }
}
