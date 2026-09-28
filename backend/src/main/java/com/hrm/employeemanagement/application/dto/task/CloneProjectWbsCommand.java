package com.hrm.employeemanagement.application.dto.task;

public record CloneProjectWbsCommand(
        Long targetProjectId,
        Long sourceProjectId
        ) {

}
