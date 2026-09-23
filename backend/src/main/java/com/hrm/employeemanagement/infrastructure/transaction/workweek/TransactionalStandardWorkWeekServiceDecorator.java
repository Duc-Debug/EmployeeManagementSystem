package com.hrm.employeemanagement.infrastructure.transaction.workweek;

import com.hrm.employeemanagement.application.dto.workweek.CapacityConversionCommand;
import com.hrm.employeemanagement.application.dto.workweek.CapacityConversionResult;
import com.hrm.employeemanagement.application.dto.workweek.StandardWorkWeekConfigResult;
import com.hrm.employeemanagement.application.dto.workweek.UpdateStandardWorkWeekCommand;
import com.hrm.employeemanagement.application.port.inbound.workweek.ConvertCapacityUnitUseCase;
import com.hrm.employeemanagement.application.port.inbound.workweek.GetStandardWorkWeekConfigUseCase;
import com.hrm.employeemanagement.application.port.inbound.workweek.UpdateStandardWorkWeekConfigUseCase;
import com.hrm.employeemanagement.application.service.workweek.StandardWorkWeekService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class TransactionalStandardWorkWeekServiceDecorator implements
        GetStandardWorkWeekConfigUseCase,
        UpdateStandardWorkWeekConfigUseCase,
        ConvertCapacityUnitUseCase {

    private final StandardWorkWeekService delegate;

    public TransactionalStandardWorkWeekServiceDecorator(StandardWorkWeekService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "StandardWorkWeekService must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public StandardWorkWeekConfigResult execute(String scopeType, Long orgUnitId) {
        return delegate.execute(scopeType, orgUnitId);
    }

    @Override
    @Transactional
    public StandardWorkWeekConfigResult execute(UpdateStandardWorkWeekCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional(readOnly = true)
    public CapacityConversionResult execute(CapacityConversionCommand command) {
        return delegate.execute(command);
    }
}

