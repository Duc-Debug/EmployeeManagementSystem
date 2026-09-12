package com.hrm.employeemanagement.application.service.reservation;

import com.hrm.employeemanagement.application.dto.reservation.CancelReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.CreateReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.ResourceReservationResult;
import com.hrm.employeemanagement.application.port.inbound.reservation.AutoProcessProjectReservationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.CancelResourceReservationUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.CreateResourceReservationUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.GetResourceReservationsUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.reservation.LoadResourceReservationPort;
import com.hrm.employeemanagement.application.port.outbound.reservation.SaveResourceReservationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.EmployeeInactiveException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationDataException;
import com.hrm.employeemanagement.domain.exception.reservation.ReservationNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import com.hrm.employeemanagement.domain.reservation.ResourceReservation;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceReservationService implements
        CreateResourceReservationUseCase,
        CancelResourceReservationUseCase,
        GetResourceReservationsUseCase,
        AutoProcessProjectReservationsUseCase {

    private final AuthorizationService authorizationService;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveWeeklyProjectAllocationPort saveAllocationPort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadResourceReservationPort loadReservationPort;
    private final SaveResourceReservationPort saveReservationPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;

    public ResourceReservationService(
            AuthorizationService authorizationService,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadResourceReservationPort loadReservationPort,
            SaveResourceReservationPort saveReservationPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            LoadUserPort loadUserPort,
            LoadOrgUnitPort loadOrgUnitPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.saveAllocationPort = Objects.requireNonNull(saveAllocationPort, "SaveWeeklyProjectAllocationPort must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.loadReservationPort = Objects.requireNonNull(loadReservationPort, "LoadResourceReservationPort must not be null");
        this.saveReservationPort = Objects.requireNonNull(saveReservationPort, "SaveResourceReservationPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
    }

    @Override
    public ResourceReservationResult createReservation(CreateReservationCommand command) {
        if (command == null) {
            throw new InvalidReservationDataException("Dữ liệu giữ chỗ không được để trống");
        }
        if (command.projectId() == null) {
            throw new InvalidReservationDataException("Mã dự án (projectId) không được để trống");
        }
        if (command.employeeId() == null) {
            throw new InvalidReservationDataException("Mã nhân sự (employeeId) không được để trống");
        }

        // [TC-04]: Kiểm tra quyền hạn cơ bản RESOURCE_RESERVATION_CREATE
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_RESERVATION_CREATE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        YearWeek yearWeek = YearWeek.of(command.year(), command.weekNumber());

        // 1. Kiểm tra dự án
        Project project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        // [Precondition A5 & TC-01]: Dự án bắt buộc phải ở trạng thái dự kiến (PLANNED)
        if (project.getStatus() != ProjectStatus.PLANNED) {
            throw new InvalidProjectDataException(
                    "Chỉ có thể giữ chỗ nguồn lực cho dự án đang ở trạng thái dự kiến (Trạng thái hiện tại: " + project.getStatus() + ")"
            );
        }

        // [A4]: Kiểm tra DataScope của User với Dự án (VT-02: SELF - PM phụ trách; VT-03: BRANCH)
        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_RESERVATION_CREATE");
            throw new PermissionDeniedException(PermissionCode.RESOURCE_RESERVATION_CREATE);
        }

        // 2. Kiểm tra nhân sự
        Employee employee = loadEmployeePort.findById(new EmployeeId(command.employeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + command.employeeId()));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new EmployeeInactiveException("Không thể giữ chỗ cho nhân sự không còn ở trạng thái hoạt động");
        }

        if (employee.getContractEndDate() != null && employee.getContractEndDate().isBefore(yearWeek.getStartDate())) {
            throw new EmployeeInactiveException(
                    "Nhân sự đã kết thúc hợp đồng lao động trước tuần được chọn (" + yearWeek.weekNumber() + "/" + yearWeek.year() + ")"
            );
        }

        // 3. Kiểm tra năng lực còn lại theo Option B / QTN-13
        Optional<WeeklyAvailability> availabilityOpt = loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(command.employeeId(), yearWeek);
        int standardHours = employee.getStandardHoursPerWeek() != null ? employee.getStandardHoursPerWeek() : 40;
        BigDecimal netAvailableHours = availabilityOpt.map(WeeklyAvailability::getNetAvailableHours)
                .orElse(BigDecimal.valueOf(standardHours));

        List<WeeklyProjectAllocation> existingAllocations = loadAllocationPort.loadAllocationsForEmployee(command.employeeId(), yearWeek);
        BigDecimal totalCommittedHours = existingAllocations.stream()
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingHours = WeeklyCapacityMatrixPolicy.calculateRemainingHours(netAvailableHours, totalCommittedHours);

        if (command.reservedHours() == null || command.reservedHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidReservationDataException("Số giờ giữ chỗ phải lớn hơn 0");
        }

        if (command.reservedHours().compareTo(remainingHours) > 0) {
            throw new InvalidReservationDataException(
                    "Số giờ giữ chỗ (" + command.reservedHours() + "h) vượt quá số giờ còn lại chưa cam kết ("
                            + remainingHours + "h) của nhân sự trong tuần " + yearWeek.weekNumber() + "/" + yearWeek.year()
            );
        }

        // 4. Kiểm tra xem dự án này đã có giữ chỗ ACTIVE cho nhân sự trong tuần chưa
        Optional<ResourceReservation> existingActive = loadReservationPort
                .findActiveByProjectAndEmployeeAndYearWeek(command.projectId(), command.employeeId(), yearWeek);

        ResourceReservation reservationToSave;
        if (existingActive.isPresent()) {
            ResourceReservation existing = existingActive.get();
            existing.updateReservedHours(command.reservedHours(), currentUserId);
            reservationToSave = existing;
        } else {
            reservationToSave = ResourceReservation.createNew(
                    command.projectId(),
                    command.employeeId(),
                    yearWeek,
                    command.reservedHours(),
                    command.note(),
                    currentUserId
            );
        }

        ResourceReservation saved = saveReservationPort.save(reservationToSave);

        // [TC-05]: Ghi nhật ký kiểm toán Business Audit Log
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "RESOURCE_RESERVATION_CREATED",
                "resource_reservations",
                saved.getId(),
                null,
                "projectId=" + project.getIdValue() + ";employeeId=" + employee.getIdValue()
                        + ";yearWeek=" + yearWeek.year() + "-W" + yearWeek.weekNumber()
                        + ";reservedHours=" + saved.getReservedHours()
        ));

        return mapToResult(saved, project, employee);
    }

    @Override
    public ResourceReservationResult cancelReservation(CancelReservationCommand command) {
        if (command == null || command.reservationId() == null) {
            throw new InvalidReservationDataException("Mã giữ chỗ (reservationId) không được để trống");
        }

        Long currentUserId = authorizationService.requireAny(
                PermissionCode.RESOURCE_RESERVATION_MANAGE,
                PermissionCode.RESOURCE_RESERVATION_CREATE
        );
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        ResourceReservation reservation = loadReservationPort.findById(command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException("Không tìm thấy bản ghi giữ chỗ với ID: " + command.reservationId()));

        Project project = loadProjectPort.findById(new ProjectId(reservation.getProjectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án của bản ghi giữ chỗ"));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_RESERVATION_CANCEL");
            throw new PermissionDeniedException(PermissionCode.RESOURCE_RESERVATION_MANAGE);
        }

        reservation.cancel(currentUserId, command.reason());
        ResourceReservation saved = saveReservationPort.save(reservation);

        // [TC-05]: Ghi nhật ký kiểm toán
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "RESOURCE_RESERVATION_CANCELLED",
                "resource_reservations",
                saved.getId(),
                "status=ACTIVE",
                "status=CANCELLED;reason=" + (command.reason() != null ? command.reason().trim() : "N/A")
        ));

        Employee employee = loadEmployeePort.findById(new EmployeeId(saved.getEmployeeId())).orElse(null);
        return mapToResult(saved, project, employee);
    }

    @Override
    public List<ResourceReservationResult> getReservations(
            Long projectId, Long employeeId, Integer year, Integer weekNumber, ReservationStatus status
    ) {
        authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);

        List<ResourceReservation> list = loadReservationPort.findReservations(projectId, employeeId, year, weekNumber, status);
        if (list.isEmpty()) {
            return List.of();
        }

        Set<Long> projectIds = list.stream().map(ResourceReservation::getProjectId).collect(Collectors.toSet());
        Set<Long> employeeIds = list.stream().map(ResourceReservation::getEmployeeId).collect(Collectors.toSet());

        Map<Long, Project> projectMap = projectIds.stream()
                .map(id -> loadProjectPort.findById(new ProjectId(id)).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Project::getIdValue, p -> p, (a, b) -> a));

        Map<Long, Employee> employeeMap = employeeIds.stream()
                .map(id -> loadEmployeePort.findById(new EmployeeId(id)).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Employee::getIdValue, e -> e, (a, b) -> a));

        return list.stream()
                .map(r -> mapToResult(r, projectMap.get(r.getProjectId()), employeeMap.get(r.getEmployeeId())))
                .toList();
    }

    @Override
    public int autoCancelForProject(Long projectId, String cancelReason, Long executedBy) {
        Objects.requireNonNull(projectId, "Mã dự án (projectId) không được để trống");
        List<ResourceReservation> activeReservations = loadReservationPort.findActiveByProjectId(projectId);
        if (activeReservations.isEmpty()) {
            return 0;
        }

        String reason = cancelReason != null && !cancelReason.isBlank() ? cancelReason : "Dự án dự kiến bị hủy";
        for (ResourceReservation reservation : activeReservations) {
            reservation.cancel(executedBy, reason);
            saveReservationPort.save(reservation);
            saveAuditLogPort.save(AuditLog.createChange(
                    executedBy,
                    "RESOURCE_RESERVATION_AUTO_CANCELLED",
                    "resource_reservations",
                    reservation.getId(),
                    "status=ACTIVE",
                    "status=CANCELLED;reason=" + reason
            ));
        }

        return activeReservations.size();
    }

    @Override
    public int autoConvertForProject(Long projectId, Long executedBy) {
        Objects.requireNonNull(projectId, "Mã dự án (projectId) không được để trống");
        List<ResourceReservation> activeReservations = loadReservationPort.findActiveByProjectId(projectId);
        if (activeReservations.isEmpty()) {
            return 0;
        }

        for (ResourceReservation reservation : activeReservations) {
            List<WeeklyProjectAllocation> existingAllocations = loadAllocationPort.loadAllocationsForEmployee(
                    reservation.getEmployeeId(), reservation.getYearWeek()
            );

            Optional<WeeklyProjectAllocation> matchingOpt = existingAllocations.stream()
                    .filter(a -> a.getProjectId().equals(projectId))
                    .findFirst();

            WeeklyProjectAllocation allocationToSave;
            if (matchingOpt.isPresent()) {
                allocationToSave = matchingOpt.get();
                allocationToSave.updateAllocatedHours(allocationToSave.getAllocatedHours().add(reservation.getReservedHours()));
            } else {
                allocationToSave = WeeklyProjectAllocation.createNew(
                        reservation.getEmployeeId(),
                        projectId,
                        reservation.getYearWeek(),
                        reservation.getReservedHours()
                );
            }

            WeeklyProjectAllocation savedAllocation = saveAllocationPort.save(allocationToSave);

            reservation.convert(savedAllocation.getId(), executedBy);
            saveReservationPort.save(reservation);

            saveAuditLogPort.save(AuditLog.createChange(
                    executedBy,
                    "RESOURCE_RESERVATION_CONVERTED",
                    "resource_reservations",
                    reservation.getId(),
                    "status=ACTIVE",
                    "status=CONVERTED;allocationId=" + savedAllocation.getId()
            ));
        }

        return activeReservations.size();
    }

    private boolean canAccessProject(User currentUser, Long currentUserId, Project project) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH ->
                    currentUser.getScopeOrgUnitId() != null
                            && loadOrgUnitPort.existsInOrgUnitBranch(project.getOrgUnitId(), currentUser.getScopeOrgUnitId());
            case SELF -> {
                Long employeeId = loadEmployeePort.findByUserId(new UserId(currentUserId))
                        .map(Employee::getIdValue)
                        .orElse(null);
                yield employeeId != null && project.isManagedBy(new EmployeeId(employeeId));
            }
        };
    }

    private User loadCurrentUserOrThrow(Long currentUserId) {
        return loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + currentUserId));
    }

    private void saveDeniedAudit(Long currentUserId, User currentUser, Long projectId, String reason) {
        saveDeniedAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PERMISSION_DENIED",
                "resource_reservations",
                projectId,
                null,
                "permission=RESOURCE_RESERVATION;role=" + (currentUser.getRole() != null ? currentUser.getRole().getCode() : "null")
                        + ";dataScope=" + currentUser.getDataScope() + ";reason=" + reason
        ));
    }

    private ResourceReservationResult mapToResult(ResourceReservation reservation, Project project, Employee employee) {
        return new ResourceReservationResult(
                reservation.getId(),
                reservation.getProjectId(),
                project != null ? project.getProjectCode() : null,
                project != null ? project.getProjectName() : null,
                reservation.getEmployeeId(),
                employee != null ? employee.getEmployeeCode() : null,
                employee != null ? employee.getFullName() : null,
                reservation.getYear(),
                reservation.getWeekNumber(),
                reservation.getReservedHours(),
                reservation.getStatus(),
                reservation.getConvertedAllocationId(),
                reservation.getCancelledReason(),
                reservation.getNote(),
                reservation.getCreatedBy(),
                reservation.getCreatedAt()
        );
    }
}
