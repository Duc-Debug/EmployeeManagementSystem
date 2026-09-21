package com.hrm.employeemanagement.infrastructure.adapter.outbound.excel;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.report.excel.ExcelReportData;
import com.hrm.employeemanagement.domain.report.excel.ExcelReportMetadata;
import com.hrm.employeemanagement.domain.report.excel.ProjectAllocationExcelRow;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PoiExcelGeneratorAdapter Unit Tests")
class PoiExcelGeneratorAdapterTest {

    private final PoiExcelGeneratorAdapter adapter = new PoiExcelGeneratorAdapter();

    @Test
    @DisplayName("Xuất workbook Excel .xlsx thành công với đầy đủ cấu trúc header, dữ liệu và summary")
    void testGenerateWorkbook_Success() throws Exception {
        YearWeek w1 = YearWeek.of(2026, 1);
        YearWeek w2 = YearWeek.of(2026, 2);
        List<YearWeek> weeks = List.of(w1, w2);

        ExcelReportMetadata meta = new ExcelReportMetadata(
                "BÁO CÁO PHÂN BỔ NGUỒN LỰC DỰ ÁN",
                1L,
                "PRJ-001",
                "Hệ thống HRM Core",
                "Nguyễn Văn PM",
                "pm_user",
                "Từ tuần T01/2026 đến tuần T02/2026",
                weeks,
                LocalDateTime.now()
        );

        ProjectAllocationExcelRow row1 = new ProjectAllocationExcelRow(
                101L,
                "EMP001",
                "Trần Văn Dev",
                "Backend Developer",
                "Phòng Công nghệ",
                Map.of(w1, BigDecimal.valueOf(20.0), w2, BigDecimal.valueOf(30.0)),
                BigDecimal.valueOf(50.0),
                "***",
                "***"
        );

        ExcelReportData data = new ExcelReportData(meta, List.of(row1), BigDecimal.valueOf(50.0));

        byte[] bytes = adapter.generateProjectAllocationWorkbook(data);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // Đọc lại workbook và xác thực các ô
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Phân bổ nguồn lực");
            assertNotNull(sheet);

            // Kiểm tra dòng tiêu đề
            Row titleRow = sheet.getRow(0);
            assertEquals("BÁO CÁO PHÂN BỔ NGUỒN LỰC DỰ ÁN", titleRow.getCell(0).getStringCellValue());

            // Tìm dòng header bảng dữ liệu (row index 6)
            Row headerRow = sheet.getRow(6);
            assertNotNull(headerRow);
            assertEquals("STT", headerRow.getCell(0).getStringCellValue());
            assertEquals("Mã nhân viên", headerRow.getCell(1).getStringCellValue());
            assertEquals("Họ và tên", headerRow.getCell(2).getStringCellValue());
            assertEquals("Vai trò dự án", headerRow.getCell(3).getStringCellValue());
            assertEquals("Phòng ban", headerRow.getCell(4).getStringCellValue());
            assertEquals("T1/2026", headerRow.getCell(5).getStringCellValue());
            assertEquals("T2/2026", headerRow.getCell(6).getStringCellValue());
            assertEquals("Tổng giờ (h)", headerRow.getCell(7).getStringCellValue());
            assertEquals("Mức lương", headerRow.getCell(8).getStringCellValue());
            assertEquals("Đơn giá chi phí", headerRow.getCell(9).getStringCellValue());

            // Dòng dữ liệu đầu tiên (row index 7)
            Row dataRow = sheet.getRow(7);
            assertNotNull(dataRow);
            assertEquals("1", dataRow.getCell(0).getStringCellValue());
            assertEquals("EMP001", dataRow.getCell(1).getStringCellValue());
            assertEquals("Trần Văn Dev", dataRow.getCell(2).getStringCellValue());
            assertEquals(20.0, dataRow.getCell(5).getNumericCellValue());
            assertEquals(30.0, dataRow.getCell(6).getNumericCellValue());
            assertEquals(50.0, dataRow.getCell(7).getNumericCellValue());
            assertEquals("***", dataRow.getCell(8).getStringCellValue());
            assertEquals("***", dataRow.getCell(9).getStringCellValue());

            // Dòng tổng cộng (row index 8)
            Row totalRow = sheet.getRow(8);
            assertNotNull(totalRow);
            assertEquals("TỔNG CỘNG:", totalRow.getCell(4).getStringCellValue());
            assertEquals(20.0, totalRow.getCell(5).getNumericCellValue());
            assertEquals(30.0, totalRow.getCell(6).getNumericCellValue());
            assertEquals(50.0, totalRow.getCell(7).getNumericCellValue());
        }
    }
}
