package com.hrm.employeemanagement.domain.role;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("P2-5: RoleCode Parsing & Alias Resolution Tests")
class RoleCodeTest {

    @ParameterizedTest(name = "Mã đầu vào \"{0}\" phải phân giải thành {1}")
    @CsvSource({
            // VT-01 (Ban Giám Đốc)
            "VT-01, VT_01", "vt-01, VT_01", "VT01, VT_01", "VT_01, VT_01",
            "ROLE-VT-01, VT_01", "ROLE_VT_01, VT_01",
            "ROLE-EXECUTIVE, VT_01", "ROLE_EXECUTIVE, VT_01", "EXECUTIVE, VT_01",
            "DIRECTOR, VT_01", "ROLE-DIRECTOR, VT_01", "ROLE_DIRECTOR, VT_01",
            "BGD, VT_01", "ROLE-BGD, VT_01", "ROLE_BGD, VT_01",

            // VT-02 (Quản Lý Dự Án)
            "VT-02, VT_02", "vt-02, VT_02", "VT02, VT_02", "VT_02, VT_02",
            "ROLE-VT-02, VT_02", "ROLE_VT_02, VT_02",
            "ROLE-PM, VT_02", "ROLE_PM, VT_02", "PM, VT_02",
            "PROJECT-MANAGER, VT_02", "PROJECT_MANAGER, VT_02",

            // VT-03 (Quản Lý Nguồn Lực)
            "VT-03, VT_03", "vt-03, VT_03", "VT03, VT_03", "VT_03, VT_03",
            "ROLE-VT-03, VT_03", "ROLE_VT_03, VT_03",
            "ROLE-RM, VT_03", "ROLE_RM, VT_03", "RM, VT_03",
            "RESOURCE-MANAGER, VT_03", "RESOURCE_MANAGER, VT_03",

            // VT-04 (Nhân Viên Chuyên Môn)
            "VT-04, VT_04", "vt-04, VT_04", "VT04, VT_04", "VT_04, VT_04",
            "ROLE-VT-04, VT_04", "ROLE_VT_04, VT_04",
            "ROLE-EMPLOYEE, VT_04", "ROLE_EMPLOYEE, VT_04", "EMPLOYEE, VT_04",
            "SPECIALIST, VT_04", "DEVELOPER, VT_04", "MEMBER, VT_04",

            // VT-05 (Nhân Sự)
            "VT-05, VT_05", "vt-05, VT_05", "VT05, VT_05", "VT_05, VT_05",
            "ROLE-VT-05, VT_05", "ROLE_VT_05, VT_05",
            "ROLE-HR, VT_05", "ROLE_HR, VT_05", "HR, VT_05",
            "HR-MANAGER, VT_05", "HR_MANAGER, VT_05",
            "HR-SPECIALIST, VT_05", "HR_SPECIALIST, VT_05",

            // VT-06 (Quản Trị Viên)
            "VT-06, VT_06", "vt-06, VT_06", "VT06, VT_06", "VT_06, VT_06",
            "ROLE-VT-06, VT_06", "ROLE_VT_06, VT_06",
            "ROLE-ADMIN, VT_06", "ROLE_ADMIN, VT_06", "ADMIN, VT_06",
            "SYSTEM-ADMIN, VT_06", "SYSTEM_ADMIN, VT_06"
    })
    void fromCode_shouldResolveCanonicalAndAliasesCorrectly(String input, RoleCode expected) {
        assertThat(RoleCode.fromCode(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void fromCode_shouldThrowExceptionWhenBlank(String input) {
        assertThatThrownBy(() -> RoleCode.fromCode(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mã vai trò không được để trống");
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN_ROLE", "SUPERUSER", "ROOT", "VT-99"})
    void fromCode_shouldThrowExceptionWhenUnknown(String input) {
        assertThatThrownBy(() -> RoleCode.fromCode(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mã vai trò không hợp lệ");
    }
}
