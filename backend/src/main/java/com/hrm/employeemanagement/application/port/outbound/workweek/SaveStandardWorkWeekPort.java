package com.hrm.employeemanagement.application.port.outbound.workweek;

import com.hrm.employeemanagement.domain.workweek.StandardWorkWeekConfig;

public interface SaveStandardWorkWeekPort {
    StandardWorkWeekConfig save(StandardWorkWeekConfig config);
}

