package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.employee;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;

@SpringBootTest
@ActiveProfiles("test")
class EmployeeControllerSecurityIntegrationTest {

    private MockMvc mockMvc;

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
    @DisplayName("GET /api/v1/employees khi chưa đăng nhập trả về 401")
    void getEmployees_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/employees với vai trò VT-06 trả về 200 (Read-Only)")
    void getEmployees_AdminVT06_Returns200() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        UserJpaEntity admin = createUser("admin_" + suffix, "VT-06", DataScope.COMPANY, null);

        mockMvc.perform(get("/api/v1/employees")
                        .with(authentication(authenticationFor(admin, RoleCode.VT_06, "EMPLOYEE_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/employees với vai trò VT-06 bị từ chối 403 (VT-06 không có quyền chỉnh sửa hồ sơ nhân sự)")
    void createEmployee_AdminVT06_Returns403() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        UserJpaEntity admin = createUser("admin_" + suffix, "VT-06", DataScope.COMPANY, null);
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();

        String payload = String.format("""
            {
                "userId": %d,
                "orgUnitId": %d,
                "employeeCode": "EMP-VT06-%s",
                "fullName": "Test VT06 Mutation",
                "professionalRole": "Developer",
                "startDate": "2026-01-01",
                "standardHoursPerWeek": 40
            }
            """, admin.getId(), root.getId(), suffix);

        mockMvc.perform(post("/api/v1/employees")
                        .with(authentication(authenticationFor(admin, RoleCode.VT_06, "EMPLOYEE_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id} với vai trò VT-06 bị từ chối 403 (VT-06 Read-Only)")
    void updateEmployee_AdminVT06_Returns403() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        UserJpaEntity admin = createUser("admin_" + suffix, "VT-06", DataScope.COMPANY, null);
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();
        EmployeeJpaEntity emp = employeeRepository.save(new EmployeeJpaEntity(
                null, admin.getId(), root.getId(), "EMP-UPD-" + suffix, "Original Name", false, 40, "ACTIVE"));

        String payload = String.format("""
            {
                "version": 0,
                "orgUnitId": %d,
                "fullName": "Updated by VT06",
                "professionalRole": "Developer",
                "startDate": "2026-01-01",
                "standardHoursPerWeek": 40
            }
            """, root.getId());

        mockMvc.perform(put("/api/v1/employees/" + emp.getId())
                        .with(authentication(authenticationFor(admin, RoleCode.VT_06, "EMPLOYEE_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/employees với vai trò VT-05 tạo hồ sơ thành công trả về 201")
    void createEmployee_HrVT05_Returns201() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        UserJpaEntity hrUser = createUser("hr_" + suffix, "VT-05", DataScope.COMPANY, null);
        UserJpaEntity targetUser = createUser("staff_" + suffix, "VT-04", DataScope.SELF, null);
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();

        String payload = String.format("""
            {
                "userId": %d,
                "orgUnitId": %d,
                "employeeCode": "EMP-VT05-%s",
                "fullName": "Created by HR",
                "professionalRole": "Developer",
                "startDate": "2026-01-01",
                "standardHoursPerWeek": 40
            }
            """, targetUser.getId(), root.getId(), suffix);

        mockMvc.perform(post("/api/v1/employees")
                        .with(authentication(authenticationFor(hrUser, RoleCode.VT_05, "EMPLOYEE_READ", "EMPLOYEE_UPDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private UserJpaEntity createUser(String username, String roleCode, DataScope dataScope, Long scopeOrgUnitId) {
        RoleJpaEntity role = roleRepository.findByCode(roleCode).orElseThrow();
        UserJpaEntity user = new UserJpaEntity(null, username, "dummy_hash", role, true);
        user.setDataScope(dataScope.name());
        user.setScopeOrgUnitId(scopeOrgUnitId);
        return userRepository.saveAndFlush(user);
    }

    private UsernamePasswordAuthenticationToken authenticationFor(UserJpaEntity user, RoleCode roleCode, String... additionalAuthorities) {
        DataScope scope = user.getDataScope() != null ? DataScope.valueOf(user.getDataScope()) : DataScope.SELF;
        User principal = new User(
                new UserId(user.getId()),
                user.getUsername(),
                user.getPasswordHash(),
                new Role(new RoleId(user.getRole().getId()), roleCode, roleCode.getName()),
                UserStatus.ACTIVE,
                new EmployeeId(1L),
                scope,
                user.getScopeOrgUnitId(),
                0L);

        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(roleCode.getCode()));
        for (String auth : additionalAuthorities) {
            authorities.add(new SimpleGrantedAuthority(auth));
        }

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}
