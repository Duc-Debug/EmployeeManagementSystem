package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.project.dto.CreateProjectFromTemplateRequest;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.entity.ProjectTemplateJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.repository.SpringDataProjectTemplateRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.projecttemplate.repository.SpringDataProjectTemplateTaskRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository.SpringDataTaskRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;

@SpringBootTest
@ActiveProfiles("test")
class CreateProjectFromTemplateIntegrationTest {

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
    private SpringDataTaskRepository taskRepository;

    @Autowired
    private SpringDataProjectTemplateRepository templateRepository;

    @Autowired
    private SpringDataProjectTemplateTaskRepository templateTaskRepository;

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
    @DisplayName("Tích hợp đầy đủ: Gọi API tạo dự án từ mẫu với DB test H2, kiểm tra Flyway V31, JPA và WBS cây công việc")
    void testCreateProjectFromTemplate_FullIntegrationSuccess() throws Exception {
        // 1. Chuẩn bị OrgUnit và User PM
        String suffix = String.valueOf(System.nanoTime());
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();

        OrgUnitJpaEntity devDept = orgUnitRepository.save(new OrgUnitJpaEntity(
                null, "DEPT-" + suffix, "Development Department", OrgUnitType.DEPARTMENT,
                root.getId(), root.getTreePath() + "0/", root.getLevel() + 1,
                OrgUnitStatus.ACTIVE, null, LocalDateTime.now(), null));
        devDept.setTreePath(root.getTreePath() + devDept.getId() + "/");
        devDept = orgUnitRepository.save(devDept);

        UserJpaEntity pmUserEntity = createPmUser("pm_" + suffix, devDept.getId());
        EmployeeJpaEntity pmEmployee = employeeRepository.save(new EmployeeJpaEntity(
                null, pmUserEntity.getId(), devDept.getId(), "EMP-" + suffix, "PM User", false, 40, "ACTIVE"));

        // 2. Kiểm tra template mẫu từ migration V31 có sẵn trong DB
        ProjectTemplateJpaEntity seedTemplate = templateRepository.findByTemplateCode("TPL-DEV-001")
                .orElseThrow(() -> new AssertionError("Seed template TPL-DEV-001 không tồn tại từ migration"));
        Long templateId = seedTemplate.getId();
        int expectedTaskCount = templateTaskRepository.findByTemplateIdOrderBySortOrderAscIdAsc(templateId).size();
        assertThat(expectedTaskCount).isEqualTo(9);

        // 3. Gọi API POST /api/v1/projects/from-template
        String expectedProjectName = "Dự án ERP Tích Hợp Test " + suffix;
        String jsonPayload = String.format("""
            {
                "templateId": %d,
                "projectName": "%s",
                "orgUnitId": %d,
                "managerId": %d,
                "startDate": "2026-10-01",
                "endDate": "2026-12-31",
                "description": "Dự án test tích hợp từ Flyway migration V31"
            }
            """, templateId, expectedProjectName, devDept.getId(), pmEmployee.getId());

        String responseBody = mockMvc.perform(post("/api/v1/projects/from-template")
                .with(authentication(authenticationFor(pmUserEntity, RoleCode.VT_02)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectName").value(expectedProjectName))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        // Trích xuất ID dự án từ response JSON
        String idStr = responseBody.replaceAll(".*\"id\":([0-9]+).*", "$1");
        Long newProjectId = Long.valueOf(idStr);

        // 4. Kiểm tra trong DB: Project và 9 Task được tạo đúng cấu trúc
        ProjectJpaEntity savedProject = projectRepository.findById(newProjectId.longValue()).orElseThrow();
        assertThat(savedProject.getProjectName()).isEqualTo(expectedProjectName);
        assertThat(savedProject.getEstimatedHours()).isEqualByComparingTo(new BigDecimal("120.00"));

        List<TaskJpaEntity> projectTasks = taskRepository.findByProjectIdOrderBySortOrderAscIdAsc(savedProject.getId());
        assertThat(projectTasks).hasSize(9);

        // Kiểm tra tất cả task con có status TODO và assigneeId = null
        for (TaskJpaEntity t : projectTasks) {
            assertThat(t.getStatus()).isEqualTo(com.hrm.employeemanagement.domain.task.TaskStatus.TODO);
            assertThat(t.getAssigneeId()).isNull();
            assertThat(t.getTaskCode()).startsWith(savedProject.getProjectCode() + "-T");
        }

        // Kiểm tra phân cấp WBS: 3 category cha và các task con trỏ đúng cha
        List<TaskJpaEntity> categories = projectTasks.stream()
                .filter(t -> t.getTaskType() == TaskType.CATEGORY)
                .toList();
        assertThat(categories).hasSize(3);

        Map<Long, TaskJpaEntity> taskMap = projectTasks.stream()
                .collect(Collectors.toMap(TaskJpaEntity::getId, t -> t));
        List<TaskJpaEntity> subTasks = projectTasks.stream()
                .filter(t -> t.getTaskType() == TaskType.TASK)
                .toList();
        assertThat(subTasks).hasSize(6);

        for (TaskJpaEntity subTask : subTasks) {
            assertThat(subTask.getParentId()).isNotNull();
            TaskJpaEntity parent = taskMap.get(subTask.getParentId());
            assertThat(parent).isNotNull();
            assertThat(parent.getTaskType()).isEqualTo(TaskType.CATEGORY);
        }
    }

    private UserJpaEntity createPmUser(String username, Long orgUnitId) {
        RoleJpaEntity pmRole = roleRepository.findByCode("VT-02").orElseThrow();
        UserJpaEntity user = new UserJpaEntity(null, username, "dummy_hash", pmRole, true);
        user.setDataScope(DataScope.ORGANIZATION_BRANCH.name());
        user.setScopeOrgUnitId(orgUnitId);
        return userRepository.saveAndFlush(user);
    }

    private UsernamePasswordAuthenticationToken authenticationFor(UserJpaEntity user, RoleCode roleCode) {
        User principal = new User(
                new UserId(user.getId()),
                user.getUsername(),
                user.getPasswordHash(),
                new Role(new RoleId(user.getRole().getId()), roleCode, roleCode.getName()),
                UserStatus.ACTIVE,
                new EmployeeId(1L));

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(roleCode.getCode())));
    }
}
