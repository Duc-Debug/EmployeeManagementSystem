package com.hrm.employeemanagement.application.port.outbound.notification.dedup;

import java.util.List;

import com.hrm.employeemanagement.application.dto.notification.dedup.EmployeeWeeklyOverloadCandidate;

public interface LoadWeeklyOverloadCandidatesPort {
    List<EmployeeWeeklyOverloadCandidate> loadOverloadCandidates(int year, int weekNumber);
}
