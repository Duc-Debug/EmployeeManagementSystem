package com.hrm.employeemanagement.application.service.workload;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.workload.GetUpcomingWorkloadQuery;
import com.hrm.employeemanagement.application.dto.workload.ProjectWorkloadAllocationResult;
import com.hrm.employeemanagement.application.dto.workload.UpcomingWorkloadResult;
import com.hrm.employeemanagement.application.dto.workload.WeeklyWorkloadItemResult;
import com.hrm.employeemanagement.application.dto.workload.WorkloadSummaryResult;
import com.hrm.employeemanagement.application.port.inbound.workload.GetUpcomingWorkloadUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdPolicy;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Pure Java Application Service cho Use Case NCL-13-CN-004:
 * Xem khối lượng công việc sắp tới của tôi (Upcoming Workload View).
 */
public class GetUpcomingWorkloadService implements GetUpcomingWorkloadUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadProjectRolePort loadProjectRolePort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final LoadCapacityThresholdPort loadCapacityThresholdPort;
    private final SaveAuditLogPort saveAuditLogPort;

    public GetUpcomingWorkloadService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadProjectPort loadProjectPort,
            LoadProjectRolePort loadProjectRolePort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadCapacityThresholdPort loadCapacityThresholdPort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "loadOrgUnitPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "loadProjectPort must not be null");
        this.loadProjectRolePort = Objects.requireNonNull(loadProjectRolePort, "loadProjectRolePort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "loadAllocationPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "loadWeeklyAvailabilityPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "loadHolidaysPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "loadApprovedLeavesPort must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.loadCapacityThresholdPort = loadCapacityThresholdPort;
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "saveAuditLogPort must not be null");
    }

    @Override
    public UpcomingWorkloadResult getMyUpcomingWorkload(Integer fromYear, Integer fromWeek, Integer durationWeeks) {
        Long currentUserId = authorizationService.require(PermissionCode.EMPLOYEE_READ);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        enforceSpecialistRole(currentUser, currentUserId);

        Employee employee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự của tài khoản hiện tại"));

        return executeWorkloadQuery(currentUserId, employee, fromYear, fromWeek, durationWeeks, true);
    }

    @Override
    public UpcomingWorkloadResult getUpcomingWorkload(GetUpcomingWorkloadQuery query) {
        Long currentUserId = authorizationService.require(PermissionCode.EMPLOYEE_READ);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        enforceSpecialistRole(currentUser, currentUserId);

        Employee employee;
        boolean isSelfAccess = false;

        if (query.employeeId() == null) {
            employee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                    .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự của tài khoản hiện tại"));
            isSelfAccess = true;
        } else {
            employee = loadEmployeePort.findById(new EmployeeId(query.employeeId()))
                    .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + query.employeeId()));

            if (currentUser.getIdValue() != null && currentUser.getIdValue().equals(employee.getUserIdValue())) {
                isSelfAccess = true;
            } else {
                enforceDataScope(currentUser, employee);
            }
        }

        return executeWorkloadQuery(currentUserId, employee, query.fromYear(), query.fromWeek(), query.durationWeeks(), isSelfAccess);
    }

    private void enforceSpecialistRole(User currentUser, Long currentUserId) {
        boolean isSpecialist = currentUser.getRole() != null &&
                (currentUser.getRole().getCode() == RoleCode.VT_04 || "VT-04".equalsIgnoreCase(currentUser.getRole().getCode().getCode()));
        if (!isSpecialist) {
            String roleCodeStr = currentUser.getRole() != null && currentUser.getRole().getCode() != null
                    ? currentUser.getRole().getCode().getCode()
                    : "UNKNOWN";
            recordDeniedAuditLog(currentUserId, null, "ROLE_NOT_SPECIALIST;role=" + roleCodeStr);
            throw new PermissionDeniedException(PermissionCode.EMPLOYEE_READ);
        }
    }

    private UpcomingWorkloadResult executeWorkloadQuery(
            Long currentUserId,
            Employee employee,
            Integer fromYearParam,
            Integer fromWeekParam,
            Integer durationParam,
            boolean isSelfAccess
    ) {
        Long employeeId = employee.getIdValue();

        // 1. Xác định tuần bắt đầu và số tuần
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

        int maxWeeks = YearWeek.maxWeeksInYear(fromYear);
        if (fromWeek < 1 || fromWeek > maxWeeks) {
            throw new IllegalArgumentException("Số tuần không hợp lệ: " + fromWeek + " (năm " + fromYear + " có " + maxWeeks + " tuần)");
        }

        int durationWeeks = (durationParam != null && durationParam > 0) ? Math.min(durationParam, 24) : 8;

        // 2. Tạo danh sách các tuần ISO liên tiếp
        List<YearWeek> targetWeeks = buildTargetWeeks(fromYear, fromWeek, durationWeeks);

        // 3. Tra cứu cấu hình ngưỡng hiệu lực
        BigDecimal overloadThreshold = CapacityThresholdPolicy.DEFAULT_OVERLOAD_THRESHOLD;
        BigDecimal idleThreshold = CapacityThresholdPolicy.DEFAULT_IDLE_THRESHOLD;

        if (loadCapacityThresholdPort != null) {
            if (employee.getOrgUnitId() != null) {
                Optional<CapacityThresholdConfig> orgUnitConfig = loadCapacityThresholdPort
                        .findByScope(CapacityThresholdScope.ORG_UNIT, employee.getOrgUnitId());
                if (orgUnitConfig.isPresent()) {
                    overloadThreshold = orgUnitConfig.get().getOverloadThreshold();
                    idleThreshold = orgUnitConfig.get().getIdleThreshold();
                } else {
                    Optional<CapacityThresholdConfig> companyConfig = loadCapacityThresholdPort
                            .findByScope(CapacityThresholdScope.COMPANY, null);
                    if (companyConfig.isPresent()) {
                        overloadThreshold = companyConfig.get().getOverloadThreshold();
                        idleThreshold = companyConfig.get().getIdleThreshold();
                    }
                }
            } else {
                Optional<CapacityThresholdConfig> companyConfig = loadCapacityThresholdPort
                        .findByScope(CapacityThresholdScope.COMPANY, null);
                if (companyConfig.isPresent()) {
                    overloadThreshold = companyConfig.get().getOverloadThreshold();
                    idleThreshold = companyConfig.get().getIdleThreshold();
                }
            }
        }

        // 4. Lấy thông tin phòng ban
        String orgUnitName = "Chưa phân bổ";
        if (employee.getOrgUnitId() != null) {
            Optional<OrgUnit> orgUnitOpt = loadOrgUnitPort.findById(new OrgUnitId(employee.getOrgUnitId()));
            if (orgUnitOpt.isPresent()) {
                orgUnitName = orgUnitOpt.get().getUnitName();
            }
        }

        // 5. Nạp dữ liệu các tuần: Holidays, Leaves, Availability overrides, Allocations
        Set<DayOfWeek> workingDays = resolveWorkingDays();
        LocalDate overallStartDate = targetWeeks.get(0).getStartDate();
        LocalDate overallEndDate = targetWeeks.get(targetWeeks.size() - 1).getEndDate();

        List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(overallStartDate, overallEndDate);

        // Nạp giờ nghỉ phép đã duyệt
        Map<YearWeek, BigDecimal> approvedLeavesMap = new HashMap<>();
        Map<Long, Map<YearWeek, BigDecimal>> batchLeaves = loadApprovedLeavesPort
                .loadApprovedLeaveHoursForEmployeesAndWeeks(List.of(employeeId), targetWeeks);
        if (batchLeaves != null && batchLeaves.containsKey(employeeId)) {
            approvedLeavesMap.putAll(batchLeaves.get(employeeId));
        }

        // Nạp giờ khả dụng tùy biến nếu có
        List<WeeklyAvailability> availabilityOverrides = loadWeeklyAvailabilityPort
                .loadAvailabilityForEmployeesAndWeeks(List.of(employeeId), targetWeeks);
        Map<YearWeek, WeeklyAvailability> availabilityMap = availabilityOverrides.stream()
                .collect(Collectors.toMap(WeeklyAvailability::getYearWeek, a -> a, (k1, k2) -> k1));

        // Nạp phân bổ dự án của nhân viên trong các tuần
        List<WeeklyProjectAllocation> allocations = loadAllocationPort
                .loadAllocationsForEmployeesAndWeeks(List.of(employeeId), targetWeeks);

        // Map projectId -> Project & projectRoleId -> ProjectRole
        List<ProjectId> projectIds = allocations.stream()
                .map(a -> new ProjectId(a.getProjectId()))
                .distinct()
                .toList();
        Map<Long, Project> projectsMap = new HashMap<>();
        if (!projectIds.isEmpty()) {
            loadProjectPort.findAllById(projectIds).forEach(p -> projectsMap.put(p.getId().value(), p));
        }

        Map<Long, ProjectRole> projectRolesMap = new HashMap<>();
        loadProjectRolePort.findAll().forEach(r -> {
            if (r.getId() != null) {
                projectRolesMap.put(r.getId().value(), r);
            }
        });

        Map<YearWeek, List<WeeklyProjectAllocation>> allocationsByWeek = allocations.stream()
                .collect(Collectors.groupingBy(a -> YearWeek.of(a.getYear(), a.getWeekNumber())));

        // 6. Xử lý từng tuần
        List<WeeklyWorkloadItemResult> weeklyWorkloads = new ArrayList<>();

        BigDecimal sumStandardHours = BigDecimal.ZERO;
        BigDecimal sumHolidayHours = BigDecimal.ZERO;
        BigDecimal sumLeaveHours = BigDecimal.ZERO;
        BigDecimal sumNetAvailableHours = BigDecimal.ZERO;
        BigDecimal sumAllocatedHours = BigDecimal.ZERO;

        int overloadedCount = 0;
        int normalCount = 0;
        int idleCount = 0;

        BigDecimal maxUtilization = BigDecimal.ZERO;
        String maxUtilizationWeek = null;

        for (YearWeek yw : targetWeeks) {
            int stdHours = availabilityMap.containsKey(yw)
                    ? availabilityMap.get(yw).getStandardHours()
                    : (employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40);

            List<Holiday> holidaysInWeek = holidays.stream()
                    .filter(h -> !h.date().isBefore(yw.getStartDate()) && !h.date().isAfter(yw.getEndDate()))
                    .toList();
            int holHours = WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidaysInWeek, workingDays);

            BigDecimal leaveHours = approvedLeavesMap.getOrDefault(yw, BigDecimal.ZERO);
            BigDecimal netHours = WeeklyAvailabilityPolicy.calculateNetAvailableHours(stdHours, holHours, leaveHours);

            List<WeeklyProjectAllocation> weekAllocations = allocationsByWeek.getOrDefault(yw, List.of());
            BigDecimal totalAllocatedHours = BigDecimal.ZERO;

            List<ProjectWorkloadAllocationResult> projectAllocationsList = new ArrayList<>();
            for (WeeklyProjectAllocation a : weekAllocations) {
                totalAllocatedHours = totalAllocatedHours.add(a.getAllocatedHours());

                Project p = projectsMap.get(a.getProjectId());
                String pName = p != null ? p.getProjectName() : ("Dự án #" + a.getProjectId());
                String pCode = p != null ? p.getProjectCode() : ("PRJ-" + a.getProjectId());

                ProjectRole role = a.getProjectRoleId() != null ? projectRolesMap.get(a.getProjectRoleId()) : null;
                String roleName = role != null ? role.getName() : null;

                BigDecimal pct = a.getAllocationPercentage();
                if (pct == null && netHours.compareTo(BigDecimal.ZERO) > 0) {
                    pct = a.getAllocatedHours().multiply(BigDecimal.valueOf(100))
                            .divide(netHours, 1, RoundingMode.HALF_UP);
                }

                projectAllocationsList.add(new ProjectWorkloadAllocationResult(
                        a.getProjectId(),
                        pName,
                        pCode,
                        a.getProjectRoleId(),
                        roleName,
                        a.getAllocatedHours().setScale(1, RoundingMode.HALF_UP),
                        pct != null ? pct.setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO
                ));
            }

            // Tính % Utilization
            BigDecimal utilization;
            if (netHours.compareTo(BigDecimal.ZERO) > 0) {
                utilization = totalAllocatedHours.multiply(BigDecimal.valueOf(100))
                        .divide(netHours, 1, RoundingMode.HALF_UP);
            } else if (totalAllocatedHours.compareTo(BigDecimal.ZERO) > 0) {
                utilization = BigDecimal.valueOf(100.0).setScale(1, RoundingMode.HALF_UP);
            } else {
                utilization = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
            }

            // Đánh giá trạng thái
            String status;
            BigDecimal overloadHours = BigDecimal.ZERO;

            if (utilization.compareTo(overloadThreshold) > 0 || (netHours.compareTo(BigDecimal.ZERO) == 0 && totalAllocatedHours.compareTo(BigDecimal.ZERO) > 0)) {
                status = "OVERLOADED";
                overloadedCount++;
                BigDecimal maxAllowableHours = netHours.multiply(overloadThreshold).divide(BigDecimal.valueOf(100), 1, RoundingMode.HALF_UP);
                overloadHours = totalAllocatedHours.subtract(maxAllowableHours).max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
            } else if (utilization.compareTo(idleThreshold) < 0) {
                status = "IDLE";
                idleCount++;
            } else {
                status = "NORMAL";
                normalCount++;
            }

            if (utilization.compareTo(maxUtilization) > 0) {
                maxUtilization = utilization;
                maxUtilizationWeek = "T" + yw.weekNumber() + "/" + yw.year();
            }

            sumStandardHours = sumStandardHours.add(BigDecimal.valueOf(stdHours));
            sumHolidayHours = sumHolidayHours.add(BigDecimal.valueOf(holHours));
            sumLeaveHours = sumLeaveHours.add(leaveHours);
            sumNetAvailableHours = sumNetAvailableHours.add(netHours);
            sumAllocatedHours = sumAllocatedHours.add(totalAllocatedHours);

            String weekLabel = "T" + yw.weekNumber() + "/" + yw.year();

            weeklyWorkloads.add(new WeeklyWorkloadItemResult(
                    yw.year(),
                    yw.weekNumber(),
                    yw.getStartDate(),
                    yw.getEndDate(),
                    weekLabel,
                    stdHours,
                    holHours,
                    leaveHours.setScale(1, RoundingMode.HALF_UP),
                    netHours.setScale(1, RoundingMode.HALF_UP),
                    totalAllocatedHours.setScale(1, RoundingMode.HALF_UP),
                    utilization.setScale(1, RoundingMode.HALF_UP),
                    status,
                    overloadHours,
                    projectAllocationsList
            ));
        }

        // Tính Average Utilization
        BigDecimal avgUtilization = BigDecimal.ZERO;
        if (sumNetAvailableHours.compareTo(BigDecimal.ZERO) > 0) {
            avgUtilization = sumAllocatedHours.multiply(BigDecimal.valueOf(100))
                    .divide(sumNetAvailableHours, 1, RoundingMode.HALF_UP);
        }

        WorkloadSummaryResult summary = new WorkloadSummaryResult(
                sumStandardHours.setScale(1, RoundingMode.HALF_UP),
                sumHolidayHours.setScale(1, RoundingMode.HALF_UP),
                sumLeaveHours.setScale(1, RoundingMode.HALF_UP),
                sumNetAvailableHours.setScale(1, RoundingMode.HALF_UP),
                sumAllocatedHours.setScale(1, RoundingMode.HALF_UP),
                avgUtilization.setScale(1, RoundingMode.HALF_UP),
                overloadedCount,
                normalCount,
                idleCount,
                maxUtilization.setScale(1, RoundingMode.HALF_UP),
                maxUtilizationWeek != null ? maxUtilizationWeek : ("T" + fromWeek + "/" + fromYear)
        );

        // 7. Ghi Audit Log thành công
        String auditAction = isSelfAccess ? "MY_WORKLOAD_VIEWED" : "EMPLOYEE_WORKLOAD_VIEWED";
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                auditAction,
                "employee_workload",
                employeeId,
                null,
                "Xem khối lượng công việc " + durationWeeks + " tuần từ T" + fromWeek + "/" + fromYear + " của nhân sự " + employee.getFullName()
        ));

        return new UpcomingWorkloadResult(
                employeeId,
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getOrgUnitId(),
                orgUnitName,
                fromYear,
                fromWeek,
                durationWeeks,
                overloadThreshold.setScale(1, RoundingMode.HALF_UP),
                idleThreshold.setScale(1, RoundingMode.HALF_UP),
                weeklyWorkloads,
                summary,
                LocalDateTime.now()
        );
    }

    private void enforceDataScope(User currentUser, Employee employee) {
        if (currentUser.getDataScope() == null) {
            recordDeniedAuditLog(currentUser.getIdValue(), employee.getIdValue(), "NO_DATA_SCOPE");
            throw new PermissionDeniedException(PermissionCode.EMPLOYEE_READ);
        }

        boolean allowed = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> currentUser.getIdValue() != null && currentUser.getIdValue().equals(employee.getUserIdValue());
            case ORGANIZATION_BRANCH -> employee.getOrgUnitId() != null
                    && currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(employee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
        };

        if (!allowed) {
            recordDeniedAuditLog(currentUser.getIdValue(), employee.getIdValue(), "OUT_OF_DATA_SCOPE");
            throw new PermissionDeniedException(PermissionCode.EMPLOYEE_READ);
        }
    }

    private void recordDeniedAuditLog(Long userId, Long targetEmployeeId, String reason) {
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.createChange(
                    userId,
                    "ACCESS_DENIED_EMPLOYEE_WORKLOAD",
                    "employee_workload",
                    targetEmployeeId,
                    null,
                    "Truy cập khối lượng công việc bị từ chối: user_id=" + userId + "; target_employee_id=" + targetEmployeeId + "; reason=" + reason
            ));
        }
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

    private Set<DayOfWeek> resolveWorkingDays() {
        if (loadWorkingCalendarPort != null) {
            CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();
            if (calendar != null && calendar.getWorkingDays() != null && !calendar.getWorkingDays().isEmpty()) {
                return calendar.getWorkingDays();
            }
        }
        return EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
    }
}
