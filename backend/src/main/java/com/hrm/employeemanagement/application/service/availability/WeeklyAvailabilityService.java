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
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final AuthorizationService authorizationService;

    public WeeklyAvailabilityService(LoadEmployeePort loadEmployeePort,
                                   LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
                                   SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort,
                                   LoadHolidaysPort loadHolidaysPort,
                                   LoadApprovedLeavesPort loadApprovedLeavesPort,
                                   AuthorizationService authorizationService) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.saveWeeklyAvailabilityPort = Objects.requireNonNull(saveWeeklyAvailabilityPort, "SaveWeeklyAvailabilityPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "LoadHolidaysPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "LoadApprovedLeavesPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public WeeklyAvailabilityResult execute(DeclareWeeklyAvailabilityCommand command) {
        authorizationService.require(PermissionCode.EMPLOYEE_UPDATE);

        Employee employee = loadEmployeePort.findById(new EmployeeId(command.employeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + command.employeeId()));

        YearWeek yearWeek = YearWeek.of(command.year(), command.weekNumber());
        WeeklyAvailabilityPolicy.validateStandardHours(command.standardHours());

        List<LocalDate> holidayDates = loadHolidaysPort.getHolidayDatesBetween(yearWeek.getStartDate(), yearWeek.getEndDate());
        int holidayHours = WeeklyAvailabilityPolicy.calculateHolidayHours(yearWeek, holidayDates);

        Long resolvedEmployeeId = employee.getIdValue() != null ? employee.getIdValue() : command.employeeId();
        BigDecimal approvedLeaveHours = loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(
                resolvedEmployeeId, yearWeek.getStartDate(), yearWeek.getEndDate());

        Optional<WeeklyAvailability> existing = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(
                resolvedEmployeeId, yearWeek);

        WeeklyAvailability availability;
        if (existing.isPresent()) {
            availability = existing.get();
            availability.updateStandardHours(command.standardHours());
        } else {
            availability = WeeklyAvailability.createCalculated(
                    resolvedEmployeeId, yearWeek, command.standardHours(), holidayHours, approvedLeaveHours);
        }

        WeeklyAvailability saved = saveWeeklyAvailabilityPort.save(availability);
        return WeeklyAvailabilityResult.fromDomain(saved);
    }

    @Override
    public WeeklyAvailabilityResult calculate(CalculateWeeklyCapacityQuery query) {
        authorizationService.require(PermissionCode.EMPLOYEE_READ);

        Employee employee = loadEmployeePort.findById(new EmployeeId(query.employeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + query.employeeId()));

        YearWeek yearWeek = YearWeek.of(query.year(), query.weekNumber());

        Long resolvedEmployeeId = employee.getIdValue() != null ? employee.getIdValue() : query.employeeId();
        Optional<WeeklyAvailability> existing = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(
                resolvedEmployeeId, yearWeek);

        int standardHours = existing.map(WeeklyAvailability::getStandardHours)
                .orElseGet(() -> employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40);

        List<LocalDate> holidayDates = loadHolidaysPort.getHolidayDatesBetween(yearWeek.getStartDate(), yearWeek.getEndDate());
        int holidayHours = WeeklyAvailabilityPolicy.calculateHolidayHours(yearWeek, holidayDates);

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
                netHours
        );

        return WeeklyAvailabilityResult.fromDomain(availability);
    }

    @Override
    public WeeklyAvailabilityResult getAvailability(Long employeeId, Integer year, Integer weekNumber) {
        return calculate(new CalculateWeeklyCapacityQuery(employeeId, year, weekNumber));
    }
}
