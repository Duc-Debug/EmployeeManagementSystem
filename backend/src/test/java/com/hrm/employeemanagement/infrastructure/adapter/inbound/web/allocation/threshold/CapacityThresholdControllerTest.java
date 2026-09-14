package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.threshold;

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
import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdHistoryResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.ConfigureCapacityThresholdCommand;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.ConfigureCapacityThresholdUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.GetCapacityThresholdHistoryUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.GetCapacityThresholdUseCase;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidCapacityThresholdException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.threshold.dto.ConfigureCapacityThresholdRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CapacityThresholdController Web API Tests (NCL-07-CN-004)")
class CapacityThresholdControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ConfigureCapacityThresholdUseCase configureUseCase;

    @Mock
    private GetCapacityThresholdUseCase getUseCase;

    @Mock
    private GetCapacityThresholdHistoryUseCase historyUseCase;

    @BeforeEach
    void setUp() {
        CapacityThresholdController controller = new CapacityThresholdController(
                configureUseCase,
                getUseCase,
                historyUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/v1/capacity-thresholds trả về 200 OK với thông tin ngưỡng hiện hành")
    void testGetThreshold_Success() throws Exception {
        CapacityThresholdResult mockResult = new CapacityThresholdResult(
                1L,
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                false,
                1L,
                LocalDateTime.now(),
                1L,
                "Giám Đốc"
        );

        when(getUseCase.getEffectiveThreshold(CapacityThresholdScope.COMPANY, null)).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/capacity-thresholds")
                        .param("scopeType", "COMPANY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overloadThreshold").value(90.0))
                .andExpect(jsonPath("$.data.idleThreshold").value(20.0));
    }

    @Test
    @DisplayName("PUT /api/v1/capacity-thresholds thành công trả về 200 OK (TC-01)")
    void testConfigureThreshold_Success() throws Exception {
        ConfigureCapacityThresholdRequest request = new ConfigureCapacityThresholdRequest(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                1L
        );

        CapacityThresholdResult mockResult = new CapacityThresholdResult(
                1L,
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                false,
                2L,
                LocalDateTime.now(),
                1L,
                "Giám Đốc"
        );

        when(configureUseCase.configureThreshold(any(ConfigureCapacityThresholdCommand.class))).thenReturn(mockResult);

        mockMvc.perform(put("/api/v1/capacity-thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overloadThreshold").value(90.0))
                .andExpect(jsonPath("$.data.idleThreshold").value(20.0));
    }

    @Test
    @DisplayName("PUT /api/v1/capacity-thresholds validation lỗi Bean Validation (null field) trả về 400")
    void testConfigureThreshold_NullField_Returns400() throws Exception {
        ConfigureCapacityThresholdRequest request = new ConfigureCapacityThresholdRequest(
                CapacityThresholdScope.COMPANY,
                null,
                null,
                BigDecimal.valueOf(20.0),
                null
        );

        mockMvc.perform(put("/api/v1/capacity-thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT /api/v1/capacity-thresholds ném InvalidCapacityThresholdException khi idle > overload trả về 400 (TC-02)")
    void testConfigureThreshold_DomainValidationFailure_Returns400() throws Exception {
        ConfigureCapacityThresholdRequest request = new ConfigureCapacityThresholdRequest(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(70.0),
                BigDecimal.valueOf(80.0),
                null
        );

        when(configureUseCase.configureThreshold(any()))
                .thenThrow(new InvalidCapacityThresholdException("Ngưỡng nhàn rỗi (80.0%) phải nhỏ hơn ngưỡng quá tải (70.0%)"));

        mockMvc.perform(put("/api/v1/capacity-thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CAPACITY_THRESHOLD"));
    }

    @Test
    @DisplayName("PUT /api/v1/capacity-thresholds ném PermissionDeniedException trả về 403 (TC-03)")
    void testConfigureThreshold_PermissionDenied_Returns403() throws Exception {
        ConfigureCapacityThresholdRequest request = new ConfigureCapacityThresholdRequest(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(90.0),
                BigDecimal.valueOf(20.0),
                null
        );

        when(configureUseCase.configureThreshold(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.CAPACITY_THRESHOLD_MANAGE));

        mockMvc.perform(put("/api/v1/capacity-thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("HIGH #1: PUT /api/v1/capacity-thresholds ném CapacityThresholdVersionConflictException trả về 409 Conflict")
    void testConfigureThreshold_VersionConflict_Returns409() throws Exception {
        ConfigureCapacityThresholdRequest request = new ConfigureCapacityThresholdRequest(
                CapacityThresholdScope.COMPANY,
                null,
                BigDecimal.valueOf(95.0),
                BigDecimal.valueOf(25.0),
                4L
        );

        when(configureUseCase.configureThreshold(any()))
                .thenThrow(new com.hrm.employeemanagement.domain.exception.allocation.CapacityThresholdVersionConflictException(
                        "Cấu hình ngưỡng đã được cập nhật bởi thao tác khác (phiên bản hiện tại: 5, phiên bản gửi lên: 4)."
                ));

        mockMvc.perform(put("/api/v1/capacity-thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION_CONFLICT"));
    }

    @Test
    @DisplayName("GET /api/v1/capacity-thresholds/history trả về 200 OK với danh sách lịch sử kiểm toán (TC-04)")
    void testGetHistory_Success() throws Exception {
        CapacityThresholdHistoryResult item = new CapacityThresholdHistoryResult(
                1L,
                1L,
                "Giám Đốc",
                "UPDATE_CAPACITY_THRESHOLD",
                "{\"overloadThreshold\":100.0,\"idleThreshold\":50.0}",
                "{\"overloadThreshold\":90.0,\"idleThreshold\":20.0}",
                LocalDateTime.now()
        );

        when(historyUseCase.getHistory(CapacityThresholdScope.COMPANY, null)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/capacity-thresholds/history")
                        .param("scopeType", "COMPANY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].action").value("UPDATE_CAPACITY_THRESHOLD"));
    }
}
