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
import com.hrm.employeemanagement.infrastructure.adapter.outbound.security.CaffeineTokenBlacklistAdapter;
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
    private CaffeineTokenBlacklistAdapter tokenBlacklistAdapter;

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

        tokenBlacklistAdapter.clear();

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
        auditLogRepository.deleteAll();
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

        // 5. Verify audit log entry was created specifically for this user
        List<AuditLogJpaEntity> logs = auditLogRepository.findAll();
        boolean hasLogoutAudit = logs.stream().anyMatch(l -> "LOGOUT".equals(l.getAction()) && testUserId.equals(l.getUserId()));
        assertThat(hasLogoutAudit).isTrue();
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
        boolean hasLogoutAllAudit = logs.stream().anyMatch(l -> "LOGOUT_ALL".equals(l.getAction()) && testUserId.equals(l.getUserId()));
        assertThat(hasLogoutAllAudit).isTrue();
    }

    @Test
    @DisplayName("E2E: Đăng xuất allDevices=true -> Đăng nhập phiên mới sau đó -> Phiên mới hoạt động bình thường (200 OK)")
    void testEndToEnd_LogoutAll_NewTokenIssuedAfterwards_RemainsValidAndAccessGranted() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TEST_USERNAME);
        loginRequest.setPassword(TEST_PASSWORD);

        // 1. Initial Login & Session
        MvcResult oldLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String oldToken = objectMapper.readTree(oldLoginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // 2. Perform Logout All Devices
        mockMvc.perform(post("/api/v1/auth/logout?allDevices=true")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk());

        // Old token is blocked
        mockMvc.perform(get("/api/v1/users/" + testUserId).header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());

        // 3. Sleep 1.1s to ensure issuedAt timestamp (which has 1-second precision in standard JWT iat claim) of new token is strictly greater than revocation timestamp
        Thread.sleep(1100);

        // 4. Log in anew after logout-all
        MvcResult newLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String newToken = objectMapper.readTree(newLoginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // 5. New token must work cleanly
        mockMvc.perform(get("/api/v1/users/" + testUserId).header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("E2E: Đăng xuất nhiều lần liên tiếp (Idempotency) -> Luôn trả về 200 OK an toàn")
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

        // First logout -> 200 OK
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Second logout with same token -> 200 OK (Idempotent)
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("E2E: Đăng xuất với token giả mạo / hỏng cấu trúc -> Xử lý an toàn không làm crash server")
    void testEndToEnd_LogoutWithTamperedOrMalformedToken_HandledGracefully() throws Exception {
        // Tampered token
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.invalid.signature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Malformed header
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "NotABearerToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}