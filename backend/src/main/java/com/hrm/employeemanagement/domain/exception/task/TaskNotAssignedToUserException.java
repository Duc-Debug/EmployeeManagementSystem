package com.hrm.employeemanagement.domain.exception.task;

public class TaskNotAssignedToUserException extends RuntimeException {

    private final Long taskId;
    private final Long employeeId;

    public TaskNotAssignedToUserException(Long taskId, Long employeeId) {
        super("Bạn không có quyền chuyển trạng thái công việc của người khác");
        this.taskId = taskId;
        this.employeeId = employeeId;
    }

    public TaskNotAssignedToUserException(String message) {
        super(message);
        this.taskId = null;
        this.employeeId = null;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }
}
