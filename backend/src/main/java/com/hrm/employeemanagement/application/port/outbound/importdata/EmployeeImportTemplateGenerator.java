package com.hrm.employeemanagement.application.port.outbound.importdata;

/**
 * Cổng ra (Outbound Port) sinh tệp biểu mẫu mẫu cho tính năng nhập dữ liệu.
 */
public interface EmployeeImportTemplateGenerator {

    /**
     * Kiểm tra định dạng biểu mẫu có được hỗ trợ hay không (vd: xlsx).
     */
    boolean supports(String format);

    /**
     * Sinh mảng byte nhị phân của tệp biểu mẫu chuẩn kèm dữ liệu mẫu.
     */
    byte[] generateTemplate();

    /**
     * Tên tệp mặc định khi tải về.
     */
    String getFilename();

    /**
     * MIME type của tệp.
     */
    String getContentType();
}
