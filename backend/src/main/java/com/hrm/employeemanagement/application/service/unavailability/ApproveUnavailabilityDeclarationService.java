package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.ApproveUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.inbound.unavailability.ApproveUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.SaveUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityConflictException;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * NCL-13-CN-003: Phê duyệt thời gian không sẵn sàng (TC-01, TC-02, TC-04, tuân thủ QTN-24).
 */
public class ApproveUnavailabilityDeclarationService implements ApproveUnavailabilityDeclarationUseCase {

    private final LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private final SaveUnavailabilityDeclarationPort saveUnavailabilityPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final UnavailabilityDataScopeValidator dataScopeValidator;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveWeeklyProjectAllocationPort saveAllocationPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;

    public ApproveUnavailabilityDeclarationService(
            LoadUnavailabilityDeclarationPort loadUnavailabilityPort,
            SaveUnavailabilityDeclarationPort saveUnavailabilityPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
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
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.dataScopeValidator = new UnavailabilityDataScopeValidator(loadOrgUnitPort);
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
    public UnavailabilityDeclarationResult approve(ApproveUnavailabilityCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        UnavailabilityDeclaration declaration = loadUnavailabilityPort.findByIdForUpdate(command.declarationId())
                .orElseThrow(() -> new UnavailabilityDeclarationNotFoundException(
                        "Không tìm thấy khai báo thời gian không sẵn sàng với mã: " + command.declarationId()));

        Employee employee = loadEmployeePort.findById(new EmployeeId(declaration.getEmployeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên: " + declaration.getEmployeeId()));

        // Kiểm tra Data Scope của người duyệt
        requireEmployeeInScope(currentUser, employee);

        Set<DayOfWeek> workingDays = resolveWorkingDays();
        Set<YearWeek> affectedWeeks = extractAffectedWeeks(declaration.getStartDate(), declaration.getEndDate());

        // TC-02: Kiểm tra xung đột với phân bổ hiện có
        List<WeeklyProjectAllocation> conflictingAllocations = findConflictingAllocations(employee.getIdValue(), affectedWeeks);
        if (!conflictingAllocations.isEmpty() && !command.confirmConflictWarning()) {
            throw new UnavailabilityConflictException(
                    String.format("Khoảng thời gian khai báo trùng với %d phân bổ dự án đã có trong tuần. Vui lòng xác nhận cảnh báo xung đột trước khi phê duyệt.",
                            conflictingAllocations.size()));
        }

        // Thực thi chuyển trạng thái Domain
        declaration.approve(currentUserId, command.approverComment());
        UnavailabilityDeclaration saved = saveUnavailabilityPort.save(declaration);

        // TC-01 & QTN-24: Trừ giờ khả dụng tuần và cập nhật cảnh báo quá tải
        recalculateCapacityAndAllocationOverload(saved, employee, affectedWeeks, workingDays, currentUserId);

        // TC-04: Ghi nhật ký kiểm toán (Audit Log)
        String auditDesc = String.format("Phê duyệt khai báo thời gian không sẵn sàng #%d của nhân viên #%d (%s đến %s). Lý do: %s. Khấu trừ: %s giờ. Ghi chú: %s",
                saved.getId(),
                saved.getEmployeeId(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getReasonType(),
                saved.getTotalHoursDeducted(),
                command.approverComment() != null ? command.approverComment() : "Không có");

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "APPROVE_UNAVAILABILITY",
                "unavailability_declarations",
                saved.getId(),
                null,
                auditDesc
        ));

        return UnavailabilityDeclarationResult.fromDomain(saved);
    }

    private List<WeeklyProjectAllocation> findConflictingAllocations(Long employeeId, Set<YearWeek> affectedWeeks) {
        if (loadAllocationPort == null) {
            return List.of();
        }
        List<WeeklyProjectAllocation> conflicts = new ArrayList<>();
        for (YearWeek yw : affectedWeeks) {
            List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployee(employeeId, yw);
            if (allocations != null) {
                for (WeeklyProjectAllocation alloc : allocations) {
                    if (alloc.getAllocatedHours() != null && alloc.getAllocatedHours().compareTo(BigDecimal.ZERO) > 0) {
                        conflicts.add(alloc);
                    }
                }
            }
        }
        return conflicts;
    }

    private void recalculateCapacityAndAllocationOverload(
            UnavailabilityDeclaration declaration,
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
            // TC-01: Lấy tất cả các khai báo APPROVED của nhân viên trong tuần này để cộng dồn chính xác
            List<UnavailabilityDeclaration> approvedInWeek = loadUnavailabilityPort.findApprovedByEmployeeIdAndDateRange(
                    employee.getIdValue(), yw.getStartDate(), yw.getEndDate());

            BigDecimal totalUnavailHoursInWeek = BigDecimal.ZERO;
            if (approvedInWeek != null) {
                for (UnavailabilityDeclaration decl : approvedInWeek) {
                    BigDecimal hours = UnavailabilityPolicy.calculateHoursInWindow(
                            decl.getStartDate(),
                            decl.getEndDate(),
                            decl.getTotalHoursDeducted(),
                            yw.getStartDate(),
                            yw.getEndDate(),
                            workingDays
                    );
                    totalUnavailHoursInWeek = totalUnavailHoursInWeek.add(hours);
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

            // TC-01: Trừ tiếp tổng số giờ không sẵn sàng đã duyệt vào giờ khả dụng của tuần
            BigDecimal finalNetAvailable = baseNet.subtract(totalUnavailHoursInWeek);
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

            // QTN-24: KHÔNG SỬA KẾ HOẠCH PHÂN BỔ - Giữ nguyên số giờ phân bổ, chỉ cập nhật cờ quá tải nếu vượt
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
                        "Phát sinh quá tải do thời gian không sẵn sàng được duyệt trong tuần",
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

    private void requireEmployeeInScope(User currentUser, Employee employee) {
        dataScopeValidator.requireEmployeeInScope(currentUser, employee, PermissionCode.UNAVAILABILITY_APPROVE);
    }
}
