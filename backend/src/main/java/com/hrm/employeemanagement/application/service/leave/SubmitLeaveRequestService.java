package com.hrm.employeemanagement.application.service.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.dto.leave.SubmitLeaveRequestCommand;
import com.hrm.employeemanagement.application.port.inbound.leave.SubmitLeaveRequestUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveBalancePort;
import com.hrm.employeemanagement.application.port.outbound.leave.LoadLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.leave.SaveLeaveRequestPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.leave.DuplicateLeaveRequestException;
import com.hrm.employeemanagement.domain.leave.LeaveBalance;
import com.hrm.employeemanagement.domain.leave.LeaveBalancePolicy;
import com.hrm.employeemanagement.domain.leave.LeaveRequest;
import com.hrm.employeemanagement.domain.leave.LeaveRequestPolicy;
import com.hrm.employeemanagement.domain.leave.LeaveStatus;
import com.hrm.employeemanagement.domain.leave.LeaveType;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SubmitLeaveRequestService implements SubmitLeaveRequestUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadLeaveRequestPort loadLeaveRequestPort;
    private final SaveLeaveRequestPort saveLeaveRequestPort;
    private final SaveAuditLogInNewTransactionPort auditLogRepository;
    private final AuthorizationService authorizationService;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadLeaveBalancePort loadLeaveBalancePort;

    public SubmitLeaveRequestService(
            LoadEmployeePort loadEmployeePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            AuthorizationService authorizationService,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadLeaveBalancePort loadLeaveBalancePort
    ) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadLeaveRequestPort = Objects.requireNonNull(loadLeaveRequestPort, "loadLeaveRequestPort must not be null");
        this.saveLeaveRequestPort = Objects.requireNonNull(saveLeaveRequestPort, "saveLeaveRequestPort must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
        this.loadHolidaysPort = loadHolidaysPort;
        this.loadLeaveBalancePort = loadLeaveBalancePort;
    }

    public SubmitLeaveRequestService(
            LoadEmployeePort loadEmployeePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            AuthorizationService authorizationService,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadHolidaysPort loadHolidaysPort
    ) {
        this(loadEmployeePort, loadLeaveRequestPort, saveLeaveRequestPort, auditLogRepository, authorizationService, loadWorkingCalendarPort, loadHolidaysPort, null);
    }

    public SubmitLeaveRequestService(
            LoadEmployeePort loadEmployeePort,
            LoadLeaveRequestPort loadLeaveRequestPort,
            SaveLeaveRequestPort saveLeaveRequestPort,
            SaveAuditLogInNewTransactionPort auditLogRepository,
            AuthorizationService authorizationService
    ) {
        this(loadEmployeePort, loadLeaveRequestPort, saveLeaveRequestPort, auditLogRepository, authorizationService, null, null, null);
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

        // 5. TC-01: Tính số ngày làm việc và số giờ bị trừ tương ứng dựa theo Lịch làm việc & Ngày lễ
        CompanyWorkingCalendar calendar = (loadWorkingCalendarPort != null)
                ? loadWorkingCalendarPort.loadCompanyCalendar()
                : null;
        Set<LocalDate> holidayDates = (loadHolidaysPort != null)
                ? new HashSet<>(loadHolidaysPort.getHolidayDatesBetween(command.startDate(), command.endDate()))
                : Collections.emptySet();

        int workingDays = LeaveRequestPolicy.calculateWorkingDays(
                command.startDate(),
                command.endDate(),
                calendar,
                holidayDates
        );
        BigDecimal hoursDeducted = LeaveRequestPolicy.calculateHoursDeducted(
                workingDays,
                currentEmployee.getStandardHoursPerWeek()
        );

        // 5.1. TC-02 (NCL-05-CN-005): Nếu là nghỉ phép năm (ANNUAL), kiểm tra không được vượt quá số ngày phép còn lại cho từng năm tương ứng
        if (command.leaveType() == LeaveType.ANNUAL && loadLeaveBalancePort != null) {
            int startYear = command.startDate().getYear();
            int endYear = command.endDate().getYear();

            for (int y = startYear; y <= endYear; y++) {
                final int targetYear = y;
                int daysInTargetYear = LeaveBalancePolicy.calculateWorkingDaysInYear(
                        command.startDate(), command.endDate(), targetYear, calendar, holidayDates);

                if (daysInTargetYear > 0) {
                    LeaveBalance balance = loadLeaveBalancePort.findOrCreateDefault(targetEmployeeId, targetYear);

                    List<LeaveRequest> yearRequests = loadLeaveRequestPort.findByEmployeeIdAndYear(targetEmployeeId, targetYear);
                    BigDecimal currentUsed = yearRequests.stream()
                            .filter(r -> r.getLeaveType() == LeaveType.ANNUAL && r.getStatus() == LeaveStatus.APPROVED)
                            .map(r -> BigDecimal.valueOf(LeaveBalancePolicy.calculateWorkingDaysInYear(
                                    r.getStartDate(), r.getEndDate(), targetYear, calendar, holidayDates)))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal currentPending = yearRequests.stream()
                            .filter(r -> r.getLeaveType() == LeaveType.ANNUAL && r.getStatus() == LeaveStatus.PENDING)
                            .map(r -> BigDecimal.valueOf(LeaveBalancePolicy.calculateWorkingDaysInYear(
                                    r.getStartDate(), r.getEndDate(), targetYear, calendar, holidayDates)))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal remainingDays = LeaveBalancePolicy.calculateRemainingDays(
                            balance.getEntitledDays(),
                            balance.getCarriedOverDays(),
                            currentUsed,
                            currentPending
                    );

                    LeaveBalancePolicy.validateSufficientBalance(remainingDays, BigDecimal.valueOf(daysInTargetYear));
                }
            }
        }

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
