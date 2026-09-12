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
import com.hrm.employeemanagement.domain.authorization.DataScope;
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

        // 2. Kiểm tra nhân sự (Pessimistic write lock trên employee row để serialize concurrent reservations)
        Employee employee = loadEmployeePort.findByIdForUpdate(new EmployeeId(command.employeeId()))
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

        // Điều chỉnh năng lực khả dụng theo hợp đồng lao động nếu hết hạn trong tuần
        netAvailableHours = WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(
                netAvailableHours,
                employee.getContractEndDate(),
                yearWeek.getStartDate(),
                yearWeek.getEndDate(),
                5
        );

        List<WeeklyProjectAllocation> existingAllocations = loadAllocationPort.loadAllocationsForEmployee(command.employeeId(), yearWeek);
        BigDecimal totalCommittedHours = existingAllocations.stream()
                .map(WeeklyProjectAllocation::getAllocatedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingHours = WeeklyCapacityMatrixPolicy.calculateRemainingHours(netAvailableHours, totalCommittedHours);

        // [CR-01 FIX]: Tính tổng số giờ của các reservation ACTIVE khác trong tuần (loại trừ chính dự án hiện tại nếu đang cập nhật)
        List<ResourceReservation> activeReservations = loadReservationPort.findActiveByEmployeeIdAndYearWeek(command.employeeId(), yearWeek);
        BigDecimal otherActiveReservedHours = activeReservations.stream()
                .filter(r -> !Objects.equals(r.getProjectId(), command.projectId()))
                .map(ResourceReservation::getReservedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableForReservation = remainingHours.subtract(otherActiveReservedHours);
        if (availableForReservation.compareTo(BigDecimal.ZERO) < 0) {
            availableForReservation = BigDecimal.ZERO;
        }

        if (command.reservedHours() == null || command.reservedHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidReservationDataException("Số giờ giữ chỗ phải lớn hơn 0");
        }

        if (command.reservedHours().compareTo(availableForReservation) > 0) {
            throw new InvalidReservationDataException(
                    "Số giờ giữ chỗ (" + command.reservedHours() + "h) vượt quá số giờ còn lại có thể giữ chỗ ("
                            + availableForReservation + "h) của nhân sự trong tuần " + yearWeek.weekNumber() + "/" + yearWeek.year()
                            + " (Khả dụng: " + netAvailableHours + "h, Đã phân bổ chính thức: " + totalCommittedHours
                            + "h, Các dự án khác đã giữ chỗ: " + otherActiveReservedHours + "h)"
            );
        }

        // 4. Kiểm tra xem dự án này đã có giữ chỗ ACTIVE cho nhân sự trong tuần chưa
        Optional<ResourceReservation> existingActive = loadReservationPort
                .findActiveByProjectAndEmployeeAndYearWeek(command.projectId(), command.employeeId(), yearWeek);

        ResourceReservation reservationToSave;
        boolean isUpdate = existingActive.isPresent();
        BigDecimal oldReservedHours = null;
        if (isUpdate) {
            ResourceReservation existing = existingActive.get();
            oldReservedHours = existing.getReservedHours();
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
        if (isUpdate) {
            saveAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "RESOURCE_RESERVATION_UPDATED",
                    "resource_reservations",
                    saved.getId(),
                    "reservedHours=" + oldReservedHours,
                    "reservedHours=" + saved.getReservedHours()
            ));
        } else {
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
        }

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
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        List<ResourceReservation> list = loadReservationPort.findReservations(projectId, employeeId, year, weekNumber, status);
        if (list.isEmpty()) {
            return List.of();
        }

        Set<Long> projectIds = list.stream().map(ResourceReservation::getProjectId).collect(Collectors.toSet());
        Set<Long> employeeIds = list.stream().map(ResourceReservation::getEmployeeId).collect(Collectors.toSet());

        // [3.3 FIX]: Batch load nhân sự thay vì lặp từng ID đơn lẻ
        List<Employee> loadedEmployees = loadEmployeePort.findAllByIdIn(
                employeeIds.stream().map(EmployeeId::new).toList()
        );
        Map<Long, Employee> employeeMap;
        if (loadedEmployees != null && !loadedEmployees.isEmpty()) {
            employeeMap = loadedEmployees.stream()
                    .collect(Collectors.toMap(Employee::getIdValue, e -> e, (a, b) -> a));
        } else {
            employeeMap = employeeIds.stream()
                    .map(id -> loadEmployeePort.findById(new EmployeeId(id)).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Employee::getIdValue, e -> e, (a, b) -> a));
        }

        Map<Long, Project> projectMap = new HashMap<>();
        Map<Long, Boolean> projectAccessCache = new HashMap<>();
        for (Long pId : projectIds) {
            Project p = loadProjectPort.findById(new ProjectId(pId)).orElse(null);
            if (p != null) {
                projectMap.put(pId, p);
                projectAccessCache.put(pId, canAccessProject(currentUser, currentUserId, p));
            } else {
                projectAccessCache.put(pId, false);
            }
        }

        // [CR-03 FIX]: Enforce DataScope của user hiện tại với từng dự án để ngăn chặn rò rỉ dữ liệu
        return list.stream()
                .filter(r -> Boolean.TRUE.equals(projectAccessCache.get(r.getProjectId())))
                .map(r -> mapToResult(r, projectMap.get(r.getProjectId()), employeeMap.get(r.getEmployeeId())))
                .toList();
    }

    @Override
    public int autoCancelForProject(Long projectId, String cancelReason) {
        Objects.requireNonNull(projectId, "Mã dự án (projectId) không được để trống");
        // [CR-02 & CR-05 FIX]: Enforce RESOURCE_ALLOCATION_MANAGE và DataScope tại Service layer
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        // [3.2 FIX]: Khóa bi quan dự án để ngăn chặn xung đột đồng thời với autoConvert
        Project project = loadProjectPort.findByIdForUpdate(new ProjectId(projectId))
                .or(() -> loadProjectPort.findById(new ProjectId(projectId)))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, projectId, "OUTSIDE_DATA_SCOPE_RESERVATION_AUTO_CANCEL");
            throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        }

        List<ResourceReservation> activeReservations = loadReservationPort.findActiveByProjectId(projectId);
        if (activeReservations.isEmpty()) {
            return 0;
        }

        String reason = cancelReason != null && !cancelReason.isBlank() ? cancelReason : "Dự án dự kiến bị hủy";
        for (ResourceReservation reservation : activeReservations) {
            reservation.cancel(currentUserId, reason);
            saveReservationPort.save(reservation);
            saveAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
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
    public int autoConvertForProject(Long projectId) {
        Objects.requireNonNull(projectId, "Mã dự án (projectId) không được để trống");
        // [CR-02 & CR-05 FIX]: Enforce RESOURCE_ALLOCATION_MANAGE và DataScope tại Service layer
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        // [3.2 FIX]: Khóa bi quan hàng dự án với findByIdForUpdate để ngăn chặn 2 request đồng thời
        Project project = loadProjectPort.findByIdForUpdate(new ProjectId(projectId))
                .or(() -> loadProjectPort.findById(new ProjectId(projectId)))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, projectId, "OUTSIDE_DATA_SCOPE_RESERVATION_AUTO_CONVERT");
            throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        }

        List<ResourceReservation> activeReservations = loadReservationPort.findActiveByProjectId(projectId);
        if (activeReservations.isEmpty()) {
            return 0;
        }

        // [3.2 FIX]: Khóa bi quan nhân sự theo thứ tự ID tăng dần (deterministic) để chống Deadlock và đảm bảo atomic
        List<Long> distinctEmployeeIds = activeReservations.stream()
                .map(ResourceReservation::getEmployeeId)
                .distinct()
                .sorted()
                .toList();

        Map<Long, Employee> employeeMap = new HashMap<>();
        for (Long empId : distinctEmployeeIds) {
            Employee emp = loadEmployeePort.findByIdForUpdate(new EmployeeId(empId))
                    .or(() -> loadEmployeePort.findById(new EmployeeId(empId)))
                    .orElse(null);
            if (emp != null) {
                employeeMap.put(empId, emp);
            }
        }

        List<YearWeek> distinctYearWeeks = activeReservations.stream()
                .map(ResourceReservation::getYearWeek)
                .distinct()
                .toList();

        // [3.3 FIX]: Batch loading availability và allocation thay cho N+1 queries trong vòng lặp kép
        Map<String, WeeklyAvailability> availabilityMap = batchLoadAvailability(distinctEmployeeIds, distinctYearWeeks);
        Map<String, List<WeeklyProjectAllocation>> allocationMap = batchLoadAllocations(distinctEmployeeIds, distinctYearWeeks);

        // [CR-04 FIX]: 1. Kiểm tra capacity trước khi convert từng reservation để không gây overload
        for (ResourceReservation reservation : activeReservations) {
            YearWeek yw = reservation.getYearWeek();
            String key = makeKey(reservation.getEmployeeId(), yw.year(), yw.weekNumber());
            WeeklyAvailability avail = availabilityMap.get(key);
            Employee emp = employeeMap.get(reservation.getEmployeeId());
            int standardHours = (emp != null && emp.getStandardHoursPerWeek() != null) ? emp.getStandardHoursPerWeek() : 40;
            BigDecimal netAvailable = avail != null ? avail.getNetAvailableHours() : BigDecimal.valueOf(standardHours);

            if (emp != null) {
                netAvailable = WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(
                        netAvailable,
                        emp.getContractEndDate(),
                        yw.getStartDate(),
                        yw.getEndDate(),
                        5
                );
            }

            List<WeeklyProjectAllocation> existingAllocations = allocationMap.getOrDefault(key, List.of());

            Optional<WeeklyProjectAllocation> matchingOpt = existingAllocations.stream()
                    .filter(a -> a.getProjectId().equals(projectId))
                    .findFirst();

            BigDecimal otherAllocatedHours = existingAllocations.stream()
                    .filter(a -> !a.getProjectId().equals(projectId))
                    .map(WeeklyProjectAllocation::getAllocatedHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal currentProjectHours = matchingOpt.map(WeeklyProjectAllocation::getAllocatedHours).orElse(BigDecimal.ZERO);
            BigDecimal newProjectHours = currentProjectHours.add(reservation.getReservedHours());
            BigDecimal totalNewAllocated = otherAllocatedHours.add(newProjectHours);

            if (totalNewAllocated.compareTo(netAvailable) > 0) {
                throw new InvalidReservationDataException(
                        "Không thể tự động chuyển đổi giữ chỗ sang phân bổ chính thức cho nhân sự ID " + reservation.getEmployeeId()
                                + " ở tuần " + yw.weekNumber() + "/" + yw.year()
                                + ": Tổng phân bổ sau khi chuyển đổi (" + totalNewAllocated + "h) sẽ vượt quá năng lực khả dụng ("
                                + netAvailable + "h). Vui lòng điều chỉnh phân bổ trước khi duyệt dự án."
                );
            }
        }

        // 2. Chuyển đổi giữ chỗ thành phân bổ chính thức
        for (ResourceReservation reservation : activeReservations) {
            YearWeek yw = reservation.getYearWeek();
            String key = makeKey(reservation.getEmployeeId(), yw.year(), yw.weekNumber());
            List<WeeklyProjectAllocation> existingAllocations = allocationMap.getOrDefault(key, List.of());

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

            reservation.convert(savedAllocation.getId(), currentUserId);
            saveReservationPort.save(reservation);

            saveAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "RESOURCE_RESERVATION_CONVERTED",
                    "resource_reservations",
                    reservation.getId(),
                    "status=ACTIVE",
                    "status=CONVERTED;allocationId=" + savedAllocation.getId()
            ));
        }

        return activeReservations.size();
    }

    private Map<String, WeeklyAvailability> batchLoadAvailability(List<Long> employeeIds, List<YearWeek> yearWeeks) {
        List<WeeklyAvailability> batch = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(employeeIds, yearWeeks);
        if (batch != null && !batch.isEmpty()) {
            return batch.stream().collect(Collectors.toMap(
                    a -> makeKey(a.getEmployeeId(), a.getYearWeek().year(), a.getYearWeek().weekNumber()),
                    a -> a,
                    (first, second) -> first
            ));
        }
        Map<String, WeeklyAvailability> map = new HashMap<>();
        for (Long empId : employeeIds) {
            for (YearWeek yw : yearWeeks) {
                loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(empId, yw)
                        .ifPresent(a -> map.put(makeKey(empId, yw.year(), yw.weekNumber()), a));
            }
        }
        return map;
    }

    private Map<String, List<WeeklyProjectAllocation>> batchLoadAllocations(List<Long> employeeIds, List<YearWeek> yearWeeks) {
        List<WeeklyProjectAllocation> batch = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(employeeIds, yearWeeks);
        if (batch != null && !batch.isEmpty()) {
            return batch.stream().collect(Collectors.groupingBy(
                    a -> makeKey(a.getEmployeeId(), a.getYearWeek().year(), a.getYearWeek().weekNumber())
            ));
        }
        Map<String, List<WeeklyProjectAllocation>> map = new HashMap<>();
        for (Long empId : employeeIds) {
            for (YearWeek yw : yearWeeks) {
                List<WeeklyProjectAllocation> list = loadAllocationPort.loadAllocationsForEmployee(empId, yw);
                if (list != null && !list.isEmpty()) {
                    map.put(makeKey(empId, yw.year(), yw.weekNumber()), new ArrayList<>(list));
                }
            }
        }
        return map;
    }

    private String makeKey(Long employeeId, int year, int weekNumber) {
        return employeeId + "_" + year + "_" + weekNumber;
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
