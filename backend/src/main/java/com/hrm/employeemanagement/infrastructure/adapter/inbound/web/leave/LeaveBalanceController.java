package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveBalanceResult;
import com.hrm.employeemanagement.application.port.inbound.leave.GetEmployeeLeaveBalanceUseCase;
import com.hrm.employeemanagement.application.port.inbound.leave.GetMyLeaveBalanceUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto.LeaveBalanceResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/leaves/balances")
public class LeaveBalanceController {

    private final GetMyLeaveBalanceUseCase getMyLeaveBalanceUseCase;
    private final GetEmployeeLeaveBalanceUseCase getEmployeeLeaveBalanceUseCase;

    public LeaveBalanceController(
            GetMyLeaveBalanceUseCase getMyLeaveBalanceUseCase,
            GetEmployeeLeaveBalanceUseCase getEmployeeLeaveBalanceUseCase
    ) {
        this.getMyLeaveBalanceUseCase = Objects.requireNonNull(getMyLeaveBalanceUseCase, "getMyLeaveBalanceUseCase must not be null");
        this.getEmployeeLeaveBalanceUseCase = Objects.requireNonNull(getEmployeeLeaveBalanceUseCase, "getEmployeeLeaveBalanceUseCase must not be null");
    }

    /**
     * NCL-05-CN-005 TC-01: Xem số ngày phép còn lại của chính mình.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('LEAVE_BALANCE_READ')")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> getMyLeaveBalance(
            @RequestParam(name = "year", required = false) Integer year
    ) {
        LeaveBalanceResult result = getMyLeaveBalanceUseCase.getMyLeaveBalance(year);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin quỹ ngày phép thành công", LeaveBalanceResponse.fromResult(result)));
    }

    /**
     * NCL-05-CN-005 TC-03: Xem số ngày phép của nhân viên theo ID.
     * Nhân viên thường không được xem của người khác (sẽ bị ném PermissionDeniedException và ghi Audit Log).
     */
    @GetMapping("/employees/{employeeId}")
    @PreAuthorize("hasAuthority('LEAVE_BALANCE_READ')")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> getEmployeeLeaveBalance(
            @PathVariable Long employeeId,
            @RequestParam(name = "year", required = false) Integer year
    ) {
        LeaveBalanceResult result = getEmployeeLeaveBalanceUseCase.getEmployeeLeaveBalance(employeeId, year);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin quỹ ngày phép nhân viên thành công", LeaveBalanceResponse.fromResult(result)));
    }
}
