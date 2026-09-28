package com.hrm.employeemanagement.application.dto.task;

import java.util.List;

public record TaskBoardResult(
        List<TaskBoardCardResult> todoTasks,
        List<TaskBoardCardResult> inProgressTasks,
        List<TaskBoardCardResult> inReviewTasks,
        List<TaskBoardCardResult> doneTasks,
        List<TaskBoardCardResult> cancelledTasks,
        int totalTasks,
        Long projectIdFilter,
        Long employeeIdFilter
) {
}
