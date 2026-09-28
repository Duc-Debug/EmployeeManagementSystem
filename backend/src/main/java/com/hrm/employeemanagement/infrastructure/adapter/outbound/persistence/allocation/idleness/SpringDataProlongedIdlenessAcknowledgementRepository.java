package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.idleness;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataProlongedIdlenessAcknowledgementRepository extends JpaRepository<ProlongedIdlenessAcknowledgementJpaEntity, Long> {

    Optional<ProlongedIdlenessAcknowledgementJpaEntity> findByEmployeeIdAndFromYearAndFromWeekAndDurationWeeks(
            Long employeeId,
            Integer fromYear,
            Integer fromWeek,
            Integer durationWeeks
    );

    @Query("SELECT p FROM ProlongedIdlenessAcknowledgementJpaEntity p " +
           "WHERE p.fromYear = :fromYear AND p.fromWeek = :fromWeek AND p.durationWeeks = :durationWeeks " +
           "AND p.employeeId IN :employeeIds")
    List<ProlongedIdlenessAcknowledgementJpaEntity> findByPeriodAndEmployeeIds(
            @Param("fromYear") Integer fromYear,
            @Param("fromWeek") Integer fromWeek,
            @Param("durationWeeks") Integer durationWeeks,
            @Param("employeeIds") List<Long> employeeIds
    );
}
