package com.hrm.employeemanagement.application.port.inbound.allocation.idleness;

import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffCommand;
import com.hrm.employeemanagement.application.dto.allocation.idleness.AcknowledgeProlongedIdleStaffResult;

/**
 * Input Port xác nhận xử lý cảnh báo nhân sự nhàn rỗi (NCL-07-CN-006-TC-04).
 */
public interface AcknowledgeProlongedIdleStaffUseCase {

    AcknowledgeProlongedIdleStaffResult acknowledgeProlongedIdleStaff(AcknowledgeProlongedIdleStaffCommand command);
}
