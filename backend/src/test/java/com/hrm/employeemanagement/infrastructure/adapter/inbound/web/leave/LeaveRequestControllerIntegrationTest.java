package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity.OrgUnitJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.repository.SpringDataOrgUnitRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NCL-05-CN-007: Toàn bộ REST API hủy đơn nghỉ phép đã duyệt")
class LeaveRequestControllerIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private SpringDataUserRepository userRepository;
    @Autowired private SpringDataRoleRepository roleRepository;
    @Autowired private SpringDataEmployeeRepository employeeRepository;
    @Autowired private SpringDataOrgUnitRepository orgUnitRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;
    private UserJpaEntity staffUser;
    private UserJpaEntity managerUser;
    private EmployeeJpaEntity staffEmployee;
    private EmployeeJpaEntity managerEmployee;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        Long orgUnitId = orgUnitRepository.findByUnitCode("COMPANY_ROOT")
                .map(OrgUnitJpaEntity::getId)
                .orElseThrow();

        RoleJpaEntity staffRole = roleRepository.findByCode("VT-04").orElseThrow();
        RoleJpaEntity managerRole = roleRepository.findByCode("VT-03").orElseThrow();
        String suffix = String.valueOf(System.nanoTime());

        staffUser = createUser("leave-staff-" + suffix, staffRole, DataScope.SELF, null);
        staffEmployee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, staffUser.getId(), orgUnitId, "LEAVE-STAFF-" + suffix,
                "Nhân viên kiểm thử luồng hủy phép", false, 40, "ACTIVE"));

        managerUser = createUser(
                "leave-manager-" + suffix, managerRole,
                DataScope.ORGANIZATION_BRANCH, orgUnitId);
        managerEmployee = employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null, managerUser.getId(), orgUnitId, "LEAVE-RM-" + suffix,
                "Quản lý kiểm thử luồng hủy phép", false, 40, "ACTIVE"));
    }

    @AfterEach
    void tearDown() {
        if (staffUser != null) {
            jdbcTemplate.update("DELETE FROM audit_logs WHERE user_id = ?", staffUser.getId());
        }
        if (managerUser != null) {
            jdbcTemplate.update("DELETE FROM audit_logs WHERE user_id = ?", managerUser.getId());
        }
        if (staffEmployee != null) {
            jdbcTemplate.update("DELETE FROM employee_weekly_availabilities WHERE employee_id = ?", staffEmployee.getId());
            jdbcTemplate.update("DELETE FROM employee_leave_balances WHERE employee_id = ?", staffEmployee.getId());
            jdbcTemplate.update("DELETE FROM leave_requests WHERE employee_id = ?", staffEmployee.getId());
        }
        if (managerEmployee != null) {
            employeeRepository.deleteById(managerEmployee.getId());
        }
        if (staffEmployee != null) {
            employeeRepository.deleteById(staffEmployee.getId());
        }
        if (managerUser != null) {
            userRepository.deleteById(managerUser.getId());
        }
        if (staffUser != null) {
            userRepository.deleteById(staffUser.getId());
        }
    }

    @Test
    @DisplayName("VT-04 nộp đơn -> VT-03 duyệt -> VT-04 xin hủy -> VT-03 duyệt hủy -> phục hồi đủ 40 giờ")
    void fullRestFlow_ApproveThenApproveCancellation_RestoresWeeklyAvailability() throws Exception {
        String staffToken = login(staffUser.getUsername());

        MvcResult submitResult = mockMvc.perform(post("/api/v1/leave-requests")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "leaveType": "ANNUAL",
                                  "startDate": "2026-11-02",
                                  "endDate": "2026-11-04",
                                  "reason": "Nghỉ việc gia đình"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        long leaveRequestId = responseData(submitResult).path("id").asLong();
        assertThat(leaveRequestId).isPositive();

        String managerToken = login(managerUser.getUsername());
        mockMvc.perform(put("/api/v1/leave-requests/{id}/approve", leaveRequestId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Đồng ý cho nghỉ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        assertWeeklyAvailability(new BigDecimal("24.00"), new BigDecimal("16.00"));

        staffToken = login(staffUser.getUsername());
        assertMyLeaveRequestStatus(staffToken, leaveRequestId, "APPROVED");

        mockMvc.perform(put("/api/v1/leave-requests/{id}/request-cancel", leaveRequestId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Kế hoạch cá nhân đã thay đổi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCEL_REQUESTED"));

        // Chờ duyệt hủy vẫn phải giữ nguyên phần giờ đã trừ.
        assertWeeklyAvailability(new BigDecimal("24.00"), new BigDecimal("16.00"));

        managerToken = login(managerUser.getUsername());
        mockMvc.perform(put("/api/v1/leave-requests/{id}/approve-cancel", leaveRequestId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Đồng ý hủy đơn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertWeeklyAvailability(new BigDecimal("0.00"), new BigDecimal("40.00"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM leave_requests WHERE id = ?",
                String.class,
                leaveRequestId)).isEqualTo("CANCELLED");
    }

    private UserJpaEntity createUser(
            String username,
            RoleJpaEntity role,
            DataScope dataScope,
            Long scopeOrgUnitId
    ) {
        UserJpaEntity user = new UserJpaEntity(
                null, username, passwordEncoder.encode(PASSWORD), role, true);
        user.setDataScope(dataScope.name());
        user.setScopeOrgUnitId(scopeOrgUnitId);
        return userRepository.saveAndFlush(user);
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();
        return responseData(result).path("token").asText();
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private void assertMyLeaveRequestStatus(String token, long leaveRequestId, String expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/leave-requests/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode matchingRequest = responseData(result).findValue("id");
        assertThat(matchingRequest).isNotNull();
        assertThat(responseData(result).findValues("id").stream()
                .anyMatch(id -> id.asLong() == leaveRequestId)).isTrue();
        assertThat(responseData(result).findParents("id").stream()
                .filter(request -> request.path("id").asLong() == leaveRequestId)
                .map(request -> request.path("status").asText()))
                .containsExactly(expectedStatus);
    }

    private void assertWeeklyAvailability(BigDecimal approvedLeaveHours, BigDecimal netAvailableHours) {
        var rows = jdbcTemplate.queryForList("""
                SELECT approved_leave_hours, net_available_hours
                FROM employee_weekly_availabilities
                WHERE employee_id = ? AND year_number = 2026 AND week_number = 45
                """, staffEmployee.getId());

        assertThat(rows).hasSize(1);
        assertThat(new BigDecimal(rows.getFirst().get("approved_leave_hours").toString()))
                .isEqualByComparingTo(approvedLeaveHours);
        assertThat(new BigDecimal(rows.getFirst().get("net_available_hours").toString()))
                .isEqualByComparingTo(netAvailableHours);
    }
}
