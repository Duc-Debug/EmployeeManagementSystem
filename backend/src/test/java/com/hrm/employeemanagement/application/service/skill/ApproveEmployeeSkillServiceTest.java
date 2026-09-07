package com.hrm.employeemanagement.application.service.skill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.skill.ApproveEmployeeSkillCommand;
import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.dto.skill.PendingEmployeeSkillItemResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.skill.EmployeeSkillNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.ProficiencyLevel;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApproveEmployeeSkillService Application Tests (NCL-02-CN-006)")
class ApproveEmployeeSkillServiceTest {

    @Mock
    private EmployeeSkillRepository employeeSkillRepository;

    @Mock
    private SkillCatalogRepository skillCatalogRepository;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private SaveAuditLogInNewTransactionPort saveAuditLogPort;

    @Mock
    private AuthorizationService authorizationService;

    private ApproveEmployeeSkillService service;
    private User rmUser;
    private Employee employee;
    private Skill javaSkill;

    @BeforeEach
    void setUp() {
        service = new ApproveEmployeeSkillService(
                employeeSkillRepository,
                skillCatalogRepository,
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                saveAuditLogPort,
                authorizationService
        );

        Role rmRole = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        rmUser = new User(new UserId(2L), "rm_user", "hash", rmRole, UserStatus.ACTIVE, new EmployeeId(2L), DataScope.ORGANIZATION_BRANCH, 10L, 0L);

        employee = new Employee(new EmployeeId(101L), new UserId(5L), 10L, "EMP001", "Nguyễn Văn A", null, null, null, false, 40, EmployeeStatus.ACTIVE);

        javaSkill = new Skill(1L, "JAVA", "Java", "Backend", "Java programming", LocalDateTime.now());
    }

    @Test
    @DisplayName("TC-01: Xác nhận giữ nguyên mức thành thạo tự khai thành công và lưu Audit Log")
    void approveSkill_KeepOriginalProficiency_Success_TC01() {
        Long skillRecordId = 10L;
        EmployeeSkill pendingSkill = EmployeeSkill.declare(101L, 1L, ProficiencyLevel.ADVANCED, new BigDecimal("2.5"));

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_APPROVE)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(employeeSkillRepository.findById(skillRecordId)).thenReturn(Optional.of(pendingSkill));
        when(loadEmployeePort.findById(new EmployeeId(101L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);
        when(employeeSkillRepository.save(any(EmployeeSkill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillCatalogRepository.findById(1L)).thenReturn(Optional.of(javaSkill));

        ApproveEmployeeSkillCommand command = new ApproveEmployeeSkillCommand(
                skillRecordId,
                null,
                "Đồng ý mức tự khai 3/5"
        );

        EmployeeSkillResult result = service.execute(command);

        assertNotNull(result);
        assertEquals("APPROVED", result.status());
        assertEquals(3, result.proficiencyLevel());
        assertEquals("Đồng ý mức tự khai 3/5", result.reviewNotes());
        assertEquals(2L, result.approvedBy());
        assertNotNull(result.approvedAt());

        verify(employeeSkillRepository).save(any(EmployeeSkill.class));

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog savedLog = auditCaptor.getValue();
        assertEquals("APPROVE_SKILL", savedLog.getAction());
        assertEquals("employee_skills", savedLog.getTableName());
    }

    @Test
    @DisplayName("TC-02: Điều chỉnh mức thành thạo kèm ghi chú thành công")
    void approveSkill_AdjustProficiency_Success_TC02() {
        Long skillRecordId = 10L;
        EmployeeSkill pendingSkill = EmployeeSkill.declare(101L, 1L, ProficiencyLevel.INTERMEDIATE, new BigDecimal("3.0"));

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_APPROVE)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(employeeSkillRepository.findById(skillRecordId)).thenReturn(Optional.of(pendingSkill));
        when(loadEmployeePort.findById(new EmployeeId(101L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);
        when(employeeSkillRepository.save(any(EmployeeSkill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(skillCatalogRepository.findById(1L)).thenReturn(Optional.of(javaSkill));

        ApproveEmployeeSkillCommand command = new ApproveEmployeeSkillCommand(
                skillRecordId,
                4, // Điều chỉnh từ 2 lên 4
                "Đã phỏng vấn và kiểm tra qua task thực tế, năng lực đạt mức 4/5"
        );

        EmployeeSkillResult result = service.execute(command);

        assertNotNull(result);
        assertEquals("APPROVED", result.status());
        assertEquals(4, result.proficiencyLevel());
        assertEquals("Đã phỏng vấn và kiểm tra qua task thực tế, năng lực đạt mức 4/5", result.reviewNotes());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog savedLog = auditCaptor.getValue();
        assertEquals("ADJUST_SKILL_PROFICIENCY", savedLog.getAction());
        assertEquals("2", savedLog.getOldValue());
        assertEquals("4", savedLog.getNewValue());
    }

    @Test
    @DisplayName("TC-02 Biên: Điều chỉnh mức thành thạo nhưng bỏ trống ghi chú -> Ném IllegalArgumentException")
    void approveSkill_AdjustWithoutNotes_ThrowsException_TC02() {
        Long skillRecordId = 10L;
        EmployeeSkill pendingSkill = EmployeeSkill.declare(101L, 1L, ProficiencyLevel.INTERMEDIATE, new BigDecimal("3.0"));

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_APPROVE)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(employeeSkillRepository.findById(skillRecordId)).thenReturn(Optional.of(pendingSkill));
        when(loadEmployeePort.findById(new EmployeeId(101L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);

        ApproveEmployeeSkillCommand command = new ApproveEmployeeSkillCommand(
                skillRecordId,
                4,
                "" // Ghi chú trống
        );

        assertThrows(IllegalArgumentException.class, () -> service.execute(command));
        verify(employeeSkillRepository, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-02 Biên: Bản ghi kỹ năng không ở trạng thái PENDING -> Ném IllegalStateException")
    void approveSkill_NotPending_ThrowsIllegalStateException_TC02() {
        Long skillRecordId = 10L;
        EmployeeSkill approvedSkill = new EmployeeSkill(
                skillRecordId, 101L, 1L, ProficiencyLevel.ADVANCED, new BigDecimal("2.0"),
                SkillStatus.APPROVED, 2L, LocalDateTime.now(), null, "Đã duyệt", LocalDateTime.now(), LocalDateTime.now()
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_APPROVE)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(employeeSkillRepository.findById(skillRecordId)).thenReturn(Optional.of(approvedSkill));
        when(loadEmployeePort.findById(new EmployeeId(101L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);

        ApproveEmployeeSkillCommand command = new ApproveEmployeeSkillCommand(
                skillRecordId,
                null,
                "Duyệt lại"
        );

        assertThrows(IllegalStateException.class, () -> service.execute(command));
    }

    @Test
    @DisplayName("TC-03: RM duyệt nhân sự ngoài nhánh quản lý (Data Scope) -> Ném PermissionDeniedException")
    void approveSkill_EmployeeOutOfScope_ThrowsPermissionDenied_TC03() {
        Long skillRecordId = 10L;
        EmployeeSkill pendingSkill = EmployeeSkill.declare(101L, 1L, ProficiencyLevel.ADVANCED, new BigDecimal("2.5"));
        // Nhân viên thuộc đơn vị 20 ngoài nhánh 10 của RM
        Employee outOfScopeEmployee = new Employee(new EmployeeId(101L), new UserId(5L), 20L, "EMP001", "Nguyễn Văn B", null, null, null, false, 40, EmployeeStatus.ACTIVE);

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_APPROVE)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(employeeSkillRepository.findById(skillRecordId)).thenReturn(Optional.of(pendingSkill));
        when(loadEmployeePort.findById(new EmployeeId(101L))).thenReturn(Optional.of(outOfScopeEmployee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(20L, 10L)).thenReturn(false);

        ApproveEmployeeSkillCommand command = new ApproveEmployeeSkillCommand(skillRecordId, null, "Note");

        assertThrows(PermissionDeniedException.class, () -> service.execute(command));
        verify(employeeSkillRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duyệt kỹ năng: Không tìm thấy bản ghi kỹ năng -> Ném EmployeeSkillNotFoundException")
    void approveSkill_NotFound_ThrowsEmployeeSkillNotFoundException() {
        Long skillRecordId = 999L;

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_APPROVE)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(employeeSkillRepository.findById(skillRecordId)).thenReturn(Optional.empty());

        ApproveEmployeeSkillCommand command = new ApproveEmployeeSkillCommand(skillRecordId, null, "Note");

        assertThrows(EmployeeSkillNotFoundException.class, () -> service.execute(command));
        verify(employeeSkillRepository, never()).save(any());
    }

    @Test
    @DisplayName("Xem danh sách kỹ năng chờ xác nhận: Lọc đúng phạm vi và từ khóa")
    void getPendingSkills_FiltersByScopeAndKeyword_Success() {
        EmployeeSkill es1 = EmployeeSkill.declare(101L, 1L, ProficiencyLevel.ADVANCED, new BigDecimal("2.5"));
        OrgUnit orgUnit = new OrgUnit(
                new OrgUnitId(10L), "DEV", "Trung tâm Phát triển",
                com.hrm.employeemanagement.domain.orgunit.OrgUnitType.DEPARTMENT,
                null, "/10/", 1,
                com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus.ACTIVE,
                "Trung tâm", null, LocalDateTime.now(), LocalDateTime.now()
        );

        when(authorizationService.require(PermissionCode.EMPLOYEE_SKILL_APPROVE)).thenReturn(2L);
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(rmUser));
        when(employeeSkillRepository.findByStatus(SkillStatus.PENDING)).thenReturn(List.of(es1));
        when(loadEmployeePort.findById(new EmployeeId(101L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, 10L)).thenReturn(true);
        when(skillCatalogRepository.findById(1L)).thenReturn(Optional.of(javaSkill));
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(orgUnit));

        List<PendingEmployeeSkillItemResult> results = service.execute("Java");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("EMP001", results.get(0).employeeCode());
        assertEquals("Nguyễn Văn A", results.get(0).employeeName());
        assertEquals("Java", results.get(0).skillName());
        assertEquals("Trung tâm Phát triển", results.get(0).orgUnitName());
    }
}
