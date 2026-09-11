package com.hrm.employeemanagement.application.port.outbound.project;

public interface SyncEmployeeProfessionalRolePort {
    int syncRoleName(String oldRoleName, String newRoleName);
}
