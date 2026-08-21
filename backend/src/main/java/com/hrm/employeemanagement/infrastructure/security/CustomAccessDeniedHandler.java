package com.hrm.employeemanagement.infrastructure.security;

import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.user.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom AccessDeniedHandler for handling RBAC authorization failures (HTTP 403 Forbidden).
 * Automatically logs unauthorized access attempts to the audit_logs table (NCL-01-CN-002-TC-04).
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final SaveAuditLogPort saveAuditLogPort;

    public CustomAccessDeniedHandler(SaveAuditLogPort saveAuditLogPort) {
        this.saveAuditLogPort = saveAuditLogPort;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Long currentUserId = resolveCurrentUserId();

        // Log unauthorized access rejection into audit_logs table
        if (saveAuditLogPort != null) {
            try {
                saveAuditLogPort.save(AuditLog.create(currentUserId, "ACCESS_DENIED", "users", null));
            } catch (Exception ignored) {
                // Prevent logging failures from concealing security rejections
            }
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này\",\"data\":null}";
        response.getWriter().write(jsonResponse);
    }

    private Long resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            if (auth.getPrincipal() instanceof User user) {
                return user.getIdValue();
            } else if (auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
                return userPrincipal.getId();
            }
        }
        return null;
    }
}
