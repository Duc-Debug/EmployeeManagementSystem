package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository;

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
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectMemberJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SpringDataProjectRepositoryScopeTest {

    @Autowired
    private SpringDataProjectRepository projectRepository;

    @Autowired
    private SpringDataProjectMemberRepository projectMemberRepository;

    @Autowired
    private SpringDataOrgUnitRepository orgUnitRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Test
    @DisplayName("Project scoped queries dùng tree_path, manager_id và project_members từ DB")
    void testProjectScopeQueries_UseDatabasePredicates() {
        String suffix =
                String.valueOf(System.nanoTime());

        OrgUnitJpaEntity root = orgUnitRepository
                .findByUnitCode("COMPANY_ROOT")
                .orElseThrow();

        OrgUnitJpaEntity tech = childOrgUnit(
                "PTECH-" + suffix,
                "Tech",
                root
        );

        OrgUnitJpaEntity backend = childOrgUnit(
                "PBE-" + suffix,
                "Backend",
                tech
        );

        OrgUnitJpaEntity qa = childOrgUnit(
                "PQA-" + suffix,
                "QA",
                tech
        );

        OrgUnitJpaEntity hr = childOrgUnit(
                "PHR-" + suffix,
                "HR",
                root
        );

        EmployeeJpaEntity manager = employee(
                "pm-user-" + suffix,
                "EMP-PM-" + suffix,
                tech
        );

        EmployeeJpaEntity member = employee(
                "dev-user-" + suffix,
                "EMP-DEV-" + suffix,
                backend
        );

        EmployeeJpaEntity otherManager = employee(
                "other-pm-" + suffix,
                "EMP-OTHER-PM-" + suffix,
                hr
        );

        ProjectJpaEntity projectA = project(
                "P-A-" + suffix,
                "Project A",
                tech,
                manager
        );

        ProjectJpaEntity projectB = project(
                "P-B-" + suffix,
                "Project B",
                backend,
                manager
        );

        ProjectJpaEntity projectC = project(
                "P-C-" + suffix,
                "Project C",
                qa,
                otherManager
        );

        ProjectJpaEntity projectD = project(
                "P-D-" + suffix,
                "Project D",
                hr,
                otherManager
        );

        projectMemberRepository.save(
                new ProjectMemberJpaEntity(
                        projectB.getId(),
                        member.getId()
                )
        );

        List<ProjectJpaEntity> branchProjects =
                projectRepository.findByOrgUnitBranch(
                        tech.getId(),
                        10,
                        0
                );

        Set<Long> branchProjectIds =
                branchProjects.stream()
                        .map(ProjectJpaEntity::getId)
                        .collect(Collectors.toSet());

        assertEquals(3, branchProjectIds.size());
        assertTrue(branchProjectIds.contains(projectA.getId()));
        assertTrue(branchProjectIds.contains(projectB.getId()));
        assertTrue(branchProjectIds.contains(projectC.getId()));
        assertFalse(branchProjectIds.contains(projectD.getId()));

        assertEquals(
                3L,
                projectRepository.countByOrgUnitBranch(
                        tech.getId()
                )
        );

        assertTrue(
                projectRepository.existsInOrgUnitBranch(
                        projectB.getId(),
                        tech.getId()
                )
        );

        assertFalse(
                projectRepository.existsInOrgUnitBranch(
                        projectD.getId(),
                        tech.getId()
                )
        );

        assertEquals(
                List.of(projectB.getId(), projectA.getId()),
                projectRepository.findManagedBy(
                                manager.getId(),
                                10,
                                0
                        )
                        .stream()
                        .map(ProjectJpaEntity::getId)
                        .toList()
        );

        assertEquals(
                2L,
                projectRepository.countManagedBy(
                        manager.getId()
                )
        );

        assertTrue(
                projectRepository.existsManagedBy(
                        projectA.getId(),
                        manager.getId()
                )
        );

        assertFalse(
                projectRepository.existsManagedBy(
                        projectD.getId(),
                        manager.getId()
                )
        );

        assertEquals(
                List.of(projectB.getId()),
                projectRepository.findMemberProjects(
                                member.getId(),
                                10,
                                0
                        )
                        .stream()
                        .map(ProjectJpaEntity::getId)
                        .toList()
        );

        assertEquals(
                1L,
                projectRepository.countMemberProjects(
                        member.getId()
                )
        );

        assertTrue(
                projectRepository.existsMember(
                        projectB.getId(),
                        member.getId()
                )
        );

        assertFalse(
                projectRepository.existsMember(
                        projectA.getId(),
                        member.getId()
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

    private EmployeeJpaEntity employee(
            String username,
            String employeeCode,
            OrgUnitJpaEntity orgUnit
    ) {
        RoleJpaEntity role = roleRepository
                .findByCode("VT-04")
                .orElseThrow();

        UserJpaEntity user =
                new UserJpaEntity(
                        null,
                        username,
                        "hash",
                        role,
                        true
                );

        user.setDataScope(
                DataScope.SELF.name()
        );

        UserJpaEntity savedUser =
                userRepository.save(user);

        return employeeRepository.save(
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
    }

    private ProjectJpaEntity project(
            String code,
            String name,
            OrgUnitJpaEntity orgUnit,
            EmployeeJpaEntity manager
    ) {
        return projectRepository.save(
                new ProjectJpaEntity(
                        null,
                        code,
                        name,
                        orgUnit.getId(),
                        manager.getId(),
                        ProjectStatus.ACTIVE,
                        null,
                        LocalDateTime.now(),
                        null,
                        null
                )
        );
    }
}
