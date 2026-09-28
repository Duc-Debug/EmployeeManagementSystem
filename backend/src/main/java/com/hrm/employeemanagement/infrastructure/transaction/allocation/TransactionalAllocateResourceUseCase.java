package com.hrm.employeemanagement.infrastructure.transaction.allocation;

import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
public class TransactionalAllocateResourceUseCase implements AllocateResourceUseCase {

    private final AllocateResourceUseCase pureDelegate;

    public TransactionalAllocateResourceUseCase(AllocateResourceUseCase pureDelegate) {
        this.pureDelegate = Objects.requireNonNull(pureDelegate, "AllocateResourceUseCase delegate must not be null");
    }

    @Override
    @Transactional
    public WeeklyCapacityResult allocateResource(AllocateResourceCommand command) {
        return pureDelegate.allocateResource(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyCapacityResult> getWeeklyCapacities(List<Long> employeeIds, Integer year, Integer weekNumber) {
        return pureDelegate.getWeeklyCapacities(employeeIds, year, weekNumber);
    }
}
