package com.hrm.employeemanagement.infrastructure.adapter.outbound.allocation;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.user.UserId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseAllocationNotificationAdapter Tests (AC-01, Assumption Gate #5)")
class DatabaseAllocationNotificationAdapterTest {

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private SaveNotificationPort saveNotificationPort;

    private DatabaseAllocationNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DatabaseAllocationNotificationAdapter(
                loadProjectPort,
                loadEmployeePort,
                saveNotificationPort
        );
    }

    @Test
    @DisplayName("AC-01 / BR-02: Thông báo được tạo cho cả PM và Nhân sự bị ảnh hưởng")
    void notifyAllocationChanged_SendsToBothPmAndEmployee() {
        Long projectId = 10L;
        Long affectedEmployeeId = 100L;
        Long actorUserId = 5L;

        Employee pmEmp = new Employee(
                new EmployeeId(200L), new UserId(20L), 1L, "EMP002", "Trần PM",
                false, 40, EmployeeStatus.ACTIVE
        );
        Employee affectedEmp = new Employee(
                new EmployeeId(100L), new UserId(30L), 1L, "EMP001", "Nguyễn Nhân Sự",
                false, 40, EmployeeStatus.ACTIVE
        );

        Project project = new Project(
                new ProjectId(projectId), "PRJ-01", "Dự án HRM", 1L, new EmployeeId(200L),
                null, null, null, null, ProjectStatus.ACTIVE, new UserId(1L), null, null, 1L
        );

        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadEmployeePort.findById(new EmployeeId(200L))).thenReturn(Optional.of(pmEmp));
        when(loadEmployeePort.findById(new EmployeeId(affectedEmployeeId))).thenReturn(Optional.of(affectedEmp));

        adapter.notifyAllocationChanged(projectId, affectedEmployeeId, actorUserId, "Tiêu đề", "Nội dung");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(saveNotificationPort, times(2)).save(captor.capture());

        var savedNotifications = captor.getAllValues();
        assertEquals(2, savedNotifications.size());

        Notification pmNotif = savedNotifications.get(0);
        assertEquals(new UserId(20L), pmNotif.getRecipientId());
        assertEquals(NotificationType.ALLOCATION_CHANGED, pmNotif.getType());

        Notification empNotif = savedNotifications.get(1);
        assertEquals(new UserId(30L), empNotif.getRecipientId());
        assertEquals(NotificationType.ALLOCATION_CHANGED, empNotif.getType());
    }

    @Test
    @DisplayName("In-transaction save: Thông báo được lưu NGAY LẬP TỨC khi notifyAllocationChanged được gọi (không defer afterCommit)")
    void notifyAllocationChanged_SavesImmediatelyWithinSameCall() {
        Long projectId = 10L;
        Long affectedEmployeeId = 100L;
        Long actorUserId = 5L;

        Employee pmEmp = new Employee(
                new EmployeeId(200L), new UserId(20L), 1L, "EMP002", "Trần PM",
                false, 40, EmployeeStatus.ACTIVE
        );
        Project project = new Project(
                new ProjectId(projectId), "PRJ-01", "Dự án HRM", 1L, new EmployeeId(200L),
                null, null, null, null, ProjectStatus.ACTIVE, new UserId(1L), null, null, 1L
        );

        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadEmployeePort.findById(new EmployeeId(200L))).thenReturn(Optional.of(pmEmp));
        when(loadEmployeePort.findById(new EmployeeId(affectedEmployeeId))).thenReturn(Optional.empty());

        // Khi gọi notify, notification phải được lưu ngay lập tức (không phải sau commit)
        adapter.notifyAllocationChanged(projectId, affectedEmployeeId, actorUserId, "Tiêu đề", "Nội dung");

        // Xác nhận save được gọi ngay (chỉ có PM vì employee không tìm thấy)
        verify(saveNotificationPort, times(1)).save(any());
    }

    @Test
    @DisplayName("Same transaction: Lỗi hạ tầng khi chuẩn bị notification phải được ném ra ngoài để rollback")
    void notifyAllocationChanged_DatabaseFailure_PropagatesException() {
        Long projectId = 10L;
        Long affectedEmployeeId = 100L;

        when(loadProjectPort.findById(any())).thenThrow(new RuntimeException("Database connection timeout"));

        assertThrows(RuntimeException.class, () -> adapter.notifyAllocationChanged(
                projectId, affectedEmployeeId, 1L, "Tiêu đề", "Nội dung"
        ));
    }

    @Test
    @DisplayName("Same transaction: Lỗi lưu notification của PM phải fail-fast để rollback toàn bộ allocation")
    void notifyAllocationChanged_WhenPmSaveFails_PropagatesAndSkipsEmployeeNotification() {
        Long projectId = 10L;
        Long affectedEmployeeId = 100L;
        Long actorUserId = 5L;

        Employee pmEmp = new Employee(
                new EmployeeId(200L), new UserId(20L), 1L, "EMP002", "Trần PM",
                false, 40, EmployeeStatus.ACTIVE
        );
        Employee affectedEmp = new Employee(
                new EmployeeId(100L), new UserId(30L), 1L, "EMP001", "Nguyễn Nhân Sự",
                false, 40, EmployeeStatus.ACTIVE
        );
        Project project = new Project(
                new ProjectId(projectId), "PRJ-01", "Dự án HRM", 1L, new EmployeeId(200L),
                null, null, null, null, ProjectStatus.ACTIVE, new UserId(1L), null, null, 1L
        );

        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(project));
        when(loadEmployeePort.findById(new EmployeeId(200L))).thenReturn(Optional.of(pmEmp));
        when(loadEmployeePort.findById(new EmployeeId(affectedEmployeeId))).thenReturn(Optional.of(affectedEmp));

        when(saveNotificationPort.save(any()))
                .thenThrow(new RuntimeException("DB deadlock on PM notification"));

        assertThrows(RuntimeException.class, () -> adapter.notifyAllocationChanged(
                projectId, affectedEmployeeId, actorUserId, "Tiêu đề", "Nội dung"
        ));

        verify(saveNotificationPort, times(1)).save(any(Notification.class));
    }
}
