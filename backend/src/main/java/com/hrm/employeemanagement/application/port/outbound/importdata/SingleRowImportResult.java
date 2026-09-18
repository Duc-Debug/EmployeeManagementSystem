package com.hrm.employeemanagement.application.port.outbound.importdata;

public record SingleRowImportResult(
        boolean success,
        String errorMessage
) {
    public static SingleRowImportResult ofSuccess() {
        return new SingleRowImportResult(true, null);
    }

    public static SingleRowImportResult ofFailure(String errorMessage) {
        return new SingleRowImportResult(false, errorMessage);
    }
}