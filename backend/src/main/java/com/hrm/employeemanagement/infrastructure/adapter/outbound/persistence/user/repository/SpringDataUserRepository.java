package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByUsername(String username);
    Optional<UserJpaEntity> findByEmail(String email);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.username = :identity OR u.email = :identity")
    Optional<UserJpaEntity> findByUsernameOrEmail(@Param("identity") String identity);

    boolean existsByUsername(String username);

    @Query("SELECT COUNT(u) FROM UserJpaEntity u WHERE u.role.code = 'VT-06' AND u.isActive = true")
    long countActiveAdmins();
}
