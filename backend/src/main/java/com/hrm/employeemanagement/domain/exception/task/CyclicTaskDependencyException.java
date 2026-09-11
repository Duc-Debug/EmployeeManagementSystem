package com.hrm.employeemanagement.domain.exception.task;

import java.util.List;

public class CyclicTaskDependencyException extends RuntimeException {

    private final List<String> cyclePath;

    public CyclicTaskDependencyException(String message, List<String> cyclePath) {
        super(message);
        this.cyclePath = cyclePath;
    }

    public CyclicTaskDependencyException(List<String> cyclePath) {
        super(buildMessage(cyclePath));
        this.cyclePath = cyclePath;
    }

    public List<String> getCyclePath() {
        return cyclePath;
    }

    private static String buildMessage(List<String> cyclePath) {
        if (cyclePath == null || cyclePath.isEmpty()) {
            return "Phát hiện quan hệ phụ thuộc vòng lặp giữa các công việc";
        }
        return "Không thể tạo phụ thuộc: Phát hiện vòng lặp giữa các công việc [" + String.join(" -> ", cyclePath) + "]";
    }
}
