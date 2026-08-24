package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.orgunit;

import com.hrm.employeemanagement.application.dto.orgunit.CreateOrgUnitCommand;
import com.hrm.employeemanagement.application.dto.orgunit.OrgUnitResult;
import com.hrm.employeemanagement.application.port.inbound.orgunit.*;
import com.hrm.employeemanagement.domain.exception.orgunit.CyclicDependencyException;
import com.hrm.employeemanagement.domain.exception.orgunit.DuplicateUnitCodeException;
import com.hrm.employeemanagement.domain.exception.orgunit.InactiveParentException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrgUnitControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateOrgUnitUseCase createOrgUnitUseCase;
    @Mock
    private UpdateOrgUnitUseCase updateOrgUnitUseCase;
    @Mock
    private MoveOrgUnitUseCase moveOrgUnitUseCase;
    @Mock
    private DeactivateOrgUnitUseCase deactivateOrgUnitUseCase;
    @Mock
    private GetOrgTreeUseCase getOrgTreeUseCase;

    @InjectMocks
    private OrgUnitController orgUnitController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orgUnitController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/org-units should return HTTP 201 Created when request is valid")
    void shouldReturn201CreatedWhenCreateOrgUnitIsValid() throws Exception {
        // Business compliant test data: DEV-CENTER is a child of COMPANY_ROOT (id: 1) with managerId: 10
        String requestJson = "{\"unitCode\":\"DEV-CENTER\",\"unitName\":\"Khối Phát Triển\",\"unitType\":\"CENTER\",\"parentId\":1,\"managerId\":10,\"description\":\"Mô tả\"}";

        OrgUnitResult mockResult = new OrgUnitResult(
                2L, "DEV-CENTER", "Khối Phát Triển", OrgUnitType.CENTER,
                1L, "/1/2/", 2, OrgUnitStatus.ACTIVE, "Mô tả", 10L, LocalDateTime.now(), null
        );

        when(createOrgUnitUseCase.execute(any(CreateOrgUnitCommand.class))).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/org-units")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.unitCode").value("DEV-CENTER"))
                .andExpect(jsonPath("$.parentId").value(1))
                .andExpect(jsonPath("$.managerId").value(10))
                .andExpect(jsonPath("$.treePath").value("/1/2/"))
                .andExpect(jsonPath("$.level").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/org-units should return HTTP 400 Bad Request when managerId is missing (TC-03)")
    void shouldReturn400BadRequestWhenManagerIdIsMissing() throws Exception {
        String requestJson = "{\"unitCode\":\"DEV-CENTER\",\"unitName\":\"Khối Phát Triển\",\"unitType\":\"CENTER\",\"parentId\":1,\"description\":\"Mô tả\"}";

        mockMvc.perform(post("/api/v1/org-units")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/org-units should return HTTP 409 Conflict when unit code already exists")
    void shouldReturn409ConflictWhenDuplicateUnitCode() throws Exception {
        String requestJson = "{\"unitCode\":\"DEV-CENTER\",\"unitName\":\"Khối Phát Triển\",\"unitType\":\"CENTER\",\"parentId\":1,\"managerId\":10,\"description\":\"Mô tả\"}";

        when(createOrgUnitUseCase.execute(any(CreateOrgUnitCommand.class)))
                .thenThrow(new DuplicateUnitCodeException("Unit code 'DEV-CENTER' already exists"));

        mockMvc.perform(post("/api/v1/org-units")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_UNIT_CODE"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PATCH /api/v1/org-units/{id}/move should return HTTP 400 Bad Request when cyclic dependency occurs")
    void shouldReturn400BadRequestWhenCyclicDependency() throws Exception {
        String requestJson = "{\"newParentId\":2}";

        when(moveOrgUnitUseCase.execute(any()))
                .thenThrow(new CyclicDependencyException("Cannot move a parent unit inside one of its own descendant nodes"));

        mockMvc.perform(patch("/api/v1/org-units/1/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CYCLIC_DEPENDENCY_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/org-units should return HTTP 400 Bad Request when parent is inactive")
    void shouldReturn400BadRequestWhenParentIsInactive() throws Exception {
        String requestJson = "{\"unitCode\":\"WEB-DEPT\",\"unitName\":\"Phòng Web\",\"unitType\":\"DEPARTMENT\",\"parentId\":2,\"managerId\":10,\"description\":\"Mô tả\"}";

        when(createOrgUnitUseCase.execute(any()))
                .thenThrow(new InactiveParentException("Cannot assign or move unit under an inactive parent unit ID: 2"));

        mockMvc.perform(post("/api/v1/org-units")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INACTIVE_PARENT_UNIT"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("PUT /api/v1/org-units/{id} should return HTTP 404 Not Found when unit does not exist")
    void shouldReturn404NotFoundWhenUnitDoesNotExist() throws Exception {
        when(updateOrgUnitUseCase.execute(any()))
                .thenThrow(new OrgUnitNotFoundException("Organizational unit not found with ID: 9999"));

        mockMvc.perform(put("/api/v1/org-units/9999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"unitName\":\"New Name\",\"unitType\":\"DEPARTMENT\",\"description\":\"\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORG_UNIT_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/v1/org-units/abc should return HTTP 400 Bad Request for PathVariable type mismatch")
    void shouldReturn400BadRequestWhenPathVariableTypeMismatch() throws Exception {
        mockMvc.perform(put("/api/v1/org-units/abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"unitName\":\"Test\",\"unitType\":\"CENTER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}
