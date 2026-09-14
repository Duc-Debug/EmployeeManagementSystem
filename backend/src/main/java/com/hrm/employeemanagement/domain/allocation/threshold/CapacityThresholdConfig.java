package com.hrm.employeemanagement.domain.allocation.threshold;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Aggregate biểu diễn bản ghi cấu hình ngưỡng cảnh báo năng lực theo QTN-23.
 * Tuân thủ Pure Java trong kiến trúc Hexagonal / DDD (không chứa Spring/JPA annotations).
 */
public class CapacityThresholdConfig {

    private Long id;
    private final CapacityThresholdScope scopeType;
    private final String scopeKey;
    private final Long orgUnitId;
    private BigDecimal overloadThreshold;
    private BigDecimal idleThreshold;
    /**
     * Concurrency token / snapshot version for optimistic locking.
     * Managed by persistence layer (JPA @Version); represents the snapshot version at which the aggregate was loaded.
     */
    private Long version;
    private final Long createdBy;
    private final LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public CapacityThresholdConfig(
            Long id,
            CapacityThresholdScope scopeType,
            String scopeKey,
            Long orgUnitId,
            BigDecimal overloadThreshold,
            BigDecimal idleThreshold,
            Long version,
            Long createdBy,
            LocalDateTime createdAt,
            Long updatedBy,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.scopeType = Objects.requireNonNull(scopeType, "scopeType không được null");
        this.scopeKey = Objects.requireNonNull(scopeKey, "scopeKey không được null");
        this.orgUnitId = orgUnitId;
        this.overloadThreshold = Objects.requireNonNull(overloadThreshold, "overloadThreshold không được null");
        this.idleThreshold = Objects.requireNonNull(idleThreshold, "idleThreshold không được null");
        this.version = version != null ? version : 0L;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy không được null");
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public static CapacityThresholdConfig createNew(
            CapacityThresholdScope scopeType,
            Long orgUnitId,
            BigDecimal overloadThreshold,
            BigDecimal idleThreshold,
            Long actorUserId
    ) {
        CapacityThresholdPolicy.validateThresholds(overloadThreshold, idleThreshold);
        String scopeKey = CapacityThresholdPolicy.computeScopeKey(scopeType, orgUnitId);

        return new CapacityThresholdConfig(
                null,
                scopeType,
                scopeKey,
                orgUnitId,
                overloadThreshold,
                idleThreshold,
                0L,
                actorUserId,
                LocalDateTime.now(),
                actorUserId,
                LocalDateTime.now()
        );
    }

    public void update(
            BigDecimal newOverloadThreshold,
            BigDecimal newIdleThreshold,
            Long actorUserId
    ) {
        CapacityThresholdPolicy.validateThresholds(newOverloadThreshold, newIdleThreshold);
        this.overloadThreshold = newOverloadThreshold;
        this.idleThreshold = newIdleThreshold;
        this.updatedBy = Objects.requireNonNull(actorUserId, "actorUserId không được null");
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasChanged(BigDecimal newOverload, BigDecimal newIdle) {
        if (newOverload == null || newIdle == null) {
            return false;
        }
        return this.overloadThreshold.compareTo(newOverload) != 0
                || this.idleThreshold.compareTo(newIdle) != 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CapacityThresholdScope getScopeType() {
        return scopeType;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public BigDecimal getOverloadThreshold() {
        return overloadThreshold;
    }

    public BigDecimal getIdleThreshold() {
        return idleThreshold;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
