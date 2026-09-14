package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.idleness;

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
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffResult;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdleStaffItemResult;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessReportResult;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessWeeklyDetailResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.idleness.AcknowledgeProlongedIdleStaffUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.idleness.GetProlongedIdleStaffUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.idleness.dto.AcknowledgeProlongedIdleStaffRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProlongedIdlenessController Web Tests (NCL-07-CN-006 / QTN-23)")
class ProlongedIdlenessControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private GetProlongedIdleStaffUseCase getProlongedIdleStaffUseCase;

    @Mock
    private AcknowledgeProlongedIdleStaffUseCase acknowledgeProlongedIdleStaffUseCase;

    @BeforeEach
    void setUp() {
        ProlongedIdlenessController controller = new ProlongedIdlenessController(
                getProlongedIdleStaffUseCase,
                acknowledgeProlongedIdleStaffUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/v1/capacity/prolonged-idleness — Thành công 200 OK trả về danh sách nhân sự nhàn rỗi")
    void getProlongedIdleStaff_Success_200OK() throws Exception {
        ProlongedIdlenessWeeklyDetailResult w1 = new ProlongedIdlenessWeeklyDetailResult(
                2026, 38, BigDecimal.valueOf(40), BigDecimal.valueOf(10), BigDecimal.valueOf(30),
                BigDecimal.valueOf(25.0), true, false
        );

        ProlongedIdleStaffItemResult item = new ProlongedIdleStaffItemResult(
                101L, "EMP0101", "Nguyễn Văn A", 10L, "Khối Kỹ Thuật", "Backend Dev",
                3, BigDecimal.valueOf(90.0), BigDecimal.valueOf(25.0), List.of(w1)
        );

        ProlongedIdlenessReportResult report = new ProlongedIdlenessReportResult(
                null, "Toàn công ty", 2026, 38, 4, BigDecimal.valueOf(30.0), 3, 1, List.of(item)
        );

        when(getProlongedIdleStaffUseCase.getProlongedIdleStaff(any())).thenReturn(report);

        mockMvc.perform(get("/api/v1/capacity/prolonged-idleness")
                        .param("fromYear", "2026")
                        .param("fromWeek", "38")
                        .param("durationWeeks", "4")
                        .param("consecutiveThreshold", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.effectiveIdleThreshold").value(30.0))
                .andExpect(jsonPath("$.data.consecutiveThreshold").value(3))
                .andExpect(jsonPath("$.data.totalIdleEmployees").value(1))
                .andExpect(jsonPath("$.data.items[0].employeeCode").value("EMP0101"))
                .andExpect(jsonPath("$.data.items[0].fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.items[0].consecutiveIdleWeeks").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/capacity/prolonged-idleness — 403 Forbidden khi người dùng không có quyền (NCL-07-CN-006-TC-03)")
    void getProlongedIdleStaff_Forbidden_403() throws Exception {
        when(getProlongedIdleStaffUseCase.getProlongedIdleStaff(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ));

        mockMvc.perform(get("/api/v1/capacity/prolonged-idleness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/v1/capacity/prolonged-idleness/acknowledge — Thành công 200 OK (NCL-07-CN-006-TC-04)")
    void acknowledge_Success_200OK() throws Exception {
        AcknowledgeProlongedIdleStaffRequest request = new AcknowledgeProlongedIdleStaffRequest(
                101L, "Đã giao thêm việc", "Điều chuyển sang dự án X"
        );

        AcknowledgeProlongedIdleStaffResult result = new AcknowledgeProlongedIdleStaffResult(
                101L, "Nguyễn Văn A", "Đã giao thêm việc", "Điều chuyển sang dự án X",
                200L, LocalDateTime.of(2026, 9, 14, 10, 0), "ACKNOWLEDGED"
        );

        when(acknowledgeProlongedIdleStaffUseCase.acknowledgeProlongedIdleStaff(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/capacity/prolonged-idleness/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeId").value(101))
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.data.actionTaken").value("Đã giao thêm việc"));
    }

    @Test
    @DisplayName("POST /api/v1/capacity/prolonged-idleness/acknowledge — 400 Bad Request khi thiếu trường bắt buộc")
    void acknowledge_BadRequest_MissingField() throws Exception {
        AcknowledgeProlongedIdleStaffRequest invalidRequest = new AcknowledgeProlongedIdleStaffRequest(
                null, "", "Ghi chú"
        );

        mockMvc.perform(post("/api/v1/capacity/prolonged-idleness/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
