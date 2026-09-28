package com.hrm.employeemanagement.infrastructure.decorator.allocation;

import java.util.Objects;

import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.DeleteWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;

/**
 * Decorator bọc DeleteWeeklyProjectAllocationPort để thực thi quy tắc QTN-18 (TC-02)
 * ở tầng persistence mutation đối với thao tác DELETE / REMOVE:
 * Ngăn chặn mọi thao tác xóa phân bổ khi tuần nằm trong một kỳ kế hoạch đã bị khóa (LOCKED).
 *
 * Áp dụng Decorator Pattern tuân thủ triệt để nguyên tắc không can thiệp, xóa hay sửa code của người khác.
 */
public class LockGuardedDeleteWeeklyProjectAllocationPortDecorator implements DeleteWeeklyProjectAllocationPort {

    private final DeleteWeeklyProjectAllocationPort delegate;
    private final CheckAllocationPeriodLockUseCase checkLockUseCase;
    private final SpringDataWeeklyProjectAllocationRepository repository;

    public LockGuardedDeleteWeeklyProjectAllocationPortDecorator(
            DeleteWeeklyProjectAllocationPort delegate,
            CheckAllocationPeriodLockUseCase checkLockUseCase,
            SpringDataWeeklyProjectAllocationRepository repository
    ) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate DeleteWeeklyProjectAllocationPort must not be null");
        this.checkLockUseCase = Objects.requireNonNull(checkLockUseCase, "CheckAllocationPeriodLockUseCase must not be null");
        this.repository = Objects.requireNonNull(repository, "SpringDataWeeklyProjectAllocationRepository must not be null");
    }

    @Override
    public void delete(WeeklyProjectAllocation allocation) {
        if (allocation == null) {
            return;
        }

        // [QTN-18]: Chặn xóa phân bổ nếu tuần thuộc kỳ đã bị khóa (LOCKED)
        checkLockUseCase.validateWeekNotLocked(allocation.getYear(), allocation.getWeekNumber());

        delegate.delete(allocation);
    }

    @Override
    public void deleteById(Long allocationId) {
        if (allocationId == null) {
            return;
        }

        // Tìm kiếm bản ghi để lấy năm và tuần phân bổ trước khi cho phép xóa
        repository.findById(allocationId).ifPresent(entity -> {
            checkLockUseCase.validateWeekNotLocked(entity.getYear(), entity.getWeekNumber());
        });

        delegate.deleteById(allocationId);
    }
}
