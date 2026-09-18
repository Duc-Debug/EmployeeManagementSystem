package com.hrm.employeemanagement.application.port.outbound.importdata;

import java.io.InputStream;
import java.util.List;

import com.hrm.employeemanagement.domain.exception.importdata.InvalidImportTemplateException;
import com.hrm.employeemanagement.domain.importdata.RawEmployeeImportRow;

/**
 * Cổng ra (Outbound Port) định nghĩa giao ước phân tích tệp dữ liệu nhân sự thô.
 * Áp dụng Strategy Pattern để hỗ trợ linh hoạt các định dạng tệp khác nhau mà không sửa logic nghiệp vụ.
 */
public interface EmployeeDataFileParser {

    /**
     * Kiểm tra xem Parser này có hỗ trợ định dạng tệp tương ứng hay không.
     *
     * @param filename Tên tệp hoặc đuôi mở rộng
     * @return true nếu hỗ trợ, ngược lại false
     */
    boolean supports(String filename);

    /**
     * Phân tích luồng nhị phân (InputStream) thành danh sách các dòng dữ liệu thô.
     *
     * @param inputStream Luồng dữ liệu tệp
     * @return Danh sách RawEmployeeImportRow
     * @throws InvalidImportTemplateException khi cấu trúc tệp/tiêu đề không đúng định dạng biểu mẫu
     */
    List<RawEmployeeImportRow> parse(InputStream inputStream) throws InvalidImportTemplateException;
}
