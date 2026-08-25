import type { DataScope, RoleCode, UserStatus } from "@/src/types/hrm";

interface StatusBadgeProps {
  status: UserStatus;
}

interface RoleBadgeProps {
  code: RoleCode;
  name: string;
}

interface ScopeBadgeProps {
  scope: DataScope;
}

const scopeLabels: Record<DataScope, string> = {
  COMPANY: "Toàn công ty",
  ORGANIZATION_BRANCH: "Theo đơn vị",
  SELF: "Cá nhân",
};

export function StatusBadge({ status }: StatusBadgeProps) {
  const isActive = status === "ACTIVE";
  return (
    <span className={isActive ? "status-badge status-badge--active" : "status-badge status-badge--locked"}>
      <span aria-hidden="true" className="status-badge__dot" />
      {isActive ? "Hoạt động" : "Đã khóa"}
    </span>
  );
}

export function RoleBadge({ code, name }: RoleBadgeProps) {
  return <span className="role-badge"><span>{code}</span>{name}</span>;
}

export function ScopeBadge({ scope }: ScopeBadgeProps) {
  return <span className="scope-badge">{scopeLabels[scope]}</span>;
}

export function getScopeLabel(scope: DataScope) {
  return scopeLabels[scope];
}
