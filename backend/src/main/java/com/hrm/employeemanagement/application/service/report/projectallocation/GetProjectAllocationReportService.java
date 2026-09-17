package com.hrm.employeemanagement.application.service.report.projectallocation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.report.projectallocation.*;
import com.hrm.employeemanagement.application.port.inbound.report.projectallocation.ExportProjectAllocationReportUseCase;
import com.hrm.employeemanagement.application.port.inbound.report.projectallocation.GetProjectAllocationReportUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
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
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Application Service thực thi Báo cáo phân bổ theo dự án (NCL-10-CN-006).
 * Cho phép Quản lý dự án (PM), Ban Giám đốc và Quản lý Nguồn lực xem tổng giờ phân bổ, nhu cầu và chênh lệch theo tuần và vai trò.
 */
public class GetProjectAllocationReportService implements GetProjectAllocationReportUseCase, ExportProjectAllocationReportUseCase {

    private static final DateTimeFormatter WEEK_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadProjectResourceDemandPort loadDemandPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadProjectRolePort loadProjectRolePort;
    private final SaveAuditLogPort saveAuditLogPort;

    public GetProjectAllocationReportService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadProjectPort loadProjectPort,
            LoadProjectResourceDemandPort loadDemandPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadProjectRolePort loadProjectRolePort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadDemandPort = Objects.requireNonNull(loadDemandPort, "LoadProjectResourceDemandPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.loadProjectRolePort = Objects.requireNonNull(loadProjectRolePort, "LoadProjectRolePort must not be null");
        this.saveAuditLogPort = saveAuditLogPort;
    }

    @Override
    public ProjectAllocationReportResult execute(ProjectAllocationReportQuery query) {
        if (query == null || query.projectId() == null) {
            throw new IllegalArgumentException("Mã dự án (projectId) không được để trống");
        }

        // 1. Phân quyền & Kiểm soát truy cập (TC-02)
        Long currentUserId;
        try {
            currentUserId = authorizationService.require(PermissionCode.PROJECT_ALLOCATION_REPORT_READ);
        } catch (PermissionDeniedException e) {
            recordDeniedAuditLog(null, query.projectId(), "UNAUTHORIZED_ROLE");
            throw e;
        }

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        // 2. Tải thông tin dự án
        Project project = loadProjectPort.findById(new ProjectId(query.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + query.projectId()));

        // 3. Kiểm tra Data Scope truy cập dự án (TC-02)
        try {
            validateProjectAccessScope(currentUser, currentUserId, project);
        } catch (PermissionDeniedException e) {
            recordDeniedAuditLog(currentUserId, query.projectId(), "DATA_SCOPE_UNAUTHORIZED");
            throw e;
        }

        // 4. Xác định khoảng thời gian tuần báo cáo
        List<YearWeek> targetWeeks = resolveTargetWeeks(project, query);
        if (targetWeeks.isEmpty()) {
            YearWeek current = YearWeek.from(LocalDate.now());
            targetWeeks = List.of(current);
        }

        YearWeek startWeek = targetWeeks.get(0);
        YearWeek endWeek = targetWeeks.get(targetWeeks.size() - 1);

        // 5. Tải danh mục vai trò, nhu cầu ước lượng và phân bổ thực tế
        List<ProjectRole> allRoles = loadProjectRolePort.findAll();
        Map<Long, ProjectRole> roleMap = allRoles.stream()
                .filter(r -> r.getIdValue() != null)
                .collect(Collectors.toMap(ProjectRole::getIdValue, r -> r, (r1, r2) -> r1));

        Long effectiveProjectId = project.getIdValue() != null ? project.getIdValue() : query.projectId();
        ProjectId projIdObj = project.getId() != null ? project.getId() : new ProjectId(effectiveProjectId);

        List<ProjectResourceDemand> demands = loadDemandPort.findByProjectId(projIdObj);
        List<WeeklyProjectAllocation> allocations = loadAllocationsForWeeks(effectiveProjectId, targetWeeks);

        // Tải danh sách nhân sự được phân bổ
        List<Long> allocatedEmployeeIds = allocations.stream()
                .map(WeeklyProjectAllocation::getEmployeeId)
                .distinct()
                .toList();

        Map<Long, Employee> employeeMap = allocatedEmployeeIds.isEmpty() ? Map.of() :
                loadEmployeePort.findAllByIdIn(allocatedEmployeeIds.stream().map(EmployeeId::new).toList()).stream()
                        .collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));

        // 6. Nhóm nhu cầu theo roleId và YearWeek
        Map<String, BigDecimal> demandMap = demands.stream()
                .collect(Collectors.toMap(
                        d -> makeKey(d.getRoleIdValue(), d.getYear(), d.getWeekNumber()),
                        ProjectResourceDemand::getRequiredHours,
                        BigDecimal::add
                ));

        // 7. Nhóm phân bổ theo role và nhân sự
        Map<Long, ProjectRole> employeeToRoleMap = new HashMap<>();
        for (Employee emp : employeeMap.values()) {
            ProjectRole matchedRole = matchEmployeeToProjectRole(emp, allRoles, demands);
            if (matchedRole != null) {
                employeeToRoleMap.put(emp.getIdValue(), matchedRole);
            }
        }

        // 8. Thu thập danh sách vai trò xuất hiện trong dự án (có demand hoặc có phân bổ)
        Set<Long> involvedRoleIds = new LinkedHashSet<>();
        demands.forEach(d -> {
            if (d.getRoleIdValue() != null) involvedRoleIds.add(d.getRoleIdValue());
        });
        employeeToRoleMap.values().forEach(r -> {
            if (r.getIdValue() != null) involvedRoleIds.add(r.getIdValue());
        });

        // Nếu chưa có vai trò nào trong demand, hiển thị các vai trò đang active trong hệ thống
        if (involvedRoleIds.isEmpty()) {
            allRoles.stream().filter(ProjectRole::isActive).forEach(r -> involvedRoleIds.add(r.getIdValue()));
        }

        // 9. Tính toán chi tiết từng vai trò theo tuần
        List<RoleAllocationBreakdownItem> roleBreakdownList = new ArrayList<>();
        List<ShortageAlertItem> shortageAlerts = new ArrayList<>();

        Map<YearWeek, BigDecimal> weeklyTotalDemand = new HashMap<>();
        Map<YearWeek, BigDecimal> weeklyTotalAllocated = new HashMap<>();

        for (YearWeek yw : targetWeeks) {
            BigDecimal totalDemandForWeek = demands.stream()
                    .filter(d -> d.getYear() == yw.year() && d.getWeekNumber() == yw.weekNumber())
                    .map(ProjectResourceDemand::getRequiredHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            weeklyTotalDemand.put(yw, totalDemandForWeek);

            BigDecimal totalAllocatedForWeek = allocations.stream()
                    .filter(a -> a.getYear() == yw.year() && a.getWeekNumber() == yw.weekNumber())
                    .map(a -> a.getAllocatedHours() != null ? a.getAllocatedHours() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            weeklyTotalAllocated.put(yw, totalAllocatedForWeek);
        }

        for (Long roleId : involvedRoleIds) {
            ProjectRole role = roleMap.get(roleId);
            String roleName = role != null ? role.getName() : "Vai trò #" + roleId;
            String roleCode = role != null ? role.getCode() : "ROLE_" + roleId;

            BigDecimal roleTotalDemand = BigDecimal.ZERO;
            BigDecimal roleTotalAllocated = BigDecimal.ZERO;
            BigDecimal roleTotalShortfall = BigDecimal.ZERO;
            BigDecimal roleTotalSurplus = BigDecimal.ZERO;

            List<WeeklyRoleAllocationItem> weeklyRoleMetrics = new ArrayList<>();

            for (YearWeek yw : targetWeeks) {
                String key = makeKey(roleId, yw.year(), yw.weekNumber());
                BigDecimal demandHours = demandMap.getOrDefault(key, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);

                List<AllocatedMemberDetailItem> memberDetails = new ArrayList<>();
                BigDecimal weekRoleAllocated = BigDecimal.ZERO;

                for (WeeklyProjectAllocation alloc : allocations) {
                    if (alloc.getYear() == yw.year() && alloc.getWeekNumber() == yw.weekNumber()) {
                        ProjectRole empRole = employeeToRoleMap.get(alloc.getEmployeeId());
                        Long empRoleId = empRole != null ? empRole.getIdValue() : null;
                        if (Objects.equals(empRoleId, roleId)) {
                            Employee emp = employeeMap.get(alloc.getEmployeeId());
                            BigDecimal hours = alloc.getAllocatedHours() != null ? alloc.getAllocatedHours().setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                            weekRoleAllocated = weekRoleAllocated.add(hours);
                            memberDetails.add(new AllocatedMemberDetailItem(
                                     alloc.getEmployeeId(),
                                     emp != null ? emp.getEmployeeCode() : "NV" + alloc.getEmployeeId(),
                                     emp != null ? emp.getFullName() : "Nhân viên #" + alloc.getEmployeeId(),
                                     emp != null ? emp.getProfessionalRole() : roleName,
                                     hours,
                                     alloc.getAllocationPercentage()
                            ));
                        }
                    }
                }

                weekRoleAllocated = weekRoleAllocated.setScale(1, RoundingMode.HALF_UP);
                BigDecimal shortfallHours = demandHours.subtract(weekRoleAllocated).max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
                BigDecimal surplusHours = weekRoleAllocated.subtract(demandHours).max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);

                String weekLabel = "T" + yw.weekNumber() + " (" + yw.getStartDate().format(WEEK_DATE_FORMATTER) + " - " + yw.getEndDate().format(WEEK_DATE_FORMATTER) + ")";

                // Nếu thiếu hụt giờ (TC-01) -> tạo cảnh báo
                if (shortfallHours.compareTo(BigDecimal.ZERO) > 0 && demandHours.compareTo(BigDecimal.ZERO) > 0) {
                    String severity = shortfallHours.compareTo(BigDecimal.valueOf(20)) >= 0 ? "HIGH" : "MEDIUM";
                    String message = String.format("Tuần %d/%d: Nhu cầu %s giờ vai trò '%s' nhưng mới phân bổ %s giờ (Thiếu %s giờ)",
                            yw.weekNumber(), yw.year(), demandHours.stripTrailingZeros().toPlainString(), roleName,
                            weekRoleAllocated.stripTrailingZeros().toPlainString(), shortfallHours.stripTrailingZeros().toPlainString());

                    shortageAlerts.add(new ShortageAlertItem(
                            yw.year(),
                            yw.weekNumber(),
                            weekLabel,
                            roleId,
                            roleName,
                            demandHours,
                            weekRoleAllocated,
                            shortfallHours,
                            severity,
                            message
                    ));
                }

                weeklyRoleMetrics.add(new WeeklyRoleAllocationItem(
                        yw.year(),
                        yw.weekNumber(),
                        yw.getStartDate(),
                        yw.getEndDate(),
                        weekLabel,
                        demandHours,
                        weekRoleAllocated,
                        shortfallHours,
                        surplusHours,
                        memberDetails
                ));

                roleTotalDemand = roleTotalDemand.add(demandHours);
                roleTotalAllocated = roleTotalAllocated.add(weekRoleAllocated);
                roleTotalShortfall = roleTotalShortfall.add(shortfallHours);
                roleTotalSurplus = roleTotalSurplus.add(surplusHours);
            }

            BigDecimal roleFulfillment = roleTotalDemand.compareTo(BigDecimal.ZERO) > 0
                    ? roleTotalAllocated.divide(roleTotalDemand, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                    : (roleTotalAllocated.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));

            roleBreakdownList.add(new RoleAllocationBreakdownItem(
                    roleId,
                    roleCode,
                    roleName,
                    roleTotalDemand.setScale(1, RoundingMode.HALF_UP),
                    roleTotalAllocated.setScale(1, RoundingMode.HALF_UP),
                    roleTotalShortfall.setScale(1, RoundingMode.HALF_UP),
                    roleTotalSurplus.setScale(1, RoundingMode.HALF_UP),
                    roleFulfillment,
                    weeklyRoleMetrics
            ));
        }

        // 10. Tổng hợp Weekly Project Summaries
        List<WeeklyProjectSummaryItem> weeklySummaries = new ArrayList<>();
        int shortageWeeksCount = 0;
        BigDecimal grandTotalDemand = BigDecimal.ZERO;
        BigDecimal grandTotalAllocated = BigDecimal.ZERO;
        BigDecimal grandTotalShortfall = BigDecimal.ZERO;
        BigDecimal grandTotalSurplus = BigDecimal.ZERO;

        for (YearWeek yw : targetWeeks) {
            BigDecimal wDemand = weeklyTotalDemand.getOrDefault(yw, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
            BigDecimal wAllocated = weeklyTotalAllocated.getOrDefault(yw, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
            BigDecimal wShortfall = wDemand.subtract(wAllocated).max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
            BigDecimal wSurplus = wAllocated.subtract(wDemand).max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);

            BigDecimal wFulfillment = wDemand.compareTo(BigDecimal.ZERO) > 0
                    ? wAllocated.divide(wDemand, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                    : (wAllocated.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));

            String status = "SUFFICIENT";
            if (wShortfall.compareTo(BigDecimal.ZERO) > 0) {
                status = "SHORTAGE";
                shortageWeeksCount++;
            } else if (wSurplus.compareTo(BigDecimal.ZERO) > 0) {
                status = "SURPLUS";
            }

            String weekLabel = "T" + yw.weekNumber() + " (" + yw.getStartDate().format(WEEK_DATE_FORMATTER) + " - " + yw.getEndDate().format(WEEK_DATE_FORMATTER) + ")";

            weeklySummaries.add(new WeeklyProjectSummaryItem(
                    yw.year(),
                    yw.weekNumber(),
                    yw.getStartDate(),
                    yw.getEndDate(),
                    weekLabel,
                    wDemand,
                    wAllocated,
                    wShortfall,
                    wSurplus,
                    wFulfillment,
                    status
            ));

            grandTotalDemand = grandTotalDemand.add(wDemand);
            grandTotalAllocated = grandTotalAllocated.add(wAllocated);
            grandTotalShortfall = grandTotalShortfall.add(wShortfall);
            grandTotalSurplus = grandTotalSurplus.add(wSurplus);
        }

        BigDecimal overallFulfillment = grandTotalDemand.compareTo(BigDecimal.ZERO) > 0
                ? grandTotalAllocated.divide(grandTotalDemand, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                : (grandTotalAllocated.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));

        // Tải tên OrgUnit & PM
        String orgUnitName = "Chưa gán";
        if (project.getOrgUnitId() != null) {
            orgUnitName = loadOrgUnitPort.findById(new OrgUnitId(project.getOrgUnitId()))
                    .map(OrgUnit::getUnitName)
                    .orElse("Chưa gán");
        }

        String pmName = "Chưa bổ nhiệm";
        if (project.getManagerId() != null) {
            pmName = loadEmployeePort.findById(project.getManagerId())
                    .map(Employee::getFullName)
                    .orElse("Chưa bổ nhiệm");
        }

        BigDecimal totalEstimated = project.getEstimatedHours() != null
                ? project.getEstimatedHours().setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);

        // 11. Ghi Audit Log thành công (TC-03)
        recordSuccessAuditLog(currentUserId, effectiveProjectId, project, startWeek, endWeek);

        return new ProjectAllocationReportResult(
                effectiveProjectId,
                project.getProjectCode(),
                project.getProjectName(),
                project.getStatus().name(),
                project.getOrgUnitId(),
                orgUnitName,
                project.getManagerIdValue(),
                pmName,
                project.getStartDate() != null ? project.getStartDate().toString() : null,
                project.getEndDate() != null ? project.getEndDate().toString() : null,
                startWeek.year(),
                startWeek.weekNumber(),
                endWeek.year(),
                endWeek.weekNumber(),
                totalEstimated,
                grandTotalDemand.setScale(1, RoundingMode.HALF_UP),
                grandTotalAllocated.setScale(1, RoundingMode.HALF_UP),
                grandTotalShortfall.setScale(1, RoundingMode.HALF_UP),
                grandTotalSurplus.setScale(1, RoundingMode.HALF_UP),
                overallFulfillment,
                shortageWeeksCount,
                weeklySummaries,
                roleBreakdownList,
                shortageAlerts,
                LocalDateTime.now()
        );
    }

    @Override
    public ProjectAllocationReportExport export(ProjectAllocationReportQuery query) {
        ProjectAllocationReportResult result = execute(query);
        StringBuilder sb = new StringBuilder();

        // UTF-8 BOM
        sb.append("\uFEFF");
        sb.append("BÁO CÁO PHÂN BỔ THEO DỰ ÁN (NCL-10-CN-006)\n");
        sb.append("Mã dự án,").append(escapeCsv(result.projectCode())).append("\n");
        sb.append("Tên dự án,").append(escapeCsv(result.projectName())).append("\n");
        sb.append("Quản lý dự án (PM),").append(escapeCsv(result.managerName())).append("\n");
        sb.append("Phòng ban phụ trách,").append(escapeCsv(result.orgUnitName())).append("\n");
        sb.append("Thời gian thực hiện,").append(result.startDate()).append(" - ").append(result.endDate()).append("\n");
        sb.append("Kỳ báo cáo,T").append(result.fromWeek()).append("/").append(result.fromYear()).append(" - T").append(result.toWeek()).append("/").append(result.toYear()).append("\n");
        sb.append("Tổng nhu cầu ước lượng (h),").append(result.totalDemandHours()).append("\n");
        sb.append("Tổng giờ đã phân bổ (h),").append(result.totalAllocatedHours()).append("\n");
        sb.append("Tổng giờ thiếu hụt (h),").append(result.totalShortfallHours()).append("\n");
        sb.append("Tỷ lệ đáp ứng phân bổ (%),").append(result.fulfillmentRate()).append("%\n\n");

        // Section 1: Tổng quan theo tuần
        sb.append("--- TỔNG QUAN THEO TUẦN ---\n");
        sb.append("Năm,Tuần,Từ ngày,Đến ngày,Nhu cầu (h),Đã phân bổ (h),Thiếu hụt (h),Dôi dư (h),Tỷ lệ đáp ứng (%),Trạng thái\n");
        for (WeeklyProjectSummaryItem w : result.weeklySummaries()) {
            sb.append(w.year()).append(",")
                    .append(w.weekNumber()).append(",")
                    .append(w.startDate()).append(",")
                    .append(w.endDate()).append(",")
                    .append(w.demandHours()).append(",")
                    .append(w.allocatedHours()).append(",")
                    .append(w.shortfallHours()).append(",")
                    .append(w.surplusHours()).append(",")
                    .append(w.fulfillmentRate()).append("%,")
                    .append(escapeCsv(w.status())).append("\n");
        }
        sb.append("\n");

        // Section 2: Chi tiết theo vai trò
        sb.append("--- PHÂN BỔ CHI TIẾT THEO VAI TRÒ CHUYÊN MÔN ---\n");
        sb.append("Mã vai trò,Tên vai trò,Tuần,Năm,Nhu cầu vai trò (h),Đã phân bổ vai trò (h),Thiếu hụt (h),Nhân sự được phân bổ,Số giờ NV (h)\n");
        for (RoleAllocationBreakdownItem rb : result.roleBreakdowns()) {
            for (WeeklyRoleAllocationItem wr : rb.weeklyRoleMetrics()) {
                if (wr.allocatedMembers().isEmpty()) {
                    sb.append(escapeCsv(rb.roleCode())).append(",")
                            .append(escapeCsv(rb.roleName())).append(",")
                            .append(wr.weekNumber()).append(",")
                            .append(wr.year()).append(",")
                            .append(wr.demandHours()).append(",")
                            .append(wr.allocatedHours()).append(",")
                            .append(wr.shortfallHours()).append(",")
                            .append("Chưa phân bổ,").append("0\n");
                } else {
                    for (AllocatedMemberDetailItem m : wr.allocatedMembers()) {
                        sb.append(escapeCsv(rb.roleCode())).append(",")
                                .append(escapeCsv(rb.roleName())).append(",")
                                .append(wr.weekNumber()).append(",")
                                .append(wr.year()).append(",")
                                .append(wr.demandHours()).append(",")
                                .append(wr.allocatedHours()).append(",")
                                .append(wr.shortfallHours()).append(",")
                                .append(escapeCsv(m.fullName() + " (" + m.employeeCode() + ")")).append(",")
                                .append(m.allocatedHours()).append("\n");
                    }
                }
            }
        }
        sb.append("\n");

        // Section 3: Cảnh báo hụt người
        sb.append("--- CẢNH BÁO THIẾU HỤT NHÂN SỰ ---\n");
        sb.append("Tuần,Năm,Vai trò,Nhu cầu (h),Đã phân bổ (h),Số giờ thiếu (h),Mức độ,Nội dung cảnh báo\n");
        for (ShortageAlertItem alert : result.shortageAlerts()) {
            sb.append(alert.weekNumber()).append(",")
                    .append(alert.year()).append(",")
                    .append(escapeCsv(alert.roleName())).append(",")
                    .append(alert.demandHours()).append(",")
                    .append(alert.allocatedHours()).append(",")
                    .append(alert.missingHours()).append(",")
                    .append(alert.severity()).append(",")
                    .append(escapeCsv(alert.message())).append("\n");
        }

        String filename = "Bao_cao_phan_bo_du_an_" + result.projectCode() + "_" + LocalDate.now() + ".csv";
        return new ProjectAllocationReportExport(filename, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void validateProjectAccessScope(User currentUser, Long currentUserId, Project project) {
        switch (currentUser.getDataScope()) {
            case COMPANY -> {
                // Toàn quyền xem dự án công ty
            }
            case ORGANIZATION_BRANCH -> {
                if (currentUser.getScopeOrgUnitId() == null) {
                    throw new PermissionDeniedException(PermissionCode.PROJECT_ALLOCATION_REPORT_READ);
                }
                boolean inScope = loadProjectPort.existsInOrgUnitBranch(project.getIdValue(), currentUser.getScopeOrgUnitId());
                if (!inScope) {
                    throw new PermissionDeniedException(PermissionCode.PROJECT_ALLOCATION_REPORT_READ);
                }
            }
            case SELF -> {
                if (currentUser.getRole() != null && currentUser.getRole().getCode() == RoleCode.VT_02) {
                    Long pmEmployeeId = resolveEmployeeId(currentUser, currentUserId);
                    if (pmEmployeeId == null) {
                        throw new PermissionDeniedException(PermissionCode.PROJECT_ALLOCATION_REPORT_READ);
                    }
                    boolean isManager = (project.getManagerId() != null && Objects.equals(project.getManagerId().value(), pmEmployeeId))
                            || loadProjectPort.existsManagedBy(project.getIdValue(), pmEmployeeId);
                    if (!isManager) {
                        throw new PermissionDeniedException(PermissionCode.PROJECT_ALLOCATION_REPORT_READ);
                    }
                } else {
                    throw new PermissionDeniedException(PermissionCode.PROJECT_ALLOCATION_REPORT_READ);
                }
            }
            default -> throw new PermissionDeniedException(PermissionCode.PROJECT_ALLOCATION_REPORT_READ);
        }
    }

    private List<YearWeek> resolveTargetWeeks(Project project, ProjectAllocationReportQuery query) {
        YearWeek start;
        YearWeek end;

        if (query.fromYear() != null && query.fromWeek() != null) {
            start = YearWeek.of(query.fromYear(), query.fromWeek());
        } else if (project.getStartDate() != null) {
            start = YearWeek.from(project.getStartDate());
        } else {
            start = YearWeek.from(LocalDate.now());
        }

        if (query.toYear() != null && query.toWeek() != null) {
            end = YearWeek.of(query.toYear(), query.toWeek());
        } else if (project.getEndDate() != null) {
            end = YearWeek.from(project.getEndDate());
        } else {
            end = YearWeek.from(start.getStartDate().plusWeeks(11));
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Tuần bắt đầu không được lớn hơn tuần kết thúc");
        }

        LocalDate monday = start.getStartDate();
        LocalDate endMonday = end.getStartDate();
        long weeksCount = java.time.temporal.ChronoUnit.WEEKS.between(monday, endMonday) + 1;
        if (weeksCount > 104) {
            throw new IllegalArgumentException("Khoảng thời gian tra cứu tối đa là 104 tuần (2 năm)");
        }

        List<YearWeek> list = new ArrayList<>();
        while (!monday.isAfter(endMonday)) {
            list.add(YearWeek.from(monday));
            monday = monday.plusWeeks(1);
        }
        return list;
    }

    private List<WeeklyProjectAllocation> loadAllocationsForWeeks(Long projectId, List<YearWeek> targetWeeks) {
        if (targetWeeks.isEmpty()) return List.of();

        Map<Integer, List<YearWeek>> byYear = targetWeeks.stream().collect(Collectors.groupingBy(YearWeek::year));
        List<WeeklyProjectAllocation> result = new ArrayList<>();

        for (Map.Entry<Integer, List<YearWeek>> entry : byYear.entrySet()) {
            int year = entry.getKey();
            List<YearWeek> weeks = entry.getValue();
            int minWeek = weeks.stream().mapToInt(YearWeek::weekNumber).min().orElse(1);
            int maxWeek = weeks.stream().mapToInt(YearWeek::weekNumber).max().orElse(52);

            List<WeeklyProjectAllocation> yearAllocations = loadAllocationPort.loadAllocationsForProjectInWeekRange(
                    projectId, year, minWeek, maxWeek);

            Set<String> weekKeySet = weeks.stream()
                    .map(w -> w.year() + "_" + w.weekNumber())
                    .collect(Collectors.toSet());

            for (WeeklyProjectAllocation a : yearAllocations) {
                if (weekKeySet.contains(a.getYear() + "_" + a.getWeekNumber())) {
                    result.add(a);
                }
            }
        }
        return result;
    }

    private static final Map<String, List<String>> ROLE_FAMILY_KEYWORDS = Map.of(
            "DEV", List.of("developer", "lập trình", "software engineer", "backend", "frontend", "fullstack", "coder", "programmer", "tech lead", "lead developer", "software developer", "engineer", "kỹ sư phần mềm", "web developer", "mobile developer", "java", "react", "golang", "python", "node"),
            "TEST", List.of("tester", "qa", "qc", "kiểm thử", "quality assurance", "automation test", "manual test", "test engineer"),
            "BA", List.of("business analyst", "phân tích", "phân tích nghiệp vụ", "product owner", "po", "system analyst", "sa"),
            "UIUX", List.of("ui", "ux", "uiux", "ui/ux", "designer", "thiết kế", "graphic designer", "product designer", "interaction designer"),
            "PM", List.of("project manager", "quản lý dự án", "scrum master", "delivery manager", "project lead", "pm"),
            "DEVOPS", List.of("devops", "sre", "sysadmin", "infrastructure", "kỹ sư hệ thống", "cloud", "system admin", "platform engineer")
    );

    private ProjectRole matchEmployeeToProjectRole(Employee employee, List<ProjectRole> allRoles, List<ProjectResourceDemand> demands) {
        if (employee == null || employee.getProfessionalRole() == null || employee.getProfessionalRole().isBlank() || allRoles == null || allRoles.isEmpty()) {
            return null;
        }
        String profRole = employee.getProfessionalRole().trim();

        // 1. Tầng 1: Khớp chính xác (case-insensitive) theo Code hoặc Name
        for (ProjectRole r : allRoles) {
            if (r.getCode() != null && r.getCode().equalsIgnoreCase(profRole)) {
                return r;
            }
            if (r.getName() != null && r.getName().equalsIgnoreCase(profRole)) {
                return r;
            }
        }

        // 2. Tầng 2: Khớp chuẩn hóa bỏ khoảng trắng, dấu gạch dưới, gạch ngang, ký tự đặc biệt
        String normalizedProf = normalizeRoleString(profRole);
        if (!normalizedProf.isEmpty()) {
            for (ProjectRole r : allRoles) {
                if (r.getCode() != null && normalizedProf.equals(normalizeRoleString(r.getCode()))) {
                    return r;
                }
                if (r.getName() != null && normalizedProf.equals(normalizeRoleString(r.getName()))) {
                    return r;
                }
            }
        }

        // 3. Tầng 3: Khớp theo ranh giới từ (Word Boundary) với Name / Code của ProjectRole
        Set<Long> demandedRoleIds = demands != null ? demands.stream()
                .map(ProjectResourceDemand::getRoleIdValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()) : Set.of();

        List<ProjectRole> candidateRoles = new ArrayList<>(allRoles);
        // Sắp xếp: role có trong demand của dự án lên trước, role có độ dài tên lớn hơn lên trước (longest match first)
        candidateRoles.sort((r1, r2) -> {
            boolean d1 = r1.getIdValue() != null && demandedRoleIds.contains(r1.getIdValue());
            boolean d2 = r2.getIdValue() != null && demandedRoleIds.contains(r2.getIdValue());
            if (d1 != d2) return d1 ? -1 : 1;
            int l1 = Math.max(r1.getName() != null ? r1.getName().length() : 0, r1.getCode() != null ? r1.getCode().length() : 0);
            int l2 = Math.max(r2.getName() != null ? r2.getName().length() : 0, r2.getCode() != null ? r2.getCode().length() : 0);
            return Integer.compare(l2, l1);
        });

        for (ProjectRole r : candidateRoles) {
            if (r.getName() != null && matchesWordBoundary(profRole, r.getName())) {
                return r;
            }
            if (r.getCode() != null && matchesWordBoundary(profRole, r.getCode())) {
                return r;
            }
        }

        // 4. Tầng 4: Khớp theo bộ từ khóa vai trò chuyên môn (Role Family Keywords)
        String lowerProf = profRole.toLowerCase();
        for (ProjectRole r : candidateRoles) {
            String roleFamilyKey = identifyRoleFamily(r);
            if (roleFamilyKey != null) {
                List<String> keywords = ROLE_FAMILY_KEYWORDS.get(roleFamilyKey);
                if (keywords != null) {
                    for (String kw : keywords) {
                        if (matchesWordBoundary(lowerProf, kw) || lowerProf.contains(kw)) {
                            // Bảo vệ đặc thù: Không gán nhân sự DevOps vào vai trò DEV chung nếu không có vai trò DevOps
                            if ("DEV".equals(roleFamilyKey) && lowerProf.contains("devops")) {
                                continue;
                            }
                            // Không gán nhân sự QA/QC/Tester vào vai trò DEV
                            if ("DEV".equals(roleFamilyKey) && (lowerProf.contains("qa") || lowerProf.contains("test") || lowerProf.contains("qc") || lowerProf.contains("kiểm thử"))) {
                                continue;
                            }
                            return r;
                        }
                    }
                }
            }
        }

        return null;
    }

    private String identifyRoleFamily(ProjectRole role) {
        if (role == null) return null;
        String code = role.getCode() != null ? role.getCode().toUpperCase() : "";
        String name = role.getName() != null ? role.getName().toUpperCase() : "";

        if (code.contains("DEVOPS") || name.contains("DEVOPS") || name.contains("HỆ THỐNG") || name.contains("SYSADMIN") || name.contains("SRE")) return "DEVOPS";
        if (code.contains("TEST") || code.contains("QA") || code.contains("QC") || name.contains("TEST") || name.contains("KIỂM THỬ") || name.contains("QA") || name.contains("QC")) return "TEST";
        if (code.contains("BA") || name.contains("BUSINESS ANALYST") || name.contains("PHÂN TÍCH")) return "BA";
        if (code.contains("UIUX") || code.contains("UI") || code.contains("UX") || name.contains("UI/UX") || name.contains("UIUX") || name.contains("THIẾT KẾ") || name.contains("DESIGNER")) return "UIUX";
        if (code.contains("PM") || name.contains("PROJECT MANAGER") || name.contains("QUẢN LÝ DỰ ÁN") || name.contains("SCRUM MASTER")) return "PM";
        if (code.contains("DEV") || name.contains("LẬP TRÌNH") || name.contains("DEVELOPER") || name.contains("ENGINEER")) return "DEV";

        return null;
    }

    private String normalizeRoleString(String input) {
        if (input == null) return "";
        return input.replaceAll("[^a-zA-Z0-9\u00C0-\u024F\u1EA0-\u1EF9]", "").toLowerCase();
    }

    private boolean matchesWordBoundary(String text, String target) {
        if (text == null || target == null || target.trim().isEmpty()) {
            return false;
        }
        String cleanTarget = target.replaceAll("[()]", "").trim();
        if (cleanTarget.isEmpty()) {
            return false;
        }
        String regex = "(?i)\\b" + Pattern.quote(cleanTarget) + "\\b";
        return Pattern.compile(regex).matcher(text).find();
    }

    private Long resolveEmployeeId(User currentUser, Long currentUserId) {
        if (currentUser.getEmployeeId() != null) {
            return currentUser.getEmployeeId().value();
        }
        return loadEmployeePort.findByUserId(new UserId(currentUserId))
                .map(Employee::getIdValue)
                .orElse(null);
    }

    private String makeKey(Long roleId, int year, int weekNumber) {
        return (roleId != null ? roleId : 0L) + "_" + year + "_" + weekNumber;
    }

    private String escapeCsv(String text) {
        if (text == null) return "";
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private void recordDeniedAuditLog(Long userId, Long projectId, String reason) {
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.createChange(
                    userId,
                    "ACCESS_DENIED_PROJECT_ALLOCATION_REPORT",
                    "project_allocation_report",
                    projectId,
                    null,
                    "user_id=" + (userId != null ? userId : "ANONYMOUS") + ";projectId=" + projectId + ";reason=" + reason
            ));
        }
    }

    private void recordSuccessAuditLog(Long userId, Long projectId, Project project, YearWeek startWeek, YearWeek endWeek) {
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.createChange(
                    userId,
                    "PROJECT_ALLOCATION_REPORT_VIEWED",
                    "project_allocation_report",
                    projectId,
                    null,
                    "Xem báo cáo phân bổ dự án " + project.getProjectCode() + " từ T" + startWeek.weekNumber() + "/" + startWeek.year()
                            + " đến T" + endWeek.weekNumber() + "/" + endWeek.year()
            ));
        }
    }
}
