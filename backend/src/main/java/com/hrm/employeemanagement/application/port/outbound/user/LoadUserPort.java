package com.hrm.employeemanagement.application.port.outbound.user;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public interface LoadUserPort {
    Optional<User> findById(UserId id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findAll(int page, int size);
    List<User> findByOrgUnitBranch(Long scopeOrgUnitId, int page, int size);
    long count();
    long countByOrgUnitBranch(Long scopeOrgUnitId);
    long countActiveAdmins();
}