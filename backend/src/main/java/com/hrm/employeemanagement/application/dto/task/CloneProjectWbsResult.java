package com.hrm.employeemanagement.application.dto.task;

import java.util.List;

public record CloneProjectWbsResult(
        Long targetProjectId,
        Long sourceProjectId,
        int totalClonedTasks,
        int totalCategories,
        List<TaskNodeResult> wbsTree
        ) {

}
