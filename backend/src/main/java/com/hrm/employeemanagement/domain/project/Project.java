package com.hrm.employeemanagement.domain.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDateRangeException;
import com.hrm.employeemanagement.domain.user.UserId;

public class Project {
    private ProjectId id;
    private String projectCode;
    private String projectName;
    private Long orgUnitId;
    private EmployeeId managerId;
    private ProjectStatus status;
    private UserId createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal estimatedHours;
    private String description;
    private Long version;

    public Project(
            ProjectId id,
            String projectCode,
            String projectName,
            Long orgUnitId,
            EmployeeId managerId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal estimatedHours,
            String description,
            ProjectStatus status,
            UserId createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version) {
        validateProjectCode(projectCode);
        validateProjectName(projectName);
        validateOrgUnitId(orgUnitId);
        validateProjectDates(startDate, endDate);
        validateEstimatedHours(estimatedHours);
        validateDescription(description);
        this.id = id;
        this.projectCode = projectCode.trim();
        this.projectName = projectName.trim();
        this.orgUnitId = orgUnitId;
        this.managerId = managerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.estimatedHours = estimatedHours != null ? estimatedHours : BigDecimal.ZERO;
        this.description = description != null ? description.trim() : null;
        this.status = status != null ? status : ProjectStatus.ACTIVE;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Project createNew(
            String projectCode,
            String projectName,
            Long orgUnitId,
            EmployeeId managerId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal estimatedHours,
            String description,
            UserId createdBy) {
                if (createdBy == null) {
            throw new InvalidProjectDataException("Người tạo dự án không được để trống");
        }
        return new Project(
                null, // id = null vì là tạo mới
                projectCode,
                projectName,
                orgUnitId,
                managerId,
                startDate,
                endDate,
                estimatedHours,
                description,
                ProjectStatus.ACTIVE, // Trạng thái mặc định khi tạo mới
                createdBy,
                LocalDateTime.now(),
                null,
                null // version ban đầu
        );
    }

    /**
     * Cập nhật thông tin dự án (Chỉ cho phép khi dự án đang ở trạng thái ACTIVE).
     */
    public void updateInfo(
            String projectName,
            EmployeeId managerId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal estimatedHours,
            String description) {
        if (this.status != ProjectStatus.ACTIVE) {
            throw new InvalidProjectDataException("Chỉ có thể chỉnh sửa thông tin dự án đang ở trạng thái hoạt động");
        }
        validateProjectName(projectName);
        validateProjectDates(startDate, endDate);
        validateEstimatedHours(estimatedHours);
        validateDescription(description);
        this.projectName = projectName.trim();
        this.managerId = managerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.estimatedHours = estimatedHours != null ? estimatedHours : BigDecimal.ZERO;
        this.description = description != null ? description.trim() : null;
        this.updatedAt = LocalDateTime.now();
    }
    /**
     * Kiểm tra PM phụ trách dự án
     */
    public boolean isManagedBy(EmployeeId employeeId) {
        return this.managerId != null && this.managerId.equals(employeeId);
    }

    // ======validation====
    /**
     * Kiểm tra mã dự án: không null, không rỗng và không vượt quá 50 ký tự.
     */
    private void validateProjectCode(String projectCode) {
        if (projectCode == null || projectCode.trim().isEmpty()) {
            throw new InvalidProjectDataException("Mã dự án không được để trống");
        }
        if (projectCode.trim().length() > 50) {
            throw new InvalidProjectDataException("Mã dự án không được vượt quá 50 ký tự");
        }
    }

    /**
     * Kiểm tra tên dự án: không null, không rỗng và không vượt quá 255 ký tự.
     */
    private void validateProjectName(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new InvalidProjectDataException("Tên dự án không được để trống");
        }
        if (projectName.trim().length() > 255) {
            throw new InvalidProjectDataException("Tên dự án không được vượt quá 255 ký tự");
        }
    }

    /**
     * Kiểm tra phòng ban/đơn vị tổ chức: bắt buộc phải có.
     */
    private void validateOrgUnitId(Long orgUnitId) {
        if (orgUnitId == null) {
            throw new InvalidProjectDataException("Đơn vị tổ chức phụ trách dự án không được để trống");
        }
    }

    /**
     * Kiểm tra ngày kết thúc dự kiến không được sớm hơn ngày bắt đầu (TC-02).
     */
    private void validateProjectDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw InvalidProjectDateRangeException.invalidRange();
        }
    }

    private static final BigDecimal MAX_ESTIMATED_HOURS = new BigDecimal("99999999.99");

    /**
     * Kiểm tra tổng giờ dự kiến: không được là số âm, không vượt quá giới hạn DECIMAL(10,2), và tối đa 2 chữ số thập phân.
     */
    private void validateEstimatedHours(BigDecimal hours) {
        if (hours != null) {
            if (hours.compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidProjectDataException("Tổng giờ dự kiến không được nhỏ hơn 0");
            }
            if (hours.compareTo(MAX_ESTIMATED_HOURS) > 0) {
                throw new InvalidProjectDataException("Tổng giờ dự kiến không được vượt quá 99,999,999.99");
            }
            if (hours.compareTo(BigDecimal.ZERO) > 0 && hours.stripTrailingZeros().scale() > 2) {
                throw new InvalidProjectDataException("Tổng giờ dự kiến chỉ được có tối đa 2 chữ số thập phân");
            }
        }
    }

    /**
     * Kiểm tra mô tả dự án: không được vượt quá 2000 ký tự.
     */
    private void validateDescription(String description) {
        if (description != null && description.trim().length() > 2000) {
            throw new InvalidProjectDataException("Mô tả dự án không được vượt quá 2000 ký tự");
        }
    }

    public ProjectId getId() {
        return id;
    }

    public Long getIdValue() {
        return id != null ? id.value() : null;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public EmployeeId getManagerId() {
        return managerId;
    }

    public Long getManagerIdValue() {
        return managerId != null ? managerId.value() : null;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public UserId getCreatedBy() {
        return createdBy;
    }

    public Long getCreatedByValue() {
        return createdBy != null ? createdBy.value() : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }

    public String getDescription() {
        return description;
    }

}
