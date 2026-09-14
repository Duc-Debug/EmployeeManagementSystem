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
    public List<Notification> findAllocationNotifications(List<Long> projectIds, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<NotificationJpaEntity> pageResult;

        if (projectIds == null || projectIds.isEmpty()) {
            pageResult = repository.findAllAllocationNotifications(pageable);
        } else {
            pageResult = repository.findAllocationNotificationsByProjectIds(projectIds, pageable);
        }

        return pageResult.getContent().stream()
                .map(mapper::toDomain)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public long countAllocationNotifications(List<Long> projectIds) {
        Pageable pageable = PageRequest.of(0, 1);
        if (projectIds == null || projectIds.isEmpty()) {
            return repository.findAllAllocationNotifications(pageable).getTotalElements();
        }
        return repository.findAllocationNotificationsByProjectIds(projectIds, pageable).getTotalElements();
    }
}
