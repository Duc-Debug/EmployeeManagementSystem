import type { RoleCode } from "@/types/hrm";

export const CANONICAL_ROLES = {
  EXECUTIVE: "VT-01",
  PM: "VT-02",
  RM: "VT-03",
  SPECIALIST: "VT-04",
  HR: "VT-05",
  ADMIN: "VT-06",
} as const;

export const ALL_CANONICAL_ROLES: readonly RoleCode[] = [
  "VT-01",
  "VT-02",
  "VT-03",
  "VT-04",
  "VT-05",
  "VT-06",
] as const;

const ROLE_ALIAS_MAP: Readonly<Record<string, RoleCode>> = {
  // VT-01 (Ban Giám Đốc)
  "VT-01": "VT-01", "VT01": "VT-01", "VT_01": "VT-01",
  "ROLE-VT-01": "VT-01", "ROLE_VT_01": "VT-01",
  "ROLE-EXECUTIVE": "VT-01", "ROLE_EXECUTIVE": "VT-01", "EXECUTIVE": "VT-01",
  "DIRECTOR": "VT-01", "ROLE-DIRECTOR": "VT-01", "ROLE_DIRECTOR": "VT-01",
  "ROLE-BGD": "VT-01", "ROLE_BGD": "VT-01", "BGD": "VT-01",
  "BAN-GIAM-DOC": "VT-01", "BAN_GIAM_DOC": "VT-01",
  "GIAM-DOC": "VT-01", "GIAM_DOC": "VT-01",

  // VT-02 (Quản Lý Dự Án / PM)
  "VT-02": "VT-02", "VT02": "VT-02", "VT_02": "VT-02",
  "ROLE-VT-02": "VT-02", "ROLE_VT_02": "VT-02",
  "ROLE-PM": "VT-02", "ROLE_PM": "VT-02", "PM": "VT-02",
  "PROJECT-MANAGER": "VT-02", "PROJECT_MANAGER": "VT-02",
  "ROLE-PROJECT-MANAGER": "VT-02", "ROLE_PROJECT_MANAGER": "VT-02",
  "QUAN-LY-DU-AN": "VT-02", "QUAN_LY_DU_AN": "VT-02",

  // VT-03 (Quản Lý Nguồn Lực / RM)
  "VT-03": "VT-03", "VT03": "VT-03", "VT_03": "VT-03",
  "ROLE-VT-03": "VT-03", "ROLE_VT_03": "VT-03",
  "ROLE-RM": "VT-03", "ROLE_RM": "VT-03", "RM": "VT-03",
  "RESOURCE-MANAGER": "VT-03", "RESOURCE_MANAGER": "VT-03",
  "ROLE-RESOURCE-MANAGER": "VT-03", "ROLE_RESOURCE_MANAGER": "VT-03",
  "QUAN-LY-NGUON-LUC": "VT-03", "QUAN_LY_NGUON_LUC": "VT-03",

  // VT-04 (Nhân Viên Chuyên Môn)
  "VT-04": "VT-04", "VT04": "VT-04", "VT_04": "VT-04",
  "ROLE-VT-04": "VT-04", "ROLE_VT_04": "VT-04",
  "ROLE-EMPLOYEE": "VT-04", "ROLE_EMPLOYEE": "VT-04", "EMPLOYEE": "VT-04",
  "ROLE-STAFF": "VT-04", "ROLE_STAFF": "VT-04", "STAFF": "VT-04",
  "SPECIALIST": "VT-04", "DEVELOPER": "VT-04", "DEV": "VT-04", "MEMBER": "VT-04",
  "NHAN-VIEN": "VT-04", "NHAN_VIEN": "VT-04",
  "CHUYEN-VIEN": "VT-04", "CHUYEN_VIEN": "VT-04",

  // VT-05 (Nhân Sự / HR)
  "VT-05": "VT-05", "VT05": "VT-05", "VT_05": "VT-05",
  "ROLE-VT-05": "VT-05", "ROLE_VT_05": "VT-05",
  "ROLE-HR": "VT-05", "ROLE_HR": "VT-05", "HR": "VT-05",
  "HR-MANAGER": "VT-05", "HR_MANAGER": "VT-05",
  "HR-SPECIALIST": "VT-05", "HR_SPECIALIST": "VT-05",
  "HUMAN-RESOURCE": "VT-05", "HUMAN_RESOURCE": "VT-05",
  "HUMAN-RESOURCES": "VT-05", "HUMAN_RESOURCES": "VT-05",
  "NHAN-SU": "VT-05", "NHAN_SU": "VT-05",

  // VT-06 (Quản Trị Viên / Admin)
  "VT-06": "VT-06", "VT06": "VT-06", "VT_06": "VT-06",
  "ROLE-VT-06": "VT-06", "ROLE_VT_06": "VT-06",
  "ROLE-ADMIN": "VT-06", "ROLE_ADMIN": "VT-06", "ADMIN": "VT-06",
  "ADMINISTRATOR": "VT-06", "ROLE-ADMINISTRATOR": "VT-06", "ROLE_ADMINISTRATOR": "VT-06",
  "SYSTEM-ADMIN": "VT-06", "SYSTEM_ADMIN": "VT-06",
  "SYS-ADMIN": "VT-06", "SYS_ADMIN": "VT-06",
  "QUAN-TRI-VIEN": "VT-06", "QUAN_TRI_VIEN": "VT-06",
};

/**
 * Chuẩn hóa bất kỳ mã role hoặc bí danh nào về 1 trong 6 mã chuẩn: VT-01 -> VT-06.
 * Trả về chuỗi rỗng "" nếu không hợp lệ hoặc rỗng.
 */
export function normalizeRoleCode(roleCode?: string | null): RoleCode | "" {
  if (!roleCode || typeof roleCode !== "string") return "";
  const trimmed = roleCode.trim().toUpperCase();
  if (!trimmed) return "";
  if (ROLE_ALIAS_MAP[trimmed]) return ROLE_ALIAS_MAP[trimmed];

  const normalizedHyphen = trimmed.replace(/_/g, "-");
  if (ROLE_ALIAS_MAP[normalizedHyphen]) return ROLE_ALIAS_MAP[normalizedHyphen];

  const strippedRole = normalizedHyphen.replace(/^ROLE-/, "");
  if (ROLE_ALIAS_MAP[strippedRole]) return ROLE_ALIAS_MAP[strippedRole];

  return "";
}

/**
 * Kiểm tra người dùng có mang vai trò mục tiêu hay không.
 */
export function isRole(
  userRole: string | null | undefined,
  target: RoleCode | readonly RoleCode[]
): boolean {
  const canonical = normalizeRoleCode(userRole);
  if (!canonical) return false;
  return Array.isArray(target) ? target.includes(canonical) : canonical === target;
}

// Helper tiện ích định danh nhanh từng vai trò
export const isExecutive = (role?: string | null): boolean => normalizeRoleCode(role) === "VT-01";
export const isPm = (role?: string | null): boolean => normalizeRoleCode(role) === "VT-02";
export const isRm = (role?: string | null): boolean => normalizeRoleCode(role) === "VT-03";
export const isSpecialist = (role?: string | null): boolean => normalizeRoleCode(role) === "VT-04";
export const isHr = (role?: string | null): boolean => normalizeRoleCode(role) === "VT-05";
export const isAdmin = (role?: string | null): boolean => normalizeRoleCode(role) === "VT-06";
