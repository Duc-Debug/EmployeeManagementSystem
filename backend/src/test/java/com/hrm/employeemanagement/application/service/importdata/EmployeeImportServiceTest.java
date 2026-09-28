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
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.importdata.ConfirmEmployeeImportCommand;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeePreviewResult;
import com.hrm.employeemanagement.application.dto.importdata.ImportEmployeeRowDto;
import com.hrm.employeemanagement.application.dto.importdata.ImportExecutionResult;
import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeDataFileParser;
import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeImportTemplateGenerator;
import com.hrm.employeemanagement.application.port.outbound.importdata.SingleRowEmployeeImportPort;
import com.hrm.employeemanagement.application.port.outbound.importdata.SingleRowImportResult;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.importdata.DataImportException;
import com.hrm.employeemanagement.domain.importdata.RawEmployeeImportRow;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;

@DisplayName("EmployeeImportService Unit Tests (NCL-12-CN-004)")
class EmployeeImportServiceTest {

    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadRolePort loadRolePort;
    private LoadEmployeePort loadEmployeePort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private PasswordEncoderPort passwordEncoder;
    private SaveAuditLogPort saveAuditLogPort;
    private SingleRowEmployeeImportPort singleRowImportPort;
    private EmployeeDataFileParser fileParser;
    private EmployeeImportTemplateGenerator templateGenerator;

    private EmployeeImportService service;
    private final Long ADMIN_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadRolePort = mock(LoadRolePort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        passwordEncoder = mock(PasswordEncoderPort.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);
        singleRowImportPort = mock(SingleRowEmployeeImportPort.class);
        fileParser = mock(EmployeeDataFileParser.class);
        templateGenerator = mock(EmployeeImportTemplateGenerator.class);

        when(fileParser.supports(anyString())).thenReturn(true);
        when(templateGenerator.supports(anyString())).thenReturn(true);

        service = new EmployeeImportService(
                authorizationService,
                loadUserPort,
                loadRolePort,
                loadEmployeePort,
                loadOrgUnitPort,
                passwordEncoder,
                saveAuditLogPort,
                singleRowImportPort,
                List.of(fileParser),
                List.of(templateGenerator)
        );

        when(authorizationService.require(PermissionCode.DATA_IMPORT)).thenReturn(ADMIN_USER_ID);
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash_" + invocation.getArgument(0));

        OrgUnit softwareCenter = mock(OrgUnit.class);
        when(softwareCenter.getId()).thenReturn(new OrgUnitId(10L));
        when(softwareCenter.getUnitName()).thenReturn("Trung tâm Phần mềm");
        when(loadOrgUnitPort.findAllActive()).thenReturn(List.of(softwareCenter));
    }

    @Test
    @DisplayName("NCL-12-CN-004-TC-01: Preview dữ liệu hợp lệ -> Thành công 100%")
    void testPreview_SuccessAllValid() {
        List<RawEmployeeImportRow> mockRows = List.of(
                new RawEmployeeImportRow(2, "EMP001", "Nguyen Van A", "an.nguyen", "an.nguyen@test.com", "Trung tâm Phần mềm", "VT-04", "Developer", "40", "2026-01-01", "2027-12-31", "FALSE"),
                new RawEmployeeImportRow(3, "EMP002", "Tran Thi B", "binh.tran", "binh.tran@test.com", "10", "VT-04", "QA", null, "01/02/2026", null, "Có")
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
        assertEquals(40, result.rows().get(1).standardHoursPerWeek()); // ô trống giờ chuẩn -> mặc định 40
        assertTrue(result.rows().get(1).isOutsourced()); // "Có" -> true
    }

    @Test
    @DisplayName("NCL-12-CN-004-TC-02: Preview phát hiện lỗi dữ liệu & quy tắc bảo mật Role")
    void testPreview_WithInvalidRows_MarksErrors() {
        List<RawEmployeeImportRow> mockRows = List.of(
                new RawEmployeeImportRow(2, "EMP001", "Nguyen Van A", "an.nguyen", "an.nguyen@test.com", "Trung tâm Phần mềm", "VT-04", "Dev", "40", "2026-01-01", "2027-12-31", "FALSE"),
                new RawEmployeeImportRow(3, "EMP002", "Tran Thi B", "binh.tran", "invalid-email", "Trung tâm Phần mềm", "VT-04", "QA", "abc", "2026-01-01", null, "FALSE"),
                new RawEmployeeImportRow(4, "EMP003", "Le Van C", "cuong.le", "cuong@test.com", "Phòng Không Tồn Tại", "VT-04", "Dev", "40", "invalid-date", null, "FALSE"),
                new RawEmployeeImportRow(5, "EMP001", "Nguyen Van Duplicate", "dup.user", "dup@test.com", "Trung tâm Phần mềm", "VT-06", "Dev", "40", "2026-01-01", null, "invalid-bool"),
                new RawEmployeeImportRow(6, "EMP005", "Pham Thi E", "e.pham", "e@test.com", "Trung tâm Phần mềm", "VT-04", "Dev", "200", "2026-01-01", null, "FALSE")
        );

        when(fileParser.parse(any(InputStream.class))).thenReturn(mockRows);

        InputStream is = new ByteArrayInputStream(new byte[0]);
        ImportEmployeePreviewResult result = service.preview(is, "test.xlsx");

        assertNotNull(result);
        assertEquals(5, result.totalRows());
        assertEquals(1, result.validRows());
        assertEquals(4, result.invalidRows());

        // Dòng 2: email sai định dạng & giờ chuẩn không đúng định dạng số
        ImportEmployeeRowDto row2 = result.rows().get(1);
        assertFalse(row2.valid());
        assertTrue(row2.errors().stream().anyMatch(e -> e.contains("không đúng định dạng")));

        // Dòng 3: phòng ban không tồn tại & ngày bắt đầu sai định dạng
        ImportEmployeeRowDto row3 = result.rows().get(2);
        assertFalse(row3.valid());
        assertTrue(row3.errors().stream().anyMatch(e -> e.contains("không tồn tại")));
        assertTrue(row3.errors().stream().anyMatch(e -> e.contains("Ngày bắt đầu")));

        // Dòng 4: trùng mã nhân viên & cấm import VT-06 & boolean sai
        ImportEmployeeRowDto row4 = result.rows().get(3);
        assertFalse(row4.valid());
        assertTrue(row4.errors().stream().anyMatch(e -> e.contains("bị trùng lặp trong tệp")));
        assertTrue(row4.errors().stream().anyMatch(e -> e.contains("Quản trị viên hệ thống (VT-06)")));
        assertTrue(row4.errors().stream().anyMatch(e -> e.contains("Trường thuê ngoài")));

        // Dòng 5: giờ chuẩn > 168h
        ImportEmployeeRowDto row5 = result.rows().get(4);
        assertFalse(row5.valid());
        assertTrue(row5.errors().stream().anyMatch(e -> e.contains("không vượt quá 168h")));
    }

    @Test
    @DisplayName("NCL-12-CN-004: Re-validation tại confirm boundary - Bỏ qua valid=true từ client và bắt lỗi")
    void testConfirm_RevalidatesServerSide_IgnoresClientValidFlag() {
        // DTO giả mạo valid=true nhưng thực chất có email sai và trùng username
        ImportEmployeeRowDto forgedRow = new ImportEmployeeRowDto(
                2, "EMP001", "Forged Admin", "forged.admin", "bad-email-format",
                "Trung tâm Phần mềm", 10L, "Trung tâm Phần mềm", "VT-06", "Hacker",
                40, LocalDate.of(2026, 1, 1), null, false, true, List.of()
        );

        ConfirmEmployeeImportCommand command = new ConfirmEmployeeImportCommand(List.of(forgedRow));
        ImportExecutionResult result = service.confirm(command);

        assertNotNull(result);
        assertEquals(0, result.importedCount());
        assertEquals(1, result.skippedCount());
        assertEquals(1, result.errors().size());
        verify(singleRowImportPort, never()).importSingleRow(anyInt(), anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), anyInt(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("NCL-12-CN-004: Xác nhận nhập và sinh mật khẩu ngẫu nhiên an toàn (SecureRandom 16 ký tự)")
    void testConfirm_GeneratesSecureRandomPassword() {
        ImportEmployeeRowDto validRow = new ImportEmployeeRowDto(
                2, "EMP001", "Nguyen Van A", "an.nguyen", "an.nguyen@test.com",
                "Trung tâm Phần mềm", 10L, "Trung tâm Phần mềm", "VT-04", "Developer",
                40, LocalDate.of(2026, 1, 1), null, false, true, List.of()
        );

        Role role = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn");
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(role));
        when(singleRowImportPort.importSingleRow(anyInt(), anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), anyInt(), any(), any(), anyBoolean()))
                .thenReturn(SingleRowImportResult.ofSuccess());

        ConfirmEmployeeImportCommand command = new ConfirmEmployeeImportCommand(List.of(validRow));
        ImportExecutionResult result = service.confirm(command);

        assertEquals(1, result.importedCount());
        assertEquals(0, result.skippedCount());

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());
        String generatedPassword = passwordCaptor.getValue();
        assertNotNull(generatedPassword);
        assertEquals(16, generatedPassword.length());
        assertFalse(generatedPassword.equals("Password@123"), "Mật khẩu không được là hardcoded Password@123");
    }

    @Test
    @DisplayName("NCL-12-CN-004: Partial Import cô lập - Dòng lỗi không ảnh hưởng dòng thành công")
    void testConfirm_PartialImport_IsolatedResults() {
        ImportEmployeeRowDto validRow1 = new ImportEmployeeRowDto(
                2, "EMP001", "Nguyen Van A", "an.nguyen", "an.nguyen@test.com",
                "Trung tâm Phần mềm", 10L, "Trung tâm Phần mềm", "VT-04", "Developer",
                40, LocalDate.of(2026, 1, 1), null, false, true, List.of()
        );
        ImportEmployeeRowDto validRow2 = new ImportEmployeeRowDto(
                3, "EMP002", "Tran Thi B", "binh.tran", "binh.tran@test.com",
                "Trung tâm Phần mềm", 10L, "Trung tâm Phần mềm", "VT-04", "QA",
                40, LocalDate.of(2026, 1, 1), null, false, true, List.of()
        );

        Role role = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn");
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(role));

        // Row 1 thành công, Row 2 gặp xung đột CSDL
        when(singleRowImportPort.importSingleRow(eq(2), eq("EMP001"), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), anyInt(), any(), any(), anyBoolean()))
                .thenReturn(SingleRowImportResult.ofSuccess());
        when(singleRowImportPort.importSingleRow(eq(3), eq("EMP002"), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), anyInt(), any(), any(), anyBoolean()))
                .thenReturn(SingleRowImportResult.ofFailure("Dòng 3 (EMP002): Xung đột dữ liệu"));

        ConfirmEmployeeImportCommand command = new ConfirmEmployeeImportCommand(List.of(validRow1, validRow2));
        ImportExecutionResult result = service.confirm(command);

        assertEquals(1, result.importedCount());
        assertEquals(1, result.skippedCount());
        assertEquals(1, result.errors().size());
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-12-CN-004: Gán scopeOrgUnitId cho VT-03 (Quản lý nguồn lực) và null cho vai trò khác")
    void testConfirm_VT03Role_SetsScopeOrgUnitId() {
        ImportEmployeeRowDto vt03Row = new ImportEmployeeRowDto(
                2, "EMP003", "Bui Quang Long", "long.bui", "long.bui@test.com",
                "Trung tâm Phần mềm", 10L, "Trung tâm Phần mềm", "VT-03", "Resource Manager",
                40, LocalDate.of(2026, 1, 1), null, false, true, List.of()
        );

        Role role = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        when(loadRolePort.findByCode(RoleCode.VT_03)).thenReturn(Optional.of(role));
        when(singleRowImportPort.importSingleRow(anyInt(), anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any(), any(), anyInt(), any(), any(), anyBoolean()))
                .thenReturn(SingleRowImportResult.ofSuccess());

        ConfirmEmployeeImportCommand command = new ConfirmEmployeeImportCommand(List.of(vt03Row));
        ImportExecutionResult result = service.confirm(command);

        assertEquals(1, result.importedCount());
        verify(singleRowImportPort).importSingleRow(
                eq(2), eq("EMP003"), eq("Bui Quang Long"), eq("long.bui"),
                anyString(), eq("long.bui@test.com"), eq(10L), eq(role),
                eq(10L), eq("Resource Manager"), eq(40), eq(LocalDate.of(2026, 1, 1)),
                isNull(), eq(false)
        );
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
    @DisplayName("Tải template mẫu gọi generator chính xác")
    void testGenerateTemplate() {
        when(templateGenerator.generateTemplate()).thenReturn(new byte[]{1, 2, 3});

        byte[] template = service.generateEmployeeTemplate("xlsx");
        assertNotNull(template);
        assertEquals(3, template.length);
    }
}