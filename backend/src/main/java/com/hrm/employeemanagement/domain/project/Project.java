package com.hrm.employeemanagement.domain.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDateRangeException;
import com.hrm.employeemanagement.domain.exception.project.ProjectAlreadyClosedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotClosedException;
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
    private Integer taskSeqCounter;
    private String closureReason;
    private LocalDateTime closedAt;
    private UserId closedBy;
    private String reopenReason;
    private LocalDateTime reopenedAt;
    private UserId reopenedBy;

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
        this(
                id,
                projectCode,
                projectName,
                orgUnitId,
                managerId,
                startDate,
                endDate,
                estimatedHours,
                description,
                status,
                createdBy,
                createdAt,
                updatedAt,
                version,
                0);
    }

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
            Long version,
            Integer taskSeqCounter,
            String closureReason,
            LocalDateTime closedAt,
            UserId closedBy,
            String reopenReason,
            LocalDateTime reopenedAt,
            UserId reopenedBy) {
        this(
                id,
                projectCode,
                projectName,
                orgUnitId,
                managerId,
                startDate,
                endDate,
                estimatedHours,
                description,
                status,
                createdBy,
                createdAt,
                updatedAt,
                version,
                taskSeqCounter);
        this.closureReason = closureReason != null ? closureReason.trim() : null;
        this.closedAt = closedAt;
        this.closedBy = closedBy;
        this.reopenReason = reopenReason != null ? reopenReason.trim() : null;
        this.reopenedAt = reopenedAt;
        this.reopenedBy = reopenedBy;
    }

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
            Long version,
            Integer taskSeqCounter) {
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
        this.taskSeqCounter = taskSeqCounter != null ? taskSeqCounter : 0;
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
     * Kiểm tra tổng giờ dự kiến: không được là số âm, không vượt quá giới hạn
     * DECIMAL(10,2), và tối đa 2 chữ số thập phân.
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

        /**
     * Đóng dự án khi hoàn thành (hoặc đóng hộ cấp quản trị)
     */
    public void close(UserId closedBy, String closureReason) {
        if (this.status == ProjectStatus.CLOSED) {
            throw new ProjectAlreadyClosedException("Dự án đã ở trạng thái đóng từ trước");
        }
        if (closedBy == null) {
            throw new InvalidProjectDataException("Người thực hiện đóng dự án không được để trống");
        }
        this.status = ProjectStatus.CLOSED;
        this.closedBy = closedBy;
        this.closureReason = closureReason != null && !closureReason.isBlank() ? closureReason.trim() : null;
        this.closedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mở lại dự án đã đóng (Chỉ dành cho Ban giám đốc hoặc Admin có lý do >= 10 ký tự)
     */
    public void reopen(UserId reopenedBy, String reopenReason) {
        if (this.status != ProjectStatus.CLOSED) {
            throw new ProjectNotClosedException("Chỉ có thể mở lại dự án đang ở trạng thái đóng");
        }
        if (reopenedBy == null) {
            throw new InvalidProjectDataException("Người thực hiện mở lại dự án không được để trống");
        }
        if (reopenReason == null || reopenReason.trim().length() < 10) {
            throw new InvalidProjectDataException("Lý do mở lại dự án bắt buộc phải có ít nhất 10 ký tự");
        }
        this.status = ProjectStatus.ACTIVE;
        this.reopenedBy = reopenedBy;
        this.reopenReason = reopenReason.trim();
        this.reopenedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == ProjectStatus.ACTIVE;
    }

    public boolean isClosed() {
        return this.status == ProjectStatus.CLOSED;
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

    public Integer getTaskSeqCounter() {
        return taskSeqCounter != null ? taskSeqCounter : 0;
    }

    public int nextTaskSequence() {
        if (this.taskSeqCounter == null) {
            this.taskSeqCounter = 0;
        }
        this.taskSeqCounter++;
        this.updatedAt = LocalDateTime.now();
        return this.taskSeqCounter;
    }
        public String getClosureReason() {
        return closureReason;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public UserId getClosedBy() {
        return closedBy;
    }

    public Long getClosedByValue() {
        return closedBy != null ? closedBy.value() : null;
    }

    public String getReopenReason() {
        return reopenReason;
    }

    public LocalDateTime getReopenedAt() {
        return reopenedAt;
    }

    public UserId getReopenedBy() {
        return reopenedBy;
    }

    public Long getReopenedByValue() {
        return reopenedBy != null ? reopenedBy.value() : null;
    }
}
