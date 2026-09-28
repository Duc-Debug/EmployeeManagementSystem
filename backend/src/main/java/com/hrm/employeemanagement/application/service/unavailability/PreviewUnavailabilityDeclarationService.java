package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityPreviewResult;
import com.hrm.employeemanagement.application.port.inbound.unavailability.PreviewUnavailabilityUseCase;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

public class PreviewUnavailabilityDeclarationService implements PreviewUnavailabilityUseCase {

    private final AuthorizationService authorizationService;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;

    public PreviewUnavailabilityDeclarationService(
            AuthorizationService authorizationService,
            LoadWorkingCalendarPort loadWorkingCalendarPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
    }

    @Override
    public UnavailabilityPreviewResult preview(LocalDate startDate, LocalDate endDate) {
        authorizationService.requireAny(
                PermissionCode.UNAVAILABILITY_DECLARE,
                PermissionCode.UNAVAILABILITY_APPROVE,
                PermissionCode.UNAVAILABILITY_READ
        );

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return new UnavailabilityPreviewResult(
                    startDate,
                    endDate,
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }

        Set<DayOfWeek> workingDays = resolveWorkingDays();
        int workingDaysCount = UnavailabilityPolicy.countWorkingDays(startDate, endDate, workingDays);
        BigDecimal totalHours = UnavailabilityPolicy.calculateTotalHours(startDate, endDate, workingDays);

        return new UnavailabilityPreviewResult(startDate, endDate, workingDaysCount, totalHours);
    }

    private Set<DayOfWeek> resolveWorkingDays() {
        if (loadWorkingCalendarPort != null) {
            CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();
            if (calendar != null && calendar.getWorkingDays() != null && !calendar.getWorkingDays().isEmpty()) {
                return calendar.getWorkingDays();
            }
        }
        return UnavailabilityPolicy.DEFAULT_WORKING_DAYS;
    }
}
