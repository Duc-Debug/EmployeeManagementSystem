package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.port.inbound.allocation.ConfirmScheduleViewedUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetMyAllocationsUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadMyAllocationsPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort.ScheduleConfirmationRecord;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.allocation.ConfirmScheduleViewedService;
import com.hrm.employeemanagement.application.service.allocation.GetMyAllocationsService;
import com.hrm.employeemanagement.domain.allocation.confirmation.AllocationItem;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.TransactionalConfirmScheduleViewedUseCase;
import com.hrm.employeemanagement.infrastructure.transaction.allocation.TransactionalGetMyAllocationsUseCase;

class MyAllocationsControllerIntegrationTest {

    private MockMvc mockMvc;

    private GetAuthenticatedUserPort authenticatedUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadMyAllocationsPort loadMyAllocationsPort;
    private ScheduleConfirmationPort scheduleConfirmationPort;
    private ClientIpResolver clientIpResolver;

    private final UserId userId = new UserId(10L);
    private final EmployeeId employeeId = new EmployeeId(20L);

    @BeforeEach
    void setUp() {
        authenticatedUserPort = mock(GetAuthenticatedUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadMyAllocationsPort = mock(LoadMyAllocationsPort.class);
        scheduleConfirmationPort = mock(ScheduleConfirmationPort.class);
        clientIpResolver = new ClientIpResolver("");

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(mockUser);

        Employee mockEmployee = mock(Employee.class);
        when(mockEmployee.getId()).thenReturn(employeeId);
        when(loadEmployeePort.findByUserId(userId)).thenReturn(Optional.of(mockEmployee));

        GetMyAllocationsService pureGetService = new GetMyAllocationsService(
                authenticatedUserPort, loadEmployeePort, loadMyAllocationsPort, scheduleConfirmationPort
        );
        GetMyAllocationsUseCase getUseCase = new TransactionalGetMyAllocationsUseCase(pureGetService);

        ConfirmScheduleViewedService pureConfirmService = new ConfirmScheduleViewedService(
                authenticatedUserPort, loadEmployeePort, loadMyAllocationsPort, scheduleConfirmationPort
        );
        ConfirmScheduleViewedUseCase confirmUseCase = new TransactionalConfirmScheduleViewedUseCase(pureConfirmService);

        MyAllocationsController controller = new MyAllocationsController(getUseCase, confirmUseCase, clientIpResolver);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MyAllocationsExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("TC-01: Xem lịch mặc định hợp lệ không truyền param -> Trả về 2 tuần, status 200")
    void tc01_GetDefaultAllocations() throws Exception {
        LocalDate currentMonday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(eq(employeeId.value()), any()))
                .thenReturn(Collections.emptyList());
        when(scheduleConfirmationPort.findByUserIdAndWeeks(eq(userId.value()), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/my-allocations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks").isArray())
                .andExpect(jsonPath("$.weeks.length()").value(2))
                .andExpect(jsonPath("$.weeks[0].week_start_date").value(currentMonday.toString()))
                .andExpect(jsonPath("$.weeks[0].confirmation_status").value("NOT_CONFIRMED"));
    }

    @Test
    @DisplayName("TC-02: Cố tình truyền user_id hoặc param lạ -> HTTP 400 INVALID_QUERY_PARAMETER")
    void tc02_RejectInvalidQueryParameter() throws Exception {
        mockMvc.perform(get("/api/v1/my-allocations?user_id=123&weeks=2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_QUERY_PARAMETER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Query parameter 'user_id' is not allowed"));
    }

    @Test
    @DisplayName("TC-03 & TC-03b: Weeks clamp biên [1, 8] và lỗi format khi truyền string abc")
    void tc03_WeeksClampingAndFormatting() throws Exception {
        // weeks = abc -> HTTP 400 INVALID_WEEKS_FORMAT
        mockMvc.perform(get("/api/v1/my-allocations?weeks=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_WEEKS_FORMAT"))
                .andExpect(jsonPath("$.status").value(400));

        // weeks = 99 -> Clamped to 8
        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(eq(employeeId.value()), any()))
                .thenReturn(Collections.emptyList());
        when(scheduleConfirmationPort.findByUserIdAndWeeks(eq(userId.value()), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/my-allocations?weeks=99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks.length()").value(8));

        // weeks = -1 -> Clamped to 1
        mockMvc.perform(get("/api/v1/my-allocations?weeks=-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks.length()").value(1));
    }

    @Test
    @DisplayName("TC-04: Tuần chưa có phân bổ -> total_hours = 0.0, confirmation_status = NOT_CONFIRMED")
    void tc04_EmptyWeek() throws Exception {
        LocalDate monday = LocalDate.of(2026, 10, 5);
        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employeeId.value(), monday))
                .thenReturn(Collections.emptyList());
        when(scheduleConfirmationPort.findByUserIdAndWeeks(eq(userId.value()), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/my-allocations?week_start=2026-10-05&weeks=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks[0].total_hours").value(0.0))
                .andExpect(jsonPath("$.weeks[0].confirmation_status").value("NOT_CONFIRMED"))
                .andExpect(jsonPath("$.weeks[0].allocations").isEmpty());
    }

    @Test
    @DisplayName("TC-05: Double click nút xác nhận -> Lần 1: 201 Created; Lần 2: 200 OK (already_confirmed: true)")
    void tc05_DoubleClickConfirmation() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        LocalDateTime firstConfirmedAt = LocalDateTime.of(2026, 9, 18, 10, 0);

        // Lần 1: Chưa có bản ghi -> Lưu mới
        when(scheduleConfirmationPort.findByUserIdAndWeek(userId.value(), monday))
                .thenReturn(Optional.empty());
        when(scheduleConfirmationPort.saveConfirmation(eq(userId.value()), eq(monday), any(), any()))
                .thenReturn(new ScheduleConfirmationPort.SaveConfirmationResult(
                        new ScheduleConfirmationRecord(1L, userId.value(), monday, firstConfirmedAt, "127.0.0.1"),
                        true
                ));

        mockMvc.perform(post("/api/v1/my-allocations/2026-09-21/confirm-viewed"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.already_confirmed").value(false))
                .andExpect(jsonPath("$.confirmation_status").value("CONFIRMED"));

        // Lần 2: Đã có bản ghi và dữ liệu phân bổ không đổi -> Trả 200 OK already_confirmed: true
        when(scheduleConfirmationPort.findByUserIdAndWeek(userId.value(), monday))
                .thenReturn(Optional.of(new ScheduleConfirmationRecord(1L, userId.value(), monday, firstConfirmedAt, "127.0.0.1")));

        mockMvc.perform(post("/api/v1/my-allocations/2026-09-21/confirm-viewed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.already_confirmed").value(true))
                .andExpect(jsonPath("$.confirmation_status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("TC-05b: Concurrency race condition khi confirm đồng thời -> Request race thua nhận HTTP 200 OK, already_confirmed: true")
    void tc05b_ConcurrentRaceConditionConfirmation() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        LocalDateTime now = LocalDateTime.now();

        // Giả lập Request 2 thấy bản ghi chưa tồn tại khi đọc (empty)
        when(scheduleConfirmationPort.findByUserIdAndWeek(userId.value(), monday))
                .thenReturn(Optional.empty());

        // Adapter bắt duplicate key và trả về newlyCreated = false
        when(scheduleConfirmationPort.saveConfirmation(eq(userId.value()), eq(monday), any(), any()))
                .thenReturn(new ScheduleConfirmationPort.SaveConfirmationResult(
                        new ScheduleConfirmationRecord(1L, userId.value(), monday, now, "127.0.0.1"),
                        false
                ));

        // Request race thua phải nhận HTTP 200 OK idempotent và already_confirmed = true
        mockMvc.perform(post("/api/v1/my-allocations/2026-09-21/confirm-viewed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.already_confirmed").value(true))
                .andExpect(jsonPath("$.confirmation_status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("TC-06: Phân bổ bị sửa sau khi xác nhận -> confirmation_status = STALE")
    void tc06_StaleStatusDetection() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 9, 18, 10, 0);

        // Allocation có updated_at lúc 11:00 > confirmed_at lúc 10:00 -> STALE
        AllocationItem modifiedItem = new AllocationItem(
                100L, 1L, "Dự án HRM", "ACTIVE", new BigDecimal("40.00"), confirmedAt.plusHours(1)
        );

        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employeeId.value(), monday))
                .thenReturn(List.of(modifiedItem));
        when(scheduleConfirmationPort.findByUserIdAndWeeks(eq(userId.value()), any()))
                .thenReturn(List.of(new ScheduleConfirmationRecord(1L, userId.value(), monday, confirmedAt, "127.0.0.1")));

        mockMvc.perform(get("/api/v1/my-allocations?week_start=2026-09-21&weeks=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks[0].confirmation_status").value("STALE"))
                .andExpect(jsonPath("$.weeks[0].total_hours").value(40.0));
    }

    @Test
    @DisplayName("TC-07: Xác nhận lại khi đang STALE -> HTTP 200 OK, already_confirmed: false, previous_stale: true")
    void tc07_ReconfirmWhenStale() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        LocalDateTime oldConfirmedAt = LocalDateTime.of(2026, 9, 18, 10, 0);

        AllocationItem modifiedItem = new AllocationItem(
                100L, 1L, "Dự án HRM", "ACTIVE", new BigDecimal("40.00"), oldConfirmedAt.plusHours(1)
        );
        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employeeId.value(), monday))
                .thenReturn(List.of(modifiedItem));
        when(scheduleConfirmationPort.findByUserIdAndWeek(userId.value(), monday))
                .thenReturn(Optional.of(new ScheduleConfirmationRecord(1L, userId.value(), monday, oldConfirmedAt, "127.0.0.1")));

        LocalDateTime newConfirmedAt = LocalDateTime.of(2026, 9, 18, 12, 0);
        when(scheduleConfirmationPort.saveConfirmation(eq(userId.value()), eq(monday), any(), any()))
                .thenReturn(new ScheduleConfirmationPort.SaveConfirmationResult(
                        new ScheduleConfirmationRecord(1L, userId.value(), monday, newConfirmedAt, "127.0.0.1"),
                        false
                ));

        mockMvc.perform(post("/api/v1/my-allocations/2026-09-21/confirm-viewed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.already_confirmed").value(false))
                .andExpect(jsonPath("$.previous_confirmation_was_stale").value(true))
                .andExpect(jsonPath("$.confirmation_status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("TC-08a & TC-08b: Bảo mật Client IP - Chặn untrusted spoofing và tin cậy trusted proxy")
    void tc08_ClientIpSecurity() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        when(scheduleConfirmationPort.findByUserIdAndWeek(userId.value(), monday))
                .thenReturn(Optional.empty());

        // TC-08a: Untrusted client trực tiếp gửi X-Forwarded-For -> Bị từ chối, lưu remoteAddr
        when(scheduleConfirmationPort.saveConfirmation(eq(userId.value()), eq(monday), any(), eq("203.0.113.195")))
                .thenReturn(new ScheduleConfirmationPort.SaveConfirmationResult(
                        new ScheduleConfirmationRecord(1L, userId.value(), monday, LocalDateTime.now(), "203.0.113.195"),
                        true
                ));

        mockMvc.perform(post("/api/v1/my-allocations/2026-09-21/confirm-viewed")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.195");
                            return request;
                        })
                        .header("X-Forwarded-For", "198.51.100.1"))
                .andExpect(status().isCreated());

        // TC-08b: Trusted reverse proxy gửi X-Forwarded-For -> Lưu đúng client IP đầu tiên
        when(scheduleConfirmationPort.saveConfirmation(eq(userId.value()), eq(monday), any(), eq("198.51.100.1")))
                .thenReturn(new ScheduleConfirmationPort.SaveConfirmationResult(
                        new ScheduleConfirmationRecord(1L, userId.value(), monday, LocalDateTime.now(), "198.51.100.1"),
                        true
                ));

        mockMvc.perform(post("/api/v1/my-allocations/2026-09-21/confirm-viewed")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1"); // Trusted proxy
                            return request;
                        })
                        .header("X-Forwarded-For", "198.51.100.1, 10.0.0.1"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-09: Phân bổ vào dự án CLOSED -> Hiển thị đầy đủ, nhãn CLOSED, và tham gia tính MAX(updated_at)")
    void tc09_ClosedProjectHandling() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        LocalDateTime updateTime = LocalDateTime.of(2026, 9, 18, 9, 0);

        AllocationItem closedProjItem = new AllocationItem(
                105L, 99L, "Dự án Cũ Đã Đóng", "CLOSED", new BigDecimal("20.00"), updateTime
        );

        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employeeId.value(), monday))
                .thenReturn(List.of(closedProjItem));
        when(scheduleConfirmationPort.findByUserIdAndWeeks(eq(userId.value()), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/my-allocations?week_start=2026-09-21&weeks=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks[0].allocations[0].project_name").value("Dự án Cũ Đã Đóng"))
                .andExpect(jsonPath("$.weeks[0].allocations[0].project_status").value("CLOSED"))
                .andExpect(jsonPath("$.weeks[0].allocations[0].allocated_hours").value(20.0));
    }

    @Test
    @DisplayName("TC-10: Sai định dạng ngày -> HTTP 400 INVALID_WEEK_FORMAT")
    void tc10_InvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/v1/my-allocations?week_start=2026-13-45"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_WEEK_FORMAT"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Sai định dạng ngày: '2026-13-45'. Định dạng hợp lệ là YYYY-MM-DD"));

        mockMvc.perform(post("/api/v1/my-allocations/2026-13-45/confirm-viewed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_WEEK_FORMAT"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("TC-11: Hard-delete limitation (Negative test) - Xóa cứng không cập nhật updated_at -> Không phát hiện STALE")
    void tc11_HardDeleteKnownLimitation() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 9, 18, 10, 0);

        // Khi 1 phân bổ bị xóa cứng dưới DB mà các phân bổ còn lại có updated_at cũ (< confirmedAt)
        AllocationItem remainingItem = new AllocationItem(
                101L, 2L, "Dự án Còn Lại", "ACTIVE", new BigDecimal("20.00"), confirmedAt.minusHours(2)
        );

        when(loadMyAllocationsPort.loadAllocationsForEmployeeInWeek(employeeId.value(), monday))
                .thenReturn(List.of(remainingItem));
        when(scheduleConfirmationPort.findByUserIdAndWeeks(eq(userId.value()), any()))
                .thenReturn(List.of(new ScheduleConfirmationRecord(1L, userId.value(), monday, confirmedAt, "127.0.0.1")));

        mockMvc.perform(get("/api/v1/my-allocations?week_start=2026-09-21&weeks=1"))
                .andExpect(status().isOk())
                // Do MAX(updated_at) của các bản ghi còn lại vẫn < confirmedAt, hệ thống vẫn báo CONFIRMED (đúng theo giới hạn đã đặc tả)
                .andExpect(jsonPath("$.weeks[0].confirmation_status").value("CONFIRMED"));
    }
}