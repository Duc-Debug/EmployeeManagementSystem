package com.hrm.employeemanagement.application.service.importdata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.importdata.ConfirmEmployeeImportCommand;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeePreviewResult;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeeRowDto;
import com.hrm.employeemanagement.application.dto.importdata.ImportExecutionResult;
import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeDataFileParser;
import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeImportTemplateGenerator;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.importdata.DataImportException;
import com.hrm.employeemanagement.domain.importdata.RawEmployeeImportRow;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

@DisplayName("EmployeeImportService Unit Tests (NCL-12-CN-004)")
class EmployeeImportServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private SaveUserPort saveUserPort;
    private LoadRolePort loadRolePort;
    private LoadEmployeePort loadEmployeePort;
    private SaveEmployeePort saveEmployeePort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private PasswordEncoderPort passwordEncoder;
    private SaveAuditLogPort saveAuditLogPort;
    private EmployeeDataFileParser fileParser;
    private EmployeeImportTemplateGenerator templateGenerator;

    private EmployeeImportService service;
    private final Long ADMIN_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        saveUserPort = mock(SaveUserPort.class);
        loadRolePort = mock(LoadRolePort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        saveEmployeePort = mock(SaveEmployeePort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        passwordEncoder = mock(PasswordEncoderPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);
        fileParser = mock(EmployeeDataFileParser.class);
        templateGenerator = mock(EmployeeImportTemplateGenerator.class);

        when(fileParser.supports(anyString())).thenReturn(true);
        when(templateGenerator.supports(anyString())).thenReturn(true);

        service = new EmployeeImportService(
                authorizationService,
                loadUserPort,
                saveUserPort,
                loadRolePort,
                loadEmployeePort,
                saveEmployeePort,
                loadOrgUnitPort,
                passwordEncoder,
                saveAuditLogPort,
                List.of(fileParser),
                List.of(templateGenerator)
        );

        when(authorizationService.require(PermissionCode.DATA_IMPORT)).thenReturn(ADMIN_USER_ID);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        OrgUnit softwareCenter = mock(OrgUnit.class);
        when(softwareCenter.getId()).thenReturn(new OrgUnitId(10L));
        when(softwareCenter.getUnitName()).thenReturn("Trung tâm Phần mềm");
        when(loadOrgUnitPort.findAllActive()).thenReturn(List.of(softwareCenter));
    }

    @Test
    @DisplayName("NCL-12-CN-004-TC-01: Preview dữ liệu hợp lệ -> Thành công 100%")
    void testPreview_SuccessAllValid() {
        List<RawEmployeeImportRow> mockRows = List.of(
                new RawEmployeeImportRow(2, "EMP001", "Nguyen Van A", "an.nguyen", "an.nguyen@test.com", "Trung tâm Phần mềm", "VT-04", "Developer", 40, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), false),
                new RawEmployeeImportRow(3, "EMP002", "Tran Thi B", "binh.tran", "binh.tran@test.com", "10", "VT-04", "QA", 40, LocalDate.of(2026, 2, 1), null, false)
        );

        when(fileParser.parse(any(InputStream.class))).thenReturn(mockRows);

        InputStream is = new ByteArrayInputStream(new byte[0]);
        ImportEmployeePreviewResult result = service.preview(is, "test.xlsx");

        assertNotNull(result);
        assertEquals(2, result.totalRows());
        assertEquals(2, result.validRows());
        assertEquals(0, result.invalidRows());
        assertTrue(result.canProceed());
        assertTrue(result.rows().get(0).valid());
        assertEquals(10L, result.rows().get(0).resolvedOrgUnitId());
    }

    @Test
    @DisplayName("NCL-12-CN-004-TC-02: Preview dữ liệu có dòng không hợp lệ -> Đánh dấu chi tiết lỗi từng dòng")
    void testPreview_WithInvalidRows_MarksErrors() {
        List<RawEmployeeImportRow> mockRows = List.of(
                new RawEmployeeImportRow(2, "EMP001", "Nguyen Van A", "an.nguyen", "an.nguyen@test.com", "Trung tâm Phần mềm", "VT-04", "Dev", 40, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31), false),
                new RawEmployeeImportRow(3, "EMP002", "Tran Thi B", "binh.tran", "invalid-email", "Trung tâm Phần mềm", "VT-04", "QA", 40, LocalDate.of(2026, 1, 1), null, false),
                new RawEmployeeImportRow(4, "EMP003", "Le Van C", "cuong.le", "cuong@test.com", "Phòng Không Tồn Tại", "VT-04", "Dev", 40, LocalDate.of(2026, 1, 1), null, false),
                new RawEmployeeImportRow(5, "EMP001", "Nguyen Van Duplicate", "dup.user", "dup@test.com", "Trung tâm Phần mềm", "VT-04", "Dev", 40, LocalDate.of(2026, 1, 1), null, false),
                new RawEmployeeImportRow(6, "EMP005", "Pham Thi E", "e.pham", "e@test.com", "Trung tâm Phần mềm", "VT-04", "Dev", 200, LocalDate.of(2026, 1, 1), null, false)
        );

        when(fileParser.parse(any(InputStream.class))).thenReturn(mockRows);

        InputStream is = new ByteArrayInputStream(new byte[0]);
        ImportEmployeePreviewResult result = service.preview(is, "test.xlsx");

        assertNotNull(result);
        assertEquals(5, result.totalRows());
        assertEquals(1, result.validRows()); // Dòng 1 hợp lệ
        assertEquals(4, result.invalidRows()); // 4 dòng lỗi

        // Dòng 2: email sai
        ImportEmployeeRowDto row2 = result.rows().get(1);
        assertFalse(row2.valid());
        assertTrue(row2.errors().stream().anyMatch(e -> e.contains("không đúng định dạng")));

        // Dòng 3: phòng ban không tồn tại
        ImportEmployeeRowDto row3 = result.rows().get(2);
        assertFalse(row3.valid());
        assertTrue(row3.errors().stream().anyMatch(e -> e.contains("không tồn tại")));

        // Dòng 4: trùng mã nhân viên
        ImportEmployeeRowDto row4 = result.rows().get(3);
        assertFalse(row4.valid());
        assertTrue(row4.errors().stream().anyMatch(e -> e.contains("bị trùng lặp trong tệp")));

        // Dòng 5: giờ chuẩn > 168h
        ImportEmployeeRowDto row5 = result.rows().get(4);
        assertFalse(row5.valid());
        assertTrue(row5.errors().stream().anyMatch(e -> e.contains("không vượt quá 168h")));
    }

    @Test
    @DisplayName("NCL-12-CN-004-TC-01 & TC-02: Xác nhận nhập từng phần (Partial Import) -> Chỉ lưu các dòng hợp lệ")
    void testConfirm_PartialImport_SavesValidRows() {
        ImportEmployeeRowDto validRow = new ImportEmployeeRowDto(
                2, "EMP001", "Nguyen Van A", "an.nguyen", "an.nguyen@test.com",
                "Trung tâm Phần mềm", 10L, "Trung tâm Phần mềm", "VT-04", "Developer",
                40, LocalDate.of(2026, 1, 1), null, false, true, List.of()
        );

        ImportEmployeeRowDto invalidRow = new ImportEmployeeRowDto(
                3, "EMP002", "Tran Thi B", "binh.tran", "invalid-email",
                "Trung tâm Phần mềm", 10L, "Trung tâm Phần mềm", "VT-04", "QA",
                40, null, null, false, false, List.of("Email không đúng định dạng")
        );

        Role role = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn");
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(role));

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(new UserId(100L));
        when(saveUserPort.save(any(User.class))).thenReturn(mockUser);

        Employee mockEmp = mock(Employee.class);
        when(mockEmp.getId()).thenReturn(new EmployeeId(200L));
        when(saveEmployeePort.save(any(Employee.class))).thenReturn(mockEmp);

        ConfirmEmployeeImportCommand command = new ConfirmEmployeeImportCommand(List.of(validRow, invalidRow));
        ImportExecutionResult result = service.confirm(command);

        assertNotNull(result);
        assertEquals(1, result.importedCount());
        assertEquals(0, result.skippedCount());
        verify(saveUserPort, times(2)).save(any(User.class));
        verify(saveEmployeePort).save(any(Employee.class));
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-12-CN-004-TC-03: Người dùng không có quyền Quản trị viên -> Ném PermissionDeniedException")
    void testSecurity_UnauthorizedUser_ThrowsException() {
        when(authorizationService.require(PermissionCode.DATA_IMPORT))
                .thenThrow(new PermissionDeniedException(PermissionCode.DATA_IMPORT));

        InputStream is = new ByteArrayInputStream(new byte[0]);
        assertThrows(PermissionDeniedException.class, () -> service.preview(is, "test.xlsx"));
        assertThrows(PermissionDeniedException.class, () -> service.confirm(new ConfirmEmployeeImportCommand(List.of())));
    }

    @Test
    @DisplayName("Định dạng tệp không có parser hỗ trợ -> Ném DataImportException")
    void testPreview_UnsupportedExtension_ThrowsException() {
        when(fileParser.supports("test.unsupported")).thenReturn(false);

        InputStream is = new ByteArrayInputStream(new byte[0]);
        assertThrows(DataImportException.class, () -> service.preview(is, "test.unsupported"));
    }

    @Test
    @DisplayName("Import VT-03 (Quản lý nguồn lực) -> Gán đúng scopeOrgUnitId cho User")
    void testConfirm_VT03Role_SetsScopeOrgUnitId() {
        ImportEmployeeRowDto vt03Row = new ImportEmployeeRowDto(
                2, "EMP003", "Bui Quang Long", "long.bui", "long.bui@test.com",
                "Trung tâm Phần mềm", 10L, "Trung tâm Phần mềm", "VT-03", "Resource Manager",
                40, LocalDate.of(2026, 1, 1), null, false, true, List.of()
        );

        Role role = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        when(loadRolePort.findByCode(RoleCode.VT_03)).thenReturn(Optional.of(role));

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(new UserId(103L));
        when(saveUserPort.save(any(User.class))).thenReturn(mockUser);

        Employee mockEmp = mock(Employee.class);
        when(mockEmp.getId()).thenReturn(new EmployeeId(203L));
        when(saveEmployeePort.save(any(Employee.class))).thenReturn(mockEmp);

        ConfirmEmployeeImportCommand command = new ConfirmEmployeeImportCommand(List.of(vt03Row));
        ImportExecutionResult result = service.confirm(command);

        assertNotNull(result);
        assertEquals(1, result.importedCount());
        verify(saveUserPort, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Tải template mẫu gọi generator chính xác")
    void testGenerateTemplate() {
        when(templateGenerator.generateTemplate()).thenReturn(new byte[]{1, 2, 3});

        byte[] template = service.generateEmployeeTemplate("xlsx");
        assertNotNull(template);
        assertEquals(3, template.length);
    }
}
