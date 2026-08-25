package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Persistence Adapter for User aggregate root.
 * Completely eliminates N+1 queries by loading only User data, allowing
 * the Application layer to perform optimized batch resolution of Employees.
 */
@Component
public class UserRepositoryAdapter implements LoadUserPort, SaveUserPort {

    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataRoleRepository springDataRoleRepository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository,
                                 SpringDataRoleRepository springDataRoleRepository,
                                 UserPersistenceMapper mapper) {
        this.springDataUserRepository = springDataUserRepository;
        this.springDataRoleRepository = springDataRoleRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        RoleJpaEntity roleJpa = springDataRoleRepository.findByCode(user.getRole().getCode().getCode())
                .orElseGet(() -> springDataRoleRepository.save(new RoleJpaEntity(null, user.getRole().getCode().getCode(), user.getRole().getName())));

        UserJpaEntity savedEntity;
        if (user.getIdValue() != null) {
            UserJpaEntity existingEntity = springDataUserRepository.findById(user.getIdValue())
                    .orElseGet(() -> mapper.toJpaEntity(user, roleJpa));
            mapper.updateJpaEntity(existingEntity, user, roleJpa);
            savedEntity = springDataUserRepository.save(existingEntity);
        } else {
            UserJpaEntity entity = mapper.toJpaEntity(user, roleJpa);
            savedEntity = springDataUserRepository.save(entity);
        }

        return mapper.toDomain(savedEntity, user.getEmployeeIdValue());
    }

    @Override
    public Optional<User> findById(UserId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return springDataUserRepository.findById(id.value())
                .map(entity -> mapper.toDomain(entity, null));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username)
                .map(entity -> mapper.toDomain(entity, null));
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsInOrgUnitBranch(
            Long userId,
            Long scopeOrgUnitId
    ) {
        return springDataUserRepository.existsInOrgUnitBranch(
                userId,
                scopeOrgUnitId
        );
    }

    @Override
    public List<User> findAll(int page, int size) {
        return springDataUserRepository.findAll(PageRequest.of(page, size)).stream()
                .map(entity -> mapper.toDomain(entity, null))
                .toList();
    }

    @Override
    public long count() {
        return springDataUserRepository.count();
    }

    @Override
    public long countActiveAdmins() {
        return springDataUserRepository.countActiveAdmins();
    }
    @Override
    public List<User> findByOrgUnitBranch(
            Long scopeOrgUnitId,
            int page,
            int size
    ) {
        int offset = page * size;

        return springDataUserRepository
                .findByOrgUnitBranch(
                        scopeOrgUnitId,
                        size,
                        offset
                )
                .stream()
                .map(entity -> mapper.toDomain(entity, null))
                .toList();
    }

    @Override
    public long countByOrgUnitBranch(Long scopeOrgUnitId) {
        return springDataUserRepository.countByOrgUnitBranch(scopeOrgUnitId);
    }
}
