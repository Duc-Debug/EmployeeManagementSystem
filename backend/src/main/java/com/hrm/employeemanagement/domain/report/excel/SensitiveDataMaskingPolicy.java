package com.hrm.employeemanagement.domain.report.excel;

import com.hrm.employeemanagement.domain.role.RoleCode;

/**
 * Domain Policy chịu trách nhiệm quy định việc che các thông tin nhạy cảm
 * khi xuất báo cáo ra bảng tính Excel theo thẩm quyền của người xuất (NCL-10-CN-003).
 */
public final class SensitiveDataMaskingPolicy {

    public static final String MASKED_STRING = "***";
    public static final String NOT_CONFIGURED_STRING = "Chưa cập nhật";

    private SensitiveDataMaskingPolicy() {
        // Utility class
    }

    /**
     * Xác định giá trị hiển thị cho cột mức lương/thu nhập.
     * Quản lý dự án (VT-02) và các vai trò không phải HR/Lãnh đạo tuyệt đối không được xem.
     */
    public static String maskSalary(RoleCode userRoleCode, Object salaryValue) {
        if (userRoleCode == null) {
            return MASKED_STRING;
        }
        // Chỉ Ban Giám Đốc (VT-01) hoặc Nhân sự (VT-05) mới có thể xem lương nếu được cấu hình
        if (userRoleCode == RoleCode.VT_01 || userRoleCode == RoleCode.VT_05) {
            return salaryValue != null ? salaryValue.toString() : NOT_CONFIGURED_STRING;
        }
        // Quản lý dự án (VT-02) và các vai trò khác luôn bị che
        return MASKED_STRING;
    }

    /**
     * Xác định giá trị hiển thị cho cột đơn giá chi phí nhân sự (Cost Rate).
     */
    public static String maskCostRate(RoleCode userRoleCode, Object costRateValue) {
        if (userRoleCode == null) {
            return MASKED_STRING;
        }
        if (userRoleCode == RoleCode.VT_01) {
            return costRateValue != null ? costRateValue.toString() : NOT_CONFIGURED_STRING;
        }
        // Quản lý dự án (VT-02) bị che đơn giá chi phí nội bộ
        return MASKED_STRING;
    }

    /**
     * Kiểm tra người dùng có quyền xuất báo cáo cho dự án hay không.
     * Quản lý dự án (VT-02) chỉ được xuất báo cáo cho dự án mà mình được bổ nhiệm làm PM.
     */
    public static boolean canExportProjectReport(RoleCode userRoleCode, Long currentEmployeeId, Long projectManagerId) {
        if (userRoleCode == null) {
            return false;
        }
        // Ban Giám đốc (VT-01) được xuất toàn bộ dự án
        if (userRoleCode == RoleCode.VT_01) {
            return true;
        }
        // Quản lý dự án (VT-02) chỉ được xuất khi chính mình là PM của dự án đó
        if (userRoleCode == RoleCode.VT_02) {
            return currentEmployeeId != null && currentEmployeeId.equals(projectManagerId);
        }
        return false;
    }
}
