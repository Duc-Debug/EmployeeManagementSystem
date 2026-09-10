package com.hrm.employeemanagement.application.service.calendar;

import com.hrm.employeemanagement.application.dto.calendar.CompanyWorkingCalendarResult;
import com.hrm.employeemanagement.application.dto.calendar.CreateHolidayCommand;
import com.hrm.employeemanagement.application.dto.calendar.HolidayResult;
import com.hrm.employeemanagement.application.dto.calendar.UpdateHolidayCommand;
import com.hrm.employeemanagement.application.dto.calendar.WorkingCalendarDayDto;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayCommandPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayQueryPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.HolidayRecord;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.SaveWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.calendar.WorkingCalendarDay;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.calendar.DuplicateHolidayException;
import com.hrm.employeemanagement.domain.exception.calendar.HolidayNotFoundException;
import com.hrm.employeemanagement.domain.exception.calendar.InvalidWorkingCalendarException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkingCalendarService Application Tests (NCL-05-CN-001)")
class WorkingCalendarServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private LoadWorkingCalendarPort loadWorkingCalendarPort;

    @Mock
    private SaveWorkingCalendarPort saveWorkingCalendarPort;

    @Mock
    private HolidayQueryPort holidayQueryPort;

    @Mock
    private HolidayCommandPort holidayCommandPort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    private WorkingCalendarService service;

    private static final Long ACTOR_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new WorkingCalendarService(
                authorizationService,
                loadWorkingCalendarPort,
                saveWorkingCalendarPort,
                holidayQueryPort,
                holidayCommandPort,
                saveAuditLogPort
        );
    }

    @Test
    @DisplayName("Lấy cấu hình lịch làm việc tuần thành công")
    void getWorkingCalendar_Success() {
        when(authorizationService.require(PermissionCode.WORKING_CALENDAR_READ)).thenReturn(ACTOR_USER_ID);
        when(loadWorkingCalendarPort.loadCompanyCalendar()).thenReturn(CompanyWorkingCalendar.createDefault());

        CompanyWorkingCalendarResult result = service.getWorkingCalendar();

        assertNotNull(result);
        assertEquals(7, result.days().size());
        assertTrue(result.days().stream().anyMatch(d -> d.dayOfWeek() == DayOfWeek.MONDAY && d.isWorkingDay()));
        assertTrue(result.days().stream().anyMatch(d -> d.dayOfWeek() == DayOfWeek.SUNDAY && !d.isWorkingDay()));
        verify(authorizationService).require(PermissionCode.WORKING_CALENDAR_READ);
    }

    @Test
    @DisplayName("Cập nhật lịch làm việc tuần thành công và lưu Audit Log (TC-04)")
    void updateWorkingCalendar_Success_PersistsAuditLog() {
        when(authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE)).thenReturn(ACTOR_USER_ID);
        when(loadWorkingCalendarPort.loadCompanyCalendar()).thenReturn(CompanyWorkingCalendar.createDefault());
        when(saveWorkingCalendarPort.saveCompanyCalendar(any())).thenAnswer(inv -> inv.getArgument(0));

        List<WorkingCalendarDayDto> updateDays = List.of(
                new WorkingCalendarDayDto(DayOfWeek.MONDAY, true),
                new WorkingCalendarDayDto(DayOfWeek.TUESDAY, true),
                new WorkingCalendarDayDto(DayOfWeek.WEDNESDAY, true),
                new WorkingCalendarDayDto(DayOfWeek.THURSDAY, true),
                new WorkingCalendarDayDto(DayOfWeek.FRIDAY, true),
                new WorkingCalendarDayDto(DayOfWeek.SATURDAY, true), // Bật Thứ 7
                new WorkingCalendarDayDto(DayOfWeek.SUNDAY, false)
        );

        CompanyWorkingCalendarResult result = service.updateWorkingCalendar(updateDays);

        assertNotNull(result);
        assertTrue(result.days().stream().anyMatch(d -> d.dayOfWeek() == DayOfWeek.SATURDAY && d.isWorkingDay()));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ACTOR_USER_ID, audit.getUserId());
        assertEquals("UPDATE_WORKING_CALENDAR", audit.getAction());
        assertEquals("working_calendar_configs", audit.getTableName());
        assertTrue(audit.getNewValue().contains("SATURDAY=true"));
    }

    @Test
    @DisplayName("Cập nhật lịch làm việc tuần không có ngày làm việc nào -> Ném InvalidWorkingCalendarException")
    void updateWorkingCalendar_AllFalse_ThrowsException() {
        when(authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE)).thenReturn(ACTOR_USER_ID);
        when(loadWorkingCalendarPort.loadCompanyCalendar()).thenReturn(CompanyWorkingCalendar.createDefault());

        List<WorkingCalendarDayDto> invalidDays = List.of(
                new WorkingCalendarDayDto(DayOfWeek.MONDAY, false),
                new WorkingCalendarDayDto(DayOfWeek.TUESDAY, false),
                new WorkingCalendarDayDto(DayOfWeek.WEDNESDAY, false),
                new WorkingCalendarDayDto(DayOfWeek.THURSDAY, false),
                new WorkingCalendarDayDto(DayOfWeek.FRIDAY, false),
                new WorkingCalendarDayDto(DayOfWeek.SATURDAY, false),
                new WorkingCalendarDayDto(DayOfWeek.SUNDAY, false)
        );

        assertThrows(InvalidWorkingCalendarException.class, () -> service.updateWorkingCalendar(invalidDays));
        verify(saveWorkingCalendarPort, never()).saveCompanyCalendar(any());
    }

    @Test
    @DisplayName("TC-01 & TC-04: Thêm ngày lễ thành công và lưu Audit Log")
    void createHoliday_Success_PersistsAuditLog() {
        when(authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE)).thenReturn(ACTOR_USER_ID);
        LocalDate holidayDate = LocalDate.of(2026, 9, 2);
        when(holidayQueryPort.existsByDate(holidayDate)).thenReturn(false);
        when(holidayCommandPort.create(holidayDate, "Quốc khánh", 8))
                .thenReturn(new HolidayRecord(1L, holidayDate, "Quốc khánh", 8));

        CreateHolidayCommand command = new CreateHolidayCommand(holidayDate, "Quốc khánh", 8);
        HolidayResult result = service.createHoliday(command);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(holidayDate, result.holidayDate());
        assertEquals("Quốc khánh", result.name());
        assertEquals(8, result.workingHoursDeducted());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals(ACTOR_USER_ID, audit.getUserId());
        assertEquals("CREATE_HOLIDAY", audit.getAction());
        assertEquals("holidays", audit.getTableName());
        assertEquals(1L, audit.getRecordId());
        assertTrue(audit.getNewValue().contains("2026-09-02"));
    }

    @Test
    @DisplayName("TC-02: Thêm ngày lễ trùng ngày đã có -> Ném DuplicateHolidayException")
    void createHoliday_DuplicateDate_ThrowsDuplicateHolidayException() {
        when(authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE)).thenReturn(ACTOR_USER_ID);
        LocalDate duplicateDate = LocalDate.of(2026, 9, 2);
        when(holidayQueryPort.existsByDate(duplicateDate)).thenReturn(true);

        CreateHolidayCommand command = new CreateHolidayCommand(duplicateDate, "Quốc khánh trùng", 8);

        DuplicateHolidayException ex = assertThrows(DuplicateHolidayException.class, () -> service.createHoliday(command));
        assertTrue(ex.getMessage().contains("2026-09-02"));
        verify(holidayCommandPort, never()).create(any(), any(), anyInt());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật ngày lễ thành công và lưu Audit Log")
    void updateHoliday_Success_PersistsAuditLog() {
        when(authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE)).thenReturn(ACTOR_USER_ID);
        LocalDate oldDate = LocalDate.of(2026, 9, 2);
        LocalDate newDate = LocalDate.of(2026, 9, 3);
        HolidayRecord existing = new HolidayRecord(5L, oldDate, "Lễ cũ", 8);

        when(holidayQueryPort.findById(5L)).thenReturn(Optional.of(existing));
        when(holidayQueryPort.existsByDateAndIdNot(newDate, 5L)).thenReturn(false);
        when(holidayCommandPort.update(5L, newDate, "Lễ mới", 4))
                .thenReturn(new HolidayRecord(5L, newDate, "Lễ mới", 4));

        UpdateHolidayCommand command = new UpdateHolidayCommand(5L, newDate, "Lễ mới", 4);
        HolidayResult result = service.updateHoliday(command);

        assertNotNull(result);
        assertEquals(4, result.workingHoursDeducted());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals("UPDATE_HOLIDAY", audit.getAction());
        assertEquals("holidays", audit.getTableName());
        assertEquals(5L, audit.getRecordId());
        assertTrue(audit.getOldValue().contains("Lễ cũ"));
        assertTrue(audit.getNewValue().contains("Lễ mới"));
    }

    @Test
    @DisplayName("Cập nhật ngày lễ không tồn tại -> Ném HolidayNotFoundException")
    void updateHoliday_NotFound_ThrowsException() {
        when(authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE)).thenReturn(ACTOR_USER_ID);
        when(holidayQueryPort.findById(99L)).thenReturn(Optional.empty());

        UpdateHolidayCommand command = new UpdateHolidayCommand(99L, LocalDate.of(2026, 9, 2), "Test", 8);

        assertThrows(HolidayNotFoundException.class, () -> service.updateHoliday(command));
    }

    @Test
    @DisplayName("Xóa ngày lễ thành công và lưu Audit Log")
    void deleteHoliday_Success_PersistsAuditLog() {
        when(authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE)).thenReturn(ACTOR_USER_ID);
        LocalDate date = LocalDate.of(2026, 9, 2);
        HolidayRecord existing = new HolidayRecord(10L, date, "Quốc khánh", 8);
        when(holidayQueryPort.findById(10L)).thenReturn(Optional.of(existing));

        service.deleteHoliday(10L);

        verify(holidayCommandPort).deleteById(10L);
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertEquals("DELETE_HOLIDAY", audit.getAction());
        assertEquals("holidays", audit.getTableName());
        assertEquals(10L, audit.getRecordId());
        assertTrue(audit.getOldValue().contains("Quốc khánh"));
        assertNull(audit.getNewValue());
    }

    @Test
    @DisplayName("TC-03: Người dùng không có quyền quản lý lịch -> Ném PermissionDeniedException")
    void unauthorizedUser_ThrowsPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.WORKING_CALENDAR_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.WORKING_CALENDAR_MANAGE));

        CreateHolidayCommand command = new CreateHolidayCommand(LocalDate.of(2026, 9, 2), "Quốc khánh", 8);

        assertThrows(PermissionDeniedException.class, () -> service.createHoliday(command));
        verify(holidayCommandPort, never()).create(any(), any(), anyInt());
    }
}
