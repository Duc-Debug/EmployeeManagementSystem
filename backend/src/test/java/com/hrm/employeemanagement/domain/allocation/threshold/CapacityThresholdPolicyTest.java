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
}
