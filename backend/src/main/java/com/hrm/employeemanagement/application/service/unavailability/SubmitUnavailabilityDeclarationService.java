package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.SubmitUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.inbound.unavailability.SubmitUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.SaveUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityPolicy;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * NCL-13-CN-003: Xử lý khai báo thời gian không sẵn sàng (TC-01, TC-03, TC-04).
 */
public class SubmitUnavailabilityDeclarationService implements SubmitUnavailabilityDeclarationUseCase {

    private final LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private final SaveUnavailabilityDeclarationPort saveUnavailabilityPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;

    public SubmitUnavailabilityDeclarationService(
            LoadUnavailabilityDeclarationPort loadUnavailabilityPort,
            SaveUnavailabilityDeclarationPort saveUnavailabilityPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort
    ) {
        this.loadUnavailabilityPort = Objects.requireNonNull(loadUnavailabilityPort, "loadUnavailabilityPort must not be null");
        this.saveUnavailabilityPort = Objects.requireNonNull(saveUnavailabilityPort, "saveUnavailabilityPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "saveAuditLogPort must not be null");
        this.loadWorkingCalendarPort = loadWorkingCalendarPort;
    }

    @Override
    public UnavailabilityDeclarationResult submit(SubmitUnavailabilityCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.UNAVAILABILITY_DECLARE);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        Employee employee = loadEmployeePort.findById(new EmployeeId(command.employeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + command.employeeId()));

        // TC-03: Kiểm tra chính chủ tài khoản - Người dùng không được khai báo thời gian không sẵn sàng của người khác
        boolean isSelf = currentUser.getIdValue() != null && currentUser.getIdValue().equals(employee.getUserIdValue());
        if (!isSelf) {
            saveAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "ACCESS_DENIED_UNAVAILABILITY_DECLARE",
                    "unavailability_declarations",
                    null,
                    null,
                    "user_id=" + currentUserId + "; target_employee_id=" + command.employeeId() + "; role=" + (currentUser.getRole() != null ? currentUser.getRole().getCode().getCode() : "UNKNOWN")
            ));
            throw new PermissionDeniedException(PermissionCode.UNAVAILABILITY_DECLARE);
        }

        UnavailabilityPolicy.validatePeriod(command.startDate(), command.endDate());

        // Kiểm tra không cho phép khai báo thời gian trong quá khứ
        if (command.startDate().isBefore(LocalDate.now())) {
            throw new com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException(
                    "Ngày bắt đầu không được trong quá khứ");
        }

        // Kiểm tra trùng lặp với khai báo PENDING hoặc APPROVED khác của nhân viên
        List<UnavailabilityDeclaration> overlapping = loadUnavailabilityPort.findActiveOverlapping(
                employee.getIdValue(), command.startDate(), command.endDate());
        if (!overlapping.isEmpty()) {
            throw new com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityConflictException(
                    "Khoảng thời gian khai báo bị trùng lặp với một khai báo khác đang chờ duyệt hoặc đã được phê duyệt");
        }

        Set<DayOfWeek> workingDays = resolveWorkingDays();
        BigDecimal totalHours = UnavailabilityPolicy.calculateTotalHours(command.startDate(), command.endDate(), workingDays);

        if (totalHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException(
                    "Khoảng thời gian khai báo không chứa ngày làm việc nào");
        }

        UnavailabilityDeclaration declaration = UnavailabilityDeclaration.create(
                employee.getIdValue(),
                command.startDate(),
                command.endDate(),
                command.reasonType(),
                command.reasonDetail(),
                totalHours
        );

        UnavailabilityDeclaration saved = saveUnavailabilityPort.save(declaration);

        // TC-04: Lưu lịch sử thao tác vào nhật ký kiểm toán
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "SUBMIT_UNAVAILABILITY",
                "unavailability_declarations",
                saved.getId(),
                null,
                String.format("Khai báo thời gian không sẵn sàng từ %s đến %s. Lý do: %s (%s). Số giờ khấu trừ: %s",
                        saved.getStartDate(),
                        saved.getEndDate(),
                        saved.getReasonType(),
                        saved.getReasonDetail() != null ? saved.getReasonDetail() : "",
                        saved.getTotalHoursDeducted())
        ));

        return UnavailabilityDeclarationResult.fromDomain(saved);
    }

    private Set<DayOfWeek> resolveWorkingDays() {
        if (loadWorkingCalendarPort != null) {
            com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();
            if (calendar != null && calendar.getWorkingDays() != null && !calendar.getWorkingDays().isEmpty()) {
                return calendar.getWorkingDays();
            }
        }
        return UnavailabilityPolicy.DEFAULT_WORKING_DAYS;
    }
}
