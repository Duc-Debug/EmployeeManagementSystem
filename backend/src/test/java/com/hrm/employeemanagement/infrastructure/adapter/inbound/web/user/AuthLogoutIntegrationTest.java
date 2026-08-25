package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.LoginRequest;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.AuditLogJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthLogoutIntegrationTest {

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
    private SpringDataAuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_USERNAME = "admin_logout_e2e";
    private static final String TEST_PASSWORD = "password123";
    private Long testUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        auditLogRepository.deleteAll();

        RoleJpaEntity adminRole = roleRepository.findByCode("VT-06")
                .orElseGet(() -> roleRepository.save(new RoleJpaEntity(null, "VT-06", "Quản trị viên")));

        UserJpaEntity user = userRepository.findByUsername(TEST_USERNAME).orElseGet(() -> {
            UserJpaEntity u = new UserJpaEntity(
                    null,
                    TEST_USERNAME,
                    passwordEncoder.encode(TEST_PASSWORD),
                    adminRole,
                    true
            );
            u.setDataScope(DataScope.COMPANY.name());
            u = userRepository.save(u);

            employeeRepository.save(new EmployeeJpaEntity(
                    null,
                    u.getId(),
                    null,
                    "EMP-LOGOUT-TEST",
                    "Admin Logout Tester",
                    false,
                    40,
                    "ACTIVE"
            ));
            return u;
        });
        testUserId = user.getId();
    }

    @Test
    @DisplayName("E2E: Đăng nhập -> Gọi API bảo vệ thành công -> Đăng xuất -> Gọi lại API bảo vệ bị chặn 401 Unauthorized")
    void testEndToEnd_Login_AccessProtectedApi_Logout_AccessDenied() throws Exception {
        // 1. Login to obtain JWT Token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TEST_USERNAME);
        loginRequest.setPassword(TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = rootNode.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        // 2. Call protected API (/api/v1/users/{id}) -> Should be 200 OK
        mockMvc.perform(get("/api/v1/users/" + testUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Perform Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

        // 4. Try accessing protected API again with the same token -> Must return 401 Unauthorized
        mockMvc.perform(get("/api/v1/users/" + testUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // 5. Verify audit log entry was created specifically for this user and action
        List<AuditLogJpaEntity> logs = auditLogRepository.findAll();
        List<AuditLogJpaEntity> userLogoutLogs = logs.stream()
                .filter(l -> testUserId.equals(l.getUserId()) && "LOGOUT".equals(l.getAction()))
                .toList();
        assertThat(userLogoutLogs).hasSize(1);
    }

    @Test
    @DisplayName("E2E: Đăng nhập 2 phiên -> Đăng xuất allDevices=true -> Cả 2 phiên đều bị thu hồi và bị chặn 401")
    void testEndToEnd_LogoutAllDevices_RevokesAllSessions() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TEST_USERNAME);
        loginRequest.setPassword(TEST_PASSWORD);

        // Session 1
        MvcResult loginResult1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String token1 = objectMapper.readTree(loginResult1.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // Session 2
        MvcResult loginResult2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String token2 = objectMapper.readTree(loginResult2.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // Both sessions can access protected API
        mockMvc.perform(get("/api/v1/users/" + testUserId).header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/" + testUserId).header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());

        // Logout all devices using Session 1
        mockMvc.perform(post("/api/v1/auth/logout?allDevices=true")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đăng xuất khỏi tất cả thiết bị thành công"));

        // Both Session 1 and Session 2 must now be rejected with 401 Unauthorized
        mockMvc.perform(get("/api/v1/users/" + testUserId).header("Authorization", "Bearer " + token1))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/" + testUserId).header("Authorization", "Bearer " + token2))
                .andExpect(status().isUnauthorized());

        // Verify LOGOUT_ALL audit log specifically for this user
        List<AuditLogJpaEntity> logs = auditLogRepository.findAll();
        List<AuditLogJpaEntity> userLogoutAllLogs = logs.stream()
                .filter(l -> testUserId.equals(l.getUserId()) && "LOGOUT_ALL".equals(l.getAction()))
                .toList();
        assertThat(userLogoutAllLogs).hasSize(1);
    }

    @Test
    @DisplayName("Boundary: Token mới được tạo sau logout-all vẫn phải hợp lệ, token cũ bị thu hồi")
    void testEndToEnd_LogoutAll_NewTokenIssuedAfterwards_RemainsValidAndAccessGranted() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TEST_USERNAME);
        loginRequest.setPassword(TEST_PASSWORD);

        // 1. First Login -> tokenOld
        MvcResult loginResultOld = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String tokenOld = objectMapper.readTree(loginResultOld.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // 2. Perform Logout All Devices
        mockMvc.perform(post("/api/v1/auth/logout?allDevices=true")
                        .header("Authorization", "Bearer " + tokenOld))
                .andExpect(status().isOk());

        // 3. Old Token MUST be rejected
        mockMvc.perform(get("/api/v1/users/" + testUserId).header("Authorization", "Bearer " + tokenOld))
                .andExpect(status().isUnauthorized());

        // 4. Sleep briefly to ensure new token issuedAt > revocation timestamp
        Thread.sleep(50);

        // 5. Second Login -> tokenNew
        MvcResult loginResultNew = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String tokenNew = objectMapper.readTree(loginResultNew.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // 6. New token MUST be accepted (200 OK)
        mockMvc.perform(get("/api/v1/users/" + testUserId).header("Authorization", "Bearer " + tokenNew))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Idempotency: Đăng xuất nhiều lần liên tiếp với cùng một token được xử lý an toàn")
    void testEndToEnd_MultipleConsecutiveLogouts_IsIdempotentAndSafe() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TEST_USERNAME);
        loginRequest.setPassword(TEST_PASSWORD);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // 1st Logout -> 200 OK
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2nd Logout with same token -> 200 OK (Idempotent, no 500 error)
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3rd Logout with same token -> 200 OK
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Calling protected endpoint remains 401 Unauthorized
        mockMvc.perform(get("/api/v1/users/" + testUserId).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: Gửi token không hợp lệ / bị làm giả (tampered) được xử lý an toàn và không gây sập hệ thống")
    void testEndToEnd_LogoutWithTamperedOrMalformedToken_HandledGracefully() throws Exception {
        String malformedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalidPayload.invalidSignature";

        // Logout with malformed token -> Should return 200 OK gracefully without throwing 500
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + malformedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Access protected API with malformed token -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/users/" + testUserId)
                        .header("Authorization", "Bearer " + malformedToken))
                .andExpect(status().isUnauthorized());
    }
}
