package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.inbound.unavailability.CancelUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.SaveUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException;
import com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityDeclarationNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityPolicy;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * NCL-13-CN-003: Xử lý hủy khai báo thời gian không sẵn sàng (TC-01, TC-03, TC-04, tuân thủ QTN-24).
 */
public class CancelUnavailabilityDeclarationService implements CancelUnavailabilityDeclarationUseCase {

    private final LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private final SaveUnavailabilityDeclarationPort saveUnavailabilityPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveWeeklyProjectAllocationPort saveAllocationPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;

    public CancelUnavailabilityDeclarationService(
            LoadUnavailabilityDeclarationPort loadUnavailabilityPort,
            SaveUnavailabilityDeclarationPort saveUnavailabilityPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort
    ) {
        this.loadUnavailabilityPort = Objects.requireNonNull(loadUnavailabilityPort, "loadUnavailabilityPort must not be null");
        this.saveUnavailabilityPort = Objects.requireNonNull(saveUnavailabilityPort, "saveUnavailabilityPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "saveAuditLogPort must not be null");
        this.loadWeeklyAvailabilityPort = loadWeeklyAvailabilityPort;
        this.saveWeeklyAvailabilityPort = saveWeeklyAvailabilityPort;
        this.loadAllocationPort = loadAllocationPort;
        this.saveAllocationPort = saveAllocationPort;
        this.loadHolidaysPort = loadHolidaysPort;
        this.loadApprovedLeavesPort = loadApprovedLeavesPort;
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
    }

    @Override
    public UnavailabilityDeclarationResult cancel(Long declarationId) {
        Long currentUserId = authorizationService.require(PermissionCode.UNAVAILABILITY_DECLARE);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        UnavailabilityDeclaration declaration = loadUnavailabilityPort.findByIdForUpdate(declarationId)
                .orElseThrow(() -> new UnavailabilityDeclarationNotFoundException(
                        "Không tìm thấy khai báo thời gian không sẵn sàng với mã: " + declarationId));

        Employee employee = loadEmployeePort.findById(new EmployeeId(declaration.getEmployeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên: " + declaration.getEmployeeId()));

        boolean isSelf = currentUser.getIdValue() != null && currentUser.getIdValue().equals(employee.getUserIdValue());
        boolean isCompanyScope = currentUser.getDataScope() == DataScope.COMPANY;

        if (!isSelf && !isCompanyScope) {
            saveAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "ACCESS_DENIED_CANCEL_UNAVAILABILITY",
                    "unavailability_declarations",
                    declarationId,
                    null,
                    "user_id=" + currentUserId + "; target_employee_id=" + declaration.getEmployeeId()
            ));
            throw new PermissionDeniedException(PermissionCode.UNAVAILABILITY_DECLARE);
        }

        boolean wasApproved = declaration.isApproved();

        // Không cho phép hủy khai báo đã duyệt nếu ngày bắt đầu đã qua
        if (wasApproved && declaration.getStartDate().isBefore(LocalDate.now())) {
            throw new InvalidUnavailabilityPeriodException("Không thể hủy khai báo đã được duyệt trong quá khứ");
        }

        declaration.cancel();
        UnavailabilityDeclaration saved = saveUnavailabilityPort.save(declaration);

        // Nếu đơn từng được duyệt, cần hoàn trả capacity tuần (tính lại dựa trên các đơn APPROVED còn lại)
        if (wasApproved) {
            Set<DayOfWeek> workingDays = resolveWorkingDays();
            Set<YearWeek> affectedWeeks = extractAffectedWeeks(declaration.getStartDate(), declaration.getEndDate());
            recalculateCapacityAndAllocationOverload(employee, affectedWeeks, workingDays, currentUserId);
        }

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "CANCEL_UNAVAILABILITY",
                "unavailability_declarations",
                saved.getId(),
                null,
                String.format("Hủy khai báo thời gian không sẵn sàng #%d của nhân viên #%d (%s đến %s)",
                        saved.getId(), saved.getEmployeeId(), saved.getStartDate(), saved.getEndDate())
        ));

        return UnavailabilityDeclarationResult.fromDomain(saved);
    }

    private void recalculateCapacityAndAllocationOverload(
            Employee employee,
            Set<YearWeek> affectedWeeks,
            Set<DayOfWeek> workingDays,
            Long currentUserId
    ) {
        if (loadWeeklyAvailabilityPort == null || saveWeeklyAvailabilityPort == null) {
            return;
        }

        int standardHoursPerWeek = (employee.getStandardHoursPerWeek() != null)
                ? employee.getStandardHoursPerWeek()
                : 40;

        for (YearWeek yw : affectedWeeks) {
            // Lấy các đơn APPROVED còn lại (đơn vừa hủy có status CANCELLED nên không được trả về)
            List<UnavailabilityDeclaration> remainingApproved = loadUnavailabilityPort.findApprovedByEmployeeIdAndDateRange(
                    employee.getIdValue(), yw.getStartDate(), yw.getEndDate());

            BigDecimal totalRemainingHours = BigDecimal.ZERO;
            if (remainingApproved != null) {
                for (UnavailabilityDeclaration decl : remainingApproved) {
                    BigDecimal hours = UnavailabilityPolicy.calculateHoursInWindow(
                            decl.getStartDate(),
                            decl.getEndDate(),
                            decl.getTotalHoursDeducted(),
                            yw.getStartDate(),
                            yw.getEndDate(),
                            workingDays
                    );
                    totalRemainingHours = totalRemainingHours.add(hours);
                }
            }

            int holidayHours = 0;
            if (loadHolidaysPort != null) {
                List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(yw.getStartDate(), yw.getEndDate());
                holidayHours = WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays);
            }

            BigDecimal approvedLeaveHours = BigDecimal.ZERO;
            if (loadApprovedLeavesPort != null) {
                approvedLeaveHours = loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(
                        employee.getIdValue(), yw.getStartDate(), yw.getEndDate());
            }

            Optional<WeeklyAvailability> existingOpt = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(
                    employee.getIdValue(), yw);

            int effectiveStandardHours = existingOpt.map(WeeklyAvailability::getStandardHours).orElse(standardHoursPerWeek);

            BigDecimal baseNet = WeeklyAvailabilityPolicy.calculateNetAvailableHours(
                    effectiveStandardHours, holidayHours, approvedLeaveHours);

            BigDecimal finalNetAvailable = baseNet.subtract(totalRemainingHours);
            if (finalNetAvailable.compareTo(BigDecimal.ZERO) < 0) {
                finalNetAvailable = BigDecimal.ZERO;
            }
            finalNetAvailable = finalNetAvailable.setScale(2, RoundingMode.HALF_UP);

            WeeklyAvailability availability = new WeeklyAvailability(
                    existingOpt.map(WeeklyAvailability::getId).orElse(null),
                    employee.getIdValue(),
                    yw,
                    effectiveStandardHours,
                    holidayHours,
                    approvedLeaveHours,
                    finalNetAvailable,
                    existingOpt.map(WeeklyAvailability::getVersion).orElse(0L)
            );

            saveWeeklyAvailabilityPort.save(availability);

            // QTN-24: Đánh giá lại cờ quá tải cho tuần này sau khi phục hồi capacity
            recalculateOverloadForWeek(employee.getIdValue(), yw, finalNetAvailable, currentUserId);
        }
    }

    private void recalculateOverloadForWeek(Long employeeId, YearWeek yearWeek, BigDecimal netAvailableHours, Long currentUserId) {
        if (loadAllocationPort == null || saveAllocationPort == null) {
            return;
        }

        List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek);
        if (allocations == null || allocations.isEmpty()) {
            return;
        }

        BigDecimal totalAllocated = allocations.stream()
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean isOverloaded = WeeklyCapacityMatrixPolicy.isOverloaded(totalAllocated, netAvailableHours);

        for (WeeklyProjectAllocation alloc : allocations) {
            boolean statusChanged = false;
            if (isOverloaded && !alloc.isOverloaded()) {
                alloc.markOverloaded(
                        "Phát sinh quá tải do điều chỉnh capacity tuần",
                        currentUserId,
                        LocalDateTime.now()
                );
                statusChanged = true;
            } else if (!isOverloaded && alloc.isOverloaded()) {
                alloc.clearOverload();
                statusChanged = true;
            }

            if (statusChanged) {
                saveAllocationPort.save(alloc);
            }
        }
    }

    private Set<YearWeek> extractAffectedWeeks(LocalDate startDate, LocalDate endDate) {
        Set<YearWeek> weeks = new LinkedHashSet<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            weeks.add(YearWeek.from(curr));
            curr = curr.plusDays(1);
        }
        return weeks;
    }

    private Set<DayOfWeek> resolveWorkingDays() {
        if (loadWorkingCalendarPort != null) {
            com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();
            if (calendar != null && calendar.getWorkingDays() != null && !calendar.getWorkingDays().isEmpty()) {
                return calendar.getWorkingDays();
            }
        }
        return UnavailabilityPolicy.DEFAULT_WORKING_DAYS;
    }
}
