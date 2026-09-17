package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "scenario_shares")
public class ScenarioShareJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "shared_with_user_id", nullable = false)
    private Long sharedWithUserId;

    @Column(name = "shared_by_user_id", nullable = false)
    private Long sharedByUserId;

    @Column(name = "access_level", nullable = false, length = 30)
    private String accessLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public ScenarioShareJpaEntity() {}

    public ScenarioShareJpaEntity(
            Long id,
            Long scenarioId,
            Long sharedWithUserId,
            Long sharedByUserId,
            String accessLevel,
            LocalDateTime createdAt,
            LocalDateTime revokedAt,
            Long version
    ) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.sharedWithUserId = sharedWithUserId;
        this.sharedByUserId = sharedByUserId;
        this.accessLevel = accessLevel;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
        this.version = version != null ? version : 0L;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public Long getSharedWithUserId() { return sharedWithUserId; }
    public void setSharedWithUserId(Long sharedWithUserId) { this.sharedWithUserId = sharedWithUserId; }
    public Long getSharedByUserId() { return sharedByUserId; }
    public void setSharedByUserId(Long sharedByUserId) { this.sharedByUserId = sharedByUserId; }
    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
