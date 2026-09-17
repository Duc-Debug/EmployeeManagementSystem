package com.hrm.employeemanagement.application.port.outbound.report.excel;

import java.util.List;

import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;

/**
 * Cổng truy xuất dữ liệu phân bổ toàn bộ vòng đời của dự án phục vụ xuất báo cáo Excel.
 */
public interface LoadProjectAllocationsForExcelReportPort {

    /**
     * Nạp toàn bộ danh sách phân bổ của một dự án từ trước đến nay mà không giới hạn theo dải tuần cố định.
     *
     * @param projectId ID định danh dự án
     * @return Danh sách các bản ghi phân bổ dự án hàng tuần
     */
    List<WeeklyProjectAllocation> loadAllAllocationsForProject(Long projectId);
}
