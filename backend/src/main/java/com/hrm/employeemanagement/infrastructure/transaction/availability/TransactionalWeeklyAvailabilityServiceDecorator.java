package com.hrm.employeemanagement.infrastructure.transaction.availability;

import com.hrm.employeemanagement.application.dto.availability.CalculateWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.dto.availability.DeclareWeeklyAvailabilityCommand;
import com.hrm.employeemanagement.application.dto.availability.WeeklyAvailabilityResult;
import com.hrm.employeemanagement.application.port.inbound.availability.CalculateWeeklyCapacityUseCase;
import com.hrm.employeemanagement.application.port.inbound.availability.DeclareWeeklyAvailabilityUseCase;
import com.hrm.employeemanagement.application.port.inbound.availability.GetWeeklyAvailabilityUseCase;
import com.hrm.employeemanagement.application.service.availability.WeeklyAvailabilityService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class TransactionalWeeklyAvailabilityServiceDecorator implements DeclareWeeklyAvailabilityUseCase,
        CalculateWeeklyCapacityUseCase, GetWeeklyAvailabilityUseCase {

    private final WeeklyAvailabilityService delegate;

    public TransactionalWeeklyAvailabilityServiceDecorator(WeeklyAvailabilityService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "WeeklyAvailabilityService must not be null");
    }

    @Override
    @Transactional
    public WeeklyAvailabilityResult execute(DeclareWeeklyAvailabilityCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyAvailabilityResult calculate(CalculateWeeklyCapacityQuery query) {
        return delegate.calculate(query);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyAvailabilityResult getAvailability(Long employeeId, Integer year, Integer weekNumber) {
        return delegate.getAvailability(employeeId, year, weekNumber);
    }
}
