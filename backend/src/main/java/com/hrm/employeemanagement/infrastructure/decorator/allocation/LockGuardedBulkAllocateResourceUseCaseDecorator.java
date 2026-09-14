package com.hrm.employeemanagement.infrastructure.decorator.allocation;

import java.time.LocalDate;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.allocation.BulkAllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.BulkAllocationResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.BulkAllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * Decorator bọc BulkAllocateResourceUseCase để thực thi chốt chặn QTN-18 cho phân bổ hàng loạt.
 */
public class LockGuardedBulkAllocateResourceUseCaseDecorator implements BulkAllocateResourceUseCase {

    private final BulkAllocateResourceUseCase delegate;
    private final CheckAllocationPeriodLockUseCase checkLockUseCase;

    public LockGuardedBulkAllocateResourceUseCaseDecorator(
            BulkAllocateResourceUseCase delegate,
            CheckAllocationPeriodLockUseCase checkLockUseCase
    ) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate BulkAllocateResourceUseCase must not be null");
        this.checkLockUseCase = Objects.requireNonNull(checkLockUseCase, "CheckAllocationPeriodLockUseCase must not be null");
    }

    @Override
    public BulkAllocationResult bulkAllocateResource(BulkAllocateResourceCommand command) {
        LocalDate start = YearWeek.of(command.fromYear(), command.fromWeek()).getStartDate();
        LocalDate end = YearWeek.of(command.toYear(), command.toWeek()).getStartDate();

        LocalDate current = start;
        while (!current.isAfter(end)) {
            YearWeek yw = YearWeek.from(current);
            checkLockUseCase.validateWeekNotLocked(yw.year(), yw.weekNumber());
            current = current.plusWeeks(1);
        }

        return delegate.bulkAllocateResource(command);
    }
}
