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
 * notification cũng rollback. Nếu lưu notification lỗi, exception được propagate để rollback
 * allocation cùng transaction, đảm bảo tính nhất quán dữ liệu.
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

        UserId senderUserId = actorUserId != null ? new UserId(actorUserId) : null;
        UserId pmUserId = null;

        // 1. Xác định PM của dự án
        Project project = loadProjectPort.findById(new ProjectId(projectId)).orElse(null);
        if (project != null && project.getManagerId() != null) {
            Employee pmEmployee = loadEmployeePort.findById(project.getManagerId()).orElse(null);
            if (pmEmployee != null && pmEmployee.getUserId() != null) {
                pmUserId = pmEmployee.getUserId();
            }
        }

        // 2. Xác định Nhân sự bị ảnh hưởng
        UserId employeeUserId = null;
        if (affectedEmployeeId != null) {
            Employee affectedEmployee = loadEmployeePort.findById(new EmployeeId(affectedEmployeeId)).orElse(null);
            if (affectedEmployee != null && affectedEmployee.getUserId() != null) {
                employeeUserId = affectedEmployee.getUserId();
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
        persistSingleNotification(pmNotification, "PM", projectId);
        persistSingleNotification(empNotification, "Nhân sự", projectId);
    }

    private void persistSingleNotification(Notification notification, String recipientRole, Long projectId) {
        if (notification == null) {
            return;
        }
        saveNotificationPort.save(notification);
        log.info("Đã tạo thông báo phân bổ cho {} (UserId: {}) của dự án ID: {}",
                recipientRole, notification.getRecipientId().value(), projectId);
    }
}
