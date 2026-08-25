/** Backend-compatible UI domain models for Employee Management System. */

export const DATA_SCOPES = [
  "COMPANY",
  "ORGANIZATION_BRANCH",
  "SELF",
] as const;

export type DataScope = (typeof DATA_SCOPES)[number];

export const ROLE_CODES = [
  "VT-01",
  "VT-02",
  "VT-03",
  "VT-04",
  "VT-05",
  "VT-06",
  "VT-07",
] as const;

export type RoleCode = (typeof ROLE_CODES)[number];

export type UserStatus = "ACTIVE" | "LOCKED";

export type OrgUnitType = "COMPANY" | "CENTER" | "DEPARTMENT" | "TEAM";

export type OrgUnitStatus = "ACTIVE" | "INACTIVE";

export type PermissionCode =
  | "USER_READ"
  | "USER_CREATE"
  | "USER_UPDATE_ROLE"
  | "USER_TOGGLE_STATUS"
  | "ORG_UNIT_READ"
  | "PROJECT_READ"
  | "EMPLOYEE_READ"
  | "EMPLOYEE_UPDATE";

export interface Role {
  code: RoleCode;
  name: string;
}

/** Matches the backend UserResult payload. */
export interface User {
  id: number;
  username: string;
  roleCode: RoleCode;
  roleName: string;
  status: UserStatus;
  employeeId: number | null;
  fullName: string;
  orgUnitId: number | null;
  orgUnitName: string | null;
  dataScope: DataScope;
  scopeOrgUnitId: number | null;
}

/** Matches GET /api/v1/org-units/tree. */
export interface OrgUnitTreeNode {
  id: number;
  unitCode: string;
  unitName: string;
  unitType: OrgUnitType;
  parentId: number | null;
  treePath: string;
  level: number;
  status: OrgUnitStatus;
  description: string | null;
  managerId: number | null;
  children: readonly OrgUnitTreeNode[];
}

export interface PermissionDefinition {
  code: PermissionCode;
  name: string;
  description: string;
}

/**
 * The current backend has no effective-permissions endpoint. This intentionally
 * read-only shape leaves permissions unresolved instead of inventing a policy.
 */
export interface ReadOnlyEffectivePermissionFixture {
  source: "DEMO_READ_ONLY";
  availability: "NOT_EXPOSED_BY_API";
  resolvedForUserId: null;
  permissions: readonly PermissionDefinition[];
  note: string;
}
