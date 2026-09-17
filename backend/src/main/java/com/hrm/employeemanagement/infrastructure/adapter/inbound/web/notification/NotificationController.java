package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.notification.GetNotificationCenterQuery;
import com.hrm.employeemanagement.application.dto.notification.NotificationCenterItemResult;
import com.hrm.employeemanagement.application.dto.notification.NotificationCenterPageResult;
import com.hrm.employeemanagement.application.dto.notification.UnreadNotificationCountResult;
import com.hrm.employeemanagement.application.port.inbound.notification.DeleteNotificationItemUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationCenterUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationDetailUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.GetUnreadNotificationCountUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.MarkAllNotificationItemsReadUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.MarkNotificationItemReadUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.CurrentUserPort;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

/**
 * Controller trung tâm thông báo (NCL-11-CN-001).
 * Cung cấp 6 User-Facing Endpoints:
 * 1. GET /api/v1/notifications (list + filter + pagination)
 * 2. GET /api/v1/notifications/unread-count (badge count)
 * 3. GET /api/v1/notifications/{id} (chi tiết + routing info, read thuần túy)
 * 4. PATCH /api/v1/notifications/{id}/read (đánh dấu 1 item đã đọc)
 * 5. PATCH /api/v1/notifications/read-all (đánh dấu tất cả đã đọc)
 * 6. DELETE /api/v1/notifications/{id} (xóa mềm + audit snapshot)
 *
 * Lưu ý quan trọng: {id} trên URL chính là notification_recipient.id (hộp thư cá nhân).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final GetNotificationCenterUseCase getNotificationCenterUseCase;
    private final GetUnreadNotificationCountUseCase getUnreadNotificationCountUseCase;
    private final GetNotificationDetailUseCase getNotificationDetailUseCase;
    private final MarkNotificationItemReadUseCase markNotificationItemReadUseCase;
    private final MarkAllNotificationItemsReadUseCase markAllNotificationItemsReadUseCase;
    private final DeleteNotificationItemUseCase deleteNotificationItemUseCase;
    private final CurrentUserPort currentUserPort;

    public NotificationController(
            GetNotificationCenterUseCase getNotificationCenterUseCase,
            GetUnreadNotificationCountUseCase getUnreadNotificationCountUseCase,
            GetNotificationDetailUseCase getNotificationDetailUseCase,
            MarkNotificationItemReadUseCase markNotificationItemReadUseCase,
            MarkAllNotificationItemsReadUseCase markAllNotificationItemsReadUseCase,
            DeleteNotificationItemUseCase deleteNotificationItemUseCase,
            CurrentUserPort currentUserPort
    ) {
        this.getNotificationCenterUseCase = getNotificationCenterUseCase;
        this.getUnreadNotificationCountUseCase = getUnreadNotificationCountUseCase;
        this.getNotificationDetailUseCase = getNotificationDetailUseCase;
        this.markNotificationItemReadUseCase = markNotificationItemReadUseCase;
        this.markAllNotificationItemsReadUseCase = markAllNotificationItemsReadUseCase;
        this.deleteNotificationItemUseCase = deleteNotificationItemUseCase;
        this.currentUserPort = currentUserPort;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationCenterPageResult>> getMyNotifications(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "ALL") String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long currentUserId = requireCurrentUserId();
        GetNotificationCenterQuery query = new GetNotificationCenterQuery(status, level, page, size);
        NotificationCenterPageResult result = getNotificationCenterUseCase.getNotifications(currentUserId, query);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadNotificationCountResult>> getUnreadCount() {
        Long currentUserId = requireCurrentUserId();
        UnreadNotificationCountResult result = getUnreadNotificationCountUseCase.getUnreadCount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thông báo chưa đọc thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationCenterItemResult>> getNotificationDetail(@PathVariable Long id) {
        Long currentUserId = requireCurrentUserId();
        NotificationCenterItemResult result = getNotificationDetailUseCase.getNotificationDetail(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết thông báo thành công", result));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        Long currentUserId = requireCurrentUserId();
        markNotificationItemReadUseCase.markAsRead(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đã đọc thành công", null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Long currentUserId = requireCurrentUserId();
        markAllNotificationItemsReadUseCase.markAllAsRead(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đọc tất cả thành công", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        Long currentUserId = requireCurrentUserId();
        deleteNotificationItemUseCase.deleteNotification(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Xóa thông báo thành công", null));
    }

    private Long requireCurrentUserId() {
        return currentUserPort.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa được xác thực"));
    }
}
