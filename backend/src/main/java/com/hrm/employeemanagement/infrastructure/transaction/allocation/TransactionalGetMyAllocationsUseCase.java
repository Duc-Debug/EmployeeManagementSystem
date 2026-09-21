package com.hrm.employeemanagement.infrastructure.transaction.allocation;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.MyWeeklyAllocationsResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetMyAllocationsUseCase;

/**
 * Transaction decorator bọc quanh GetMyAllocationsUseCase.
 * Quản lý ranh giới Transaction readOnly tại Infrastructure layer theo kiến trúc Hexagonal.
 */
public class TransactionalGetMyAllocationsUseCase implements GetMyAllocationsUseCase {

    private final GetMyAllocationsUseCase pureDelegate;

    public TransactionalGetMyAllocationsUseCase(GetMyAllocationsUseCase pureDelegate) {
        this.pureDelegate = Objects.requireNonNull(pureDelegate, "GetMyAllocationsUseCase pureDelegate must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public MyWeeklyAllocationsResult getMyAllocations(LocalDate weekStart, Integer weeks) {
        return pureDelegate.getMyAllocations(weekStart, weeks);
    }
}