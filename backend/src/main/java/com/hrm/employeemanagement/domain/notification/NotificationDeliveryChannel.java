package com.hrm.employeemanagement.domain.notification;

public enum NotificationDeliveryChannel {
    ALL,
    IN_APP_ONLY,
    EMAIL_ONLY,
    NONE;

    public boolean isEmailEnabled() {
        return this == ALL || this == EMAIL_ONLY;
    }

    public boolean isInAppEnabled() {
        return this == ALL || this == IN_APP_ONLY;
    }

    public static NotificationDeliveryChannel fromFlags(boolean inApp, boolean email) {
        if (inApp && email) {
            return ALL;
        } else if (inApp) {
            return IN_APP_ONLY;
        } else if (email) {
            return EMAIL_ONLY;
        } else {
            return NONE;
        }
    }
}
