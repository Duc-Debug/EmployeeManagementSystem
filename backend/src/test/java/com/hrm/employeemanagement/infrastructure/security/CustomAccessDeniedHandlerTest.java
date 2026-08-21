package com.hrm.employeemanagement.infrastructure.security;

import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomAccessDeniedHandlerTest {

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @BeforeEach
    void setUp() {
        customAccessDeniedHandler = new CustomAccessDeniedHandler(saveAuditLogPort);
    }

    @Test
    @DisplayName("Ghi nhật ký ACCESS_DENIED vào audit_logs và trả về 403 khi người dùng không đủ quyền (NCL-01-CN-002-TC-04)")
    void testHandle_AccessDenied_LogsAuditEventAndReturns403() throws Exception {
        Role staffRole = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn");
        User staffUser = new User(new UserId(15L), "staff_member", "hash", staffRole, UserStatus.ACTIVE, null);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(staffUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        customAccessDeniedHandler.handle(request, response, exception);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Bạn không có quyền truy cập chức năng này"));

        // Verify audit log creation
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort, times(1)).save(captor.capture());

        AuditLog loggedAudit = captor.getValue();
        assertEquals(15L, loggedAudit.getUserId());
        assertEquals("ACCESS_DENIED", loggedAudit.getAction());
        assertEquals("users", loggedAudit.getTableName());
    }
}
