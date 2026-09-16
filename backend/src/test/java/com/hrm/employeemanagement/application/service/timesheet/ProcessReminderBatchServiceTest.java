package com.hrm.employeemanagement.application.service.timesheet;

import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetHistoryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetStatus;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessReminderBatchServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 18);
    private static final LocalDateTime REMINDED_AT = LocalDateTime.of(2026, 9, 18, 9, 0);
    private static final EmployeeId EMPLOYEE_ID = new EmployeeId(10L);

    @Mock private LoadTimesheetPort loadTimesheetPort;
    @Mock private SaveTimesheetPort saveTimesheetPort;
    @Mock private LoadEmployeePort loadEmployeePort;
    @Mock private SaveNotificationPort saveNotificationPort;
    @Mock private SaveTimesheetHistoryPort saveTimesheetHistoryPort;

    private ProcessReminderBatchService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-18T09:00:00Z"), ZoneOffset.UTC);
        service = new ProcessReminderBatchService(loadTimesheetPort, saveTimesheetPort, loadEmployeePort,
                saveNotificationPort, saveTimesheetHistoryPort, clock);
    }

    @Test
    void processBatch_createsReminderHistoryAndMarksEligibleTimesheetAsReminded() {
        Timesheet timesheet = draftTimesheet();
        when(loadTimesheetPort.findDraftTimesheetsForReminderUpTo(TODAY, 500)).thenReturn(List.of(timesheet));
        when(loadEmployeePort.findAllByIdIn(List.of(EMPLOYEE_ID))).thenReturn(List.of(employeeWithUser()));

        int processed = service.processBatch(TODAY, 500);

        assertEquals(1, processed);
        assertEquals(REMINDED_AT, timesheet.getRemindedAt());
        ArgumentCaptor<List> notifications = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> histories = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> timesheets = ArgumentCaptor.forClass(List.class);
        verify(saveNotificationPort).saveAll(notifications.capture());
        verify(saveTimesheetHistoryPort).saveAll(histories.capture());
        verify(saveTimesheetPort).saveAll(timesheets.capture());
        assertEquals(1, notifications.getValue().size());
        assertEquals(1, histories.getValue().size());
        assertEquals(List.of(timesheet), timesheets.getValue());
    }

    @Test
    void processBatch_doesNotMarkTimesheetWhenEmployeeIsMissing_soItCanBeRetried() {
        Timesheet timesheet = draftTimesheet();
        when(loadTimesheetPort.findDraftTimesheetsForReminderUpTo(TODAY, 500)).thenReturn(List.of(timesheet));
        when(loadEmployeePort.findAllByIdIn(List.of(EMPLOYEE_ID))).thenReturn(List.of());

        int processed = service.processBatch(TODAY, 500);

        assertEquals(0, processed);
        assertNull(timesheet.getRemindedAt());
        verify(saveNotificationPort, never()).saveAll(anyList());
        verify(saveTimesheetHistoryPort, never()).saveAll(anyList());
        verify(saveTimesheetPort, never()).saveAll(anyList());
    }

    private Timesheet draftTimesheet() {
        return new Timesheet(new TimesheetId(100L), EMPLOYEE_ID, TODAY.minusWeeks(1), TODAY.minusDays(5),
                BigDecimal.ZERO, TimesheetStatus.DRAFT, null, null, null, null, null, null, 0L, List.of());
    }

    private Employee employeeWithUser() {
        return new Employee(EMPLOYEE_ID, new UserId(20L), 1L, "EMP-010", "Nguyen Van A",
                false, 40, EmployeeStatus.ACTIVE);
    }
}
