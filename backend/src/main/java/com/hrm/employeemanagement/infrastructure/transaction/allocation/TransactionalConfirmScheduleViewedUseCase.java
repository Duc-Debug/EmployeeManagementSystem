package com.hrm.employeemanagement.infrastructure.transaction.allocation;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.ConfirmScheduleViewedResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.ConfirmScheduleViewedUseCase;

/**
 * Transaction decorator bọc quanh ConfirmScheduleViewedUseCase.
 * Quản lý ranh giới Transaction ghi tại Infrastructure layer theo kiến trúc Hexagonal.
 */
public class TransactionalConfirmScheduleViewedUseCase implements ConfirmScheduleViewedUseCase {

    private final ConfirmScheduleViewedUseCase pureDelegate;

    public TransactionalConfirmScheduleViewedUseCase(ConfirmScheduleViewedUseCase pureDelegate) {
        this.pureDelegate = Objects.requireNonNull(pureDelegate, "ConfirmScheduleViewedUseCase pureDelegate must not be null");
    }

    @Override
    @Transactional
    public ConfirmScheduleViewedResult confirmScheduleViewed(LocalDate weekStart, String ipAddress) {
        return pureDelegate.confirmScheduleViewed(weekStart, ipAddress);
    }
}