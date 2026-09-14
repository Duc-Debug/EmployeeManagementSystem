package com.hrm.employeemanagement.application.service.allocation.threshold;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdHistoryResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.ConfigureCapacityThresholdCommand;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.ConfigureCapacityThresholdUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.GetCapacityThresholdHistoryUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.GetCapacityThresholdUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdHistoryPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.LoadCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.threshold.SaveCapacityThresholdPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdPolicy;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Pure Java Application Service cho Use Case NCL-07-CN-004:
 * Cấu hình ngưỡng cảnh báo quá tải và nhàn rỗi theo QTN-23.
 */
public class CapacityThresholdService implements
        ConfigureCapacityThresholdUseCase,
        GetCapacityThresholdUseCase,
        GetCapacityThresholdHistoryUseCase {

    private final AuthorizationService authorizationService;
    private final LoadCapacityThresholdPort loadCapacityThresholdPort;
    private final SaveCapacityThresholdPort saveCapacityThresholdPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadCapacityThresholdHistoryPort historyPort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;

    public CapacityThresholdService(
            AuthorizationService authorizationService,
            LoadCapacityThresholdPort loadCapacityThresholdPort,
            SaveCapacityThresholdPort saveCapacityThresholdPort,
            SaveAuditLogPort saveAuditLogPort,
            LoadCapacityThresholdHistoryPort historyPort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.loadCapacityThresholdPort = Objects.requireNonNull(loadCapacityThresholdPort, "loadCapacityThresholdPort must not be null");
        this.saveCapacityThresholdPort = Objects.requireNonNull(saveCapacityThresholdPort, "saveCapacityThresholdPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "saveAuditLogPort must not be null");
        this.historyPort = Objects.requireNonNull(historyPort, "historyPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "loadOrgUnitPort must not be null");
    }

    @Override
    public CapacityThresholdResult configureThreshold(ConfigureCapacityThresholdCommand command) {
        // 1. Enforce Authorization (BR-05 / TC-03): Chỉ VT-01 (CAPACITY_THRESHOLD_MANAGE) mới được phép cấu hình
        Long currentUserId = authorizationService.require(PermissionCode.CAPACITY_THRESHOLD_MANAGE);

        // 2. Business Validation (BR-03 / TC-02 / Gate #B / Gate #E): Ràng buộc giá trị và quy tắc idle < overload
        CapacityThresholdPolicy.validateThresholds(command.overloadThreshold(), command.idleThreshold());

        CapacityThresholdScope scopeType = command.scopeType() != null ? command.scopeType() : CapacityThresholdScope.COMPANY;
        if (scopeType == CapacityThresholdScope.ORG_UNIT && command.orgUnitId() != null) {
            enforceOrgUnitDataScope(currentUserId, command.orgUnitId());
        }

        String scopeKey = CapacityThresholdPolicy.computeScopeKey(scopeType, command.orgUnitId());

        Optional<CapacityThresholdConfig> existingOpt = loadCapacityThresholdPort.findByScopeKey(scopeKey);

        CapacityThresholdConfig savedConfig;
        if (existingOpt.isPresent()) {
            CapacityThresholdConfig existing = existingOpt.get();

            // [HIGH #1 FIX]: Enforce Optimistic Locking - So khớp version gửi lên với version hiện tại trong DB
            CapacityThresholdPolicy.validateVersion(command.version(), existing.getVersion());

            // Gate #N: NO-OP check - nếu không thay đổi giá trị thì không cập nhật và không ghi audit log
            if (!existing.hasChanged(command.overloadThreshold(), command.idleThreshold())) {
                return mapToResult(existing, false, false);
            }

            String oldValue = String.format("{\"overloadThreshold\":%.1f,\"idleThreshold\":%.1f}",
                    existing.getOverloadThreshold(), existing.getIdleThreshold());
            String newValue = String.format("{\"overloadThreshold\":%.1f,\"idleThreshold\":%.1f}",
                    command.overloadThreshold(), command.idleThreshold());

            existing.update(command.overloadThreshold(), command.idleThreshold(), currentUserId);
            savedConfig = saveCapacityThresholdPort.save(existing);

            // 3. Ghi Audit Log trong cùng Transaction (BR-06 / TC-04 / Gate #J)
            AuditLog auditLog = AuditLog.createChange(
                    currentUserId,
                    "UPDATE_CAPACITY_THRESHOLD",
                    "capacity_threshold_configs",
                    savedConfig.getId(),
                    oldValue,
                    newValue
            );
            saveAuditLogPort.save(auditLog);
        } else {
            String newValue = String.format("{\"overloadThreshold\":%.1f,\"idleThreshold\":%.1f}",
                    command.overloadThreshold(), command.idleThreshold());

            CapacityThresholdConfig newConfig = CapacityThresholdConfig.createNew(
                    scopeType,
                    command.orgUnitId(),
                    command.overloadThreshold(),
                    command.idleThreshold(),
                    currentUserId
            );
            savedConfig = saveCapacityThresholdPort.save(newConfig);

            // Ghi Audit Log tạo mới
            AuditLog auditLog = AuditLog.createChange(
                    currentUserId,
                    "CREATE_CAPACITY_THRESHOLD",
                    "capacity_threshold_configs",
                    savedConfig.getId(),
                    null,
                    newValue
            );
            saveAuditLogPort.save(auditLog);
        }

        return mapToResult(savedConfig, false, false);
    }

    @Override
    public CapacityThresholdResult getEffectiveThreshold(CapacityThresholdScope scopeType, Long orgUnitId) {
        // Enforce Authorization: Cho phép VT-01, VT-02, VT-03 đọc cấu hình
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.CAPACITY_THRESHOLD_READ,
                PermissionCode.CAPACITY_THRESHOLD_MANAGE,
                PermissionCode.RESOURCE_ALLOCATION_READ
        );

        CapacityThresholdScope actualScope = scopeType != null ? scopeType : CapacityThresholdScope.COMPANY;

        // [HIGH #2 FIX]: Enforce DataScope chặt chẽ khi đọc cấu hình theo ORG_UNIT
        if (actualScope == CapacityThresholdScope.ORG_UNIT && orgUnitId != null) {
            enforceOrgUnitDataScope(currentUserId, orgUnitId);
        }

        String scopeKey = CapacityThresholdPolicy.computeScopeKey(actualScope, orgUnitId);

        boolean isInherited = false;
        Optional<CapacityThresholdConfig> configOpt = loadCapacityThresholdPort.findByScopeKey(scopeKey);

        // Fallback scope: Nếu tìm theo ORG_UNIT không thấy -> thử fallback về COMPANY
        if (configOpt.isEmpty() && actualScope == CapacityThresholdScope.ORG_UNIT) {
            configOpt = loadCapacityThresholdPort.findByScopeKey("COMPANY");
            if (configOpt.isPresent()) {
                // [HIGH #3 FIX]: Đánh dấu rõ ràng đây là cấu hình kế thừa từ COMPANY
                isInherited = true;
            }
        }

        // BR-04 / Gate #M: Fallback về ngưỡng mặc định nếu chưa từng cấu hình riêng
        if (configOpt.isEmpty()) {
            return new CapacityThresholdResult(
                    null,
                    actualScope,
                    orgUnitId,
                    CapacityThresholdPolicy.DEFAULT_OVERLOAD_THRESHOLD,
                    CapacityThresholdPolicy.DEFAULT_IDLE_THRESHOLD,
                    true,
                    false,
                    0L,
                    null,
                    null,
                    null
            );
        }

        return mapToResult(configOpt.get(), false, isInherited);
    }

    @Override
    public List<CapacityThresholdHistoryResult> getHistory(CapacityThresholdScope scopeType, Long orgUnitId) {
        // Enforce Authorization: Cho phép VT-01, VT-02, VT-03 xem lịch sử
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.CAPACITY_THRESHOLD_READ,
                PermissionCode.CAPACITY_THRESHOLD_MANAGE
        );

        CapacityThresholdScope actualScope = scopeType != null ? scopeType : CapacityThresholdScope.COMPANY;

        // [HIGH #2 FIX]: Enforce DataScope chặt chẽ khi xem lịch sử theo ORG_UNIT
        if (actualScope == CapacityThresholdScope.ORG_UNIT && orgUnitId != null) {
            enforceOrgUnitDataScope(currentUserId, orgUnitId);
        }

        String scopeKey = CapacityThresholdPolicy.computeScopeKey(actualScope, orgUnitId);

        Optional<CapacityThresholdConfig> configOpt = loadCapacityThresholdPort.findByScopeKey(scopeKey);
        if (configOpt.isEmpty()) {
            return Collections.emptyList();
        }

        return historyPort.loadHistory("capacity_threshold_configs", configOpt.get().getId());
    }

    /**
     * [HIGH #2 FIX]: Thẩm tra DataScope của người dùng hiện tại đối với orgUnitId được yêu cầu.
     * Ngăn chặn người dùng chi nhánh A xem dữ liệu chi nhánh B.
     */
    private void enforceOrgUnitDataScope(Long currentUserId, Long orgUnitId) {
        if (orgUnitId == null) {
            return;
        }

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        if (currentUser.getDataScope() == null) {
            throw new PermissionDeniedException(PermissionCode.CAPACITY_THRESHOLD_READ);
        }

        switch (currentUser.getDataScope()) {
            case COMPANY -> {
                // Toàn công ty: được phép truy cập mọi orgUnitId
            }
            case ORGANIZATION_BRANCH -> {
                if (currentUser.getScopeOrgUnitId() == null) {
                    throw new PermissionDeniedException(PermissionCode.CAPACITY_THRESHOLD_READ);
                }
                boolean inScope = loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, currentUser.getScopeOrgUnitId());
                if (!inScope) {
                    throw new PermissionDeniedException(PermissionCode.CAPACITY_THRESHOLD_READ);
                }
            }
            case SELF -> throw new PermissionDeniedException(PermissionCode.CAPACITY_THRESHOLD_READ);
            default -> throw new PermissionDeniedException(PermissionCode.CAPACITY_THRESHOLD_READ);
        }
    }

    private CapacityThresholdResult mapToResult(CapacityThresholdConfig config, boolean isDefault, boolean isInherited) {
        String updaterName = null;
        if (config.getUpdatedBy() != null) {
            updaterName = loadUserPort.findById(new UserId(config.getUpdatedBy()))
                    .map(User::getUsername)
                    .orElse(null);
        }

        return new CapacityThresholdResult(
                config.getId(),
                config.getScopeType(),
                config.getOrgUnitId(),
                config.getOverloadThreshold(),
                config.getIdleThreshold(),
                isDefault,
                isInherited,
                config.getVersion(),
                config.getUpdatedAt(),
                config.getUpdatedBy(),
                updaterName
        );
    }
}
