package com.hrm.employeemanagement.infrastructure.transaction.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.ApproveUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.RejectUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.SubmitUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityConflictCheckResult;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.inbound.unavailability.ApproveUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.CancelUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.CheckUnavailabilityConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.GetDepartmentUnavailabilityDeclarationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.GetMyUnavailabilityDeclarationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.RejectUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.port.inbound.unavailability.SubmitUnavailabilityDeclarationUseCase;
import com.hrm.employeemanagement.application.service.unavailability.ApproveUnavailabilityDeclarationService;
import com.hrm.employeemanagement.application.service.unavailability.CancelUnavailabilityDeclarationService;
import com.hrm.employeemanagement.application.service.unavailability.CheckUnavailabilityConflictService;
import com.hrm.employeemanagement.application.service.unavailability.GetUnavailabilityDeclarationsService;
import com.hrm.employeemanagement.application.service.unavailability.RejectUnavailabilityDeclarationService;
import com.hrm.employeemanagement.application.service.unavailability.SubmitUnavailabilityDeclarationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Decorator quản lý Transaction ranh giới cho các Use Case của NCL-13-CN-003 theo kiến trúc Hexagonal.
 */
public class TransactionalUnavailabilityServiceDecorator implements
        SubmitUnavailabilityDeclarationUseCase,
        ApproveUnavailabilityDeclarationUseCase,
        RejectUnavailabilityDeclarationUseCase,
        CancelUnavailabilityDeclarationUseCase,
        GetMyUnavailabilityDeclarationsUseCase,
        GetDepartmentUnavailabilityDeclarationsUseCase,
        CheckUnavailabilityConflictUseCase {

    private final SubmitUnavailabilityDeclarationService submitService;
    private final ApproveUnavailabilityDeclarationService approveService;
    private final RejectUnavailabilityDeclarationService rejectService;
    private final CancelUnavailabilityDeclarationService cancelService;
    private final GetUnavailabilityDeclarationsService getService;
    private final CheckUnavailabilityConflictService checkService;

    public TransactionalUnavailabilityServiceDecorator(
            SubmitUnavailabilityDeclarationService submitService,
            ApproveUnavailabilityDeclarationService approveService,
            RejectUnavailabilityDeclarationService rejectService,
            CancelUnavailabilityDeclarationService cancelService,
            GetUnavailabilityDeclarationsService getService,
            CheckUnavailabilityConflictService checkService
    ) {
        this.submitService = Objects.requireNonNull(submitService, "submitService must not be null");
        this.approveService = Objects.requireNonNull(approveService, "approveService must not be null");
        this.rejectService = Objects.requireNonNull(rejectService, "rejectService must not be null");
        this.cancelService = Objects.requireNonNull(cancelService, "cancelService must not be null");
        this.getService = Objects.requireNonNull(getService, "getService must not be null");
        this.checkService = Objects.requireNonNull(checkService, "checkService must not be null");
    }

    @Override
    @Transactional
    public UnavailabilityDeclarationResult submit(SubmitUnavailabilityCommand command) {
        return submitService.submit(command);
    }

    @Override
    @Transactional
    public UnavailabilityDeclarationResult approve(ApproveUnavailabilityCommand command) {
        return approveService.approve(command);
    }

    @Override
    @Transactional
    public UnavailabilityDeclarationResult reject(RejectUnavailabilityCommand command) {
        return rejectService.reject(command);
    }

    @Override
    @Transactional
    public UnavailabilityDeclarationResult cancel(Long declarationId) {
        return cancelService.cancel(declarationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnavailabilityDeclarationResult> getMyDeclarations() {
        return getService.getMyDeclarations();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnavailabilityDeclarationResult> getPendingDeclarations(Long requestedOrgUnitId) {
        return getService.getPendingDeclarations(requestedOrgUnitId);
    }

    @Override
    @Transactional(readOnly = true)
    public UnavailabilityConflictCheckResult checkConflict(Long declarationId) {
        return checkService.checkConflict(declarationId);
    }
}
