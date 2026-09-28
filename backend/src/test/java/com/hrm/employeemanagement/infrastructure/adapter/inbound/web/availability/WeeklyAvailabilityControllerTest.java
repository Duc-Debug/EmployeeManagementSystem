package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.availability;

import com.hrm.employeemanagement.application.dto.availability.CalculateWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.dto.availability.DeclareWeeklyAvailabilityCommand;
import com.hrm.employeemanagement.application.dto.availability.WeeklyAvailabilityResult;
import com.hrm.employeemanagement.application.port.inbound.availability.CalculateWeeklyCapacityUseCase;
import com.hrm.employeemanagement.application.port.inbound.availability.DeclareWeeklyAvailabilityUseCase;
import com.hrm.employeemanagement.application.port.inbound.availability.GetWeeklyAvailabilityUseCase;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyAvailabilityController Web API Tests")
class WeeklyAvailabilityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeclareWeeklyAvailabilityUseCase declareWeeklyAvailabilityUseCase;

    @Mock
    private CalculateWeeklyCapacityUseCase calculateWeeklyCapacityUseCase;

    @Mock
    private GetWeeklyAvailabilityUseCase getWeeklyAvailabilityUseCase;

    @BeforeEach
    void setUp() {
        WeeklyAvailabilityController controller = new WeeklyAvailabilityController(
                declareWeeklyAvailabilityUseCase,
                calculateWeeklyCapacityUseCase,
                getWeeklyAvailabilityUseCase
        );

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id}/availability - Khai báo giờ chuẩn tuần thành công")
    void declareAvailability_Success() throws Exception {
        WeeklyAvailabilityResult result = new WeeklyAvailabilityResult(
                1L, 2026, 36, 40, 0, BigDecimal.ZERO, new BigDecimal("40.00"));

        when(declareWeeklyAvailabilityUseCase.execute(any(DeclareWeeklyAvailabilityCommand.class)))
                .thenReturn(result);

        String jsonPayload = """
            {
                "year": 2026,
                "weekNumber": 36,
                "standardHours": 40
            }
            """;

        mockMvc.perform(put("/api/v1/employees/1/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.weekNumber").value(36))
                .andExpect(jsonPath("$.standardHours").value(40))
                .andExpect(jsonPath("$.netAvailableHours").value(40.0));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id}/availability - Báo lỗi 400 khi số giờ chuẩn <= 0")
    void declareAvailability_InvalidHours() throws Exception {
        String jsonPayload = """
            {
                "year": 2026,
                "weekNumber": 36,
                "standardHours": 0
            }
            """;

        mockMvc.perform(put("/api/v1/employees/1/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id}/availability - Báo lỗi 404 khi nhân viên không tồn tại")
    void declareAvailability_NotFound() throws Exception {
        when(declareWeeklyAvailabilityUseCase.execute(any(DeclareWeeklyAvailabilityCommand.class)))
                .thenThrow(new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: 999"));

        String jsonPayload = """
            {
                "year": 2026,
                "weekNumber": 36,
                "standardHours": 40
            }
            """;

        mockMvc.perform(put("/api/v1/employees/999/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id}/capacity - Lấy năng lực khả dụng tuần theo QTN-10 thành công")
    void getWeeklyCapacity_Success() throws Exception {
        WeeklyAvailabilityResult result = new WeeklyAvailabilityResult(
                1L, 2026, 36, 40, 8, new BigDecimal("8.00"), new BigDecimal("24.00"));

        when(calculateWeeklyCapacityUseCase.calculate(any(CalculateWeeklyCapacityQuery.class)))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/employees/1/capacity")
                        .param("year", "2026")
                        .param("weekNumber", "36"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standardHours").value(40))
                .andExpect(jsonPath("$.holidayHours").value(8))
                .andExpect(jsonPath("$.approvedLeaveHours").value(8.0))
                .andExpect(jsonPath("$.netAvailableHours").value(24.0));
    }
}
