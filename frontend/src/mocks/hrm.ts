import type {
  OrgUnitTreeNode,
  PermissionDefinition,
  ReadOnlyEffectivePermissionFixture,
  Role,
  User,
} from "../types/hrm";

/**
 * Demo-only role labels mirror the backend role seed. Do not use these values
 * to infer authorization; the backend remains the source of truth.
 */
export const DEMO_ROLES = [
  { code: "VT-01", name: "Ban giám đốc" },
  { code: "VT-02", name: "Quản lý dự án" },
  { code: "VT-03", name: "Quản lý nguồn lực" },
  { code: "VT-04", name: "Nhân viên chuyên môn" },
  { code: "VT-05", name: "Nhân sự" },
  { code: "VT-06", name: "Quản trị viên" },
  { code: "VT-07", name: "Nhân viên công ty" },
] as const satisfies readonly Role[];

/**
 * Minimal tree-shaped demonstration data. It deliberately has no manager,
 * headcount, or member details because the tree API does not provide them.
 */
export const DEMO_ORG_UNIT_TREE = [
  {
    id: 9000,
    unitCode: "DEMO-COMPANY",
    unitName: "Công ty demo",
    unitType: "COMPANY",
    parentId: null,
    treePath: "/9000/",
    level: 0,
    status: "ACTIVE",
    description: null,
    managerId: null,
    children: [
      {
        id: 9001,
        unitCode: "DEMO-CENTER",
        unitName: "Khối demo",
        unitType: "CENTER",
        parentId: 9000,
        treePath: "/9000/9001/",
        level: 1,
        status: "ACTIVE",
        description: null,
        managerId: null,
        children: [],
      },
    ],
  },
] as const satisfies readonly OrgUnitTreeNode[];

/** Clearly labeled UI fixtures; these are not accounts in the backend. */
export const DEMO_USERS = [
  {
    id: 10001,
    username: "demo.admin",
    roleCode: "VT-06",
    roleName: "Quản trị viên",
    status: "ACTIVE",
    employeeId: 20001,
    fullName: "Người dùng demo quản trị",
    orgUnitId: 9000,
    orgUnitName: "Công ty demo",
    dataScope: "COMPANY",
    scopeOrgUnitId: null,
  },
  {
    id: 10002,
    username: "demo.branch",
    roleCode: "VT-03",
    roleName: "Quản lý nguồn lực",
    status: "ACTIVE",
    employeeId: 20002,
    fullName: "Người dùng demo đơn vị",
    orgUnitId: 9001,
    orgUnitName: "Khối demo",
    dataScope: "ORGANIZATION_BRANCH",
    scopeOrgUnitId: 9001,
  },
  {
    id: 10003,
    username: "demo.employee",
    roleCode: "VT-07",
    roleName: "Nhân viên công ty",
    status: "LOCKED",
    employeeId: 20003,
    fullName: "Người dùng demo cá nhân",
    orgUnitId: 9001,
    orgUnitName: "Khối demo",
    dataScope: "SELF",
    scopeOrgUnitId: null,
  },
] as const satisfies readonly User[];

/** Names and descriptions reflect the backend permission seed only. */
export const DEMO_PERMISSION_CATALOG = [
  {
    code: "USER_READ",
    name: "Xem tài khoản",
    description: "Cho phép xem danh sách và thông tin tài khoản",
  },
  {
    code: "USER_CREATE",
    name: "Tạo tài khoản",
    description: "Cho phép tạo tài khoản mới",
  },
  {
    code: "USER_UPDATE_ROLE",
    name: "Phân quyền tài khoản",
    description: "Cho phép thay đổi vai trò và phạm vi dữ liệu của tài khoản",
  },
  {
    code: "USER_TOGGLE_STATUS",
    name: "Khóa hoặc mở tài khoản",
    description: "Cho phép thay đổi trạng thái hoạt động của tài khoản",
  },
  {
    code: "ORG_UNIT_READ",
    name: "Xem cơ cấu tổ chức",
    description: "Cho phép xem cây tổ chức",
  },
  {
    code: "PROJECT_READ",
    name: "Xem dự án",
    description: "Cho phép xem danh sách và thông tin dự án",
  },
  {
    code: "EMPLOYEE_READ",
    name: "Xem nhân viên",
    description: "Cho phép xem thông tin nhân viên",
  },
  {
    code: "EMPLOYEE_UPDATE",
    name: "Cập nhật nhân viên",
    description: "Cho phép cập nhật thông tin nhân viên",
  },
] as const satisfies readonly PermissionDefinition[];

/**
 * Placeholder for a read-only permissions panel. It grants nothing because an
 * actual effective-permissions API is not available yet.
 */
export const DEMO_READ_ONLY_EFFECTIVE_PERMISSIONS = {
  source: "DEMO_READ_ONLY",
  availability: "NOT_EXPOSED_BY_API",
  resolvedForUserId: null,
  permissions: [],
  note: "Dữ liệu quyền hiệu lực cần được cung cấp bởi API backend.",
} as const satisfies ReadOnlyEffectivePermissionFixture;
