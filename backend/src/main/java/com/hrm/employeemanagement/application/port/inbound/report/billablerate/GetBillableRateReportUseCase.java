package com.hrm.employeemanagement.application.port.inbound.report.billablerate;

import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateQuery;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateResult;

public interface GetBillableRateReportUseCase {
    BillableRateResult execute(BillableRateQuery query);
}
