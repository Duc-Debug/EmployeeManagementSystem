import { Icon } from "@/components/ui/Icon";
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

const scopeConfigs: Record<DataScope, { icon: "access" | "building" | "user"; label: string; tone: "indigo" | "blue" | "slate" }> = {
  COMPANY: { icon: "access", label: "Toàn công ty", tone: "indigo" },
  ORGANIZATION_BRANCH: { icon: "building", label: "Theo đơn vị", tone: "blue" },
  SELF: { icon: "user", label: "Cá nhân", tone: "slate" },
};

export function StatusBadge({ status }: StatusBadgeProps) {
  const isActive = status === "ACTIVE";
  return (
    <span className={`status-badge ${isActive ? "status-badge--active" : "status-badge--locked"}`}>
      <span aria-hidden="true" className="status-badge__dot" />
      {isActive ? "Hoạt động" : "Đã khóa"}
    </span>
  );
}

export function RoleBadge({ code, name }: RoleBadgeProps) {
  const isSystemAdmin = code === "VT-06";
  return (
    <span className={`role-badge ${isSystemAdmin ? "role-badge--admin" : ""}`}>
      <span className="role-badge__code">{code}</span>
      <span className="role-badge__name">{name}</span>
    </span>
  );
}

export function ScopeBadge({ scope }: ScopeBadgeProps) {
  const config = scopeConfigs[scope] ?? scopeConfigs.SELF;
  return (
    <span className={`scope-badge scope-badge--${config.tone}`}>
      <Icon name={config.icon} />
      <span>{config.label}</span>
    </span>
  );
}

export function getScopeLabel(scope: DataScope) {
  return scopeConfigs[scope]?.label ?? "Cá nhân";
}
