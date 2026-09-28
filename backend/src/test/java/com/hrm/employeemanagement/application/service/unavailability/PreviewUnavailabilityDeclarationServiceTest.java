package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityPreviewResult;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.calendar.WorkingCalendarDay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreviewUnavailabilityDeclarationService Unit Tests")
class PreviewUnavailabilityDeclarationServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private LoadWorkingCalendarPort loadWorkingCalendarPort;

    private PreviewUnavailabilityDeclarationService service;

    @BeforeEach
    void setUp() {
        service = new PreviewUnavailabilityDeclarationService(authorizationService, loadWorkingCalendarPort);
    }

    @Test
    @DisplayName("Tính toán preview thành công với CompanyWorkingCalendar mặc định (Mon-Fri)")
    void testPreviewWithDefaultCalendar() {
        when(loadWorkingCalendarPort.loadCompanyCalendar()).thenReturn(CompanyWorkingCalendar.createDefault());

        LocalDate start = LocalDate.of(2026, 9, 21); // Monday
        LocalDate end = LocalDate.of(2026, 9, 25);   // Friday

        UnavailabilityPreviewResult result = service.preview(start, end);

        assertEquals(5, result.workingDays());
        assertEquals(BigDecimal.valueOf(40.00).setScale(2), result.totalHoursDeducted());
        verify(authorizationService).requireAny(
                PermissionCode.UNAVAILABILITY_DECLARE,
                PermissionCode.UNAVAILABILITY_APPROVE,
                PermissionCode.UNAVAILABILITY_READ
        );
    }

    @Test
    @DisplayName("Tính toán preview thành công khi CompanyWorkingCalendar bao gồm Thứ 7")
    void testPreviewWithSaturdayWorking() {
        CompanyWorkingCalendar sixDayCalendar = new CompanyWorkingCalendar(List.of(
                new WorkingCalendarDay(DayOfWeek.MONDAY, true),
                new WorkingCalendarDay(DayOfWeek.TUESDAY, true),
                new WorkingCalendarDay(DayOfWeek.WEDNESDAY, true),
                new WorkingCalendarDay(DayOfWeek.THURSDAY, true),
                new WorkingCalendarDay(DayOfWeek.FRIDAY, true),
                new WorkingCalendarDay(DayOfWeek.SATURDAY, true),
                new WorkingCalendarDay(DayOfWeek.SUNDAY, false)
        ));
        when(loadWorkingCalendarPort.loadCompanyCalendar()).thenReturn(sixDayCalendar);

        LocalDate start = LocalDate.of(2026, 9, 21); // Monday
        LocalDate end = LocalDate.of(2026, 9, 26);   // Saturday (now a working day!)

        UnavailabilityPreviewResult result = service.preview(start, end);

        assertEquals(6, result.workingDays());
        assertEquals(BigDecimal.valueOf(48.00).setScale(2), result.totalHoursDeducted());
    }

    @Test
    @DisplayName("Ngày rỗng hoặc startDate sau endDate trả về 0 ngày và 0 giờ")
    void testPreviewInvalidDates() {
        UnavailabilityPreviewResult result1 = service.preview(null, LocalDate.of(2026, 9, 25));
        assertEquals(0, result1.workingDays());
        assertEquals(BigDecimal.ZERO.setScale(2), result1.totalHoursDeducted());

        UnavailabilityPreviewResult result2 = service.preview(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 20));
        assertEquals(0, result2.workingDays());
        assertEquals(BigDecimal.ZERO.setScale(2), result2.totalHoursDeducted());
    }
}
