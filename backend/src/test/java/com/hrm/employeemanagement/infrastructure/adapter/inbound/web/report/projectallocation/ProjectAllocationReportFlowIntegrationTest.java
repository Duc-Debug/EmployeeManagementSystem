package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.projectallocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataAllocationChangeLogRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectResourceDemandRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillGroupJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillGroupRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Project Allocation Flow Integration Tests (End-to-End API -> Service -> DB -> Report)")
class ProjectAllocationReportFlowIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataEmployeeRepository employeeRepository;

    @Autowired
    private SpringDataOrgUnitRepository orgUnitRepository;

    @Autowired
    private SpringDataProjectRepository projectRepository;

    @Autowired
    private SpringDataProjectRoleRepository projectRoleRepository;

    @Autowired
    private SpringDataSkillGroupRepository skillGroupRepository;

    @Autowired
    private SpringDataProjectResourceDemandRepository demandRepository;

    @Autowired
    private SpringDataWeeklyProjectAllocationRepository allocationRepository;

    @Autowired
    private SpringDataAllocationChangeLogRepository allocationChangeLogRepository;

    @Autowired
    private SpringDataAuditLogRepository auditLogRepository;

    private MockMvc mockMvc;
    private UserJpaEntity testUser;
    private EmployeeJpaEntity pmEmployee;
    private EmployeeJpaEntity developerEmployee;
    private OrgUnitJpaEntity orgUnit;
    private ProjectJpaEntity project;
    private SkillGroupJpaEntity skillGroup;
    private ProjectRoleJpaEntity devRole;
    private ProjectRoleJpaEntity testRole;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // 1. OrgUnit
        orgUnit = new OrgUnitJpaEntity(
                null,
                "OU_FLOW_" + System.nanoTime(),
                "Phòng Công Nghệ Flow",
                OrgUnitType.DEPARTMENT,
                null,
                "/",
                1,
                OrgUnitStatus.ACTIVE,
                "Mô tả",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        orgUnit = orgUnitRepository.saveAndFlush(orgUnit);

        // 2. Role & User (Resource Manager VT-03 has both allocation manage & report read permissions)
        RoleJpaEntity rmRole = roleRepository.findByCode("VT-03").orElseThrow();
        testUser = new UserJpaEntity(
                null,
                "flow_rm_" + System.nanoTime(),
                "password_hash",
                rmRole,
                true
        );
        testUser.setDataScope(DataScope.ORGANIZATION_BRANCH.name());
        testUser.setScopeOrgUnitId(orgUnit.getId());
        testUser = userRepository.saveAndFlush(testUser);

        // 3. Employees
        pmEmployee = new EmployeeJpaEntity(
                null,
                null,
                orgUnit.getId(),
                "NV_PM_" + System.nanoTime(),
                "Quản Lý Dự Án PM",
                "Project Manager",
                LocalDate.of(2020, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE.name()
        );
        pmEmployee = employeeRepository.saveAndFlush(pmEmployee);

        developerEmployee = new EmployeeJpaEntity(
                null,
                null,
                orgUnit.getId(),
                "NV_DEV_" + System.nanoTime(),
                "Kỹ Sư Phần Mềm A",
                "Senior Software Engineer",
                LocalDate.of(2021, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE.name()
        );
        developerEmployee = employeeRepository.saveAndFlush(developerEmployee);

        // 4. Project
        project = new ProjectJpaEntity(
                null,
                "PRJ_FLOW_" + System.nanoTime(),
                "Dự Án HRM Flow Test",
                orgUnit.getId(),
                pmEmployee.getId(),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20),
                BigDecimal.valueOf(160.0),
                "Mô tả dự án",
                ProjectStatus.ACTIVE,
                testUser.getId(),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now(),
                0L
        );
        project = projectRepository.saveAndFlush(project);

        // 5. Skill Group & Project Roles
        skillGroup = new SkillGroupJpaEntity(
                null,
                "SG_FLOW_" + System.nanoTime(),
                "Skill Group for flow test",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        skillGroup = skillGroupRepository.saveAndFlush(skillGroup);

        devRole = new ProjectRoleJpaEntity(
                null,
                "DEV_FLOW_" + System.nanoTime(),
                "Lập trình viên Flow",
                "Dev role",
                skillGroup.getId(),
                "ACTIVE"
        );
        devRole = projectRoleRepository.saveAndFlush(devRole);

        testRole = new ProjectRoleJpaEntity(
                null,
                "TEST_FLOW_" + System.nanoTime(),
                "Kiểm thử viên Flow",
                "Test role",
                skillGroup.getId(),
                "ACTIVE"
        );
        testRole = projectRoleRepository.saveAndFlush(testRole);

        // 6. Demand: Need 40h for devRole in week 38/2026
        ProjectResourceDemandJpaEntity demand = new ProjectResourceDemandJpaEntity(
                null,
                project.getId(),
                devRole.getId(),
                2026,
                38,
                BigDecimal.valueOf(40.0),
                0L
        );
        demandRepository.saveAndFlush(demand);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        allocationChangeLogRepository.deleteAll();
        if (project != null && project.getId() != null) {
            List<WeeklyProjectAllocationJpaEntity> allocs = allocationRepository.findByProjectIdAndYearAndWeekNumberBetween(project.getId(), 2026, 1, 53);
            allocationRepository.deleteAll(allocs);
            List<ProjectResourceDemandJpaEntity> demands = demandRepository.findByProjectId(project.getId());
            demandRepository.deleteAll(demands);
            projectRepository.deleteById(project.getId());
        }
        if (devRole != null && devRole.getId() != null) {
            projectRoleRepository.deleteById(devRole.getId());
        }
        if (testRole != null && testRole.getId() != null) {
            projectRoleRepository.deleteById(testRole.getId());
        }
        if (skillGroup != null && skillGroup.getId() != null) {
            skillGroupRepository.deleteById(skillGroup.getId());
        }
        if (testUser != null && testUser.getId() != null) {
            auditLogRepository.deleteByUserId(testUser.getId());
            userRepository.deleteById(testUser.getId());
        }
        if (developerEmployee != null && developerEmployee.getId() != null) {
            employeeRepository.deleteById(developerEmployee.getId());
        }
        if (pmEmployee != null && pmEmployee.getId() != null) {
            employeeRepository.deleteById(pmEmployee.getId());
        }
        if (orgUnit != null && orgUnit.getId() != null) {
            orgUnitRepository.deleteById(orgUnit.getId());
        }
    }

    private UsernamePasswordAuthenticationToken createAuth() {
        User domainUser = new User(
                new UserId(testUser.getId()),
                testUser.getUsername(),
                testUser.getPasswordHash(),
                new Role(new RoleId(testUser.getRole().getId()), RoleCode.VT_03, testUser.getRole().getName()),
                UserStatus.ACTIVE,
                new EmployeeId(1L),
                DataScope.ORGANIZATION_BRANCH,
                orgUnit.getId(),
                0L
        );
        return new UsernamePasswordAuthenticationToken(
                domainUser,
                null,
                List.of(new SimpleGrantedAuthority(RoleCode.VT_03.getCode()))
        );
    }

    @Test
    @DisplayName("End-to-End: POST /api/v1/allocations với projectRoleId -> WeeklyProjectAllocation được lưu kèm projectRoleId -> Báo cáo hiển thị chính xác theo role đó")
    void testEndToEnd_AllocateWithProjectRoleId_FlowsToReportDirectly() throws Exception {
        // Step 1: Gọi POST /api/v1/allocations với projectRoleId = devRole.getId()
        String requestJson = String.format("""
                {
                    "employeeId": %d,
                    "projectId": %d,
                    "projectRoleId": %d,
                    "year": 2026,
                    "weekNumber": 38,
                    "allocatedHours": 30.0
                }
                """, developerEmployee.getId(), project.getId(), devRole.getId());

        mockMvc.perform(post("/api/v1/allocations")
                        .with(authentication(createAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Kiểm tra trong database: weekly_project_allocations phải có project_role_id = devRole.getId()
        List<WeeklyProjectAllocationJpaEntity> dbAllocs = allocationRepository.findByProjectIdAndYearAndWeekNumberBetween(
                project.getId(), 2026, 38, 38
        );
        assertEquals(1, dbAllocs.size());
        assertEquals(devRole.getId(), dbAllocs.get(0).getProjectRoleId());

        // Step 2: Gọi GET /api/v1/reports/project-allocation/{projectId}
        mockMvc.perform(get("/api/v1/reports/project-allocation/" + project.getId())
                        .with(authentication(createAuth()))
                        .param("fromYear", "2026")
                        .param("fromWeek", "38")
                        .param("toYear", "2026")
                        .param("toWeek", "38"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDemandHours").value(40.0))
                .andExpect(jsonPath("$.data.totalAllocatedHours").value(30.0))
                .andExpect(jsonPath("$.data.totalShortfallHours").value(10.0))
                .andExpect(jsonPath("$.data.roleBreakdowns[0].roleId").value(devRole.getId()))
                .andExpect(jsonPath("$.data.roleBreakdowns[0].totalDemandHours").value(40.0))
                .andExpect(jsonPath("$.data.roleBreakdowns[0].totalAllocatedHours").value(30.0))
                .andExpect(jsonPath("$.data.roleBreakdowns[0].totalShortfallHours").value(10.0))
                .andExpect(jsonPath("$.data.roleBreakdowns[0].weeklyRoleMetrics[0].allocatedMembers[0].employeeId").value(developerEmployee.getId()));
    }

    @Test
    @DisplayName("Validation: POST /api/v1/allocations thiếu projectRoleId -> Trả về 400 VALIDATION_ERROR")
    void testValidation_MissingProjectRoleId_Returns400() throws Exception {
        String requestJson = String.format("""
                {
                    "employeeId": %d,
                    "projectId": %d,
                    "year": 2026,
                    "weekNumber": 38,
                    "allocatedHours": 30.0
                }
                """, developerEmployee.getId(), project.getId());

        mockMvc.perform(post("/api/v1/allocations")
                        .with(authentication(createAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}