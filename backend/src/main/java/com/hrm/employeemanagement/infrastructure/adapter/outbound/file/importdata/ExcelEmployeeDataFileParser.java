package com.hrm.employeemanagement.infrastructure.adapter.outbound.file.importdata;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
 * sử dụng thư viện Apache POI.
 */
@Component
public class ExcelEmployeeDataFileParser implements EmployeeDataFileParser {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

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

            for (int r = firstRowNum + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowCompletelyEmpty(row)) {
                    continue;
                }

                String employeeCode = getCellValueAsString(row.getCell(0));
                String fullName = getCellValueAsString(row.getCell(1));
                String username = getCellValueAsString(row.getCell(2));
                String email = getCellValueAsString(row.getCell(3));
                String orgUnitIdentifier = getCellValueAsString(row.getCell(4));
                String roleCode = getCellValueAsString(row.getCell(5));
                String professionalRole = getCellValueAsString(row.getCell(6));
                Integer standardHours = getCellValueAsInteger(row.getCell(7));
                LocalDate startDate = getCellValueAsDate(row.getCell(8));
                LocalDate contractEndDate = getCellValueAsDate(row.getCell(9));
                Boolean isOutsourced = getCellValueAsBoolean(row.getCell(10));

                rows.add(new RawEmployeeImportRow(
                        r + 1,
                        employeeCode,
                        fullName,
                        username,
                        email,
                        orgUnitIdentifier,
                        roleCode,
                        professionalRole,
                        standardHours,
                        startDate,
                        contractEndDate,
                        isOutsourced
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
            case STRING -> cell.getStringCellValue().trim();
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

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String str = getCellValueAsString(cell);
        if (str == null || str.isBlank()) return null;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate getCellValueAsDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return new java.sql.Date(date.getTime()).toLocalDate();
        }
        String str = getCellValueAsString(cell);
        if (str == null || str.isBlank()) return null;

        String clean = str.trim();
        for (DateTimeFormatter dtf : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(clean, dtf);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Boolean getCellValueAsBoolean(Cell cell) {
        if (cell == null) return false;
        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        }
        String str = getCellValueAsString(cell);
        if (str == null || str.isBlank()) return false;
        String s = str.trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "1".equals(s) || "có".equals(s) || "yes".equals(s) || "thuê ngoài".equals(s);
    }
}
