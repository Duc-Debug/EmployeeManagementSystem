package com.hrm.employeemanagement.domain.importdata;

/**
 * Đối tượng dữ liệu thô (POJO Record) thu được từ tầng phân tích tệp (Infrastructure Parser).
 * Lưu trữ các giá trị dạng văn bản thô từ tệp nguồn để tầng Validation phân biệt chính xác
 * giữa trường để trống (dùng giá trị mặc định) và trường có giá trị nhưng sai định dạng (báo lỗi).
 */
public record RawEmployeeImportRow(
        int rowNumber,
        String employeeCode,
        String fullName,
        String username,
        String email,
        String orgUnitIdentifier,
        String roleCode,
        String professionalRole,
        String rawStandardHours,
        String rawStartDate,
        String rawContractEndDate,
        String rawIsOutsourced
) {
}
