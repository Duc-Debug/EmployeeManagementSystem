package com.hrm.employeemanagement.application.port.outbound.user;

import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;

import java.util.List;
import java.util.Optional;

public interface LoadRolePort {
    Optional<Role> findById(RoleId id);
    Optional<Role> findByCode(RoleCode code);
    List<Role> findAll();
    Role save(Role role);
    void lockRoleForUpdate(RoleCode code);
}
