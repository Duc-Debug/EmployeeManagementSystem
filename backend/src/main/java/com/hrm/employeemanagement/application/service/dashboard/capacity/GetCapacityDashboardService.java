package com.hrm.employeemanagement.application.service.dashboard.capacity;

import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardQuery;
import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardResult;
import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardResult.*;
import com.hrm.employeemanagement.application.port.inbound.dashboard.capacity.GetCapacityDashboardUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.LoadScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application Service thực thi nghiệp vụ Bảng điều khiển năng lực (NCL-10-CN-001).
 * Tổng hợp 5 chỉ số chính và bức tranh phân bổ năng lực cho Ban Giám Đốc và các Quản lý.
 */
public class GetCapacityDashboardService implements GetCapacityDashboardUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final LoadScheduleConflictPort loadScheduleConflictPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final LoadCapacityThresholdPort loadCapacityThresholdPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public GetCapacityDashboardService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadScheduleConflictPort loadScheduleConflictPort,
            LoadProjectPort loadProjectPort,
            LoadProjectMemberPort loadProjectMemberPort,
            LoadCapacityThresholdPort loadCapacityThresholdPort,
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
        this.loadScheduleConflictPort = loadScheduleConflictPort;
        this.loadProjectPort = loadProjectPort;
        this.loadProjectMemberPort = loadProjectMemberPort;
        this.loadCapacityThresholdPort = loadCapacityThresholdPort;
        this.saveAuditLogPort = saveAuditLogPort;
    }

    @Override
    public CapacityDashboardResult execute(CapacityDashboardQuery query) {
        // 1. Phân quyền & Kiểm soát truy cập (TC-03)
        Long currentUserId;
        try {
            currentUserId = authorizationService.require(PermissionCode.CAPACITY_DASHBOARD_READ);
        } catch (PermissionDeniedException e) {
            recordDeniedAuditLog(null, "UNAUTHORIZED_ROLE");
            throw e;
        }

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 2. Data Scope Validation
        Long effectiveOrgUnitId;
        try {
            switch (currentUser.getDataScope()) {
                case COMPANY -> effectiveOrgUnitId = query != null ? query.orgUnitId() : null;
                case ORGANIZATION_BRANCH -> {
                    if (currentUser.getScopeOrgUnitId() == null) {
                        throw new PermissionDeniedException(PermissionCode.CAPACITY_DASHBOARD_READ);
                    }
                    if (query != null && query.orgUnitId() != null) {
                        boolean inScope = loadOrgUnitPort.existsInOrgUnitBranch(query.orgUnitId(), currentUser.getScopeOrgUnitId());
                        if (!inScope) {
                            throw new PermissionDeniedException(PermissionCode.CAPACITY_DASHBOARD_READ);
                        }
                        effectiveOrgUnitId = query.orgUnitId();
                    } else {
                        effectiveOrgUnitId = currentUser.getScopeOrgUnitId();
                    }
                }
                case SELF -> {
                    if (currentUser.getRole() != null && currentUser.getRole().getCode() == RoleCode.VT_02) {
                        Long pmOrgUnitId = resolveEmployeeOrgUnitId(currentUser, currentUserId);
                        if (pmOrgUnitId == null) {
                            throw new PermissionDeniedException(PermissionCode.CAPACITY_DASHBOARD_READ);
                        }
                        if (query != null && query.orgUnitId() != null) {
                            boolean inScope = loadOrgUnitPort.existsInOrgUnitBranch(query.orgUnitId(), pmOrgUnitId);
                            if (!inScope) {
                                throw new PermissionDeniedException(PermissionCode.CAPACITY_DASHBOARD_READ);
                            }
                            effectiveOrgUnitId = query.orgUnitId();
                        } else {
                            effectiveOrgUnitId = pmOrgUnitId;
                        }
                    } else {
                        throw new PermissionDeniedException(PermissionCode.CAPACITY_DASHBOARD_READ);
                    }
                }
                default -> throw new PermissionDeniedException(PermissionCode.CAPACITY_DASHBOARD_READ);
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

        // 3. Xác định kỳ hiển thị (Mặc định 8 tuần tới - TC-01)
        int fromYear;
        int fromWeek;
        if (query != null && query.fromYear() != null && query.fromWeek() != null) {
            fromYear = query.fromYear();
            fromWeek = query.fromWeek();
        } else {
            LocalDate now = LocalDate.now();
            fromYear = now.get(IsoFields.WEEK_BASED_YEAR);
            fromWeek = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        }

        int durationWeeks = (query != null && query.durationWeeks() != null) ? query.durationWeeks() : 8;
        durationWeeks = Math.max(1, Math.min(durationWeeks, 52));

        List<YearWeek> targetWeeks = buildTargetWeeks(fromYear, fromWeek, durationWeeks);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");

        // 4. Tải danh sách nhân sự trong scope
        List<Employee> targetEmployees = loadEmployeesInScope(effectiveOrgUnitId);

        // 5. Đếm chính xác số lượng dự án hoạt động và tải danh sách tóm tắt (preview giới hạn) cho bảng điều khiển
        long activeProjectsCount = countActiveProjectsInScope(effectiveOrgUnitId);
        List<Project> activeProjectsPreview = loadActiveProjectsPreviewInScope(effectiveOrgUnitId, currentUser);

        // 6. Xử lý trường hợp dữ liệu rỗng (TC-02)
        if (targetEmployees.isEmpty()) {
            List<WeeklyCapacityDashboardItem> emptyWeekly = targetWeeks.stream()
                    .map(yw -> new WeeklyCapacityDashboardItem(
                            yw.year(),
                            yw.weekNumber(),
                            yw.getStartDate(),
                            yw.getEndDate(),
                            "T" + yw.weekNumber() + " (" + yw.getStartDate().format(dtf) + " - " + yw.getEndDate().format(dtf) + ")",
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                            0
                    ))
                    .toList();

            List<ActiveProjectSummaryItem> activeProjectItems = mapActiveProjectItems(activeProjectsPreview);

            recordSuccessAuditLog(currentUserId, orgUnitName, fromYear, fromWeek, durationWeeks);

            return new CapacityDashboardResult(
                    effectiveOrgUnitId,
                    orgUnitName,
                    fromYear,
                    fromWeek,
                    durationWeeks,
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    0,
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    0,
                    activeProjectsCount,
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    emptyWeekly,
                    List.of(),
                    List.of(),
                    List.of(),
                    activeProjectItems,
                    LocalDateTime.now()
            );
        }

        List<Long> employeeIds = targetEmployees.stream().map(Employee::getIdValue).toList();
        Map<Long, Employee> employeeMap = targetEmployees.stream().collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));

        // 7. Cấu hình ngưỡng cảnh báo tải
        BigDecimal overloadThreshold = WeeklyCapacityMatrixPolicy.DEFAULT_OVERLOAD_THRESHOLD;
        BigDecimal idleThreshold = WeeklyCapacityMatrixPolicy.UNDERUTILIZED_THRESHOLD;
        if (loadCapacityThresholdPort != null) {
            Optional<CapacityThresholdConfig> configOpt = Optional.empty();
            if (effectiveOrgUnitId != null) {
                configOpt = loadCapacityThresholdPort.findByScope(CapacityThresholdScope.ORG_UNIT, effectiveOrgUnitId);
            }
            if (configOpt.isEmpty()) {
                configOpt = loadCapacityThresholdPort.findByScope(CapacityThresholdScope.COMPANY, null);
            }
            if (configOpt.isPresent()) {
                overloadThreshold = configOpt.get().getOverloadThreshold();
                idleThreshold = configOpt.get().getIdleThreshold();
            }
        }

        // 8. Batch load dữ liệu phân bổ, khả dụng, nghỉ phép, ngày lễ
        List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<String, BigDecimal> allocationMap = allocations.stream()
                .collect(Collectors.groupingBy(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        Collectors.reducing(BigDecimal.ZERO, WeeklyProjectAllocation::getAllocatedHours, BigDecimal::add)
                ));

        List<WeeklyAvailability> availabilities = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<String, WeeklyAvailability> availabilityMap = availabilities.stream()
                .collect(Collectors.toMap(
                        a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                        a -> a,
                        (existing, replacing) -> existing
                ));

        Map<Long, Map<YearWeek, BigDecimal>> leaveHoursMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(employeeIds, targetWeeks);

        LocalDate minStart = targetWeeks.get(0).getStartDate();
        LocalDate maxEnd = targetWeeks.get(targetWeeks.size() - 1).getEndDate();
        List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(minStart, maxEnd);
        Set<DayOfWeek> workingDays = resolveWorkingDays();
        int weekWorkingDaysCount = workingDays.isEmpty() ? 5 : workingDays.size();
        Map<YearWeek, Integer> holidayHoursByWeek = targetWeeks.stream()
                .collect(Collectors.toMap(
                        yw -> yw,
                        yw -> WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays)
                ));

        // Tải tên phòng ban
        List<Long> orgUnitIds = targetEmployees.stream().map(Employee::getOrgUnitId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> orgUnitNameMap = orgUnitIds.isEmpty() ? Map.of() :
                loadOrgUnitPort.findAllByIdIn(orgUnitIds).stream()
                        .collect(Collectors.toMap(u -> u.getId().getValue(), OrgUnit::getUnitName, (e1, e2) -> e1));

        // 9. Tính toán chi tiết từng nhân sự theo tuần
        Map<YearWeek, BigDecimal> weeklyAvailableMap = new HashMap<>();
        Map<YearWeek, BigDecimal> weeklyAllocatedMap = new HashMap<>();
        Map<YearWeek, Integer> weeklyOverloadedCountMap = new HashMap<>();

        Map<Long, DepartmentCapacityAccumulator> deptMap = new HashMap<>();
        List<OverloadedEmployeeItem> overloadedEmployeesList = new ArrayList<>();
        Set<Long> overloadedEmployeeIds = new HashSet<>();

        BigDecimal totalAvailableHours = BigDecimal.ZERO;
        BigDecimal totalAllocatedHours = BigDecimal.ZERO;

        for (Employee emp : targetEmployees) {
            BigDecimal empTotalAllocated = BigDecimal.ZERO;
            BigDecimal empTotalAvailable = BigDecimal.ZERO;
            int empOverloadedWeeksCount = 0;
            Long empDeptId = emp.getOrgUnitId() != null ? emp.getOrgUnitId() : 0L;
            String empDeptName = emp.getOrgUnitId() != null ? orgUnitNameMap.getOrDefault(emp.getOrgUnitId(), "Chưa gán") : "Chưa gán";

            DepartmentCapacityAccumulator deptAcc = deptMap.computeIfAbsent(
                    empDeptId,
                    k -> new DepartmentCapacityAccumulator(k, empDeptName)
            );
            deptAcc.employeeCount++;

            for (YearWeek yw : targetWeeks) {
                String key = makeKey(emp.getIdValue(), yw.year(), yw.weekNumber());

                WeeklyAvailability savedAvail = availabilityMap.get(key);
                int standardHours = savedAvail != null
                        ? savedAvail.getStandardHours()
                        : (emp.getStandardHoursPerWeek() != null ? emp.getStandardHoursPerWeek() : 40);
                int holidayHours = holidayHoursByWeek.getOrDefault(yw, 0);
                BigDecimal leaveHours = leaveHoursMap.getOrDefault(emp.getIdValue(), Map.of()).getOrDefault(yw, BigDecimal.ZERO);
                BigDecimal baseAvailableHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, leaveHours);

                BigDecimal netAvailableHours = WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(
                        baseAvailableHours,
                        emp.getContractEndDate(),
                        yw.getStartDate(),
                        yw.getEndDate(),
                        weekWorkingDaysCount
                );

                BigDecimal allocatedHours = allocationMap.getOrDefault(key, BigDecimal.ZERO);
                CapacityStatus status = WeeklyCapacityMatrixPolicy.determineStatus(allocatedHours, netAvailableHours, overloadThreshold, idleThreshold);
                boolean isOverloaded = (status == CapacityStatus.OVERLOADED);

                if (isOverloaded) {
                    empOverloadedWeeksCount++;
                    weeklyOverloadedCountMap.put(yw, weeklyOverloadedCountMap.getOrDefault(yw, 0) + 1);
                }

                empTotalAllocated = empTotalAllocated.add(allocatedHours);
                empTotalAvailable = empTotalAvailable.add(netAvailableHours);

                weeklyAvailableMap.put(yw, weeklyAvailableMap.getOrDefault(yw, BigDecimal.ZERO).add(netAvailableHours));
                weeklyAllocatedMap.put(yw, weeklyAllocatedMap.getOrDefault(yw, BigDecimal.ZERO).add(allocatedHours));

                deptAcc.totalAllocated = deptAcc.totalAllocated.add(allocatedHours);
                deptAcc.totalAvailable = deptAcc.totalAvailable.add(netAvailableHours);
            }

            if (empOverloadedWeeksCount > 0) {
                overloadedEmployeeIds.add(emp.getIdValue());
                deptAcc.overloadedEmployeesCount++;
                BigDecimal empAvgUtil = WeeklyCapacityMatrixPolicy.calculateAverageUtilization(empTotalAllocated, empTotalAvailable);
                overloadedEmployeesList.add(new OverloadedEmployeeItem(
                        emp.getIdValue(),
                        emp.getEmployeeCode(),
                        emp.getFullName(),
                        emp.getOrgUnitId(),
                        empDeptName,
                        emp.getProfessionalRole() != null ? emp.getProfessionalRole() : "Nhân viên",
                        empTotalAllocated.setScale(1, RoundingMode.HALF_UP),
                        empTotalAvailable.setScale(1, RoundingMode.HALF_UP),
                        empAvgUtil,
                        empOverloadedWeeksCount
                ));
            }

            totalAllocatedHours = totalAllocatedHours.add(empTotalAllocated);
            totalAvailableHours = totalAvailableHours.add(empTotalAvailable);
        }

        // Sắp xếp danh sách nhân sự quá tải theo số tuần quá tải giảm dần, sau đó tỷ lệ sử dụng giảm dần
        overloadedEmployeesList.sort(
                Comparator.comparingInt(OverloadedEmployeeItem::overloadedWeeksCount).reversed()
                        .thenComparing(OverloadedEmployeeItem::averageUtilizationRate, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        // 10. Tổng hợp Weekly Metrics
        List<WeeklyCapacityDashboardItem> weeklyMetrics = new ArrayList<>();
        for (YearWeek yw : targetWeeks) {
            BigDecimal wAvailable = weeklyAvailableMap.getOrDefault(yw, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
            BigDecimal wAllocated = weeklyAllocatedMap.getOrDefault(yw, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
            BigDecimal wRemaining = wAvailable.subtract(wAllocated).max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
            BigDecimal wUtil = WeeklyCapacityMatrixPolicy.calculateAverageUtilization(wAllocated, wAvailable);
            int wOverloaded = weeklyOverloadedCountMap.getOrDefault(yw, 0);

            weeklyMetrics.add(new WeeklyCapacityDashboardItem(
                    yw.year(),
                    yw.weekNumber(),
                    yw.getStartDate(),
                    yw.getEndDate(),
                    "T" + yw.weekNumber() + " (" + yw.getStartDate().format(dtf) + " - " + yw.getEndDate().format(dtf) + ")",
                    wAvailable,
                    wAllocated,
                    wRemaining,
                    wUtil,
                    wOverloaded
            ));
        }

        // 11. Tổng hợp Department Breakdown (sắp xếp theo tỷ lệ sử dụng giảm dần)
        List<DepartmentCapacityItem> departmentBreakdown = deptMap.values().stream()
                .map(d -> {
                    BigDecimal alloc = d.totalAllocated.setScale(1, RoundingMode.HALF_UP);
                    BigDecimal avail = d.totalAvailable.setScale(1, RoundingMode.HALF_UP);
                    BigDecimal free = avail.subtract(alloc).max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
                    BigDecimal util = WeeklyCapacityMatrixPolicy.calculateAverageUtilization(alloc, avail);
                    return new DepartmentCapacityItem(
                            d.orgUnitId != 0L ? d.orgUnitId : null,
                            d.orgUnitName,
                            d.employeeCount,
                            alloc,
                            avail,
                            free,
                            util,
                            d.overloadedEmployeesCount
                    );
                })
                .sorted(Comparator.comparing(DepartmentCapacityItem::utilizationRate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // 12. Tải và tổng hợp xung đột lịch chưa xử lý (unresolved schedule conflicts)
        List<UnresolvedConflictItem> unresolvedConflictItems = loadUnresolvedConflicts(targetWeeks, employeeIds, employeeMap);
        int unresolvedScheduleConflictsCount = unresolvedConflictItems.size();

        // 13. Danh mục dự án đang chạy
        List<ActiveProjectSummaryItem> activeProjectItems = mapActiveProjectItems(activeProjectsPreview);

        // 14. Tính 5 chỉ số cốt lõi (KPIs)
        BigDecimal averageCapacityUtilization = WeeklyCapacityMatrixPolicy.calculateAverageUtilization(totalAllocatedHours, totalAvailableHours);
        int overloadedEmployeesCount = overloadedEmployeeIds.size();
        BigDecimal departmentFreeHours = totalAvailableHours.subtract(totalAllocatedHours).max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);

        recordSuccessAuditLog(currentUserId, orgUnitName, fromYear, fromWeek, durationWeeks);

        return new CapacityDashboardResult(
                effectiveOrgUnitId,
                orgUnitName,
                fromYear,
                fromWeek,
                durationWeeks,
                averageCapacityUtilization,
                overloadedEmployeesCount,
                departmentFreeHours,
                unresolvedScheduleConflictsCount,
                activeProjectsCount,
                totalAvailableHours.setScale(1, RoundingMode.HALF_UP),
                totalAllocatedHours.setScale(1, RoundingMode.HALF_UP),
                weeklyMetrics,
                departmentBreakdown,
                overloadedEmployeesList,
                unresolvedConflictItems,
                activeProjectItems,
                LocalDateTime.now()
        );
    }

    private List<UnresolvedConflictItem> loadUnresolvedConflicts(
            List<YearWeek> targetWeeks,
            List<Long> employeeIds,
            Map<Long, Employee> employeeMap
    ) {
        if (loadScheduleConflictPort == null || targetWeeks.isEmpty() || employeeIds.isEmpty()) {
            return List.of();
        }

        Set<Long> empIdSet = new HashSet<>(employeeIds);
        Set<String> targetWeekKeys = targetWeeks.stream()
                .map(yw -> yw.year() + "_" + yw.weekNumber())
                .collect(Collectors.toSet());

        // Nhóm các tuần theo năm để truy vấn
        Map<Integer, List<YearWeek>> weeksByYear = targetWeeks.stream()
                .collect(Collectors.groupingBy(YearWeek::year));

        List<ScheduleConflict> allConflicts = new ArrayList<>();
        for (Map.Entry<Integer, List<YearWeek>> entry : weeksByYear.entrySet()) {
            int year = entry.getKey();
            int minWeek = entry.getValue().stream().mapToInt(YearWeek::weekNumber).min().orElse(1);
            int maxWeek = entry.getValue().stream().mapToInt(YearWeek::weekNumber).max().orElse(52);

            List<ScheduleConflict> list = loadScheduleConflictPort.findConflicts(year, minWeek, maxWeek, null, null, null);
            if (list != null) {
                allConflicts.addAll(list);
            }
        }

        return allConflicts.stream()
                .filter(c -> c.getStatus() != ScheduleConflictStatus.RESOLVED)
                .filter(c -> empIdSet.contains(c.getEmployeeId()))
                .filter(c -> targetWeekKeys.contains(c.getYearNumber() + "_" + c.getWeekNumber()))
                .map(c -> {
                    Employee emp = employeeMap.get(c.getEmployeeId());
                    String empCode = emp != null ? emp.getEmployeeCode() : "NV" + c.getEmployeeId();
                    String empName = emp != null ? emp.getFullName() : "Nhân viên #" + c.getEmployeeId();
                    int projCount = 0;
                    if (c.getProjectIds() != null && !c.getProjectIds().isBlank()) {
                        projCount = c.getProjectIds().split(",").length;
                    }
                    BigDecimal allocHours = c.getTotalAllocatedHours() != null ? c.getTotalAllocatedHours().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    return new UnresolvedConflictItem(
                            c.getId(),
                            c.getEmployeeId(),
                            empCode,
                            empName,
                            c.getConflictType() != null ? c.getConflictType().name() : "Xung đột lịch",
                            c.getYearNumber(),
                            c.getWeekNumber(),
                            projCount,
                            allocHours,
                            c.getStatus() != null ? c.getStatus().name() : "OPEN",
                            c.getDetails()
                    );
                })
                .sorted(Comparator.comparingInt(UnresolvedConflictItem::yearNumber)
                        .thenComparingInt(UnresolvedConflictItem::weekNumber))
                .toList();
    }

    private static final int DEFAULT_DASHBOARD_ACTIVE_PROJECTS_PREVIEW_LIMIT = 50;

    private long countActiveProjectsInScope(Long effectiveOrgUnitId) {
        if (loadProjectPort == null) {
            return 0L;
        }
        if (effectiveOrgUnitId != null) {
            return loadProjectPort.countActiveProjectsByOrgUnitBranch(effectiveOrgUnitId);
        }
        return loadProjectPort.countActiveProjects();
    }

    private List<Project> loadActiveProjectsPreviewInScope(Long effectiveOrgUnitId, User currentUser) {
        if (loadProjectPort == null) {
            return List.of();
        }

        List<Project> activeProjects;
        if (effectiveOrgUnitId != null) {
            activeProjects = loadProjectPort.findActiveProjectsByOrgUnitBranch(effectiveOrgUnitId, 0, DEFAULT_DASHBOARD_ACTIVE_PROJECTS_PREVIEW_LIMIT);
        } else {
            activeProjects = loadProjectPort.findActiveProjects(0, DEFAULT_DASHBOARD_ACTIVE_PROJECTS_PREVIEW_LIMIT);
        }

        if (activeProjects == null) {
            return List.of();
        }

        return activeProjects;
    }

    private List<ActiveProjectSummaryItem> mapActiveProjectItems(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return List.of();
        }

        List<Long> orgUnitIds = projects.stream().map(Project::getOrgUnitId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> orgNameMap = orgUnitIds.isEmpty() ? Map.of() :
                loadOrgUnitPort.findAllByIdIn(orgUnitIds).stream()
                        .collect(Collectors.toMap(u -> u.getId().getValue(), OrgUnit::getUnitName, (e1, e2) -> e1));

        List<EmployeeId> managerIds = projects.stream()
                .map(Project::getManagerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> managerNameMap = managerIds.isEmpty() ? Map.of() :
                loadEmployeePort.findAllByIdIn(managerIds).stream()
                        .collect(Collectors.toMap(Employee::getIdValue, Employee::getFullName, (e1, e2) -> e1));

        List<Long> projectIds = projects.stream().map(Project::getIdValue).filter(Objects::nonNull).toList();
        Map<Long, Integer> memberCountMap = (loadProjectMemberPort != null && !projectIds.isEmpty())
                ? loadProjectMemberPort.countMembersByProjectIds(projectIds)
                : Map.of();

        return projects.stream()
                .map(p -> {
                    String orgName = p.getOrgUnitId() != null ? orgNameMap.getOrDefault(p.getOrgUnitId(), "Chưa gán") : "Chưa gán";
                    String pmName = p.getManagerIdValue() != null ? managerNameMap.getOrDefault(p.getManagerIdValue(), "Chưa bổ nhiệm") : "Chưa bổ nhiệm";
                    int memberCount = (p.getIdValue() != null) ? memberCountMap.getOrDefault(p.getIdValue(), 0) : 0;
                    Integer estHours = p.getEstimatedHours() != null ? p.getEstimatedHours().intValue() : null;
                    return new ActiveProjectSummaryItem(
                            p.getIdValue(),
                            p.getProjectCode(),
                            p.getProjectName(),
                            p.getOrgUnitId(),
                            orgName,
                            pmName,
                            p.getStartDate() != null ? p.getStartDate().toString() : null,
                            p.getEndDate() != null ? p.getEndDate().toString() : null,
                            estHours,
                            memberCount,
                            p.getStatus().name()
                    );
                })
                .toList();
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
        return Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        );
    }

    private Long resolveEmployeeOrgUnitId(User currentUser, Long currentUserId) {
        if (currentUser.getEmployeeId() != null) {
            Employee emp = loadEmployeePort.findById(currentUser.getEmployeeId()).orElse(null);
            if (emp != null && emp.getOrgUnitId() != null) {
                return emp.getOrgUnitId();
            }
        }
        return loadEmployeePort.findByUserId(new UserId(currentUserId))
                .map(Employee::getOrgUnitId)
                .orElse(null);
    }

    private void recordDeniedAuditLog(Long userId, String reason) {
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.createChange(
                    userId,
                    "ACCESS_DENIED_CAPACITY_DASHBOARD",
                    "capacity_dashboard",
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
                    "CAPACITY_DASHBOARD_VIEWED",
                    "capacity_dashboard",
                    null,
                    null,
                    "Xem bảng điều khiển năng lực " + durationWeeks + " tuần từ T" + fromWeek + "/" + fromYear + " cho đơn vị " + orgUnitName
            ));
        }
    }

    private static class DepartmentCapacityAccumulator {
        final Long orgUnitId;
        final String orgUnitName;
        int employeeCount = 0;
        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalAvailable = BigDecimal.ZERO;
        int overloadedEmployeesCount = 0;

        DepartmentCapacityAccumulator(Long orgUnitId, String orgUnitName) {
            this.orgUnitId = orgUnitId;
            this.orgUnitName = orgUnitName;
        }
    }
}
