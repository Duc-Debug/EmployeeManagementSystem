package com.hrm.employeemanagement.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomAccessDeniedHandlerTest {

    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @BeforeEach
    void setUp() {
        customAccessDeniedHandler = new CustomAccessDeniedHandler();
    }

    @Test
    @DisplayName("Trả về HTTP 403 Forbidden khi người dùng không đủ quyền truy cập")
    void testHandle_AccessDenied_Returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        customAccessDeniedHandler.handle(request, response, exception);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Bạn không có quyền truy cập chức năng này"));
    }
}
