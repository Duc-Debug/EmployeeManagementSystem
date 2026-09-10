package com.hrm.employeemanagement.application.port.outbound.project;

public interface SaveProjectMemberPort {
    void addMember(Long projectId, Long employeeId);
    void removeMember(Long projectId, Long employeeId);
}
