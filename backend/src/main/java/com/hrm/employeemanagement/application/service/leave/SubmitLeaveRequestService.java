package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.dto.leave.SubmitLeaveRequestCommand;
import com.hrm.employeemanagement.application.port.inbound.leave.SubmitLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.leave.DuplicateLeaveRequestException;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.leave.LeaveRequestPolicy;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.util.Objects;

public class SubmitLeaveRequestService implements SubmitLeaveRequestUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final SaveLeaveRequestPort saveLeaveRequestPort;
    private final SaveAuditLogInNewTransactionPort auditLogRepository;
    private final AuthorizationService authorizationService;

    public SubmitLeaveRequestService(
            LoadEmployeePort loadEmployeePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            AuthorizationService authorizationService
    ) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.saveLeaveRequestPort = Objects.requireNonNull(saveLeaveRequestPort, "saveLeaveRequestPort must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
    }

    @Override
    public LeaveRequestResult submitLeaveRequest(SubmitLeaveRequestCommand command) {
        // 1. TC-04: Kiểm tra quyền LEAVE_REQUEST_CREATE của người dùng hiện tại
        Long currentUserId = authorizationService.require(PermissionCode.LEAVE_REQUEST_CREATE);

        // 2. Xác thực nhân sự gắn với tài khoản đang đăng nhập
        Employee currentEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Tài khoản chưa được liên kết với hồ sơ nhân sự"));

        // Đảm bảo chỉ nộp đơn cho chính mình nếu command có truyền employeeId
        if (command.employeeId() != null && !currentEmployee.getIdValue().equals(command.employeeId())) {
            throw new PermissionDeniedException(PermissionCode.LEAVE_REQUEST_CREATE);
        }

        Long targetEmployeeId = currentEmployee.getIdValue();

        // Khóa pessimistic lock trên Employee để serialize các request gửi đơn cùng lúc của cùng nhân sự
        loadEmployeePort.findByIdForUpdate(new EmployeeId(targetEmployeeId));

        // 3. TC-03: Kiểm tra tính hợp lệ của khoảng ngày
        LeaveRequestPolicy.validateDateRange(command.startDate(), command.endDate());

        // 4. TC-02: Kiểm tra dữ liệu trùng lặp (Overlapping Leave)
        boolean hasOverlap = loadLeaveRequestPort.existsOverlappingLeave(
                targetEmployeeId,
                command.startDate(),
                command.endDate()
        );
        if (hasOverlap) {
            throw DuplicateLeaveRequestException.overlapping();
        }

        // 5. TC-01: Tính số ngày làm việc và số giờ bị trừ tương ứng
        int workingDays = LeaveRequestPolicy.calculateWorkingDays(command.startDate(), command.endDate());
        BigDecimal hoursDeducted = LeaveRequestPolicy.calculateHoursDeducted(
                workingDays,
                currentEmployee.getStandardHoursPerWeek()
        );

        // 6. TC-01: Khởi tạo đơn nghỉ phép ở trạng thái PENDING
        LeaveRequest leaveRequest = LeaveRequest.createPending(
                targetEmployeeId,
                command.leaveType(),
                command.startDate(),
                command.endDate(),
                workingDays,
                hoursDeducted,
                command.reason()
        );

        LeaveRequest saved = saveLeaveRequestPort.save(leaveRequest);

        // 7. TC-05: Lưu nhật ký kiểm toán vào audit_logs
        String auditDetails = String.format(
                "Loại nghỉ: %s, Từ: %s Đến: %s, Số ngày: %d, Số giờ: %s",
                saved.getLeaveType(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getDaysCount(),
                saved.getHoursDeducted()
        );

        auditLogRepository.save(AuditLog.createChange(
                currentUserId,
                "CREATE_LEAVE_REQUEST",
                "leave_requests",
                saved.getId(),
                null,
                auditDetails
        ));

        return LeaveRequestResult.fromDomain(saved);
    }
}
