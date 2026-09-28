package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.allocation.AllocationNotificationPageResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetAllocationNotificationsUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * REST Controller cho màn hình danh sách thông báo phân bổ (NCL-07-CN-003, BR-05, AC-03).
 * RBAC và DataScope được kiểm tra nghiêm ngặt tại Application Service:
 * - VT-03 (Quản lý nguồn lực): Xem thông báo trong phạm vi bộ phận.
 * - VT-02 (Quản lý dự án): Xem thông báo của dự án được phân công.
 * - Các vai trò khác: Trả về HTTP 403 Forbidden và ghi nhật ký kiểm toán từ chối truy cập.
 */
@RestController
@RequestMapping("/api/v1/allocations/notifications")
@Validated
@PreAuthorize("isAuthenticated()")
public class AllocationNotificationController {

    private final GetAllocationNotificationsUseCase getAllocationNotificationsUseCase;

    public AllocationNotificationController(GetAllocationNotificationsUseCase getAllocationNotificationsUseCase) {
        this.getAllocationNotificationsUseCase = Objects.requireNonNull(
                getAllocationNotificationsUseCase,
                "GetAllocationNotificationsUseCase must not be null"
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AllocationNotificationPageResult>> getAllocationNotifications(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size
    ) {
        AllocationNotificationPageResult result = getAllocationNotificationsUseCase.getAllocationNotifications(
                projectId,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo phân bổ thành công", result));
    }
}
