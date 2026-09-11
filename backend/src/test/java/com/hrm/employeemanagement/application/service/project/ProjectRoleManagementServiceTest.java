package com.hrm.employeemanagement.application.service.project;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.demand.CreateProjectRoleCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleUsageResult;
import com.hrm.employeemanagement.application.dto.project.demand.UpdateProjectRoleCommand;
import com.hrm.employeemanagement.application.port.outbound.project.CountProjectRoleUsagePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SyncEmployeeProfessionalRolePort;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillGroupPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleCodeException;
import com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleNameException;
import com.hrm.employeemanagement.domain.exception.role.InvalidProjectRoleDataException;
import com.hrm.employeemanagement.domain.exception.role.InvalidProjectRoleStateException;
import com.hrm.employeemanagement.domain.exception.role.RoleNotFoundException;
import com.hrm.employeemanagement.domain.exception.skill.SkillGroupNotFoundException;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleStatus;
import com.hrm.employeemanagement.domain.skill.SkillGroup;
import com.hrm.employeemanagement.domain.skill.SkillGroupId;
import com.hrm.employeemanagement.domain.skill.SkillStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("NCL-12-CN-001: Quản lý danh mục vai trò chuyên môn Unit Tests")
class ProjectRoleManagementServiceTest {

    private static final Long ADMIN_USER_ID = 1L;
    private static final Long SKILL_GROUP_ID = 10L;

    @Mock
    private LoadProjectRolePort loadProjectRolePort;

    @Mock
    private SaveProjectRolePort saveProjectRolePort;

    @Mock
    private CountProjectRoleUsagePort countUsagePort;

    @Mock
    private LoadSkillGroupPort loadSkillGroupPort;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private SyncEmployeeProfessionalRolePort syncEmployeeProfessionalRolePort;

    private ProjectRoleManagementService service;

    @BeforeEach
    void setUp() {
        service = new ProjectRoleManagementService(
                loadProjectRolePort,
                saveProjectRolePort,
                countUsagePort,
                loadSkillGroupPort,
                authorizationService,
                saveAuditLogPort,
                syncEmployeeProfessionalRolePort);
    }

    @Test
    @DisplayName("NCL-12-CN-001-TC-01: Thêm vai trò chuyên môn mới kèm nhóm kỹ năng thành công")
    void tc01_createProjectRole_success() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);
        when(loadProjectRolePort.existsByCodeIgnoreCase("SEC")).thenReturn(false);
        when(loadProjectRolePort.existsByNameIgnoreCase("Security Engineer")).thenReturn(false);

        SkillGroup group = new SkillGroup(
                new SkillGroupId(SKILL_GROUP_ID), "DevOps & Cloud", "Mô tả", SkillStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadSkillGroupPort.findById(new SkillGroupId(SKILL_GROUP_ID))).thenReturn(Optional.of(group));

        ProjectRole saved = new ProjectRole(
                new ProjectRoleId(100L), "SEC", "Security Engineer", "Bảo mật",
                SKILL_GROUP_ID, "DevOps & Cloud", ProjectRoleStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(saveProjectRolePort.save(any(ProjectRole.class))).thenReturn(saved);

        CreateProjectRoleCommand command = new CreateProjectRoleCommand("SEC", "Security Engineer", "Bảo mật", SKILL_GROUP_ID);
        ProjectRoleResult result = service.createProjectRole(command);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.code()).isEqualTo("SEC");
        assertThat(result.name()).isEqualTo("Security Engineer");
        assertThat(result.skillGroupId()).isEqualTo(SKILL_GROUP_ID);
        assertThat(result.skillGroupName()).isEqualTo("DevOps & Cloud");
        assertThat(result.status()).isEqualTo("ACTIVE");

        // TC-04: Lưu lịch sử audit
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertThat(audit.getUserId()).isEqualTo(ADMIN_USER_ID);
        assertThat(audit.getAction()).isEqualTo("PROJECT_ROLE_CREATED");
        assertThat(audit.getTableName()).isEqualTo("project_roles");
        assertThat(audit.getRecordId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("NCL-12-CN-001-TC-02: Ngừng dùng vai trò đang được dùng trong dự án -> Cảnh báo và chuyển INACTIVE (không xóa vật lý)")
    void tc02_deactivateProjectRole_whenInUse_shouldDeactivateAndNotDelete() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);

        ProjectRole existing = new ProjectRole(
                new ProjectRoleId(5L), "DEV", "Lập trình viên", "Phát triển code",
                SKILL_GROUP_ID, "Software Dev", ProjectRoleStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadProjectRolePort.findById(new ProjectRoleId(5L))).thenReturn(Optional.of(existing));

        // Kiểm tra usage trước khi ngừng sử dụng
        when(countUsagePort.countDemandsByRoleId(5L)).thenReturn(8L);
        when(countUsagePort.countEmployeesByProfessionalRole("Lập trình viên", "DEV")).thenReturn(3L);

        ProjectRoleUsageResult usage = service.checkUsage(5L);
        assertThat(usage.inUse()).isTrue();
        assertThat(usage.demandCount()).isEqualTo(8L);
        assertThat(usage.employeeCount()).isEqualTo(3L);
        assertThat(usage.warningMessage()).contains("8 nhu cầu dự án");

        // Khi xác nhận deactivate
        when(saveProjectRolePort.save(any(ProjectRole.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectRoleResult result = service.deactivateProjectRole(5L);

        assertThat(result.status()).isEqualTo("INACTIVE");
        assertThat(existing.getStatus()).isEqualTo(ProjectRoleStatus.INACTIVE);

        // Đảm bảo ghi audit log DEACTIVATED
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertThat(audit.getAction()).isEqualTo("PROJECT_ROLE_DEACTIVATED");
        assertThat(audit.getRecordId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("NCL-12-CN-001-TC-03: Không có quyền quản trị -> Từ chối truy cập (PermissionDeniedException)")
    void tc03_nonAdmin_shouldThrowPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.PROJECT_ROLE_MANAGE));

        CreateProjectRoleCommand command = new CreateProjectRoleCommand("TEST_ROLE", "Tên vai trò", "Mô tả", SKILL_GROUP_ID);

        assertThatThrownBy(() -> service.createProjectRole(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(saveProjectRolePort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-12-CN-001-TC-04: Cập nhật thông tin vai trò chuyên môn ghi nhận audit log đầy đủ oldValue và newValue")
    void tc04_updateProjectRole_shouldSaveAuditLogWithChanges() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);

        ProjectRole existing = new ProjectRole(
                new ProjectRoleId(10L), "BA", "Chuyên viên BA cũ", "Mô tả cũ",
                SKILL_GROUP_ID, "Business Analysis", ProjectRoleStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadProjectRolePort.findById(new ProjectRoleId(10L))).thenReturn(Optional.of(existing));
        when(loadProjectRolePort.existsByNameIgnoreCaseAndIdNot("Chuyên viên BA mới", 10L)).thenReturn(false);

        SkillGroup group = new SkillGroup(
                new SkillGroupId(SKILL_GROUP_ID), "Business Analysis", "Mô tả", SkillStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadSkillGroupPort.findById(new SkillGroupId(SKILL_GROUP_ID))).thenReturn(Optional.of(group));
        when(saveProjectRolePort.save(any(ProjectRole.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProjectRoleCommand updateCommand = new UpdateProjectRoleCommand(
                10L, "Chuyên viên BA mới", "Mô tả mới cập nhật", SKILL_GROUP_ID);
        ProjectRoleResult result = service.updateProjectRole(updateCommand);

        assertThat(result.name()).isEqualTo("Chuyên viên BA mới");
        assertThat(result.description()).isEqualTo("Mô tả mới cập nhật");

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertThat(audit.getAction()).isEqualTo("PROJECT_ROLE_UPDATED");
        assertThat(audit.getOldValue()).contains("Chuyên viên BA cũ");
        assertThat(audit.getNewValue()).contains("Chuyên viên BA mới");
    }

    @Test
    @DisplayName("Validation: Trùng mã vai trò ném DuplicateProjectRoleCodeException")
    void createProjectRole_duplicateCode_throwsException() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);
        when(loadProjectRolePort.existsByCodeIgnoreCase("DEV")).thenReturn(true);

        CreateProjectRoleCommand command = new CreateProjectRoleCommand("DEV", "Lập trình", "Mô tả", SKILL_GROUP_ID);

        assertThatThrownBy(() -> service.createProjectRole(command))
                .isInstanceOf(DuplicateProjectRoleCodeException.class)
                .hasMessageContaining("DEV");
    }

    @Test
    @DisplayName("Validation: Trùng tên vai trò ném DuplicateProjectRoleNameException")
    void createProjectRole_duplicateName_throwsException() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);
        when(loadProjectRolePort.existsByCodeIgnoreCase("NEW_DEV")).thenReturn(false);
        when(loadProjectRolePort.existsByNameIgnoreCase("Lập trình")).thenReturn(true);

        CreateProjectRoleCommand command = new CreateProjectRoleCommand("NEW_DEV", "Lập trình", "Mô tả", SKILL_GROUP_ID);

        assertThatThrownBy(() -> service.createProjectRole(command))
                .isInstanceOf(DuplicateProjectRoleNameException.class)
                .hasMessageContaining("Lập trình");
    }

    @Test
    @DisplayName("Validation: Nhóm kỹ năng không tồn tại ném SkillGroupNotFoundException")
    void createProjectRole_skillGroupNotFound_throwsException() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);
        when(loadProjectRolePort.existsByCodeIgnoreCase("TEST_CODE")).thenReturn(false);
        when(loadProjectRolePort.existsByNameIgnoreCase("Test Name")).thenReturn(false);
        when(loadSkillGroupPort.findById(new SkillGroupId(999L))).thenReturn(Optional.empty());

        CreateProjectRoleCommand command = new CreateProjectRoleCommand("TEST_CODE", "Test Name", "Mô tả", 999L);

        assertThatThrownBy(() -> service.createProjectRole(command))
                .isInstanceOf(SkillGroupNotFoundException.class);
    }

    @Test
    @DisplayName("Validation: Nhóm kỹ năng đã bị INACTIVE ném InvalidProjectRoleDataException")
    void createProjectRole_skillGroupInactive_throwsException() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);
        when(loadProjectRolePort.existsByCodeIgnoreCase("TEST_CODE")).thenReturn(false);
        when(loadProjectRolePort.existsByNameIgnoreCase("Test Name")).thenReturn(false);

        SkillGroup inactiveGroup = new SkillGroup(
                new SkillGroupId(SKILL_GROUP_ID), "Inactive Group", "Mô tả", SkillStatus.INACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadSkillGroupPort.findById(new SkillGroupId(SKILL_GROUP_ID))).thenReturn(Optional.of(inactiveGroup));

        CreateProjectRoleCommand command = new CreateProjectRoleCommand("TEST_CODE", "Test Name", "Mô tả", SKILL_GROUP_ID);

        assertThatThrownBy(() -> service.createProjectRole(command))
                .isInstanceOf(InvalidProjectRoleDataException.class)
                .hasMessageContaining("ngừng hoạt động");
    }

    @Test
    @DisplayName("Kích hoạt lại vai trò (Reactivate) thành công")
    void activateProjectRole_success() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);

        ProjectRole inactiveRole = new ProjectRole(
                new ProjectRoleId(20L), "QA", "QA Engineer", "Kiểm thử",
                SKILL_GROUP_ID, "Testing", ProjectRoleStatus.INACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadProjectRolePort.findById(new ProjectRoleId(20L))).thenReturn(Optional.of(inactiveRole));

        SkillGroup group = new SkillGroup(
                new SkillGroupId(SKILL_GROUP_ID), "Testing", "Mô tả", SkillStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadSkillGroupPort.findById(new SkillGroupId(SKILL_GROUP_ID))).thenReturn(Optional.of(group));
        when(saveProjectRolePort.save(any(ProjectRole.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectRoleResult result = service.activateProjectRole(20L);

        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(inactiveRole.getStatus()).isEqualTo(ProjectRoleStatus.ACTIVE);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertThat(audit.getAction()).isEqualTo("PROJECT_ROLE_ACTIVATED");
    }

    @Test
    @DisplayName("getProjectRoles lọc vai trò active hoặc trả về toàn bộ khi includeInactive=true")
    void getProjectRoles_filtersCorrectly() {
        ProjectRole active1 = new ProjectRole(new ProjectRoleId(1L), "DEV", "Developer", "Lập trình", 1L, null, ProjectRoleStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        ProjectRole inactive2 = new ProjectRole(new ProjectRoleId(2L), "OLD", "Old Role", "Cũ", 1L, null, ProjectRoleStatus.INACTIVE, LocalDateTime.now(), LocalDateTime.now());

        SkillGroup group = new SkillGroup(new SkillGroupId(1L), "General", "Chung", SkillStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        when(loadSkillGroupPort.findAll()).thenReturn(List.of(group));

        // Khi includeInactive = false (mặc định)
        when(loadProjectRolePort.findAllActive()).thenReturn(List.of(active1));
        List<ProjectRoleResult> activeOnly = service.getProjectRoles(false);
        assertThat(activeOnly).hasSize(1);
        assertThat(activeOnly.get(0).code()).isEqualTo("DEV");

        // Khi includeInactive = true
        when(loadProjectRolePort.findAll()).thenReturn(List.of(active1, inactive2));
        List<ProjectRoleResult> allRoles = service.getProjectRoles(true);
        assertThat(allRoles).hasSize(2);
    }

    @Test
    @DisplayName("MEDIUM-01: Ngừng sử dụng vai trò đã INACTIVE ném InvalidProjectRoleStateException")
    void deactivateRole_whenAlreadyInactive_throwsInvalidProjectRoleStateException() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);

        ProjectRole alreadyInactive = new ProjectRole(
                new ProjectRoleId(5L), "OLD_DEV", "Developer Cũ", "Mô tả",
                SKILL_GROUP_ID, "Engineering", ProjectRoleStatus.INACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadProjectRolePort.findById(new ProjectRoleId(5L))).thenReturn(Optional.of(alreadyInactive));

        assertThatThrownBy(() -> service.deactivateProjectRole(5L))
                .isInstanceOf(InvalidProjectRoleStateException.class)
                .hasMessageContaining("ngừng sử dụng");

        verify(saveProjectRolePort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("MEDIUM-01: Kích hoạt lại vai trò đang ACTIVE ném InvalidProjectRoleStateException")
    void activateRole_whenAlreadyActive_throwsInvalidProjectRoleStateException() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);

        ProjectRole alreadyActive = new ProjectRole(
                new ProjectRoleId(5L), "ACTIVE_DEV", "Developer Active", "Mô tả",
                SKILL_GROUP_ID, "Engineering", ProjectRoleStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadProjectRolePort.findById(new ProjectRoleId(5L))).thenReturn(Optional.of(alreadyActive));

        SkillGroup group = new SkillGroup(
                new SkillGroupId(SKILL_GROUP_ID), "Engineering", "Mô tả", SkillStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadSkillGroupPort.findById(new SkillGroupId(SKILL_GROUP_ID))).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.activateProjectRole(5L))
                .isInstanceOf(InvalidProjectRoleStateException.class)
                .hasMessageContaining("đang hoạt động");

        verify(saveProjectRolePort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("HIGH-01: Đổi tên vai trò chuyên môn tự động đồng bộ sang bảng employees")
    void updateProjectRole_whenNameChanges_shouldSyncEmployeeProfessionalRole() {
        when(authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE)).thenReturn(ADMIN_USER_ID);

        ProjectRole role = new ProjectRole(
                new ProjectRoleId(5L), "DEV", "Lập trình viên", "Mô tả cũ",
                SKILL_GROUP_ID, "Engineering", ProjectRoleStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadProjectRolePort.findById(new ProjectRoleId(5L))).thenReturn(Optional.of(role));
        when(loadProjectRolePort.existsByNameIgnoreCaseAndIdNot("Senior Software Engineer", 5L)).thenReturn(false);

        SkillGroup group = new SkillGroup(
                new SkillGroupId(SKILL_GROUP_ID), "Engineering", "Mô tả", SkillStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
        when(loadSkillGroupPort.findById(new SkillGroupId(SKILL_GROUP_ID))).thenReturn(Optional.of(group));
        when(saveProjectRolePort.save(any(ProjectRole.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProjectRoleCommand cmd = new UpdateProjectRoleCommand(5L, "Senior Software Engineer", "Mô tả mới", SKILL_GROUP_ID);
        service.updateProjectRole(cmd);

        // Kiểm tra port đồng bộ tên nhân sự được gọi với oldName và newName
        verify(syncEmployeeProfessionalRolePort).syncRoleName("Lập trình viên", "Senior Software Engineer");
    }

    @Test
    @DisplayName("HIGH-01: checkUsage kiểm tra nhân sự bằng cả roleName và roleCode")
    void checkUsage_shouldQueryByRoleNameAndCode() {
        when(loadProjectRolePort.findById(new ProjectRoleId(5L))).thenReturn(Optional.of(new ProjectRole(
                new ProjectRoleId(5L), "DEV", "Senior Software Engineer", "Mô tả",
                SKILL_GROUP_ID, "Engineering", ProjectRoleStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now()
        )));

        when(countUsagePort.countDemandsByRoleId(5L)).thenReturn(2L);
        when(countUsagePort.countEmployeesByProfessionalRole("Senior Software Engineer", "DEV")).thenReturn(3L);

        ProjectRoleUsageResult result = service.checkUsage(5L);

        assertThat(result.inUse()).isTrue();
        assertThat(result.demandCount()).isEqualTo(2L);
        assertThat(result.employeeCount()).isEqualTo(3L);
        assertThat(result.warningMessage()).contains("2 nhu cầu dự án").contains("3 hồ sơ nhân sự");
    }
}