package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "resource_scenarios")
public class ResourceScenarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "org_unit_id", nullable = false)
    private Long orgUnitId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "from_year", nullable = false)
    private Integer fromYear;

    @Column(name = "from_week", nullable = false)
    private Integer fromWeek;

    @Column(name = "duration_weeks", nullable = false)
    private Integer durationWeeks;

    @Column(name = "base_snapshot_at", nullable = false)
    private LocalDateTime baseSnapshotAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public ResourceScenarioJpaEntity() {}

    public ResourceScenarioJpaEntity(
            Long id,
            String code,
            String name,
            String description,
            Long orgUnitId,
            String status,
            Integer fromYear,
            Integer fromWeek,
            Integer durationWeeks,
            LocalDateTime baseSnapshotAt,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.orgUnitId = orgUnitId;
        this.status = status;
        this.fromYear = fromYear;
        this.fromWeek = fromWeek;
        this.durationWeeks = durationWeeks;
        this.baseSnapshotAt = baseSnapshotAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getFromYear() { return fromYear; }
    public void setFromYear(Integer fromYear) { this.fromYear = fromYear; }
    public Integer getFromWeek() { return fromWeek; }
    public void setFromWeek(Integer fromWeek) { this.fromWeek = fromWeek; }
    public Integer getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(Integer durationWeeks) { this.durationWeeks = durationWeeks; }
    public LocalDateTime getBaseSnapshotAt() { return baseSnapshotAt; }
    public void setBaseSnapshotAt(LocalDateTime baseSnapshotAt) { this.baseSnapshotAt = baseSnapshotAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
