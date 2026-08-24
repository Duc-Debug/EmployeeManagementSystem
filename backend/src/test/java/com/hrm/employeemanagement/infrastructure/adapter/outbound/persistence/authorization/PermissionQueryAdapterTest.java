package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.authorization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PermissionQueryAdapterTest {

    @Autowired
    private PermissionQueryAdapter permissionQueryAdapter;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("hasPermission phản ánh role mới trong DB ở lần query kế tiếp")
    void testHasPermission_RoleRevokedInDatabase_ReturnsFalseOnNextQuery() {
        String suffix =
                String.valueOf(System.nanoTime());

        RoleJpaEntity roleWithUserRead = roleRepository
                .findByCode("VT-06")
                .orElseThrow();

        RoleJpaEntity roleWithoutUserRead = roleRepository
                .findByCode("VT-04")
                .orElseThrow();

        UserJpaEntity user = new UserJpaEntity(
                null,
                "authz-revoke-" + suffix,
                "hash",
                roleWithUserRead,
                true
        );

        user.setDataScope(DataScope.SELF.name());

        UserJpaEntity savedUser =
                userRepository.saveAndFlush(user);

        assertTrue(
                permissionQueryAdapter.hasPermission(
                        savedUser.getId(),
                        PermissionCode.USER_READ
                )
        );

        savedUser.setRole(roleWithoutUserRead);
        userRepository.saveAndFlush(savedUser);
        entityManager.clear();

        assertFalse(
                permissionQueryAdapter.hasPermission(
                        savedUser.getId(),
                        PermissionCode.USER_READ
                )
        );
    }
}
