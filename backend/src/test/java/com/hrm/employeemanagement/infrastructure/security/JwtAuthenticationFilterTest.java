package com.hrm.employeemanagement.infrastructure.security;

import com.hrm.employeemanagement.application.port.outbound.security.TokenBlacklistPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.security.JwtTokenProviderAdapter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProviderAdapter tokenProvider;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private TokenBlacklistPort tokenBlacklistPort;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private UserStatusCache userStatusCache;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userStatusCache = new UserStatusCache();
        filter = new JwtAuthenticationFilter(tokenProvider, loadUserPort, userStatusCache, tokenBlacklistPort);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Token hợp lệ và chưa bị blacklist: thiết lập SecurityContext thành công khi đã đổi mật khẩu")
    void testDoFilter_ValidToken_NotBlacklisted() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistPort.isBlacklisted(token)).thenReturn(false);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("admin");
        when(tokenProvider.getTokenVersionFromToken(token)).thenReturn(1);

        Role role = new Role(new RoleId(1L), RoleCode.VT_06, "Admin");
        User user = new User(new UserId(1L), "admin", "hash", role, UserStatus.ACTIVE, new EmployeeId(1L), "admin@example.com", java.time.Instant.now(), 1L);
        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin", ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Token hợp lệ gửi qua Cookie: thiết lập SecurityContext thành công")
    void testDoFilter_ValidToken_FromCookie() throws Exception {
        String token = "valid.jwt.cookie.token";
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("accessToken", token);
        when(request.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[]{cookie});
        when(tokenBlacklistPort.isBlacklisted(token)).thenReturn(false);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("admin");
        when(tokenProvider.getTokenVersionFromToken(token)).thenReturn(1);

        Role role = new Role(new RoleId(1L), RoleCode.VT_06, "Admin");
        User user = new User(new UserId(1L), "admin", "hash", role, UserStatus.ACTIVE, new EmployeeId(1L), "admin@example.com", java.time.Instant.now(), 1L);
        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin", ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Token hợp lệ nhưng user chưa đổi mật khẩu (passwordChangedAt == null): chặn API thường với 403 PASSWORD_CHANGE_REQUIRED")
    void testDoFilter_PasswordChangeRequired_BlocksNormalApi() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistPort.isBlacklisted(token)).thenReturn(false);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("admin");
        when(tokenProvider.getTokenVersionFromToken(token)).thenReturn(1);
        when(request.getRequestURI()).thenReturn("/api/v1/users");

        java.io.StringWriter stringWriter = new java.io.StringWriter();
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(stringWriter));

        Role role = new Role(new RoleId(1L), RoleCode.VT_06, "Admin");
        User user = new User(new UserId(1L), "admin", "hash", role, UserStatus.ACTIVE, new EmployeeId(1L)); // passwordChangedAt is null
        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(stringWriter.toString().contains("PASSWORD_CHANGE_REQUIRED"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Token hợp lệ và user chưa đổi mật khẩu: cho phép truy cập /api/v1/auth/me")
    void testDoFilter_PasswordChangeRequired_AllowsAuthMe() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistPort.isBlacklisted(token)).thenReturn(false);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("admin");
        when(tokenProvider.getTokenVersionFromToken(token)).thenReturn(1);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/me");

        Role role = new Role(new RoleId(1L), RoleCode.VT_06, "Admin");
        User user = new User(new UserId(1L), "admin", "hash", role, UserStatus.ACTIVE, new EmployeeId(1L));
        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Token hợp lệ và user chưa đổi mật khẩu: cho phép truy cập /api/v1/auth/change-password")
    void testDoFilter_PasswordChangeRequired_AllowsChangePassword() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistPort.isBlacklisted(token)).thenReturn(false);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("admin");
        when(tokenProvider.getTokenVersionFromToken(token)).thenReturn(1);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/change-password");

        Role role = new Role(new RoleId(1L), RoleCode.VT_06, "Admin");
        User user = new User(new UserId(1L), "admin", "hash", role, UserStatus.ACTIVE, new EmployeeId(1L));
        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Token hợp lệ và user chưa đổi mật khẩu: cho phép truy cập /api/v1/auth/logout")
    void testDoFilter_PasswordChangeRequired_AllowsLogout() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistPort.isBlacklisted(token)).thenReturn(false);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("admin");
        when(tokenProvider.getTokenVersionFromToken(token)).thenReturn(1);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/logout");

        Role role = new Role(new RoleId(1L), RoleCode.VT_06, "Admin");
        User user = new User(new UserId(1L), "admin", "hash", role, UserStatus.ACTIVE, new EmployeeId(1L));
        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Token nằm trong blacklist: không thiết lập SecurityContext")
    void testDoFilter_BlacklistedToken_Ignored() throws Exception {
        String token = "blacklisted.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistPort.isBlacklisted(token)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider, never()).validateToken(token);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Phiên người dùng đã bị thu hồi (isUserRevoked = true): không thiết lập SecurityContext")
    void testDoFilter_UserRevoked_Ignored() throws Exception {
        String token = "user.revoked.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistPort.isBlacklisted(token)).thenReturn(false);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("admin");
        when(tokenProvider.getIssuedAtTimestampFromToken(token)).thenReturn(1000L);
        when(tokenBlacklistPort.isUserRevoked("admin", 1000L)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(loadUserPort, never()).findByUsername("admin");
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
