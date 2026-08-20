package com.hrm.employeemanagement.domain.repository.user;

import com.hrm.employeemanagement.domain.model.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findAll();
    List<User> findAll(int page, int size);
    long count();
    long countActiveAdmins();
}
