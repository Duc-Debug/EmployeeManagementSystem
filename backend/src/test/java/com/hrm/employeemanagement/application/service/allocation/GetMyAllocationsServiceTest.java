package com.hrm.employeemanagement.application.service.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadMyAllocationsPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort.ScheduleConfirmationRecord;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.allocation.confirmation.AllocationItem;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

class GetMyAllocationsServiceTest {

    private GetAuthenticatedUserPort authenticatedUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadMyAllocationsPort loadMyAllocationsPort;
    private ScheduleConfirmationPort scheduleConfirmationPort;
    private GetMyAllocationsService service;

    private final UserId userId = new UserId(100L);
    private final EmployeeId employeeId = new EmployeeId(200L);

    @BeforeEach
    void setUp() {
        authenticatedUserPort = mock(GetAuthenticatedUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadMyAllocationsPort = mock(LoadMyAllocationsPort.class);
        scheduleConfirmationPort = mock(ScheduleConfirmationPort.class);

        service = new GetMyAllocationsService(
                authenticatedUserPort,
                loadEmployeePort,
                loadMyAllocationsPort,
                scheduleConfirmationPort
        );

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(mockUser);

        Employee mockEmployee = mock(Employee.class);
        when(mockEmployee.getId()).thenReturn(employeeId);
        when(loadEmployeePort.findByUserId(userId)).thenReturn(Optional.of(mockEmployee));
    }

    @Test
    @DisplayName("getMyAllocations: Trả về đầy đủ kịch bản CONFIRMED, STALE và NOT_CONFIRMED")
    void testGetMyAllocationsSuccess() {
        LocalDate monday1 = LocalDate.of(2026, 9, 21);
        LocalDate monday2 = LocalDate.of(2026, 9, 28);

        LocalDateTime now = LocalDateTime.of(2026, 9, 18, 10, 0);

        // Week 1: Confirmed
        AllocationItem item1 = new AllocationItem(1L, 10L, "Project A", "ACTIVE", new BigDecimal("40.00"), now.minusDays(1));
        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employeeId.value(), monday1))
                .thenReturn(List.of(item1));
        ScheduleConfirmationRecord conf1 = new ScheduleConfirmationRecord(1L, userId.value(), monday1, now, "127.0.0.1");

        // Week 2: Stale (allocation updated after confirmation)
        AllocationItem item2 = new AllocationItem(2L, 20L, "Project B", "ACTIVE", new BigDecimal("35.00"), now.plusHours(2));
        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employeeId.value(), monday2))
                .thenReturn(List.of(item2));
        ScheduleConfirmationRecord conf2 = new ScheduleConfirmationRecord(2L, userId.value(), monday2, now, "127.0.0.1");

        when(scheduleConfirmationPort.findByUserIdAndWeeks(eq(userId.value()), any()))
                .thenReturn(List.of(conf1, conf2));

        var result = service.getMyAllocations(monday1, 2);

        assertThat(result.weeks()).hasSize(2);
        var week1 = result.weeks().get(0);
        assertThat(week1.weekStartDate()).isEqualTo(monday1);
        assertThat(week1.totalHours()).isEqualTo(new BigDecimal("40.00"));
        assertThat(week1.confirmationStatus()).isEqualTo("CONFIRMED");

        var week2 = result.weeks().get(1);
        assertThat(week2.weekStartDate()).isEqualTo(monday2);
        assertThat(week2.totalHours()).isEqualTo(new BigDecimal("35.00"));
        assertThat(week2.confirmationStatus()).isEqualTo("STALE");
    }

    @Test
    @DisplayName("TC-04: Tuần chưa có phân bổ -> total_hours = 0.00, confirmation_status = NOT_CONFIRMED")
    void testEmptyWeek() {
        LocalDate monday = LocalDate.of(2026, 10, 5);
        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employeeId.value(), monday))
                .thenReturn(Collections.emptyList());
        when(scheduleConfirmationPort.findByUserIdAndWeeks(eq(userId.value()), any()))
                .thenReturn(Collections.emptyList());

        var result = service.getMyAllocations(monday, 1);

        assertThat(result.weeks()).hasSize(1);
        var week = result.weeks().get(0);
        assertThat(week.totalHours()).isEqualTo(new BigDecimal("0.00"));
        assertThat(week.confirmationStatus()).isEqualTo("NOT_CONFIRMED");
        assertThat(week.confirmedAt()).isNull();
        assertThat(week.allocations()).isEmpty();
    }
}