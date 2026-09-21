package com.hrm.employeemanagement.infrastructure.adapter.outbound.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.hrm.employeemanagement.application.port.outbound.report.excel.GenerateExcelWorkbookPort;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.report.excel.ExcelReportData;
import com.hrm.employeemanagement.domain.report.excel.ExcelReportMetadata;
import com.hrm.employeemanagement.domain.report.excel.ProjectAllocationExcelRow;

/**
 * Adapter triển khai GenerateExcelWorkbookPort sử dụng Apache POI.
 * Tạo bảng tính Excel (.xlsx) chuẩn hóa, chuyên nghiệp và che thông tin nhạy cảm.
 */
public class PoiExcelGeneratorAdapter implements GenerateExcelWorkbookPort {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public byte[] generateProjectAllocationWorkbook(ExcelReportData data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Phân bổ nguồn lực");
            sheet.setDisplayGridlines(true);

            ExcelReportMetadata meta = data.getMetadata();
            List<YearWeek> weeks = meta.targetWeeks();

            // 1. Tạo Fonts & Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);

            Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            CellStyle labelStyle = workbook.createCellStyle();
            labelStyle.setFont(labelFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(headerStyle);

            CellStyle dataCellStyle = workbook.createCellStyle();
            setBorders(dataCellStyle);
            dataCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle numberCellStyle = workbook.createCellStyle();
            setBorders(numberCellStyle);
            numberCellStyle.setAlignment(HorizontalAlignment.RIGHT);
            numberCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            numberCellStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.0"));

            CellStyle centerCellStyle = workbook.createCellStyle();
            setBorders(centerCellStyle);
            centerCellStyle.setAlignment(HorizontalAlignment.CENTER);
            centerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font totalFont = workbook.createFont();
            totalFont.setBold(true);

            CellStyle totalLabelStyle = workbook.createCellStyle();
            totalLabelStyle.setFont(totalFont);
            setBorders(totalLabelStyle);
            totalLabelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle totalNumberStyle = workbook.createCellStyle();
            totalNumberStyle.setFont(totalFont);
            setBorders(totalNumberStyle);
            totalNumberStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalNumberStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalNumberStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalNumberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.0"));

            // 2. Viết tiêu đề báo cáo
            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(meta.reportTitle());
            titleCell.setCellStyle(titleStyle);
            rowIdx++; // Dòng trống

            // 3. Viết khối thông tin dự án & kỳ báo cáo
            writeInfoRow(sheet, rowIdx++, "Mã dự án:", meta.projectCode(), "Tên dự án:", meta.projectName(), labelStyle);
            writeInfoRow(sheet, rowIdx++, "Quản lý dự án (PM):", meta.projectManagerName(), "Kỳ báo cáo:", meta.timeRangeText(), labelStyle);
            writeInfoRow(sheet, rowIdx++, "Người xuất báo cáo:", meta.exportedByUsername(), "Thời điểm xuất:", meta.generatedAt().format(DATE_TIME_FORMATTER), labelStyle);
            rowIdx++; // Dòng trống

            // 4. Viết Header của bảng dữ liệu
            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(26);

            int colIdx = 0;
            createHeaderCell(headerRow, colIdx++, "STT", headerStyle);
            createHeaderCell(headerRow, colIdx++, "Mã nhân viên", headerStyle);
            createHeaderCell(headerRow, colIdx++, "Họ và tên", headerStyle);
            createHeaderCell(headerRow, colIdx++, "Vai trò dự án", headerStyle);
            createHeaderCell(headerRow, colIdx++, "Phòng ban", headerStyle);

            for (YearWeek yw : weeks) {
                createHeaderCell(headerRow, colIdx++, "T" + yw.weekNumber() + "/" + yw.year(), headerStyle);
            }

            createHeaderCell(headerRow, colIdx++, "Tổng giờ (h)", headerStyle);
            createHeaderCell(headerRow, colIdx++, "Mức lương", headerStyle);
            createHeaderCell(headerRow, colIdx++, "Đơn giá chi phí", headerStyle);

            // 5. Viết các dòng dữ liệu
            int stt = 1;
            BigDecimal[] weekTotals = new BigDecimal[weeks.size()];
            for (int i = 0; i < weeks.size(); i++) {
                weekTotals[i] = BigDecimal.ZERO;
            }

            for (ProjectAllocationExcelRow item : data.getRows()) {
                Row dataRow = sheet.createRow(rowIdx++);
                colIdx = 0;

                createCell(dataRow, colIdx++, String.valueOf(stt++), centerCellStyle);
                createCell(dataRow, colIdx++, item.getEmployeeCode(), centerCellStyle);
                createCell(dataRow, colIdx++, item.getFullName(), dataCellStyle);
                createCell(dataRow, colIdx++, item.getProjectRole(), dataCellStyle);
                createCell(dataRow, colIdx++, item.getOrgUnitName(), dataCellStyle);

                for (int i = 0; i < weeks.size(); i++) {
                    YearWeek yw = weeks.get(i);
                    BigDecimal hours = item.getHoursForWeek(yw);
                    weekTotals[i] = weekTotals[i].add(hours);
                    createNumberCell(dataRow, colIdx++, hours.doubleValue(), numberCellStyle);
                }

                createNumberCell(dataRow, colIdx++, item.getTotalHours().doubleValue(), numberCellStyle);
                createCell(dataRow, colIdx++, item.getMaskedSalary(), centerCellStyle);
                createCell(dataRow, colIdx++, item.getMaskedCostRate(), centerCellStyle);
            }

            // 6. Dòng tổng cộng (Summary Row)
            Row totalRow = sheet.createRow(rowIdx++);
            colIdx = 0;
            createCell(totalRow, colIdx++, "", totalLabelStyle);
            createCell(totalRow, colIdx++, "", totalLabelStyle);
            createCell(totalRow, colIdx++, "", totalLabelStyle);
            createCell(totalRow, colIdx++, "", totalLabelStyle);
            createCell(totalRow, colIdx++, "TỔNG CỘNG:", totalLabelStyle);

            for (BigDecimal weekTotal : weekTotals) {
                createNumberCell(totalRow, colIdx++, weekTotal.doubleValue(), totalNumberStyle);
            }

            createNumberCell(totalRow, colIdx++, data.getTotalAllocatedHours().doubleValue(), totalNumberStyle);
            createCell(totalRow, colIdx++, "-", totalLabelStyle);
            createCell(totalRow, colIdx++, "-", totalLabelStyle);

            // 7. Tự động căn độ rộng cột (Auto-size columns)
            int totalColumns = 5 + weeks.size() + 3;
            for (int c = 0; c < totalColumns; c++) {
                sheet.autoSizeColumn(c);
                int currentWidth = sheet.getColumnWidth(c);
                sheet.setColumnWidth(c, Math.min(Math.max(currentWidth + 1024, 3000), 65280));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo tệp bảng tính Excel: " + e.getMessage(), e);
        }
    }

    private void createHeaderCell(Row row, int col, String text, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(text);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int col, String text, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(text != null ? text : "");
        cell.setCellStyle(style);
    }

    private void createNumberCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeInfoRow(Sheet sheet, int rowIdx, String label1, String val1, String label2, String val2, CellStyle labelStyle) {
        Row row = sheet.createRow(rowIdx);
        Cell c0 = row.createCell(0);
        c0.setCellValue(label1);
        c0.setCellStyle(labelStyle);

        Cell c1 = row.createCell(1);
        c1.setCellValue(val1 != null ? val1 : "");

        Cell c3 = row.createCell(3);
        c3.setCellValue(label2);
        c3.setCellStyle(labelStyle);

        Cell c4 = row.createCell(4);
        c4.setCellValue(val2 != null ? val2 : "");
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
