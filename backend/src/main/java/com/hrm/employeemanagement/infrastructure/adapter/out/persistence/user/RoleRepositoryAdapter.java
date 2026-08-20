package com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user;

import com.hrm.employeemanagement.domain.model.role.Role;
import com.hrm.employeemanagement.domain.model.role.RoleCode;
import com.hrm.employeemanagement.domain.repository.user.RoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository.SpringDataRoleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RoleRepositoryAdapter implements RoleRepository {

    private final SpringDataRoleRepository springDataRoleRepository;

    public RoleRepositoryAdapter(SpringDataRoleRepository springDataRoleRepository) {
        this.springDataRoleRepository = springDataRoleRepository;
    }

    @Override
    public Optional<Role> findById(Long id) {
        return springDataRoleRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<Role> findByCode(RoleCode code) {
        return springDataRoleRepository.findByCode(code.getCode()).map(this::mapToDomain);
    }

    @Override
    public List<Role> findAll() {
        return springDataRoleRepository.findAll().stream().map(this::mapToDomain).toList();
    }

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = new RoleJpaEntity(role.getId(), role.getCode().getCode(), role.getName());
        RoleJpaEntity saved = springDataRoleRepository.save(entity);
        return mapToDomain(saved);
    }

    private Role mapToDomain(RoleJpaEntity entity) {
        return new Role(entity.getId(), RoleCode.fromCode(entity.getCode()), entity.getName());
    }
}
