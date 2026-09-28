package com.hrm.employeemanagement.domain.unavailability;

import com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException;
import com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class UnavailabilityDeclaration {

    private Long id;
    private Long employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private UnavailabilityReasonType reasonType;
    private String reasonDetail;
    private BigDecimal totalHoursDeducted;
    private UnavailabilityStatus status;
    private Long approverId;
    private String approverComment;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public UnavailabilityDeclaration(
            Long id,
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            UnavailabilityReasonType reasonType,
            String reasonDetail,
            BigDecimal totalHoursDeducted,
            UnavailabilityStatus status,
            Long approverId,
            String approverComment,
            LocalDateTime approvedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId không được để trống");
        this.startDate = Objects.requireNonNull(startDate, "startDate không được để trống");
        this.endDate = Objects.requireNonNull(endDate, "endDate không được để trống");
        if (startDate.isAfter(endDate)) {
            throw new InvalidUnavailabilityPeriodException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        this.reasonType = Objects.requireNonNull(reasonType, "reasonType không được để trống");
        this.reasonDetail = reasonDetail;
        this.totalHoursDeducted = totalHoursDeducted != null ? totalHoursDeducted : BigDecimal.ZERO;
        this.status = status != null ? status : UnavailabilityStatus.PENDING;
        this.approverId = approverId;
        this.approverComment = approverComment;
        this.approvedAt = approvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public static UnavailabilityDeclaration create(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            UnavailabilityReasonType reasonType,
            String reasonDetail,
            BigDecimal totalHoursDeducted
    ) {
        return new UnavailabilityDeclaration(
                null,
                employeeId,
                startDate,
                endDate,
                reasonType,
                reasonDetail,
                totalHoursDeducted,
                UnavailabilityStatus.PENDING,
                null,
                null,
                null,
                LocalDateTime.now(),
                null,
                0L
        );
    }

    public void approve(Long approverId, String approverComment) {
        if (this.status != UnavailabilityStatus.PENDING) {
            throw new InvalidUnavailabilityStatusException(
                    "Chỉ có thể phê duyệt khai báo thời gian không sẵn sàng đang ở trạng thái PENDING. Trạng thái hiện tại: " + this.status);
        }
        this.status = UnavailabilityStatus.APPROVED;
        this.approverId = Objects.requireNonNull(approverId, "approverId không được để trống khi duyệt");
        this.approverComment = approverComment;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(Long approverId, String rejectReason) {
        if (this.status != UnavailabilityStatus.PENDING) {
            throw new InvalidUnavailabilityStatusException(
                    "Chỉ có thể từ chối khai báo thời gian không sẵn sàng đang ở trạng thái PENDING. Trạng thái hiện tại: " + this.status);
        }
        this.status = UnavailabilityStatus.REJECTED;
        this.approverId = Objects.requireNonNull(approverId, "approverId không được để trống khi từ chối");
        this.approverComment = rejectReason;
        this.approvedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == UnavailabilityStatus.CANCELLED) {
            throw new InvalidUnavailabilityStatusException("Khai báo đã bị hủy trước đó");
        }
        if (this.status == UnavailabilityStatus.REJECTED) {
            throw new InvalidUnavailabilityStatusException("Không thể hủy khai báo đã bị từ chối");
        }
        this.status = UnavailabilityStatus.CANCELLED;
    }

    public boolean isApproved() {
        return this.status == UnavailabilityStatus.APPROVED;
    }

    public boolean isPending() {
        return this.status == UnavailabilityStatus.PENDING;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public UnavailabilityReasonType getReasonType() {
        return reasonType;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public BigDecimal getTotalHoursDeducted() {
        return totalHoursDeducted;
    }

    public UnavailabilityStatus getStatus() {
        return status;
    }

    public Long getApproverId() {
        return approverId;
    }

    public String getApproverComment() {
        return approverComment;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
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
}
