package com.hrm.employeemanagement.domain.orgunit;

import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.exception.orgunit.InvalidOrgUnitManagerException;
import com.hrm.employeemanagement.domain.exception.orgunit.InvalidTreePathException;
import com.hrm.employeemanagement.domain.exception.orgunit.RequiredFieldMissingException;

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
        if (parentId != null && (managerId == null || managerId <= 0)) {
            throw new InvalidOrgUnitManagerException("Người quản lý (managerId) không được để trống và phải lớn hơn 0");
        }
        if (managerId != null && managerId <= 0) {
            throw new InvalidOrgUnitManagerException("Người quản lý (managerId) không được để trống và phải lớn hơn 0");
        }
        this.id = id;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.unitType = unitType;
        this.parentId = parentId;
        this.treePath = normalizeTreePath(treePath);
        this.level = level;
        this.status = status != null ? status : OrgUnitStatus.ACTIVE;
        this.description = description;
        this.managerId = managerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Standardize treePath to start and end with trailing slash '/'
    private String normalizeTreePath(String path) {
        if (path == null || path.isBlank()) {
            throw new InvalidTreePathException("Đường dẫn cây không được rỗng hoặc trống.");
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    // Hành vi nghiệp vụ: Cập nhật thông tin đơn vị
    public void updateInfo(String unitName, OrgUnitType unitType, Long managerId, String description) {
        if (unitName == null || unitName.trim().isEmpty()) {
            throw RequiredFieldMissingException.of("Tên đơn vị (unitName)");
        }
        if (unitType == null) {
            throw RequiredFieldMissingException.of("Loại đơn vị (unitType)");
        }
        this.assignManager(managerId);
        this.unitName = unitName;
        this.unitType = unitType;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    // Hành vi nghiệp vụ: Di chuyển sang nút cha mới (Re-parenting)
    public void changeParent(OrgUnitId newParentId, String newTreePath, Integer newLevel) {
        if (newLevel == null) {
            throw RequiredFieldMissingException.of("Cấp độ (level)");
        }
        if (newLevel < 1) {
            throw new IllegalArgumentException("Mức độ phải dương và lớn hơn 0.");
        }

        // Ghi chú: newParentId CÓ THỂ null nếu đơn vị là nút Gốc (Root Node)
        this.parentId = newParentId;
        this.treePath = normalizeTreePath(newTreePath);
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
        if (managerId == null || managerId <= 0) {
            throw new InvalidOrgUnitManagerException("Người quản lý (managerId) không được để trống và phải lớn hơn 0");
        }
        this.managerId = managerId;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
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
