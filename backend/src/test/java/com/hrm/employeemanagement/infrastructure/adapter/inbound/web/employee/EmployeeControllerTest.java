package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.employee;

import com.hrm.employeemanagement.application.dto.employee.CreateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.dto.employee.EmployeeProfileResult;
import com.hrm.employeemanagement.application.dto.employee.UpdateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.port.inbound.employee.CreateEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.employee.GetEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.employee.UpdateEmployeeProfileUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateEmployeeProfileUseCase createEmployeeProfileUseCase;

    @Mock
    private UpdateEmployeeProfileUseCase updateEmployeeProfileUseCase;

    @Mock
    private GetEmployeeProfileUseCase getEmployeeProfileUseCase;

    @BeforeEach
    void setUp() {
        EmployeeController controller = new EmployeeController(
                createEmployeeProfileUseCase,
                updateEmployeeProfileUseCase,
                getEmployeeProfileUseCase
        );

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/employees - Khai báo hồ sơ nhân sự thành công")
    void createProfile_Success() throws Exception {
        String jsonPayload = """
            {
              "userId": 1,
              "orgUnitId": 1,
              "employeeCode": "NV-2026-001",
              "fullName": "Nguyễn Văn A",
              "professionalRole": "Developer",
              "startDate": "2026-01-01",
              "contractEndDate": "2028-01-01",
              "standardHoursPerWeek": 40
            }
            """;

        EmployeeProfileResult result = new EmployeeProfileResult(
                100L, 1L, 1L, "NV-2026-001", "Nguyễn Văn A", "Developer",
                LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 1), false, 40, "ACTIVE"
        );

        when(createEmployeeProfileUseCase.execute(any(CreateEmployeeProfileCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.employeeCode").value("NV-2026-001"))
                .andExpect(jsonPath("$.standardHoursPerWeek").value(40));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/1 - Cập nhật hồ sơ nhân sự thành công")
    void updateProfile_Success() throws Exception {
        String jsonPayload = """
            {
              "version": 0,
              "orgUnitId": 1,
              "fullName": "Nguyễn Văn B",
              "professionalRole": "Senior Developer",
              "startDate": "2026-01-01",
              "contractEndDate": "2028-01-01",
              "standardHoursPerWeek": 44
            }
            """;

        EmployeeProfileResult result = new EmployeeProfileResult(
                1L, 1L, 1L, "NV-2026-001", "Nguyễn Văn B", "Senior Developer",
                LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 1), false, 44, "ACTIVE"
        );

        when(updateEmployeeProfileUseCase.execute(any(UpdateEmployeeProfileCommand.class))).thenReturn(result);

        mockMvc.perform(put("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn B"))
                .andExpect(jsonPath("$.standardHoursPerWeek").value(44));
    }

    @Test
    @DisplayName("GET /api/v1/employees/1 - Lấy chi tiết hồ sơ nhân sự thành công")
    void getById_Success() throws Exception {
        EmployeeProfileResult result = new EmployeeProfileResult(
                1L, 1L, 1L, "NV-2026-001", "Nguyễn Văn A", "Developer",
                LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 1), false, 40, "ACTIVE"
        );

        when(getEmployeeProfileUseCase.getById(1L)).thenReturn(result);

        mockMvc.perform(get("/api/v1/employees/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/employees/by-user/1 - Lấy hồ sơ theo tài khoản")
    void getByUserId_Success() throws Exception {
        EmployeeProfileResult result = new EmployeeProfileResult(
                1L, 1L, 1L, "NV-2026-001", "Nguyễn Văn A", "Developer",
                LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 1), false, 40, "ACTIVE");
        when(getEmployeeProfileUseCase.getByUserId(1L)).thenReturn(result);

        mockMvc.perform(get("/api/v1/employees/by-user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/employees - Thiếu userId và employeeCode trả về 400")
    void createProfile_MissingRequiredFields_ReturnsBadRequest() throws Exception {
        String jsonPayload = """
            {
              "version": 0,
              "orgUnitId": 1,
              "fullName": "Nguyễn Văn A",
              "professionalRole": "Senior Java Developer",
              "startDate": "2026-01-01",
              "contractEndDate": "2028-01-01",
              "standardHoursPerWeek": 44
            }
            """;

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/1 - Thiếu quyền trả về 403 thay vì 500")
    void updateProfile_PermissionDenied_ReturnsForbidden() throws Exception {
        when(updateEmployeeProfileUseCase.execute(any(UpdateEmployeeProfileCommand.class)))
                .thenThrow(new PermissionDeniedException(PermissionCode.EMPLOYEE_UPDATE));

        mockMvc.perform(put("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdatePayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/1 - Thiếu version trả về 400")
    void updateProfile_MissingVersion_ReturnsBadRequest() throws Exception {
        String jsonPayload = """
            {
              "orgUnitId": 1,
              "fullName": "Nguyễn Văn A",
              "standardHoursPerWeek": 40
            }
            """;

        mockMvc.perform(put("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/employees - User không tồn tại trả về 404 thay vì 500")
    void createProfile_UserNotFound_ReturnsNotFound() throws Exception {
        when(createEmployeeProfileUseCase.execute(any(CreateEmployeeProfileCommand.class)))
                .thenThrow(new UserNotFoundException("Không tìm thấy người dùng với ID: 999"));

        String jsonPayload = """
            {
              "userId": 999,
              "orgUnitId": 1,
              "employeeCode": "EMP-999",
              "fullName": "Nguyễn Văn A",
              "professionalRole": "Senior Java Developer",
              "startDate": "2026-01-01",
              "contractEndDate": "2028-01-01",
              "standardHoursPerWeek": 44
            }
            """;

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    private String validUpdatePayload() {
        return """
            {
              "version": 0,
              "orgUnitId": 1,
              "fullName": "Nguyễn Văn A",
              "professionalRole": "Senior Java Developer",
              "startDate": "2026-01-01",
              "contractEndDate": "2028-01-01",
              "standardHoursPerWeek": 44
            }
            """;
    }
}
