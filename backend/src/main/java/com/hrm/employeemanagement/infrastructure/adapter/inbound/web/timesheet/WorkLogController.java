package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.timesheet.AssignedTaskOptionResult;
import com.hrm.employeemanagement.application.dto.timesheet.CreateWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.DeleteWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.UpdateWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.CreateWorkLogUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.DeleteWorkLogUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetMyAssignedTasksForWorkLogUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetWeeklyTimesheetUseCase;
import com.hrm.employeemanagement.application.dto.timesheet.SubmitWeeklyTimesheetCommand;
import com.hrm.employeemanagement.application.port.inbound.timesheet.SubmitWeeklyTimesheetUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.UpdateWorkLogUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet.dto.CreateWorkLogRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet.dto.SubmitTimesheetRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet.dto.UpdateWorkLogRequest;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/work-logs")
public class WorkLogController {

    private final CreateWorkLogUseCase createWorkLogUseCase;
    private final UpdateWorkLogUseCase updateWorkLogUseCase;
    private final DeleteWorkLogUseCase deleteWorkLogUseCase;
    private final GetWeeklyTimesheetUseCase getWeeklyTimesheetUseCase;
    private final GetMyAssignedTasksForWorkLogUseCase getMyAssignedTasksForWorkLogUseCase;
    private final SubmitWeeklyTimesheetUseCase submitWeeklyTimesheetUseCase;

    public WorkLogController(
            CreateWorkLogUseCase createWorkLogUseCase,
            UpdateWorkLogUseCase updateWorkLogUseCase,
            DeleteWorkLogUseCase deleteWorkLogUseCase,
            GetWeeklyTimesheetUseCase getWeeklyTimesheetUseCase,
            GetMyAssignedTasksForWorkLogUseCase getMyAssignedTasksForWorkLogUseCase,
            SubmitWeeklyTimesheetUseCase submitWeeklyTimesheetUseCase) {
        this.createWorkLogUseCase = Objects.requireNonNull(createWorkLogUseCase, "CreateWorkLogUseCase must not be null");
        this.updateWorkLogUseCase = Objects.requireNonNull(updateWorkLogUseCase, "UpdateWorkLogUseCase must not be null");
        this.deleteWorkLogUseCase = Objects.requireNonNull(deleteWorkLogUseCase, "DeleteWorkLogUseCase must not be null");
        this.getWeeklyTimesheetUseCase = Objects.requireNonNull(getWeeklyTimesheetUseCase, "GetWeeklyTimesheetUseCase must not be null");
        this.getMyAssignedTasksForWorkLogUseCase = Objects.requireNonNull(getMyAssignedTasksForWorkLogUseCase, "GetMyAssignedTasksForWorkLogUseCase must not be null");
        this.submitWeeklyTimesheetUseCase = Objects.requireNonNull(submitWeeklyTimesheetUseCase, "SubmitWeeklyTimesheetUseCase must not be null");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkLogResult>> createWorkLog(@Valid @RequestBody CreateWorkLogRequest request) {
        CreateWorkLogCommand command = new CreateWorkLogCommand(
                request.projectId(),
                request.taskId(),
                request.workDate(),
                request.hours(),
                request.isBillable(),
                request.description()
        );
        WorkLogResult result = createWorkLogUseCase.createWorkLog(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ghi giờ làm việc thành công.", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkLogResult>> updateWorkLog(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateWorkLogRequest request) {
        UpdateWorkLogCommand command = new UpdateWorkLogCommand(
                id,
                request.projectId(),
                request.taskId(),
                request.workDate(),
                request.hours(),
                request.isBillable(),
                request.description()
        );
        WorkLogResult result = updateWorkLogUseCase.updateWorkLog(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giờ làm việc thành công.", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkLog(@PathVariable("id") Long id) {
        deleteWorkLogUseCase.deleteWorkLog(new DeleteWorkLogCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa dòng ghi giờ thành công.", null));
    }

    @GetMapping("/my-week")
    public ResponseEntity<ApiResponse<WeeklyTimesheetResult>> getMyWeeklyTimesheet(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        WeeklyTimesheetResult result = getWeeklyTimesheetUseCase.getMyWeeklyTimesheet(date != null ? date : LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin bảng chấm công tuần thành công.", result));
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<ApiResponse<List<AssignedTaskOptionResult>>> getMyAssignedTasks() {
        List<AssignedTaskOptionResult> results = getMyAssignedTasksForWorkLogUseCase.getMyAssignedTasksForWorkLog();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách công việc được phân công thành công.", results));
    }

    @PostMapping("/my-week/submit")
    public ResponseEntity<ApiResponse<WeeklyTimesheetResult>> submitWeeklyTimesheet(
            @RequestBody(required = false) SubmitTimesheetRequest request) {
        LocalDate dateInWeek = request != null && request.dateInWeek() != null ? request.dateInWeek() : LocalDate.now();
        Long timesheetId = request != null ? request.timesheetId() : null;
        WeeklyTimesheetResult result = submitWeeklyTimesheetUseCase.submitWeeklyTimesheet(
                new SubmitWeeklyTimesheetCommand(dateInWeek, timesheetId)
        );
        return ResponseEntity.ok(ApiResponse.success("Nộp bảng chấm công tuần thành công.", result));
    }
}
