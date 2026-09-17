package com.hrm.employeemanagement.application.service.scenario;

import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.scenario.ScenarioShareResult;
import com.hrm.employeemanagement.application.dto.scenario.ShareScenarioCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadResourceScenarioPort;
import com.hrm.employeemanagement.application.port.outbound.scenario.LoadScenarioSharePort;
import com.hrm.employeemanagement.application.port.outbound.scenario.SaveScenarioSharePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.application.dto.scenario.ShareCandidateResult;
import com.hrm.employeemanagement.domain.exception.scenario.CorruptedScenarioSnapshotException;
import com.hrm.employeemanagement.domain.exception.scenario.DuplicateScenarioShareException;
import com.hrm.employeemanagement.domain.exception.scenario.InvalidShareRecipientException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotSavedException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioShare;
import com.hrm.employeemanagement.domain.scenario.ScenarioStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScenarioShareService Unit Tests (NCL-08-CN-006)")
class ScenarioShareServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadResourceScenarioPort loadScenarioPort;

    @Mock
    private LoadScenarioSharePort loadScenarioSharePort;

    @Mock
    private SaveScenarioSharePort saveScenarioSharePort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    private ScenarioShareService shareService;

    private final Long ownerUserId = 100L;
    private final Long orgUnitId = 10L;
    private final Long scenarioId = 1L;

    private User ownerUser;
    private ResourceScenario savedScenario;
    private ResourceScenario draftScenario;

    @BeforeEach
    void setUp() {
        shareService = new ScenarioShareService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadOrgUnitPort,
                loadProjectPort,
                loadScenarioPort,
                loadScenarioSharePort,
                saveScenarioSharePort,
                saveAuditLogPort,
                deniedAuditLogPort
        );

        Role vt03Role = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        ownerUser = new User(new UserId(ownerUserId), "rm_user", "hashed", vt03Role, UserStatus.ACTIVE,
                new EmployeeId(1001L), DataScope.ORGANIZATION_BRANCH, orgUnitId, null, null, 1, 1L);

        String snapshotJson = "{\"scenarioId\":1,\"projectIds\":[501,502],\"projectNames\":[\"Project Alpha\",\"Project Beta\"],\"weeklyMetrics\":[],\"employeeSnapshots\":[]}";

        savedScenario = new ResourceScenario(
                scenarioId, "SCN-001", "Kịch bản test", "Mô tả", "Ghi chú", snapshotJson,
                orgUnitId, ScenarioStatus.SAVED, 2026, 38, 8, LocalDateTime.now(),
                ownerUserId, LocalDateTime.now(), LocalDateTime.now(), 1L
        );

        draftScenario = new ResourceScenario(
                scenarioId, "SCN-001", "Kịch bản draft", "Mô tả", null, null,
                orgUnitId, ScenarioStatus.DRAFT, 2026, 38, 8, LocalDateTime.now(),
                ownerUserId, LocalDateTime.now(), LocalDateTime.now(), 1L
        );
    }

    private User createUser(Long userId, String username, RoleCode roleCode, Long scopeOrgUnitId, Long empId) {
        Role role = new Role(new RoleId(roleCode.ordinal() + 1L), roleCode, roleCode.name());
        DataScope dataScope = switch (roleCode) {
            case VT_01 -> DataScope.COMPANY;
            case VT_03 -> DataScope.ORGANIZATION_BRANCH;
            default -> DataScope.SELF;
        };
        Long targetScopeOrgId = (dataScope == DataScope.ORGANIZATION_BRANCH) ? scopeOrgUnitId : null;
        return new User(new UserId(userId), username, "hashed", role, UserStatus.ACTIVE,
                empId != null ? new EmployeeId(empId) : null,
                dataScope, targetScopeOrgId, username + "@example.com", null, 1, 1L);
    }

    private Employee createEmployee(Long empId, Long userId, Long orgId, String code, String name) {
        return new Employee(new EmployeeId(empId), new UserId(userId), orgId, code, name,
                "DEV", null, null, false, 40, EmployeeStatus.ACTIVE);
    }

    @Test
    @DisplayName("BR-05: Share kịch bản DRAFT -> Throw ScenarioNotSavedException")
    void shareScenario_DraftScenario_ThrowsException() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(draftScenario));

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(200L));

        assertThrows(ScenarioNotSavedException.class, () -> shareService.shareScenario(command));
        verify(saveScenarioSharePort, never()).saveAll(any());
    }

    @Test
    @DisplayName("BR-02: User không phải owner cố gắng share -> Throw PermissionDeniedException và ghi audit SCENARIO_ACCESS_DENIED")
    void shareScenario_NotOwner_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(999L);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(200L));

        assertThrows(PermissionDeniedException.class, () -> shareService.shareScenario(command));
        verify(deniedAuditLogPort, times(1)).save(any(AuditLog.class));
        verify(saveScenarioSharePort, never()).saveAll(any());
    }

    @Test
    @DisplayName("BR-03: Share cho VT-01 (Giám đốc) thành công bất kể scope")
    void shareScenario_ValidVT01_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        User vt01 = createUser(201L, "director", RoleCode.VT_01, null, 2001L);
        when(loadUserPort.findById(new UserId(201L))).thenReturn(Optional.of(vt01));
        when(loadScenarioSharePort.hasActiveShare(scenarioId, 201L)).thenReturn(false);
        when(saveScenarioSharePort.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(201L));
        List<ScenarioShareResult> results = shareService.shareScenario(command);

        assertEquals(1, results.size());
        assertEquals(201L, results.get(0).sharedWithUserId());
        assertEquals("VIEW_ONLY", results.get(0).accessLevel());
        verify(saveScenarioSharePort, times(1)).saveAll(any());
        verify(saveAuditLogPort, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("BR-03: Share cho VT-02 (PM) quản lý dự án trong kịch bản -> Thành công")
    void shareScenario_ValidVT02_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        User vt02 = createUser(202L, "pm_user", RoleCode.VT_02, null, 2002L);
        Employee emp02 = createEmployee(2002L, 202L, 999L, "EMP-02", "PM User");
        when(loadUserPort.findById(new UserId(202L))).thenReturn(Optional.of(vt02));
        when(loadEmployeePort.findById(new EmployeeId(2002L))).thenReturn(Optional.of(emp02));
        when(loadScenarioSharePort.hasActiveShare(scenarioId, 202L)).thenReturn(false);
        when(saveScenarioSharePort.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        when(loadProjectPort.findAllManagedProjectIds(2002L)).thenReturn(List.of(501L));

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(202L));
        List<ScenarioShareResult> results = shareService.shareScenario(command);

        assertEquals(1, results.size());
        assertEquals(202L, results.get(0).sharedWithUserId());
    }

    @Test
    @DisplayName("BR-03: Share cho VT-02 KHÔNG quản lý dự án nào trong kịch bản -> Throw InvalidShareRecipientException")
    void shareScenario_VT02NoMatchingProject_ThrowsInvalidRecipient() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        User vt02 = createUser(202L, "pm_user", RoleCode.VT_02, null, 2002L);
        Employee emp02 = createEmployee(2002L, 202L, 999L, "EMP-02", "PM User");
        when(loadUserPort.findById(new UserId(202L))).thenReturn(Optional.of(vt02));
        when(loadEmployeePort.findById(new EmployeeId(2002L))).thenReturn(Optional.of(emp02));

        when(loadProjectPort.findAllManagedProjectIds(2002L)).thenReturn(List.of(999L));

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(202L));
        assertThrows(InvalidShareRecipientException.class, () -> shareService.shareScenario(command));
        verify(saveScenarioSharePort, never()).saveAll(any());
    }

    @Test
    @DisplayName("BR-03: Share cho VT-03 cùng OrgUnit -> Thành công")
    void shareScenario_ValidVT03SameOrgUnit_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        User peerVt03 = createUser(203L, "peer_rm", RoleCode.VT_03, orgUnitId, 2003L);
        when(loadUserPort.findById(new UserId(203L))).thenReturn(Optional.of(peerVt03));
        when(loadScenarioSharePort.hasActiveShare(scenarioId, 203L)).thenReturn(false);
        when(saveScenarioSharePort.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(203L));
        List<ScenarioShareResult> results = shareService.shareScenario(command);

        assertEquals(1, results.size());
        assertEquals(203L, results.get(0).sharedWithUserId());
    }

    @Test
    @DisplayName("BR-03: Share cho VT-03 KHÁC OrgUnit -> Throw InvalidShareRecipientException")
    void shareScenario_VT03DifferentOrgUnit_ThrowsInvalidRecipient() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        User otherOrgVt03 = createUser(203L, "other_rm", RoleCode.VT_03, 999L, 2003L);
        when(loadUserPort.findById(new UserId(203L))).thenReturn(Optional.of(otherOrgVt03));

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(203L));
        assertThrows(InvalidShareRecipientException.class, () -> shareService.shareScenario(command));
        verify(saveScenarioSharePort, never()).saveAll(any());
    }

    @Test
    @DisplayName("BR-03: Share cho VT-04 / VT-05 / VT-06 -> Throw InvalidShareRecipientException")
    void shareScenario_UnauthorizedRole_ThrowsInvalidRecipient() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        User vt04 = createUser(204L, "team_lead", RoleCode.VT_04, null, 2004L);
        when(loadUserPort.findById(new UserId(204L))).thenReturn(Optional.of(vt04));

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(204L));
        assertThrows(InvalidShareRecipientException.class, () -> shareService.shareScenario(command));
        verify(saveScenarioSharePort, never()).saveAll(any());
    }

    @Test
    @DisplayName("BR-10: Batch share gồm 1 user hợp lệ và 1 user không hợp lệ -> Toàn bộ request fail, không user nào được share")
    void shareScenario_AtomicBatchFailure_RollsBackAll() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        User validVt01 = createUser(201L, "director", RoleCode.VT_01, null, 2001L);
        User invalidVt04 = createUser(204L, "dev_lead", RoleCode.VT_04, null, 2004L);

        when(loadUserPort.findById(new UserId(201L))).thenReturn(Optional.of(validVt01));
        when(loadUserPort.findById(new UserId(204L))).thenReturn(Optional.of(invalidVt04));

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(201L, 204L));

        assertThrows(InvalidShareRecipientException.class, () -> shareService.shareScenario(command));
        verify(saveScenarioSharePort, never()).saveAll(any());
        verify(saveAuditLogPort, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("BR-09: Thu hồi chia sẻ -> Đánh dấu revoked_at và ghi audit SCENARIO_UNSHARED")
    void unshareScenario_Success_SoftRevokeAndAudit() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        ScenarioShare activeShare = new ScenarioShare(50L, scenarioId, 201L, ownerUserId, "VIEW_ONLY", LocalDateTime.now(), null, 0L);
        when(loadScenarioSharePort.findActiveShare(scenarioId, 201L)).thenReturn(Optional.of(activeShare));
        when(saveScenarioSharePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        shareService.unshareScenario(scenarioId, 201L);

        assertFalse(activeShare.isActive());
        assertNotNull(activeShare.getRevokedAt());
        verify(saveScenarioSharePort, times(1)).save(activeShare);
        verify(saveAuditLogPort, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Chia sẻ cho người dùng đã có active share -> Throw DuplicateScenarioShareException")
    void shareScenario_AlreadyActive_ThrowsDuplicateException() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        User vt01 = createUser(201L, "director", RoleCode.VT_01, null, 2001L);
        when(loadUserPort.findById(new UserId(201L))).thenReturn(Optional.of(vt01));
        when(loadScenarioSharePort.hasActiveShare(scenarioId, 201L)).thenReturn(true);

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(201L));
        assertThrows(DuplicateScenarioShareException.class, () -> shareService.shareScenario(command));
        verify(saveScenarioSharePort, never()).saveAll(any());
    }

    @Test
    @DisplayName("Đồng thời chia sẻ vi phạm database constraint -> SaveScenarioSharePort ném DuplicateScenarioShareException")
    void shareScenario_ConcurrentDataIntegrityViolation_ThrowsDuplicateException() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        User vt01 = createUser(201L, "director", RoleCode.VT_01, null, 2001L);
        when(loadUserPort.findById(new UserId(201L))).thenReturn(Optional.of(vt01));
        when(loadScenarioSharePort.hasActiveShare(scenarioId, 201L)).thenReturn(false);
        when(saveScenarioSharePort.saveAll(any())).thenThrow(new DuplicateScenarioShareException("Kịch bản đã được chia sẻ cho người dùng trong danh sách này"));

        ShareScenarioCommand command = new ShareScenarioCommand(scenarioId, List.of(201L));
        assertThrows(DuplicateScenarioShareException.class, () -> shareService.shareScenario(command));
    }

    @Test
    @DisplayName("Issue 5: getActiveShares bởi Owner -> Trả về danh sách shares kèm thông tin enrich và sharedByName")
    void getActiveShares_ByOwner_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        ScenarioShare activeShare = new ScenarioShare(50L, scenarioId, 201L, ownerUserId, "VIEW_ONLY", LocalDateTime.now(), null, 0L);
        when(loadScenarioSharePort.findActiveSharesByScenarioId(scenarioId)).thenReturn(List.of(activeShare));

        User recipientUser = createUser(201L, "director", RoleCode.VT_01, null, 2001L);
        User ownerUser = createUser(ownerUserId, "rm_lead", RoleCode.VT_03, orgUnitId, 2003L);
        when(loadUserPort.findById(new UserId(201L))).thenReturn(Optional.of(recipientUser));
        when(loadUserPort.findById(new UserId(ownerUserId))).thenReturn(Optional.of(ownerUser));

        List<ScenarioShareResult> shares = shareService.getActiveShares(scenarioId);
        assertEquals(1, shares.size());
        ScenarioShareResult result = shares.get(0);
        assertEquals(201L, result.sharedWithUserId());
        assertEquals(201L, result.userId());
        assertEquals("director", result.username());
        assertEquals(ownerUserId, result.sharedBy());
        assertEquals("rm_lead", result.sharedByName());
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("Issue 5: getActiveShares bởi user không phải owner -> Throw PermissionDeniedException")
    void getActiveShares_NotOwner_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(999L);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));

        assertThrows(PermissionDeniedException.class, () -> shareService.getActiveShares(scenarioId));
        verify(deniedAuditLogPort, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Issue 6: Snapshot corrupt trong kịch bản -> Throw CorruptedScenarioSnapshotException")
    void getShareCandidates_CorruptedSnapshot_ThrowsCorruptedScenarioSnapshotException() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        ResourceScenario corruptedScenario = ResourceScenario.createNew(
                "SCN-CORRUPT", "Corrupt Scenario", "Mô tả", orgUnitId, 2026, 38, 4, ownerUserId
        );
        corruptedScenario.setId(scenarioId);
        corruptedScenario.saveSnapshot("invalid_json{[[}");
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(corruptedScenario));

        assertThrows(CorruptedScenarioSnapshotException.class, () -> shareService.getShareCandidates(scenarioId, null));
    }

    @Test
    @DisplayName("Issue 7: getShareCandidates batch load employees và trả về managedProjectIds")
    void getShareCandidates_BatchesEmployeeLookup_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE)).thenReturn(ownerUserId);
        when(loadScenarioPort.findById(scenarioId)).thenReturn(Optional.of(savedScenario));
        when(loadScenarioSharePort.findActiveSharesByScenarioId(scenarioId)).thenReturn(List.of());

        User vt01 = createUser(201L, "director", RoleCode.VT_01, null, 2001L);
        User vt02 = createUser(202L, "pm_lead", RoleCode.VT_02, null, 2002L);
        when(loadUserPort.findAll(0, 1000)).thenReturn(List.of(vt01, vt02));

        Employee emp01 = createEmployee(2001L, 201L, orgUnitId, "EMP01", "Director User");
        Employee emp02 = createEmployee(2002L, 202L, orgUnitId, "EMP02", "PM Lead User");
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(List.of(emp01, emp02));

        when(loadProjectPort.findAllManagedProjectIds(2002L)).thenReturn(List.of(501L));
        Project p501 = mock(Project.class);
        when(p501.getId()).thenReturn(new ProjectId(501L));
        when(p501.getProjectName()).thenReturn("Project 1");
        when(loadProjectPort.findAllById(anyList())).thenReturn(List.of(p501));

        List<ShareCandidateResult> candidates = shareService.getShareCandidates(scenarioId, null);
        assertEquals(2, candidates.size());

        ShareCandidateResult pmCandidate = candidates.stream()
                .filter(c -> c.userId().equals(202L))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(501L), pmCandidate.managedProjectIds());
        assertEquals(List.of("Project 1"), pmCandidate.managedProjectNames());
    }
}
