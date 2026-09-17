package com.hrm.employeemanagement.application.service.scenario;

import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.hrm.employeemanagement.application.dto.scenario.*;
import com.hrm.employeemanagement.application.port.inbound.scenario.*;
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
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.scenario.*;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.scenario.ResourceScenario;
import com.hrm.employeemanagement.domain.scenario.ScenarioShare;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScenarioShareService implements
        ShareSimulationScenarioUseCase,
        UnshareSimulationScenarioUseCase,
        GetShareCandidatesUseCase,
        GetScenarioSharesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScenarioShareService.class);

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadResourceScenarioPort loadScenarioPort;
    private final LoadScenarioSharePort loadScenarioSharePort;
    private final SaveScenarioSharePort saveScenarioSharePort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;
    private final ObjectMapper objectMapper;

    public ScenarioShareService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadProjectPort loadProjectPort,
            LoadResourceScenarioPort loadScenarioPort,
            LoadScenarioSharePort loadScenarioSharePort,
            SaveScenarioSharePort saveScenarioSharePort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadScenarioPort = Objects.requireNonNull(loadScenarioPort, "LoadResourceScenarioPort must not be null");
        this.loadScenarioSharePort = Objects.requireNonNull(loadScenarioSharePort, "LoadScenarioSharePort must not be null");
        this.saveScenarioSharePort = Objects.requireNonNull(saveScenarioSharePort, "SaveScenarioSharePort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.deniedAuditLogPort = Objects.requireNonNull(deniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public List<ShareCandidateResult> getShareCandidates(Long scenarioId, String query) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        assertOwner(scenario, currentUserId);

        // BR-05: Chỉ share SAVED Scenario. Không share DRAFT.
        if (!scenario.isSaved()) {
            throw new ScenarioNotSavedException("Chỉ có thể tìm kiếm người nhận chia sẻ cho kịch bản đã được lưu (SAVED)");
        }

        List<Long> scenarioProjectIds = extractProjectIdsFromSnapshot(scenario.getSnapshotData());

        // Lấy tất cả user đang active
        Set<Long> existingActiveUserIds = loadScenarioSharePort.findActiveSharesByScenarioId(scenarioId).stream()
                .map(ScenarioShare::getSharedWithUserId)
                .collect(Collectors.toSet());

        List<User> allUsers = loadUserPort.findAll(0, 1000);
        List<ShareCandidateResult> candidates = new ArrayList<>();
        Map<Long, String> orgUnitCache = new HashMap<>();

        String lowerQuery = (query != null && !query.trim().isEmpty()) ? query.trim().toLowerCase() : null;

        for (User u : allUsers) {
            if (u.getStatus() != UserStatus.ACTIVE) {
                continue;
            }
            // Không tự chia sẻ cho chính mình
            if (u.getIdValue().equals(scenario.getCreatedBy())) {
                continue;
            }

            // Đã có active share thì không hiển thị làm candidate
            if (existingActiveUserIds.contains(u.getIdValue())) {
                continue;
            }

            RoleCode roleCode = u.getRole() != null ? u.getRole().getCode() : null;
            if (roleCode == null) {
                continue;
            }

            // BR-03: Chỉ cho phép VT-01, VT-02, VT-03. VT-04/05/06 không hợp lệ.
            if (roleCode != RoleCode.VT_01 && roleCode != RoleCode.VT_02 && roleCode != RoleCode.VT_03) {
                continue;
            }

            Employee emp = null;
            if (u.getEmployeeId() != null) {
                emp = loadEmployeePort.findById(u.getEmployeeId()).orElse(null);
            }

            List<String> managedProjectNames = Collections.emptyList();

            if (roleCode == RoleCode.VT_02) {
                // VT-02: Phải phụ trách ít nhất một Project trong Scenario
                if (emp == null) {
                    continue;
                }
                List<Long> managedIds = loadProjectPort.findAllManagedProjectIds(emp.getIdValue());
                List<Long> matchedProjectIds = managedIds.stream()
                        .filter(scenarioProjectIds::contains)
                        .toList();
                if (matchedProjectIds.isEmpty()) {
                    continue;
                }
                List<ProjectId> pIds = matchedProjectIds.stream().map(ProjectId::new).toList();
                managedProjectNames = loadProjectPort.findAllById(pIds).stream()
                        .map(Project::getProjectName)
                        .toList();
            } else if (roleCode == RoleCode.VT_03) {
                // VT-03: recipient.org_unit_id == scenario.org_unit_id
                Long userOrgUnitId = u.getScopeOrgUnitId();
                if (userOrgUnitId == null && emp != null) {
                    userOrgUnitId = emp.getOrgUnitId();
                }
                if (userOrgUnitId == null || !userOrgUnitId.equals(scenario.getOrgUnitId())) {
                    continue;
                }
            }

            String fullName = emp != null ? emp.getFullName() : u.getUsername();
            String empCode = emp != null ? emp.getEmployeeCode() : "";
            String email = u.getEmail() != null ? u.getEmail() : "";

            // Bộ lọc từ khóa
            if (lowerQuery != null) {
                boolean match = u.getUsername().toLowerCase().contains(lowerQuery)
                        || fullName.toLowerCase().contains(lowerQuery)
                        || empCode.toLowerCase().contains(lowerQuery)
                        || email.toLowerCase().contains(lowerQuery);
                if (!match) {
                    continue;
                }
            }

            String orgUnitName = "";
            Long targetOrgId = u.getScopeOrgUnitId() != null ? u.getScopeOrgUnitId() : (emp != null ? emp.getOrgUnitId() : null);
            if (targetOrgId != null) {
                orgUnitName = orgUnitCache.computeIfAbsent(targetOrgId, id ->
                        loadOrgUnitPort.findById(new OrgUnitId(id))
                                .map(OrgUnit::getUnitName)
                                .orElse(""));
            }

            candidates.add(new ShareCandidateResult(
                    u.getIdValue(),
                    u.getUsername(),
                    fullName,
                    empCode,
                    email,
                    roleCode.getCode(),
                    u.getRole().getName(),
                    targetOrgId,
                    orgUnitName,
                    managedProjectNames
            ));
        }

        candidates.sort(Comparator.comparing(ShareCandidateResult::fullName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return candidates;
    }

    @Override
    public List<ScenarioShareResult> shareScenario(ShareScenarioCommand command) {
        if (command == null || command.scenarioId() == null) {
            throw new IllegalArgumentException("Dữ liệu chia sẻ không hợp lệ");
        }
        if (command.userIds() == null || command.userIds().isEmpty()) {
            throw new IllegalArgumentException("Danh sách người nhận chia sẻ không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        ResourceScenario scenario = loadScenarioPort.findById(command.scenarioId())
                .orElseThrow(() -> new ScenarioNotFoundException(command.scenarioId()));

        assertOwner(scenario, currentUserId);

        // BR-05: Chỉ share SAVED Scenario. Không share DRAFT.
        if (!scenario.isSaved()) {
            throw new ScenarioNotSavedException("Chỉ được chia sẻ kịch bản ở trạng thái đã lưu (SAVED)");
        }

        List<Long> scenarioProjectIds = extractProjectIdsFromSnapshot(scenario.getSnapshotData());

        // Kiểm tra duplicate trong chính danh sách gửi lên
        Set<Long> uniqueRequestedIds = new HashSet<>(command.userIds());
        if (uniqueRequestedIds.size() != command.userIds().size()) {
            throw new DuplicateScenarioShareException("Danh sách người nhận chia sẻ chứa người dùng bị lặp lại");
        }

        // BR-10: Share atomic: Toàn bộ danh sách phải hợp lệ. Nếu 1 recipient invalid -> toàn bộ request fail!
        List<User> validatedUsers = new ArrayList<>();
        for (Long recipientUserId : command.userIds()) {
            if (recipientUserId == null) {
                throw new InvalidShareRecipientException("Mã người dùng không được null");
            }
            if (recipientUserId.equals(scenario.getCreatedBy())) {
                throw new InvalidShareRecipientException("Không thể chia sẻ kịch bản cho chính chủ sở hữu");
            }

            User recipient = loadUserPort.findById(new UserId(recipientUserId))
                    .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + recipientUserId));

            if (recipient.getStatus() != UserStatus.ACTIVE) {
                throw new InvalidShareRecipientException("Người dùng " + recipient.getUsername() + " đang không ở trạng thái hoạt động");
            }

            // Kiểm tra đã có active share chưa
            if (loadScenarioSharePort.hasActiveShare(scenario.getId(), recipientUserId)) {
                throw new DuplicateScenarioShareException("Kịch bản đã được chia sẻ cho người dùng " + recipient.getUsername());
            }

            RoleCode roleCode = recipient.getRole().getCode();
            // BR-03: Valid recipient ∈ {VT-01, VT-02, VT-03}
            if (roleCode != RoleCode.VT_01 && roleCode != RoleCode.VT_02 && roleCode != RoleCode.VT_03) {
                throw new InvalidShareRecipientException("Vai trò " + roleCode.getCode() + " (" + recipient.getRole().getName() + ") không được phép nhận chia sẻ kịch bản");
            }

            Employee emp = null;
            if (recipient.getEmployeeId() != null) {
                emp = loadEmployeePort.findById(recipient.getEmployeeId()).orElse(null);
            }

            if (roleCode == RoleCode.VT_02) {
                // VT-02: Phải phụ trách ít nhất một Project trong Scenario
                if (emp == null) {
                    throw new InvalidShareRecipientException("Quản lý dự án " + recipient.getUsername() + " chưa được liên kết hồ sơ nhân viên");
                }
                List<Long> managedProjectIds = loadProjectPort.findAllManagedProjectIds(emp.getIdValue());
                boolean hasOverlap = managedProjectIds.stream().anyMatch(scenarioProjectIds::contains);
                if (!hasOverlap) {
                    throw new InvalidShareRecipientException("Quản lý dự án " + recipient.getUsername() + " không phụ trách bất kỳ dự án nào trong kịch bản này");
                }
            } else if (roleCode == RoleCode.VT_03) {
                // VT-03: recipient.org_unit_id == scenario.org_unit_id
                Long userOrgUnitId = recipient.getScopeOrgUnitId();
                if (userOrgUnitId == null && emp != null) {
                    userOrgUnitId = emp.getOrgUnitId();
                }
                if (userOrgUnitId == null || !userOrgUnitId.equals(scenario.getOrgUnitId())) {
                    throw new InvalidShareRecipientException("Quản lý nguồn lực " + recipient.getUsername() + " không thuộc đơn vị tổ chức của kịch bản");
                }
            }

            validatedUsers.add(recipient);
        }

        // Tạo và lưu các ScenarioShare
        List<ScenarioShare> sharesToSave = new ArrayList<>();
        for (User recipient : validatedUsers) {
            sharesToSave.add(ScenarioShare.create(scenario.getId(), recipient.getIdValue(), currentUserId));
        }

        List<ScenarioShare> savedShares = saveScenarioSharePort.saveAll(sharesToSave);

        // BR-11: Ghi Audit log SCENARIO_SHARED
        for (ScenarioShare share : savedShares) {
            saveAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "SCENARIO_SHARED",
                    "scenario_shares",
                    share.getId(),
                    null,
                    "scenarioId=" + scenario.getId() + ";sharedWithUserId=" + share.getSharedWithUserId() + ";accessLevel=" + share.getAccessLevel()
            ));
        }

        return savedShares.stream().map(this::enrichShareResult).toList();
    }

    @Override
    public void unshareScenario(Long scenarioId, Long sharedWithUserId) {
        if (scenarioId == null || sharedWithUserId == null) {
            throw new IllegalArgumentException("Mã kịch bản và người được chia sẻ không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        assertOwner(scenario, currentUserId);

        ScenarioShare activeShare = loadScenarioSharePort.findActiveShare(scenarioId, sharedWithUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin chia sẻ đang hoạt động của người dùng này"));

        // BR-09: Revoke bằng revoked_at = now(). Không hard delete share record.
        activeShare.revoke();
        ScenarioShare saved = saveScenarioSharePort.save(activeShare);

        // BR-11: Ghi Audit log SCENARIO_UNSHARED
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "SCENARIO_UNSHARED",
                "scenario_shares",
                saved.getId(),
                "revokedAt=null",
                "revokedAt=" + saved.getRevokedAt() + ";scenarioId=" + scenarioId + ";sharedWithUserId=" + sharedWithUserId
        ));
    }

    @Override
    public List<ScenarioShareResult> getActiveShares(Long scenarioId) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_SCENARIO_READ);
        ResourceScenario scenario = loadScenarioPort.findById(scenarioId)
                .orElseThrow(() -> new ScenarioNotFoundException(scenarioId));

        // Quyền xem danh sách share: Owner hoặc người đang có active share
        boolean isOwner = scenario.getCreatedBy().equals(currentUserId);
        boolean hasActiveShare = loadScenarioSharePort.hasActiveShare(scenarioId, currentUserId);
        if (!isOwner && !hasActiveShare) {
            deniedAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "SCENARIO_ACCESS_DENIED",
                    "resource_scenarios",
                    scenarioId,
                    null,
                    "action=GET_ACTIVE_SHARES;reason=NOT_OWNER_OR_RECIPIENT"
            ));
            throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_READ);
        }

        List<ScenarioShare> shares = loadScenarioSharePort.findActiveSharesByScenarioId(scenarioId);
        return shares.stream().map(this::enrichShareResult).toList();
    }

    private ScenarioShareResult enrichShareResult(ScenarioShare share) {
        User recipient = loadUserPort.findById(new UserId(share.getSharedWithUserId())).orElse(null);
        String username = recipient != null ? recipient.getUsername() : "User #" + share.getSharedWithUserId();
        String roleCode = recipient != null ? recipient.getRole().getCode().getCode() : "";
        String roleName = recipient != null ? recipient.getRole().getName() : "";

        String fullName = username;
        if (recipient != null && recipient.getEmployeeId() != null) {
            Employee emp = loadEmployeePort.findById(recipient.getEmployeeId()).orElse(null);
            if (emp != null) {
                fullName = emp.getFullName();
            }
        }

        return new ScenarioShareResult(
                share.getId(),
                share.getScenarioId(),
                share.getSharedWithUserId(),
                username,
                fullName,
                roleCode,
                roleName,
                share.getSharedByUserId(),
                share.getAccessLevel(),
                share.getCreatedAt(),
                share.getRevokedAt(),
                share.isActive()
        );
    }

    private void assertOwner(ResourceScenario scenario, Long currentUserId) {
        // BR-02: VT-03 là owner. VT-01 không mặc định là owner.
        if (!scenario.getCreatedBy().equals(currentUserId)) {
            deniedAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "SCENARIO_ACCESS_DENIED",
                    "resource_scenarios",
                    scenario.getId(),
                    null,
                    "action=MUTATION;reason=NOT_SCENARIO_OWNER;ownerId=" + scenario.getCreatedBy()
            ));
            throw new PermissionDeniedException(PermissionCode.RESOURCE_SCENARIO_MANAGE);
        }
    }

    private List<Long> extractProjectIdsFromSnapshot(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.trim().isEmpty()) {
            return List.of();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(snapshotJson, new TypeReference<>() {});
            Object projectIdsObj = map.get("projectIds");
            if (projectIdsObj instanceof List<?> list) {
                return list.stream()
                        .map(o -> Long.valueOf(o.toString()))
                        .toList();
            }
        } catch (Exception ignored) {}
        return List.of();
    }
}
