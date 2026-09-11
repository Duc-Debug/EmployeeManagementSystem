package com.hrm.employeemanagement.application.port.inbound.task;

import com.hrm.employeemanagement.application.dto.task.cascade.CascadeDelayWarningResult;
import com.hrm.employeemanagement.application.dto.task.cascade.EvaluateCascadeDelayCommand;

public interface EvaluateCascadeDelayUseCase {
    CascadeDelayWarningResult evaluateCascadeDelay(EvaluateCascadeDelayCommand command);
}
