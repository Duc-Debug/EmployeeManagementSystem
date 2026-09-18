package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hrm.employeemanagement.application.dto.unavailability.ApproveUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.RejectUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.SubmitUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityConflictCheckResult;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.inbound.unavailability.ApproveUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.CancelUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.CheckUnavailabilityConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.GetDepartmentUnavailabilityDeclarationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.GetMyUnavailabilityDeclarationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.RejectUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.SubmitUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityReasonType;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability.dto.ApproveUnavailabilityRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability.dto.RejectUnavailabilityRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.unavailability.dto.SubmitUnavailabilityRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnavailabilityDeclarationController REST API Tests")
class UnavailabilityDeclarationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SubmitUnavailabilityDeclarationUseCase submitUseCase;

    @Mock
    private ApproveUnavailabilityDeclarationUseCase approveUseCase;

    @Mock
    private RejectUnavailabilityDeclarationUseCase rejectUseCase;

    @Mock
    private CancelUnavailabilityDeclarationUseCase cancelUseCase;

    @Mock
    private GetMyUnavailabilityDeclarationsUseCase getMyDeclarationsUseCase;

    @Mock
    private GetDepartmentUnavailabilityDeclarationsUseCase getDepartmentDeclarationsUseCase;

    @Mock
    private CheckUnavailabilityConflictUseCase checkConflictUseCase;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        UnavailabilityDeclarationController controller = new UnavailabilityDeclarationController(
                submitUseCase,
                approveUseCase,
                rejectUseCase,
                cancelUseCase,
                getMyDeclarationsUseCase,
                getDepartmentDeclarationsUseCase,
                checkConflictUseCase
        );

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new UnavailabilityExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/unavailability-declarations - Tạo khai báo thành công (201 Created)")
    void testSubmitSuccess() throws Exception {
        SubmitUnavailabilityRequest request = new SubmitUnavailabilityRequest(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.TRAINING,
                "Khóa học DevOps"
        );

        UnavailabilityDeclarationResult result = new UnavailabilityDeclarationResult(
                10L,
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.TRAINING,
                "Khóa học DevOps",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.PENDING,
                null,
                null,
                null,
                LocalDateTime.now(),
                null
        );

        when(submitUseCase.submit(any(SubmitUnavailabilityCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/unavailability-declarations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalHoursDeducted").value(16.00));
    }

    @Test
    @DisplayName("POST /api/v1/unavailability-declarations - Không có quyền hoặc không chính chủ (403 Forbidden)")
    void testSubmitForbidden() throws Exception {
        SubmitUnavailabilityRequest request = new SubmitUnavailabilityRequest(
                2L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.TRAINING,
                "Khóa học"
        );

        when(submitUseCase.submit(any(SubmitUnavailabilityCommand.class)))
                .thenThrow(new PermissionDeniedException(PermissionCode.UNAVAILABILITY_DECLARE));

        mockMvc.perform(post("/api/v1/unavailability-declarations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PERMISSION_DENIED"));
    }

    @Test
    @DisplayName("GET /api/v1/unavailability-declarations/me - Lấy danh sách cá nhân thành công")
    void testGetMyDeclarations() throws Exception {
        UnavailabilityDeclarationResult r1 = new UnavailabilityDeclarationResult(
                10L,
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.TRAINING,
                "Đào tạo",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.PENDING,
                null,
                null,
                null,
                LocalDateTime.now(),
                null
        );

        when(getMyDeclarationsUseCase.getMyDeclarations()).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/v1/unavailability-declarations/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/unavailability-declarations/{id}/check-conflict - Kiểm tra xung đột trước khi duyệt")
    void testCheckConflict() throws Exception {
        UnavailabilityConflictCheckResult conflictResult = new UnavailabilityConflictCheckResult(
                true,
                1,
                BigDecimal.valueOf(20.00),
                List.of(new UnavailabilityConflictCheckResult.ConflictingAllocationInfo(
                        5L, 2L, 2026, 39, BigDecimal.valueOf(20.00)
                )),
                "Cảnh báo trùng phân bổ"
        );

        when(checkConflictUseCase.checkConflict(10L)).thenReturn(conflictResult);

        mockMvc.perform(get("/api/v1/unavailability-declarations/10/check-conflict"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasConflict").value(true))
                .andExpect(jsonPath("$.data.conflictingAllocationsCount").value(1))
                .andExpect(jsonPath("$.data.totalConflictingHours").value(20.00));
    }

    @Test
    @DisplayName("POST /api/v1/unavailability-declarations/{id}/approve - Phê duyệt thành công")
    void testApproveSuccess() throws Exception {
        ApproveUnavailabilityRequest request = new ApproveUnavailabilityRequest("Đồng ý duyệt", true);

        UnavailabilityDeclarationResult result = new UnavailabilityDeclarationResult(
                10L,
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.TRAINING,
                "Khóa học DevOps",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.APPROVED,
                99L,
                "Đồng ý duyệt",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(approveUseCase.approve(any(ApproveUnavailabilityCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/unavailability-declarations/10/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approverId").value(99));
    }

    @Test
    @DisplayName("POST /api/v1/unavailability-declarations/{id}/reject - Từ chối thành công")
    void testRejectSuccess() throws Exception {
        RejectUnavailabilityRequest request = new RejectUnavailabilityRequest("Từ chối vì dự án gấp");

        UnavailabilityDeclarationResult result = new UnavailabilityDeclarationResult(
                10L,
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.TRAINING,
                "Khóa học DevOps",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.REJECTED,
                99L,
                "Từ chối vì dự án gấp",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(rejectUseCase.reject(any(RejectUnavailabilityCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/unavailability-declarations/10/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.approverComment").value("Từ chối vì dự án gấp"));
    }

    @Test
    @DisplayName("DELETE /api/v1/unavailability-declarations/{id} - Hủy khai báo thành công (200 OK)")
    void testCancelSuccess() throws Exception {
        UnavailabilityDeclarationResult result = new UnavailabilityDeclarationResult(
                10L,
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.TRAINING,
                "Khóa học",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.CANCELLED,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(cancelUseCase.cancel(10L)).thenReturn(result);

        mockMvc.perform(delete("/api/v1/unavailability-declarations/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(cancelUseCase).cancel(10L);
    }
}
