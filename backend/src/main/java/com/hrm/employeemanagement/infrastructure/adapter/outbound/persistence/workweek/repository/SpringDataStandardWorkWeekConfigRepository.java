package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.workweek.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.workweek.entity.StandardWorkWeekConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataStandardWorkWeekConfigRepository extends JpaRepository<StandardWorkWeekConfigJpaEntity, Long> {

    @Query("SELECT c FROM StandardWorkWeekConfigJpaEntity c LEFT JOIN FETCH c.days WHERE c.scopeKey = :scopeKey")
    Optional<StandardWorkWeekConfigJpaEntity> findByScopeKeyWithDays(@Param("scopeKey") String scopeKey);

    @Query("SELECT c FROM StandardWorkWeekConfigJpaEntity c LEFT JOIN FETCH c.days WHERE c.scopeType = 'COMPANY'")
    Optional<StandardWorkWeekConfigJpaEntity> findCompanyDefaultWithDays();
}

