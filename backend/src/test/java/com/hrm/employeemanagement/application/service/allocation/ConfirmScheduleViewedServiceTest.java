package com.hrm.employeemanagement.application.service.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

class ConfirmScheduleViewedServiceTest {

    private GetAuthenticatedUserPort authenticatedUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadMyAllocationsPort loadMyAllocationsPort;
    private ScheduleConfirmationPort scheduleConfirmationPort;
    private ConfirmScheduleViewedService service;

    private final UserId userId = new UserId(100L);
    private final EmployeeId employeeId = new EmployeeId(200L);
    private final LocalDate monday = LocalDate.of(2026, 9, 21);

    @BeforeEach
    void setUp() {
        authenticatedUserPort = mock(GetAuthenticatedUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadMyAllocationsPort = mock(LoadMyAllocationsPort.class);
        scheduleConfirmationPort = mock(ScheduleConfirmationPort.class);

        service = new ConfirmScheduleViewedService(
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
    @DisplayName("Kịch bản 1: Xác nhận lần đầu -> HTTP 201 Created, already_confirmed: false")
    void testFirstTimeConfirmation() {
        when(scheduleConfirmationPort.findByUserIdAndWeek(userId.value(), monday))
                .thenReturn(Optional.empty());

        LocalDateTime now = LocalDateTime.now();
        when(scheduleConfirmationPort.saveConfirmation(eq(userId.value()), eq(monday), any(), eq("192.168.1.1")))
                .thenReturn(new ScheduleConfirmationPort.SaveConfirmationResult(
                        new ScheduleConfirmationRecord(1L, userId.value(), monday, now, "192.168.1.1"),
                        true
                ));

        var result = service.confirmScheduleViewed(monday, "192.168.1.1");

        assertThat(result.httpStatusCode()).isEqualTo(201);
        assertThat(result.alreadyConfirmed()).isFalse();
        assertThat(result.previousConfirmationWasStale()).isNull();
        assertThat(result.confirmationStatus()).isEqualTo("CONFIRMED");

        verify(scheduleConfirmationPort).saveConfirmation(eq(userId.value()), eq(monday), any(), eq("192.168.1.1"));
    }

    @Test
    @DisplayName("Fix HIGH: Concurrent Race condition ở first confirmation -> Request race thua nhận HTTP 200 OK, already_confirmed: true")
    void testFirstTimeConfirmation_RaceConditionLosingRequest_Returns200AndAlreadyConfirmedTrue() {
        // Ban đầu cả 2 request đều thấy empty
        when(scheduleConfirmationPort.findByUserIdAndWeek(userId.value(), monday))
                .thenReturn(Optional.empty());

        LocalDateTime committedAtByWinner = LocalDateTime.now();
        // Adapter bắt DataIntegrityViolationException và trả về newlyCreated = false
        when(scheduleConfirmationPort.saveConfirmation(eq(userId.value()), eq(monday), any(), eq("192.168.1.1")))
                .thenReturn(new ScheduleConfirmationPort.SaveConfirmationResult(
                        new ScheduleConfirmationRecord(1L, userId.value(), monday, committedAtByWinner, "192.168.1.1"),
                        false
                ));

        var result = service.confirmScheduleViewed(monday, "192.168.1.1");

        // Request race thua phải nhận HTTP 200 OK idempotent và already_confirmed = true
        assertThat(result.httpStatusCode()).isEqualTo(200);
        assertThat(result.alreadyConfirmed()).isTrue();
        assertThat(result.previousConfirmationWasStale()).isNull();
        assertThat(result.confirmationStatus()).isEqualTo("CONFIRMED");
        assertThat(result.confirmedAt()).isEqualTo(committedAtByWinner);
    }

    @Test
    @DisplayName("TC-05: Double-click khi dữ liệu không đổi -> HTTP 200 OK, already_confirmed: true, không ghi DB")
    void testDoubleClickUnchanged() {
        LocalDateTime oldConfirmed = LocalDateTime.of(2026, 9, 18, 10, 0);
        when(scheduleConfirmationPort.findByUserIdAndWeek(userId.value(), monday))
                .thenReturn(Optional.of(new ScheduleConfirmationRecord(1L, userId.value(), monday, oldConfirmed, "192.168.1.1")));

        AllocationItem item = new AllocationItem(1L, 10L, "Project A", "ACTIVE", new BigDecimal("40.00"), oldConfirmed.minusHours(1));
        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employeeId.value(), monday))
                .thenReturn(List.of(item));

        var result = service.confirmScheduleViewed(monday, "192.168.1.1");

        assertThat(result.httpStatusCode()).isEqualTo(200);
        assertThat(result.alreadyConfirmed()).isTrue();
        assertThat(result.confirmedAt()).isEqualTo(oldConfirmed);
        assertThat(result.previousConfirmationWasStale()).isNull();

        verify(scheduleConfirmationPort, never()).saveConfirmation(any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-07: Xác nhận lại khi STALE -> HTTP 200 OK, already_confirmed: false, previous_confirmation_was_stale: true")
    void testReconfirmStale() {
        LocalDateTime oldConfirmed = LocalDateTime.of(2026, 9, 18, 10, 0);
        when(scheduleConfirmationPort.findByUserIdAndWeek(userId.value(), monday))
                .thenReturn(Optional.of(new ScheduleConfirmationRecord(1L, userId.value(), monday, oldConfirmed, "192.168.1.1")));

        // Allocation modified at 11:00 > 10:00 -> STALE
        AllocationItem item = new AllocationItem(1L, 10L, "Project A", "ACTIVE", new BigDecimal("40.00"), oldConfirmed.plusHours(1));
        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employeeId.value(), monday))
                .thenReturn(List.of(item));

        LocalDateTime newConfirmed = LocalDateTime.now();
        when(scheduleConfirmationPort.saveConfirmation(eq(userId.value()), eq(monday), any(), eq("192.168.1.1")))
                .thenReturn(new ScheduleConfirmationPort.SaveConfirmationResult(
                        new ScheduleConfirmationRecord(1L, userId.value(), monday, newConfirmed, "192.168.1.1"),
                        false
                ));

        var result = service.confirmScheduleViewed(monday, "192.168.1.1");

        assertThat(result.httpStatusCode()).isEqualTo(200);
        assertThat(result.alreadyConfirmed()).isFalse();
        assertThat(result.previousConfirmationWasStale()).isTrue();
        assertThat(result.confirmedAt()).isEqualTo(newConfirmed);

        verify(scheduleConfirmationPort).saveConfirmation(eq(userId.value()), eq(monday), any(), eq("192.168.1.1"));
    }
}