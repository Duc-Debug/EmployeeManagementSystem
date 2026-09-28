package com.hrm.employeemanagement.domain.skill;

public enum ProficiencyLevel {
    BASIC(1), INTERMEDIATE(2), ADVANCED(3), PROFICIENT(4), EXPERT(5);
    private final int value;

    ProficiencyLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ProficiencyLevel fromValue(int value) {
        for (ProficiencyLevel level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("Mức thành thạo phải từ 1 đến 5");
    }
}
