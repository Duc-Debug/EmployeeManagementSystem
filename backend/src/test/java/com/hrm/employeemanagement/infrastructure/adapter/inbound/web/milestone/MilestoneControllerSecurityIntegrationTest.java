package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.milestone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.entity.TaskJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.task.repository.SpringDataTaskRepository;
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
class MilestoneControllerSecurityIntegrationTest {

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
    @DisplayName("POST /api/v1/projects/{projectId}/milestones khong dang nhap tra ve 401")
    void testCreateMilestone_NoLogin_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/projects/1/milestones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mốc 1\",\"plannedDate\":\"2026-10-01\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-03: POST /api/v1/projects/{projectId}/milestones voi vai tro khong phai PM (VT-04) tra ve 403 va ghi nhat ky tu choi")
    void testCreateMilestone_UnauthorizedRole_Returns403AndPersistsDeniedAudit() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        UserJpaEntity employeeUser = user(
                "emp-no-pm-" + suffix,
                "VT-04",
                DataScope.SELF,
                null);

        mockMvc.perform(post("/api/v1/projects/1/milestones")
                        .with(authentication(authenticationFor(employeeUser, RoleCode.VT_04)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mốc trái quyền\",\"plannedDate\":\"2026-10-01\"}"))
                .andExpect(status().isForbidden());

        // Verify audit log ghi nhan tu choi truy cap (TC-03)
        List<AuditLogJpaEntity> deniedLogs = auditLogRepository.findAll().stream()
                .filter(l -> ("PERMISSION_DENIED".equals(l.getAction()) || "PROJECT_ACCESS_DENIED".equals(l.getAction()))
                        && employeeUser.getId().equals(l.getUserId()))
                .toList();

        assertThat(deniedLogs).isNotEmpty();
        assertThat(deniedLogs.get(0).getNewValue()).contains("PROJECT_MILESTONE_MANAGE");
    }

    @Test
    @DisplayName("TC-01 & TC-04: PM tao moc tien do thanh cong tren du an co WBS va duoc ghi audit log")
    void testCreateMilestone_ProjectManager_Success() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT")
                .orElseThrow();

        UserJpaEntity pmUser = user(
                "pm-milestone-" + suffix,
                "VT-02",
                DataScope.COMPANY,
                null);

        EmployeeJpaEntity pmEmployee = employeeRepository.saveAndFlush(
                new EmployeeJpaEntity(
                        null,
                        pmUser.getId(),
                        root.getId(),
                        "EMP-PM-" + suffix,
                        pmUser.getUsername(),
                        false,
                        40,
                        "ACTIVE"));

        ProjectJpaEntity project = projectRepository.saveAndFlush(
                new ProjectJpaEntity(
                        null,
                        "PRJ-SEC-" + suffix,
                        "Dự án Milestone Test",
                        root.getId(),
                        pmEmployee.getId(),
                        ProjectStatus.ACTIVE,
                        pmUser.getId(),
                        LocalDateTime.now(),
                        null,
                        0L));

        // Tao 1 task de du an thoa man dieu kien co WBS (TC-01)
        taskRepository.saveAndFlush(new TaskJpaEntity(
                null,
                project.getId(),
                null,
                "TASK-01",
                "Cong viec WBS",
                "Mo ta",
                TaskType.TASK,
                pmEmployee.getId(),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO,
                TaskStatus.TODO,
                0,
                pmUser.getId(),
                LocalDateTime.now(),
                null,
                0L));

        String requestJson = """
                {
                    "name": "Mốc bàn giao đợt 1",
                    "description": "Nghiệm thu phần mềm",
                    "plannedDate": "%s"
                }
                """.formatted(LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/v1/projects/" + project.getId() + "/milestones")
                        .with(authentication(authenticationFor(pmUser, RoleCode.VT_02)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated());

        // Verify audit log ghi nhan thanh cong CREATE_MILESTONE (TC-04)
        List<AuditLogJpaEntity> successLogs = auditLogRepository.findAll().stream()
                .filter(l -> "CREATE_MILESTONE".equals(l.getAction()) && pmUser.getId().equals(l.getUserId()))
                .toList();

        assertThat(successLogs).isNotEmpty();
    }

    private UserJpaEntity user(
            String username,
            String roleCode,
            DataScope dataScope,
            Long scopeOrgUnitId) {
        RoleJpaEntity role = roleRepository
                .findByCode(roleCode)
                .orElseThrow();

        UserJpaEntity user = new UserJpaEntity(
                null,
                username,
                "hash",
                role,
                true);

        user.setDataScope(dataScope.name());
        user.setScopeOrgUnitId(scopeOrgUnitId);

        return userRepository.saveAndFlush(user);
    }

    private UsernamePasswordAuthenticationToken authenticationFor(
            UserJpaEntity user,
            RoleCode roleCode) {
        User principal = new User(
                new UserId(user.getId()),
                user.getUsername(),
                user.getPasswordHash(),
                new Role(
                        new RoleId(user.getRole().getId()),
                        roleCode,
                        roleCode.getName()),
                UserStatus.ACTIVE,
                new EmployeeId(1L),
                DataScope.valueOf(user.getDataScope()),
                user.getScopeOrgUnitId(),
                1L);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(roleCode.getCode())));
    }
}
