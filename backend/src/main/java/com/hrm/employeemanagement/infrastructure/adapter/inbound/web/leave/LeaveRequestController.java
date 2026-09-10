package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.dto.leave.SubmitLeaveRequestCommand;
import com.hrm.employeemanagement.application.port.inbound.leave.SubmitLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto.SubmitLeaveRequestWebRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController {

    private final SubmitLeaveRequestUseCase submitLeaveRequestUseCase;
    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final LoadEmployeePort loadEmployeePort;
    private final AuthorizationService authorizationService;

    public LeaveRequestController(
            SubmitLeaveRequestUseCase submitLeaveRequestUseCase,
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            AuthorizationService authorizationService
    ) {
        this.submitLeaveRequestUseCase = Objects.requireNonNull(submitLeaveRequestUseCase, "submitLeaveRequestUseCase must not be null");
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    /**
     * NCL-05-CN-002: Gửi đơn xin nghỉ phép mới (TC-01).
     * TC-04: Giới hạn nghiêm ngặt chỉ vai trò VT-04 (Nhân viên chuyên môn) mới được nộp đơn.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('VT-04', 'ROLE_VT-04', 'LEAVE_REQUEST_CREATE')")
    public ResponseEntity<ApiResponse<LeaveRequestResult>> submitLeaveRequest(
            @Valid @RequestBody SubmitLeaveRequestWebRequest request
    ) {
        SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                null, // employeeId được tự động lấy theo User đăng nhập trong Service
                request.getLeaveType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getReason()
        );

        LeaveRequestResult result = submitLeaveRequestUseCase.submitLeaveRequest(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi đơn nghỉ phép thành công. Đơn đang chờ quản lý phê duyệt.", result));
    }

    /**
     * Lấy danh sách toàn bộ đơn nghỉ phép của nhân viên đang đăng nhập.
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('VT-04', 'ROLE_VT-04', 'LEAVE_REQUEST_CREATE')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResult>>> getMyLeaveRequests() {
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE);
        Employee employee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Tài khoản chưa được liên kết với hồ sơ nhân sự"));

        List<LeaveRequestResult> results = loadLeaveRequestPort.findByEmployeeId(employee.getIdValue())
                .stream()
                .map(LeaveRequestResult::fromDomain)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn nghỉ phép thành công", results));
    }
}
