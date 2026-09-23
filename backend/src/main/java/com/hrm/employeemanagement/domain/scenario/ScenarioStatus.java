package com.hrm.employeemanagement.domain.scenario;

public enum ScenarioStatus {
    DRAFT("draft"),
    SAVED("saved"),
    APPLIED("applied"),
    DISCARDED("discarded");

    private final String value;

    ScenarioStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ScenarioStatus fromString(String text) {
        if (text == null) {
            return DRAFT;
        }
        for (ScenarioStatus s : values()) {
            if (s.value.equalsIgnoreCase(text) || s.name().equalsIgnoreCase(text)) {
                return s;
            }
        }
        return DRAFT;
    }
}
