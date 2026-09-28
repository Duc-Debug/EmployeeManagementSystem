package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.period;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPeriodResult;
import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPlanSnapshotResult;
import com.hrm.employeemanagement.application.dto.allocation.period.PeriodLockCheckResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CreateAllocationPeriodUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.GetAllocationPeriodsUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.LockAllocationPeriodUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.UnlockAllocationPeriodUseCase;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodStatus;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodType;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodNotFoundException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.period.dto.CreatePeriodRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation.period.dto.UnlockPeriodRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AllocationPeriodController Web REST API Tests (NCL-06-CN-009)")
class AllocationPeriodControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CreateAllocationPeriodUseCase createUseCase;

    @Mock
    private LockAllocationPeriodUseCase lockUseCase;

    @Mock
    private UnlockAllocationPeriodUseCase unlockUseCase;

    @Mock
    private GetAllocationPeriodsUseCase getPeriodsUseCase;

    @Mock
    private CheckAllocationPeriodLockUseCase checkLockUseCase;

    @BeforeEach
    void setUp() {
        AllocationPeriodController controller = new AllocationPeriodController(
                createUseCase,
                lockUseCase,
                unlockUseCase,
                getPeriodsUseCase,
                checkLockUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AllocationPeriodExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/allocations/periods: Tạo mới kỳ kế hoạch phân bổ thành công (201 Created)")
    void shouldCreatePeriodSuccessfully() throws Exception {
        CreatePeriodRequest request = new CreatePeriodRequest(
                "Kế hoạch Quý 1/2026",
                AllocationPeriodType.QUARTER,
                2026,
                1,
                13
        );

        AllocationPeriodResult mockResult = new AllocationPeriodResult(
                1L, "Kế hoạch Quý 1/2026", AllocationPeriodType.QUARTER, 2026, 1, 13,
                AllocationPeriodStatus.OPEN, null, null, null, null, null, 1L, LocalDateTime.now(), null
        );

        when(createUseCase.createPeriod(any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/allocations/periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("Kế hoạch Quý 1/2026")))
                .andExpect(jsonPath("$.data.status", is("OPEN")));
    }

    @Test
    @DisplayName("TC-01: POST /api/v1/allocations/periods/{id}/lock: Khóa kỳ phân bổ thành công (200 OK)")
    void tc01_shouldLockPeriodSuccessfully() throws Exception {
        AllocationPlanSnapshotResult snapshotResult = new AllocationPlanSnapshotResult(
                10L, 1L, 1, 45, BigDecimal.valueOf(1800.0), 3L, LocalDateTime.now(), List.of()
        );

        AllocationPeriodResult mockResult = new AllocationPeriodResult(
                1L, "Kế hoạch Quý 1/2026", AllocationPeriodType.QUARTER, 2026, 1, 13,
                AllocationPeriodStatus.LOCKED, 3L, LocalDateTime.now(), null, null, null, 1L, LocalDateTime.now(), snapshotResult
        );

        when(lockUseCase.lockPeriod(any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/allocations/periods/1/lock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.status", is("LOCKED")))
                .andExpect(jsonPath("$.data.latestSnapshot.snapshotVersion", is(1)))
                .andExpect(jsonPath("$.data.latestSnapshot.totalAllocations", is(45)));
    }

    @Test
    @DisplayName("TC-04: POST /api/v1/allocations/periods/{id}/unlock: Mở lại kỳ kế hoạch thành công (200 OK)")
    void tc04_shouldUnlockPeriodSuccessfully() throws Exception {
        UnlockPeriodRequest request = new UnlockPeriodRequest("Yêu cầu điều chỉnh phân bổ theo chỉ đạo");

        AllocationPeriodResult mockResult = new AllocationPeriodResult(
                1L, "Kế hoạch Quý 1/2026", AllocationPeriodType.QUARTER, 2026, 1, 13,
                AllocationPeriodStatus.OPEN, 3L, LocalDateTime.now(), 3L, LocalDateTime.now(),
                "Yêu cầu điều chỉnh phân bổ theo chỉ đạo", 1L, LocalDateTime.now(), null
        );

        when(unlockUseCase.unlockPeriod(any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/allocations/periods/1/unlock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("OPEN")))
                .andExpect(jsonPath("$.data.unlockReason", is("Yêu cầu điều chỉnh phân bổ theo chỉ đạo")));
    }

    @Test
    @DisplayName("TC-03: Trả về 403 Forbidden khi thiếu quyền RESOURCE_ALLOCATION_LOCK")
    void tc03_shouldReturn403WhenPermissionDenied() throws Exception {
        when(lockUseCase.lockPeriod(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_LOCK));

        mockMvc.perform(post("/api/v1/allocations/periods/1/lock"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("PERMISSION_DENIED")));
    }

    @Test
    @DisplayName("Trả về 404 Not Found khi kỳ kế hoạch không tồn tại")
    void shouldReturn404WhenPeriodNotFound() throws Exception {
        when(lockUseCase.lockPeriod(any()))
                .thenThrow(new AllocationPeriodNotFoundException(999L));

        mockMvc.perform(post("/api/v1/allocations/periods/999/lock"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("ALLOCATION_PERIOD_NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/v1/allocations/periods/check-lock: Kiểm tra tuần bị khóa")
    void shouldCheckWeekLockEndpoint() throws Exception {
        when(checkLockUseCase.checkWeekLock(2026, 10))
                .thenReturn(PeriodLockCheckResult.locked(1L, "Kế hoạch Quý 1/2026", 2026, 10));

        mockMvc.perform(get("/api/v1/allocations/periods/check-lock")
                        .param("year", "2026")
                        .param("weekNumber", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.isLocked", is(true)))
                .andExpect(jsonPath("$.data.periodId", is(1)));
    }
}
