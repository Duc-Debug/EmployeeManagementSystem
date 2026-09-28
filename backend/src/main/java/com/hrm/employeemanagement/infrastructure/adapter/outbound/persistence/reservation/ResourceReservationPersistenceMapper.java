package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.reservation;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.reservation.ResourceReservation;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.reservation.entity.ResourceReservationJpaEntity;

public class ResourceReservationPersistenceMapper {

    public static ResourceReservation toDomain(ResourceReservationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ResourceReservation(
                entity.getId(),
                entity.getProjectId(),
                entity.getEmployeeId(),
                YearWeek.of(entity.getYearNumber(), entity.getWeekNumber()),
                entity.getReservedHours(),
                entity.getStatus(),
                entity.getConvertedAllocationId(),
                entity.getCancelledReason(),
                entity.getNote(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public static ResourceReservationJpaEntity toEntity(ResourceReservation domain) {
        if (domain == null) {
            return null;
        }
        return new ResourceReservationJpaEntity(
                domain.getId(),
                domain.getProjectId(),
                domain.getEmployeeId(),
                domain.getYear(),
                domain.getWeekNumber(),
                domain.getReservedHours(),
                domain.getStatus(),
                domain.getConvertedAllocationId(),
                domain.getCancelledReason(),
                domain.getNote(),
                domain.getCreatedBy(),
                domain.getCreatedAt(),
                domain.getUpdatedBy(),
                domain.getUpdatedAt(),
                domain.getVersion()
        );
    }
}
