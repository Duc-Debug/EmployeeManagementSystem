package com.hrm.employeemanagement.application.service.notification.dedup;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.notification.dedup.NotificationDedupConfigResult;
import com.hrm.employeemanagement.application.dto.notification.dedup.UpdateNotificationDedupConfigCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.GetNotificationDedupConfigUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.UpdateNotificationDedupConfigUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.PermissionQueryPort;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupConfigRepositoryPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfig;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfigHistory;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Application Service quản lý cấu hình chống gửi trùng thông báo (NCL-11-CN-003).
 * Đáp ứng:
 * - TC-03: Kiểm tra quyền NOTIFICATION_DEDUPLICATION_MANAGE (chỉ VT-06), ghi log ACCESS_DENIED_NOTIFICATION_DEDUP_CONFIG khi bị từ chối.
 * - TC-04: Lưu lịch sử thay đổi cấu hình gồm người thực hiện, nội dung thay đổi, thời điểm.
 */
public class NotificationDedupConfigService implements GetNotificationDedupConfigUseCase, UpdateNotificationDedupConfigUseCase {

    public static final String ACCESS_DENIED_ACTION = "ACCESS_DENIED_NOTIFICATION_DEDUP_CONFIG";

    private final NotificationDedupConfigRepositoryPort configRepositoryPort;
    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final PermissionQueryPort permissionQueryPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    public NotificationDedupConfigService(
            NotificationDedupConfigRepositoryPort configRepositoryPort,
            GetAuthenticatedUserPort authenticatedUserPort,
            PermissionQueryPort permissionQueryPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort
    ) {
        this.configRepositoryPort = Objects.requireNonNull(configRepositoryPort, "configRepositoryPort must not be null");
        this.authenticatedUserPort = Objects.requireNonNull(authenticatedUserPort, "authenticatedUserPort must not be null");
        this.permissionQueryPort = Objects.requireNonNull(permissionQueryPort, "permissionQueryPort must not be null");
        this.deniedAuditLogPort = Objects.requireNonNull(deniedAuditLogPort, "deniedAuditLogPort must not be null");
    }

    @Override
    public NotificationDedupConfigResult getConfig() {
        enforceAuthorization();
        NotificationDedupConfig config = configRepositoryPort.loadConfig();
        return mapToResult(config);
    }

    @Override
    public NotificationDedupConfigResult updateConfig(UpdateNotificationDedupConfigCommand command, Long actorUserId) {
        Long currentUserId = enforceAuthorization();
        Long effectiveActorId = actorUserId != null ? actorUserId : currentUserId;

        NotificationDedupConfig config = configRepositoryPort.loadConfig();

        String previousValue = String.format("{\"isEnabled\":%b,\"dedupWindowDays\":%d,\"scanIntervalMinutes\":%d}",
                config.isEnabled(), config.getDedupWindowDays(), config.getScanIntervalMinutes());

        config.update(
                command.isEnabled(),
                command.dedupWindowDays(),
                command.scanIntervalMinutes(),
                effectiveActorId,
                LocalDateTime.now()
        );

        NotificationDedupConfig savedConfig = configRepositoryPort.saveConfig(config);

        String newValue = String.format("{\"isEnabled\":%b,\"dedupWindowDays\":%d,\"scanIntervalMinutes\":%d}",
                savedConfig.isEnabled(), savedConfig.getDedupWindowDays(), savedConfig.getScanIntervalMinutes());

        String changeSummary = String.format("Cập nhật cấu hình chống trùng: isEnabled=%b, windowDays=%d, intervalMinutes=%d",
                savedConfig.isEnabled(), savedConfig.getDedupWindowDays(), savedConfig.getScanIntervalMinutes());

        NotificationDedupConfigHistory history = NotificationDedupConfigHistory.create(
                effectiveActorId,
                "UPDATE_DEDUP_CONFIG",
                previousValue,
                newValue,
                changeSummary
        );
        configRepositoryPort.saveHistory(history);

        return mapToResult(savedConfig);
    }

    private Long enforceAuthorization() {
        User currentUser = authenticatedUserPort.getAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Không tìm thấy người dùng đã xác thực");
        }

        Long currentUserId = currentUser.getIdValue();
        if (!permissionQueryPort.hasPermission(currentUserId, PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE)) {
            deniedAuditLogPort.save(
                    AuditLog.createChange(
                            currentUserId,
                            ACCESS_DENIED_ACTION,
                            "notification_dedup_configs",
                            null,
                            null,
                            "user_id=" + currentUserId + ";role=" + (currentUser.getRole() != null ? currentUser.getRole().getCode() : "UNKNOWN")
                    )
            );
            throw new PermissionDeniedException(PermissionCode.NOTIFICATION_DEDUPLICATION_MANAGE);
        }

        return currentUserId;
    }

    private NotificationDedupConfigResult mapToResult(NotificationDedupConfig config) {
        return new NotificationDedupConfigResult(
                config.isEnabled(),
                config.getDedupWindowDays(),
                config.getScanIntervalMinutes(),
                config.getUpdatedBy(),
                config.getUpdatedAt(),
                config.getVersion()
        );
    }
}
