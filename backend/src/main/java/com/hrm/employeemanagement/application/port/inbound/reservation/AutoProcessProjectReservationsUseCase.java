package com.hrm.employeemanagement.application.port.inbound.reservation;

public interface AutoProcessProjectReservationsUseCase {
    int autoCancelForProject(Long projectId, String cancelReason, Long executedBy);
    int autoConvertForProject(Long projectId, Long executedBy);
}
