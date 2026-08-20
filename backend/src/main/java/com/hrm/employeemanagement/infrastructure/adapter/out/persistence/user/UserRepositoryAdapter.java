package com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user;

import com.hrm.employeemanagement.domain.model.role.Role;
import com.hrm.employeemanagement.domain.model.role.RoleCode;
import com.hrm.employeemanagement.domain.model.user.User;
import com.hrm.employeemanagement.domain.model.user.UserStatus;
import com.hrm.employeemanagement.domain.repository.user.UserRepository;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.out.persistence.user.repository.SpringDataUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataRoleRepository springDataRoleRepository;
    private final SpringDataEmployeeRepository springDataEmployeeRepository;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository,
                                 SpringDataRoleRepository springDataRoleRepository,
                                 SpringDataEmployeeRepository springDataEmployeeRepository) {
        this.springDataUserRepository = springDataUserRepository;
        this.springDataRoleRepository = springDataRoleRepository;
        this.springDataEmployeeRepository = springDataEmployeeRepository;
    }

    @Override
    public User save(User user) {
        RoleJpaEntity roleJpa = springDataRoleRepository.findByCode(user.getRole().getCode().getCode())
                .orElseGet(() -> springDataRoleRepository.save(new RoleJpaEntity(null, user.getRole().getCode().getCode(), user.getRole().getName())));

        UserJpaEntity entity = new UserJpaEntity(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                roleJpa,
                user.isActive()
        );

        UserJpaEntity savedEntity = springDataUserRepository.save(entity);
        Long empId = user.getEmployeeId();
        if (empId == null) {
            empId = springDataEmployeeRepository.findByUserId(savedEntity.getId())
                    .map(e -> e.getId()).orElse(null);
        }

        return mapToDomain(savedEntity, empId);
    }

    @Override
    public Optional<User> findById(Long id) {
        return springDataUserRepository.findById(id)
                .map(entity -> {
                    Long empId = springDataEmployeeRepository.findByUserId(entity.getId())
                            .map(e -> e.getId()).orElse(null);
                    return mapToDomain(entity, empId);
                });
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username)
                .map(entity -> {
                    Long empId = springDataEmployeeRepository.findByUserId(entity.getId())
                            .map(e -> e.getId()).orElse(null);
                    return mapToDomain(entity, empId);
                });
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataUserRepository.existsByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return springDataUserRepository.findAll().stream()
                .map(entity -> {
                    Long empId = springDataEmployeeRepository.findByUserId(entity.getId())
                            .map(e -> e.getId()).orElse(null);
                    return mapToDomain(entity, empId);
                })
                .toList();
    }

    @Override
    public List<User> findAll(int page, int size) {
        return springDataUserRepository.findAll(PageRequest.of(page, size)).stream()
                .map(entity -> {
                    Long empId = springDataEmployeeRepository.findByUserId(entity.getId())
                            .map(e -> e.getId()).orElse(null);
                    return mapToDomain(entity, empId);
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

    private User mapToDomain(UserJpaEntity entity, Long employeeId) {
        RoleCode roleCode = RoleCode.fromCode(entity.getRole().getCode());
        Role role = new Role(entity.getRole().getId(), roleCode, entity.getRole().getName());
        UserStatus status = Boolean.TRUE.equals(entity.getIsActive()) ? UserStatus.ACTIVE : UserStatus.LOCKED;
        return new User(entity.getId(), entity.getUsername(), entity.getPasswordHash(), role, status, employeeId);
    }
}
