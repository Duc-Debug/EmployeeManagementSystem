package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.user.RoleResult;
import com.hrm.employeemanagement.application.port.inbound.user.GetRoleListUseCase;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleController Web Inbound Adapter Tests")
class RoleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetRoleListUseCase getRoleListUseCase;

    @InjectMocks
    private RoleController roleController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController).build();
    }

    @Test
    @DisplayName("GET /api/v1/roles trả về danh sách vai trò")
    void shouldReturnRoleList() throws Exception {
        RoleResult role1 = new RoleResult(1L, "VT-01", "Ban giám đốc", null);
        RoleResult role2 = new RoleResult(2L, "VT-02", "Quản lý dự án", null);

        when(getRoleListUseCase.getRoles()).thenReturn(List.of(role1, role2));

        mockMvc.perform(get("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("VT-01"))
                .andExpect(jsonPath("$.data[0].name").value("Ban giám đốc"))
                .andExpect(jsonPath("$.data[1].code").value("VT-02"))
                .andExpect(jsonPath("$.data[1].name").value("Quản lý dự án"));
    }
}
