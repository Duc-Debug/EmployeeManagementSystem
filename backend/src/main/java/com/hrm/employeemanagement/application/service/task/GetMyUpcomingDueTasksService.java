package com.hrm.employeemanagement.application.service.task;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.hrm.employeemanagement.application.dto.task.UpcomingDueTaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyUpcomingDueTasksUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDueReminderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.notification.NotificationAccessDeniedException;
import com.hrm.employeemanagement.domain.notification.TaskDueReminderPolicy;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Service lấy danh sách công việc sắp đến hạn của Nhân viên chuyên môn (VT-04) (TC-03).
 * Kiểm tra vai trò người dùng và ghi nhật ký kiểm toán nếu bị từ chối truy cập.
 * Pure Java, không sử dụng Spring annotations trực tiếp.
 */
public class GetMyUpcomingDueTasksService implements GetMyUpcomingDueTasksUseCase {

    private final GetAuthenticatedUserPort getAuthenticatedUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadTaskDueReminderPort loadTaskDueReminderPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;
    private final Clock clock;

    public GetMyUpcomingDueTasksService(
            GetAuthenticatedUserPort getAuthenticatedUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadTaskDueReminderPort loadTaskDueReminderPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
            Clock clock
    ) {
        this.getAuthenticatedUserPort = Objects.requireNonNull(getAuthenticatedUserPort, "getAuthenticatedUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "loadEmployeePort must not be null");
        this.loadTaskDueReminderPort = Objects.requireNonNull(loadTaskDueReminderPort, "loadTaskDueReminderPort must not be null");
        this.deniedAuditLogPort = deniedAuditLogPort;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public GetMyUpcomingDueTasksService(
            GetAuthenticatedUserPort getAuthenticatedUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadTaskDueReminderPort loadTaskDueReminderPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort
    ) {
        this(getAuthenticatedUserPort, loadEmployeePort, loadTaskDueReminderPort, deniedAuditLogPort, Clock.systemDefaultZone());
    }

    @Override
    public List<UpcomingDueTaskResult> execute() {
        User currentUser = getAuthenticatedUserPort.getAuthenticatedUser();
        if (currentUser == null) {
            throw new NotificationAccessDeniedException("Không tìm thấy thông tin người dùng đã xác thực");
        }

        // 1. Phân quyền theo TC-03: Chỉ cho phép Nhân viên chuyên môn (VT-04)
        boolean isSpecialist = currentUser.getRole() != null &&
                (currentUser.getRole().getCode() == RoleCode.VT_04 || "VT-04".equalsIgnoreCase(currentUser.getRole().getCode().getCode()));

        if (!isSpecialist) {
            String roleCodeStr = currentUser.getRole() != null && currentUser.getRole().getCode() != null
                    ? currentUser.getRole().getCode().getCode()
                    : "UNKNOWN";

            if (deniedAuditLogPort != null) {
                deniedAuditLogPort.save(
                        AuditLog.createChange(
                                currentUser.getIdValue(),
                                "PERMISSION_DENIED",
                                "task_due_reminders",
                                null,
                                null,
                                "role=" + roleCodeStr + ";reason=ROLE_NOT_SPECIALIST"
                        )
                );
            }

            throw new NotificationAccessDeniedException(
                    "Người dùng không thuộc vai trò Nhân viên chuyên môn (VT-04), không có quyền truy cập chức năng nhắc việc sắp đến hạn"
            );
        }

        // 2. Tra cứu Employee hồ sơ gắn với User (kèm cơ chế fallback qua currentUser.getEmployeeId())
        Optional<Employee> employeeOpt = loadEmployeePort.findByUserId(currentUser.getId());
        if (employeeOpt.isEmpty() && currentUser.getEmployeeId() != null) {
            employeeOpt = loadEmployeePort.findById(currentUser.getEmployeeId());
        }

        EmployeeId employeeId = employeeOpt.map(Employee::getId).orElse(currentUser.getEmployeeId());
        if (employeeId == null) {
            return List.of();
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate toDate = today.plusDays(TaskDueReminderPolicy.DEFAULT_DUE_SOON_DAYS);

        // 3. Tải danh sách công việc sắp đến hạn trong 3 ngày tới
        List<Task> tasks = loadTaskDueReminderPort.findUpcomingTasksByAssignee(employeeId, today, toDate);
        List<UpcomingDueTaskResult> results = new ArrayList<>();

        for (Task task : tasks) {
            if (!TaskDueReminderPolicy.isEligibleStatus(task.getStatus())) {
                continue;
            }

            LocalDate dueDate = task.getDueDate() != null ? task.getDueDate() : task.getPlannedEndDate();
            if (dueDate == null) {
                continue;
            }

            long daysRemaining = TaskDueReminderPolicy.calculateDaysRemaining(dueDate, today);
            String directUrl = TaskDueReminderPolicy.buildDirectTaskUrl(task.getProjectIdValue(), task.getIdValue());

            results.add(new UpcomingDueTaskResult(
                    task.getIdValue(),
                    task.getProjectIdValue(),
                    task.getTaskCode(),
                    task.getName(),
                    dueDate,
                    daysRemaining,
                    task.getStatus() != null ? task.getStatus().name() : null,
                    directUrl
            ));
        }

        return results;
    }
}
