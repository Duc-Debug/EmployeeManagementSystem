package com.hrm.employeemanagement.application.port.inbound.report.billablerate;

import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateExport;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateQuery;

public interface ExportBillableRateReportUseCase {
    BillableRateExport export(BillableRateQuery query);
}
