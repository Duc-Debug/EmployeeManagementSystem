package com.hrm.employeemanagement.infrastructure.transaction.notification;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

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
import com.hrm.employeemanagement.application.service.notification.NotificationCenterApplicationService;

public class TransactionalNotificationCenterDecorator implements
        GetNotificationCenterUseCase,
        GetUnreadNotificationCountUseCase,
        GetNotificationDetailUseCase,
        MarkNotificationItemReadUseCase,
        MarkAllNotificationItemsReadUseCase,
        DeleteNotificationItemUseCase {

    private final NotificationCenterApplicationService delegate;

    public TransactionalNotificationCenterDecorator(NotificationCenterApplicationService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "NotificationCenterApplicationService must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationCenterPageResult getNotifications(Long currentUserId, GetNotificationCenterQuery query) {
        return delegate.getNotifications(currentUserId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResult getUnreadCount(Long currentUserId) {
        return delegate.getUnreadCount(currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationCenterItemResult getNotificationDetail(Long recipientId, Long currentUserId) {
        return delegate.getNotificationDetail(recipientId, currentUserId);
    }

    @Override
    @Transactional
    public void markAsRead(Long recipientId, Long currentUserId) {
        delegate.markAsRead(recipientId, currentUserId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long currentUserId) {
        delegate.markAllAsRead(currentUserId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long recipientId, Long currentUserId) {
        delegate.deleteNotification(recipientId, currentUserId);
    }
}
