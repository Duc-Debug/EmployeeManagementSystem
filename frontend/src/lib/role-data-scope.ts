export type RoleDataScope = "COMPANY" | "ORGANIZATION_BRANCH" | "SELF";

const ROLE_DATA_SCOPE: Readonly<Record<string, RoleDataScope>> = {
  "VT-01": "COMPANY",
  "VT-02": "SELF",
  "VT-03": "ORGANIZATION_BRANCH",
  "VT-04": "SELF",
  "VT-05": "COMPANY",
  "VT-06": "COMPANY",
};

export function getDefaultDataScopeForRole(roleCode: string): RoleDataScope {
  if (!roleCode) return "SELF";
  const normalized = roleCode.toUpperCase().replace(/_/g, "-");
  return ROLE_DATA_SCOPE[normalized] || "SELF";
}
