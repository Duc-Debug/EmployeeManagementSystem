package com.hrm.employeemanagement.domain.role;

public enum RoleCode {
    VT_01("VT-01", "Ban giám đốc"),
    VT_02("VT-02", "Quản lý dự án"),
    VT_03("VT-03", "Quản lý nguồn lực"),
    VT_04("VT-04", "Nhân viên chuyên môn"),
    VT_05("VT-05", "Nhân sự"),
    VT_06("VT-06", "Quản trị viên");

    private final String code;
    private final String name;

    RoleCode(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static RoleCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã vai trò không được để trống");
        }
        String clean = code.trim().toUpperCase();
        for (RoleCode rc : values()) {
            if (rc.code.equalsIgnoreCase(clean) || rc.name().equalsIgnoreCase(clean)) {
                return rc;
            }
        }
        // Chuẩn hóa loại bỏ gạch dưới và prefix ROLE- hoặc ROLE_
        String normalized = clean.replace("_", "-").replace("ROLE-", "");
        for (RoleCode rc : values()) {
            if (rc.code.equalsIgnoreCase(normalized)
                    || rc.code.replace("-", "").equalsIgnoreCase(normalized.replace("-", ""))) {
                return rc;
            }
        }
        // Nhận diện alias lịch sử
        return switch (normalized) {
            case "EXECUTIVE", "DIRECTOR", "BGD", "BAN-GIAM-DOC", "GIAM-DOC" -> VT_01;
            case "PM", "PROJECT-MANAGER", "QUAN-LY-DU-AN" -> VT_02;
            case "RM", "RESOURCE-MANAGER", "QUAN-LY-NGUON-LUC" -> VT_03;
            case "EMPLOYEE", "SPECIALIST", "DEVELOPER", "MEMBER", "STAFF", "DEV", "NHAN-VIEN", "CHUYEN-VIEN" -> VT_04;
            case "HR", "HR-MANAGER", "HR-SPECIALIST", "HUMAN-RESOURCE", "HUMAN-RESOURCES", "NHAN-SU" -> VT_05;
            case "ADMIN", "SYSTEM-ADMIN", "ADMINISTRATOR", "SYS-ADMIN", "QUAN-TRI-VIEN" -> VT_06;
            default -> throw new IllegalArgumentException("Mã vai trò không hợp lệ: " + code);
        };
    }
}
