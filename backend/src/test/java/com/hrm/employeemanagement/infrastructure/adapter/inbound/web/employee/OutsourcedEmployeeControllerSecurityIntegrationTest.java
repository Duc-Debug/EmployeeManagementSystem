package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.employee;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;

@SpringBootTest
@ActiveProfiles("test")
class OutsourcedEmployeeControllerSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

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
    @DisplayName("NCL-14-CN-001-TC-03: POST /api/v1/employees/outsourced khi chưa đăng nhập trả về 401")
    void declareOutsourced_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/employees/outsourced")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("NCL-14-CN-001-TC-03: POST /api/v1/employees/outsourced với vai trò VT-06 bị từ chối 403 (VT-06 không có quyền)")
    void declareOutsourced_AdminVT06_Returns403() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        UserJpaEntity admin = createUser("admin_" + suffix, "VT-06", DataScope.COMPANY, null);
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();

        String payload = String.format("""
            {
                "orgUnitId": %d,
                "employeeCode": "EXT-VT06-%s",
                "fullName": "Outsourced Person",
                "providerName": "Vendor ABC",
                "professionalRole": "Developer",
                "startDate": "2026-10-01",
                "contractEndDate": "2026-12-31",
                "standardHoursPerWeek": 40
            }
            """, root.getId(), suffix);

        mockMvc.perform(post("/api/v1/employees/outsourced")
                        .with(authentication(authenticationFor(admin, RoleCode.VT_06, "EMPLOYEE_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NCL-14-CN-001-TC-03: POST /api/v1/employees/outsourced với vai trò VT-04 bị từ chối 403")
    void declareOutsourced_StaffVT04_Returns403() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        UserJpaEntity staff = createUser("staff_" + suffix, "VT-04", DataScope.SELF, null);
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();

        String payload = String.format("""
            {
                "orgUnitId": %d,
                "employeeCode": "EXT-VT04-%s",
                "fullName": "Outsourced Person",
                "providerName": "Vendor ABC",
                "professionalRole": "Developer",
                "startDate": "2026-10-01",
                "contractEndDate": "2026-12-31",
                "standardHoursPerWeek": 40
            }
            """, root.getId(), suffix);

        mockMvc.perform(post("/api/v1/employees/outsourced")
                        .with(authentication(authenticationFor(staff, RoleCode.VT_04)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NCL-14-CN-001-TC-01: POST /api/v1/employees/outsourced với vai trò VT-05 tạo thành công trả về 201 Created")
    void declareOutsourced_HrVT05_Returns201() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        UserJpaEntity hrUser = createUser("hr_" + suffix, "VT-05", DataScope.COMPANY, null);
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();

        String payload = String.format("""
            {
                "orgUnitId": %d,
                "employeeCode": "EXT-VT05-%s",
                "fullName": "Chuyên Gia Thuê Ngoài",
                "providerName": "Đối Tác Công Nghệ FPT",
                "professionalRole": "Senior Solution Architect",
                "startDate": "2026-10-01",
                "contractEndDate": "2026-12-31",
                "standardHoursPerWeek": 40
            }
            """, root.getId(), suffix);

        mockMvc.perform(post("/api/v1/employees/outsourced")
                        .with(authentication(authenticationFor(hrUser, RoleCode.VT_05, "EMPLOYEE_READ", "EMPLOYEE_UPDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Chuyên Gia Thuê Ngoài"))
                .andExpect(jsonPath("$.data.providerName").value("Đối Tác Công Nghệ FPT"))
                .andExpect(jsonPath("$.data.isOutsourced").value(true));
    }

    @Test
    @DisplayName("NCL-14-CN-001-TC-02: POST /api/v1/employees/outsourced với ngày kết thúc < ngày bắt đầu trả về 400 Bad Request")
    void declareOutsourced_InvalidDates_Returns400() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        UserJpaEntity hrUser = createUser("hr_" + suffix, "VT-05", DataScope.COMPANY, null);
        OrgUnitJpaEntity root = orgUnitRepository.findByUnitCode("COMPANY_ROOT").orElseThrow();

        String payload = String.format("""
            {
                "orgUnitId": %d,
                "employeeCode": "EXT-INV-%s",
                "fullName": "Chuyên Gia Lỗi Ngày",
                "providerName": "Đối Tác Lỗi",
                "professionalRole": "Developer",
                "startDate": "2026-10-15",
                "contractEndDate": "2026-10-01",
                "standardHoursPerWeek": 40
            }
            """, root.getId(), suffix);

        mockMvc.perform(post("/api/v1/employees/outsourced")
                        .with(authentication(authenticationFor(hrUser, RoleCode.VT_05, "EMPLOYEE_READ", "EMPLOYEE_UPDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ngày kết thúc hợp đồng thuê không được sớm hơn ngày bắt đầu"));
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
