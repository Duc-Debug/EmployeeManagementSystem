package com.hrm.employeemanagement.infrastructure.adapter.outbound.file.importdata;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeDataFileParser;
import com.hrm.employeemanagement.domain.exception.importdata.DataImportException;
import com.hrm.employeemanagement.domain.exception.importdata.InvalidImportTemplateException;
import com.hrm.employeemanagement.domain.importdata.RawEmployeeImportRow;

/**
 * Bộ chuyển đổi hạ tầng (Infrastructure Adapter) đọc và phân tích tệp Excel (.xlsx, .xls)
 * sử dụng thư viện Apache POI với các lớp bảo vệ chống Zip-bomb và giới hạn tài nguyên.
 */
@Component
public class ExcelEmployeeDataFileParser implements EmployeeDataFileParser {

    public static final int MAX_IMPORT_ROWS = 2000;

    static {
        // POI Zip Security Safeguards
        try {
            ZipSecureFile.setMinInflateRatio(0.01);
            ZipSecureFile.setMaxEntrySize(50L * 1024 * 1024); // 50MB
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean supports(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    @Override
    public List<RawEmployeeImportRow> parse(InputStream inputStream) throws InvalidImportTemplateException {
        List<RawEmployeeImportRow> rows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new InvalidImportTemplateException("Tệp Excel không chứa trang tính (Sheet) nào hợp lệ.");
            }

            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();

            if (lastRowNum < firstRowNum) {
                throw new InvalidImportTemplateException("Tệp Excel trống không có dữ liệu.");
            }

            Row headerRow = sheet.getRow(firstRowNum);
            if (headerRow == null) {
                throw new InvalidImportTemplateException("Không tìm thấy dòng tiêu đề trong tệp Excel.");
            }

            validateHeader(headerRow);

            int dataRowCount = 0;
            for (int r = firstRowNum + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowCompletelyEmpty(row)) {
                    continue;
                }

                dataRowCount++;
                if (dataRowCount > MAX_IMPORT_ROWS) {
                    throw new InvalidImportTemplateException(
                            "Tệp dữ liệu vượt quá giới hạn tối đa cho phép (" + MAX_IMPORT_ROWS + " dòng). Vui lòng chia nhỏ tệp để xử lý."
                    );
                }

                String employeeCode = getCellValueAsString(row.getCell(0));
                String fullName = getCellValueAsString(row.getCell(1));
                String username = getCellValueAsString(row.getCell(2));
                String email = getCellValueAsString(row.getCell(3));
                String orgUnitIdentifier = getCellValueAsString(row.getCell(4));
                String roleCode = getCellValueAsString(row.getCell(5));
                String professionalRole = getCellValueAsString(row.getCell(6));
                String rawStandardHours = getCellValueAsString(row.getCell(7));
                String rawStartDate = getCellValueAsString(row.getCell(8));
                String rawContractEndDate = getCellValueAsString(row.getCell(9));
                String rawIsOutsourced = getCellValueAsString(row.getCell(10));

                rows.add(new RawEmployeeImportRow(
                        r + 1,
                        employeeCode,
                        fullName,
                        username,
                        email,
                        orgUnitIdentifier,
                        roleCode,
                        professionalRole,
                        rawStandardHours,
                        rawStartDate,
                        rawContractEndDate,
                        rawIsOutsourced
                ));
            }

        } catch (InvalidImportTemplateException e) {
            throw e;
        } catch (Exception e) {
            throw new DataImportException("Lỗi đọc cấu trúc tệp Excel: " + e.getMessage(), e);
        }

        return rows;
    }

    private void validateHeader(Row headerRow) {
        String col0 = getCellValueAsString(headerRow.getCell(0));
        String col1 = getCellValueAsString(headerRow.getCell(1));
        String col2 = getCellValueAsString(headerRow.getCell(2));
        String col4 = getCellValueAsString(headerRow.getCell(4));

        boolean match = (col0 != null && (col0.contains("Mã") || col0.contains("Code") || col0.contains("ID")))
                && (col1 != null && (col1.contains("Họ") || col1.contains("Tên") || col1.contains("Name")))
                && (col2 != null && (col2.contains("đăng nhập") || col2.contains("username") || col2.contains("Tên")))
                && (col4 != null && (col4.contains("Phòng") || col4.contains("Đơn vị") || col4.contains("Dept") || col4.contains("Unit")));

        if (!match) {
            throw new InvalidImportTemplateException(
                    "Cấu trúc tiêu đề tệp Excel không đúng biểu mẫu chuẩn. "
                    + "Các cột bắt buộc: [0: Mã nhân viên, 1: Họ và tên, 2: Tên đăng nhập, 3: Email, 4: Phòng ban / Đơn vị, ...]"
            );
        }
    }

    private boolean isRowCompletelyEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellValueAsString(cell);
                if (val != null && !val.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> {
                String s = cell.getStringCellValue().trim();
                yield s.isEmpty() ? null : s;
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate d = cell.getLocalDateTimeCellValue().toLocalDate();
                    yield d.format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    yield String.valueOf((long) num);
                }
                yield String.valueOf(num);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> null;
        };
    }
}
