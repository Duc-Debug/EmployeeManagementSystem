package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.RoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.UserJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataUserRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.security.CaffeineTokenBlacklistAdapter;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.security.JwtTokenProviderAdapter;
import com.hrm.employeemanagement.infrastructure.security.JwtProperties;
import com.hrm.employeemanagement.infrastructure.security.UserStatusCache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Security Tests: JWT Validation, Backend Authorization Source of Truth, and Invalidation")
class JwtAndLocalStorageSecurityTest {

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
    private JwtTokenProviderAdapter jwtTokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private CaffeineTokenBlacklistAdapter tokenBlacklistAdapter;

    @Autowired
    private UserStatusCache userStatusCache;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_SPECIALIST_USER = "sec_specialist_vt04";
    private static final String TEST_PASSWORD = "Password@123";
    private UserJpaEntity specialistUserEntity;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        tokenBlacklistAdapter.clear();
        userStatusCache.clear();

        RoleJpaEntity vt04Role = roleRepository.findByCode("VT-04")
                .orElseGet(() -> roleRepository.save(new RoleJpaEntity(null, "VT-04", "Nhân viên chuyên môn")));

        roleRepository.findByCode("VT-06")
                .orElseGet(() -> roleRepository.save(new RoleJpaEntity(null, "VT-06", "Quản trị viên")));

        specialistUserEntity = userRepository.findByUsername(TEST_SPECIALIST_USER).orElseGet(() -> {
            UserJpaEntity u = new UserJpaEntity(
                    null,
                    TEST_SPECIALIST_USER,
                    passwordEncoder.encode(TEST_PASSWORD),
                    vt04Role,
                    true
            );
            u.setDataScope(DataScope.SELF.name());
            u.setPasswordChangedAt(Instant.now().minusSeconds(86400));
            u.setTokenVersion(1);
            u = userRepository.save(u);

            employeeRepository.save(new EmployeeJpaEntity(
                    null,
                    u.getId(),
                    null,
                    "EMP-SEC-01",
                    "Security Specialist",
                    false,
                    40,
                    "ACTIVE"
            ));
            return u;
        });
    }

    @AfterEach
    void tearDown() {
        userStatusCache.clear();
        tokenBlacklistAdapter.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Test 1 & 6: Tampering client permissions/roles in localStorage cannot bypass backend authorization")
    void testTamperedClientPermissionsCannotBypassBackendAuthorization() throws Exception {
        // Authenticate as regular specialist (VT-04)
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", TEST_SPECIALIST_USER,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andReturn();

        String token = loginResult.getResponse().getCookie(com.hrm.employeemanagement.infrastructure.security.JwtAuthenticationFilter.COOKIE_NAME).getValue();

        // 1. Specialist can access their own profile
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(TEST_SPECIALIST_USER))
                .andExpect(jsonPath("$.data.roleCode").value("VT-04"));

        // 2. Privileged admin endpoint: /api/v1/org-units/1 requires VT-06
        // Even if the client sends spoofed role/permission headers, the backend loads authorities from DB
        mockMvc.perform(get("/api/v1/org-units/1")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Role", "VT-06")
                        .header("X-Permissions", "DATA_BACKUP_MANAGE,ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test 4: Expired JWT token is strictly rejected with 401 Unauthorized")
    void testExpiredTokenIsRejectedWith401() throws Exception {
        // Generate an expired token using past dates
        Date pastIssuedAt = new Date(System.currentTimeMillis() - 7200_000); // 2 hours ago
        Date pastExpiry = new Date(System.currentTimeMillis() - 3600_000);   // 1 hour ago

        var key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(TEST_SPECIALIST_USER)
                .claim("userId", specialistUserEntity.getId())
                .claim("roleCode", "VT-04")
                .claim("tv", specialistUserEntity.getTokenVersion())
                .issuedAt(pastIssuedAt)
                .expiration(pastExpiry)
                .signWith(key)
                .compact();

        // Request with expired token must be rejected with 401
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test 3: Logout blacklists token immediately, preventing reuse")
    void testLogoutBlacklistsTokenImmediately() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", TEST_SPECIALIST_USER,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andReturn();

        String token = loginResult.getResponse().getCookie(com.hrm.employeemanagement.infrastructure.security.JwtAuthenticationFilter.COOKIE_NAME).getValue();

        // Verify token works initially
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Perform logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Subsequent call with the logged-out token must fail (401 Unauthorized)
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test 5: Password change increments tokenVersion and invalidates old token")
    void testPasswordChangeInvalidatesOldToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", TEST_SPECIALIST_USER,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andReturn();

        String oldToken = loginResult.getResponse().getCookie(com.hrm.employeemanagement.infrastructure.security.JwtAuthenticationFilter.COOKIE_NAME).getValue();

        String newPassword = "NewSecPassword@456";

        // Change password using oldToken
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", TEST_PASSWORD,
                                "newPassword", newPassword,
                                "confirmPassword", newPassword
                        ))))
                .andExpect(status().isOk());

        // Now, old token MUST be rejected because tokenVersion changed on the user
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());

        // Reset password back for other tests
        specialistUserEntity = userRepository.findByUsername(TEST_SPECIALIST_USER).orElseThrow();
        specialistUserEntity.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        specialistUserEntity.setPasswordChangedAt(Instant.now());
        userRepository.save(specialistUserEntity);
    }

    @Test
    @DisplayName("Test: JWT payload is decoupled from permissions (does not contain permissions claim)")
    void testJwtDoesNotContainPermissionsClaim() {
        User user = new User(
                new com.hrm.employeemanagement.domain.user.UserId(specialistUserEntity.getId()),
                specialistUserEntity.getUsername(),
                specialistUserEntity.getPasswordHash(),
                new com.hrm.employeemanagement.domain.role.Role(
                        new com.hrm.employeemanagement.domain.role.RoleId(1L),
                        RoleCode.VT_04,
                        "Chuyên viên"
                ),
                UserStatus.ACTIVE,
                null
        );

        String token = jwtTokenProvider.generateToken(user);
        assertThat(token).isNotBlank();

        var key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(TEST_SPECIALIST_USER);
        assertThat(claims.get("userId")).isNotNull();
        assertThat(claims.get("roleCode")).isEqualTo("VT-04");
        assertThat(claims.get("tv")).isNotNull();
        // Crucial security architecture check: permissions claim must NOT be present in JWT
        assertThat(claims.get("permissions")).isNull();
    }

    @Test
    @DisplayName("Test: Login sets HttpOnly and SameSite cookie, and protected endpoints authenticate via cookie without Authorization header")
    void testLoginSetsHttpOnlySameSiteCookieAndProtectedEndpointAuthenticatesViaCookie() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", TEST_SPECIALIST_USER,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String setCookieHeader = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("nexushrm_jwt=");
        assertThat(setCookieHeader).containsIgnoringCase("HttpOnly");
        assertThat(setCookieHeader).containsIgnoringCase("SameSite=None");
        assertThat(setCookieHeader).containsIgnoringCase("Secure");
        assertThat(setCookieHeader).containsIgnoringCase("Path=/");

        Cookie jwtCookie = loginResult.getResponse().getCookie("nexushrm_jwt");
        assertThat(jwtCookie).isNotNull();
        assertThat(jwtCookie.getValue()).isNotBlank();

        // Access protected endpoint /api/v1/auth/me using ONLY the HttpOnly cookie (NO Authorization header)
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(TEST_SPECIALIST_USER))
                .andExpect(jsonPath("$.data.roleCode").value("VT-04"));
    }

    @Test
    @DisplayName("Test: Logout with HttpOnly cookie clears cookie and blacklists token")
    void testLogoutWithCookieClearsCookieAndBlacklistsToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", TEST_SPECIALIST_USER,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse().getCookie("nexushrm_jwt");
        assertThat(jwtCookie).isNotNull();

        // Perform logout with ONLY the cookie
        MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andReturn();

        String clearCookieHeader = logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(clearCookieHeader).isNotNull();
        assertThat(clearCookieHeader).contains("nexushrm_jwt=");
        assertThat(clearCookieHeader).contains("Max-Age=0");

        // Subsequent call with the logged-out cookie must fail (401 Unauthorized) because token was blacklisted
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(jwtCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CSRF Test: Untrusted Origin is blocked with 403 Forbidden on state-changing requests")
    void testCsrfProtectionBlocksUntrustedOrigin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", TEST_SPECIALIST_USER,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse().getCookie("nexushrm_jwt");
        assertThat(jwtCookie).isNotNull();

        // Cross-site attack attempt from evil.com with victim's cookie
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(jwtCookie)
                        .header("Origin", "https://evil.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF Test: Wildcard Vercel subdomains (e.g. malicious.vercel.app) are strictly blocked")
    void testCsrfProtectionBlocksWildcardVercelSubdomains() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", TEST_SPECIALIST_USER,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse().getCookie("nexushrm_jwt");
        assertThat(jwtCookie).isNotNull();

        // Attack from another Vercel deployment must be rejected
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(jwtCookie)
                        .header("Origin", "https://malicious.vercel.app"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF Test: Trusted production Vercel frontend origin is accepted")
    void testCsrfProtectionAllowsTrustedVercelFrontendOrigin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", TEST_SPECIALIST_USER,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse().getCookie("nexushrm_jwt");
        assertThat(jwtCookie).isNotNull();

        // Legitimate production frontend origin must succeed
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(jwtCookie)
                        .header("Origin", "https://employee-management-system-izcr9mk17-duc-debug.vercel.app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("CSRF Test: Sec-Fetch-Site cross-site from untrusted origin is strictly blocked")
    void testCsrfProtectionBlocksSecFetchCrossSiteFromUntrustedOrigin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", TEST_SPECIALIST_USER,
                                "password", TEST_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse().getCookie("nexushrm_jwt");
        assertThat(jwtCookie).isNotNull();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(jwtCookie)
                        .header("Sec-Fetch-Site", "cross-site")
                        .header("Origin", "https://attacker.org"))
                .andExpect(status().isForbidden());
    }
}
