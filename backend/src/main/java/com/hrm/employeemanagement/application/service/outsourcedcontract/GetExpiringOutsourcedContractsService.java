package com.hrm.employeemanagement.application.service.outsourcedcontract;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.ExpiringOutsourcedContractListResult;
import com.hrm.employeemanagement.application.dto.outsourcedcontract.ExpiringOutsourcedContractResult;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.GetExpiringOutsourcedContractsUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedAllocationPort.OutsourcedAllocationRecord;
import com.hrm.employeemanagement.application.port.outbound.outsourcedcontract.LoadOutsourcedContractPort;
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
 * Service thực thi use case lấy danh sách hợp đồng thuê ngoài sắp hết hạn hoặc quá hạn
 * kèm theo chi tiết các dòng phân bổ bị ảnh hưởng theo QTN-21.
 * Tuân thủ kiểm tra quyền VT-03 (Quản lý nguồn lực) và VT-05 (Nhân sự), ghi audit log khi bị từ chối (TC-03).
 */
public class GetExpiringOutsourcedContractsService implements GetExpiringOutsourcedContractsUseCase {

    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;
    private final LoadOutsourcedContractPort loadContractPort;
    private final LoadOutsourcedAllocationPort loadAllocationPort;

    public GetExpiringOutsourcedContractsService(
            GetAuthenticatedUserPort authenticatedUserPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
            LoadOutsourcedContractPort loadContractPort,
            LoadOutsourcedAllocationPort loadAllocationPort
    ) {
        this.authenticatedUserPort = Objects.requireNonNull(authenticatedUserPort, "authenticatedUserPort must not be null");
        this.deniedAuditLogPort = Objects.requireNonNull(deniedAuditLogPort, "deniedAuditLogPort must not be null");
        this.loadContractPort = Objects.requireNonNull(loadContractPort, "loadContractPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "loadAllocationPort must not be null");
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
            // [TC-03] Người dùng không phải Quản lý nguồn lực hoặc Nhân sự -> Ghi audit log và từ chối truy cập
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
    public ExpiringOutsourcedContractListResult execute(Integer thresholdDays) {
        User currentUser = checkAuthorization();

        int threshold = (thresholdDays != null && thresholdDays > 0)
                ? Math.min(thresholdDays, 365)
                : OutsourcedContractExpirationPolicy.DEFAULT_WARNING_THRESHOLD_DAYS;

        List<Employee> outsourcedEmployees = loadContractPort.findAllOutsourcedEmployeesWithContract();
        if (outsourcedEmployees.isEmpty()) {
            return ExpiringOutsourcedContractListResult.of(Collections.emptyList());
        }

        // DataScope enforcement: VT-03 (Quản lý chi nhánh) chỉ xem nhân sự thuộc chi nhánh mình phụ trách
        if (currentUser.getRole().getCode() == RoleCode.VT_03 && currentUser.getScopeOrgUnitId() != null) {
            outsourcedEmployees = outsourcedEmployees.stream()
                    .filter(e -> Objects.equals(e.getOrgUnitId(), currentUser.getScopeOrgUnitId()))
                    .toList();
            if (outsourcedEmployees.isEmpty()) {
                return ExpiringOutsourcedContractListResult.of(Collections.emptyList());
            }
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

        LocalDate today = LocalDate.now();
        List<ExpiringOutsourcedContractResult> results = new ArrayList<>();

        for (Employee employee : outsourcedEmployees) {
            LocalDate contractEndDate = employee.getContractEndDate();
            if (contractEndDate == null) {
                continue;
            }

            long daysRemaining = OutsourcedContractExpirationPolicy.calculateDaysRemaining(contractEndDate, today);
            OutsourcedContractStatus status = OutsourcedContractExpirationPolicy.evaluateStatus(contractEndDate, today, threshold);

            // Tìm các phân bổ của nhân viên này
            List<OutsourcedAllocationRecord> employeeAllocations = allAllocations.stream()
                    .filter(a -> Objects.equals(a.employeeId(), employee.getIdValue()))
                    .toList();

            List<OutsourcedContractAffectedAllocation> affectedAllocations = new ArrayList<>();
            for (OutsourcedAllocationRecord alloc : employeeAllocations) {
                String projectName = projectNames.getOrDefault(alloc.projectId(), "Dự án #" + alloc.projectId());
                Optional<OutsourcedContractAffectedAllocation> impactOpt = OutsourcedContractExpirationPolicy.evaluateAllocationImpact(
                        contractEndDate, alloc.yearWeek(), alloc.allocatedHours(), alloc.allocationId(), alloc.projectId(), projectName
                );
                impactOpt.ifPresent(affectedAllocations::add);
            }

            // Chỉ đưa vào danh sách nếu hợp đồng sắp hết hạn (<= threshold), đã hết hạn, hoặc có phân bổ bị ảnh hưởng
            if (OutsourcedContractExpirationPolicy.isContractRequiringWarning(status, affectedAllocations)) {
                String orgUnitName = employee.getOrgUnitId() != null
                        ? orgUnitNames.getOrDefault(employee.getOrgUnitId(), "N/A")
                        : "N/A";

                ExpiringOutsourcedContract contract = new ExpiringOutsourcedContract(
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
                );
                results.add(ExpiringOutsourcedContractResult.fromDomain(contract));
            }
        }

        // Ưu tiên hiển thị: Hợp đồng quá hạn và sắp hết hạn gấp nhất (daysRemaining nhỏ nhất) lên đầu danh sách
        results.sort(java.util.Comparator.comparingLong(ExpiringOutsourcedContractResult::daysRemaining));

        return ExpiringOutsourcedContractListResult.of(results);
    }
}
