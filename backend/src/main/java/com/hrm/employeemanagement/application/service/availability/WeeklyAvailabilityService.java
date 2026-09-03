package com.hrm.employeemanagement.application.service.availability;

import com.hrm.employeemanagement.application.dto.availability.CalculateWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.dto.availability.DeclareWeeklyAvailabilityCommand;
import com.hrm.employeemanagement.application.dto.availability.WeeklyAvailabilityResult;
import com.hrm.employeemanagement.application.port.inbound.availability.CalculateWeeklyCapacityUseCase;
import com.hrm.employeemanagement.application.port.inbound.availability.DeclareWeeklyAvailabilityUseCase;
import com.hrm.employeemanagement.application.port.inbound.availability.GetWeeklyAvailabilityUseCase;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
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
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WeeklyAvailabilityService implements DeclareWeeklyAvailabilityUseCase,
        CalculateWeeklyCapacityUseCase, GetWeeklyAvailabilityUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final AuthorizationService authorizationService;

    public WeeklyAvailabilityService(LoadEmployeePort loadEmployeePort,
                                    LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
                                    SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort,
                                    LoadHolidaysPort loadHolidaysPort,
                                    LoadApprovedLeavesPort loadApprovedLeavesPort,
                                    SaveAuditLogPort saveAuditLogPort,
                                    LoadUserPort loadUserPort,
                                    LoadOrgUnitPort loadOrgUnitPort,
                                    AuthorizationService authorizationService) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.saveWeeklyAvailabilityPort = Objects.requireNonNull(saveWeeklyAvailabilityPort, "SaveWeeklyAvailabilityPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "LoadHolidaysPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "LoadApprovedLeavesPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public WeeklyAvailabilityResult execute(DeclareWeeklyAvailabilityCommand command) {
        User currentUser = requireCurrentUser(PermissionCode.EMPLOYEE_UPDATE);

        Employee employee = loadEmployeePort.findById(new EmployeeId(command.employeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + command.employeeId()));

        requireEmployeeInScope(currentUser, employee, PermissionCode.EMPLOYEE_UPDATE);

        YearWeek yearWeek = YearWeek.of(command.year(), command.weekNumber());
        WeeklyAvailabilityPolicy.validateStandardHours(command.standardHours());

        List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(yearWeek.getStartDate(), yearWeek.getEndDate());
        int holidayHours = WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yearWeek, holidays);

        Long resolvedEmployeeId = employee.getIdValue() != null ? employee.getIdValue() : command.employeeId();
        BigDecimal approvedLeaveHours = loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(
                resolvedEmployeeId, yearWeek.getStartDate(), yearWeek.getEndDate());

        Optional<WeeklyAvailability> existing = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(
                resolvedEmployeeId, yearWeek);

        Integer oldHours = existing.map(WeeklyAvailability::getStandardHours).orElse(null);

        WeeklyAvailability availability;
        if (existing.isPresent()) {
            availability = existing.get();
            availability.updateStandardHours(command.standardHours());
        } else {
            availability = WeeklyAvailability.createCalculated(
                    resolvedEmployeeId, yearWeek, command.standardHours(), holidayHours, approvedLeaveHours);
        }

        WeeklyAvailability saved = saveWeeklyAvailabilityPort.save(availability);

        // NCL-02-CN-003-TC-04: Ghi nhận nhật ký kiểm toán (Audit Log) khi thay đổi giờ chuẩn tuần
        saveAuditLogPort.save(AuditLog.createChange(
                currentUser.getIdValue(),
                "DECLARE_WEEKLY_AVAILABILITY",
                "employee_weekly_availabilities",
                saved.getId(),
                oldHours != null ? String.valueOf(oldHours) : null,
                String.valueOf(saved.getStandardHours())
        ));

        return WeeklyAvailabilityResult.fromDomain(saved);
    }

    @Override
    public WeeklyAvailabilityResult calculate(CalculateWeeklyCapacityQuery query) {
        User currentUser = requireCurrentUser(PermissionCode.EMPLOYEE_READ);

        Employee employee = loadEmployeePort.findById(new EmployeeId(query.employeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + query.employeeId()));

        requireEmployeeInScope(currentUser, employee, PermissionCode.EMPLOYEE_READ);

        YearWeek yearWeek = YearWeek.of(query.year(), query.weekNumber());

        Long resolvedEmployeeId = employee.getIdValue() != null ? employee.getIdValue() : query.employeeId();
        Optional<WeeklyAvailability> existing = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(
                resolvedEmployeeId, yearWeek);

        int standardHours = existing.map(WeeklyAvailability::getStandardHours)
                .orElseGet(() -> employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40);

        List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(yearWeek.getStartDate(), yearWeek.getEndDate());
        int holidayHours = WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yearWeek, holidays);

        BigDecimal approvedLeaveHours = loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(
                resolvedEmployeeId, yearWeek.getStartDate(), yearWeek.getEndDate());

        BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, approvedLeaveHours);

        WeeklyAvailability availability = new WeeklyAvailability(
                existing.map(WeeklyAvailability::getId).orElse(null),
                resolvedEmployeeId,
                yearWeek,
                standardHours,
                holidayHours,
                approvedLeaveHours,
                netHours,
                existing.map(WeeklyAvailability::getVersion).orElse(0L)
        );

        return WeeklyAvailabilityResult.fromDomain(availability);
    }

    @Override
    public WeeklyAvailabilityResult getAvailability(Long employeeId, Integer year, Integer weekNumber) {
        return calculate(new CalculateWeeklyCapacityQuery(employeeId, year, weekNumber));
    }

    private User requireCurrentUser(PermissionCode permission) {
        Long currentUserId = authorizationService.require(permission);
        return loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException(
                        "Không tìm thấy người dùng hiện tại với ID: " + currentUserId));
    }

    private void requireEmployeeInScope(User currentUser, Employee employee, PermissionCode permission) {
        boolean allowed = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> currentUser.getIdValue() != null && currentUser.getIdValue().equals(employee.getUserIdValue());
            case ORGANIZATION_BRANCH -> employee.getOrgUnitId() != null
                    && currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(
                            employee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
        };
        if (!allowed) {
            throw new PermissionDeniedException(permission);
        }
    }
}
