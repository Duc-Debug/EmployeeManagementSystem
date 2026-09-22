package com.hrm.employeemanagement.application.dto.backup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RestoreBackupRequest {
    @NotBlank(message = "Mã xác nhận không được để trống")
    private String confirmationCode; // Must be "RESTORE"

    @NotBlank(message = "Lý do phục hồi không được để trống")
    @Size(min = 10, max = 500, message = "Lý do phục hồi phải từ 10 đến 500 ký tự")
    private String reason;

    public RestoreBackupRequest() {
    }

    public RestoreBackupRequest(String confirmationCode, String reason) {
        this.confirmationCode = confirmationCode;
        this.reason = reason;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
