package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import jakarta.persistence.UniqueConstraint;

/**
 * Entity lưu trữ phân bổ nguồn lực theo tuần.
 * 
 * Quy tắc nghiệp vụ (Business Rule):
 * - Mỗi nhân sự trong 1 dự án tại 1 tuần cụ thể chỉ có DUY NHẤT 1 bản ghi phân bổ tương ứng với 1 vai trò chuyên môn (projectRoleId).
 * - Ràng buộc duy nhất: uk_emp_proj_year_week (employee_id, project_id, year_number, week_number).
 * - Khi phân bổ lại cùng nhân sự - dự án - tuần, hệ thống sẽ CẬP NHẬT (update) số giờ và vai trò (projectRoleId) mới.
 */
@Entity
@Table(
        name = "weekly_project_allocations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_emp_proj_year_week",
                        columnNames = {"employee_id", "project_id", "year_number", "week_number"}
                )
        }
)
public class WeeklyProjectAllocationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "project_role_id")
    private Long projectRoleId;

    @Column(name = "year_number", nullable = false)
    private Integer year;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "allocated_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal allocatedHours;

    @Column(name = "allocation_percentage", precision = 5, scale = 2)
    private BigDecimal allocationPercentage;

    @Column(name = "is_overloaded", nullable = false)
    private Boolean isOverloaded = false;

    @Column(name = "overload_reason", columnDefinition = "TEXT")
    private String overloadReason;

    @Column(name = "overload_approved_by")
    private Long overloadApprovedBy;

    @Column(name = "overload_approved_at")
    private LocalDateTime overloadApprovedAt;

    @Column(name = "variance_note", length = 1000)
    private String varianceNote;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public WeeklyProjectAllocationJpaEntity() {
    }

    public WeeklyProjectAllocationJpaEntity(Long id, Long employeeId, Long projectId, Integer year,
            Integer weekNumber, BigDecimal allocatedHours, Long version) {
        this(id, employeeId, projectId, null, year, weekNumber, allocatedHours, null, false, null, null, null, null, null, version);
    }

    public WeeklyProjectAllocationJpaEntity(Long id, Long employeeId, Long projectId, Long projectRoleId, Integer year,
            Integer weekNumber, BigDecimal allocatedHours, Long version) {
        this(id, employeeId, projectId, projectRoleId, year, weekNumber, allocatedHours, null, false, null, null, null, null, null, version);
    }

    public WeeklyProjectAllocationJpaEntity(Long id, Long employeeId, Long projectId, Integer year,
            Integer weekNumber, BigDecimal allocatedHours, BigDecimal allocationPercentage, Long version) {
        this(id, employeeId, projectId, null, year, weekNumber, allocatedHours, allocationPercentage, false, null, null, null, null, null, version);
    }

    public WeeklyProjectAllocationJpaEntity(Long id, Long employeeId, Long projectId, Long projectRoleId, Integer year,
            Integer weekNumber, BigDecimal allocatedHours, BigDecimal allocationPercentage, Long version) {
        this(id, employeeId, projectId, projectRoleId, year, weekNumber, allocatedHours, allocationPercentage, false, null, null, null, null, null, version);
    }

    public WeeklyProjectAllocationJpaEntity(Long id, Long employeeId, Long projectId, Integer year,
            Integer weekNumber, BigDecimal allocatedHours, BigDecimal allocationPercentage, Boolean isOverloaded, String overloadReason,
            Long overloadApprovedBy, LocalDateTime overloadApprovedAt, Long version) {
        this(id, employeeId, projectId, null, year, weekNumber, allocatedHours, allocationPercentage, isOverloaded, overloadReason, overloadApprovedBy, overloadApprovedAt, null, null, version);
    }

    public WeeklyProjectAllocationJpaEntity(Long id, Long employeeId, Long projectId, Long projectRoleId, Integer year,
            Integer weekNumber, BigDecimal allocatedHours, BigDecimal allocationPercentage, Boolean isOverloaded, String overloadReason,
            Long overloadApprovedBy, LocalDateTime overloadApprovedAt, Long version) {
        this(id, employeeId, projectId, projectRoleId, year, weekNumber, allocatedHours, allocationPercentage, isOverloaded, overloadReason, overloadApprovedBy, overloadApprovedAt, null, null, version);
    }

    public WeeklyProjectAllocationJpaEntity(Long id, Long employeeId, Long projectId, Integer year,
            Integer weekNumber, BigDecimal allocatedHours, BigDecimal allocationPercentage, Boolean isOverloaded, String overloadReason,
            Long overloadApprovedBy, LocalDateTime overloadApprovedAt, String varianceNote, Long updatedBy, Long version) {
        this(id, employeeId, projectId, null, year, weekNumber, allocatedHours, allocationPercentage, isOverloaded, overloadReason, overloadApprovedBy, overloadApprovedAt, varianceNote, updatedBy, version);
    }

    public WeeklyProjectAllocationJpaEntity(Long id, Long employeeId, Long projectId, Long projectRoleId, Integer year,
            Integer weekNumber, BigDecimal allocatedHours, BigDecimal allocationPercentage, Boolean isOverloaded, String overloadReason,
            Long overloadApprovedBy, LocalDateTime overloadApprovedAt, String varianceNote, Long updatedBy, Long version) {
        this.id = id;
        this.employeeId = employeeId;
        this.projectId = projectId;
        this.projectRoleId = projectRoleId;
        this.year = year;
        this.weekNumber = weekNumber;
        this.allocatedHours = allocatedHours;
        this.allocationPercentage = allocationPercentage;
        this.isOverloaded = isOverloaded != null ? isOverloaded : false;
        this.overloadReason = overloadReason;
        this.overloadApprovedBy = overloadApprovedBy;
        this.overloadApprovedAt = overloadApprovedAt;
        this.varianceNote = varianceNote;
        this.updatedBy = updatedBy;
        this.version = version != null ? version : 0L;
    }

    // Getters and Setters...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectRoleId() {
        return projectRoleId;
    }

    public void setProjectRoleId(Long projectRoleId) {
        this.projectRoleId = projectRoleId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public BigDecimal getAllocatedHours() {
        return allocatedHours;
    }

    public void setAllocatedHours(BigDecimal allocatedHours) {
        this.allocatedHours = allocatedHours;
    }

    public BigDecimal getAllocationPercentage() {
        return allocationPercentage;
    }

    public void setAllocationPercentage(BigDecimal allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getIsOverloaded() {
        return isOverloaded;
    }

    public void setIsOverloaded(Boolean isOverloaded) {
        this.isOverloaded = isOverloaded != null ? isOverloaded : false;
    }

    public String getOverloadReason() {
        return overloadReason;
    }

    public void setOverloadReason(String overloadReason) {
        this.overloadReason = overloadReason;
    }

    public Long getOverloadApprovedBy() {
        return overloadApprovedBy;
    }

    public void setOverloadApprovedBy(Long overloadApprovedBy) {
        this.overloadApprovedBy = overloadApprovedBy;
    }

    public LocalDateTime getOverloadApprovedAt() {
        return overloadApprovedAt;
    }

    public void setOverloadApprovedAt(LocalDateTime overloadApprovedAt) {
        this.overloadApprovedAt = overloadApprovedAt;
    }

    public String getVarianceNote() {
        return varianceNote;
    }

    public void setVarianceNote(String varianceNote) {
        this.varianceNote = varianceNote;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
