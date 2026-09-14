package com.hrm.employeemanagement.infrastructure.adapter.outbound.allocation;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.port.outbound.notification.SaveNotificationPort;
import com.hrm.employeemanagement.domain.notification.Notification;

/**
 * Persister độc lập lưu thông báo phân bổ trong transaction riêng biệt (REQUIRES_NEW)
 * để đảm bảo tính độc lập và Exception Isolation theo đúng hợp đồng kiến trúc (NCL-07-CN-003).
 */
@Component
public class TransactionalAllocationNotificationPersister {

    private final SaveNotificationPort saveNotificationPort;

    public TransactionalAllocationNotificationPersister(SaveNotificationPort saveNotificationPort) {
        this.saveNotificationPort = Objects.requireNonNull(saveNotificationPort, "SaveNotificationPort must not be null");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification saveInNewTransaction(Notification notification) {
        return saveNotificationPort.save(notification);
    }
}
