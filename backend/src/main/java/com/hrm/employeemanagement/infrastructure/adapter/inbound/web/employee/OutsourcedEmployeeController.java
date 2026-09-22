package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.employee;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.employee.DeclareOutsourcedEmployeeCommand;
import com.hrm.employeemanagement.application.dto.employee.OutsourcedEmployeeResult;
import com.hrm.employeemanagement.application.port.inbound.employee.DeclareOutsourcedEmployeeUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/employees/outsourced")
public class OutsourcedEmployeeController {

    private final DeclareOutsourcedEmployeeUseCase declareOutsourcedEmployeeUseCase;

    public OutsourcedEmployeeController(DeclareOutsourcedEmployeeUseCase declareOutsourcedEmployeeUseCase) {
        this.declareOutsourcedEmployeeUseCase = declareOutsourcedEmployeeUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VT-05') or (hasAuthority('EMPLOYEE_UPDATE') and !hasAuthority('VT-06'))")
    public ResponseEntity<ApiResponse<OutsourcedEmployeeResult>> declareOutsourcedEmployee(
            @Valid @RequestBody DeclareOutsourcedEmployeeRequest request
    ) {
        DeclareOutsourcedEmployeeCommand command = new DeclareOutsourcedEmployeeCommand(
                request.orgUnitId(),
                request.employeeCode(),
                request.fullName(),
                request.providerName(),
                request.professionalRole(),
                request.startDate(),
                request.contractEndDate(),
                request.standardHoursPerWeek(),
                request.skillIds()
        );

        OutsourcedEmployeeResult result = declareOutsourcedEmployeeUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khai báo hồ sơ nhân sự thuê ngoài thành công", result));
    }

    public record DeclareOutsourcedEmployeeRequest(
            @NotNull(message = "Đơn vị phòng ban tiếp nhận không được để trống")
            Long orgUnitId,

            @Size(max = 50, message = "Mã nhân viên không được vượt quá 50 ký tự")
            String employeeCode,

            @NotBlank(message = "Họ tên nhân sự không được để trống")
            @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
            String fullName,

            @NotBlank(message = "Đơn vị cung cấp không được để trống")
            @Size(max = 255, message = "Đơn vị cung cấp không được vượt quá 255 ký tự")
            String providerName,

            @Size(max = 100, message = "Vai trò chuyên môn không được vượt quá 100 ký tự")
            String professionalRole,

            @NotNull(message = "Ngày bắt đầu hợp đồng thuê không được để trống")
            LocalDate startDate,

            @NotNull(message = "Ngày kết thúc hợp đồng thuê không được để trống")
            LocalDate contractEndDate,

            @Min(value = 1, message = "Số giờ chuẩn phải lớn hơn 0")
            @Max(value = 168, message = "Số giờ chuẩn không được lớn hơn 168")
            Integer standardHoursPerWeek,

            List<Long> skillIds
    ) {}
}
