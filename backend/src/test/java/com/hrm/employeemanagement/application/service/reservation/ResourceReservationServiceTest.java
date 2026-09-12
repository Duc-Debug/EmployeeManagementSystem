package com.hrm.employeemanagement.application.service.reservation;

import com.hrm.employeemanagement.application.dto.reservation.CancelReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.CreateReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.ResourceReservationResult;
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
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.reservation.InvalidReservationDataException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import com.hrm.employeemanagement.domain.reservation.ResourceReservation;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceReservationService Application Unit Tests (UC NCL-06-CN-005)")
class ResourceReservationServiceTest {

    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    @Mock
    private SaveWeeklyProjectAllocationPort saveAllocationPort;
    @Mock
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    @Mock
    private LoadResourceReservationPort loadReservationPort;
    @Mock
    private SaveResourceReservationPort saveReservationPort;
    @Mock
    private SaveAuditLogPort saveAuditLogPort;
    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    private ResourceReservationService service;

    private final Long pmUserId = 2L;
    private final Long pmEmployeeId = 20L;
    private final Long targetEmployeeId = 50L;
    private final Long projectId = 100L;
    private final YearWeek yearWeek = YearWeek.of(2026, 40);

    private User pmUser;
    private Project plannedProject;
    private Employee targetEmployee;

    @BeforeEach
    void setUp() {
        service = new ResourceReservationService(
                authorizationService,
                loadEmployeePort,
                loadProjectPort,
                loadAllocationPort,
                saveAllocationPort,
                loadWeeklyAvailabilityPort,
                loadReservationPort,
                saveReservationPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                loadUserPort,
                loadOrgUnitPort
        );

        Role pmRole = new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án");
        pmUser = new User(new UserId(pmUserId), "pm_user", "hash", pmRole, UserStatus.ACTIVE, new EmployeeId(pmEmployeeId), DataScope.SELF, null, 0L);

        plannedProject = new Project(
                new ProjectId(projectId),
                "PRJ-PLAN-01",
                "Dự án ERP mới",
                1L,
                new EmployeeId(pmEmployeeId),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(500),
                "Mô tả dự án dự kiến",
                ProjectStatus.PLANNED,
                new UserId(pmUserId),
                LocalDateTime.now(),
                null,
                0L,
                0
        );

        targetEmployee = new Employee(
                new EmployeeId(targetEmployeeId),
                new UserId(pmUserId + 100),
                1L,
                "EMP050",
                "Nguyen Van A",
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("TC-01: Happy Path - Tạo giữ chỗ 20h/tuần thành công cho dự án dự kiến")
    void shouldCreateReservationSuccessfully() {
        when(authorizationService.require(PermissionCode.RESOURCE_RESERVATION_CREATE)).thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(plannedProject));
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(
                new Employee(new EmployeeId(pmEmployeeId), new UserId(pmUserId), 1L, "PM01", "PM User", false, 40, EmployeeStatus.ACTIVE)
        ));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(targetEmployeeId))).thenReturn(Optional.of(targetEmployee));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(targetEmployeeId, yearWeek)).thenReturn(Optional.of(
                new WeeklyAvailability(1L, targetEmployeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40.0))
        ));
        when(loadAllocationPort.loadAllocationsForEmployee(targetEmployeeId, yearWeek)).thenReturn(List.of());
        when(loadReservationPort.findActiveByProjectAndEmployeeAndYearWeek(projectId, targetEmployeeId, yearWeek)).thenReturn(Optional.empty());

        when(saveReservationPort.save(any(ResourceReservation.class))).thenAnswer(invocation -> {
            ResourceReservation r = invocation.getArgument(0);
            return new ResourceReservation(
                    1L, r.getProjectId(), r.getEmployeeId(), r.getYearWeek(), r.getReservedHours(),
                    r.getStatus(), r.getConvertedAllocationId(), r.getCancelledReason(), r.getNote(),
                    r.getCreatedBy(), r.getCreatedAt(), null, null, 0L
            );
        });

        CreateReservationCommand command = new CreateReservationCommand(
                projectId, targetEmployeeId, yearWeek.year(), yearWeek.weekNumber(), BigDecimal.valueOf(20.0), "Giữ chỗ sprint 1"
        );

        ResourceReservationResult result = service.createReservation(command);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(projectId, result.projectId());
        assertEquals(targetEmployeeId, result.employeeId());
        assertEquals(BigDecimal.valueOf(20.0), result.reservedHours());
        assertEquals(ReservationStatus.ACTIVE, result.status());

        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("Ném InvalidProjectDataException khi dự án KHÔNG ở trạng thái dự kiến (PLANNED)")
    void shouldThrowWhenProjectIsNotPlanned() {
        Project activeProject = new Project(
                new ProjectId(projectId), "PRJ-ACT-01", "Dự án đang chạy", 1L, new EmployeeId(pmEmployeeId),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.valueOf(500),
                "Mô tả", ProjectStatus.ACTIVE, new UserId(pmUserId), LocalDateTime.now(), null, 0L, 0
        );

        when(authorizationService.require(PermissionCode.RESOURCE_RESERVATION_CREATE)).thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(activeProject));

        CreateReservationCommand command = new CreateReservationCommand(
                projectId, targetEmployeeId, yearWeek.year(), yearWeek.weekNumber(), BigDecimal.valueOf(20.0), "Note"
        );

        InvalidProjectDataException ex = assertThrows(InvalidProjectDataException.class, () ->
                service.createReservation(command)
        );
        assertTrue(ex.getMessage().contains("trạng thái dự kiến"));
    }

    @Test
    @DisplayName("Ném InvalidReservationDataException khi số giờ giữ chỗ vượt quá năng lực còn lại chưa cam kết")
    void shouldThrowWhenReservedHoursExceedsRemainingCapacity() {
        when(authorizationService.require(PermissionCode.RESOURCE_RESERVATION_CREATE)).thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(plannedProject));
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(
                new Employee(new EmployeeId(pmEmployeeId), new UserId(pmUserId), 1L, "PM01", "PM User", false, 40, EmployeeStatus.ACTIVE)
        ));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(targetEmployeeId))).thenReturn(Optional.of(targetEmployee));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(targetEmployeeId, yearWeek)).thenReturn(Optional.of(
                new WeeklyAvailability(1L, targetEmployeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40.0))
        ));

        // Đã cam kết 30h cho dự án khác -> còn lại 10h
        WeeklyProjectAllocation otherAlloc = new WeeklyProjectAllocation(
                1L, targetEmployeeId, 999L, yearWeek, BigDecimal.valueOf(30.0)
        );
        when(loadAllocationPort.loadAllocationsForEmployee(targetEmployeeId, yearWeek)).thenReturn(List.of(otherAlloc));

        CreateReservationCommand command = new CreateReservationCommand(
                projectId, targetEmployeeId, yearWeek.year(), yearWeek.weekNumber(), BigDecimal.valueOf(15.0), "Yêu cầu 15h nhưng chỉ còn 10h"
        );

        InvalidReservationDataException ex = assertThrows(InvalidReservationDataException.class, () ->
                service.createReservation(command)
        );
        assertTrue(ex.getMessage().contains("vượt quá số giờ còn lại có thể giữ chỗ"));
    }

    @Test
    @DisplayName("TC-04: Từ chối truy cập (403) và ghi security log khi PM thao tác ngoài phạm vi DataScope (dự án của người khác)")
    void shouldDenyWhenPmAttemptsToReserveOnAnotherPmProject() {
        // Dự án do PM khác quản lý (managerId = 999L != pmEmployeeId)
        Project otherPmProject = new Project(
                new ProjectId(projectId), "PRJ-OTHER", "Dự án của PM khác", 1L, new EmployeeId(999L),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.valueOf(500),
                "Mô tả", ProjectStatus.PLANNED, new UserId(999L), LocalDateTime.now(), null, 0L, 0
        );

        when(authorizationService.require(PermissionCode.RESOURCE_RESERVATION_CREATE)).thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(otherPmProject));
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(
                new Employee(new EmployeeId(pmEmployeeId), new UserId(pmUserId), 1L, "PM01", "PM User", false, 40, EmployeeStatus.ACTIVE)
        ));

        CreateReservationCommand command = new CreateReservationCommand(
                projectId, targetEmployeeId, yearWeek.year(), yearWeek.weekNumber(), BigDecimal.valueOf(10.0), "Note"
        );

        assertThrows(PermissionDeniedException.class, () ->
                service.createReservation(command)
        );

        // Kiểm tra ghi security log từ chối truy cập
        verify(saveDeniedAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("TC-02: Auto-cancel tất cả bản ghi giữ chỗ ACTIVE khi dự án bị hủy (CR-02 & CR-05)")
    void shouldAutoCancelActiveReservationsWhenProjectCancelled() {
        ResourceReservation r1 = ResourceReservation.createNew(projectId, 101L, yearWeek, BigDecimal.valueOf(20.0), "Note 1", 1L);
        ResourceReservation r2 = ResourceReservation.createNew(projectId, 102L, yearWeek, BigDecimal.valueOf(15.0), "Note 2", 1L);

        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(plannedProject));
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(
                new Employee(new EmployeeId(pmEmployeeId), new UserId(pmUserId), 1L, "PM01", "PM User", false, 40, EmployeeStatus.ACTIVE)
        ));
        when(loadReservationPort.findActiveByProjectId(projectId)).thenReturn(List.of(r1, r2));

        int cancelledCount = service.autoCancelForProject(projectId, "Dự án không được phê duyệt");

        assertEquals(2, cancelledCount);
        assertTrue(r1.isCancelled());
        assertTrue(r2.isCancelled());
        assertEquals("Dự án không được phê duyệt", r1.getCancelledReason());
        verify(saveReservationPort, times(2)).save(any());
        verify(saveAuditLogPort, times(2)).save(any());
    }

    @Test
    @DisplayName("TC-03: Auto-convert tất cả bản ghi giữ chỗ ACTIVE thành phân bổ chính thức khi dự án được duyệt (CR-02 & CR-05)")
    void shouldAutoConvertActiveReservationsToAllocationsWhenProjectApproved() {
        ResourceReservation r1 = ResourceReservation.createNew(projectId, 101L, yearWeek, BigDecimal.valueOf(20.0), "Note 1", 1L);

        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(plannedProject));
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(
                new Employee(new EmployeeId(pmEmployeeId), new UserId(pmUserId), 1L, "PM01", "PM User", false, 40, EmployeeStatus.ACTIVE)
        ));
        when(loadReservationPort.findActiveByProjectId(projectId)).thenReturn(List.of(r1));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(101L, yearWeek)).thenReturn(Optional.of(
                new WeeklyAvailability(1L, 101L, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40.0))
        ));
        when(loadAllocationPort.loadAllocationsForEmployee(101L, yearWeek)).thenReturn(List.of());

        when(saveAllocationPort.save(any(WeeklyProjectAllocation.class))).thenAnswer(invocation -> {
            WeeklyProjectAllocation a = invocation.getArgument(0);
            return new WeeklyProjectAllocation(555L, a.getEmployeeId(), a.getProjectId(), a.getYearWeek(), a.getAllocatedHours());
        });

        int convertedCount = service.autoConvertForProject(projectId);

        assertEquals(1, convertedCount);
        assertTrue(r1.isConverted());
        assertEquals(555L, r1.getConvertedAllocationId());
        verify(saveAllocationPort, times(1)).save(any());
        verify(saveReservationPort, times(1)).save(r1);
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("CR-01: Ném InvalidReservationDataException khi số giờ giữ chỗ vượt quá năng lực còn lại do các reservation ACTIVE khác")
    void shouldThrowWhenReservedHoursExceedsRemainingAfterOtherActiveReservations() {
        when(authorizationService.require(PermissionCode.RESOURCE_RESERVATION_CREATE)).thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(plannedProject));
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(
                new Employee(new EmployeeId(pmEmployeeId), new UserId(pmUserId), 1L, "PM01", "PM User", false, 40, EmployeeStatus.ACTIVE)
        ));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(targetEmployeeId))).thenReturn(Optional.of(targetEmployee));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(targetEmployeeId, yearWeek)).thenReturn(Optional.of(
                new WeeklyAvailability(1L, targetEmployeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40.0))
        ));

        // Phân bổ chính thức: 20h -> Còn lại: 20h
        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(1L, targetEmployeeId, 888L, yearWeek, BigDecimal.valueOf(20.0));
        when(loadAllocationPort.loadAllocationsForEmployee(targetEmployeeId, yearWeek)).thenReturn(List.of(alloc));

        // Reservation ACTIVE khác cho dự án 999L: 15h -> Chỉ còn lại có thể giữ chỗ: 40 - 20 - 15 = 5h
        ResourceReservation otherReservation = ResourceReservation.createNew(999L, targetEmployeeId, yearWeek, BigDecimal.valueOf(15.0), "Dự án khác", 1L);
        when(loadReservationPort.findActiveByEmployeeIdAndYearWeek(targetEmployeeId, yearWeek)).thenReturn(List.of(otherReservation));

        // Yêu cầu giữ chỗ 10h (> 5h)
        CreateReservationCommand command = new CreateReservationCommand(
                projectId, targetEmployeeId, yearWeek.year(), yearWeek.weekNumber(), BigDecimal.valueOf(10.0), "Cần 10h"
        );

        InvalidReservationDataException ex = assertThrows(InvalidReservationDataException.class, () ->
                service.createReservation(command)
        );
        assertTrue(ex.getMessage().contains("vượt quá số giờ còn lại có thể giữ chỗ"));
    }

    @Test
    @DisplayName("CR-03: getReservations lọc bỏ các bản ghi giữ chỗ thuộc dự án ngoài DataScope của user")
    void shouldFilterReservationsByDataScope() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(
                new Employee(new EmployeeId(pmEmployeeId), new UserId(pmUserId), 1L, "PM01", "PM User", false, 40, EmployeeStatus.ACTIVE)
        ));

        Project otherProject = new Project(
                new ProjectId(200L), "PRJ-OTHER", "Dự án PM khác", 1L, new EmployeeId(999L),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.valueOf(500),
                "Mô tả", ProjectStatus.PLANNED, new UserId(999L), LocalDateTime.now(), null, 0L, 0
        );

        ResourceReservation rInScope = ResourceReservation.createNew(projectId, targetEmployeeId, yearWeek, BigDecimal.valueOf(10.0), "Scope OK", pmUserId);
        ResourceReservation rOutScope = ResourceReservation.createNew(200L, targetEmployeeId, yearWeek, BigDecimal.valueOf(15.0), "Outside Scope", 999L);

        when(loadReservationPort.findReservations(null, null, null, null, null)).thenReturn(List.of(rInScope, rOutScope));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(plannedProject));
        when(loadProjectPort.findById(new ProjectId(200L))).thenReturn(Optional.of(otherProject));
        when(loadEmployeePort.findById(new EmployeeId(targetEmployeeId))).thenReturn(Optional.of(targetEmployee));

        List<ResourceReservationResult> results = service.getReservations(null, null, null, null, null);

        assertEquals(1, results.size());
        assertEquals(projectId, results.get(0).projectId());
    }

    @Test
    @DisplayName("CR-04: autoConvertForProject ném ngoại lệ khi tổng phân bổ sau chuyển đổi vượt quá năng lực khả dụng")
    void shouldThrowWhenAutoConvertExceedsAvailableCapacity() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(plannedProject));
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(
                new Employee(new EmployeeId(pmEmployeeId), new UserId(pmUserId), 1L, "PM01", "PM User", false, 40, EmployeeStatus.ACTIVE)
        ));

        ResourceReservation res = ResourceReservation.createNew(projectId, targetEmployeeId, yearWeek, BigDecimal.valueOf(25.0), "Giữ 25h", pmUserId);
        when(loadReservationPort.findActiveByProjectId(projectId)).thenReturn(List.of(res));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(targetEmployeeId, yearWeek)).thenReturn(Optional.of(
                new WeeklyAvailability(1L, targetEmployeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40.0))
        ));

        // Đã phân bổ 20h cho dự án khác. 20h + 25h = 45h > 40h -> Vượt capacity!
        WeeklyProjectAllocation existingAlloc = new WeeklyProjectAllocation(1L, targetEmployeeId, 888L, yearWeek, BigDecimal.valueOf(20.0));
        when(loadAllocationPort.loadAllocationsForEmployee(targetEmployeeId, yearWeek)).thenReturn(List.of(existingAlloc));

        InvalidReservationDataException ex = assertThrows(InvalidReservationDataException.class, () ->
                service.autoConvertForProject(projectId)
        );
        assertTrue(ex.getMessage().contains("vượt quá năng lực khả dụng"));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("CR-05: autoCancelForProject và autoConvertForProject từ chối khi thao tác dự án ngoài DataScope")
    void shouldDenyAutoOperationsWhenProjectOutsideDataScope() {
        Project otherProject = new Project(
                new ProjectId(200L), "PRJ-OTHER", "Dự án PM khác", 1L, new EmployeeId(999L),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.valueOf(500),
                "Mô tả", ProjectStatus.PLANNED, new UserId(999L), LocalDateTime.now(), null, 0L, 0
        );

        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(new ProjectId(200L))).thenReturn(Optional.of(otherProject));
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(
                new Employee(new EmployeeId(pmEmployeeId), new UserId(pmUserId), 1L, "PM01", "PM User", false, 40, EmployeeStatus.ACTIVE)
        ));

        assertThrows(PermissionDeniedException.class, () -> service.autoCancelForProject(200L, "Cancel"));
        assertThrows(PermissionDeniedException.class, () -> service.autoConvertForProject(200L));

        verify(saveDeniedAuditLogPort, times(2)).save(any());
    }

    @Test
    @DisplayName("Hủy thủ công bản ghi giữ chỗ thành công (CancelReservation)")
    void shouldCancelReservationManually() {
        ResourceReservation activeRes = ResourceReservation.createNew(projectId, targetEmployeeId, yearWeek, BigDecimal.valueOf(20.0), "Note", pmUserId);
        when(authorizationService.requireAny(PermissionCode.RESOURCE_RESERVATION_MANAGE, PermissionCode.RESOURCE_RESERVATION_CREATE))
                .thenReturn(pmUserId);
        when(loadUserPort.findById(new UserId(pmUserId))).thenReturn(Optional.of(pmUser));
        when(loadReservationPort.findById(10L)).thenReturn(Optional.of(activeRes));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(plannedProject));
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(
                new Employee(new EmployeeId(pmEmployeeId), new UserId(pmUserId), 1L, "PM01", "PM User", false, 40, EmployeeStatus.ACTIVE)
        ));
        when(saveReservationPort.save(any(ResourceReservation.class))).thenAnswer(inv -> inv.getArgument(0));

        CancelReservationCommand command = new CancelReservationCommand(10L, "Thay đổi kế hoạch");
        ResourceReservationResult result = service.cancelReservation(command);

        assertNotNull(result);
        assertEquals(ReservationStatus.CANCELLED, result.status());
        assertEquals("Thay đổi kế hoạch", result.cancelledReason());
        verify(saveAuditLogPort, times(1)).save(any());
    }
}
