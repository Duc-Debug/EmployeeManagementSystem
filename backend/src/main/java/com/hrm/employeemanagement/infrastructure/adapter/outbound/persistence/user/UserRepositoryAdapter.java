package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user;

import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserRepositoryAdapter implements LoadUserPort, SaveUserPort {

    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataRoleRepository springDataRoleRepository;
    private final SpringDataEmployeeRepository springDataEmployeeRepository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository,
                                 SpringDataRoleRepository springDataRoleRepository,
                                 SpringDataEmployeeRepository springDataEmployeeRepository,
                                 UserPersistenceMapper mapper) {
        this.springDataUserRepository = springDataUserRepository;
        this.springDataRoleRepository = springDataRoleRepository;
        this.springDataEmployeeRepository = springDataEmployeeRepository;
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
            existingEntity.setUsername(user.getUsername());
            existingEntity.setPasswordHash(user.getPasswordHash());
            existingEntity.setRole(roleJpa);
            existingEntity.setIsActive(user.isActive());
            savedEntity = springDataUserRepository.save(existingEntity);
        } else {
            UserJpaEntity entity = mapper.toJpaEntity(user, roleJpa);
            savedEntity = springDataUserRepository.save(entity);
        }

        Long empId = user.getEmployeeIdValue();
        if (empId == null) {
            empId = springDataEmployeeRepository.findByUserId(savedEntity.getId())
                    .map(e -> e.getId()).orElse(null);
        }

        return mapper.toDomain(savedEntity, empId);
    }

    @Override
    public Optional<User> findById(UserId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return springDataUserRepository.findById(id.value())
                .map(entity -> {
                    Long empId = springDataEmployeeRepository.findByUserId(entity.getId())
                            .map(e -> e.getId()).orElse(null);
                    return mapper.toDomain(entity, empId);
                });
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username)
                .map(entity -> {
                    Long empId = springDataEmployeeRepository.findByUserId(entity.getId())
                            .map(e -> e.getId()).orElse(null);
                    return mapper.toDomain(entity, empId);
                });
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataUserRepository.existsByUsername(username);
    }

    @Override
    public List<User> findAll(int page, int size) {
        return springDataUserRepository.findAll(PageRequest.of(page, size)).stream()
                .map(entity -> {
                    Long empId = springDataEmployeeRepository.findByUserId(entity.getId())
                            .map(e -> e.getId()).orElse(null);
                    return mapper.toDomain(entity, empId);
                })
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
}
