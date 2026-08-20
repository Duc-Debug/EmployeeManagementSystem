package com.hrm.employeemanagement.domain.repository.user;

import com.hrm.employeemanagement.domain.model.role.Role;
import com.hrm.employeemanagement.domain.model.role.RoleCode;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findById(Long id);
    Optional<Role> findByCode(RoleCode code);
    List<Role> findAll();
    Role save(Role role);
}
