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
] as const;

export type RoleCode = (typeof ROLE_CODES)[number];

export type UserStatus = "ACTIVE" | "LOCKED";

export type OrgUnitType = "COMPANY" | "CENTER" | "DEPARTMENT" | "TEAM";

export type OrgUnitStatus = "ACTIVE" | "INACTIVE";

export interface Role {
  code: RoleCode;
  name: string;
  description?: string;
  goal?: string;
  permissions?: string;
  scopeData?: string;
  limitations?: string;
}

/** Matches the backend UserResult payload. */
export interface User {
  id: number;
  username: string;
  email?: string;
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
