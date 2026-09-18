package com.hrm.employeemanagement.domain.importdata;

import java.time.LocalDate;

/**
 * Đối tượng dữ liệu thô (POJO Record) thu được từ tầng phân tích tệp (Infrastructure Parser).
 * Không chứa logic nghiệp vụ, hoàn toàn độc lập với định dạng tệp vật lý (Excel, CSV, JSON, ...).
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
        Integer standardHoursPerWeek,
        LocalDate startDate,
        LocalDate contractEndDate,
        Boolean isOutsourced
) {
}
