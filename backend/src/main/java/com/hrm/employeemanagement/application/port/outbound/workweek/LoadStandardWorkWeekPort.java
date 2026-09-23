package com.hrm.employeemanagement.application.port.outbound.workweek;

import java.util.Optional;

import com.hrm.employeemanagement.domain.workweek.StandardWorkWeekConfig;
import com.hrm.employeemanagement.domain.workweek.WorkWeekScope;

public interface LoadStandardWorkWeekPort {
    Optional<StandardWorkWeekConfig> findByScope(WorkWeekScope scope);
    Optional<StandardWorkWeekConfig> findCompanyDefault();
}

