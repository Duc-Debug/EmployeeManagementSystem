package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import com.hrm.employeemanagement.infrastructure.adapter.outbound.security.JwtTokenProviderAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Acceptance & Integration Test Suite for Feature NCL-01-CN-004:
 * Phân quyền theo vai trò và phạm vi dữ liệu (Role-based & Data Scope Authorization)
 * Covers all 4 Acceptance Test Cases: TC-01, TC-02, TC-03, TC-04.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NCL-01-CN-004: Phân quyền theo vai trò và phạm vi dữ liệu Integration Tests")
class Ncl01Cn004AuthorizationIntegrationTest {

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

    @Autowired
    private JwtTokenProviderAdapter jwtTokenProvider;

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
    @DisplayName("NCL-01-CN-004-TC-01: Xem danh sách dự án thành công theo phạm vi một nhánh")
    void testTC01_GetProjects_ScopedToBranch_Success() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        OrgUnitJpaEntity branchTech = createChildOrgUnit("TC01-TECH-" + suffix, "Tech Center", root);
        OrgUnitJpaEntity branchHr = createChildOrgUnit("TC01-HR-" + suffix, "HR Department", root);

        UserJpaEntity pmUserEntity = createUserEntity("tc01_pm_" + suffix, "VT-02", DataScope.ORGANIZATION_BRANCH, branchTech.getId());
        EmployeeJpaEntity pmEmployee = createEmployeeEntity(pmUserEntity, "EMP-PM-" + suffix, branchTech);

        EmployeeJpaEntity otherManager = createEmployeeEntity(
                createUserEntity("tc01_other_" + suffix, "VT-04", DataScope.SELF, null),
                "EMP-OTHER-" + suffix,
                branchHr
        );

        // Tạo 2 dự án thuộc branchTech và 1 dự án thuộc branchHr
        ProjectJpaEntity techProject1 = createProjectEntity("PRJ-TECH-1-" + suffix, branchTech, pmEmployee, pmUserEntity);
        ProjectJpaEntity techProject2 = createProjectEntity("PRJ-TECH-2-" + suffix, branchTech, pmEmployee, pmUserEntity);
        createProjectEntity("PRJ-HR-1-" + suffix, branchHr, otherManager, pmUserEntity);

        String jwtToken = generateJwtToken(pmUserEntity, RoleCode.VT_02);

        // PM gửi request xem danh sách dự án
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[?(@.projectCode == '" + techProject1.getProjectCode() + "')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.projectCode == '" + techProject2.getProjectCode() + "')]").exists());
    }

    @Test
    @DisplayName("NCL-01-CN-004-TC-02: Truy cập dự án ngoài phạm vi qua link trực tiếp bị từ chối 403 và ghi nhật ký")
    void testTC02_DirectAccessOutsideScope_DeniedAndAuditLogged() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        OrgUnitJpaEntity branchTech = createChildOrgUnit("TC02-TECH-" + suffix, "Tech Center", root);
        OrgUnitJpaEntity branchFinance = createChildOrgUnit("TC02-FIN-" + suffix, "Finance Dept", root);

        UserJpaEntity userEntity = createUserEntity("tc02_user_" + suffix, "VT-02", DataScope.ORGANIZATION_BRANCH, branchTech.getId());
        EmployeeJpaEntity manager = createEmployeeEntity(
                createUserEntity("tc02_mgr_" + suffix, "VT-04", DataScope.SELF, null),
                "EMP-FIN-MGR-" + suffix,
                branchFinance
        );

        // Dự án nằm ở phòng ban Finance (ngoài branchTech)
        ProjectJpaEntity financeProject = createProjectEntity("PRJ-FIN-" + suffix, branchFinance, manager, userEntity);

        String jwtToken = generateJwtToken(userEntity, RoleCode.VT_02);

        // User cố truy cập link trực tiếp dự án của Finance
        mockMvc.perform(get("/api/v1/projects/" + financeProject.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden());

        // Kiểm tra Audit Log có ghi nhận PROJECT_ACCESS_DENIED
        List<AuditLogJpaEntity> deniedLogs = auditLogRepository.findAll().stream()
                .filter(a -> "PROJECT_ACCESS_DENIED".equals(a.getAction())
                        && "projects".equals(a.getTableName())
                        && financeProject.getId().equals(a.getRecordId())
                        && userEntity.getId().equals(a.getUserId()))
                .toList();

        assertThat(deniedLogs).hasSize(1);
        AuditLogJpaEntity log = deniedLogs.get(0);
        assertThat(log.getNewValue())
                .contains("permission=PROJECT_READ")
                .contains("dataScope=ORGANIZATION_BRANCH")
                .contains("reason=OUTSIDE_DATA_SCOPE");
    }

    @Test
    @DisplayName("NCL-01-CN-004-TC-03: Thay đổi quyền khi đang đăng nhập -> Áp dụng quyền mới ngay ở lần thao tác kế tiếp")
    void testTC03_LiveAuthorizationChange_AppliesImmediatelyOnNextRequest() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        OrgUnitJpaEntity branchTech = createChildOrgUnit("TC03-TECH-" + suffix, "Tech Center", root);
        OrgUnitJpaEntity branchSales = createChildOrgUnit("TC03-SALES-" + suffix, "Sales Dept", root);

        // Ban đầu Admin tạo user với quyền SELF (VT-04: Nhân viên chuyên môn)
        UserJpaEntity targetUser = createUserEntity("tc03_target_" + suffix, "VT-04", DataScope.SELF, null);
        EmployeeJpaEntity targetEmployee = createEmployeeEntity(targetUser, "EMP-TC03-" + suffix, branchTech);

        // Dự án thuộc phòng Sales
        ProjectJpaEntity salesProject = createProjectEntity("PRJ-SALES-" + suffix, branchSales, targetEmployee, targetUser);

        // User đăng nhập và có JWT token
        String userJwtToken = generateJwtToken(targetUser, RoleCode.VT_04);

        // Request 1: Với quyền VT-04 + DataScope.SELF, user KHÔNG xem được danh sách dự án của Sales
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // Admin thực hiện nâng quyền user lên Quản lý dự án (VT-02) với phạm vi COMPANY (Toàn công ty)
        UserJpaEntity adminUser = createUserEntity("tc03_admin_" + suffix, "VT-06", DataScope.COMPANY, null);
        String adminJwtToken = generateJwtToken(adminUser, RoleCode.VT_06);

        String updateRolePayload = """
                {
                    "roleCode": "VT-02",
                    "dataScope": "COMPANY",
                    "scopeOrgUnitId": null
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + targetUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRolePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Request 2 (Lần thao tác kế tiếp của User bằng chính JWT token cũ):
        // Hệ thống áp dụng ngay phân quyền mới (COMPANY), user lập tức thấy được dự án
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + userJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.projectCode == '" + salesProject.getProjectCode() + "')]").exists());
    }

    @Test
    @DisplayName("NCL-01-CN-004-TC-04: Lưu lịch sử kiểm toán đầy đủ khi thay đổi phân quyền (Audit Log)")
    void testTC04_UpdateAuthorization_PersistsComprehensiveAuditLog() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        OrgUnitJpaEntity branchTech = createChildOrgUnit("TC04-TECH-" + suffix, "Tech Center", root);

        UserJpaEntity adminUser = createUserEntity("tc04_admin_" + suffix, "VT-06", DataScope.COMPANY, null);
        UserJpaEntity targetUser = createUserEntity("tc04_user_" + suffix, "VT-04", DataScope.SELF, null);
        createEmployeeEntity(targetUser, "EMP-TC04-" + suffix, branchTech);

        String adminJwtToken = generateJwtToken(adminUser, RoleCode.VT_06);

        String updateRolePayload = """
                {
                    "roleCode": "VT-02",
                    "dataScope": "ORGANIZATION_BRANCH",
                    "scopeOrgUnitId": %d
                }
                """.formatted(branchTech.getId());

        // Admin thực hiện phân quyền
        mockMvc.perform(put("/api/v1/users/" + targetUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRolePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleCode").value("VT-02"))
                .andExpect(jsonPath("$.data.dataScope").value("ORGANIZATION_BRANCH"))
                .andExpect(jsonPath("$.data.scopeOrgUnitId").value(branchTech.getId()));

        // Kiểm tra Audit Log được lưu lại
        List<AuditLogJpaEntity> auditLogs = auditLogRepository.findAll().stream()
                .filter(a -> "UPDATE_AUTHORIZATION".equals(a.getAction())
                        && "users".equals(a.getTableName())
                        && targetUser.getId().equals(a.getRecordId()))
                .toList();

        assertThat(auditLogs).isNotEmpty();
        AuditLogJpaEntity audit = auditLogs.get(auditLogs.size() - 1);
        assertThat(audit.getUserId()).isEqualTo(adminUser.getId());
        assertThat(audit.getCreatedAt()).isNotNull();
        assertThat(audit.getOldValue())
                .contains("role=VT-04")
                .contains("dataScope=SELF");
        assertThat(audit.getNewValue())
                .contains("role=VT-02")
                .contains("dataScope=ORGANIZATION_BRANCH")
                .contains("scopeOrgUnitId=" + branchTech.getId());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private OrgUnitJpaEntity createChildOrgUnit(String code, String name, OrgUnitJpaEntity parent) {
        OrgUnitJpaEntity unit = new OrgUnitJpaEntity(
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
        );
        unit.setManagerId(1L);
        OrgUnitJpaEntity saved = orgUnitRepository.saveAndFlush(unit);
        saved.setTreePath(parent.getTreePath() + saved.getId() + "/");
        return orgUnitRepository.saveAndFlush(saved);
    }

    private UserJpaEntity createUserEntity(String username, String roleCode, DataScope dataScope, Long scopeOrgUnitId) {
        RoleJpaEntity role = roleRepository.findByCode(roleCode).orElseThrow();
        UserJpaEntity user = new UserJpaEntity(
                null,
                username,
                "hashed_pwd",
                role,
                true
        );
        user.setDataScope(dataScope.name());
        user.setScopeOrgUnitId(scopeOrgUnitId);
        user.setTokenVersion(1);
        return userRepository.saveAndFlush(user);
    }

    private EmployeeJpaEntity createEmployeeEntity(UserJpaEntity user, String employeeCode, OrgUnitJpaEntity orgUnit) {
        return employeeRepository.saveAndFlush(
                new EmployeeJpaEntity(
                        null,
                        user.getId(),
                        orgUnit.getId(),
                        employeeCode,
                        user.getUsername(),
                        false,
                        40,
                        "ACTIVE"
                )
        );
    }

    private ProjectJpaEntity createProjectEntity(String code, OrgUnitJpaEntity orgUnit, EmployeeJpaEntity manager, UserJpaEntity createdBy) {
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

    private String generateJwtToken(UserJpaEntity userEntity, RoleCode roleCode) {
        User domainUser = new User(
                new UserId(userEntity.getId()),
                userEntity.getUsername(),
                userEntity.getPasswordHash(),
                new Role(
                        new RoleId(userEntity.getRole().getId()),
                        roleCode,
                        roleCode.getName()
                ),
                UserStatus.ACTIVE,
                new EmployeeId(1L),
                DataScope.valueOf(userEntity.getDataScope()),
                userEntity.getScopeOrgUnitId(),
                null,
                null,
                userEntity.getTokenVersion() != null ? userEntity.getTokenVersion() : 1,
                null
        );
        return jwtTokenProvider.generateToken(domainUser);
    }
}
