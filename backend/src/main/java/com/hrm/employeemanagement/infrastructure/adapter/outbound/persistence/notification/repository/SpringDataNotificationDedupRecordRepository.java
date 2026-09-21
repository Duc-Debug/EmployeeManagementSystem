package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationDedupRecordJpaEntity;

@Repository
public interface SpringDataNotificationDedupRecordRepository extends JpaRepository<NotificationDedupRecordJpaEntity, Long> {

    Optional<NotificationDedupRecordJpaEntity> findByActiveDedupKey(String activeDedupKey);

    List<NotificationDedupRecordJpaEntity> findByTargetEntityTypeAndTargetEntityIdAndYearWeekAndStatus(
            String targetEntityType,
            String targetEntityId,
            String yearWeek,
            String status
    );

    @Modifying
    @Query("UPDATE NotificationDedupRecordJpaEntity r " +
            "SET r.status = 'INACTIVE', r.activeDedupKey = NULL, r.resolvedAt = :resolvedAt " +
            "WHERE r.targetEntityType = :targetEntityType " +
            "AND r.targetEntityId = :targetEntityId " +
            "AND r.yearWeek = :yearWeek " +
            "AND r.status = 'ACTIVE'")
    int resolveActiveRecords(
            @Param("targetEntityType") String targetEntityType,
            @Param("targetEntityId") String targetEntityId,
            @Param("yearWeek") String yearWeek,
            @Param("resolvedAt") LocalDateTime resolvedAt
    );
}
