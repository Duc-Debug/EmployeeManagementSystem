package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import com.hrm.employeemanagement.infrastructure.security.JwtAuthenticationFilter;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
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

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class FirstTimePasswordChangeIntegrationTest {

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
    private com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataAuditLogRepository auditLogRepository;

    @Autowired
    private UserStatusCache userStatusCache;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String USERNAME = "first_time_admin_cache_test";
    private static final String INITIAL_PASSWORD = "InitialPassword@123";
    private static final String NEW_PASSWORD = "NewPassword@123";

    private Long userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        userStatusCache.clear();

        RoleJpaEntity adminRole = roleRepository.findByCode("VT-06")
                .orElseGet(() -> roleRepository.save(new RoleJpaEntity(null, "VT-06", "Quản trị viên")));

        // Clean any existing user with the same test username
        userRepository.findByUsername(USERNAME).ifPresent(existing -> {
            auditLogRepository.deleteAll();
            employeeRepository.findByUserId(existing.getId()).ifPresent(employeeRepository::delete);
            userRepository.deleteById(existing.getId());
        });

        // Step A: Create user with passwordChangedAt = null (first time login simulation)
        UserJpaEntity entity = new UserJpaEntity(
                null,
                USERNAME,
                passwordEncoder.encode(INITIAL_PASSWORD),
                adminRole,
                true
        );
        entity.setDataScope(DataScope.COMPANY.name());
        entity.setPasswordChangedAt(null);
        entity.setTokenVersion(1);
        entity = userRepository.saveAndFlush(entity);
        userId = entity.getId();

        employeeRepository.saveAndFlush(new EmployeeJpaEntity(
                null,
                userId,
                null,
                "EMP-CACHE-TEST",
                "First Time Admin Tester",
                false,
                40,
                "ACTIVE"
        ));
    }

    @AfterEach
    void tearDown() {
        userStatusCache.clear();
        if (userId != null) {
            auditLogRepository.deleteAll();
            employeeRepository.findByUserId(userId).ifPresent(employeeRepository::delete);
            userRepository.deleteById(userId);
        }
    }

    @Test
    @DisplayName("P1 E2E Flow: Login -> 403 PASSWORD_CHANGE_REQUIRED (cached) -> Change Password -> Cache Evicted -> New Session Allowed (200 OK)")
    void testCompleteSequence_PasswordChange_WithUserStatusCache_UnblocksAccount() throws Exception {
        // 1. Ensure cache is clean before starting
        assertThat(userStatusCache.get(USERNAME)).isEmpty();

        // 2. Login with initial password -> gets token1
        MvcResult loginResult1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", USERNAME,
                                "password", INITIAL_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(true))
                .andReturn();

        Cookie cookie1 = loginResult1.getResponse().getCookie(JwtAuthenticationFilter.COOKIE_NAME);
        assertThat(cookie1).isNotNull();
        String token1 = cookie1.getValue();
        assertThat(token1).isNotBlank();

        // 3. Access protected API (e.g. GET /api/v1/users) -> must be blocked with 403 PASSWORD_CHANGE_REQUIRED
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PASSWORD_CHANGE_REQUIRED"));

        // Verify the user is now cached with passwordChangedAt == null
        Optional<User> cachedUserBefore = userStatusCache.get(USERNAME);
        assertThat(cachedUserBefore).isPresent();
        assertThat(cachedUserBefore.get().getPasswordChangedAt()).isNull();

        // 4. Access allowed endpoint /api/v1/auth/me with token1 -> must be allowed (200 OK)
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(true));

        // 5. Change password using token1 via POST /api/v1/auth/change-password
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", INITIAL_PASSWORD,
                                "newPassword", NEW_PASSWORD,
                                "confirmPassword", NEW_PASSWORD
                        ))))
                .andExpect(status().isOk());

        // Verify that userStatusCache for this user was actively evicted!
        assertThat(userStatusCache.get(USERNAME)).isEmpty();

        // 6. Old token1 is now invalidated (tokenVersion incremented) -> protected API returns 401
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isUnauthorized());

        // 7. Login with NEW password -> gets token2 with requiresPasswordChange = false
        MvcResult loginResult2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", USERNAME,
                                "password", NEW_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(false))
                .andReturn();

        Cookie cookie2 = loginResult2.getResponse().getCookie(JwtAuthenticationFilter.COOKIE_NAME);
        assertThat(cookie2).isNotNull();
        String token2 = cookie2.getValue();
        assertThat(token2).isNotBlank();

        // 8. Access /auth/me with token2 -> must succeed (200 OK) with requiresPasswordChange = false
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(false));

        // 9. Access protected API with token2 -> must succeed (200 OK), NOT blocked by stale cache!
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());

        // 10. Verify that userStatusCache now caches the updated User with passwordChangedAt != null
        Optional<User> cachedUserAfter = userStatusCache.get(USERNAME);
        assertThat(cachedUserAfter).isPresent();
        assertThat(cachedUserAfter.get().getPasswordChangedAt()).isNotNull();

        // 11. Subsequent request to protected API hits the cache and still succeeds (200 OK)
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("P1 Resilience: Even if cache somehow retains stale User (tokenVersion=1), filter reloads from DB on tokenVersion=2 so /auth/me and protected API succeed on first request")
    void testStaleUserCache_OnTokenVersionMismatch_ReloadsUserAndSucceeds() throws Exception {
        // Change password in DB directly to simulate updated password & tokenVersion = 2
        UserJpaEntity entity = userRepository.findById(userId).orElseThrow();
        entity.setPasswordHash(passwordEncoder.encode(NEW_PASSWORD));
        entity.setTokenVersion(2);
        entity.setPasswordChangedAt(java.time.Instant.now());
        userRepository.saveAndFlush(entity);

        // Intentionally poison the Caffeine cache with a stale User entity (tokenVersion = 1, passwordChangedAt = null)
        User staleUser = new User(
                new com.hrm.employeemanagement.domain.user.UserId(userId),
                USERNAME,
                "stale_hash",
                new com.hrm.employeemanagement.domain.role.Role(
                        new com.hrm.employeemanagement.domain.role.RoleId(1L),
                        com.hrm.employeemanagement.domain.role.RoleCode.VT_06,
                        "Quản trị viên"
                ),
                com.hrm.employeemanagement.domain.user.UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                null,
                null, // passwordChangedAt is null!
                1,    // tokenVersion is 1!
                null
        );
        userStatusCache.put(USERNAME, staleUser);

        // Login with new password -> obtains JWT with tokenVersion = 2
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", USERNAME,
                                "password", NEW_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = loginResult.getResponse().getCookie(JwtAuthenticationFilter.COOKIE_NAME);
        assertThat(cookie).isNotNull();
        String token = cookie.getValue();
        assertThat(token).isNotBlank();

        // The very first request after login to /auth/me MUST succeed (200 OK) even though cache was stale!
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresPasswordChange").value(false));

        // Subsequent protected API request also succeeds (200 OK)
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
