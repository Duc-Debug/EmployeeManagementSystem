package com.hrm.employeemanagement.infrastructure.decorator.allocation;

import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * Decorator bọc SaveWeeklyProjectAllocationPort để thực thi quy tắc QTN-18 (TC-02)
 * ở tầng persistence mutation: Ngăn chặn mọi thao tác thêm mới, sửa đổi hoặc gỡ/xóa phân bổ
 * khi tuần nằm trong một kỳ kế hoạch đã bị khóa (LOCKED).
 *
 * Áp dụng Decorator Pattern tuân thủ triệt để nguyên tắc không can thiệp, xóa hay sửa code của người khác.
 */
public class LockGuardedSaveWeeklyProjectAllocationPortDecorator implements SaveWeeklyProjectAllocationPort {

    private final SaveWeeklyProjectAllocationPort delegate;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final CheckAllocationPeriodLockUseCase checkLockUseCase;

    public LockGuardedSaveWeeklyProjectAllocationPortDecorator(
            SaveWeeklyProjectAllocationPort delegate,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            CheckAllocationPeriodLockUseCase checkLockUseCase
    ) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate SaveWeeklyProjectAllocationPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.checkLockUseCase = Objects.requireNonNull(checkLockUseCase, "CheckAllocationPeriodLockUseCase must not be null");
    }

    @Override
    public WeeklyProjectAllocation save(WeeklyProjectAllocation allocation) {
        if (allocation == null) {
            return null;
        }

        int year = allocation.getYear();
        int weekNumber = allocation.getWeekNumber();

        // Kiểm tra xem có phải là thao tác thay đổi giá trị phân bổ (thêm mới, sửa giờ, đổi %, hoặc gỡ phân bổ)
        boolean isAllocationValueModified = true;
        if (allocation.getId() != null) {
            Optional<WeeklyProjectAllocation> existingOpt = loadAllocationPort.loadAllocation(
                    allocation.getEmployeeId(),
                    allocation.getProjectId(),
                    YearWeek.of(year, weekNumber)
            );
            if (existingOpt.isPresent()) {
                WeeklyProjectAllocation existing = existingOpt.get();
                boolean hoursUnchanged = Objects.equals(existing.getAllocatedHours(), allocation.getAllocatedHours());
                boolean pctUnchanged = Objects.equals(existing.getAllocationPercentage(), allocation.getAllocationPercentage());

                // Nếu số giờ và tỷ lệ % phân bổ không thay đổi (ví dụ hệ thống chỉ cập nhật cờ isOverloaded
                // khi duyệt nghỉ phép hoặc chạy job), thì không coi là hành vi sửa đổi phân bổ từ người dùng
                if (hoursUnchanged && pctUnchanged) {
                    isAllocationValueModified = false;
                }
            }
        }

        if (isAllocationValueModified) {
            // [QTN-18]: Chặn toàn bộ mutation của phân bổ nếu tuần thuộc kỳ đã bị khóa
            checkLockUseCase.validateWeekNotLocked(year, weekNumber);
        }

        return delegate.save(allocation);
    }
}
