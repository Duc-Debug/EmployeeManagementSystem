package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveImpactResult;
import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.dto.leave.SubmitLeaveRequestCommand;
import com.hrm.employeemanagement.application.port.inbound.leave.*;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto.ApproveLeaveWebRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto.RejectLeaveWebRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto.SubmitLeaveRequestWebRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController {

    private final SubmitLeaveRequestUseCase submitLeaveRequestUseCase;
    private final CancelLeaveRequestUseCase cancelLeaveRequestUseCase;
    private final ApproveLeaveRequestUseCase approveLeaveRequestUseCase;
    private final RejectLeaveRequestUseCase rejectLeaveRequestUseCase;
    private final GetLeaveImpactUseCase getLeaveImpactUseCase;
    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final LoadEmployeePort loadEmployeePort;
    private final AuthorizationService authorizationService;

    public LeaveRequestController(
            SubmitLeaveRequestUseCase submitLeaveRequestUseCase,
            CancelLeaveRequestUseCase cancelLeaveRequestUseCase,
            ApproveLeaveRequestUseCase approveLeaveRequestUseCase,
            RejectLeaveRequestUseCase rejectLeaveRequestUseCase,
            GetLeaveImpactUseCase getLeaveImpactUseCase,
            LoadLeaveRequestPort loadLeaveRequestPort,
            LoadEmployeePort loadEmployeePort,
            AuthorizationService authorizationService
    ) {
        this.submitLeaveRequestUseCase = Objects.requireNonNull(submitLeaveRequestUseCase, "submitLeaveRequestUseCase must not be null");
        this.cancelLeaveRequestUseCase = Objects.requireNonNull(cancelLeaveRequestUseCase, "cancelLeaveRequestUseCase must not be null");
        this.approveLeaveRequestUseCase = Objects.requireNonNull(approveLeaveRequestUseCase, "approveLeaveRequestUseCase must not be null");
        this.rejectLeaveRequestUseCase = Objects.requireNonNull(rejectLeaveRequestUseCase, "rejectLeaveRequestUseCase must not be null");
        this.getLeaveImpactUseCase = Objects.requireNonNull(getLeaveImpactUseCase, "getLeaveImpactUseCase must not be null");
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    /**
     * NCL-05-CN-002: Gửi đơn xin nghỉ phép mới (TC-01).
     */
    @PostMapping
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_CREATE')")
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
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_CREATE')")
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

    /**
     * Hủy đơn xin nghỉ phép cá nhân (chỉ khi đang ở trạng thái PENDING).
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_CREATE')")
    public ResponseEntity<ApiResponse<Void>> cancelLeaveRequest(@PathVariable Long id) {
        cancelLeaveRequestUseCase.cancelLeaveRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn xin nghỉ phép thành công", null));
    }

    /**
     * NCL-05-CN-003: Lấy danh sách đơn xin nghỉ phép đang chờ duyệt (dành cho RM / HR).
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_APPROVE')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResult>>> getPendingLeaveRequests() {
        List<LeaveRequestResult> results = loadLeaveRequestPort.findPendingRequests()
                .stream()
                .map(LeaveRequestResult::fromDomain)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn chờ duyệt thành công", results));
    }

    /**
     * NCL-05-CN-003: TC-02 - Đánh giá ảnh hưởng của việc nghỉ phép tới các dự án hiện có.
     */
    @GetMapping("/{id}/impact")
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_APPROVE')")
    public ResponseEntity<ApiResponse<LeaveImpactResult>> getLeaveImpact(@PathVariable Long id) {
        LeaveImpactResult result = getLeaveImpactUseCase.getLeaveImpact(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tác động dự án thành công", result));
    }

    /**
     * NCL-05-CN-003: TC-01 & QTN-10 - Phê duyệt đơn xin nghỉ phép.
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_APPROVE')")
    public ResponseEntity<ApiResponse<LeaveRequestResult>> approveLeaveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveLeaveWebRequest request
    ) {
        String comment = request != null ? request.getComment() : null;
        LeaveRequestResult result = approveLeaveRequestUseCase.approveLeaveRequest(id, comment);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt đơn nghỉ phép thành công", result));
    }

    /**
     * NCL-05-CN-003: TC-04 - Từ chối đơn xin nghỉ phép kèm lý do bắt buộc.
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_APPROVE')")
    public ResponseEntity<ApiResponse<LeaveRequestResult>> rejectLeaveRequest(
            @PathVariable Long id,
            @Valid @RequestBody RejectLeaveWebRequest request
    ) {
        LeaveRequestResult result = rejectLeaveRequestUseCase.rejectLeaveRequest(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối đơn nghỉ phép", result));
    }
}
