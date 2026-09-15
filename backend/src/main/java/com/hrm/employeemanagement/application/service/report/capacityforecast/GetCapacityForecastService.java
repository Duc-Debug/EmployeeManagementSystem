package com.hrm.employeemanagement.application.service.report.capacityforecast;

import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastQuery;
import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastResult;
import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastResult.*;
import com.hrm.employeemanagement.application.port.inbound.report.capacityforecast.GetCapacityForecastUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.reservation.LoadResourceReservationPort;
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
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.reservation.ResourceReservation;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application Service thực thi Use Case Báo cáo dự báo năng lực các tuần tới (NCL-10-CN-004).
 */
public class GetCapacityForecastService implements GetCapacityForecastUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final LoadResourceReservationPort loadReservationPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public GetCapacityForecastService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadResourceReservationPort loadReservationPort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "LoadHolidaysPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "LoadApprovedLeavesPort must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.loadReservationPort = loadReservationPort;
        this.saveAuditLogPort = saveAuditLogPort;
    }

    @Override
    public CapacityForecastResult execute(CapacityForecastQuery query) {
        // 1. Phân quyền: Kiểm tra CAPACITY_FORECAST_REPORT_READ
        // AuthorizationService owns missing-permission auditing. Do not catch and audit
        // the same denial here, otherwise one request creates two audit records.
        Long currentUserId = authorizationService.require(PermissionCode.CAPACITY_FORECAST_REPORT_READ);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 2. Data Scope Validation
        Long effectiveOrgUnitId;
        try {
            switch (currentUser.getDataScope()) {
                case COMPANY -> effectiveOrgUnitId = query != null ? query.orgUnitId() : null;
                case ORGANIZATION_BRANCH -> {
                    if (currentUser.getScopeOrgUnitId() == null) {
                        throw new PermissionDeniedException(PermissionCode.CAPACITY_FORECAST_REPORT_READ);
                    }
                    if (query != null && query.orgUnitId() != null) {
                        boolean inScope = loadOrgUnitPort.existsInOrgUnitBranch(query.orgUnitId(), currentUser.getScopeOrgUnitId());
                        if (!inScope) {
                            throw new PermissionDeniedException(PermissionCode.CAPACITY_FORECAST_REPORT_READ);
                        }
                        effectiveOrgUnitId = query.orgUnitId();
                    } else {
                        effectiveOrgUnitId = currentUser.getScopeOrgUnitId();
                    }
                }
                default -> throw new PermissionDeniedException(PermissionCode.CAPACITY_FORECAST_REPORT_READ);
            }
        } catch (PermissionDeniedException e) {
            recordDeniedAuditLog(currentUserId, "DATA_SCOPE_UNAUTHORIZED");
            throw e;
        }

        String orgUnitName = "Toàn công ty";
        if (effectiveOrgUnitId != null) {
            OrgUnit unit = loadOrgUnitPort.findById(new OrgUnitId(effectiveOrgUnitId))
                    .orElseThrow(() -> new OrgUnitNotFoundException("Không tìm thấy bộ phận: " + effectiveOrgUnitId));
            orgUnitName = unit.getUnitName();
        }

        // 3. Validation tham số từ ngày/tuần
        Integer fromYearParam = query != null ? query.fromYear() : null;
        Integer fromWeekParam = query != null ? query.fromWeek() : null;

        int fromYear;
        int fromWeek;
        if (fromYearParam == null && fromWeekParam == null) {
            LocalDate now = LocalDate.now();
            fromYear = now.get(IsoFields.WEEK_BASED_YEAR);
            fromWeek = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        } else if (fromYearParam != null && fromWeekParam != null) {
            fromYear = fromYearParam;
            fromWeek = fromWeekParam;
        } else {
            throw new IllegalArgumentException("fromYear và fromWeek phải được cung cấp cùng nhau hoặc đều để trống (null)");
        }

        int maxWeeksInYear = YearWeek.maxWeeksInYear(fromYear);
        if (fromWeek < 1 || fromWeek > maxWeeksInYear) {
            throw new IllegalArgumentException("Tuần bắt đầu không hợp lệ: " + fromWeek + " (năm " + fromYear + " có tối đa " + maxWeeksInYear + " tuần)");
        }

        YearWeek requestedStart = YearWeek.of(fromYear, fromWeek);
        YearWeek currentWeek = YearWeek.from(LocalDate.now());
        if (requestedStart.isBefore(currentWeek)) {
            throw new IllegalArgumentException("Tuần bắt đầu phải là tuần hiện tại hoặc tuần tương lai (từ tuần T" + currentWeek.weekNumber() + "/" + currentWeek.year() + " trở đi)");
        }

        int rawDuration = (query != null && query.durationWeeks() != null) ? query.durationWeeks() : 12;
        if (rawDuration < 4 || rawDuration > 16) {
            throw new IllegalArgumentException("Số tuần dự báo phải từ 4 đến 16 tuần");
        }
        int durationWeeks = rawDuration;

        // 4. Tạo danh sách các tuần ISO liên tiếp
        List<YearWeek> targetWeeks = buildTargetWeeks(fromYear, fromWeek, durationWeeks);

        // 5. Tải danh sách nhân sự active trong scope
        List<Employee> targetEmployees = loadEmployeesInScope(effectiveOrgUnitId);

        if (targetEmployees.isEmpty()) {
            // Không có nhân sự trong phạm vi -> Trả về danh sách tuần giá trị 0
            List<WeeklyForecastItem> emptyWeeks = targetWeeks.stream()
                    .map(yw -> new WeeklyForecastItem(
                            yw.year(),
                            yw.weekNumber(),
                            yw.getStartDate(),
                            yw.getEndDate(),
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            ForecastStatus.AVAILABLE
                    ))
                    .toList();

            CapacityForecastSummary emptySummary = new CapacityForecastSummary(
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    0
            );

            recordSuccessAuditLog(currentUserId, orgUnitName, fromYear, fromWeek, durationWeeks);

            return new CapacityForecastResult(
                    effectiveOrgUnitId,
                    orgUnitName,
                    fromYear,
                    fromWeek,
                    durationWeeks,
                    emptyWeeks,
                    emptySummary,
                    LocalDateTime.now()
            );
        }

        List<Long> employeeIds = targetEmployees.stream().map(Employee::getIdValue).toList();

        // 6. Batch load dữ liệu
        // Batch load phân bổ chính thức
        List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<String, BigDecimal> allocationMap = allocations.stream()
                .collect(Collectors.groupingBy(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        Collectors.reducing(BigDecimal.ZERO, WeeklyProjectAllocation::getAllocatedHours, BigDecimal::add)
                ));

        // Batch load tính khả dụng khai báo riêng
        List<WeeklyAvailability> availabilities = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<String, WeeklyAvailability> availabilityMap = availabilities.stream()
                .collect(Collectors.toMap(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        a -> a,
                        (existing, replacing) -> existing
                ));

        // Batch load nghỉ phép đã duyệt
        Map<Long, Map<YearWeek, BigDecimal>> leaveHoursMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(employeeIds, targetWeeks);

        // Batch load ngày lễ
        LocalDate minStart = targetWeeks.get(0).getStartDate();
        LocalDate maxEnd = targetWeeks.get(targetWeeks.size() - 1).getEndDate();
        List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(minStart, maxEnd);
        Set<DayOfWeek> workingDays = resolveWorkingDays();
        Map<YearWeek, Integer> holidayHoursByWeek = targetWeeks.stream()
                .collect(Collectors.toMap(
                        yw -> yw,
                        yw -> WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays)
                ));

        // Batch load giữ chỗ nguồn lực ACTIVE
        Map<String, BigDecimal> reservationMap = Map.of();
        if (loadReservationPort != null) {
            List<ResourceReservation> activeReservations = loadReservationPort.findActiveByEmployeeIdsAndYearWeeks(employeeIds, targetWeeks);
            reservationMap = activeReservations.stream()
                    .collect(Collectors.groupingBy(
                            r -> makeKey(r.getEmployeeId(), r.getYear(), r.getWeekNumber()),
                            Collectors.reducing(BigDecimal.ZERO, ResourceReservation::getReservedHours, BigDecimal::add)
                    ));
        }

        // 7. Tổng hợp dữ liệu năng lực theo từng tuần
        List<WeeklyForecastItem> weekItems = new ArrayList<>();
        BigDecimal totalAvailable = BigDecimal.ZERO;
        BigDecimal totalCommitted = BigDecimal.ZERO;
        BigDecimal totalReserved = BigDecimal.ZERO;
        BigDecimal totalProjectedRemaining = BigDecimal.ZERO;
        int overCapacityWeeksCount = 0;

        int weekWorkingDaysCount = workingDays.isEmpty() ? 5 : workingDays.size();

        for (YearWeek yw : targetWeeks) {
            BigDecimal weekAvailableHours = BigDecimal.ZERO;
            BigDecimal weekCommittedHours = BigDecimal.ZERO;
            BigDecimal weekReservedHours = BigDecimal.ZERO;

            for (Employee emp : targetEmployees) {
                String key = makeKey(emp.getIdValue(), yw.year(), yw.weekNumber());

                WeeklyAvailability savedAvail = availabilityMap.get(key);
                BigDecimal baseAvailableHours;
                if (savedAvail != null) {
                    baseAvailableHours = savedAvail.getNetAvailableHours();
                } else {
                    int standardHours = emp.getStandardHoursPerWeek() != null ? emp.getStandardHoursPerWeek() : 40;
                    int holidayHours = holidayHoursByWeek.getOrDefault(yw, 0);
                    BigDecimal leaveHours = leaveHoursMap.getOrDefault(emp.getIdValue(), Map.of()).getOrDefault(yw, BigDecimal.ZERO);
                    baseAvailableHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, leaveHours);
                }

                BigDecimal netAvailableHours = WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(
                        baseAvailableHours,
                        emp.getContractEndDate(),
                        yw.getStartDate(),
                        yw.getEndDate(),
                        weekWorkingDaysCount
                );

                BigDecimal empAllocated = allocationMap.getOrDefault(key, BigDecimal.ZERO);
                BigDecimal empReserved = reservationMap.getOrDefault(key, BigDecimal.ZERO);

                weekAvailableHours = weekAvailableHours.add(netAvailableHours);
                weekCommittedHours = weekCommittedHours.add(empAllocated);
                weekReservedHours = weekReservedHours.add(empReserved);
            }

            weekAvailableHours = weekAvailableHours.setScale(1, RoundingMode.HALF_UP);
            weekCommittedHours = weekCommittedHours.setScale(1, RoundingMode.HALF_UP);
            weekReservedHours = weekReservedHours.setScale(1, RoundingMode.HALF_UP);

            BigDecimal committedRemaining = weekAvailableHours.subtract(weekCommittedHours).setScale(1, RoundingMode.HALF_UP);
            BigDecimal projectedRemaining = weekAvailableHours.subtract(weekCommittedHours).subtract(weekReservedHours).setScale(1, RoundingMode.HALF_UP);

            BigDecimal committedUtilization = calculateUtilization(weekCommittedHours, weekAvailableHours);
            BigDecimal projectedUtilization = calculateUtilization(weekCommittedHours.add(weekReservedHours), weekAvailableHours);

            ForecastStatus status;
            if (projectedRemaining.compareTo(BigDecimal.ZERO) < 0
                    || (weekAvailableHours.compareTo(BigDecimal.ZERO) <= 0 && weekCommittedHours.add(weekReservedHours).compareTo(BigDecimal.ZERO) > 0)
                    || (projectedUtilization != null && projectedUtilization.compareTo(BigDecimal.valueOf(100.0)) > 0)) {
                status = ForecastStatus.OVER_CAPACITY;
                overCapacityWeeksCount++;
            } else if (projectedUtilization != null && projectedUtilization.compareTo(BigDecimal.valueOf(80.0)) >= 0) {
                status = ForecastStatus.NEAR_FULL;
            } else {
                status = ForecastStatus.AVAILABLE;
            }

            weekItems.add(new WeeklyForecastItem(
                    yw.year(),
                    yw.weekNumber(),
                    yw.getStartDate(),
                    yw.getEndDate(),
                    weekAvailableHours,
                    weekCommittedHours,
                    weekReservedHours,
                    committedRemaining,
                    projectedRemaining,
                    committedUtilization,
                    projectedUtilization,
                    status
            ));

            totalAvailable = totalAvailable.add(weekAvailableHours);
            totalCommitted = totalCommitted.add(weekCommittedHours);
            totalReserved = totalReserved.add(weekReservedHours);
            totalProjectedRemaining = totalProjectedRemaining.add(projectedRemaining);
        }

        CapacityForecastSummary summary = new CapacityForecastSummary(
                totalAvailable.setScale(1, RoundingMode.HALF_UP),
                totalCommitted.setScale(1, RoundingMode.HALF_UP),
                totalReserved.setScale(1, RoundingMode.HALF_UP),
                totalProjectedRemaining.setScale(1, RoundingMode.HALF_UP),
                overCapacityWeeksCount
        );

        recordSuccessAuditLog(currentUserId, orgUnitName, fromYear, fromWeek, durationWeeks);

        return new CapacityForecastResult(
                effectiveOrgUnitId,
                orgUnitName,
                fromYear,
                fromWeek,
                durationWeeks,
                weekItems,
                summary,
                LocalDateTime.now()
        );
    }

    private BigDecimal calculateUtilization(BigDecimal totalHours, BigDecimal availableHours) {
        if (availableHours.compareTo(BigDecimal.ZERO) <= 0) {
            if (totalHours.compareTo(BigDecimal.ZERO) > 0) {
                return null; // Không xác định / Vô cực
            }
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return totalHours.multiply(BigDecimal.valueOf(100))
                .divide(availableHours, 1, RoundingMode.HALF_UP);
    }

    private List<Long> resolveScopeBranchOrgUnitIds(Long effectiveOrgUnitId) {
        if (effectiveOrgUnitId != null) {
            Optional<OrgUnit> unitOpt = loadOrgUnitPort.findById(new OrgUnitId(effectiveOrgUnitId));
            if (unitOpt.isPresent()) {
                List<OrgUnit> subTree = loadOrgUnitPort.findSubTree(unitOpt.get().getTreePath());
                return subTree.stream().map(u -> u.getId().getValue()).toList();
            }
            return List.of(effectiveOrgUnitId);
        }
        return null;
    }

    private List<Employee> loadEmployeesInScope(Long effectiveOrgUnitId) {
        List<Long> branchIds = resolveScopeBranchOrgUnitIds(effectiveOrgUnitId);
        if (branchIds != null) {
            return loadEmployeePort.findActiveByOrgUnitIds(branchIds);
        }
        return loadEmployeePort.findAllActive();
    }

    private List<YearWeek> buildTargetWeeks(int fromYear, int fromWeek, int durationWeeks) {
        List<YearWeek> list = new ArrayList<>(durationWeeks);
        YearWeek start = YearWeek.of(fromYear, fromWeek);
        LocalDate monday = start.getStartDate();
        for (int i = 0; i < durationWeeks; i++) {
            int y = monday.get(IsoFields.WEEK_BASED_YEAR);
            int w = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            list.add(YearWeek.of(y, w));
            monday = monday.plusWeeks(1);
        }
        return list;
    }

    private String makeKey(Long employeeId, int year, int weekNumber) {
        return employeeId + "_" + year + "_" + weekNumber;
    }

    private Set<DayOfWeek> resolveWorkingDays() {
        if (loadWorkingCalendarPort != null) {
            CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();
            if (calendar != null && calendar.getWorkingDays() != null && !calendar.getWorkingDays().isEmpty()) {
                return calendar.getWorkingDays();
            }
        }
        return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
    }

    private void recordDeniedAuditLog(Long userId, String reason) {
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.createChange(
                    userId,
                    "ACCESS_DENIED_CAPACITY_FORECAST_REPORT",
                    "capacity_forecast_report",
                    null,
                    null,
                    "user_id=" + (userId != null ? userId : "ANONYMOUS") + ";reason=" + reason
            ));
        }
    }

    private void recordSuccessAuditLog(Long userId, String orgUnitName, int fromYear, int fromWeek, int durationWeeks) {
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.createChange(
                    userId,
                    "CAPACITY_FORECAST_REPORT_VIEWED",
                    "capacity_forecast_report",
                    null,
                    null,
                    "Xem báo cáo dự báo năng lực " + durationWeeks + " tuần từ T" + fromWeek + "/" + fromYear + " cho đơn vị " + orgUnitName
            ));
        }
    }
}
