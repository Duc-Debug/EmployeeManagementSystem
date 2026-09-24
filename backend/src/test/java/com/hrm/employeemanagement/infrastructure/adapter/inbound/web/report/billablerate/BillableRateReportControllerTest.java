package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.billablerate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateExport;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateItem;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateResult;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateSummary;
import com.hrm.employeemanagement.application.dto.report.billablerate.DepartmentBillableRateSummary;
import com.hrm.employeemanagement.application.port.inbound.report.billablerate.ExportBillableRateReportUseCase;
import com.hrm.employeemanagement.application.port.inbound.report.billablerate.GetBillableRateReportUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillableRateReportController Unit Tests (NCL-10-CN-002)")
class BillableRateReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetBillableRateReportUseCase getBillableRateReportUseCase;

    @Mock
    private ExportBillableRateReportUseCase exportBillableRateReportUseCase;

    @InjectMocks
    private BillableRateReportController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API Báo cáo tỷ lệ giờ tính phí trả về HTTP 200 OK và cấu trúc dữ liệu chính xác")
    void getBillableRateReport_Success() throws Exception {
        DepartmentBillableRateSummary deptSummary = new DepartmentBillableRateSummary(
                10L, "Phòng Công Nghệ",
                BigDecimal.valueOf(160.0), BigDecimal.valueOf(0.0),
                BigDecimal.valueOf(160.0), BigDecimal.valueOf(120.0),
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(130.0),
                BigDecimal.valueOf(75.0), 1
        );

        BillableRateItem item = new BillableRateItem(
                1L, "EMP001", "Nguyen Van A", 10L, "Phòng Công Nghệ",
                BigDecimal.valueOf(160.0), BigDecimal.valueOf(0.0),
                BigDecimal.valueOf(0.0), BigDecimal.valueOf(160.0),
                BigDecimal.valueOf(120.0), BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(130.0), BigDecimal.valueOf(75.0),
                true, "OPTIMAL"
        );

        BillableRateSummary summary = new BillableRateSummary(
                BigDecimal.valueOf(160.0), BigDecimal.valueOf(0.0),
                BigDecimal.valueOf(0.0), BigDecimal.valueOf(160.0),
                BigDecimal.valueOf(120.0), BigDecimal.valueOf(10.0),
                BigDecimal.valueOf(130.0), BigDecimal.valueOf(75.0),
                1, 1, 4
        );

        BillableRateResult result = new BillableRateResult(
                null, "Toàn công ty",
                2026, 36, 2026, 39,
                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 27),
                List.of(deptSummary), List.of(item),
                summary, true, "Lấy báo cáo tỷ lệ giờ tính phí thành công",
                LocalDateTime.now()
        );

        when(getBillableRateReportUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/reports/billable-rate")
                        .param("fromYear", "2026")
                        .param("fromWeek", "36")
                        .param("toYear", "2026")
                        .param("toWeek", "39"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.overallBillableRate").value(75.0))
                .andExpect(jsonPath("$.data.summary.totalAvailableHours").value(160.0))
                .andExpect(jsonPath("$.data.summary.totalBillableHours").value(120.0))
                .andExpect(jsonPath("$.data.employeeBreakdown[0].employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.data.employeeBreakdown[0].billableRate").value(75.0));
    }

    @Test
    @DisplayName("API Báo cáo tỷ lệ giờ tính phí trả về HTTP 403 khi bị từ chối quyền")
    void getBillableRateReport_Forbidden() throws Exception {
        when(getBillableRateReportUseCase.execute(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.BILLABLE_HOURS_REPORT_READ));

        mockMvc.perform(get("/api/v1/reports/billable-rate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API Xuất CSV trả về byte array kèm Header Content-Disposition phù hợp")
    void exportBillableRateReport_Success() throws Exception {
        byte[] sampleCsv = "header1,header2\nval1,val2".getBytes();
        BillableRateExport export = new BillableRateExport("bao-cao-ty-le-gio-tinh-phi-W36_2026-W39_2026.csv", sampleCsv);

        when(exportBillableRateReportUseCase.export(any())).thenReturn(export);

        mockMvc.perform(get("/api/v1/reports/billable-rate/export")
                        .param("fromYear", "2026")
                        .param("fromWeek", "36")
                        .param("toYear", "2026")
                        .param("toWeek", "39"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"bao-cao-ty-le-gio-tinh-phi-W36_2026-W39_2026.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().bytes(sampleCsv));
    }
}
