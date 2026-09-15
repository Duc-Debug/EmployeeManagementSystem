package com.hrm.employeemanagement.application.port.inbound.allocation.idleness;

import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessQuery;
import com.hrm.employeemanagement.application.dto.allocation.idleness.ProlongedIdlenessReportResult;

/**
 * Input Port rà soát và trả về danh sách nhân sự nhàn rỗi kéo dài (NCL-07-CN-006 / QTN-23).
 */
public interface GetProlongedIdleStaffUseCase {

    ProlongedIdlenessReportResult getProlongedIdleStaff(ProlongedIdlenessQuery query);
}
