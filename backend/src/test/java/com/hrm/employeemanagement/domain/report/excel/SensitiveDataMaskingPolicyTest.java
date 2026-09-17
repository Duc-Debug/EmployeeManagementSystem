package com.hrm.employeemanagement.domain.report.excel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.role.RoleCode;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SensitiveDataMaskingPolicy Domain Unit Tests")
class SensitiveDataMaskingPolicyTest {

    @Test
    @DisplayName("Quản lý dự án (VT-02) luôn bị che cột mức lương thành '***'")
    void testMaskSalary_ProjectManager_Masked() {
        String result = SensitiveDataMaskingPolicy.maskSalary(RoleCode.VT_02, 25000000L);
        assertEquals("***", result);
    }

    @Test
    @DisplayName("Nhân viên chuyên môn (VT-04) luôn bị che cột mức lương thành '***'")
    void testMaskSalary_Employee_Masked() {
        String result = SensitiveDataMaskingPolicy.maskSalary(RoleCode.VT_04, 20000000L);
        assertEquals("***", result);
    }

    @Test
    @DisplayName("Ban Giám Đốc (VT-01) được hiển thị giá trị mức lương")
    void testMaskSalary_Director_Visible() {
        String result = SensitiveDataMaskingPolicy.maskSalary(RoleCode.VT_01, 30000000L);
        assertEquals("30000000", result);
    }

    @Test
    @DisplayName("Quản lý dự án (VT-02) luôn bị che cột đơn giá chi phí thành '***'")
    void testMaskCostRate_ProjectManager_Masked() {
        String result = SensitiveDataMaskingPolicy.maskCostRate(RoleCode.VT_02, 150000.0);
        assertEquals("***", result);
    }

    @Test
    @DisplayName("Ban Giám Đốc (VT-01) được hiển thị đơn giá chi phí")
    void testMaskCostRate_Director_Visible() {
        String result = SensitiveDataMaskingPolicy.maskCostRate(RoleCode.VT_01, 200000.0);
        assertEquals("200000.0", result);
    }

    @Test
    @DisplayName("PM được phép xuất báo cáo khi chính mình là Quản lý dự án của dự án đó")
    void testCanExport_ProjectManager_MatchingId_Allowed() {
        Long pmEmployeeId = 10L;
        Long projectManagerId = 10L;

        boolean canExport = SensitiveDataMaskingPolicy.canExportProjectReport(RoleCode.VT_02, pmEmployeeId, projectManagerId);
        assertTrue(canExport);
    }

    @Test
    @DisplayName("PM bị từ chối xuất báo cáo khi dự án do người khác làm Quản lý")
    void testCanExport_ProjectManager_DifferentId_Denied() {
        Long pmEmployeeId = 10L;
        Long otherProjectManagerId = 99L;

        boolean canExport = SensitiveDataMaskingPolicy.canExportProjectReport(RoleCode.VT_02, pmEmployeeId, otherProjectManagerId);
        assertFalse(canExport);
    }

    @Test
    @DisplayName("Ban Giám Đốc (VT-01) được phép xuất báo cáo của bất kỳ dự án nào")
    void testCanExport_Director_AlwaysAllowed() {
        boolean canExport = SensitiveDataMaskingPolicy.canExportProjectReport(RoleCode.VT_01, 5L, 99L);
        assertTrue(canExport);
    }

    @Test
    @DisplayName("Nhân viên chuyên môn (VT-04) không được phép xuất báo cáo dự án")
    void testCanExport_Employee_Denied() {
        boolean canExport = SensitiveDataMaskingPolicy.canExportProjectReport(RoleCode.VT_04, 10L, 10L);
        assertFalse(canExport);
    }
}
