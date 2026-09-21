package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.notification.NotificationPreferenceResult;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.ResetNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.UpdateNotificationPreferenceUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification.dto.UpdateNotificationPreferenceRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

/**
 * Controller cấu hình kênh và tần suất nhận thông báo (NCL-11-CN-002).
 * Cung cấp 3 User-Facing Endpoints:
 * 1. GET /api/v1/notification-preferences/me
 * 2. PUT /api/v1/notification-preferences/me
 * 3. POST /api/v1/notification-preferences/me/reset
 */
@RestController
@RequestMapping("/api/v1/notification-preferences")
@PreAuthorize("isAuthenticated()")
public class NotificationPreferenceController {

    private final GetNotificationPreferenceUseCase getNotificationPreferenceUseCase;
    private final UpdateNotificationPreferenceUseCase updateNotificationPreferenceUseCase;
    private final ResetNotificationPreferenceUseCase resetNotificationPreferenceUseCase;
    private final CurrentUserPort currentUserPort;

    public NotificationPreferenceController(
            GetNotificationPreferenceUseCase getNotificationPreferenceUseCase,
            UpdateNotificationPreferenceUseCase updateNotificationPreferenceUseCase,
            ResetNotificationPreferenceUseCase resetNotificationPreferenceUseCase,
            CurrentUserPort currentUserPort
    ) {
        this.getNotificationPreferenceUseCase = Objects.requireNonNull(getNotificationPreferenceUseCase, "getNotificationPreferenceUseCase must not be null");
        this.updateNotificationPreferenceUseCase = Objects.requireNonNull(updateNotificationPreferenceUseCase, "updateNotificationPreferenceUseCase must not be null");
        this.resetNotificationPreferenceUseCase = Objects.requireNonNull(resetNotificationPreferenceUseCase, "resetNotificationPreferenceUseCase must not be null");
        this.currentUserPort = Objects.requireNonNull(currentUserPort, "currentUserPort must not be null");
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<NotificationPreferenceResult>> getMyPreference() {
        Long currentUserId = requireCurrentUserId();
        NotificationPreferenceResult result = getNotificationPreferenceUseCase.getMyPreference(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu hình thông báo thành công", result));
    }

    @RequestMapping(value = "/me", method = {org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<NotificationPreferenceResult>> updateMyPreference(
            @RequestBody UpdateNotificationPreferenceRequest request
    ) {
        Long currentUserId = requireCurrentUserId();
        NotificationPreferenceResult result = updateNotificationPreferenceUseCase.updateMyPreference(
                currentUserId,
                request != null ? request.toCommand() : null
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình thông báo thành công", result));
    }

    @PostMapping("/me/reset")
    public ResponseEntity<ApiResponse<NotificationPreferenceResult>> resetMyPreference() {
        Long currentUserId = requireCurrentUserId();
        NotificationPreferenceResult result = resetNotificationPreferenceUseCase.resetMyPreference(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục cấu hình thông báo mặc định thành công", result));
    }

    private Long requireCurrentUserId() {
        return currentUserPort.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa được xác thực"));
    }
}
