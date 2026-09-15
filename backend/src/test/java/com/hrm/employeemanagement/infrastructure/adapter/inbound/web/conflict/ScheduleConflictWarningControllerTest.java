package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.conflict;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hrm.employeemanagement.application.dto.conflict.AssignScheduleConflictHandlerCommand;
import com.hrm.employeemanagement.application.dto.conflict.ResolveScheduleConflictWithNoteCommand;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;
import com.hrm.employeemanagement.application.port.inbound.conflict.AssignScheduleConflictHandlerUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.GetScheduleConflictsUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.NotifyScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ResolveScheduleConflictWithNoteUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ScanScheduleConflictsUseCase;
import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.conflict.dto.AssignScheduleConflictHandlerRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.conflict.dto.ResolveScheduleConflictWithNoteRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

class ScheduleConflictWarningControllerTest {

    @Mock
    private GetScheduleConflictsUseCase getUseCase;
    @Mock
    private ScanScheduleConflictsUseCase scanUseCase;
    @Mock
    private NotifyScheduleConflictUseCase notifyUseCase;
    @Mock
    private ResolveScheduleConflictWithNoteUseCase resolveWithNoteUseCase;
    @Mock
    private AssignScheduleConflictHandlerUseCase assignHandlerUseCase;

    private ScheduleConflictWarningController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ScheduleConflictWarningController(
                getUseCase, scanUseCase, notifyUseCase, resolveWithNoteUseCase, assignHandlerUseCase
        );
    }

    private ScheduleConflictResult createMockResult(ScheduleConflictStatus status) {
        return new ScheduleConflictResult(
                1001L, 10L, "NV010", "Nguyễn Văn A", "Phòng Lập Trình",
                2026, 37, "Tuần 37/2026", ConflictType.MULTI_PROJECT_ALLOCATION, "Phân bổ nhiều dự án",
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                status, status.name(), "Chi tiết xung đột",
                null, null, null,
                20L, "NV020", "Trần Quản Lý",
                "Đã điều chuyển bớt 10h sang nhân sự khác",
                false, null, LocalDateTime.now(), 1L, "Admin User",
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("GET /api/v1/schedule-conflicts returns HTTP 200 with conflict list")
    void testGetScheduleConflicts() {
        ScheduleConflictResult mockResult = createMockResult(ScheduleConflictStatus.OPEN);

        when(getUseCase.getScheduleConflicts(any())).thenReturn(List.of(mockResult));

        ResponseEntity<ApiResponse<List<ScheduleConflictResult>>> response = controller.getScheduleConflicts(
                2026, 37, 37, null, null, null, null
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Nguyễn Văn A", response.getBody().getData().get(0).employeeName());

        verify(getUseCase).getScheduleConflicts(any());
    }

    @Test
    @DisplayName("POST /api/v1/schedule-conflicts/scan returns HTTP 200 with scan results")
    void testScanScheduleConflicts() {
        when(scanUseCase.scanScheduleConflicts(2026, 37, 37)).thenReturn(List.of());

        ResponseEntity<ApiResponse<List<ScheduleConflictResult>>> response = controller.scanScheduleConflicts(2026, 37, 37);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(scanUseCase).scanScheduleConflicts(2026, 37, 37);
    }

    @Test
    @DisplayName("POST /api/v1/schedule-conflicts/{id}/notify returns HTTP 200 with notified result")
    void testNotifyScheduleConflict() {
        ScheduleConflictResult mockResult = createMockResult(ScheduleConflictStatus.NOTIFIED);

        when(notifyUseCase.notifyScheduleConflict(1001L)).thenReturn(mockResult);

        ResponseEntity<ApiResponse<ScheduleConflictResult>> response = controller.notifyScheduleConflict(1001L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ScheduleConflictStatus.NOTIFIED, response.getBody().getData().status());

        verify(notifyUseCase).notifyScheduleConflict(1001L);
    }

    @Test
    @DisplayName("POST /api/v1/schedule-conflicts/{id}/resolve-with-note returns HTTP 200 with note")
    void testResolveScheduleConflictWithNote() {
        ScheduleConflictResult mockResult = createMockResult(ScheduleConflictStatus.RESOLVED);
        ResolveScheduleConflictWithNoteRequest request = new ResolveScheduleConflictWithNoteRequest(20L, "Đã xử lý xong");

        when(resolveWithNoteUseCase.resolveScheduleConflictWithNote(any())).thenReturn(mockResult);

        ResponseEntity<ApiResponse<ScheduleConflictResult>> response = controller.resolveScheduleConflictWithNote(1001L, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ScheduleConflictStatus.RESOLVED, response.getBody().getData().status());
        verify(resolveWithNoteUseCase).resolveScheduleConflictWithNote(any());
    }

    @Test
    @DisplayName("POST /api/v1/schedule-conflicts/{id}/assign-handler returns HTTP 200")
    void testAssignScheduleConflictHandler() {
        ScheduleConflictResult mockResult = createMockResult(ScheduleConflictStatus.OPEN);
        AssignScheduleConflictHandlerRequest request = new AssignScheduleConflictHandlerRequest(20L);

        when(assignHandlerUseCase.assignScheduleConflictHandler(any())).thenReturn(mockResult);

        ResponseEntity<ApiResponse<ScheduleConflictResult>> response = controller.assignScheduleConflictHandler(1001L, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(assignHandlerUseCase).assignScheduleConflictHandler(any());
    }
}
