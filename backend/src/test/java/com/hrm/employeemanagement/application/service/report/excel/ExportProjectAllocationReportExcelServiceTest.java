package com.hrm.employeemanagement.application.service.report.excel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.dto.report.excel.ExportReportExcelQuery;
import com.hrm.employeemanagement.application.dto.report.excel.ExportReportExcelResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.report.excel.GenerateExcelWorkbookPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectMemberRole;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.report.excel.exception.NoReportDataToExportException;
import com.hrm.employeemanagement.domain.report.excel.exception.ReportExportAuditException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@DisplayName("ExportProjectAllocationReportExcelService Application Tests")
class ExportProjectAllocationReportExcelServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadProjectPort loadProjectPort;
    private LoadProjectMemberPort loadProjectMemberPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private GenerateExcelWorkbookPort generateExcelWorkbookPort;
    private SaveAuditLogPort saveAuditLogPort;
    private ExportProjectAllocationReportExcelService service;

    private static final Long PM_USER_ID = 20L;
    private static final Long PM_EMPLOYEE_ID = 100L;
    private static final Long PROJECT_ID = 1L;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadProjectPort = mock(LoadProjectPort.class);
        loadProjectMemberPort = mock(LoadProjectMemberPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        generateExcelWorkbookPort = mock(GenerateExcelWorkbookPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);

        service = new ExportProjectAllocationReportExcelService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadProjectPort,
                loadProjectMemberPort,
                loadAllocationPort,
                generateExcelWorkbookPort,
                saveAuditLogPort
        );

        // Mặc định cho phép permission
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(PM_USER_ID);

        // Mock User là PM (VT-02)
        Role pmRole = new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án");
        User pmUser = new User(new UserId(PM_USER_ID), "pm_john", "hash", pmRole, UserStatus.ACTIVE,
                new EmployeeId(PM_EMPLOYEE_ID), DataScope.SELF, null, 0L);
        when(loadUserPort.findById(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmUser));

        // Mock Project mà PM phụ trách
        Project project = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-001",
                "Dự án Chuyển đổi số",
                10L,
                new EmployeeId(PM_EMPLOYEE_ID),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(1000),
                "Mô tả dự án",
                ProjectStatus.ACTIVE,
                new UserId(1L),
                LocalDateTime.now(),
                LocalDateTime.now(),
                0L
        );
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));
    }

    private Employee createEmployee(Long id, String code, String name) {
        return new Employee(
                new EmployeeId(id),
                new UserId(id),
                10L,
                code,
                name,
                "Lập trình viên",
                LocalDate.of(2025, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE,
                0L
        );
    }

    @Test
    @DisplayName("NCL-10-CN-003-TC-01: Luồng thành công - Báo cáo có số liệu trong kỳ, tạo tệp đúng số liệu đã chọn")
    void testTC01_SuccessFlow() {
        YearWeek w1 = YearWeek.of(2026, 1);
        YearWeek w2 = YearWeek.of(2026, 2);

        WeeklyProjectAllocation a1 = WeeklyProjectAllocation.createNew(201L, PROJECT_ID, w1, BigDecimal.valueOf(40.0));
        WeeklyProjectAllocation a2 = WeeklyProjectAllocation.createNew(201L, PROJECT_ID, w2, BigDecimal.valueOf(35.0));
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(eq(PROJECT_ID), eq(2026), anyInt(), anyInt()))
                .thenReturn(List.of(a1, a2));

        Employee dev = createEmployee(201L, "DEV01", "Trần Kỹ Sư");
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(dev));

        Employee pmEmp = createEmployee(PM_EMPLOYEE_ID, "PM01", "Nguyễn Quản Lý");
        when(loadEmployeePort.findById(new EmployeeId(PM_EMPLOYEE_ID))).thenReturn(Optional.of(pmEmp));

        ProjectMemberResult member = new ProjectMemberResult(201L, "DEV01", "Trần Kỹ Sư", "dev@hrm.com", 10L, "Phòng Dev", ProjectMemberRole.MEMBER, "ACTIVE");
        when(loadProjectMemberPort.findMembersByProjectId(PROJECT_ID)).thenReturn(List.of(member));

        byte[] fakeBytes = new byte[]{1, 2, 3, 4, 5};
        when(generateExcelWorkbookPort.generateProjectAllocationWorkbook(any())).thenReturn(fakeBytes);

        ExportReportExcelQuery query = new ExportReportExcelQuery(PROJECT_ID, 2026, 1, 2026, 2);
        ExportReportExcelResult result = service.export(query);

        assertNotNull(result);
        assertArrayEquals(fakeBytes, result.content());
        assertTrue(result.filename().startsWith("Bao_Cao_Phan_Bo_PRJ-001_"));
        assertTrue(result.filename().endsWith(".xlsx"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", result.contentType());

        // Kiểm tra QTN-02 & TC-04: Lịch sử được lưu
        verify(saveAuditLogPort).save(argThat(log ->
                "EXPORT_REPORT_EXCEL".equals(log.getAction())
                        && log.getUserId().equals(PM_USER_ID)
                        && log.getRecordId().equals(PROJECT_ID)
                        && log.getNewValue().contains("PRJ-001")
        ));
    }

    @Test
    @DisplayName("NCL-10-CN-003-TC-02: Dữ liệu rỗng - Không có số liệu trong kỳ đã chọn -> Báo không có dữ liệu để xuất")
    void testTC02_EmptyData_ThrowsException() {
        // Mock không có dòng phân bổ nào trong kỳ
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(eq(PROJECT_ID), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        ExportReportExcelQuery query = new ExportReportExcelQuery(PROJECT_ID, 2026, 1, 2026, 4);

        NoReportDataToExportException ex = assertThrows(
                NoReportDataToExportException.class,
                () -> service.export(query)
        );

        assertTrue(ex.getMessage().contains("Không có dữ liệu để xuất"));
        // Đảm bảo không tạo file Excel và không ghi audit log xuất thành công
        verify(generateExcelWorkbookPort, never()).generateProjectAllocationWorkbook(any());
    }

    @Test
    @DisplayName("NCL-10-CN-003-TC-02: Dữ liệu rỗng - Tổng số giờ phân bổ bằng 0 -> Báo không có dữ liệu để xuất")
    void testTC02_ZeroHours_ThrowsException() {
        YearWeek w1 = YearWeek.of(2026, 1);
        WeeklyProjectAllocation a1 = WeeklyProjectAllocation.createNew(201L, PROJECT_ID, w1, BigDecimal.ZERO);
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(eq(PROJECT_ID), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(a1));

        ExportReportExcelQuery query = new ExportReportExcelQuery(PROJECT_ID, 2026, 1, 2026, 2);

        NoReportDataToExportException ex = assertThrows(
                NoReportDataToExportException.class,
                () -> service.export(query)
        );

        assertTrue(ex.getMessage().contains("Không có dữ liệu để xuất"));
    }

    @Test
    @DisplayName("NCL-10-CN-003-TC-03: Không có quyền - Người dùng không phải Quản lý dự án của dự án đó -> Từ chối và ghi nhật ký lần từ chối")
    void testTC03_UnauthorizedUser_ThrowsExceptionAndLogsDenial() {
        // Giả sử user hiện tại là PM nhưng dự án này lại do người khác (managerId = 999L) làm PM
        Project otherProject = new Project(
                new ProjectId(2L),
                "PRJ-002",
                "Dự án của người khác",
                10L,
                new EmployeeId(999L), // Không trùng với PM_EMPLOYEE_ID = 100L
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(500),
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(1L),
                LocalDateTime.now(),
                LocalDateTime.now(),
                0L
        );
        when(loadProjectPort.findById(new ProjectId(2L))).thenReturn(Optional.of(otherProject));

        ExportReportExcelQuery query = new ExportReportExcelQuery(2L, 2026, 1, 2026, 4);

        assertThrows(PermissionDeniedException.class, () -> service.export(query));

        // Xác minh ghi nhật ký từ chối truy cập
        verify(saveAuditLogPort).save(argThat(log ->
                "ACCESS_DENIED_REPORT_EXCEL_EXPORT".equals(log.getAction())
                        && log.getUserId().equals(PM_USER_ID)
                        && log.getRecordId().equals(2L)
        ));
    }

    @Test
    @DisplayName("NCL-10-CN-003-TC-03: Không có quyền - Vai trò Nhân viên (VT-04) mở xuất báo cáo -> Từ chối truy cập")
    void testTC03_EmployeeRole_ThrowsException() {
        Role devRole = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn");
        User devUser = new User(new UserId(30L), "dev_user", "hash", devRole, UserStatus.ACTIVE,
                new EmployeeId(201L), DataScope.SELF, null, 0L);
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(30L);
        when(loadUserPort.findById(new UserId(30L))).thenReturn(Optional.of(devUser));

        ExportReportExcelQuery query = new ExportReportExcelQuery(PROJECT_ID, 2026, 1, 2026, 4);

        assertThrows(PermissionDeniedException.class, () -> service.export(query));
        verify(saveAuditLogPort).save(argThat(log -> "ACCESS_DENIED_REPORT_EXCEL_EXPORT".equals(log.getAction())));
    }

    @Test
    @DisplayName("NCL-10-CN-003-TC-04 & QTN-02: Bắt buộc ghi nhật ký kiểm toán - Nếu ghi nhật ký lỗi, hủy bỏ thao tác và ném ngoại lệ")
    void testQTN02_AuditLogFailure_AbortsOperation() {
        YearWeek w1 = YearWeek.of(2026, 1);
        WeeklyProjectAllocation a1 = WeeklyProjectAllocation.createNew(201L, PROJECT_ID, w1, BigDecimal.valueOf(40.0));
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(eq(PROJECT_ID), eq(2026), anyInt(), anyInt()))
                .thenReturn(List.of(a1));

        Employee dev = createEmployee(201L, "DEV01", "Trần Kỹ Sư");
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(dev));

        when(generateExcelWorkbookPort.generateProjectAllocationWorkbook(any())).thenReturn(new byte[]{1, 2, 3});

        // Giả lập lỗi khi ghi Audit Log vào cơ sở dữ liệu
        doThrow(new RuntimeException("Database connection timeout when saving audit log"))
                .when(saveAuditLogPort).save(argThat(log -> "EXPORT_REPORT_EXCEL".equals(log.getAction())));

        ExportReportExcelQuery query = new ExportReportExcelQuery(PROJECT_ID, 2026, 1, 2026, 2);

        // QTN-02 bắt buộc: Không cho hoàn tất thao tác nếu không ghi được nhật ký!
        ReportExportAuditException ex = assertThrows(
                ReportExportAuditException.class,
                () -> service.export(query)
        );

        assertTrue(ex.getMessage().contains("QTN-02"));
    }
}
