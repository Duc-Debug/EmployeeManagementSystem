package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification;

import java.net.URI;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.domain.notification.NotificationLevel;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.ErrorResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.notification.dto.CreateInternalNotificationRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

/**
 * Controller tiếp nhận sự kiện thông báo nội bộ (POST /internal/notifications).
 * Tuân thủ Security Contract nghiêm ngặt:
 * - Không dành cho browser/frontend người dùng thông thường.
 * - Không dùng ROLE_ADMIN để bypass.
 * - Yêu cầu xác thực machine-to-machine qua X-Internal-Token header.
 */
@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private static final Logger log = LoggerFactory.getLogger(InternalNotificationController.class);

    private final CreateNotificationEventUseCase createNotificationEventUseCase;
    private final String expectedInternalToken;

    public InternalNotificationController(
            CreateNotificationEventUseCase createNotificationEventUseCase,
            @Value("${app.security.internal-token:#{null}}") String expectedInternalToken
    ) {
        this.createNotificationEventUseCase = Objects.requireNonNull(createNotificationEventUseCase, "createNotificationEventUseCase must not be null");
        if (expectedInternalToken == null || expectedInternalToken.trim().isEmpty()) {
            throw new IllegalStateException("Cấu hình 'app.security.internal-token' là bắt buộc và không được phép để trống hoặc dùng default credential trong source.");
        }
        this.expectedInternalToken = expectedInternalToken.trim();
    }

    @PostMapping
    public ResponseEntity<?> createNotification(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody CreateInternalNotificationRequest request
    ) {
        // Kiểm tra machine-to-machine security contract
        if (token == null || !token.equals(expectedInternalToken)) {
            log.warn("Từ chối truy cập /internal/notifications: X-Internal-Token không hợp lệ hoặc bị thiếu");
            ErrorResponse error = ErrorResponse.of(
                    "UNAUTHORIZED_INTERNAL_CALL",
                    "Truy cập bị từ chối. Endpoint nội bộ yêu cầu xác thực service-to-service hợp lệ.",
                    HttpStatus.FORBIDDEN.value()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        CreateNotificationEventCommand command = new CreateNotificationEventCommand(
                request.eventType(),
                NotificationLevel.fromString(request.level()),
                request.title(),
                request.message(),
                request.relatedEntityType(),
                request.relatedEntityId(),
                request.sourceEventKey(),
                request.recipientUserIds()
        );

        Long eventId = createNotificationEventUseCase.execute(command);
        return ResponseEntity.created(URI.create("/internal/notifications/" + eventId))
                .body(ApiResponse.success("Tạo sự kiện thông báo thành công", eventId));
    }
}
