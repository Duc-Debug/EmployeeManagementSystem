package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult;
import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityMatrixResult.*;
import com.hrm.employeemanagement.application.dto.allocation.CompanyWeeklyCapacityQuery;
import com.hrm.employeemanagement.application.port.inbound.allocation.AllocateResourceUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetCompanyWeeklyCapacityUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.SearchResourceBySkillAndAvailabilityUseCase;
import com.hrm.employeemanagement.domain.allocation.CapacityStatus;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationOverloadWarningException;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceAllocationController Web API Tests")
class ResourceAllocationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AllocateResourceUseCase allocateResourceUseCase;

    @Mock
    private com.hrm.employeemanagement.application.port.inbound.allocation.BulkAllocateResourceUseCase bulkAllocateResourceUseCase;

    @Mock
    private SearchResourceBySkillAndAvailabilityUseCase searchResourceUseCase;

    @Mock
    private GetCompanyWeeklyCapacityUseCase getCompanyWeeklyCapacityUseCase;

    @BeforeEach
    void setUp() {
        ResourceAllocationController controller = new ResourceAllocationController(
                allocateResourceUseCase,
                bulkAllocateResourceUseCase,
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

    @Test
    @DisplayName("GET /api/v1/allocations/weekly-matrix - Nhận tham số page, size, search và trả về metadata phân trang")
    void getWeeklyCapacityMatrix_WithPaginationAndSearch() throws Exception {
        HeaderWeekInfo weekInfo = new HeaderWeekInfo(
                2026, 37, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), "T37 (07/09 - 13/09)"
        );
        CapacityMatrixSummaryResult summary = new CapacityMatrixSummaryResult(
                25, 1, 0, 0, 0, BigDecimal.valueOf(80.0)
        );
        CompanyWeeklyCapacityMatrixResult matrixResult = new CompanyWeeklyCapacityMatrixResult(
                10L, "Phòng CNTT", 2026, 37, 1, List.of(weekInfo), List.of(), summary, 1, 10, 25, 3
        );

        when(getCompanyWeeklyCapacityUseCase.getWeeklyCapacityMatrix(any(CompanyWeeklyCapacityQuery.class)))
                .thenReturn(matrixResult);

        mockMvc.perform(get("/api/v1/allocations/weekly-matrix")
                        .param("page", "1")
                        .param("size", "10")
                        .param("search", "Nguyen")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalEmployees").value(25))
                .andExpect(jsonPath("$.data.totalPages").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/allocations/weekly-matrix - Trả về 400 BAD REQUEST khi tuần 53 không hợp lệ với năm chỉ có 52 tuần (2025 W53)")
    void getWeeklyCapacityMatrix_InvalidIsoWeek_ReturnsBadRequest() throws Exception {
        // Năm 2025 chỉ có 52 tuần ISO, tuần 53 là không hợp lệ -> Query constructor ném InvalidWeekNumberException
        mockMvc.perform(get("/api/v1/allocations/weekly-matrix")
                        .param("fromYear", "2025")
                        .param("fromWeek", "53")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("GET /api/v1/allocations/weekly-matrix - Nhận tham số status và truyền chính xác vào UseCase")
    void getWeeklyCapacityMatrix_WithStatusFilter() throws Exception {
        HeaderWeekInfo weekInfo = new HeaderWeekInfo(
                2026, 37, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), "T37 (07/09 - 13/09)"
        );
        CapacityMatrixSummaryResult summary = new CapacityMatrixSummaryResult(
                50, 1, 5, 5, 10, BigDecimal.valueOf(85.0)
        );
        CompanyWeeklyCapacityMatrixResult matrixResult = new CompanyWeeklyCapacityMatrixResult(
                10L, "Phòng CNTT", 2026, 37, 1, List.of(weekInfo), List.of(), summary, 0, 20, 5, 1
        );

        when(getCompanyWeeklyCapacityUseCase.getWeeklyCapacityMatrix(argThat(q ->
                q.status() == com.hrm.employeemanagement.domain.allocation.CapacityStatus.OVERLOADED
        ))).thenReturn(matrixResult);

        mockMvc.perform(get("/api/v1/allocations/weekly-matrix")
                        .param("status", "OVERLOADED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEmployees").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/allocations/search - Từ chối toYear/toWeek chỉ được truyền một phần")
    void searchResources_WithPartialEndPeriod_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/allocations/search")
                        .param("skillId", "1")
                        .param("fromYear", "2026")
                        .param("fromWeek", "50")
                        .param("toYear", "2027"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("toYear và toWeek phải được cung cấp cùng nhau"));

        verifyNoInteractions(searchResourceUseCase);
    }

    @Test
    @DisplayName("POST /api/v1/allocations - Phân bổ theo tỷ lệ phần trăm thành công (200 OK)")
    void allocateResource_WithPercentage_Success() throws Exception {
        com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult capacityResult =
                new com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult(
                        100L, "EMP001", "Nguyễn Văn A", 2026, 36, 40,
                        BigDecimal.valueOf(40), BigDecimal.valueOf(20), BigDecimal.valueOf(20), false, null
                );

        when(allocateResourceUseCase.allocateResource(argThat(cmd ->
                cmd.employeeId().equals(100L) &&
                cmd.projectId().equals(10L) &&
                cmd.allocationPercentage() != null &&
                cmd.allocationPercentage().compareTo(BigDecimal.valueOf(50)) == 0
        ))).thenReturn(capacityResult);

        String jsonBody = """
                {
                    "employeeId": 100,
                    "projectId": 10,
                    "year": 2026,
                    "weekNumber": 36,
                    "allocationPercentage": 50
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/allocations")
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phân bổ nhân sự vào dự án theo tuần thành công"))
                .andExpect(jsonPath("$.data.totalAllocatedHours").value(20));
    }

    @Test
    @DisplayName("NCL-06-CN-007 TC-03: POST /api/v1/allocations - Từ chối khi không có quyền Quản lý nguồn lực (403 FORBIDDEN)")
    void allocateResource_Forbidden_WhenNotResourceManager() throws Exception {
        when(allocateResourceUseCase.allocateResource(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE));

        String jsonBody = """
                {
                    "employeeId": 100,
                    "projectId": 10,
                    "year": 2026,
                    "weekNumber": 36,
                    "allocationPercentage": 50
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/allocations")
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/v1/allocations - Trả về 400 và mã ALLOCATION_OVERLOAD_WARNING kèm details khi quá tải")
    void allocateResource_OverloadWarning_ReturnsBadRequestWithDetails() throws Exception {
        when(allocateResourceUseCase.allocateResource(any())).thenThrow(
                new AllocationOverloadWarningException(
                        "Quá tải 5 giờ",
                        BigDecimal.valueOf(40),
                        BigDecimal.valueOf(45),
                        BigDecimal.valueOf(5)
                )
        );

        String requestJson = """
                {
                    "employeeId": 1,
                    "projectId": 2,
                    "year": 2026,
                    "weekNumber": 38,
                    "allocatedHours": 45
                }
                """;

        mockMvc.perform(post("/api/v1/allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ALLOCATION_OVERLOAD_WARNING"))
                .andExpect(jsonPath("$.details.availableHours").value(40))
                .andExpect(jsonPath("$.details.allocatedHours").value(45))
                .andExpect(jsonPath("$.details.overloadHours").value(5));
    }

    @Test
    @DisplayName("NCL-06-CN-007 BLOCKING: POST /api/v1/allocations - Trả về 400 khi truyền đồng thời cả allocatedHours và allocationPercentage")
    void allocateResource_BothHoursAndPercentage_ReturnsBadRequest() throws Exception {
        String jsonBody = """
                {
                    "employeeId": 100,
                    "projectId": 10,
                    "year": 2026,
                    "weekNumber": 36,
                    "allocatedHours": 20,
                    "allocationPercentage": 50
                }
                """;

        mockMvc.perform(post("/api/v1/allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("NCL-06-CN-007 BLOCKING: POST /api/v1/allocations/bulk - Trả về 400 khi truyền đồng thời cả allocatedHoursPerWeek và allocationPercentagePerWeek")
    void bulkAllocateResource_BothHoursAndPercentage_ReturnsBadRequest() throws Exception {
        String jsonBody = """
                {
                    "employeeId": 100,
                    "projectId": 10,
                    "fromYear": 2026,
                    "fromWeek": 1,
                    "toYear": 2026,
                    "toWeek": 4,
                    "allocatedHoursPerWeek": 20,
                    "allocationPercentagePerWeek": 50
                }
                """;

        mockMvc.perform(post("/api/v1/allocations/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }
}
