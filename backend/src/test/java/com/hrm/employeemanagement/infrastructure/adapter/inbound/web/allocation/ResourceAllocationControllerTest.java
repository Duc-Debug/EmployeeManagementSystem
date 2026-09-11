package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult;
import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult.*;
import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetCompanyWeeklyCapacityUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.SearchResourceBySkillAndAvailabilityUseCase;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceAllocationController Web API Tests")
class ResourceAllocationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AllocateResourceUseCase allocateResourceUseCase;

    @Mock
    private SearchResourceBySkillAndAvailabilityUseCase searchResourceUseCase;

    @Mock
    private GetCompanyWeeklyCapacityUseCase getCompanyWeeklyCapacityUseCase;

    @BeforeEach
    void setUp() {
        ResourceAllocationController controller = new ResourceAllocationController(
                allocateResourceUseCase,
                searchResourceUseCase,
                getCompanyWeeklyCapacityUseCase
        );

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/allocations/weekly-matrix - Trả về ma trận năng lực theo tuần thành công (200 OK)")
    void getWeeklyCapacityMatrix_Success() throws Exception {
        HeaderWeekInfo weekInfo = new HeaderWeekInfo(
                2026, 37, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), "T37 (07/09 - 13/09)"
        );
        CapacityMatrixCellResult cell = new CapacityMatrixCellResult(
                2026, 37, BigDecimal.valueOf(32.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(8.0),
                BigDecimal.valueOf(80.0), false, BigDecimal.ZERO, CapacityStatus.OPTIMAL
        );
        EmployeeCapacityRowResult row = new EmployeeCapacityRowResult(
                1L, "EMP001", "Nguyễn Văn A", 10L, "Phòng CNTT", "Backend Dev",
                List.of(cell), BigDecimal.valueOf(32.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(80.0), 0
        );
        CapacityMatrixSummaryResult summary = new CapacityMatrixSummaryResult(
                1, 1, 0, 0, 0, BigDecimal.valueOf(80.0)
        );

        CompanyWeeklyCapacityMatrixResult matrixResult = new CompanyWeeklyCapacityMatrixResult(
                10L, "Phòng CNTT", 2026, 37, 1, List.of(weekInfo), List.of(row), summary
        );

        when(getCompanyWeeklyCapacityUseCase.getWeeklyCapacityMatrix(any(CompanyWeeklyCapacityQuery.class)))
                .thenReturn(matrixResult);

        mockMvc.perform(get("/api/v1/allocations/weekly-matrix")
                        .param("orgUnitId", "10")
                        .param("fromYear", "2026")
                        .param("fromWeek", "37")
                        .param("durationWeeks", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lấy bảng năng lực theo tuần thành công"))
                .andExpect(jsonPath("$.data.orgUnitId").value(10))
                .andExpect(jsonPath("$.data.weeks[0].weekNumber").value(37))
                .andExpect(jsonPath("$.data.rows[0].employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.data.rows[0].cells[0].utilizationPercentage").value(80.0))
                .andExpect(jsonPath("$.data.rows[0].cells[0].status").value("OPTIMAL"));
    }

    @Test
    @DisplayName("GET /api/v1/allocations/weekly-matrix - Trả về 403 khi bị từ chối quyền (PermissionDeniedException)")
    void getWeeklyCapacityMatrix_Forbidden() throws Exception {
        when(getCompanyWeeklyCapacityUseCase.getWeeklyCapacityMatrix(any(CompanyWeeklyCapacityQuery.class)))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ));

        mockMvc.perform(get("/api/v1/allocations/weekly-matrix")
                        .param("orgUnitId", "999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}