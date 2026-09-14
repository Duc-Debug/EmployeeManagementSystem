package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.notification.NotificationResult;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.MarkNotificationReadUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final GetNotificationsUseCase getNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final CurrentUserPort currentUserPort;

    public NotificationController(
            GetNotificationsUseCase getNotificationsUseCase,
            MarkNotificationReadUseCase markNotificationReadUseCase,
            CurrentUserPort currentUserPort) {
        this.getNotificationsUseCase = getNotificationsUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
        this.currentUserPort = currentUserPort;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResult>>> getMyNotifications() {
        Long currentUserId = currentUserPort.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa được xác thực"));

        List<NotificationResult> results = getNotificationsUseCase.execute(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", results));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        Long currentUserId = currentUserPort.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa được xác thực"));

        markNotificationReadUseCase.execute(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đã đọc thành công", null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Long currentUserId = currentUserPort.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa được xác thực"));

        markNotificationReadUseCase.executeAll(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đọc tất cả thành công", null));
    }
}

