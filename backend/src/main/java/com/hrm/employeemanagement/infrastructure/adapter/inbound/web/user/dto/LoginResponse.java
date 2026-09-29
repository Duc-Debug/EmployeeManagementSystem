package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto;

/**
 * DTO trả về cho client sau khi đăng nhập thành công.
 * TUYỆT ĐỐI KHÔNG chứa token để ngăn chặn triệt để nguy cơ đánh cắp JWT qua XSS trong JavaScript runtime.
 * Access Token được gửi duy nhất qua HttpOnly SameSite Cookie.
 */
public record LoginResponse(
        Long userId,
        String username,
        String roleCode,
        boolean requiresPasswordChange
) {
}
