package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.orgunit.entity;

import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "org_units")
public class OrgUnitJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "unit_code", nullable = false, unique = true, length = 50)
    private String unitCode;
    @Column(name = "unit_name", nullable = false)
    private String unitName;
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 50)
    private OrgUnitType unitType;
    @Column(name = "parent_id")
    private Long parentId;
    @Column(name = "tree_path", nullable = false, length = 500)
    private String treePath;
    @Column(name = "level", nullable = false)
    private Integer level;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrgUnitStatus status;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "manager_id")
    private Long managerId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public OrgUnitJpaEntity() {
    }

    public OrgUnitJpaEntity(Long id, String unitCode, String unitName, OrgUnitType unitType,
            Long parentId, String treePath, Integer level, OrgUnitStatus status,
            String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.unitType = unitType;
        this.parentId = parentId;
        this.treePath = treePath;
        this.level = level;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public OrgUnitType getUnitType() {
        return unitType;
    }

    public void setUnitType(OrgUnitType unitType) {
        this.unitType = unitType;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getTreePath() {
        return treePath;
    }

    public void setTreePath(String treePath) {
        this.treePath = treePath;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public OrgUnitStatus getStatus() {
        return status;
    }

    public void setStatus(OrgUnitStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}