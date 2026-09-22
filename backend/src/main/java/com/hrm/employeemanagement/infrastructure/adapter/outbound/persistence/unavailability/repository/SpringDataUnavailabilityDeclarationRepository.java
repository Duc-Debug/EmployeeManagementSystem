package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.unavailability.repository;

import com.hrm.employeemanagement.domain.unavailability.UnavailabilityStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.unavailability.entity.UnavailabilityDeclarationJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataUnavailabilityDeclarationRepository extends JpaRepository<UnavailabilityDeclarationJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UnavailabilityDeclarationJpaEntity u WHERE u.id = :id")
    Optional<UnavailabilityDeclarationJpaEntity> findByIdForUpdate(@Param("id") Long id);

    List<UnavailabilityDeclarationJpaEntity> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    @Query("SELECT u FROM UnavailabilityDeclarationJpaEntity u WHERE u.status = 'PENDING' ORDER BY u.createdAt ASC")
    List<UnavailabilityDeclarationJpaEntity> findAllPending();

    @Query("SELECT u FROM UnavailabilityDeclarationJpaEntity u WHERE u.status = 'PENDING' AND u.employeeId IN " +
           "(SELECT e.id FROM EmployeeJpaEntity e WHERE e.orgUnitId IN :orgUnitIds) ORDER BY u.createdAt ASC")
    List<UnavailabilityDeclarationJpaEntity> findPendingByOrgUnitIds(@Param("orgUnitIds") List<Long> orgUnitIds);

    @Query("SELECT u FROM UnavailabilityDeclarationJpaEntity u WHERE u.employeeId = :employeeId " +
           "AND u.status IN ('PENDING', 'APPROVED') AND u.startDate <= :endDate AND u.endDate >= :startDate")
    List<UnavailabilityDeclarationJpaEntity> findActiveOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT u FROM UnavailabilityDeclarationJpaEntity u WHERE u.employeeId = :employeeId " +
           "AND u.status = 'APPROVED' AND u.startDate <= :endDate AND u.endDate >= :startDate")
    List<UnavailabilityDeclarationJpaEntity> findApprovedByEmployeeIdAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(u.totalHoursDeducted), 0.00) FROM UnavailabilityDeclarationJpaEntity u " +
           "WHERE u.employeeId = :employeeId AND u.status = 'APPROVED' " +
           "AND u.startDate <= :endDate AND u.endDate >= :startDate")
    BigDecimal sumApprovedHoursBetween(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
