package com.hrm.employeemanagement.application.service.allocation.period;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPeriodResult;
import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPlanSnapshotItemResult;
import com.hrm.employeemanagement.application.dto.allocation.period.AllocationPlanSnapshotResult;
import com.hrm.employeemanagement.application.dto.allocation.period.CreatePeriodCommand;
import com.hrm.employeemanagement.application.dto.allocation.period.LockPeriodCommand;
import com.hrm.employeemanagement.application.dto.allocation.period.PeriodLockCheckResult;
import com.hrm.employeemanagement.application.dto.allocation.period.UnlockPeriodCommand;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CreateAllocationPeriodUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.GetAllocationPeriodsUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.LockAllocationPeriodUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.UnlockAllocationPeriodUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationPlanSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationPlanningPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.LoadAllocationsForPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.SaveAllocationPlanSnapshotPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.period.SaveAllocationPlanningPeriodPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodLockPolicy;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPeriodStatus;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanSnapshot;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanSnapshotItem;
import com.hrm.employeemanagement.domain.allocation.period.AllocationPlanningPeriod;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodNotFoundException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;

/**
 * Triển khai nghiệp vụ cho chức năng NCL-06-CN-009: Khóa kế hoạch phân bổ của kỳ (QTN-18).
 */
public class AllocationPeriodService implements
        CreateAllocationPeriodUseCase,
        LockAllocationPeriodUseCase,
        UnlockAllocationPeriodUseCase,
        GetAllocationPeriodsUseCase,
        CheckAllocationPeriodLockUseCase {

    private final AuthorizationService authorizationService;
    private final SaveAllocationPlanningPeriodPort savePeriodPort;
    private final LoadAllocationPlanningPeriodPort loadPeriodPort;
    private final SaveAllocationPlanSnapshotPort saveSnapshotPort;
    private final LoadAllocationPlanSnapshotPort loadSnapshotPort;
    private final LoadAllocationsForPeriodPort loadAllocationsPort;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;

    public AllocationPeriodService(
            AuthorizationService authorizationService,
            SaveAllocationPlanningPeriodPort savePeriodPort,
            LoadAllocationPlanningPeriodPort loadPeriodPort,
            SaveAllocationPlanSnapshotPort saveSnapshotPort,
            LoadAllocationPlanSnapshotPort loadSnapshotPort,
            LoadAllocationsForPeriodPort loadAllocationsPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.savePeriodPort = Objects.requireNonNull(savePeriodPort, "SaveAllocationPlanningPeriodPort must not be null");
        this.loadPeriodPort = Objects.requireNonNull(loadPeriodPort, "LoadAllocationPlanningPeriodPort must not be null");
        this.saveSnapshotPort = Objects.requireNonNull(saveSnapshotPort, "SaveAllocationPlanSnapshotPort must not be null");
        this.loadSnapshotPort = Objects.requireNonNull(loadSnapshotPort, "LoadAllocationPlanSnapshotPort must not be null");
        this.loadAllocationsPort = Objects.requireNonNull(loadAllocationsPort, "LoadAllocationsForPeriodPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
    }

    @Override
    public AllocationPeriodResult createPeriod(CreatePeriodCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE);

        if (loadPeriodPort.existsOverlapping(command.year(), command.startWeek(), command.endWeek(), null)) {
            throw new InvalidAllocationPeriodException("Đã tồn tại kỳ kế hoạch khác bao phủ hoặc trùng lặp khoảng tuần "
                    + command.startWeek() + " - " + command.endWeek() + " trong năm " + command.year());
        }

        AllocationPlanningPeriod period = AllocationPlanningPeriod.createNew(
                command.name(),
                command.periodType(),
                command.year(),
                command.startWeek(),
                command.endWeek(),
                currentUserId
        );

        AllocationPlanningPeriod saved = savePeriodPort.save(period);
        return toPeriodResult(saved, null);
    }

    @Override
    public AllocationPeriodResult lockPeriod(LockPeriodCommand command) {
        // [TC-03]: Kiểm tra thẩm quyền Quản lý nguồn lực (RESOURCE_ALLOCATION_LOCK)
        Long currentUserId;
        try {
            currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_LOCK);
        } catch (PermissionDeniedException e) {
            saveAuditLogPort.save(AuditLog.createChange(
                    null,
                    "ACCESS_DENIED_PLAN_PERIOD_LOCK",
                    "allocation_planning_periods",
                    command.periodId(),
                    null,
                    "Từ chối truy cập thao tác khóa kế hoạch phân bổ của kỳ do người dùng không có quyền RESOURCE_ALLOCATION_LOCK"
            ));
            throw e;
        }

        AllocationPlanningPeriod period = loadPeriodPort.findById(command.periodId())
                .orElseThrow(() -> new AllocationPeriodNotFoundException(command.periodId()));

        // [TC-01]: Chuyển trạng thái kỳ sang LOCKED
        period.lock(currentUserId);
        AllocationPlanningPeriod savedPeriod = savePeriodPort.save(period);

        // [TC-01]: Tạo bản chụp kế hoạch (Snapshot) của kỳ
        int nextVersion = loadSnapshotPort.countSnapshotsByPeriodId(savedPeriod.getId()) + 1;
        List<WeeklyProjectAllocation> allocationsInPeriod = loadAllocationsPort.loadAllocationsInWeekRange(
                savedPeriod.getYear(), savedPeriod.getStartWeek(), savedPeriod.getEndWeek());

        List<AllocationPlanSnapshotItem> items = new ArrayList<>();
        for (WeeklyProjectAllocation alloc : allocationsInPeriod) {
            items.add(AllocationPlanSnapshotItem.createNew(
                    alloc.getId(),
                    alloc.getEmployeeId(),
                    alloc.getProjectId(),
                    alloc.getYear(),
                    alloc.getWeekNumber(),
                    alloc.getAllocatedHours(),
                    alloc.getAllocationPercentage(),
                    alloc.isOverloaded(),
                    alloc.getOverloadReason()
            ));
        }

        AllocationPlanSnapshot snapshot = AllocationPlanSnapshot.createNew(
                savedPeriod.getId(),
                nextVersion,
                currentUserId,
                items
        );
        AllocationPlanSnapshot savedSnapshot = saveSnapshotPort.save(snapshot);

        // [TC-04]: Ghi nhật ký kiểm toán lưu vết người thực hiện, nội dung và thời điểm
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PLAN_PERIOD_LOCKED",
                "allocation_planning_periods",
                savedPeriod.getId(),
                "status=OPEN",
                "status=LOCKED;snapshotVersion=" + savedSnapshot.getSnapshotVersion()
                        + ";totalAllocations=" + savedSnapshot.getTotalAllocations()
                        + ";totalHours=" + savedSnapshot.getTotalAllocatedHours()
        ));

        return toPeriodResult(savedPeriod, toSnapshotResult(savedSnapshot));
    }

    @Override
    public AllocationPeriodResult unlockPeriod(UnlockPeriodCommand command) {
        // [TC-03]: Kiểm tra thẩm quyền Quản lý nguồn lực (RESOURCE_ALLOCATION_LOCK)
        Long currentUserId;
        try {
            currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_LOCK);
        } catch (PermissionDeniedException e) {
            saveAuditLogPort.save(AuditLog.createChange(
                    null,
                    "ACCESS_DENIED_PLAN_PERIOD_UNLOCK",
                    "allocation_planning_periods",
                    command.periodId(),
                    null,
                    "Từ chối truy cập thao tác mở lại kỳ kế hoạch phân bổ do thiếu quyền RESOURCE_ALLOCATION_LOCK"
            ));
            throw e;
        }

        AllocationPlanningPeriod period = loadPeriodPort.findById(command.periodId())
                .orElseThrow(() -> new AllocationPeriodNotFoundException(command.periodId()));

        period.unlock(currentUserId, command.reason());
        AllocationPlanningPeriod saved = savePeriodPort.save(period);

        // [TC-04]: Ghi nhật ký kiểm toán mở lại kỳ
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PLAN_PERIOD_UNLOCKED",
                "allocation_planning_periods",
                saved.getId(),
                "status=LOCKED",
                "status=OPEN;reason=" + command.reason().trim()
        ));

        Optional<AllocationPlanSnapshot> latestSnapOpt = loadSnapshotPort.findLatestByPeriodId(saved.getId());
        return toPeriodResult(saved, latestSnapOpt.map(this::toSnapshotResult).orElse(null));
    }

    @Override
    public List<AllocationPeriodResult> getPeriods(Integer year, AllocationPeriodStatus status) {
        authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);
        List<AllocationPlanningPeriod> periods = loadPeriodPort.findAll(year, status);
        return periods.stream()
                .map(p -> {
                    Optional<AllocationPlanSnapshot> snapOpt = loadSnapshotPort.findLatestByPeriodId(p.getId());
                    return toPeriodResult(p, snapOpt.map(this::toSnapshotResult).orElse(null));
                })
                .toList();
    }

    @Override
    public AllocationPeriodResult getPeriodById(Long id) {
        authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);
        AllocationPlanningPeriod period = loadPeriodPort.findById(id)
                .orElseThrow(() -> new AllocationPeriodNotFoundException(id));
        Optional<AllocationPlanSnapshot> snapOpt = loadSnapshotPort.findLatestByPeriodId(period.getId());
        return toPeriodResult(period, snapOpt.map(this::toSnapshotResult).orElse(null));
    }

    @Override
    public List<AllocationPlanSnapshotResult> getPeriodSnapshots(Long periodId) {
        authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);
        return loadSnapshotPort.findByPeriodId(periodId).stream()
                .map(this::toSnapshotResult)
                .toList();
    }

    @Override
    public AllocationPlanSnapshotResult getSnapshotDetail(Long periodId, Long snapshotId) {
        authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);
        AllocationPlanSnapshot snapshot = loadSnapshotPort.findSnapshotById(snapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản chụp kế hoạch với ID: " + snapshotId));
        if (!snapshot.getPeriodId().equals(periodId)) {
            throw new IllegalArgumentException("Bản chụp không thuộc kỳ kế hoạch được yêu cầu");
        }
        return toSnapshotResult(snapshot);
    }

    @Override
    public PeriodLockCheckResult checkWeekLock(int year, int weekNumber) {
        List<AllocationPlanningPeriod> lockedPeriods = loadPeriodPort.findLockedPeriodsCoveringWeek(year, weekNumber);
        Optional<AllocationPlanningPeriod> lockedPeriodOpt = AllocationPeriodLockPolicy.findLockedPeriodCoveringWeek(year, weekNumber, lockedPeriods);
        if (lockedPeriodOpt.isPresent()) {
            AllocationPlanningPeriod p = lockedPeriodOpt.get();
            return PeriodLockCheckResult.locked(p.getId(), p.getName(), year, weekNumber);
        }
        return PeriodLockCheckResult.unlocked(year, weekNumber);
    }

    @Override
    public void validateWeekNotLocked(int year, int weekNumber) {
        List<AllocationPlanningPeriod> lockedPeriods = loadPeriodPort.findLockedPeriodsCoveringWeek(year, weekNumber);
        AllocationPeriodLockPolicy.validateCanModifyAllocation(year, weekNumber, lockedPeriods);
    }

    private AllocationPeriodResult toPeriodResult(AllocationPlanningPeriod period, AllocationPlanSnapshotResult latestSnapshot) {
        return new AllocationPeriodResult(
                period.getId(),
                period.getName(),
                period.getPeriodType(),
                period.getYear(),
                period.getStartWeek(),
                period.getEndWeek(),
                period.getStatus(),
                period.getLockedBy(),
                period.getLockedAt(),
                period.getUnlockedBy(),
                period.getUnlockedAt(),
                period.getUnlockReason(),
                period.getCreatedBy(),
                period.getCreatedAt(),
                latestSnapshot
        );
    }

    private AllocationPlanSnapshotResult toSnapshotResult(AllocationPlanSnapshot snapshot) {
        List<AllocationPlanSnapshotItemResult> itemResults = snapshot.getItems() != null
                ? snapshot.getItems().stream()
                .map(item -> new AllocationPlanSnapshotItemResult(
                        item.getId(),
                        item.getSnapshotId(),
                        item.getOriginalAllocationId(),
                        item.getEmployeeId(),
                        item.getProjectId(),
                        item.getYear(),
                        item.getWeekNumber(),
                        item.getAllocatedHours(),
                        item.getAllocationPercentage(),
                        item.isOverloaded(),
                        item.getOverloadReason()
                ))
                .toList()
                : List.of();

        return new AllocationPlanSnapshotResult(
                snapshot.getId(),
                snapshot.getPeriodId(),
                snapshot.getSnapshotVersion(),
                snapshot.getTotalAllocations(),
                snapshot.getTotalAllocatedHours(),
                snapshot.getCreatedBy(),
                snapshot.getCreatedAt(),
                itemResults
        );
    }
}
