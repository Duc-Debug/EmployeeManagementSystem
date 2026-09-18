package com.hrm.employeemanagement.infrastructure.adapter.outbound.file.importdata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.hrm.employeemanagement.domain.exception.importdata.DataImportException;
import com.hrm.employeemanagement.domain.exception.importdata.InvalidImportTemplateException;
import com.hrm.employeemanagement.domain.importdata.RawEmployeeImportRow;

@DisplayName("ExcelEmployeeDataFileParser Unit Tests (Infrastructure Adapter)")
class ExcelEmployeeDataFileParserTest {

    private ExcelEmployeeDataFileParser parser;

    @BeforeEach
    void setUp() {
        parser = new ExcelEmployeeDataFileParser();
    }

    @Test
    @DisplayName("Kiểm tra định dạng file hỗ trợ .xlsx và .xls")
    void testSupports() {
        assertTrue(parser.supports("danh_sach.xlsx"));
        assertTrue(parser.supports("danh_sach.XLSX"));
        assertTrue(parser.supports("danh_sach.xls"));
        assertFalse(parser.supports("danh_sach.csv"));
        assertFalse(parser.supports("danh_sach.pdf"));
        assertFalse(parser.supports(null));
    }

    @Test
    @DisplayName("Phân tích tệp Excel hợp lệ -> Trả về danh sách RawEmployeeImportRow chính xác")
    void testParse_ValidExcelFile() throws Exception {
        byte[] excelBytes = createSampleWorkbook(new Object[][]{
                {"EMP001", "Nguyễn Văn A", "an.nguyen", "an.nguyen@test.com", "Trung tâm Phần mềm", "VT-04", "Developer", 40, "2026-01-01", "2027-12-31", "FALSE"},
                {"EMP002", "Trần Thị B", "binh.tran", "binh.tran@test.com", "10", "VT-04", "QA", 40, "2026-02-01", "", "TRUE"}
        });

        InputStream is = new ByteArrayInputStream(excelBytes);
        List<RawEmployeeImportRow> rows = parser.parse(is);

        assertNotNull(rows);
        assertEquals(2, rows.size());

        RawEmployeeImportRow row1 = rows.get(0);
        assertEquals(2, row1.rowNumber());
        assertEquals("EMP001", row1.employeeCode());
        assertEquals("Nguyễn Văn A", row1.fullName());
        assertEquals("an.nguyen", row1.username());
        assertEquals("an.nguyen@test.com", row1.email());
        assertEquals("Trung tâm Phần mềm", row1.orgUnitIdentifier());
        assertEquals("40", row1.rawStandardHours());
        assertEquals("2026-01-01", row1.rawStartDate());
        assertEquals("2027-12-31", row1.rawContractEndDate());
        assertEquals("FALSE", row1.rawIsOutsourced());

        RawEmployeeImportRow row2 = rows.get(1);
        assertEquals("EMP002", row2.employeeCode());
        assertNull(row2.rawContractEndDate());
        assertEquals("TRUE", row2.rawIsOutsourced());
    }

    @Test
    @DisplayName("Tệp Excel sai tiêu đề cột -> Ném InvalidImportTemplateException")
    void testParse_InvalidHeader_ThrowsException() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Cột Tự Do 1");
            header.createCell(1).setCellValue("Cột Tự Do 2");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            byte[] bytes = baos.toByteArray();

            InputStream is = new ByteArrayInputStream(bytes);
            assertThrows(InvalidImportTemplateException.class, () -> parser.parse(is));
        }
    }

    @Test
    @DisplayName("Tệp Excel vượt quá giới hạn MAX_IMPORT_ROWS (2000 dòng) -> Ném DataImportException")
    void testParse_ExceedsMaxRows_ThrowsException() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Employees");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "Mã nhân viên", "Họ và tên", "Tên đăng nhập", "Email",
                    "Phòng ban / Đơn vị", "Mã vai trò", "Chức danh", "Giờ chuẩn",
                    "Ngày bắt đầu", "Ngày kết thúc HĐ", "Thuê ngoài"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // Tạo 2001 dòng dữ liệu
            for (int r = 1; r <= 2001; r++) {
                Row row = sheet.createRow(r);
                row.createCell(0).setCellValue("EMP" + r);
                row.createCell(1).setCellValue("Employee " + r);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            byte[] bytes = baos.toByteArray();

            InputStream is = new ByteArrayInputStream(bytes);
            InvalidImportTemplateException ex = assertThrows(InvalidImportTemplateException.class, () -> parser.parse(is));
            assertTrue(ex.getMessage().contains("vượt quá giới hạn tối đa cho phép (2000 dòng)"));
        }
    }

    private byte[] createSampleWorkbook(Object[][] data) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Employees");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "Mã nhân viên", "Họ và tên", "Tên đăng nhập", "Email",
                    "Phòng ban / Đơn vị", "Mã vai trò", "Chức danh", "Giờ chuẩn",
                    "Ngày bắt đầu", "Ngày kết thúc HĐ", "Thuê ngoài"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < data[r].length; c++) {
                    Object val = data[r][c];
                    if (val instanceof Number n) {
                        row.createCell(c).setCellValue(n.doubleValue());
                    } else {
                        row.createCell(c).setCellValue(String.valueOf(val));
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}