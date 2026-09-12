package com.hrm.employeemanagement.infrastructure.transaction.reservation;

import com.hrm.employeemanagement.application.dto.reservation.CancelReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.CreateReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.ResourceReservationResult;
import com.hrm.employeemanagement.application.port.inbound.reservation.AutoProcessProjectReservationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.CancelResourceReservationUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.CreateResourceReservationUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.GetResourceReservationsUseCase;
import com.hrm.employeemanagement.application.service.reservation.ResourceReservationService;
import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

public class TransactionalResourceReservationServiceDecorator implements
        CreateResourceReservationUseCase,
        CancelResourceReservationUseCase,
        GetResourceReservationsUseCase,
        AutoProcessProjectReservationsUseCase {

    private final ResourceReservationService delegate;

    public TransactionalResourceReservationServiceDecorator(ResourceReservationService delegate) {
        this.delegate = Objects.requireNonNull(delegate, "ResourceReservationService delegate must not be null");
    }

    @Override
    @Transactional
    public ResourceReservationResult createReservation(CreateReservationCommand command) {
        return delegate.createReservation(command);
    }

    @Override
    @Transactional
    public ResourceReservationResult cancelReservation(CancelReservationCommand command) {
        return delegate.cancelReservation(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceReservationResult> getReservations(
            Long projectId, Long employeeId, Integer year, Integer weekNumber, ReservationStatus status
    ) {
        return delegate.getReservations(projectId, employeeId, year, weekNumber, status);
    }

    @Override
    @Transactional
    public int autoCancelForProject(Long projectId, String cancelReason) {
        return delegate.autoCancelForProject(projectId, cancelReason);
    }

    @Override
    @Transactional
    public int autoConvertForProject(Long projectId) {
        return delegate.autoConvertForProject(projectId);
    }
}
