package com.hrm.employeemanagement.domain.orgunit;

/**
 * OrgUnit entity mô tả cây tổ 
 */

import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;

public class OrgUnit {
    private OrgUnitId id;
    private String unitCode;
    private String unitName;
    private OrgUnitType unitType;
    private OrgUnitId parentId;
    private String treePath;
    private Integer level;
    private OrgUnitStatus status;
    private String description;
    private Long managerId; // Bổ nhiệm người quản lý (Nullable)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor đầy đủ
    public OrgUnit(OrgUnitId id, String unitCode, String unitName, OrgUnitType unitType,
            OrgUnitId parentId, String treePath, Integer level, OrgUnitStatus status,
            String description, Long managerId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.unitType = unitType;
        this.parentId = parentId;
        this.treePath = treePath;
        this.level = level;
        this.status = status != null ? status : OrgUnitStatus.ACTIVE;
        this.description = description;
        this.managerId = managerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Hành vi nghiệp vụ: Cập nhật thông tin đơn vị
    public void updateInfo(String unitName, OrgUnitType unitType, String description) {
        if (unitName == null || unitName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đơn vị không được để trống");
        }
        this.unitName = unitName;
        this.unitType = unitType;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    // Hành vi nghiệp vụ: Di chuyển sang nút cha mới (Re-parenting)
    public void changeParent(OrgUnitId newParentId, String newTreePath, Integer newLevel) {
        if (newTreePath == null || newTreePath.isBlank()) {
            throw new OrgUnitNotFoundException("Tree path cannot be blank");
        }
        if (newLevel == null || newLevel < 1) {
            throw new OrgUnitNotFoundException("Level must be positive and greater than 0");
        }

        // Ghi chú: newParentId CÓ THỂ null nếu đơn vị là nút Gốc (Root Node)
        this.parentId = newParentId;
        this.treePath = newTreePath;
        this.level = newLevel;
        this.updatedAt = LocalDateTime.now();
    }

    // Hành vi nghiệp vụ: Vô hiệu hóa / Khóa đơn vị (Soft Delete)
    public void deactivate() {
        this.status = OrgUnitStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    // Hành vi nghiệp vụ: Bổ nhiệm Trưởng phòng/Quản lý
    public void assignManager(Long managerId) {
        this.managerId = managerId;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters (Không dùng Setter tự do để bảo vệ trạng thái)
    public OrgUnitId getId() {
        return id;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public OrgUnitType getUnitType() {
        return unitType;
    }

    public OrgUnitId getParentId() {
        return parentId;
    }

    public String getTreePath() {
        return treePath;
    }

    public Integer getLevel() {
        return level;
    }

    public OrgUnitStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public Long getManagerId() {
        return managerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
