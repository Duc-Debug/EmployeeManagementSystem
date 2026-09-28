package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.employee;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import com.hrm.employeemanagement.application.dto.employee.CreateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.dto.employee.EmployeeProfileResult;
import com.hrm.employeemanagement.application.dto.employee.UpdateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.employee.CreateEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.employee.GetEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.employee.UpdateEmployeeProfileUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final CreateEmployeeProfileUseCase createEmployeeProfileUseCase;
    private final UpdateEmployeeProfileUseCase updateEmployeeProfileUseCase;
    private final GetEmployeeProfileUseCase getEmployeeProfileUseCase;

    public EmployeeController(CreateEmployeeProfileUseCase createEmployeeProfileUseCase,
                              UpdateEmployeeProfileUseCase updateEmployeeProfileUseCase,
                              GetEmployeeProfileUseCase getEmployeeProfileUseCase) {
        this.createEmployeeProfileUseCase = createEmployeeProfileUseCase;
        this.updateEmployeeProfileUseCase = updateEmployeeProfileUseCase;
        this.getEmployeeProfileUseCase = getEmployeeProfileUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VT-05') or (hasAuthority('EMPLOYEE_UPDATE') and !hasAuthority('VT-06'))")
    public ResponseEntity<EmployeeProfileResult> createProfile(@Valid @RequestBody CreateEmployeeRequest request) {
        CreateEmployeeProfileCommand command = new CreateEmployeeProfileCommand(
            request.userId(),
            request.orgUnitId(),
            request.employeeCode(),
            request.fullName(),
            request.professionalRole(),
            request.startDate(),
            request.contractEndDate(),
            request.standardHoursPerWeek()
        );
        EmployeeProfileResult result = createEmployeeProfileUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VT-05') or (hasAuthority('EMPLOYEE_UPDATE') and !hasAuthority('VT-06'))")
    public ResponseEntity<EmployeeProfileResult> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        UpdateEmployeeProfileCommand command = new UpdateEmployeeProfileCommand(
            id,
            request.orgUnitId(),
            request.fullName(),
            request.professionalRole(),
            request.startDate(),
            request.contractEndDate(),
            request.standardHoursPerWeek(),
            request.version()
        );
        EmployeeProfileResult result = updateEmployeeProfileUseCase.execute(command);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResult<EmployeeProfileResult>>> getEmployees(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<EmployeeProfileResult> result = getEmployeeProfileUseCase.getEmployees(page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hồ sơ nhân sự thành công", result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeProfileResult> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getEmployeeProfileUseCase.getById(id));
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeProfileResult> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(getEmployeeProfileUseCase.getByUserId(userId));
    }

    // Request DTOs
    public record CreateEmployeeRequest(
        @NotNull(message = "UserId không được để trống") Long userId,
        @NotNull(message = "OrgUnitId không được để trống") Long orgUnitId,
        @NotBlank(message = "Mã nhân viên không được để trống") String employeeCode,
        @NotBlank(message = "Họ tên không được để trống") String fullName,
        String professionalRole,
        LocalDate startDate,
        LocalDate contractEndDate,
        @NotNull @Min(value = 1, message = "Số giờ chuẩn phải lớn hơn 0")
        @Max(value = 168, message = "Số giờ chuẩn không được lớn hơn 168") Integer standardHoursPerWeek
    ) {}

    public record UpdateEmployeeRequest(
        @NotNull(message = "Version không được để trống") Long version,
        @NotNull(message = "OrgUnitId không được để trống") Long orgUnitId,
        @NotBlank(message = "Họ tên không được để trống") String fullName,
        String professionalRole,
        LocalDate startDate,
        LocalDate contractEndDate,
        @NotNull @Min(value = 1, message = "Số giờ chuẩn phải lớn hơn 0")
        @Max(value = 168, message = "Số giờ chuẩn không được lớn hơn 168") Integer standardHoursPerWeek
    ) {}
}
