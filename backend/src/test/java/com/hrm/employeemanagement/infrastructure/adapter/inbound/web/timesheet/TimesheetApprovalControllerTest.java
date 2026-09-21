package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogResult;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.AdjustApprovedWorkLogUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.ApproveTimesheetUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetPendingApprovalsUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryVersionConflictException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotApprovedException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogAdjustmentReasonRequiredException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimesheetApprovalController Tests - Adjust Approved Work Log")
class TimesheetApprovalControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ApproveTimesheetUseCase approveTimesheetUseCase;

    @Mock
    private GetPendingApprovalsUseCase getPendingApprovalsUseCase;

    @Mock
    private AdjustApprovedWorkLogUseCase adjustApprovedWorkLogUseCase;

    @InjectMocks
    private TimesheetApprovalController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private WorkLogResult createSampleWorkLogResult(Long id, BigDecimal hours) {
        return new WorkLogResult(
                id,
                10L,
                20L,
                "Lê Văn Dev",
                1L,
                "PROJ-01",
                "Dự án Quản trị",
                5L,
                "TASK-01",
                "Nhiệm vụ backend",
                LocalDate.of(2026, 9, 14),
                hours,
                true,
                "Mô tả công việc thực tế",
                "APPROVED",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L
        );
    }

    @Test
    @DisplayName("TC-01: Điều chỉnh dòng giờ đã duyệt thành công trả về HTTP 200 OK")
    void testAdjustApprovedWorkLog_Success() throws Exception {
        WorkLogResult entryResult = createSampleWorkLogResult(1L, new BigDecimal("6.00"));
        AdjustApprovedWorkLogResult result = new AdjustApprovedWorkLogResult(entryResult, List.of());

        when(adjustApprovedWorkLogUseCase.adjustApprovedWorkLog(any(AdjustApprovedWorkLogCommand.class)))
                .thenReturn(result);

        String jsonBody = """
                {
                    "hours": 6.00,
                    "taskId": 5,
                    "isBillable": true,
                    "description": "Cập nhật lại giờ làm việc",
                    "reason": "Điều chỉnh do ghi nhận thiếu giờ họp",
                    "version": 0
                }
                """;

        mockMvc.perform(put("/api/v1/work-logs/approvals/entries/1/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entry.id").value(1))
                .andExpect(jsonPath("$.entry.hours").value(6.00))
                .andExpect(jsonPath("$.entry.status").value("APPROVED"));
    }

    @Test
    @DisplayName("TC-02: Thất bại khi lý do giải trình để trống hoặc dưới 10 ký tự (HTTP 400)")
    void testAdjustApprovedWorkLog_ThrowsBadRequest_WhenReasonTooShort() throws Exception {
        String jsonBody = """
                {
                    "hours": 6.00,
                    "reason": "Ngắn",
                    "version": 0
                }
                """;

        mockMvc.perform(put("/api/v1/work-logs/approvals/entries/1/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-03: Thất bại khi không có quyền điều chỉnh (HTTP 403)")
    void testAdjustApprovedWorkLog_ThrowsForbidden_WhenPermissionDenied() throws Exception {
        when(adjustApprovedWorkLogUseCase.adjustApprovedWorkLog(any(AdjustApprovedWorkLogCommand.class)))
                .thenThrow(new PermissionDeniedException(PermissionCode.WORK_LOG_ADJUST));

        String jsonBody = """
                {
                    "hours": 6.00,
                    "reason": "Điều chỉnh do ghi nhận thiếu giờ họp",
                    "version": 0
                }
                """;

        mockMvc.perform(put("/api/v1/work-logs/approvals/entries/1/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("TC-04: Thất bại khi dòng ghi giờ không ở trạng thái APPROVED (HTTP 400)")
    void testAdjustApprovedWorkLog_ThrowsBadRequest_WhenNotApproved() throws Exception {
        when(adjustApprovedWorkLogUseCase.adjustApprovedWorkLog(any(AdjustApprovedWorkLogCommand.class)))
                .thenThrow(new TimesheetNotApprovedException("Chỉ có thể điều chỉnh dòng giờ công đã được duyệt"));

        String jsonBody = """
                {
                    "hours": 6.00,
                    "reason": "Điều chỉnh do ghi nhận thiếu giờ họp",
                    "version": 0
                }
                """;

        mockMvc.perform(put("/api/v1/work-logs/approvals/entries/1/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TIMESHEET_NOT_APPROVED"));
    }

    @Test
    @DisplayName("TC-05: Thất bại khi xung đột phiên bản (HTTP 409 CONFLICT)")
    void testAdjustApprovedWorkLog_ThrowsConflict_WhenVersionConflict() throws Exception {
        when(adjustApprovedWorkLogUseCase.adjustApprovedWorkLog(any(AdjustApprovedWorkLogCommand.class)))
                .thenThrow(new TimesheetEntryVersionConflictException("Dòng giờ công đã bị thay đổi bởi người khác"));

        String jsonBody = """
                {
                    "hours": 6.00,
                    "reason": "Điều chỉnh do ghi nhận thiếu giờ họp",
                    "version": 0
                }
                """;

        mockMvc.perform(put("/api/v1/work-logs/approvals/entries/1/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TIMESHEET_ENTRY_VERSION_CONFLICT"));
    }

    @Test
    @DisplayName("TC-06: Thất bại khi thiếu trường version trong request (HTTP 400 BAD REQUEST)")
    void testAdjustApprovedWorkLog_ThrowsBadRequest_WhenVersionMissing() throws Exception {
        String jsonBody = """
                {
                    "hours": 6.00,
                    "reason": "Điều chỉnh do ghi nhận thiếu giờ họp"
                }
                """;

        mockMvc.perform(put("/api/v1/work-logs/approvals/entries/1/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }
}
