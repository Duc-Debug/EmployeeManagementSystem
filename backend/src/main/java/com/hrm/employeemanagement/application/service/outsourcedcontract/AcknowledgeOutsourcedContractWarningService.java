package com.hrm.employeemanagement.application.service.outsourcedcontract;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractCommand;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.AcknowledgeOutsourcedContractResult;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.AcknowledgeOutsourcedContractWarningUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedContractPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Service ghi nhận xử lý cảnh báo hợp đồng thuê ngoài (NCL-14-CN-003 TC-04):
 * - Quản lý nguồn lực (VT-03) hoặc Nhân sự (VT-05) xác nhận ghi nhận/xử lý cảnh báo.
 * - Hệ thống ghi lại người thực hiện, nội dung ghi chú và thời điểm vào audit_logs.
 */
public class AcknowledgeOutsourcedContractWarningService implements AcknowledgeOutsourcedContractWarningUseCase {

    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadOutsourcedContractPort loadContractPort;

    public AcknowledgeOutsourcedContractWarningService(
            GetAuthenticatedUserPort authenticatedUserPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
            SaveAuditLogPort saveAuditLogPort,
            LoadOutsourcedContractPort loadContractPort
    ) {
        this.authenticatedUserPort = Objects.requireNonNull(authenticatedUserPort, "authenticatedUserPort must not be null");
        this.deniedAuditLogPort = Objects.requireNonNull(deniedAuditLogPort, "deniedAuditLogPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "saveAuditLogPort must not be null");
        this.loadContractPort = Objects.requireNonNull(loadContractPort, "loadContractPort must not be null");
    }

    private User checkAuthorization() {
        User currentUser = authenticatedUserPort.getAuthenticatedUser();
        if (currentUser == null) {
            deniedAuditLogPort.save(AuditLog.create(
                    null,
                    "ACCESS_DENIED",
                    "OUTSOURCED_CONTRACT_EXPIRATION",
                    null
            ));
            throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        }

        RoleCode roleCode = currentUser.getRole().getCode();
        if (roleCode != RoleCode.VT_03 && roleCode != RoleCode.VT_05) {
            // [TC-03] Không có quyền -> Ghi nhận nhật ký lần từ chối
            deniedAuditLogPort.save(AuditLog.create(
                    currentUser.getIdValue(),
                    "ACCESS_DENIED",
                    "OUTSOURCED_CONTRACT_EXPIRATION",
                    null
            ));
            throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        }
        return currentUser;
    }

    @Override
    public AcknowledgeOutsourcedContractResult execute(AcknowledgeOutsourcedContractCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.employeeId(), "employeeId must not be null");

        User currentUser = checkAuthorization();

        Employee employee = loadContractPort.findById(command.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên với ID: " + command.employeeId()));

        if (!Boolean.TRUE.equals(employee.getIsOutsourced())) {
            throw new IllegalArgumentException("Nhân viên " + employee.getFullName() + " không phải là nhân sự thuê ngoài");
        }

        // DataScope enforcement: VT-03 (Quản lý chi nhánh) chỉ được xử lý nhân sự thuộc chi nhánh mình phụ trách
        if (currentUser.getRole().getCode() == RoleCode.VT_03 && currentUser.getScopeOrgUnitId() != null) {
            if (!Objects.equals(employee.getOrgUnitId(), currentUser.getScopeOrgUnitId())) {
                deniedAuditLogPort.save(AuditLog.create(
                        currentUser.getIdValue(),
                        "ACCESS_DENIED",
                        "OUTSOURCED_CONTRACT_EXPIRATION",
                        employee.getIdValue()
                ));
                throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
            }
        }

        // [TC-04] Lưu lịch sử: Hệ thống ghi lại người thực hiện, nội dung và thời điểm vào audit_logs
        String note = command.actionNote() != null && !command.actionNote().isBlank()
                ? command.actionNote().trim()
                : "Đã xác nhận theo dõi thời hạn hợp đồng thuê ngoài";

        String details = String.format("note=%s;expectedResolutionDate=%s",
                note, command.expectedResolutionDate() != null ? command.expectedResolutionDate() : "NONE");

        saveAuditLogPort.save(AuditLog.createChange(
                currentUser.getIdValue(),
                "ACKNOWLEDGE_WARNING",
                "OUTSOURCED_CONTRACT_EXPIRATION",
                employee.getIdValue(),
                null,
                details
        ));

        return new AcknowledgeOutsourcedContractResult(
                employee.getIdValue(),
                LocalDateTime.now(),
                "ACKNOWLEDGED",
                "Đã ghi nhận xử lý cảnh báo thời hạn hợp đồng thuê ngoài thành công."
        );
    }
}
