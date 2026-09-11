package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hrm.employeemanagement.application.dto.task.cascade.CascadeDelayWarningResult;
import com.hrm.employeemanagement.application.dto.task.cascade.EvaluateCascadeDelayCommand;
import com.hrm.employeemanagement.application.port.inbound.task.EvaluateCascadeDelayUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskActualEndDateUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task.dto.EvaluateCascadeDelayRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

class CascadeDelayWarningControllerTest {

    @Mock
    private EvaluateCascadeDelayUseCase evaluateUseCase;
    @Mock
    private UpdateTaskActualEndDateUseCase updateUseCase;

    private CascadeDelayWarningController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new CascadeDelayWarningController(evaluateUseCase, updateUseCase);
    }

    @Test
    @DisplayName("Evaluate Cascade Delay Endpoint returns HTTP 200 OK")
    void testEvaluateCascadeDelayEndpoint() {
        Long projectId = 1L;
        Long taskId = 10L;
        LocalDate newDate = LocalDate.of(2026, 10, 8);
        EvaluateCascadeDelayRequest request = new EvaluateCascadeDelayRequest(newDate);

        CascadeDelayWarningResult mockResult = new CascadeDelayWarningResult(
                taskId,
                "TASK-01",
                "Công việc 1",
                LocalDate.of(2026, 10, 5),
                newDate,
                3L,
                false,
                List.of(),
                List.of(),
                "Phát hiện trễ"
        );

        EvaluateCascadeDelayCommand expectedCommand = new EvaluateCascadeDelayCommand(projectId, taskId, newDate);
        when(evaluateUseCase.evaluateCascadeDelay(expectedCommand)).thenReturn(mockResult);

        ResponseEntity<ApiResponse<CascadeDelayWarningResult>> response = controller.evaluateCascadeDelay(projectId, taskId, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Đánh giá ảnh hưởng trễ dây chuyền thành công", response.getBody().getMessage());
        assertEquals(mockResult, response.getBody().getData());

        verify(evaluateUseCase).evaluateCascadeDelay(expectedCommand);
    }

    @Test
    @DisplayName("Update Actual End Date Endpoint returns HTTP 200 OK")
    void testUpdateActualEndDateEndpoint() {
        Long projectId = 1L;
        Long taskId = 10L;
        LocalDate newDate = LocalDate.of(2026, 10, 8);
        EvaluateCascadeDelayRequest request = new EvaluateCascadeDelayRequest(newDate);

        CascadeDelayWarningResult mockResult = new CascadeDelayWarningResult(
                taskId,
                "TASK-01",
                "Công việc 1",
                LocalDate.of(2026, 10, 5),
                newDate,
                3L,
                false,
                List.of(),
                List.of(),
                "Cập nhật thành công"
        );

        EvaluateCascadeDelayCommand expectedCommand = new EvaluateCascadeDelayCommand(projectId, taskId, newDate);
        when(updateUseCase.updateActualEndDate(expectedCommand)).thenReturn(mockResult);

        ResponseEntity<ApiResponse<CascadeDelayWarningResult>> response = controller.updateActualEndDate(projectId, taskId, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockResult, response.getBody().getData());

        verify(updateUseCase).updateActualEndDate(expectedCommand);
    }
}
