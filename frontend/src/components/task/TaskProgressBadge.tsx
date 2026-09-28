import React from "react";
import {
  Clock,
  PlayCircle,
  FileCheck2,
  CheckCircle2,
  Ban,
  HelpCircle,
} from "lucide-react";
import {
  isValidSpecialistStatus,
  PROGRESS_STATUS_METADATA,
  type SpecialistTaskProgressStatus,
} from "@/lib/api/taskProgress";
import { cn } from "@/lib/utils";

interface TaskProgressBadgeProps {
  status: string;
  size?: "xs" | "sm" | "md";
  showIcon?: boolean;
  className?: string;
}

export const TaskProgressBadge: React.FC<TaskProgressBadgeProps> = ({
  status,
  size = "sm",
  showIcon = true,
  className,
}) => {
  const isSpecialist = isValidSpecialistStatus(status);
  const meta = isSpecialist ? PROGRESS_STATUS_METADATA[status as SpecialistTaskProgressStatus] : null;

  const sizeStyles = {
    xs: "px-1.5 py-0.5 text-[10px] gap-1",
    sm: "px-2.5 py-1 text-xs gap-1.5",
    md: "px-3 py-1.5 text-sm gap-2",
  };

  const iconSizes = {
    xs: "w-3 h-3",
    sm: "w-3.5 h-3.5",
    md: "w-4 h-4",
  };

  const renderIcon = () => {
    if (!showIcon) return null;
    const iconClass = iconSizes[size];

    switch (status) {
      case "TODO":
        return <Clock className={iconClass} />;
      case "IN_PROGRESS":
        return <PlayCircle className={iconClass} />;
      case "IN_REVIEW":
        return <FileCheck2 className={iconClass} />;
      case "DONE":
        return <CheckCircle2 className={iconClass} />;
      case "CANCELLED":
        return <Ban className={iconClass} />;
      default:
        return <HelpCircle className={iconClass} />;
    }
  };

  if (!meta) {
    if (status === "CANCELLED") {
      return (
        <span
          className={cn(
            "inline-flex items-center font-medium rounded-full border bg-rose-50 text-rose-700 border-rose-200",
            sizeStyles[size],
            className
          )}
        >
          {renderIcon()}
          <span>Đã hủy</span>
        </span>
      );
    }

    return (
      <span
        className={cn(
          "inline-flex items-center font-medium rounded-full border bg-slate-100 text-slate-600 border-slate-200",
          sizeStyles[size],
          className
        )}
      >
        {renderIcon()}
        <span>{status || "Không xác định"}</span>
      </span>
    );
  }

  return (
    <span
      className={cn(
        "inline-flex items-center font-medium rounded-full border transition-colors",
        meta.badgeClass,
        sizeStyles[size],
        className
      )}
      title={meta.description}
    >
      {renderIcon()}
      <span>{meta.label}</span>
    </span>
  );
};

export default TaskProgressBadge;
