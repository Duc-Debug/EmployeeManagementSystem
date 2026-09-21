package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification.dedup;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.notification.dedup.NotificationDedupConfigResult;
import com.hrm.employeemanagement.application.dto.notification.dedup.OverloadScanResult;
import com.hrm.employeemanagement.application.dto.notification.dedup.UpdateNotificationDedupConfigCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.GetNotificationDedupConfigUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.TriggerManualOverloadScanUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.UpdateNotificationDedupConfigUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification.dedup.dto.UpdateNotificationDedupConfigRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

/**
 * Controller quản trị cấu hình chống gửi trùng thông báo (NCL-11-CN-003).
 * Chỉ người dùng có quyền NOTIFICATION_DEDUPLICATION_MANAGE (VT-06) mới được phép truy cập.
 */
@RestController
@RequestMapping("/api/v1/admin/notification-dedup-config")
@PreAuthorize("hasAuthority('NOTIFICATION_DEDUPLICATION_MANAGE')")
public class NotificationDedupConfigController {

    private final GetNotificationDedupConfigUseCase getConfigUseCase;
    private final UpdateNotificationDedupConfigUseCase updateConfigUseCase;
    private final TriggerManualOverloadScanUseCase triggerManualScanUseCase;
    private final CurrentUserPort currentUserPort;

    public NotificationDedupConfigController(
            GetNotificationDedupConfigUseCase getConfigUseCase,
            UpdateNotificationDedupConfigUseCase updateConfigUseCase,
            TriggerManualOverloadScanUseCase triggerManualScanUseCase,
            CurrentUserPort currentUserPort
    ) {
        this.getConfigUseCase = Objects.requireNonNull(getConfigUseCase, "getConfigUseCase must not be null");
        this.updateConfigUseCase = Objects.requireNonNull(updateConfigUseCase, "updateConfigUseCase must not be null");
        this.triggerManualScanUseCase = Objects.requireNonNull(triggerManualScanUseCase, "triggerManualScanUseCase must not be null");
        this.currentUserPort = Objects.requireNonNull(currentUserPort, "currentUserPort must not be null");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationDedupConfigResult>> getConfig() {
        NotificationDedupConfigResult result = getConfigUseCase.getConfig();
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu hình chống gửi trùng thành công", result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<NotificationDedupConfigResult>> updateConfig(
            @Valid @RequestBody UpdateNotificationDedupConfigRequest request
    ) {
        Long actorUserId = currentUserPort.getCurrentUserId().orElse(null);
        UpdateNotificationDedupConfigCommand command = new UpdateNotificationDedupConfigCommand(
                request.isEnabled(),
                request.dedupWindowDays(),
                request.scanIntervalMinutes()
        );
        NotificationDedupConfigResult result = updateConfigUseCase.updateConfig(command, actorUserId);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình chống gửi trùng thành công", result));
    }

    @PostMapping("/trigger-scan")
    public ResponseEntity<ApiResponse<OverloadScanResult>> triggerScan(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer weekNumber
    ) {
        OverloadScanResult result = triggerManualScanUseCase.triggerManualScan(year, weekNumber);
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt rà soát quá tải thành công", result));
    }
}
