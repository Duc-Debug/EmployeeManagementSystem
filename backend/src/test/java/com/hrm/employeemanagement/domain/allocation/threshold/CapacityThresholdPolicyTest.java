package com.hrm.employeemanagement.domain.allocation.threshold;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.allocation.InvalidCapacityThresholdException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapacityThresholdPolicyTest {

    @Test
    @DisplayName("TC-01: Ngưỡng hợp lệ khi idle < overload")
    void testValidThresholds() {
        CapacityThresholdPolicy.validateThresholds(BigDecimal.valueOf(90.0), BigDecimal.valueOf(20.0));
        // Không ném ngoại lệ
    }

    @Test
    @DisplayName("TC-02: Ném InvalidCapacityThresholdException khi idleThreshold > overloadThreshold")
    void testIdleGreaterThanOverload_ThrowsException() {
        assertThatThrownBy(() -> CapacityThresholdPolicy.validateThresholds(BigDecimal.valueOf(70.0), BigDecimal.valueOf(80.0)))
                .isInstanceOf(InvalidCapacityThresholdException.class)
                .hasMessageContaining("phải nhỏ hơn");
    }

    @Test
    @DisplayName("Gate #B: Ném InvalidCapacityThresholdException khi idleThreshold == overloadThreshold")
    void testIdleEqualsOverload_ThrowsException() {
        assertThatThrownBy(() -> CapacityThresholdPolicy.validateThresholds(BigDecimal.valueOf(80.0), BigDecimal.valueOf(80.0)))
                .isInstanceOf(InvalidCapacityThresholdException.class)
                .hasMessageContaining("phải nhỏ hơn");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi overloadThreshold hoặc idleThreshold là null")
    void testNullThresholds_ThrowsException() {
        assertThatThrownBy(() -> CapacityThresholdPolicy.validateThresholds(null, BigDecimal.valueOf(20.0)))
                .isInstanceOf(InvalidCapacityThresholdException.class)
                .hasMessageContaining("Ngưỡng quá tải không được để trống");

        assertThatThrownBy(() -> CapacityThresholdPolicy.validateThresholds(BigDecimal.valueOf(90.0), null))
                .isInstanceOf(InvalidCapacityThresholdException.class)
                .hasMessageContaining("Ngưỡng nhàn rỗi không được để trống");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi idleThreshold < 0")
    void testNegativeIdleThreshold_ThrowsException() {
        assertThatThrownBy(() -> CapacityThresholdPolicy.validateThresholds(BigDecimal.valueOf(90.0), BigDecimal.valueOf(-5.0)))
                .isInstanceOf(InvalidCapacityThresholdException.class)
                .hasMessageContaining("không được nhỏ hơn 0%");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi overloadThreshold > 200")
    void testOverloadExceedsMax_ThrowsException() {
        assertThatThrownBy(() -> CapacityThresholdPolicy.validateThresholds(BigDecimal.valueOf(250.0), BigDecimal.valueOf(20.0)))
                .isInstanceOf(InvalidCapacityThresholdException.class)
                .hasMessageContaining("không được vượt quá 200%");
    }

    @Test
    @DisplayName("Tính toán scopeKey cho COMPANY và ORG_UNIT")
    void testComputeScopeKey() {
        assertThat(CapacityThresholdPolicy.computeScopeKey(CapacityThresholdScope.COMPANY, null))
                .isEqualTo("COMPANY");
        assertThat(CapacityThresholdPolicy.computeScopeKey(CapacityThresholdScope.ORG_UNIT, 10L))
                .isEqualTo("ORG_UNIT_10");

        assertThatThrownBy(() -> CapacityThresholdPolicy.computeScopeKey(CapacityThresholdScope.ORG_UNIT, null))
                .isInstanceOf(InvalidCapacityThresholdException.class)
                .hasMessageContaining("orgUnitId là bắt buộc");
    }

    @Test
    @DisplayName("validateVersion: Hợp lệ khi version gửi lên khớp với version hiện tại")
    void testValidateVersion_Matching_Success() {
        CapacityThresholdPolicy.validateVersion(5L, 5L);
        // Không ném ngoại lệ
    }

    @Test
    @DisplayName("validateVersion: Hợp lệ khi tạo mới cấu hình (cả hai version đều null)")
    void testValidateVersion_BothNull_Success() {
        CapacityThresholdPolicy.validateVersion(null, null);
        // Không ném ngoại lệ
    }

    @Test
    @DisplayName("validateVersion: Ném InvalidCapacityThresholdException khi update bản ghi hiện có nhưng version bị null")
    void testValidateVersion_NullExpectedVersionOnExisting_ThrowsInvalidCapacityThreshold() {
        assertThatThrownBy(() -> CapacityThresholdPolicy.validateVersion(null, 5L))
                .isInstanceOf(InvalidCapacityThresholdException.class)
                .hasMessageContaining("Phiên bản cấu hình (version) là bắt buộc khi cập nhật");
    }

    @Test
    @DisplayName("validateVersion: Ném CapacityThresholdVersionConflictException khi version không khớp")
    void testValidateVersion_Mismatch_ThrowsConflict() {
        assertThatThrownBy(() -> CapacityThresholdPolicy.validateVersion(4L, 5L))
                .isInstanceOf(com.hrm.employeemanagement.domain.exception.allocation.CapacityThresholdVersionConflictException.class)
                .hasMessageContaining("Cấu hình ngưỡng đã được cập nhật bởi thao tác khác");
    }
}
