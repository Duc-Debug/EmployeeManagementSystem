package com.hrm.employeemanagement.infrastructure.transaction.allocation.threshold;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdHistoryResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.ConfigureCapacityThresholdCommand;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.ConfigureCapacityThresholdUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.GetCapacityThresholdHistoryUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.threshold.GetCapacityThresholdUseCase;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;

/**
 * Transactional Decorator bọc Spring @Transactional xung quanh CapacityThresholdService thuần Java.
 * Tuân thủ nghiêm ngặt Hexagonal Architecture (ArchUnit).
 */
public class TransactionalCapacityThresholdServiceDecorator implements
        ConfigureCapacityThresholdUseCase,
        GetCapacityThresholdUseCase,
        GetCapacityThresholdHistoryUseCase {

    private final ConfigureCapacityThresholdUseCase configureDelegate;
    private final GetCapacityThresholdUseCase getDelegate;
    private final GetCapacityThresholdHistoryUseCase historyDelegate;

    public TransactionalCapacityThresholdServiceDecorator(
            ConfigureCapacityThresholdUseCase configureDelegate,
            GetCapacityThresholdUseCase getDelegate,
            GetCapacityThresholdHistoryUseCase historyDelegate
    ) {
        this.configureDelegate = Objects.requireNonNull(configureDelegate, "configureDelegate must not be null");
        this.getDelegate = Objects.requireNonNull(getDelegate, "getDelegate must not be null");
        this.historyDelegate = Objects.requireNonNull(historyDelegate, "historyDelegate must not be null");
    }

    @Override
    @Transactional
    public CapacityThresholdResult configureThreshold(ConfigureCapacityThresholdCommand command) {
        return configureDelegate.configureThreshold(command);
    }

    @Override
    @Transactional(readOnly = true)
    public CapacityThresholdResult getEffectiveThreshold(CapacityThresholdScope scopeType, Long orgUnitId) {
        return getDelegate.getEffectiveThreshold(scopeType, orgUnitId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CapacityThresholdHistoryResult> getHistory(CapacityThresholdScope scopeType, Long orgUnitId) {
        return historyDelegate.getHistory(scopeType, orgUnitId);
    }
}
