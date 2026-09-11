package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave;

import com.hrm.employeemanagement.application.dto.leave.DepartmentMonthlyLeaveCalendarResult;
import com.hrm.employeemanagement.application.dto.leave.GetDepartmentMonthlyLeaveCalendarQuery;
import com.hrm.employeemanagement.application.port.inbound.leave.GetDepartmentMonthlyLeaveCalendarUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
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
@DisplayName("DepartmentLeaveCalendarController Web API Tests (NCL-05-CN-006)")
class DepartmentLeaveCalendarControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetDepartmentMonthlyLeaveCalendarUseCase getDepartmentMonthlyLeaveCalendarUseCase;

    @BeforeEach
    void setUp() {
        DepartmentLeaveCalendarController controller = new DepartmentLeaveCalendarController(
                getDepartmentMonthlyLeaveCalendarUseCase
        );

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/leave-requests/department-calendar - Thành công trả về 200 OK với cấu trúc lịch tháng (TC-01)")
    void getCalendar_Success() throws Exception {
        DepartmentMonthlyLeaveCalendarResult.LeaveCalendarItemResult item1 =
                new DepartmentMonthlyLeaveCalendarResult.LeaveCalendarItemResult(
                        101L, 1L, "EMP-01", "Nguyễn Văn A",
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3),
                        "APPROVED", new BigDecimal("24.00"), "ANNUAL", "Nghỉ phép năm"
                );

        DepartmentMonthlyLeaveCalendarResult.DailyLeaveSummaryResult day1 =
                new DepartmentMonthlyLeaveCalendarResult.DailyLeaveSummaryResult(
                        LocalDate.of(2026, 9, 1), "TUESDAY", 1, 1, 0, false, null,
                        true, false, new BigDecimal("8.00"), List.of(item1)
                );

        DepartmentMonthlyLeaveCalendarResult result = new DepartmentMonthlyLeaveCalendarResult(
                10L, "DEV-DEP", "Phòng Phát triển",
                2026, 9, 5, 0.50, 1, 0,
                List.of(item1), List.of(day1)
        );

        when(getDepartmentMonthlyLeaveCalendarUseCase.execute(any(GetDepartmentMonthlyLeaveCalendarQuery.class)))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/leave-requests/department-calendar")
                        .param("orgUnitId", "10")
                        .param("year", "2026")
                        .param("month", "9")
                        .param("includeSubUnits", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orgUnitId").value(10))
                .andExpect(jsonPath("$.data.orgUnitCode").value("DEV-DEP"))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(9))
                .andExpect(jsonPath("$.data.totalDepartmentEmployees").value(5))
                .andExpect(jsonPath("$.data.totalLeaveRequests").value(1))
                .andExpect(jsonPath("$.data.leaveItems[0].employeeCode").value("EMP-01"))
                .andExpect(jsonPath("$.data.dailySummaries[0].date").value("2026-09-01"))
                .andExpect(jsonPath("$.data.dailySummaries[0].isWorkingDay").value(true))
                .andExpect(jsonPath("$.data.dailySummaries[0].isHoliday").value(false))
                .andExpect(jsonPath("$.data.dailySummaries[0].totalLeaveHours").value(8.00));
    }

    @Test
    @DisplayName("GET /api/v1/leave-requests/department-calendar/{orgUnitId} - Đường dẫn path variable trả về 200 OK")
    void getCalendarByPath_Success() throws Exception {
        DepartmentMonthlyLeaveCalendarResult result = new DepartmentMonthlyLeaveCalendarResult(
                10L, "DEV-DEP", "Phòng Phát triển",
                2026, 9, 5, 0.50, 0, 0,
                List.of(), List.of()
        );

        when(getDepartmentMonthlyLeaveCalendarUseCase.execute(any(GetDepartmentMonthlyLeaveCalendarQuery.class)))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/leave-requests/department-calendar/10")
                        .param("year", "2026")
                        .param("month", "9")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orgUnitId").value(10));
    }

    @Test
    @DisplayName("Khi người dùng không có quyền (PermissionDeniedException) -> Trả về 403 Forbidden (TC-03)")
    void getCalendar_PermissionDenied() throws Exception {
        when(getDepartmentMonthlyLeaveCalendarUseCase.execute(any(GetDepartmentMonthlyLeaveCalendarQuery.class)))
                .thenThrow(new PermissionDeniedException(PermissionCode.DEPARTMENT_LEAVE_READ));

        mockMvc.perform(get("/api/v1/leave-requests/department-calendar")
                        .param("orgUnitId", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Khi phòng ban không tồn tại (OrgUnitNotFoundException) -> Trả về 404 Not Found")
    void getCalendar_OrgUnitNotFound() throws Exception {
        when(getDepartmentMonthlyLeaveCalendarUseCase.execute(any(GetDepartmentMonthlyLeaveCalendarQuery.class)))
                .thenThrow(new OrgUnitNotFoundException("Không tìm thấy bộ phận với ID: 999"));

        mockMvc.perform(get("/api/v1/leave-requests/department-calendar")
                        .param("orgUnitId", "999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORG_UNIT_NOT_FOUND"));
    }
}
