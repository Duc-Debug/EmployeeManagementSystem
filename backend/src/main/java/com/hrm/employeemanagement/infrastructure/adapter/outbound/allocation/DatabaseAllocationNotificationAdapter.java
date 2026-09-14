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
 * Đảm bảo exception isolation: lỗi hạ tầng notification không bao giờ làm rollback allocation đã commit (Assumption Gate #5).
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

            // 3. Tạo thông báo cho PM
            if (pmUserId != null) {
                Notification pmNotification = Notification.create(
                        pmUserId,
                        senderUserId,
                        NotificationType.ALLOCATION_CHANGED,
                        "PROJECT_ALLOCATION",
                        projectId,
                        title,
                        content
                );
                saveNotificationPort.save(pmNotification);
                log.info("Đã tạo thông báo phân bổ cho PM (UserId: {}) của dự án ID: {}", pmUserId.value(), projectId);
            }

            // 4. Tạo thông báo cho Nhân sự bị ảnh hưởng (nếu khác PM)
            if (employeeUserId != null && !employeeUserId.equals(pmUserId)) {
                Notification empNotification = Notification.create(
                        employeeUserId,
                        senderUserId,
                        NotificationType.ALLOCATION_CHANGED,
                        "PROJECT_ALLOCATION",
                        projectId,
                        title,
                        content
                );
                saveNotificationPort.save(empNotification);
                log.info("Đã tạo thông báo phân bổ cho Nhân sự (UserId: {}) trong dự án ID: {}", employeeUserId.value(), projectId);
            }
        } catch (Exception e) {
            // Safe notification: ghi log lỗi hạ tầng mà KHÔNG làm rollback transaction phân bổ (Assumption Gate #5)
            log.error("Lỗi hạ tầng khi lưu thông báo phân bổ vào DB cho dự án ID {}: {}", projectId, e.getMessage(), e);
        }
    }

    @Override
    public String notifyAllocationAdjusted(Long projectId, Long managerId, String message) {
        log.info("[ALLOCATION_NOTIFICATION] Dự án: {}, QLDA: {}, Nội dung: {}", projectId, managerId, message);
        return managerId != null ? String.valueOf(managerId) : null;
    }
}
