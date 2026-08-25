import type {
  OrgUnitTreeNode,
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
    unitCode: "COMPANY",
    unitName: "Công ty",
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
        unitCode: "OPERATIONS",
        unitName: "Khối vận hành",
        unitType: "CENTER",
        parentId: 9000,
        treePath: "/9000/9001/",
        level: 1,
        status: "ACTIVE",
        description: null,
        managerId: null,
        children: [
          {
            id: 9003,
            unitCode: "HR",
            unitName: "Phòng Nhân sự",
            unitType: "DEPARTMENT",
            parentId: 9001,
            treePath: "/9000/9001/9003/",
            level: 2,
            status: "ACTIVE",
            description: null,
            managerId: null,
            children: [],
          },
        ],
      },
      {
        id: 9002,
        unitCode: "TECHNOLOGY",
        unitName: "Khối Công nghệ",
        unitType: "CENTER",
        parentId: 9000,
        treePath: "/9000/9002/",
        level: 1,
        status: "ACTIVE",
        description: null,
        managerId: null,
        children: [
          {
            id: 9004,
            unitCode: "PLATFORM",
            unitName: "Nhóm Nền tảng",
            unitType: "TEAM",
            parentId: 9002,
            treePath: "/9000/9002/9004/",
            level: 2,
            status: "ACTIVE",
            description: null,
            managerId: null,
            children: [],
          },
        ],
      },
    ],
  },
] as const satisfies readonly OrgUnitTreeNode[];

/** Clearly labeled UI fixtures; these are not accounts in the backend. */
export const DEMO_USERS = [
  {
    id: 10001,
    username: "minh.anh",
    email: "minh.anh@company.com",
    roleCode: "VT-06",
    roleName: "Quản trị viên",
    status: "ACTIVE",
    employeeId: 20001,
    fullName: "Nguyễn Minh Anh",
    orgUnitId: 9000,
    orgUnitName: "Công ty",
    dataScope: "COMPANY",
    scopeOrgUnitId: null,
  },
  {
    id: 10002,
    username: "quoc.huy",
    email: "quoc.huy@company.com",
    roleCode: "VT-03",
    roleName: "Quản lý nguồn lực",
    status: "ACTIVE",
    employeeId: 20002,
    fullName: "Trần Quốc Huy",
    orgUnitId: 9002,
    orgUnitName: "Khối Công nghệ",
    dataScope: "ORGANIZATION_BRANCH",
    scopeOrgUnitId: 9002,
  },
  {
    id: 10003,
    username: "ngoc.mai",
    email: "ngoc.mai@company.com",
    roleCode: "VT-07",
    roleName: "Nhân viên công ty",
    status: "LOCKED",
    employeeId: 20003,
    fullName: "Lê Ngọc Mai",
    orgUnitId: 9003,
    orgUnitName: "Phòng Nhân sự",
    dataScope: "SELF",
    scopeOrgUnitId: null,
  },
] as const satisfies readonly User[];
