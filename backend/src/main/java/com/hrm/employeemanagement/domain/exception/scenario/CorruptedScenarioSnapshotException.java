package com.hrm.employeemanagement.domain.exception.scenario;

public class CorruptedScenarioSnapshotException extends RuntimeException {
    public CorruptedScenarioSnapshotException(String message) {
        super(message);
    }

    public CorruptedScenarioSnapshotException(String message, Throwable cause) {
        super(message, cause);
    }
}
