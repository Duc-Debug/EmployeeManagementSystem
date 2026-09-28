package com.hrm.employeemanagement.domain.reservation;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationDataException;
import com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationStateException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Thực thể miền đại diện cho một bản ghi giữ chỗ nguồn lực cho dự án dự kiến theo QTN-13:
 * - Giữ chỗ không phải cam kết chính thức (không cộng vào đã cam kết).
 * - Vòng đời: ACTIVE -> CONVERTED (khi dự án được duyệt triển khai) hoặc ACTIVE -> CANCELLED (khi dự án bị hủy hoặc quá hạn).
 * - Không cho phép đảo ngược trạng thái từ CONVERTED hoặc CANCELLED.
 */
public class ResourceReservation {

    private Long id;
    private Long projectId;
    private Long employeeId;
    private YearWeek yearWeek;
    private BigDecimal reservedHours;
    private ReservationStatus status;
    private Long convertedAllocationId;
    private String cancelledReason;
    private String note;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Long version;

    public ResourceReservation(
            Long id,
            Long projectId,
            Long employeeId,
            YearWeek yearWeek,
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
        this.projectId = Objects.requireNonNull(projectId, "Mã dự án (projectId) không được để trống");
        this.employeeId = Objects.requireNonNull(employeeId, "Mã nhân sự (employeeId) không được để trống");
        this.yearWeek = Objects.requireNonNull(yearWeek, "Tuần/Năm (YearWeek) không được để trống");
        validateAndSetReservedHours(reservedHours);
        this.status = status != null ? status : ReservationStatus.ACTIVE;
        this.convertedAllocationId = convertedAllocationId;
        this.cancelledReason = cancelledReason != null ? cancelledReason.trim() : null;
        this.note = note != null ? note.trim() : null;
        this.createdBy = Objects.requireNonNull(createdBy, "Người tạo giữ chỗ không được để trống");
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public static ResourceReservation createNew(
            Long projectId,
            Long employeeId,
            YearWeek yearWeek,
            BigDecimal reservedHours,
            String note,
            Long createdBy
    ) {
        return new ResourceReservation(
                null,
                projectId,
                employeeId,
                yearWeek,
                reservedHours,
                ReservationStatus.ACTIVE,
                null,
                null,
                note,
                createdBy,
                LocalDateTime.now(),
                null,
                null,
                0L
        );
    }

    /**
     * Hủy giữ chỗ nguồn lực (TC-02). Chỉ áp dụng cho bản ghi đang ACTIVE.
     */
    public void cancel(Long cancelledBy, String reason) {
        if (this.status != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException(
                    "Không thể hủy giữ chỗ vì bản ghi không ở trạng thái ACTIVE (Trạng thái hiện tại: " + this.status + ")"
            );
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelledReason = reason != null && !reason.isBlank() ? reason.trim() : "Hủy giữ chỗ";
        this.updatedBy = cancelledBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Chuyển đổi giữ chỗ thành phân bổ chính thức khi dự án được duyệt (TC-03).
     */
    public void convert(Long allocationId, Long convertedBy) {
        if (this.status != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException(
                    "Không thể chuyển đổi giữ chỗ vì bản ghi không ở trạng thái ACTIVE (Trạng thái hiện tại: " + this.status + ")"
            );
        }
        if (allocationId == null) {
            throw new InvalidReservationDataException("Mã phân bổ chuyển đổi (allocationId) không được để trống");
        }
        this.status = ReservationStatus.CONVERTED;
        this.convertedAllocationId = allocationId;
        this.updatedBy = convertedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateReservedHours(BigDecimal newHours, Long updatedBy) {
        if (this.status != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException("Chỉ có thể điều chỉnh số giờ giữ chỗ khi bản ghi đang ở trạng thái ACTIVE");
        }
        validateAndSetReservedHours(newHours);
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateAndSetReservedHours(BigDecimal hours) {
        if (hours == null) {
            throw new InvalidReservationDataException("Số giờ giữ chỗ không được để trống");
        }
        if (hours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidReservationDataException("Số giờ giữ chỗ phải lớn hơn 0 (Giờ nhập: " + hours + ")");
        }
        if (hours.compareTo(BigDecimal.valueOf(168)) > 0) {
            throw new InvalidReservationDataException("Số giờ giữ chỗ trong tuần không được vượt quá 168 giờ");
        }
        this.reservedHours = hours;
    }

    public boolean isActive() {
        return this.status == ReservationStatus.ACTIVE;
    }

    public boolean isConverted() {
        return this.status == ReservationStatus.CONVERTED;
    }

    public boolean isCancelled() {
        return this.status == ReservationStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public YearWeek getYearWeek() {
        return yearWeek;
    }

    public int getYear() {
        return yearWeek.year();
    }

    public int getWeekNumber() {
        return yearWeek.weekNumber();
    }

    public BigDecimal getReservedHours() {
        return reservedHours;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Long getConvertedAllocationId() {
        return convertedAllocationId;
    }

    public String getCancelledReason() {
        return cancelledReason;
    }

    public String getNote() {
        return note;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
