package com.hrm.employeemanagement.application.service.outsourcedcontract;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.hrm.employeemanagement.application.dto.notification.CreateNotificationEventCommand;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.ScanOutsourcedContractsResult;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.ScanOutsourcedContractExpirationsUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadNotificationRecipientUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedAllocationPort.OutsourcedAllocationRecord;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedContractPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.outsourcedcontract.ExpiringOutsourcedContract;
import com.hrm.employeemanagement.domain.outsourcedcontract.OutsourcedContractAffectedAllocation;
import com.hrm.employeemanagement.domain.outsourcedcontract.OutsourcedContractExpirationPolicy;
import com.hrm.employeemanagement.domain.outsourcedcontract.OutsourcedContractStatus;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Service thực thi use case rà soát thời hạn hợp đồng thuê ngoài (NCL-14-CN-003):
 * - Rà soát tự động định kỳ hoặc thủ công.
 * - Phát hiện hợp đồng sắp hết hạn trong 30 ngày (ví dụ 25 ngày theo TC-01).
 * - Phát hiện các phân bổ vắt qua ngày hết hạn theo QTN-21.
 * - Gửi cảnh báo tới Quản lý nguồn lực (VT-03) và Nhân sự (VT-05) với cơ chế chống trùng QTN-19.
 * - TC-02: Không gửi cảnh báo khi không có hợp đồng sắp hết hạn.
 * - TC-03: Chặn và ghi log khi người dùng không có quyền gọi rà soát thủ công.
 * - TC-04: Ghi nhật ký kiểm toán khi thực hiện rà soát.
 */
public class ScanOutsourcedContractExpirationsService implements ScanOutsourcedContractExpirationsUseCase {

    private static final int MANUAL_SCAN_COOLDOWN_SECONDS = 10;

    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadOutsourcedContractPort loadContractPort;
    private final LoadOutsourcedAllocationPort loadAllocationPort;
    private final LoadNotificationRecipientUserPort recipientUserPort;
    private final CreateNotificationEventUseCase createNotificationEventUseCase;

    private final AtomicReference<LocalDateTime> lastManualScanTime = new AtomicReference<>(null);
    private final AtomicReference<ScanOutsourcedContractsResult> lastManualScanResult = new AtomicReference<>(null);

    public ScanOutsourcedContractExpirationsService(
            GetAuthenticatedUserPort authenticatedUserPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
            SaveAuditLogPort saveAuditLogPort,
            LoadOutsourcedContractPort loadContractPort,
            LoadOutsourcedAllocationPort loadAllocationPort,
            LoadNotificationRecipientUserPort recipientUserPort,
            CreateNotificationEventUseCase createNotificationEventUseCase
    ) {
        this.authenticatedUserPort = Objects.requireNonNull(authenticatedUserPort, "authenticatedUserPort must not be null");
        this.deniedAuditLogPort = Objects.requireNonNull(deniedAuditLogPort, "deniedAuditLogPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "saveAuditLogPort must not be null");
        this.loadContractPort = Objects.requireNonNull(loadContractPort, "loadContractPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "loadAllocationPort must not be null");
        this.recipientUserPort = Objects.requireNonNull(recipientUserPort, "recipientUserPort must not be null");
        this.createNotificationEventUseCase = Objects.requireNonNull(createNotificationEventUseCase, "createNotificationEventUseCase must not be null");
    }

    private User checkAuthorizationForManualTrigger() {
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
    public ScanOutsourcedContractsResult execute(boolean isManualTrigger) {
        User executingUser = null;
        if (isManualTrigger) {
            executingUser = checkAuthorizationForManualTrigger();

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastScan = lastManualScanTime.get();
            if (lastScan != null) {
                long elapsedSeconds = Duration.between(lastScan, now).getSeconds();
                if (elapsedSeconds < MANUAL_SCAN_COOLDOWN_SECONDS) {
                    ScanOutsourcedContractsResult cached = lastManualScanResult.get();
                    if (cached != null) {
                        return new ScanOutsourcedContractsResult(
                                cached.scannedAt(),
                                cached.totalScanned(),
                                cached.totalExpiringContractsFound(),
                                0,
                                0,
                                String.format("Hệ thống vừa rà soát cách đây %d giây (vui lòng đợi tối thiểu %d giây giữa 2 lần quét thủ công).",
                                        elapsedSeconds, MANUAL_SCAN_COOLDOWN_SECONDS)
                        );
                    }
                }
            }
        }

        LocalDate today = LocalDate.now();
        List<Employee> outsourcedEmployees = loadContractPort.findAllOutsourcedEmployeesWithContract();

        // DataScope enforcement: VT-03 (Quản lý chi nhánh) chỉ rà soát nhân sự thuộc chi nhánh mình phụ trách
        if (isManualTrigger && executingUser != null
                && executingUser.getRole().getCode() == RoleCode.VT_03
                && executingUser.getScopeOrgUnitId() != null) {
            Long scopeOrgUnitId = executingUser.getScopeOrgUnitId();
            outsourcedEmployees = outsourcedEmployees.stream()
                    .filter(e -> Objects.equals(e.getOrgUnitId(), scopeOrgUnitId))
                    .toList();
        }

        int totalScanned = outsourcedEmployees.size();

        if (totalScanned == 0) {
            // [TC-02] Dữ liệu rỗng: Không có hợp đồng nào
            ScanOutsourcedContractsResult emptyResult = new ScanOutsourcedContractsResult(
                    LocalDateTime.now(), 0, 0, 0, 0,
                    "Không tìm thấy nhân sự thuê ngoài nào có hợp đồng trong hệ thống."
            );
            if (isManualTrigger) {
                recordManualScanAudit(executingUser, 0, 0, 0, 0);
                lastManualScanTime.set(LocalDateTime.now());
                lastManualScanResult.set(emptyResult);
            }
            return emptyResult;
        }

        List<Long> employeeIds = outsourcedEmployees.stream()
                .map(Employee::getIdValue)
                .filter(Objects::nonNull)
                .toList();

        List<Long> orgUnitIds = outsourcedEmployees.stream()
                .map(Employee::getOrgUnitId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> orgUnitNames = loadContractPort.findOrgUnitNamesByIds(orgUnitIds);
        List<OutsourcedAllocationRecord> allAllocations = loadAllocationPort.findAllocationsByEmployeeIds(employeeIds);

        List<Long> projectIds = allAllocations.stream()
                .map(OutsourcedAllocationRecord::projectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> projectNames = loadAllocationPort.findProjectNamesByIds(projectIds);

        List<ExpiringOutsourcedContract> expiringContracts = new ArrayList<>();

        for (Employee employee : outsourcedEmployees) {
            LocalDate contractEndDate = employee.getContractEndDate();
            if (contractEndDate == null) {
                continue;
            }

            long daysRemaining = OutsourcedContractExpirationPolicy.calculateDaysRemaining(contractEndDate, today);
            OutsourcedContractStatus status = OutsourcedContractExpirationPolicy.evaluateStatus(
                    contractEndDate, today, OutsourcedContractExpirationPolicy.DEFAULT_WARNING_THRESHOLD_DAYS
            );

            List<OutsourcedAllocationRecord> empAllocations = allAllocations.stream()
                    .filter(a -> Objects.equals(a.employeeId(), employee.getIdValue()))
                    .toList();

            List<OutsourcedContractAffectedAllocation> affectedAllocations = new ArrayList<>();
            for (OutsourcedAllocationRecord alloc : empAllocations) {
                String projectName = projectNames.getOrDefault(alloc.projectId(), "Dự án #" + alloc.projectId());
                Optional<OutsourcedContractAffectedAllocation> impactOpt = OutsourcedContractExpirationPolicy.evaluateAllocationImpact(
                        contractEndDate, alloc.yearWeek(), alloc.allocatedHours(), alloc.allocationId(), alloc.projectId(), projectName
                );
                impactOpt.ifPresent(affectedAllocations::add);
            }

            if (OutsourcedContractExpirationPolicy.isContractRequiringWarning(status, affectedAllocations)) {
                String orgUnitName = employee.getOrgUnitId() != null
                        ? orgUnitNames.getOrDefault(employee.getOrgUnitId(), "N/A")
                : "N/A";

                expiringContracts.add(new ExpiringOutsourcedContract(
                        employee.getIdValue(),
                        employee.getEmployeeCode(),
                        employee.getFullName(),
                        employee.getProfessionalRole(),
                        employee.getOrgUnitId(),
                        orgUnitName,
                        employee.getStartDate(),
                        contractEndDate,
                        daysRemaining,
                        status,
                        affectedAllocations
                ));
            }
        }

        // Ưu tiên hợp đồng quá hạn và sắp hết hạn gấp nhất lên đầu danh sách
        expiringContracts.sort(java.util.Comparator.comparingLong(ExpiringOutsourcedContract::getDaysRemaining));

        // [TC-02] Dữ liệu rỗng: Không có hợp đồng thuê nào sắp hết hạn
        if (expiringContracts.isEmpty()) {
            ScanOutsourcedContractsResult emptyExpiringResult = new ScanOutsourcedContractsResult(
                    LocalDateTime.now(), totalScanned, 0, 0, 0,
                    "Không có hợp đồng thuê ngoài nào sắp hết hạn trong vòng 30 ngày tới. Hệ thống không gửi cảnh báo nào."
            );
            if (isManualTrigger) {
                recordManualScanAudit(executingUser, totalScanned, 0, 0, 0);
                lastManualScanTime.set(LocalDateTime.now());
                lastManualScanResult.set(emptyExpiringResult);
            }
            return emptyExpiringResult;
        }

        // [TC-01] Gửi thông báo tới Quản lý nguồn lực (VT-03) và Nhân sự (VT-05)
        List<Long> recipientUserIds = recipientUserPort.findResourceManagersAndHrUserIds();
        int notificationEventsCreated = 0;
        int notificationsSent = 0;

        for (ExpiringOutsourcedContract contract : expiringContracts) {
            String title = String.format(
                    "Cảnh báo: Hợp đồng thuê ngoài sắp hết hạn - %s (%s)",
                    contract.getFullName(), contract.getEmployeeCode()
            );

            StringBuilder msg = new StringBuilder();
            if (contract.getDaysRemaining() < 0) {
                msg.append(String.format("Hợp đồng thuê ngoài của nhân sự %s (%s) đã hết hạn vào ngày %s (quá hạn %d ngày). ",
                        contract.getFullName(), contract.getEmployeeCode(), contract.getContractEndDate(), Math.abs(contract.getDaysRemaining())));
            } else {
                msg.append(String.format("Hợp đồng thuê ngoài của nhân sự %s (%s) sẽ hết hạn vào ngày %s (còn %d ngày). ",
                        contract.getFullName(), contract.getEmployeeCode(), contract.getContractEndDate(), contract.getDaysRemaining()));
            }

            if (contract.hasAffectedAllocations()) {
                msg.append(String.format("Phát hiện %d phân bổ bị ảnh hưởng hoặc vắt qua ngày hết hạn vi phạm quy tắc QTN-21: ",
                        contract.countAffectedAllocations()));
                for (OutsourcedContractAffectedAllocation alloc : contract.getAffectedAllocations()) {
                    msg.append(String.format("[%s, Tuần %02d/%d: %.1fh (%s)] ",
                            alloc.projectName(), alloc.yearWeek().weekNumber(), alloc.yearWeek().year(),
                            alloc.allocatedHours(), alloc.affectedType().name()));
                }
            } else {
                msg.append("Hiện chưa có phân bổ nào bị ảnh hưởng sau ngày hết hạn.");
            }

            // QTN-19: Idempotency source_event_key đảm bảo không bắn thông báo trùng khi quét nhiều lần
            String sourceEventKey = String.format("OUTSOURCED_CONTRACT_EXPIRY_%d_%s",
                    contract.getEmployeeId(), contract.getContractEndDate());

            CreateNotificationEventCommand notifCmd = new CreateNotificationEventCommand(
                    "OUTSOURCED_CONTRACT_EXPIRING_WARNING",
                    com.hrm.employeemanagement.domain.notification.NotificationLevel.CAO,
                    title,
                    msg.toString().trim(),
                    "EMPLOYEE",
                    String.valueOf(contract.getEmployeeId()),
                    sourceEventKey,
                    recipientUserIds
            );

            createNotificationEventUseCase.execute(notifCmd);
            notificationEventsCreated++;
            notificationsSent += (recipientUserIds != null ? recipientUserIds.size() : 0);
        }

        // [TC-04] Lưu lịch sử thao tác rà soát nếu là kích hoạt thủ công
        if (isManualTrigger) {
            recordManualScanAudit(executingUser, totalScanned, expiringContracts.size(), notificationEventsCreated, notificationsSent);
        }

        String details = String.format(
                "Đã rà soát %d nhân sự thuê ngoài, phát hiện %d hợp đồng sắp hết hạn/quá hạn, tạo %d sự kiện cảnh báo và gửi %d thông báo cho Quản lý nguồn lực và Nhân sự.",
                totalScanned, expiringContracts.size(), notificationEventsCreated, notificationsSent
        );

        ScanOutsourcedContractsResult finalResult = new ScanOutsourcedContractsResult(
                LocalDateTime.now(), totalScanned, expiringContracts.size(), notificationEventsCreated, notificationsSent, details
        );

        if (isManualTrigger) {
            lastManualScanTime.set(LocalDateTime.now());
            lastManualScanResult.set(finalResult);
        }

        return finalResult;
    }

    private void recordManualScanAudit(
            User executingUser,
            int totalScanned,
            int expiringFound,
            int notificationEventsCreated,
            int notificationsSent
    ) {
        if (executingUser != null) {
            saveAuditLogPort.save(AuditLog.createChange(
                    executingUser.getIdValue(),
                    "MANUAL_SCAN",
                    "OUTSOURCED_CONTRACT_EXPIRATION",
                    null,
                    null,
                    String.format("totalScanned=%d;expiringFound=%d;notificationEventsCreated=%d;notificationsSent=%d",
                            totalScanned, expiringFound, notificationEventsCreated, notificationsSent)
            ));
        }
    }
}
