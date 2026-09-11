package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.task;

import java.util.List;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.task.MyTaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyTasksUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/tasks")
public class MyTaskController {

    private final GetMyTasksUseCase getMyTasksUseCase;

    public MyTaskController(GetMyTasksUseCase getMyTasksUseCase) {
        this.getMyTasksUseCase = Objects.requireNonNull(getMyTasksUseCase, "GetMyTasksUseCase must not be null");
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MyTaskResult>>> getMyTasks() {
        List<MyTaskResult> tasks = getMyTasksUseCase.getMyTasks();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách công việc của tôi thành công", tasks));
    }
}

