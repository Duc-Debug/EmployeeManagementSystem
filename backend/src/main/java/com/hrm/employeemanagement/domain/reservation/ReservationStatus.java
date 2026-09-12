package com.hrm.employeemanagement.domain.reservation;

public enum ReservationStatus {
    ACTIVE,
    CONVERTED,
    CANCELLED;

    public static ReservationStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        return ReservationStatus.valueOf(value.trim().toUpperCase());
    }
}
