package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.RejectUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.inbound.unavailability.RejectUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
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
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityDeclarationNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.util.Objects;

public class RejectUnavailabilityDeclarationService implements RejectUnavailabilityDeclarationUseCase {

    private final LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private final SaveUnavailabilityDeclarationPort saveUnavailabilityPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final UnavailabilityDataScopeValidator dataScopeValidator;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogPort saveAuditLogPort;

    public RejectUnavailabilityDeclarationService(
            LoadUnavailabilityDeclarationPort loadUnavailabilityPort,
            SaveUnavailabilityDeclarationPort saveUnavailabilityPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort
    ) {
        this.loadUnavailabilityPort = Objects.requireNonNull(loadUnavailabilityPort, "loadUnavailabilityPort must not be null");
        this.saveUnavailabilityPort = Objects.requireNonNull(saveUnavailabilityPort, "saveUnavailabilityPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.dataScopeValidator = new UnavailabilityDataScopeValidator(loadOrgUnitPort);
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "saveAuditLogPort must not be null");
    }

    @Override
    public UnavailabilityDeclarationResult reject(RejectUnavailabilityCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

        UnavailabilityDeclaration declaration = loadUnavailabilityPort.findByIdForUpdate(command.declarationId())
                .orElseThrow(() -> new UnavailabilityDeclarationNotFoundException(
                        "Không tìm thấy khai báo thời gian không sẵn sàng với mã: " + command.declarationId()));

        Employee employee = loadEmployeePort.findById(new EmployeeId(declaration.getEmployeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên: " + declaration.getEmployeeId()));

        requireEmployeeInScope(currentUser, employee);

        declaration.reject(currentUserId, command.rejectReason());
        UnavailabilityDeclaration saved = saveUnavailabilityPort.save(declaration);

        // TC-04: Ghi nhật ký kiểm toán
        String auditDesc = String.format("Từ chối khai báo thời gian không sẵn sàng #%d của nhân viên #%d (%s đến %s). Lý do từ chối: %s",
                saved.getId(),
                saved.getEmployeeId(),
                saved.getStartDate(),
                saved.getEndDate(),
                command.rejectReason() != null ? command.rejectReason() : "Không có");

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "REJECT_UNAVAILABILITY",
                "unavailability_declarations",
                saved.getId(),
                null,
                auditDesc
        ));

        return UnavailabilityDeclarationResult.fromDomain(saved);
    }

    private void requireEmployeeInScope(User currentUser, Employee employee) {
        dataScopeValidator.requireEmployeeInScope(currentUser, employee, PermissionCode.UNAVAILABILITY_APPROVE);
    }
}
