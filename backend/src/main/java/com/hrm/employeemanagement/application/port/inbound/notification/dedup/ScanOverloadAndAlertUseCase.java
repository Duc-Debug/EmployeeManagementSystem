package com.hrm.employeemanagement.application.port.inbound.notification.dedup;

import com.hrm.employeemanagement.application.dto.notification.dedup.OverloadScanResult;

public interface ScanOverloadAndAlertUseCase {
    OverloadScanResult scanAndAlert(int year, int weekNumber);
    OverloadScanResult scanCurrentWeek();
}
