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
    @DisplayName("Token hợp lệ và chưa bị blacklist: thiết lập SecurityContext thành công")
    void testDoFilter_ValidToken_NotBlacklisted() throws Exception {
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistPort.isBlacklisted(token)).thenReturn(false);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn("admin");

        Role role = new Role(new RoleId(1L), RoleCode.VT_06, "Admin");
        User user = new User(new UserId(1L), "admin", "hash", role, UserStatus.ACTIVE, new EmployeeId(1L));
        when(loadUserPort.findByUsername("admin")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin", ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername());
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
}
