package com.hrm.employeemanagement.application.service.report.timesheetvariance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceItem;
import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceQuery;
import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceResult;
import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceSummary;
import com.hrm.employeemanagement.application.port.inbound.report.timesheetvariance.GetTimesheetVarianceUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.report.timesheetvariance.LoadTimesheetVariancePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Application Service thực thi Use Case Báo cáo đối chiếu giờ phân bổ với giờ thực tế (NCL-09-CN-004).
 */
public class GetTimesheetVarianceService implements GetTimesheetVarianceUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadTimesheetVariancePort loadTimesheetVariancePort;
    private final SaveAuditLogPort saveAuditLogPort;

    public GetTimesheetVarianceService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadProjectPort loadProjectPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadTimesheetVariancePort loadTimesheetVariancePort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.loadTimesheetVariancePort = Objects.requireNonNull(loadTimesheetVariancePort, "LoadTimesheetVariancePort must not be null");
        this.saveAuditLogPort = saveAuditLogPort;
    }

    @Override
    public TimesheetVarianceResult execute(TimesheetVarianceQuery query) {
        // 1. Phân quyền: Kiểm tra TIMESHEET_VARIANCE_READ
        Long currentUserId = authorizationService.require(PermissionCode.TIMESHEET_VARIANCE_READ);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 2. Data Scope Validation (QTN-01)
        Long effectiveOrgUnitId;
        try {
            switch (currentUser.getDataScope()) {
                case COMPANY -> effectiveOrgUnitId = query != null ? query.orgUnitId() : null;
                case ORGANIZATION_BRANCH -> {
                    if (currentUser.getScopeOrgUnitId() == null) {
                        throw new PermissionDeniedException(PermissionCode.TIMESHEET_VARIANCE_READ);
                    }
                    if (query != null && query.orgUnitId() != null) {
                        boolean inScope = loadOrgUnitPort.existsInOrgUnitBranch(query.orgUnitId(), currentUser.getScopeOrgUnitId());
                        if (!inScope) {
                            throw new PermissionDeniedException(PermissionCode.TIMESHEET_VARIANCE_READ);
                        }
                        effectiveOrgUnitId = query.orgUnitId();
                    } else {
                        effectiveOrgUnitId = currentUser.getScopeOrgUnitId();
                    }
                }
                default -> throw new PermissionDeniedException(PermissionCode.TIMESHEET_VARIANCE_READ);
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

        // 3. Xử lý khoảng tuần (Week Range Validation)
        YearWeek startWeek;
        YearWeek endWeek;

        if (query == null || (query.fromYear() == null && query.fromWeek() == null)) {
            LocalDate now = LocalDate.now();
            int currentYear = now.get(IsoFields.WEEK_BASED_YEAR);
            int currentWeek = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            startWeek = YearWeek.of(currentYear, currentWeek);
            endWeek = startWeek;
        } else {
            if (query.fromYear() == null || query.fromWeek() == null) {
                throw new IllegalArgumentException("fromYear và fromWeek phải được cung cấp cùng nhau");
            }
            startWeek = YearWeek.of(query.fromYear(), query.fromWeek());

            if ((query.toYear() != null && query.toWeek() == null) || (query.toYear() == null && query.toWeek() != null)) {
                throw new IllegalArgumentException("toYear và toWeek phải được cung cấp cùng nhau");
            }

            if (query.toYear() != null && query.toWeek() != null) {
                endWeek = YearWeek.of(query.toYear(), query.toWeek());
                if (startWeek.isAfter(endWeek)) {
                    throw new IllegalArgumentException("Tuần bắt đầu không được lớn hơn tuần kết thúc");
                }
            } else {
                endWeek = startWeek;
            }
        }

        List<YearWeek> targetWeeks = buildTargetWeeks(startWeek, endWeek);

        // 4. Lấy danh sách nhân viên trong Scope (tối ưu nạp trực tiếp 1 nhân viên nếu query.employeeId() != null)
        List<Employee> targetEmployees = loadEmployeesInScope(effectiveOrgUnitId, query != null ? query.employeeId() : null);

        if (targetEmployees.isEmpty()) {
            TimesheetVarianceSummary emptySummary = new TimesheetVarianceSummary(
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    0, targetWeeks.size(), 0, 0, 0, 0
            );

            recordSuccessAuditLog(currentUserId, orgUnitName, startWeek, endWeek, 0);

            return new TimesheetVarianceResult(
                    effectiveOrgUnitId,
                    orgUnitName,
                    startWeek.year(),
                    startWeek.weekNumber(),
                    endWeek.year(),
                    endWeek.weekNumber(),
                    Collections.emptyList(),
                    emptySummary,
                    false,
                    "Chưa đủ dữ liệu thực tế để đối chiếu",
                    LocalDateTime.now()
            );
        }

        List<Long> employeeIds = targetEmployees.stream().map(Employee::getIdValue).toList();
        Map<Long, Employee> employeeMap = targetEmployees.stream().collect(Collectors.toMap(Employee::getIdValue, e -> e));

        // Load map OrgUnit tên
        Map<Long, String> orgUnitNames = new HashMap<>();
        for (Employee emp : targetEmployees) {
            if (emp.getOrgUnitId() != null && !orgUnitNames.containsKey(emp.getOrgUnitId())) {
                loadOrgUnitPort.findById(new OrgUnitId(emp.getOrgUnitId()))
                        .ifPresent(u -> orgUnitNames.put(emp.getOrgUnitId(), u.getUnitName()));
            }
        }

        Long filterProjectId = query != null ? query.projectId() : null;

        // 5. Tải dữ liệu phân bổ (Kế hoạch)
        List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(employeeIds, targetWeeks);
        if (filterProjectId != null) {
            allocations = allocations.stream().filter(a -> a.getProjectId().equals(filterProjectId)).toList();
        }

        // Map: employeeId_projectId_year_week -> allocatedHours
        Map<String, BigDecimal> allocationMap = new HashMap<>();
        Set<String> activeKeys = new HashSet<>();
        Set<Long> involvedProjectIds = new HashSet<>();

        for (WeeklyProjectAllocation a : allocations) {
            String key = a.getEmployeeId() + "_" + a.getProjectId() + "_" + a.getYear() + "_" + a.getWeekNumber();
            allocationMap.merge(key, a.getAllocatedHours(), BigDecimal::add);
            activeKeys.add(key);
            involvedProjectIds.add(a.getProjectId());
        }

        // 6. Tải dữ liệu thực tế đã duyệt (status = 'APPROVED')
        Map<String, BigDecimal> actualHoursMap = loadTimesheetVariancePort.loadApprovedActualHours(employeeIds, targetWeeks, filterProjectId);

        // Bổ sung các key có actualHours nhưng chưa có trong allocation (làm ngoài phân bổ)
        for (String key : actualHoursMap.keySet()) {
            if (key.chars().filter(ch -> ch == '_').count() == 3) {
                activeKeys.add(key);
                String[] parts = key.split("_");
                involvedProjectIds.add(Long.parseLong(parts[1]));
            }
        }

        // Tải thông tin Projects
        Map<Long, Project> projectMap = new HashMap<>();
        if (!involvedProjectIds.isEmpty()) {
            List<ProjectId> projectIds = involvedProjectIds.stream().map(ProjectId::new).toList();
            List<Project> loadedProjects = loadProjectPort.findAllById(projectIds);
            for (Project p : loadedProjects) {
                projectMap.put(p.getId().value(), p);
            }
        }

        // 7. Tổng hợp các dòng TimesheetVarianceItem
        List<TimesheetVarianceItem> items = new ArrayList<>();
        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        BigDecimal totalVariance = BigDecimal.ZERO;

        int positiveCount = 0;
        int negativeCount = 0;
        int onTrackCount = 0;
        int noActualCount = 0;
        boolean hasAnyActual = false;

        // Nếu người dùng lọc 1 dự án hoặc muốn xem chi tiết theo nhân sự + tuần + dự án
        for (String key : activeKeys) {
            String[] parts = key.split("_");
            if (parts.length != 4) continue;

            Long empId = Long.parseLong(parts[0]);
            Long projId = Long.parseLong(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int w = Integer.parseInt(parts[3]);

            Employee emp = employeeMap.get(empId);
            if (emp == null) continue;

            Project proj = projectMap.get(projId);
            String projectName = proj != null ? proj.getProjectName() : ("Dự án #" + projId);

            YearWeek yw = YearWeek.of(y, w);
            BigDecimal allocated = allocationMap.getOrDefault(key, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
            BigDecimal actual = actualHoursMap.getOrDefault(key, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);

            boolean hasActual = actual.compareTo(BigDecimal.ZERO) > 0;
            if (hasActual) {
                hasAnyActual = true;
            }

            BigDecimal variance = actual.subtract(allocated).setScale(1, RoundingMode.HALF_UP);
            BigDecimal variancePercentage = calculateVariancePercentage(variance, allocated);

            String status;
            if (!hasActual && allocated.compareTo(BigDecimal.ZERO) > 0) {
                status = "NO_ACTUAL_DATA";
                noActualCount++;
            } else if (variance.compareTo(BigDecimal.ZERO) > 0) {
                status = "POSITIVE_VARIANCE";
                positiveCount++;
            } else if (variance.compareTo(BigDecimal.ZERO) < 0) {
                status = "NEGATIVE_VARIANCE";
                negativeCount++;
            } else {
                status = "ON_TRACK";
                onTrackCount++;
            }

            String unitName = emp.getOrgUnitId() != null ? orgUnitNames.getOrDefault(emp.getOrgUnitId(), "-") : "-";

            items.add(new TimesheetVarianceItem(
                    emp.getIdValue(),
                    emp.getEmployeeCode(),
                    emp.getFullName(),
                    emp.getOrgUnitId(),
                    unitName,
                    projId,
                    projectName,
                    y,
                    w,
                    yw.getStartDate(),
                    yw.getEndDate(),
                    allocated,
                    actual,
                    variance,
                    variancePercentage,
                    hasActual,
                    status
            ));

            totalAllocated = totalAllocated.add(allocated);
            totalActual = totalActual.add(actual);
            totalVariance = totalVariance.add(variance);
        }

        // Sắp xếp danh sách: theo Tuần, Tên nhân viên, Dự án
        items.sort(Comparator
                .comparing(TimesheetVarianceItem::year)
                .thenComparing(TimesheetVarianceItem::weekNumber)
                .thenComparing(TimesheetVarianceItem::fullName)
                .thenComparing(TimesheetVarianceItem::projectName));

        Set<Long> uniqueEmployees = items.stream().map(TimesheetVarianceItem::employeeId).collect(Collectors.toSet());

        TimesheetVarianceSummary summary = new TimesheetVarianceSummary(
                totalAllocated.setScale(1, RoundingMode.HALF_UP),
                totalActual.setScale(1, RoundingMode.HALF_UP),
                totalVariance.setScale(1, RoundingMode.HALF_UP),
                uniqueEmployees.size(),
                targetWeeks.size(),
                positiveCount,
                negativeCount,
                onTrackCount,
                noActualCount
        );

        String message = hasAnyActual
                ? "Lấy báo cáo đối chiếu giờ phân bổ với thực tế thành công"
                : "Chưa đủ dữ liệu thực tế để đối chiếu";

        recordSuccessAuditLog(currentUserId, orgUnitName, startWeek, endWeek, items.size());

        return new TimesheetVarianceResult(
                effectiveOrgUnitId,
                orgUnitName,
                startWeek.year(),
                startWeek.weekNumber(),
                endWeek.year(),
                endWeek.weekNumber(),
                items,
                summary,
                hasAnyActual,
                message,
                LocalDateTime.now()
        );
    }

    private BigDecimal calculateVariancePercentage(BigDecimal variance, BigDecimal allocated) {
        if (allocated == null || allocated.compareTo(BigDecimal.ZERO) == 0) {
            // Khi giờ phân bổ = 0, phép tính (actual - allocated) / allocated có mẫu số bằng 0 -> trả về null (N/A)
            return null;
        }
        return variance.multiply(BigDecimal.valueOf(100))
                .divide(allocated, 1, RoundingMode.HALF_UP);
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

    private List<Employee> loadEmployeesInScope(Long effectiveOrgUnitId, Long filterEmployeeId) {
        if (filterEmployeeId != null) {
            Employee emp = loadEmployeePort.findById(new EmployeeId(filterEmployeeId))
                    .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên: " + filterEmployeeId));
            if (effectiveOrgUnitId != null) {
                List<Long> branchIds = resolveScopeBranchOrgUnitIds(effectiveOrgUnitId);
                if (emp.getOrgUnitId() == null || (branchIds != null && !branchIds.contains(emp.getOrgUnitId()))) {
                    throw new EmployeeNotFoundException("Nhân viên nằm ngoài phạm vi quản lý: " + filterEmployeeId);
                }
            }
            return List.of(emp);
        }

        List<Long> branchIds = resolveScopeBranchOrgUnitIds(effectiveOrgUnitId);
        if (branchIds != null) {
            return loadEmployeePort.findActiveByOrgUnitIds(branchIds);
        }
        return loadEmployeePort.findAllActive();
    }

    private List<YearWeek> buildTargetWeeks(YearWeek start, YearWeek end) {
        List<YearWeek> list = new ArrayList<>();
        YearWeek current = start;
        LocalDate monday = current.getStartDate();
        LocalDate endMonday = end.getStartDate();

        while (!monday.isAfter(endMonday)) {
            int y = monday.get(IsoFields.WEEK_BASED_YEAR);
            int w = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            list.add(YearWeek.of(y, w));
            monday = monday.plusWeeks(1);
        }
        return list;
    }

    private void recordDeniedAuditLog(Long userId, String reason) {
        if (saveAuditLogPort != null) {
            try {
                saveAuditLogPort.save(AuditLog.createChange(
                        userId,
                        "ACCESS_DENIED_TIMESHEET_VARIANCE_REPORT",
                        "timesheet_variance_report",
                        null,
                        null,
                        "user_id=" + (userId != null ? userId : "ANONYMOUS") + ";reason=" + reason
                ));
            } catch (Exception ignored) {
            }
        }
    }

    private void recordSuccessAuditLog(Long userId, String orgUnitName, YearWeek start, YearWeek end, int itemCount) {
        if (saveAuditLogPort != null) {
            try {
                saveAuditLogPort.save(AuditLog.createChange(
                        userId,
                        "TIMESHEET_VARIANCE_REPORT_VIEWED",
                        "timesheet_variance_report",
                        null,
                        null,
                        "Xem báo cáo đối chiếu giờ phân bổ vs thực tế từ T" + start.weekNumber() + "/" + start.year() +
                        " đến T" + end.weekNumber() + "/" + end.year() + " cho đơn vị " + orgUnitName + " (Tổng " + itemCount + " dòng)"
                ));
            } catch (Exception ignored) {
            }
        }
    }
}
