package com.hrm.employeemanagement.infrastructure.adapter.outbound.allocation;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.AllocationNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Adapter lưu thông báo thay đổi phân bổ vào bảng notifications cho Quản lý dự án (PM)
 * và Nhân viên chuyên môn bị ảnh hưởng (NCL-07-CN-003, BR-02, BR-06).
 *
 * <p><b>Transaction contract:</b> Notification được lưu trực tiếp trong cùng transaction
 * với allocation (không dùng afterCommit hay REQUIRES_NEW). Nếu allocation rollback,
 * notification cũng rollback — đảm bảo tính nhất quán dữ liệu.
 *
 * <p><b>Exception isolation:</b> Lỗi lưu thông báo của một người nhận được catch và ghi log
 * riêng biệt, tuyệt đối không làm rollback allocation hay ngắt việc lưu thông báo của người
 * nhận còn lại.
 */
@Component
@Primary
public class DatabaseAllocationNotificationAdapter implements AllocationNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(DatabaseAllocationNotificationAdapter.class);

    private final LoadProjectPort loadProjectPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveNotificationPort saveNotificationPort;

    public DatabaseAllocationNotificationAdapter(
            LoadProjectPort loadProjectPort,
            LoadEmployeePort loadEmployeePort,
            SaveNotificationPort saveNotificationPort
    ) {
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.saveNotificationPort = Objects.requireNonNull(saveNotificationPort, "SaveNotificationPort must not be null");
    }

    @Override
    public void notifyAllocationChanged(
            Long projectId,
            Long affectedEmployeeId,
            Long actorUserId,
            String title,
            String content
    ) {
        if (projectId == null) {
            log.warn("Bỏ qua thông báo phân bổ do projectId bị null");
            return;
        }

        try {
            UserId senderUserId = actorUserId != null ? new UserId(actorUserId) : null;
            UserId pmUserId = null;

            // 1. Xác định PM của dự án
            try {
                Project project = loadProjectPort.findById(new ProjectId(projectId)).orElse(null);
                if (project != null && project.getManagerId() != null) {
                    Employee pmEmployee = loadEmployeePort.findById(project.getManagerId()).orElse(null);
                    if (pmEmployee != null && pmEmployee.getUserId() != null) {
                        pmUserId = pmEmployee.getUserId();
                    }
                }
            } catch (Exception ex) {
                log.warn("Không thể tra cứu thông tin PM của dự án ID {}: {}", projectId, ex.getMessage());
            }

            // 2. Xác định Nhân sự bị ảnh hưởng
            UserId employeeUserId = null;
            if (affectedEmployeeId != null) {
                try {
                    Employee affectedEmployee = loadEmployeePort.findById(new EmployeeId(affectedEmployeeId)).orElse(null);
                    if (affectedEmployee != null && affectedEmployee.getUserId() != null) {
                        employeeUserId = affectedEmployee.getUserId();
                    }
                } catch (Exception ex) {
                    log.warn("Không thể tra cứu thông tin nhân sự bị ảnh hưởng ID {}: {}", affectedEmployeeId, ex.getMessage());
                }
            }

            Notification pmNotification = pmUserId != null
                    ? Notification.create(
                            pmUserId,
                            senderUserId,
                            NotificationType.ALLOCATION_CHANGED,
                            "PROJECT_ALLOCATION",
                            projectId,
                            title,
                            content
                    )
                    : null;

            Notification empNotification = (employeeUserId != null && !employeeUserId.equals(pmUserId))
                    ? Notification.create(
                            employeeUserId,
                            senderUserId,
                            NotificationType.ALLOCATION_CHANGED,
                            "PROJECT_ALLOCATION",
                            projectId,
                            title,
                            content
                    )
                    : null;

            // 3. Lưu thông báo trực tiếp trong cùng transaction với allocation
            persistSingleNotificationSafely(pmNotification, "PM", projectId);
            persistSingleNotificationSafely(empNotification, "Nhân sự", projectId);

        } catch (Exception e) {
            log.error("Lỗi chuẩn bị thông báo phân bổ cho dự án ID {}: {}", projectId, e.getMessage(), e);
        }
    }

    private void persistSingleNotificationSafely(Notification notification, String recipientRole, Long projectId) {
        if (notification == null) {
            return;
        }
        try {
            saveNotificationPort.save(notification);
            log.info("Đã tạo thông báo phân bổ cho {} (UserId: {}) của dự án ID: {}",
                    recipientRole, notification.getRecipientId().value(), projectId);
        } catch (Exception e) {
            // Recipient Isolation: lỗi lưu thông báo của người nhận này không ảnh hưởng đến người nhận khác
            log.error("Lỗi hạ tầng khi lưu thông báo phân bổ cho {} (UserId: {}) trong dự án ID {} (Recipient Isolation): {}",
                    recipientRole, notification.getRecipientId().value(), projectId, e.getMessage(), e);
        }
    }
}
