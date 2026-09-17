package com.hrm.employeemanagement.application.service.report.excel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.report.excel.ExportReportExcelQuery;
import com.hrm.employeemanagement.application.dto.report.excel.ExportReportExcelResult;
import com.hrm.employeemanagement.application.port.inbound.report.excel.ExportProjectAllocationReportExcelUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.report.excel.GenerateExcelWorkbookPort;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
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
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.domain.report.excel.ExcelReportData;
import com.hrm.employeemanagement.domain.report.excel.ExcelReportMetadata;
import com.hrm.employeemanagement.domain.report.excel.ProjectAllocationExcelRow;
import com.hrm.employeemanagement.domain.report.excel.SensitiveDataMaskingPolicy;
import com.hrm.employeemanagement.domain.report.excel.exception.NoReportDataToExportException;
import com.hrm.employeemanagement.domain.report.excel.exception.ReportExportAuditException;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Application Service thực thi Use Case Xuất báo cáo phân bổ dự án ra file Excel (NCL-10-CN-003).
 * Dành riêng cho Quản lý dự án (VT-02) và Ban Giám Đốc (VT-01).
 * Tuân thủ nghiêm ngặt quy tắc QTN-02 và che dữ liệu nhạy cảm.
 */
public class ExportProjectAllocationReportExcelService implements ExportProjectAllocationReportExcelUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final GenerateExcelWorkbookPort generateExcelWorkbookPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadOrgUnitPort loadOrgUnitPort;

    public ExportProjectAllocationReportExcelService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadProjectMemberPort loadProjectMemberPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            GenerateExcelWorkbookPort generateExcelWorkbookPort,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this(authorizationService, loadUserPort, loadEmployeePort, loadProjectPort, loadProjectMemberPort,
                loadAllocationPort, generateExcelWorkbookPort, saveAuditLogPort, null);
    }

    public ExportProjectAllocationReportExcelService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadProjectMemberPort loadProjectMemberPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            GenerateExcelWorkbookPort generateExcelWorkbookPort,
            SaveAuditLogPort saveAuditLogPort,
            LoadOrgUnitPort loadOrgUnitPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadProjectMemberPort = loadProjectMemberPort;
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.generateExcelWorkbookPort = Objects.requireNonNull(generateExcelWorkbookPort, "GenerateExcelWorkbookPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.loadOrgUnitPort = loadOrgUnitPort;
    }

    @Override
    public ExportReportExcelResult export(ExportReportExcelQuery rawQuery) {
        Objects.requireNonNull(rawQuery, "Query không được null");
        ExportReportExcelQuery query = rawQuery.withDefaults();

        // 1. Phân quyền: Yêu cầu permission RESOURCE_ALLOCATION_READ (TC-03)
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại: " + currentUserId));

        // 2. Tải thông tin dự án
        Project project = loadProjectPort.findById(new ProjectId(query.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + query.projectId()));

        // 3. Kiểm soát quyền Quản lý dự án (VT-02) theo dự án phụ trách (TC-03)
        RoleCode roleCode = currentUser.getRole() != null ? currentUser.getRole().getCode() : null;
        Long userEmployeeId = currentUser.getEmployeeIdValue();
        Long projectManagerId = project.getManagerIdValue();

        boolean canExport = SensitiveDataMaskingPolicy.canExportProjectReport(roleCode, userEmployeeId, projectManagerId);
        if (!canExport) {
            recordDeniedAuditLog(currentUserId, query.projectId(), "UNAUTHORIZED_ROLE_OR_PROJECT_MANAGER");
            throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ);
        }

        // 4. Tạo danh sách các tuần trong khoảng thời gian yêu cầu
        List<YearWeek> targetWeeks = buildTargetWeeks(query.fromYear(), query.fromWeek(), query.toYear(), query.toWeek());
        if (targetWeeks.isEmpty()) {
            throw new NoReportDataToExportException("Khoảng thời gian yêu cầu không hợp lệ hoặc không có tuần nào");
        }

        // 5. Tải dữ liệu phân bổ của dự án trong các tuần đó
        List<WeeklyProjectAllocation> allocations = loadAllocationsForWeeks(project.getIdValue(), targetWeeks);

        // 6. Kiểm tra dữ liệu rỗng (TC-02: Không có số liệu trong kỳ đã chọn)
        if (allocations == null || allocations.isEmpty()) {
            throw new NoReportDataToExportException("Không có dữ liệu để xuất trong kỳ đã chọn");
        }

        BigDecimal totalAllocatedHours = allocations.stream()
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAllocatedHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NoReportDataToExportException("Không có dữ liệu để xuất trong kỳ đã chọn");
        }

        // 7. Tải thông tin nhân sự và vai trò trong dự án
        List<Long> employeeIds = allocations.stream()
                .map(WeeklyProjectAllocation::getEmployeeId)
                .distinct()
                .toList();

        Map<Long, Employee> employeeMap = loadEmployeePort.findAllByIdIn(
                employeeIds.stream().map(EmployeeId::new).toList()
        ).stream().collect(Collectors.toMap(Employee::getIdValue, e -> e, (e1, e2) -> e1));

        Map<Long, String> projectRoleMap = loadProjectRoles(project.getIdValue(), employeeIds);
        Map<Long, String> orgUnitNameMap = loadOrgUnitNames(employeeMap.values());

        // 8. Gom nhóm phân bổ theo nhân viên và áp dụng Masking Policy
        Map<Long, Map<YearWeek, BigDecimal>> hoursByEmpAndWeek = new HashMap<>();
        for (WeeklyProjectAllocation alloc : allocations) {
            hoursByEmpAndWeek.computeIfAbsent(alloc.getEmployeeId(), k -> new HashMap<>())
                    .merge(alloc.getYearWeek(), alloc.getAllocatedHours(), BigDecimal::add);
        }

        List<ProjectAllocationExcelRow> excelRows = new ArrayList<>();
        for (Long empId : employeeIds) {
            Employee emp = employeeMap.get(empId);
            String empCode = emp != null ? emp.getEmployeeCode() : "NV" + empId;
            String fullName = emp != null ? emp.getFullName() : "Nhân viên #" + empId;

            String roleInProj = projectRoleMap.get(empId);
            if (roleInProj == null || roleInProj.isBlank()) {
                roleInProj = (emp != null && emp.getProfessionalRole() != null && !emp.getProfessionalRole().isBlank())
                        ? emp.getProfessionalRole()
                        : "Thành viên dự án";
            }

            String orgUnitName = "Phòng ban";
            if (emp != null && emp.getOrgUnitId() != null) {
                orgUnitName = orgUnitNameMap.getOrDefault(emp.getOrgUnitId(), "Phòng ban");
            }

            Map<YearWeek, BigDecimal> empWeekly = hoursByEmpAndWeek.getOrDefault(empId, Map.of());
            BigDecimal empTotal = empWeekly.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            String maskedSalary = SensitiveDataMaskingPolicy.maskSalary(roleCode, null);
            String maskedCostRate = SensitiveDataMaskingPolicy.maskCostRate(roleCode, null);

            excelRows.add(new ProjectAllocationExcelRow(
                    empId,
                    empCode,
                    fullName,
                    roleInProj,
                    orgUnitName,
                    empWeekly,
                    empTotal,
                    maskedSalary,
                    maskedCostRate
            ));
        }

        // Sắp xếp danh sách nhân sự theo họ tên
        excelRows.sort(Comparator.comparing(ProjectAllocationExcelRow::getFullName));

        // 9. Chuẩn bị Metadata báo cáo
        String pmName = "Chưa bổ nhiệm";
        if (projectManagerId != null) {
            Optional<Employee> pmOpt = loadEmployeePort.findById(new EmployeeId(projectManagerId));
            if (pmOpt.isPresent()) {
                pmName = pmOpt.get().getFullName();
            }
        }

        String timeRangeText = String.format("Từ tuần T%02d/%d đến tuần T%02d/%d",
                query.fromWeek(), query.fromYear(), query.toWeek(), query.toYear());

        ExcelReportMetadata metadata = new ExcelReportMetadata(
                "BÁO CÁO PHÂN BỔ NGUỒN LỰC DỰ ÁN",
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                pmName,
                currentUser.getUsername(),
                timeRangeText,
                targetWeeks,
                LocalDateTime.now()
        );

        ExcelReportData reportData = new ExcelReportData(metadata, excelRows, totalAllocatedHours);

        // 10. Sinh binary file Excel .xlsx qua Adapter
        byte[] excelBytes = generateExcelWorkbookPort.generateProjectAllocationWorkbook(reportData);

        String filename = String.format("Bao_Cao_Phan_Bo_%s_%s.xlsx",
                project.getProjectCode().replaceAll("[^a-zA-Z0-9_-]", "_"),
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now()));

        // 11. Thực thi nghiêm ngặt QTN-02 (TC-04): Ghi nhật ký kiểm toán xuất dữ liệu
        // Nếu không ghi được nhật ký -> ném ngoại lệ và không cho hoàn tất thao tác xuất!
        recordSuccessAuditLog(currentUserId, project.getIdValue(), project.getProjectCode(), timeRangeText, filename);

        return new ExportReportExcelResult(
                filename,
                excelBytes,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
    }

    private List<YearWeek> buildTargetWeeks(int fromYear, int fromWeek, int toYear, int toWeek) {
        List<YearWeek> list = new ArrayList<>();
        YearWeek current = YearWeek.of(fromYear, fromWeek);
        YearWeek end = YearWeek.of(toYear, toWeek);

        if (current.isAfter(end)) {
            return list;
        }

        LocalDate monday = current.getStartDate();
        while (true) {
            int y = monday.get(IsoFields.WEEK_BASED_YEAR);
            int w = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            YearWeek yw = YearWeek.of(y, w);
            list.add(yw);

            if (yw.equals(end) || yw.isAfter(end)) {
                break;
            }
            monday = monday.plusWeeks(1);
        }
        return list;
    }

    private List<WeeklyProjectAllocation> loadAllocationsForWeeks(Long projectId, List<YearWeek> targetWeeks) {
        Map<Integer, List<Integer>> weeksByYear = targetWeeks.stream()
                .collect(Collectors.groupingBy(
                        YearWeek::year,
                        Collectors.mapping(YearWeek::weekNumber, Collectors.toList())
                ));

        List<WeeklyProjectAllocation> result = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : weeksByYear.entrySet()) {
            int year = entry.getKey();
            int minWeek = entry.getValue().stream().min(Integer::compareTo).orElse(1);
            int maxWeek = entry.getValue().stream().max(Integer::compareTo).orElse(52);

            List<WeeklyProjectAllocation> batch = loadAllocationPort.loadAllocationsForProjectInWeekRange(
                    projectId, year, minWeek, maxWeek
            );
            if (batch != null) {
                // Lọc đúng các tuần nằm trong targetWeeks
                for (WeeklyProjectAllocation alloc : batch) {
                    if (targetWeeks.contains(alloc.getYearWeek())) {
                        result.add(alloc);
                    }
                }
            }
        }
        return result;
    }

    private Map<Long, String> loadProjectRoles(Long projectId, List<Long> employeeIds) {
        if (loadProjectMemberPort == null || employeeIds == null || employeeIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<ProjectMemberResult> members = loadProjectMemberPort.findMembersByProjectId(projectId);
            if (members != null) {
                return members.stream()
                        .filter(m -> m.employeeId() != null && m.roleInProject() != null)
                        .collect(Collectors.toMap(
                                ProjectMemberResult::employeeId,
                                m -> m.roleInProject().name(),
                                (r1, r2) -> r1
                        ));
            }
        } catch (Exception e) {
            // Không làm gián đoạn báo cáo nếu nạp vai trò phụ bị lỗi
        }
        return Map.of();
    }

    private Map<Long, String> loadOrgUnitNames(java.util.Collection<Employee> employees) {
        if (loadOrgUnitPort == null || employees == null || employees.isEmpty()) {
            return Map.of();
        }
        try {
            List<Long> orgUnitIds = employees.stream()
                    .map(Employee::getOrgUnitId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (orgUnitIds.isEmpty()) {
                return Map.of();
            }
            List<OrgUnit> orgUnits = loadOrgUnitPort.findAllByIdIn(orgUnitIds);
            if (orgUnits != null) {
                return orgUnits.stream()
                        .filter(u -> u.getId() != null && u.getId().getValue() != null && u.getUnitName() != null)
                        .collect(Collectors.toMap(
                                u -> u.getId().getValue(),
                                OrgUnit::getUnitName,
                                (n1, n2) -> n1
                        ));
            }
        } catch (Exception e) {
            // Không làm gián đoạn báo cáo nếu nạp tên phòng ban bị lỗi
        }
        return Map.of();
    }

    private void recordDeniedAuditLog(Long userId, Long projectId, String reason) {
        try {
            saveAuditLogPort.save(AuditLog.createChange(
                    userId,
                    "ACCESS_DENIED_REPORT_EXCEL_EXPORT",
                    "weekly_project_allocations",
                    projectId,
                    null,
                    "Từ chối xuất báo cáo Excel dự án ID " + projectId + "; lý do: " + reason
            ));
        } catch (Exception ignored) {
            // Log từ chối tốt nhất có thể
        }
    }

    private void recordSuccessAuditLog(Long userId, Long projectId, String projectCode, String timeRangeText, String filename) {
        String detail = String.format("Xuất báo cáo file Excel: Dự án [%s], Kỳ [%s], Tệp [%s]", projectCode, timeRangeText, filename);
        try {
            saveAuditLogPort.save(AuditLog.createChange(
                    userId,
                    "EXPORT_REPORT_EXCEL",
                    "weekly_project_allocations",
                    projectId,
                    null,
                    detail
            ));
        } catch (Exception e) {
            // QTN-02 Enforcement: Không cho hoàn tất thao tác nếu không ghi được nhật ký!
            throw new ReportExportAuditException(
                    "Không thể ghi nhận nhật ký kiểm toán theo quy tắc QTN-02. Thao tác xuất báo cáo bị hủy bỏ.",
                    e
            );
        }
    }
}
