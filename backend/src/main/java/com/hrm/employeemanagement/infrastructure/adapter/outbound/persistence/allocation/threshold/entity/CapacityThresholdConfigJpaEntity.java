package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.threshold.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "capacity_threshold_configs")
public class CapacityThresholdConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private CapacityThresholdScope scopeType;

    @Column(name = "scope_key", nullable = false, unique = true, length = 100)
    private String scopeKey;

    @Column(name = "org_unit_id")
    private Long orgUnitId;

    @Column(name = "overload_threshold", nullable = false, precision = 5, scale = 1)
    private BigDecimal overloadThreshold;

    @Column(name = "idle_threshold", nullable = false, precision = 5, scale = 1)
    private BigDecimal idleThreshold;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public CapacityThresholdConfigJpaEntity() {
    }

    public CapacityThresholdConfigJpaEntity(
            Long id,
            CapacityThresholdScope scopeType,
            String scopeKey,
            Long orgUnitId,
            BigDecimal overloadThreshold,
            BigDecimal idleThreshold,
            Long createdBy,
            LocalDateTime createdAt,
            Long updatedBy,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        this.scopeType = scopeType;
        this.scopeKey = scopeKey;
        this.orgUnitId = orgUnitId;
        this.overloadThreshold = overloadThreshold;
        this.idleThreshold = idleThreshold;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.version = version;
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

    public void setScopeType(CapacityThresholdScope scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeKey() {
        return scopeKey;
    }

    public void setScopeKey(String scopeKey) {
        this.scopeKey = scopeKey;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public BigDecimal getOverloadThreshold() {
        return overloadThreshold;
    }

    public void setOverloadThreshold(BigDecimal overloadThreshold) {
        this.overloadThreshold = overloadThreshold;
    }

    public BigDecimal getIdleThreshold() {
        return idleThreshold;
    }

    public void setIdleThreshold(BigDecimal idleThreshold) {
        this.idleThreshold = idleThreshold;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
