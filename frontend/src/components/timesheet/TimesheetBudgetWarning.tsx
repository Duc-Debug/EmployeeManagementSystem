import { AlertTriangle } from "lucide-react";
import type { WorkLogResult } from "@/lib/api/work-logs";

export function TimesheetBudgetWarning({ entry }: {
  entry: Pick<WorkLogResult, "hours" | "taskActualHours" | "taskPendingHours" | "taskBudgetHours">;
}) {
  const budget = Number(entry.taskBudgetHours);
  const pending = Number(entry.taskPendingHours ?? entry.hours);
  const worked = Number(entry.taskActualHours ?? 0) + pending;
  if (!Number.isFinite(budget) || budget <= 0 || worked < budget * 0.8) return null;

  return (
    <div className="mt-2 inline-flex items-start gap-1 rounded-lg border border-amber-300 bg-amber-50 px-2 py-1 text-xs font-semibold text-amber-800">
      <AlertTriangle className="mt-0.5 h-3.5 w-3.5 shrink-0" />
      <span>Vượt 80% ngân sách (Đã làm: {worked}h / Ngân sách: {budget}h)
        <span className="block font-normal">Bao gồm {pending}h đang chờ duyệt.</span>
      </span>
    </div>
  );
}
