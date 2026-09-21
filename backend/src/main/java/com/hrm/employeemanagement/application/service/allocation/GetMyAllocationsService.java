package com.hrm.employeemanagement.application.service.allocation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.allocation.MyWeeklyAllocationsResult;
import com.hrm.employeemanagement.application.dto.allocation.MyWeeklyAllocationsResult.AllocationItemDto;
import com.hrm.employeemanagement.application.dto.allocation.MyWeeklyAllocationsResult.WeekScheduleDto;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetMyAllocationsUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadMyAllocationsPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort.ScheduleConfirmationRecord;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.allocation.confirmation.AllocationItem;
import com.hrm.employeemanagement.domain.allocation.confirmation.ConfirmationStatus;
import com.hrm.employeemanagement.domain.allocation.confirmation.ScheduleConfirmationPolicy;
import com.hrm.employeemanagement.domain.allocation.confirmation.WeeklyAllocationCalculator;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Pure Application Service (POJO) - không phụ thuộc Spring framework annotations.
 * Transaction boundary được đảm bảo thông qua TransactionalGetMyAllocationsUseCase decorator.
 */
public class GetMyAllocationsService implements GetMyAllocationsUseCase {

    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadMyAllocationsPort loadMyAllocationsPort;
    private final ScheduleConfirmationPort scheduleConfirmationPort;

    public GetMyAllocationsService(
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
    public MyWeeklyAllocationsResult getMyAllocations(LocalDate weekStart, Integer weeks) {
        User currentUser = authenticatedUserPort.getAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Không tìm thấy người dùng đã xác thực");
        }

        Long userId = currentUser.getId().value();
        LocalDate baseMonday = weekStart != null ? ScheduleConfirmationPolicy.normalizeToMonday(weekStart)
                : ScheduleConfirmationPolicy.normalizeToMonday(LocalDate.now());

        int clampedWeeks = ScheduleConfirmationPolicy.clampWeeks(weeks);

        List<LocalDate> weekMondays = new ArrayList<>();
        for (int i = 0; i < clampedWeeks; i++) {
            weekMondays.add(baseMonday.plusWeeks(i));
        }

        Employee employee = loadEmployeePort.findByUserId(currentUser.getId()).orElse(null);

        List<ScheduleConfirmationRecord> confirmations = scheduleConfirmationPort.findByUserIdAndWeeks(userId, weekMondays);
        Map<LocalDate, ScheduleConfirmationRecord> confirmationMap = confirmations.stream()
                .collect(Collectors.toMap(ScheduleConfirmationRecord::weekStartDate, Function.identity(), (a, b) -> a));

        List<WeekScheduleDto> weekScheduleDtos = new ArrayList<>();
        for (LocalDate monday : weekMondays) {
            List<AllocationItem> weekAllocations;
            if (employee != null) {
                weekAllocations = loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employee.getId().value(), monday);
            } else {
                weekAllocations = Collections.emptyList();
            }

            var totalHours = WeeklyAllocationCalculator.calculateTotalHours(weekAllocations);
            var maxUpdatedAt = WeeklyAllocationCalculator.findMaxUpdatedAt(weekAllocations);
            ScheduleConfirmationRecord conf = confirmationMap.get(monday);
            LocalDateTime confirmedAt = conf != null ? conf.confirmedAt() : null;

            ConfirmationStatus status = ScheduleConfirmationPolicy.determineConfirmationStatus(confirmedAt, maxUpdatedAt);

            List<AllocationItemDto> allocationDtos = weekAllocations.stream()
                    .map(item -> new AllocationItemDto(
                            item.allocationId(),
                            item.projectId(),
                            item.projectName(),
                            item.projectStatus(),
                            item.allocatedHours()
                    ))
                    .toList();

            weekScheduleDtos.add(new WeekScheduleDto(
                    monday,
                    totalHours,
                    status.name(),
                    confirmedAt,
                    allocationDtos
            ));
        }

        return new MyWeeklyAllocationsResult(weekScheduleDtos);
    }
}