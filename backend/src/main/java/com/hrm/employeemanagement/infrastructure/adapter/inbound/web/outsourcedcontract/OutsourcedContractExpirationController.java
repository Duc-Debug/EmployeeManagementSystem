package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.outsourcedcontract;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractCommand;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractResult;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.ExpiringOutsourcedContractListResult;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.ScanOutsourcedContractsResult;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.AcknowledgeOutsourcedContractWarningUseCase;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.GetExpiringOutsourcedContractsUseCase;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.ScanOutsourcedContractExpirationsUseCase;

@RestController
@RequestMapping({"/api/v1/outsourced-contracts", "/api/outsourced-contracts"})
public class OutsourcedContractExpirationController {

    private final GetExpiringOutsourcedContractsUseCase getExpiringOutsourcedContractsUseCase;
    private final ScanOutsourcedContractExpirationsUseCase scanOutsourcedContractExpirationsUseCase;
    private final AcknowledgeOutsourcedContractWarningUseCase acknowledgeOutsourcedContractWarningUseCase;

    public OutsourcedContractExpirationController(
            GetExpiringOutsourcedContractsUseCase getExpiringOutsourcedContractsUseCase,
            ScanOutsourcedContractExpirationsUseCase scanOutsourcedContractExpirationsUseCase,
            AcknowledgeOutsourcedContractWarningUseCase acknowledgeOutsourcedContractWarningUseCase
    ) {
        this.getExpiringOutsourcedContractsUseCase = Objects.requireNonNull(getExpiringOutsourcedContractsUseCase, "getExpiringOutsourcedContractsUseCase must not be null");
        this.scanOutsourcedContractExpirationsUseCase = Objects.requireNonNull(scanOutsourcedContractExpirationsUseCase, "scanOutsourcedContractExpirationsUseCase must not be null");
        this.acknowledgeOutsourcedContractWarningUseCase = Objects.requireNonNull(acknowledgeOutsourcedContractWarningUseCase, "acknowledgeOutsourcedContractWarningUseCase must not be null");
    }

    /**
     * Lấy danh sách các hợp đồng thuê ngoài sắp hết hạn (trong vòng thresholdDays) hoặc quá hạn,
     * kèm chi tiết các phân bổ dự án bị ảnh hưởng/vắt qua ngày hết hạn theo QTN-21.
     */
    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('VT-03') or hasAuthority('VT-05') or hasRole('VT-03') or hasRole('VT-05') or hasAuthority('RESOURCE_ALLOCATION_MANAGE')")
    public ResponseEntity<ExpiringOutsourcedContractListResult> getExpiringContracts(
            @RequestParam(name = "thresholdDays", defaultValue = "30") Integer thresholdDays
    ) {
        int effectiveThreshold = (thresholdDays != null && thresholdDays > 0)
                ? Math.min(thresholdDays, 365)
                : 30;
        ExpiringOutsourcedContractListResult result = getExpiringOutsourcedContractsUseCase.execute(effectiveThreshold);
        return ResponseEntity.ok(result);
    }

    /**
     * Kích hoạt rà soát thủ công thời hạn hợp đồng thuê ngoài và gửi cảnh báo tới Quản lý nguồn lực (VT-03) và Nhân sự (VT-05).
     */
    @PostMapping("/scan")
    @PreAuthorize("hasAuthority('VT-03') or hasAuthority('VT-05') or hasRole('VT-03') or hasRole('VT-05') or hasAuthority('RESOURCE_ALLOCATION_MANAGE')")
    public ResponseEntity<ScanOutsourcedContractsResult> scanContractsManually() {
        ScanOutsourcedContractsResult result = scanOutsourcedContractExpirationsUseCase.execute(true);
        return ResponseEntity.ok(result);
    }

    /**
     * DTO nhận yêu cầu xác nhận ghi nhận/xử lý cảnh báo hợp đồng từ client.
     */
    public record AcknowledgeRequest(
            String actionNote,
            LocalDate expectedResolutionDate
    ) {}

    /**
     * Xác nhận xử lý cảnh báo thời hạn hợp đồng và ghi nhận nhật ký kiểm toán (TC-04).
     */
    @PostMapping("/{employeeId}/acknowledge")
    @PreAuthorize("hasAuthority('VT-03') or hasAuthority('VT-05') or hasRole('VT-03') or hasRole('VT-05') or hasAuthority('RESOURCE_ALLOCATION_MANAGE')")
    public ResponseEntity<AcknowledgeOutsourcedContractResult> acknowledgeContractWarning(
            @PathVariable("employeeId") Long employeeId,
            @RequestBody(required = false) AcknowledgeRequest request
    ) {
        String note = request != null ? request.actionNote() : null;
        LocalDate resolutionDate = request != null ? request.expectedResolutionDate() : null;

        AcknowledgeOutsourcedContractCommand command = new AcknowledgeOutsourcedContractCommand(
                employeeId, note, resolutionDate
        );
        AcknowledgeOutsourcedContractResult result = acknowledgeOutsourcedContractWarningUseCase.execute(command);
        return ResponseEntity.ok(result);
    }
}
