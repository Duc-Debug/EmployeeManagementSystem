package com.hrm.employeemanagement.application.service.notification;

import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.notification.NotificationResult;
import com.hrm.employeemanagement.application.port.inbound.notification.GetNotificationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.notification.MarkNotificationReadUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.LoadNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class NotificationApplicationService
        implements GetNotificationsUseCase, MarkNotificationReadUseCase {

    private final LoadNotificationPort loadNotificationPort;
    private final SaveNotificationPort saveNotificationPort;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;

    public NotificationApplicationService(
            LoadNotificationPort loadNotificationPort,
            SaveNotificationPort saveNotificationPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort) {
        this.loadNotificationPort = Objects.requireNonNull(loadNotificationPort, "LoadNotificationPort must not be null");
        this.saveNotificationPort = Objects.requireNonNull(saveNotificationPort, "SaveNotificationPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
    }

    @Override
    public List<NotificationResult> execute(Long recipientUserId) {
        UserId recipientId = new UserId(recipientUserId);
        List<Notification> notifications = loadNotificationPort.findAllByRecipientId(recipientId);
        List<UserId> senderIds = notifications.stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UserId, User> usersById = loadUserPort.findAllByIdIn(senderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UserId, Employee> employeesByUserId = loadEmployeePort.findAllByUserIdIn(senderIds).stream()
                .filter(employee -> employee.getUserId() != null)
                .collect(Collectors.toMap(Employee::getUserId, Function.identity(), (first, ignored) -> first));
        return notifications.stream().map(notification -> mapToResult(notification, usersById, employeesByUserId)).toList();
    }

    @Override
    public void execute(Long notificationId, Long recipientUserId) {
        NotificationId id = NotificationId.of(notificationId);
        Notification notification = loadNotificationPort.findById(id).orElse(null);
        if (notification != null && notification.getRecipientId().value().equals(recipientUserId)) {
            notification.markAsRead();
            saveNotificationPort.save(notification);
        }
    }

    @Override
    public void executeAll(Long recipientUserId) {
        UserId recipientId = new UserId(recipientUserId);
        saveNotificationPort.markAllAsRead(recipientId);
    }

    private NotificationResult mapToResult(Notification n, Map<UserId, User> usersById,
            Map<UserId, Employee> employeesByUserId) {
        String senderName = "Hệ thống";
        String senderEmail = "";
        if (n.getSenderId() != null) {
            User senderUser = usersById.get(n.getSenderId());
            if (senderUser != null) {
                senderEmail = senderUser.getEmail();
                Employee employee = employeesByUserId.get(n.getSenderId());
                if (employee != null && employee.getFullName() != null && !employee.getFullName().isBlank()) {
                    senderName = employee.getFullName();
                } else {
                    senderName = senderUser.getUsername();
                }
            }
        }

        return new NotificationResult(
                n.getId() != null ? n.getId().value() : null,
                n.getRecipientId().value(),
                n.getSenderId() != null ? n.getSenderId().value() : null,
                senderName,
                senderEmail,
                n.getType().name(),
                n.getTargetType(),
                n.getTargetId(),
                n.getTitle(),
                n.getContent(),
                n.isRead(),
                n.getCreatedAt());
    }
}

