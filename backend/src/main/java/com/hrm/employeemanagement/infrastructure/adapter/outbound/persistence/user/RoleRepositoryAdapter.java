package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.application.port.outbound.user.LoadRolePort;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RoleRepositoryAdapter implements LoadRolePort {

    private final SpringDataRoleRepository springDataRoleRepository;
    private final UserPersistenceMapper mapper;

    public RoleRepositoryAdapter(SpringDataRoleRepository springDataRoleRepository, UserPersistenceMapper mapper) {
        this.springDataRoleRepository = springDataRoleRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Role> findById(RoleId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return springDataRoleRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByCode(RoleCode code) {
        if (code == null) return Optional.empty();
        return springDataRoleRepository.findByCode(code.getCode()).map(mapper::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return springDataRoleRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = mapper.toJpaEntity(role);
        RoleJpaEntity saved = springDataRoleRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
