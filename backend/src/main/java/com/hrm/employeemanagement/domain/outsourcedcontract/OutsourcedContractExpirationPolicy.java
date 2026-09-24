package com.hrm.employeemanagement.domain.outsourcedcontract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * Domain Policy thực thi các quy tắc nghiệp vụ rà soát thời hạn hợp đồng thuê ngoài
 * và phân loại các phân bổ vi phạm/có nguy cơ theo quy tắc QTN-21.
 */
public final class OutsourcedContractExpirationPolicy {

    public static final int DEFAULT_WARNING_THRESHOLD_DAYS = 30;

    private OutsourcedContractExpirationPolicy() {
        // Private constructor for utility policy class
    }

    /**
     * Tính số ngày còn lại đến khi hợp đồng hết hạn.
     */
    public static long calculateDaysRemaining(LocalDate contractEndDate, LocalDate referenceDate) {
        Objects.requireNonNull(contractEndDate, "contractEndDate must not be null");
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        return ChronoUnit.DAYS.between(ref, contractEndDate);
    }

    /**
     * Đánh giá trạng thái hợp đồng thuê ngoài theo ngưỡng cảnh báo.
     */
    public static OutsourcedContractStatus evaluateStatus(
            LocalDate contractEndDate,
            LocalDate referenceDate,
            int warningThresholdDays
    ) {
        long daysRemaining = calculateDaysRemaining(contractEndDate, referenceDate);
        if (daysRemaining < 0) {
            return OutsourcedContractStatus.EXPIRED;
        }
        if (daysRemaining <= warningThresholdDays) {
            return OutsourcedContractStatus.EXPIRING_SOON;
        }
        return OutsourcedContractStatus.ACTIVE_SAFE;
    }

    /**
     * Kiểm tra và phân loại mức độ ảnh hưởng của một dòng phân bổ tuần đối với thời hạn hợp đồng thuê ngoài (QTN-21).
     * 
     * Quy tắc QTN-21: Nhân sự thuê ngoài chỉ được phân bổ trong thời hạn hợp đồng thuê.
     * - Nếu tuần phân bổ bắt đầu sau ngày hết hạn -> AFTER_EXPIRY (vi phạm phân bổ ngoài hạn).
     * - Nếu ngày hết hạn nằm giữa tuần (thứ Hai đến thứ Bảy) -> SPANS_OVER_EXPIRY (vắt qua hạn hợp đồng).
     * - Nếu tuần kết thúc trước hoặc đúng ngày hết hạn -> Hợp lệ (Optional.empty()).
     */
    public static Optional<OutsourcedContractAffectedAllocation> evaluateAllocationImpact(
            LocalDate contractEndDate,
            YearWeek yearWeek,
            BigDecimal allocatedHours,
            Long allocationId,
            Long projectId,
            String projectName
    ) {
        if (contractEndDate == null || yearWeek == null) {
            return Optional.empty();
        }
        if (allocatedHours == null || allocatedHours.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        LocalDate weekStartDate = yearWeek.getStartDate();
        LocalDate weekEndDate = yearWeek.getEndDate();

        // 1. Tuần phân bổ nằm hoàn toàn sau ngày hết hạn
        if (weekStartDate.isAfter(contractEndDate)) {
            String reason = String.format(
                    "Tuần phân bổ %02d/%d (%s đến %s) bắt đầu sau ngày hết hạn hợp đồng (%s), vi phạm quy tắc QTN-21.",
                    yearWeek.weekNumber(), yearWeek.year(), weekStartDate, weekEndDate, contractEndDate
            );
            return Optional.of(new OutsourcedContractAffectedAllocation(
                    allocationId, projectId, projectName, yearWeek, weekStartDate, weekEndDate,
                    allocatedHours, AffectedAllocationType.AFTER_EXPIRY, reason
            ));
        }

        // 2. Ngày hết hạn rơi vào giữa tuần (từ thứ Hai đến thứ Bảy của tuần)
        // Khi đó, ngày Chủ nhật của tuần (weekEndDate) nằm sau ngày hết hạn hợp đồng
        if (weekEndDate.isAfter(contractEndDate) && !contractEndDate.isBefore(weekStartDate)) {
            String reason = String.format(
                    "Hợp đồng hết hạn vào ngày %s ở giữa tuần %02d/%d (%s đến %s), các ngày làm việc sau ngày này vắt qua hạn hợp đồng vi phạm quy tắc QTN-21.",
                    contractEndDate, yearWeek.weekNumber(), yearWeek.year(), weekStartDate, weekEndDate
            );
            return Optional.of(new OutsourcedContractAffectedAllocation(
                    allocationId, projectId, projectName, yearWeek, weekStartDate, weekEndDate,
                    allocatedHours, AffectedAllocationType.SPANS_OVER_EXPIRY, reason
            ));
        }

        return Optional.empty();
    }

    /**
     * Xác định xem một hợp đồng thuê ngoài có thuộc diện cần cảnh báo rà soát hay không.
     */
    public static boolean isContractRequiringWarning(
            OutsourcedContractStatus status,
            List<OutsourcedContractAffectedAllocation> affectedAllocations
    ) {
        return status == OutsourcedContractStatus.EXPIRING_SOON
                || status == OutsourcedContractStatus.EXPIRED
                || (affectedAllocations != null && !affectedAllocations.isEmpty());
    }
}
