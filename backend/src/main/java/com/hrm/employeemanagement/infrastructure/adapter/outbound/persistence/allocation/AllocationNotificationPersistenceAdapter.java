package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadAllocationNotificationPort;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.NotificationPersistenceMapper;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationRepository;

@Component
public class AllocationNotificationPersistenceAdapter implements LoadAllocationNotificationPort {

    private final SpringDataNotificationRepository repository;
    private final NotificationPersistenceMapper mapper;

    public AllocationNotificationPersistenceAdapter(
            SpringDataNotificationRepository repository,
            NotificationPersistenceMapper mapper
    ) {
        this.repository = Objects.requireNonNull(repository, "SpringDataNotificationRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "NotificationPersistenceMapper must not be null");
    }

    @Override
    public List<Notification> findAllocationNotifications(Long recipientId, List<Long> projectIds, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<NotificationJpaEntity> pageResult;

        boolean hasProjects = projectIds != null && !projectIds.isEmpty();

        if (recipientId != null) {
            if (hasProjects) {
                pageResult = repository.findByRecipientIdAndProjectIds(recipientId, projectIds, pageable);
            } else {
                pageResult = repository.findByRecipientId(recipientId, pageable);
            }
        } else {
            if (hasProjects) {
                pageResult = repository.findAllocationNotificationsByProjectIds(projectIds, pageable);
            } else {
                // An toàn mặc định: nếu không chỉ định recipientId và không có projectId, không trả về dữ liệu toàn công ty
                return java.util.Collections.emptyList();
            }
        }

        return pageResult.getContent().stream()
                .map(mapper::toDomain)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public long countAllocationNotifications(Long recipientId, List<Long> projectIds) {
        boolean hasProjects = projectIds != null && !projectIds.isEmpty();

        if (recipientId != null) {
            if (hasProjects) {
                return repository.countByRecipientIdAndProjectIds(recipientId, projectIds);
            } else {
                return repository.countByRecipientId(recipientId);
            }
        } else {
            if (hasProjects) {
                return repository.countAllocationNotificationsByProjectIds(projectIds);
            } else {
                // An toàn mặc định
                return 0L;
            }
        }
    }

    @Override
    public List<Notification> findAllCompanyAllocationNotifications(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<NotificationJpaEntity> pageResult = repository.findAllAllocationNotifications(pageable);
        return pageResult.getContent().stream()
                .map(mapper::toDomain)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public long countAllCompanyAllocationNotifications() {
        return repository.countAllAllocationNotifications();
    }
}
