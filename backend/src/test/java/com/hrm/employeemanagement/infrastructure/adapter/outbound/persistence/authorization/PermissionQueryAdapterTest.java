package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.authorization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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

    @Test
    @DisplayName("PROJECT_READ duoc cap dung role seed va khong cap cho VT-05")
    void testHasPermission_ProjectReadSeededForExpectedRolesOnly() {
        String suffix =
                String.valueOf(System.nanoTime());

        for (String roleCode : List.of(
                "VT-01",
                "VT-02",
                "VT-03",
                "VT-04",
                "VT-06"
        )) {
            UserJpaEntity user =
                    userWithRole(
                            "project-read-"
                                    + roleCode
                                    + "-"
                                    + suffix,
                            roleCode
                    );

            assertTrue(
                    permissionQueryAdapter.hasPermission(
                            user.getId(),
                            PermissionCode.PROJECT_READ
                    ),
                    roleCode + " must have PROJECT_READ"
            );
        }

        UserJpaEntity hrUser =
                userWithRole(
                        "project-read-vt05-" + suffix,
                        "VT-05"
                );

        assertFalse(
                permissionQueryAdapter.hasPermission(
                        hrUser.getId(),
                        PermissionCode.PROJECT_READ
                )
        );
    }

    private UserJpaEntity userWithRole(
            String username,
            String roleCode
    ) {
        RoleJpaEntity role = roleRepository
                .findByCode(roleCode)
                .orElseThrow();

        UserJpaEntity user = new UserJpaEntity(
                null,
                username,
                "hash",
                role,
                true
        );

        user.setDataScope(
                DataScope.SELF.name()
        );

        return userRepository.saveAndFlush(user);
    }
}
