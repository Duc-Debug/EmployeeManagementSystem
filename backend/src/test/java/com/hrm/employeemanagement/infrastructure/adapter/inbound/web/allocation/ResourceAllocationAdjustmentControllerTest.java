package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.dto.allocation.AdjustAllocationCommand;
import com.hrm.employeemanagement.application.dto.allocation.AllocationChangeLogResult;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.AdjustResourceAllocationUseCase;
import com.hrm.employeemanagement.domain.allocation.AdjustmentAction;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationNotFoundException;
import com.hrm.employeemanagement.domain.exception.allocation.CannotRemoveAllocationWithActualHoursException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto.AdjustAllocationRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.dto.VarianceNoteRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceAllocationAdjustmentController Web API Tests")
class ResourceAllocationAdjustmentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AdjustResourceAllocationUseCase adjustResourceAllocationUseCase;

    private WeeklyCapacityResult sampleCapacityResult;

    @BeforeEach
    void setUp() {
        ResourceAllocationAdjustmentController controller = new ResourceAllocationAdjustmentController(adjustResourceAllocationUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResourceAllocationAdjustmentExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        sampleCapacityResult = new WeeklyCapacityResult(
                100L, "EMP001", "John Doe", 2026, 35, 40,
                BigDecimal.valueOf(40), BigDecimal.valueOf(10), BigDecimal.valueOf(30),
                false, null
        );
    }

    @Test
    @DisplayName("PATCH /api/v1/allocations/{id} - TC-01: Edit hours successfully returns 200")
    void shouldAdjustHoursSuccessfully() throws Exception {
        AdjustAllocationRequest request = new AdjustAllocationRequest(
                AdjustmentAction.EDIT_HOURS,
                BigDecimal.valueOf(10),
                null,
                null, null, null, null
        );

        when(adjustResourceAllocationUseCase.adjustAllocation(eq(1L), any(AdjustAllocationCommand.class)))
                .thenReturn(sampleCapacityResult);

        mockMvc.perform(patch("/api/v1/allocations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeId").value(100))
                .andExpect(jsonPath("$.data.totalAllocatedHours").value(10));
    }

    @Test
    @DisplayName("DELETE /api/v1/allocations/{id} - Success returns 200")
    void shouldDeleteAllocationSuccessfully() throws Exception {
        doNothing().when(adjustResourceAllocationUseCase).removeAllocation(1L);

        mockMvc.perform(delete("/api/v1/allocations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Gỡ dòng phân bổ nguồn lực thành công"));
    }

    @Test
    @DisplayName("DELETE /api/v1/allocations/{id} - TC-02: Returns 409 Conflict when week ended and actual hours exist")
    void shouldReturn409ConflictWhenCannotRemove() throws Exception {
        doThrow(new CannotRemoveAllocationWithActualHoursException(1L, 2026, 1))
                .when(adjustResourceAllocationUseCase).removeAllocation(1L);

        mockMvc.perform(delete("/api/v1/allocations/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/allocations/{id}/variance-note - TC-02 alternative: Saves variance note returns 200")
    void shouldSaveVarianceNoteSuccessfully() throws Exception {
        VarianceNoteRequest request = new VarianceNoteRequest("Nghỉ việc đột xuất");

        when(adjustResourceAllocationUseCase.noteVariance(1L, "Nghỉ việc đột xuất"))
                .thenReturn(sampleCapacityResult);

        mockMvc.perform(post("/api/v1/allocations/1/variance-note")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeId").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/allocations/{id}/history - TC-04: Returns 200 and list of change logs")
    void shouldReturnHistorySuccessfully() throws Exception {
        AllocationChangeLogResult log = new AllocationChangeLogResult(
                10L, 1L, AdjustmentAction.EDIT_HOURS,
                "{\"allocatedHours\":20}", "{\"allocatedHours\":10}",
                1L, "admin", LocalDateTime.now(), "300"
        );
        when(adjustResourceAllocationUseCase.getHistory(1L)).thenReturn(List.of(log));

        mockMvc.perform(get("/api/v1/allocations/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].action").value("EDIT_HOURS"))
                .andExpect(jsonPath("$.data[0].changedBy").value(1))
                .andExpect(jsonPath("$.data[0].changedByName").value("admin"));
    }

    @Test
    @DisplayName("PATCH /api/v1/allocations/{id} - TC-03: Returns 403 Forbidden on permission denied")
    void shouldReturn403WhenPermissionDenied() throws Exception {
        AdjustAllocationRequest request = new AdjustAllocationRequest(
                AdjustmentAction.EDIT_HOURS, BigDecimal.valueOf(10), null, null, null, null, null
        );

        when(adjustResourceAllocationUseCase.adjustAllocation(any(), any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE));

        mockMvc.perform(patch("/api/v1/allocations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/allocations/{id}/history - Returns 404 Not Found when allocation does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        when(adjustResourceAllocationUseCase.getHistory(999L))
                .thenThrow(new AllocationNotFoundException(999L));

        mockMvc.perform(get("/api/v1/allocations/999/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
