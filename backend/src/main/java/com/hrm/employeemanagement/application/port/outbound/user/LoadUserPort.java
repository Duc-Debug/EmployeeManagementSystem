package com.hrm.employeemanagement.application.port.outbound.user;

import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.util.List;
import java.util.Optional;

public interface LoadUserPort {
    Optional<User> findById(UserId id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findAll(int page, int size);
    long count();
    long countActiveAdmins();
}
