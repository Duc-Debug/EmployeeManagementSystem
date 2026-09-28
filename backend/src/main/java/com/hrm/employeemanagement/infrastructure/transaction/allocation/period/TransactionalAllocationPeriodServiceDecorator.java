package com.hrm.employeemanagement.infrastructure.transaction.allocation.period;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPeriodResult;
import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPlanSnapshotResult;
import com.hrm.employeemanagement.application.dto.allocation.period.CreatePeriodCommand;
import com.hrm.employeemanagement.application.dto.allocation.period.LockPeriodCommand;
import com.hrm.employeemanagement.application.dto.allocation.period.PeriodLockCheckResult;
import com.hrm.employeemanagement.application.dto.allocation.period.UnlockPeriodCommand;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CreateAllocationPeriodUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.GetAllocationPeriodsUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.LockAllocationPeriodUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.UnlockAllocationPeriodUseCase;
import com.hrm.employeemanagement.application.service.allocation.period.AllocationPeriodService;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodStatus;

/**
 * Transactional Decorator quản lý boundary Transaction tại tầng Infrastructure theo đúng backend-guideline.md.
 */
public class TransactionalAllocationPeriodServiceDecorator implements
        CreateAllocationPeriodUseCase,
        LockAllocationPeriodUseCase,
        UnlockAllocationPeriodUseCase,
        GetAllocationPeriodsUseCase,
        CheckAllocationPeriodLockUseCase {

    private final AllocationPeriodService delegate;

    public TransactionalAllocationPeriodServiceDecorator(AllocationPeriodService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "AllocationPeriodService must not be null");
    }

    @Override
    @Transactional
    public AllocationPeriodResult createPeriod(CreatePeriodCommand command) {
        return delegate.createPeriod(command);
    }

    @Override
    @Transactional
    public AllocationPeriodResult lockPeriod(LockPeriodCommand command) {
        return delegate.lockPeriod(command);
    }

    @Override
    @Transactional
    public AllocationPeriodResult unlockPeriod(UnlockPeriodCommand command) {
        return delegate.unlockPeriod(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationPeriodResult> getPeriods(Integer year, AllocationPeriodStatus status) {
        return delegate.getPeriods(year, status);
    }

    @Override
    @Transactional(readOnly = true)
    public AllocationPeriodResult getPeriodById(Long id) {
        return delegate.getPeriodById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllocationPlanSnapshotResult> getPeriodSnapshots(Long periodId) {
        return delegate.getPeriodSnapshots(periodId);
    }

    @Override
    @Transactional(readOnly = true)
    public AllocationPlanSnapshotResult getSnapshotDetail(Long periodId, Long snapshotId) {
        return delegate.getSnapshotDetail(periodId, snapshotId);
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodLockCheckResult checkWeekLock(int year, int weekNumber) {
        return delegate.checkWeekLock(year, weekNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateWeekNotLocked(int year, int weekNumber) {
        delegate.validateWeekNotLocked(year, weekNumber);
    }
}
