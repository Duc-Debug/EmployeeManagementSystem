package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.task.TaskDueReminderScanResult;
import com.hrm.employeemanagement.application.dto.task.UpcomingDueTaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyUpcomingDueTasksUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.ScanAndSendTaskDueRemindersUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

/**
 * REST Controller cung cấp các API cho chức năng Nhắc việc sắp đến hạn (NCL-11-CN-004):
 * 1. GET /api/v1/tasks/due-reminders/my-tasks: Dành cho Nhân viên chuyên môn (VT-04) xem các công việc sắp đến hạn.
 * 2. POST /api/v1/tasks/due-reminders/scan: Kích hoạt rà soát và gửi thông báo nhắc việc theo QTN-19.
 */
@RestController
@RequestMapping("/api/v1/tasks/due-reminders")
@PreAuthorize("isAuthenticated()")
public class TaskDueReminderController {

    private final GetMyUpcomingDueTasksUseCase getMyUpcomingDueTasksUseCase;
    private final ScanAndSendTaskDueRemindersUseCase scanAndSendTaskDueRemindersUseCase;

    public TaskDueReminderController(
            GetMyUpcomingDueTasksUseCase getMyUpcomingDueTasksUseCase,
            ScanAndSendTaskDueRemindersUseCase scanAndSendTaskDueRemindersUseCase
    ) {
        this.getMyUpcomingDueTasksUseCase = Objects.requireNonNull(getMyUpcomingDueTasksUseCase, "getMyUpcomingDueTasksUseCase must not be null");
        this.scanAndSendTaskDueRemindersUseCase = Objects.requireNonNull(scanAndSendTaskDueRemindersUseCase, "scanAndSendTaskDueRemindersUseCase must not be null");
    }

    /**
     * Mở chức năng xem danh sách công việc sắp đến hạn của chính mình.
     * Yêu cầu vai trò Nhân viên chuyên môn (VT-04) theo TC-03.
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<ApiResponse<List<UpcomingDueTaskResult>>> getMyUpcomingDueTasks() {
        List<UpcomingDueTaskResult> tasks = getMyUpcomingDueTasksUseCase.execute();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách công việc sắp đến hạn thành công", tasks));
    }

    /**
     * Kích hoạt rà soát và gửi thông báo nhắc việc sắp đến hạn (hỗ trợ kiểm thử và chạy thủ công).
     * Phân quyền: Chỉ Quản trị viên (VT-06 / Admin) hoặc Ban Giám Đốc (VT-01) mới có quyền kích hoạt batch operation toàn hệ thống.
     * Tuân thủ quy tắc chống gửi trùng QTN-19 (TC-01, TC-02, TC-04).
     */
    @PostMapping("/scan")
    @PreAuthorize("hasAnyAuthority('VT-01', 'VT-06', 'ROLE_ADMIN', 'ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<TaskDueReminderScanResult>> scanAndSendReminders(
            @RequestParam(name = "scanDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scanDate
    ) {
        LocalDate effectiveDate = scanDate != null ? scanDate : LocalDate.now();
        TaskDueReminderScanResult result = scanAndSendTaskDueRemindersUseCase.execute(effectiveDate);
        return ResponseEntity.ok(ApiResponse.success("Rà soát và gửi thông báo nhắc việc thành công", result));
    }
}
