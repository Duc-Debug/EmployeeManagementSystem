"use client";

import { useState } from "react";
import { FileSpreadsheet } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthUser } from "@/lib/auth-session";
import { canExportProjectAllocationExcel } from "@/lib/api/project-allocation-excel";
import { ProjectAllocationExcelExportModal } from "./ProjectAllocationExcelExportModal";

export interface ProjectAllocationExcelExportButtonProps {
  projectId: number;
  projectCode?: string;
  projectName?: string;
  initialFromYear?: number;
  initialFromWeek?: number;
  initialToYear?: number;
  initialToWeek?: number;
  className?: string;
  buttonText?: string;
  variant?: "primary" | "secondary" | "outline" | "emerald";
  onSuccess?: (filename: string) => void;
  hideIfNoPermission?: boolean;
}

export function ProjectAllocationExcelExportButton({
  projectId,
  projectCode,
  projectName,
  initialFromYear,
  initialFromWeek,
  initialToYear,
  initialToWeek,
  className,
  buttonText = "Xuất báo cáo Excel",
  variant = "emerald",
  onSuccess,
  hideIfNoPermission = true,
}: ProjectAllocationExcelExportButtonProps) {
  const [isOpen, setIsOpen] = useState(false);
  const user = useAuthUser();

  const hasPermission = canExportProjectAllocationExcel(
    user?.roleCode,
    user?.permissions
  );

  if (!hasPermission && hideIfNoPermission) {
    return null;
  }

  const variantStyles = {
    emerald:
      "bg-emerald-600 hover:bg-emerald-700 text-white border-transparent shadow-xs active:scale-[0.98]",
    primary:
      "bg-indigo-600 hover:bg-indigo-700 text-white border-transparent shadow-xs active:scale-[0.98]",
    secondary:
      "bg-slate-100 hover:bg-slate-200 text-slate-800 border-slate-200 active:scale-[0.98]",
    outline:
      "bg-white hover:bg-slate-50 text-slate-700 border-slate-200 shadow-xs active:scale-[0.98]",
  };

  return (
    <>
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        disabled={!hasPermission}
        title={
          hasPermission
            ? "Xuất báo cáo phân bổ dự án ra file Excel (.xlsx)"
            : "Bạn không có quyền xuất báo cáo phân bổ dự án này"
        }
        className={cn(
          "inline-flex items-center gap-2 rounded-xl border px-3.5 py-2 text-xs font-semibold transition disabled:cursor-not-allowed disabled:opacity-50",
          variantStyles[variant],
          className
        )}
      >
        <FileSpreadsheet className="h-4 w-4 shrink-0" />
        <span>{buttonText}</span>
      </button>

      <ProjectAllocationExcelExportModal
        open={isOpen}
        onClose={() => setIsOpen(false)}
        projectId={projectId}
        projectCode={projectCode}
        projectName={projectName}
        initialFromYear={initialFromYear}
        initialFromWeek={initialFromWeek}
        initialToYear={initialToYear}
        initialToWeek={initialToWeek}
        onSuccess={onSuccess}
      />
    </>
  );
}

export default ProjectAllocationExcelExportButton;
