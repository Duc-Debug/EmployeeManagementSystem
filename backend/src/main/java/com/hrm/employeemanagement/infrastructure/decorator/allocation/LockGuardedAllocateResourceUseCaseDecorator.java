package com.hrm.employeemanagement.infrastructure.decorator.allocation;

import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;

/**
 * Decorator bọc AllocateResourceUseCase để thực thi chốt chặn quy tắc QTN-18 (TC-02):
 * Ngăn chặn thêm/sửa phân bổ khi tuần nằm trong một kỳ kế hoạch đã bị khóa (LOCKED).
 * Áp dụng Decorator Pattern giúp tuân thủ triệt để nguyên tắc không can thiệp, xóa hay sửa code người khác.
 */
public class LockGuardedAllocateResourceUseCaseDecorator implements AllocateResourceUseCase {

    private final AllocateResourceUseCase delegate;
    private final CheckAllocationPeriodLockUseCase checkLockUseCase;

    public LockGuardedAllocateResourceUseCaseDecorator(
            AllocateResourceUseCase delegate,
            CheckAllocationPeriodLockUseCase checkLockUseCase
    ) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate AllocateResourceUseCase must not be null");
        this.checkLockUseCase = Objects.requireNonNull(checkLockUseCase, "CheckAllocationPeriodLockUseCase must not be null");
    }

    @Override
    public WeeklyCapacityResult allocateResource(AllocateResourceCommand command) {
        // [QTN-18 / TC-02]: Kiểm tra kỳ kế hoạch có đang bị khóa hay không trước khi thực hiện phân bổ
        checkLockUseCase.validateWeekNotLocked(command.year(), command.weekNumber());

        // Nếu kỳ đang mở, chuyển tiếp thực thi logic nghiệp vụ của service gốc
        return delegate.allocateResource(command);
    }

    @Override
    public List<WeeklyCapacityResult> getWeeklyCapacities(List<Long> employeeIds, Integer year, Integer weekNumber) {
        // Thao tác xem năng lực là read-only, ủy quyền trực tiếp cho service gốc
        return delegate.getWeeklyCapacities(employeeIds, year, weekNumber);
    }
}
