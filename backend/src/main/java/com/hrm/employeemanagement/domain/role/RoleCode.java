package com.hrm.employeemanagement.domain.role;

public enum RoleCode {
    VT_01("VT-01", "Ban giám đốc"),
    VT_02("VT-02", "Quản lý dự án"),
    VT_03("VT-03", "Quản lý nguồn lực"),
    VT_04("VT-04", "Nhân viên chuyên môn"),
    VT_05("VT-05", "Nhân sự"),
    VT_06("VT-06", "Quản trị viên"),
    VT_07("VT-07", "Nhân viên công ty");

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
        for (RoleCode rc : values()) {
            if (rc.code.equalsIgnoreCase(code) || rc.name().equalsIgnoreCase(code)) {
                return rc;
            }
        }
        throw new IllegalArgumentException("Mã vai trò không hợp lệ: " + code);
    }
}
