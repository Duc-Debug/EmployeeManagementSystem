package com.hrm.employeemanagement.application.port.inbound.conflict;

import com.hrm.employeemanagement.application.dto.conflict.AssignScheduleConflictHandlerCommand;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;

public interface AssignScheduleConflictHandlerUseCase {
    ScheduleConflictResult assignScheduleConflictHandler(AssignScheduleConflictHandlerCommand command);
}
