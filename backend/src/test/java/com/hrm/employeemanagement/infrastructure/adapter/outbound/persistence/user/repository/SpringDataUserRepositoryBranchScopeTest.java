package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SpringDataUserRepositoryBranchScopeTest {

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataOrgUnitRepository orgUnitRepository;

    @Test
    @DisplayName("Branch query trả target trong node gốc và descendants, không trả user ngoài nhánh")
    void testFindAndExistsInOrgUnitBranch_UsesTreePathScope() {
        OrgUnitJpaEntity root = orgUnitRepository
                .findByUnitCode("COMPANY_ROOT")
                .orElseThrow();

        String suffix = String.valueOf(System.nanoTime());

        OrgUnitJpaEntity tech = childOrgUnit(
                "TECH-" + suffix,
                "Tech",
                root
        );

        OrgUnitJpaEntity backend = childOrgUnit(
                "BE-" + suffix,
                "Backend",
                tech
        );

        OrgUnitJpaEntity qa = childOrgUnit(
                "QA-" + suffix,
                "QA",
                tech
        );

        OrgUnitJpaEntity hr = childOrgUnit(
                "HR-" + suffix,
                "HR",
                root
        );

        UserJpaEntity techUser = userWithEmployee(
                "tech-user-" + suffix,
                "EMP-TECH-" + suffix,
                tech
        );

        UserJpaEntity backendUser = userWithEmployee(
                "backend-user-" + suffix,
                "EMP-BE-" + suffix,
                backend
        );

        UserJpaEntity qaUser = userWithEmployee(
                "qa-user-" + suffix,
                "EMP-QA-" + suffix,
                qa
        );

        UserJpaEntity hrUser = userWithEmployee(
                "hr-user-" + suffix,
                "EMP-HR-" + suffix,
                hr
        );

        List<UserJpaEntity> branchUsers =
                userRepository.findByOrgUnitBranch(
                        tech.getId(),
                        10,
                        0
                );

        Set<Long> branchUserIds = branchUsers.stream()
                .map(UserJpaEntity::getId)
                .collect(Collectors.toSet());

        assertEquals(3, branchUserIds.size());
        assertTrue(branchUserIds.contains(techUser.getId()));
        assertTrue(branchUserIds.contains(backendUser.getId()));
        assertTrue(branchUserIds.contains(qaUser.getId()));
        assertFalse(branchUserIds.contains(hrUser.getId()));

        assertTrue(
                orgUnitRepository.existsInOrgUnitBranch(
                        backend.getId(),
                        tech.getId()
                )
        );

        assertFalse(
                orgUnitRepository.existsInOrgUnitBranch(
                        hr.getId(),
                        tech.getId()
                )
        );

        assertEquals(
                3L,
                userRepository.countByOrgUnitBranch(tech.getId())
        );

        assertTrue(
                userRepository.existsInOrgUnitBranch(
                        backendUser.getId(),
                        tech.getId()
                )
        );

        assertFalse(
                userRepository.existsInOrgUnitBranch(
                        hrUser.getId(),
                        tech.getId()
                )
        );
    }

    private OrgUnitJpaEntity childOrgUnit(
            String code,
            String name,
            OrgUnitJpaEntity parent
    ) {
        OrgUnitJpaEntity saved = orgUnitRepository.save(
                new OrgUnitJpaEntity(
                        null,
                        code,
                        name,
                        OrgUnitType.DEPARTMENT,
                        parent.getId(),
                        parent.getTreePath() + "0/",
                        parent.getLevel() + 1,
                        OrgUnitStatus.ACTIVE,
                        null,
                        LocalDateTime.now(),
                        null
                )
        );

        saved.setTreePath(
                parent.getTreePath()
                        + saved.getId()
                        + "/"
        );

        return orgUnitRepository.save(saved);
    }

    private UserJpaEntity userWithEmployee(
            String username,
            String employeeCode,
            OrgUnitJpaEntity orgUnit
    ) {
        RoleJpaEntity role = roleRepository
                .findByCode("VT-04")
                .orElseThrow();

        UserJpaEntity user = new UserJpaEntity(
                null,
                username,
                "hash",
                role,
                true
        );

        user.setDataScope(DataScope.SELF.name());

        UserJpaEntity savedUser =
                userRepository.save(user);

        employeeRepository.save(
                new EmployeeJpaEntity(
                        null,
                        savedUser.getId(),
                        orgUnit.getId(),
                        employeeCode,
                        username,
                        false,
                        40,
                        "ACTIVE"
                )
        );

        return savedUser;
    }
}
