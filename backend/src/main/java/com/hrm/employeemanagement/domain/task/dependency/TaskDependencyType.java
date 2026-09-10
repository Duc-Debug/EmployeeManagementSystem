package com.hrm.employeemanagement.domain.task.dependency;

public enum TaskDependencyType {
    FINISH_TO_START("FS", "Hoàn tất mới được bắt đầu"),
    START_TO_START("SS", "Cùng bắt đầu"),
    FINISH_TO_FINISH("FF", "Cùng kết thúc"),
    START_TO_FINISH("SF", "Bắt đầu mới được kết thúc");

    private final String code;
    private final String description;

    TaskDependencyType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
