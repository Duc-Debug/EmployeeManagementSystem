package com.hrm.employeemanagement.application.service.allocation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.allocation.ConfirmScheduleViewedResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.ConfirmScheduleViewedUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadMyAllocationsPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort.ScheduleConfirmationRecord;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.allocation.confirmation.AllocationItem;
import com.hrm.employeemanagement.domain.allocation.confirmation.ScheduleConfirmationPolicy;
import com.hrm.employeemanagement.domain.allocation.confirmation.ScheduleConfirmationPolicy.ConfirmationActionResult;
import com.hrm.employeemanagement.domain.allocation.confirmation.WeeklyAllocationCalculator;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Pure Application Service (POJO) - không phụ thuộc Spring framework annotations.
 * Transaction boundary được đảm bảo thông qua TransactionalConfirmScheduleViewedUseCase decorator.
 */
public class ConfirmScheduleViewedService implements ConfirmScheduleViewedUseCase {

    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadMyAllocationsPort loadMyAllocationsPort;
    private final ScheduleConfirmationPort scheduleConfirmationPort;

    public ConfirmScheduleViewedService(
            GetAuthenticatedUserPort authenticatedUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadMyAllocationsPort loadMyAllocationsPort,
            ScheduleConfirmationPort scheduleConfirmationPort) {
        this.authenticatedUserPort = Objects.requireNonNull(authenticatedUserPort, "GetAuthenticatedUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadMyAllocationsPort = Objects.requireNonNull(loadMyAllocationsPort, "LoadMyAllocationsPort must not be null");
        this.scheduleConfirmationPort = Objects.requireNonNull(scheduleConfirmationPort, "ScheduleConfirmationPort must not be null");
    }

    @Override
    public ConfirmScheduleViewedResult confirmScheduleViewed(LocalDate weekStart, String ipAddress) {
        User currentUser = authenticatedUserPort.getAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Không tìm thấy người dùng đã xác thực");
        }

        Long userId = currentUser.getId().value();
        LocalDate monday = ScheduleConfirmationPolicy.normalizeToMonday(weekStart);

        Employee employee = loadEmployeePort.findByUserId(currentUser.getId()).orElse(null);

        List<AllocationItem> allocations = Collections.emptyList();
        if (employee != null) {
            allocations = loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employee.getId().value(), monday);
        }

        LocalDateTime maxUpdatedAt = WeeklyAllocationCalculator.findMaxUpdatedAt(allocations);
        Optional<ScheduleConfirmationRecord> existing = scheduleConfirmationPort.findByUserIdAndWeek(userId, monday);

        LocalDateTime currentConfirmedAt = existing.map(ScheduleConfirmationRecord::confirmedAt).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        ConfirmationActionResult actionResult = ScheduleConfirmationPolicy.evaluateConfirmationAction(
                currentConfirmedAt,
                maxUpdatedAt,
                now
        );

        LocalDateTime finalConfirmedAt;
        int statusCode = actionResult.httpStatusCode();
        boolean alreadyConfirmed = actionResult.alreadyConfirmed();

        if (actionResult.httpStatusCode() == 201 || actionResult.previousConfirmationWasStale()) {
            // First time hoặc re-confirm khi STALE -> Lưu xuống DB qua port
            ScheduleConfirmationPort.SaveConfirmationResult saveResult = scheduleConfirmationPort.saveConfirmation(
                    userId,
                    monday,
                    actionResult.confirmedAt(),
                    ipAddress
            );
            finalConfirmedAt = saveResult.record().confirmedAt();

            // Nếu ban đầu dự định 201 Created nhưng thực tế bị race condition thua (newlyCreated = false)
            // thì chuyển thành idempotent HTTP 200 OK với alreadyConfirmed = true
            if (statusCode == 201 && !saveResult.newlyCreated()) {
                statusCode = 200;
                alreadyConfirmed = true;
            }
        } else {
            // Double-click / retry khi không đổi -> Giữ mốc confirmed_at cũ
            finalConfirmedAt = actionResult.confirmedAt();
        }

        return new ConfirmScheduleViewedResult(
                statusCode,
                monday,
                finalConfirmedAt,
                "CONFIRMED",
                alreadyConfirmed,
                actionResult.previousConfirmationWasStale() ? Boolean.TRUE : null
        );
    }
}