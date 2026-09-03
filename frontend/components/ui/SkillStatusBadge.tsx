import { Icon } from "@/components/ui/Icon";

export type SkillApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";

interface SkillStatusBadgeProps {
  status: SkillApprovalStatus;
}

const statusConfigs: Record<SkillApprovalStatus, { label: string; tone: "warning" | "success" | "danger"; icon: "time" | "check" | "close" }> = {
  PENDING: { icon: "time", label: "Chờ xác nhận", tone: "warning" },
  APPROVED: { icon: "check", label: "Đã xác nhận", tone: "success" },
  REJECTED: { icon: "close", label: "Bị từ chối", tone: "danger" },
};

export function SkillStatusBadge({ status }: SkillStatusBadgeProps) {
  const config = statusConfigs[status] ?? statusConfigs.PENDING;

  const toneClassMap: Record<string, string> = {
    warning: "status-badge--warning",
    success: "status-badge--active",
    danger: "status-badge--locked",
  };

  return (
    <span className={`status-badge ${toneClassMap[config.tone] || ""}`}>
      <span aria-hidden="true" className="status-badge__dot" />
      <span>{config.label}</span>
    </span>
  );
}
