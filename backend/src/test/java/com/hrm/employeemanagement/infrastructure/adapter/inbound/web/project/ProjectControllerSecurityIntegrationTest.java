package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;

@SpringBootTest
@ActiveProfiles("test")
class ProjectControllerSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SpringDataOrgUnitRepository orgUnitRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataProjectRepository projectRepository;

    @Autowired
    private SpringDataAuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id} khong dang nhap tra ve 401")
    void testGetProjectById_NoLogin_Returns401()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/projects/1")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/projects thieu PROJECT_READ tra ve 403")
    void testGetProjects_NoProjectRead_Returns403()
            throws Exception {
        String suffix =
                String.valueOf(System.nanoTime());

        UserJpaEntity user =
                user(
                        "no-project-read-" + suffix,
                        "VT-05",
                        DataScope.SELF,
                        null
                );

        mockMvc.perform(
                        get("/api/v1/projects")
                                .with(authentication(
                                        authenticationFor(
                                                user,
                                                RoleCode.VT_05
                                        )
                                ))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Direct URL ngoai branch tra ve 403 nhung denied audit van duoc persist")
    void testGetProjectById_OutsideBranch_PersistsDeniedAudit()
            throws Exception {
        String suffix =
                String.valueOf(System.nanoTime());

        OrgUnitJpaEntity root = orgUnitRepository
                .findByUnitCode("COMPANY_ROOT")
                .orElseThrow();

        OrgUnitJpaEntity tech = childOrgUnit(
                "SEC-TECH-" + suffix,
                "Tech",
                root
        );

        OrgUnitJpaEntity hr = childOrgUnit(
                "SEC-HR-" + suffix,
                "HR",
                root
        );

        UserJpaEntity actor =
                user(
                        "branch-actor-" + suffix,
                        "VT-02",
                        DataScope.ORGANIZATION_BRANCH,
                        tech.getId()
                );

        EmployeeJpaEntity manager =
                employee(
                        "outside-manager-" + suffix,
                        "EMP-OUT-MGR-" + suffix,
                        hr
                );

        ProjectJpaEntity outsideProject =
                project(
                        "SEC-P-" + suffix,
                        hr,
                        manager,
                        actor
                );

        mockMvc.perform(
                        get("/api/v1/projects/" + outsideProject.getId())
                                .with(authentication(
                                        authenticationFor(
                                                actor,
                                                RoleCode.VT_02
                                        )
                                ))
                )
                .andExpect(status().isForbidden());

        List<AuditLogJpaEntity> deniedAudits =
                auditLogRepository.findAll()
                        .stream()
                        .filter(audit ->
                                "PROJECT_ACCESS_DENIED".equals(
                                        audit.getAction()
                                )
                                        && "projects".equals(
                                                audit.getTableName()
                                        )
                                        && outsideProject.getId().equals(
                                                audit.getRecordId()
                                        )
                        )
                        .toList();

        assertThat(deniedAudits)
                .hasSize(1);

        AuditLogJpaEntity audit =
                deniedAudits.get(0);

        assertThat(audit.getUserId())
                .isEqualTo(actor.getId());

        assertThat(audit.getCreatedAt())
                .isNotNull();

        assertThat(audit.getNewValue())
                .contains("permission=PROJECT_READ")
                .contains("dataScope=ORGANIZATION_BRANCH")
                .contains("scopeOrgUnitId=" + tech.getId())
                .contains("reason=OUTSIDE_DATA_SCOPE");
    }

    private OrgUnitJpaEntity childOrgUnit(
            String code,
            String name,
            OrgUnitJpaEntity parent
    ) {
        OrgUnitJpaEntity saved =
                orgUnitRepository.save(
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

    private UserJpaEntity user(
            String username,
            String roleCode,
            DataScope dataScope,
            Long scopeOrgUnitId
    ) {
        RoleJpaEntity role = roleRepository
                .findByCode(roleCode)
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
                dataScope.name()
        );

        user.setScopeOrgUnitId(
                scopeOrgUnitId
        );

        return userRepository.saveAndFlush(user);
    }

    private EmployeeJpaEntity employee(
            String username,
            String employeeCode,
            OrgUnitJpaEntity orgUnit
    ) {
        UserJpaEntity user =
                user(
                        username,
                        "VT-04",
                        DataScope.SELF,
                        null
                );

        return employeeRepository.saveAndFlush(
                new EmployeeJpaEntity(
                        null,
                        user.getId(),
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
            OrgUnitJpaEntity orgUnit,
            EmployeeJpaEntity manager,
            UserJpaEntity createdBy
    ) {
        return projectRepository.saveAndFlush(
                new ProjectJpaEntity(
                        null,
                        code,
                        "Project " + code,
                        orgUnit.getId(),
                        manager.getId(),
                        ProjectStatus.ACTIVE,
                        createdBy.getId(),
                        LocalDateTime.now(),
                        null,
                        null
                )
        );
    }

    private UsernamePasswordAuthenticationToken authenticationFor(
            UserJpaEntity user,
            RoleCode roleCode
    ) {
        User principal =
                new User(
                        new UserId(user.getId()),
                        user.getUsername(),
                        user.getPasswordHash(),
                        new Role(
                                new RoleId(user.getRole().getId()),
                                roleCode,
                                roleCode.getName()
                        ),
                        UserStatus.ACTIVE,
                        new EmployeeId(1L)
                );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(
                        new SimpleGrantedAuthority(
                                roleCode.getCode()
                        )
                )
        );
    }
}
