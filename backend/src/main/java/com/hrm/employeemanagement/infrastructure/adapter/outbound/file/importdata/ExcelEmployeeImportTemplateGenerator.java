package com.hrm.employeemanagement.infrastructure.adapter.outbound.file.importdata;

import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.importdata.EmployeeImportTemplateGenerator;
import com.hrm.employeemanagement.domain.exception.importdata.DataImportException;

/**
 * Bộ chuyển đổi hạ tầng sinh biểu mẫu Excel (.xlsx) chuẩn hóa.
 */
@Component
public class ExcelEmployeeImportTemplateGenerator implements EmployeeImportTemplateGenerator {

    @Override
    public boolean supports(String format) {
        return format == null || "xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format) || "xls".equalsIgnoreCase(format);
    }

    @Override
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("DanhSachNhanVien");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Mã nhân viên (*)",
                    "Họ và tên (*)",
                    "Tên đăng nhập (*)",
                    "Email",
                    "Phòng ban / Đơn vị (*)",
                    "Mã vai trò (VT-01..06)",
                    "Chức danh chuyên môn",
                    "Giờ chuẩn / Tuần",
                    "Ngày bắt đầu (YYYY-MM-DD)",
                    "Ngày kết thúc HĐ (YYYY-MM-DD)",
                    "Thuê ngoài (TRUE/FALSE)"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            Object[][] sampleData = {
                    {"EMP001", "Nguyễn Văn An", "an.nguyen", "an.nguyen@company.com", "Trung tâm Phần mềm", "VT-04", "Frontend Developer", 40, "2026-01-01", "2027-12-31", "FALSE"},
                    {"EMP002", "Trần Thị Bình", "binh.tran", "binh.tran@company.com", "Trung tâm Phần mềm", "VT-04", "Backend Developer", 40, "2026-02-15", "", "FALSE"},
                    {"EMP003", "Lê Hoàng Cường", "cuong.le", "cuong.le@company.com", "Phòng Đảm bảo chất lượng", "VT-04", "QA Engineer", 40, "2026-03-01", "", "TRUE"}
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < sampleData[r].length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = sampleData[r][c];
                    if (val instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(val));
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new DataImportException("Lỗi tạo biểu mẫu Excel: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFilename() {
        return "Bieu_mau_nhap_nhan_vien.xlsx";
    }

    @Override
    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }
}
