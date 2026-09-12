package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.reservation.entity;

import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_reservations")
public class ResourceReservationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "year_number", nullable = false)
    private int yearNumber;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "reserved_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal reservedHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "converted_allocation_id")
    private Long convertedAllocationId;

    @Column(name = "cancelled_reason", length = 500)
    private String cancelledReason;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public ResourceReservationJpaEntity() {}

    public ResourceReservationJpaEntity(
            Long id,
            Long projectId,
            Long employeeId,
            int yearNumber,
            int weekNumber,
            BigDecimal reservedHours,
            ReservationStatus status,
            Long convertedAllocationId,
            String cancelledReason,
            String note,
            Long createdBy,
            LocalDateTime createdAt,
            Long updatedBy,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        this.projectId = projectId;
        this.employeeId = employeeId;
        this.yearNumber = yearNumber;
        this.weekNumber = weekNumber;
        this.reservedHours = reservedHours;
        this.status = status;
        this.convertedAllocationId = convertedAllocationId;
        this.cancelledReason = cancelledReason;
        this.note = note;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public int getYearNumber() {
        return yearNumber;
    }

    public void setYearNumber(int yearNumber) {
        this.yearNumber = yearNumber;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public BigDecimal getReservedHours() {
        return reservedHours;
    }

    public void setReservedHours(BigDecimal reservedHours) {
        this.reservedHours = reservedHours;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Long getConvertedAllocationId() {
        return convertedAllocationId;
    }

    public void setConvertedAllocationId(Long convertedAllocationId) {
        this.convertedAllocationId = convertedAllocationId;
    }

    public String getCancelledReason() {
        return cancelledReason;
    }

    public void setCancelledReason(String cancelledReason) {
        this.cancelledReason = cancelledReason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
