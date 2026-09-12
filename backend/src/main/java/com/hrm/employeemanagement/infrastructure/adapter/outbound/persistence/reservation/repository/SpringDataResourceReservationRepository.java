package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.reservation.repository;

import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.reservation.entity.ResourceReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataResourceReservationRepository extends JpaRepository<ResourceReservationJpaEntity, Long> {

    Optional<ResourceReservationJpaEntity> findFirstByProjectIdAndEmployeeIdAndYearNumberAndWeekNumberAndStatus(
            Long projectId, Long employeeId, int yearNumber, int weekNumber, ReservationStatus status
    );

    List<ResourceReservationJpaEntity> findAllByEmployeeIdAndYearNumberAndWeekNumberAndStatus(
            Long employeeId, int yearNumber, int weekNumber, ReservationStatus status
    );

    List<ResourceReservationJpaEntity> findAllByProjectId(Long projectId);

    List<ResourceReservationJpaEntity> findAllByProjectIdAndStatus(Long projectId, ReservationStatus status);

    @Query("SELECT r FROM ResourceReservationJpaEntity r WHERE r.employeeId IN :employeeIds AND r.status = :status")
    List<ResourceReservationJpaEntity> findAllByEmployeeIdInAndStatus(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("status") ReservationStatus status
    );

    @Query("SELECT r FROM ResourceReservationJpaEntity r WHERE " +
            "(:projectId IS NULL OR r.projectId = :projectId) AND " +
            "(:employeeId IS NULL OR r.employeeId = :employeeId) AND " +
            "(:year IS NULL OR r.yearNumber = :year) AND " +
            "(:weekNumber IS NULL OR r.weekNumber = :weekNumber) AND " +
            "(:status IS NULL OR r.status = :status) " +
            "ORDER BY r.yearNumber DESC, r.weekNumber DESC, r.id DESC")
    List<ResourceReservationJpaEntity> findReservations(
            @Param("projectId") Long projectId,
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year,
            @Param("weekNumber") Integer weekNumber,
            @Param("status") ReservationStatus status
    );
}
