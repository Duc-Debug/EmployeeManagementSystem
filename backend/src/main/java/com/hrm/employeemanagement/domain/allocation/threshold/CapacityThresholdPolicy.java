package com.hrm.employeemanagement.domain.allocation.threshold;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.allocation.InvalidCapacityThresholdException;

/**
 * Domain Policy thực hiện các quy tắc nghiệp vụ cho cấu hình ngưỡng cảnh báo theo QTN-23:
 * - BR-01: Ngưỡng % quá tải và % nhàn rỗi.
 * - BR-03 (TC-02): idleThreshold < overloadThreshold (ngưỡng nhàn rỗi không được lớn hơn hoặc bằng ngưỡng quá tải).
 * - BR-04: Ngưỡng mặc định (100% quá tải, 50% nhàn rỗi).
 * - Giới hạn giá trị: 0 <= idleThreshold < overloadThreshold <= 200.
 */
public class CapacityThresholdPolicy {

    public static final BigDecimal DEFAULT_OVERLOAD_THRESHOLD = BigDecimal.valueOf(100.0).setScale(1, RoundingMode.HALF_UP);
    public static final BigDecimal DEFAULT_IDLE_THRESHOLD = BigDecimal.valueOf(50.0).setScale(1, RoundingMode.HALF_UP);
    public static final BigDecimal MIN_THRESHOLD = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
    public static final BigDecimal MAX_IDLE_THRESHOLD = BigDecimal.valueOf(100.0).setScale(1, RoundingMode.HALF_UP);
    public static final BigDecimal MAX_THRESHOLD = BigDecimal.valueOf(200.0).setScale(1, RoundingMode.HALF_UP);

    public static void validateThresholds(BigDecimal overloadThreshold, BigDecimal idleThreshold) {
        if (overloadThreshold == null) {
            throw new InvalidCapacityThresholdException("Ngưỡng quá tải không được để trống");
        }
        if (idleThreshold == null) {
            throw new InvalidCapacityThresholdException("Ngưỡng nhàn rỗi không được để trống");
        }

        if (idleThreshold.compareTo(MIN_THRESHOLD) < 0) {
            throw new InvalidCapacityThresholdException("Ngưỡng nhàn rỗi không được nhỏ hơn 0%");
        }

        if (idleThreshold.compareTo(MAX_IDLE_THRESHOLD) > 0) {
            throw new InvalidCapacityThresholdException("Ngưỡng nhàn rỗi không được vượt quá 100%");
        }

        if (overloadThreshold.compareTo(MIN_THRESHOLD) <= 0) {
            throw new InvalidCapacityThresholdException("Ngưỡng quá tải phải lớn hơn 0%");
        }

        if (overloadThreshold.compareTo(MAX_THRESHOLD) > 0) {
            throw new InvalidCapacityThresholdException("Ngưỡng quá tải không được vượt quá 200%");
        }

        // BR-03 (TC-02) & Assumption Gate #B: idleThreshold phải nhỏ hơn overloadThreshold
        if (idleThreshold.compareTo(overloadThreshold) >= 0) {
            throw new InvalidCapacityThresholdException(
                    "Ngưỡng nhàn rỗi (" + idleThreshold + "%) phải nhỏ hơn ngưỡng quá tải (" + overloadThreshold + "%)"
            );
        }
    }

    public static void validateVersion(Long expectedVersion, Long currentVersion) {
        if (currentVersion != null) {
            if (expectedVersion == null) {
                throw new InvalidCapacityThresholdException("Phiên bản cấu hình (version) là bắt buộc khi cập nhật cấu hình đã tồn tại");
            }
            if (!Objects.equals(expectedVersion, currentVersion)) {
                throw new com.hrm.employeemanagement.domain.exception.allocation.CapacityThresholdVersionConflictException(
                        String.format(
                                "Cấu hình ngưỡng đã được cập nhật bởi thao tác khác (phiên bản hiện tại: %d, phiên bản gửi lên: %d). Vui lòng tải lại dữ liệu mới nhất.",
                                currentVersion, expectedVersion
                        )
                );
            }
        }
    }

    public static void validateScope(CapacityThresholdScope scopeType, Long orgUnitId) {
        Objects.requireNonNull(scopeType, "scopeType không được null");
        if (scopeType == CapacityThresholdScope.COMPANY && orgUnitId != null) {
            throw new InvalidCapacityThresholdException("orgUnitId phải null khi scopeType là COMPANY");
        }
        if (scopeType == CapacityThresholdScope.ORG_UNIT && orgUnitId == null) {
            throw new InvalidCapacityThresholdException("orgUnitId là bắt buộc khi scopeType là ORG_UNIT");
        }
    }

    public static String computeScopeKey(CapacityThresholdScope scopeType, Long orgUnitId) {
        validateScope(scopeType, orgUnitId);
        if (scopeType == CapacityThresholdScope.ORG_UNIT) {
            return "ORG_UNIT_" + orgUnitId;
        }
        return "COMPANY";
    }
}
