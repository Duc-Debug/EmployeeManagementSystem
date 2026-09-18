import React, { useState } from "react";
import {
  X,
  Clock,
  Calendar,
  Briefcase,
  ExternalLink,
  Copy,
  Check,
  ShieldAlert,
} from "lucide-react";
import {
  formatDaysRemaining,
  formatDueDateVietnamese,
  type UpcomingDueTaskResult,
} from "@/lib/api/task-due-reminders";

interface TaskDueDetailModalProps {
  task: UpcomingDueTaskResult | null;
  isOpen: boolean;
  onClose: () => void;
  onNavigateProject?: (projectId: number, taskId: number) => void;
}

export const TaskDueDetailModal: React.FC<TaskDueDetailModalProps> = ({
  task,
  isOpen,
  onClose,
  onNavigateProject,
}) => {
  const [copied, setCopied] = useState<boolean>(false);

  if (!isOpen || !task) return null;

  const urgency = formatDaysRemaining(task.daysRemaining);
  const formattedDueDate = formatDueDateVietnamese(task.dueDate);
  const directLink = task.directUrl || `/projects/${task.projectId}/tasks/${task.taskId}`;

  const handleCopyLink = () => {
    navigator.clipboard.writeText(window.location.origin + directLink);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const handleGoToProject = () => {
    onClose();
    if (onNavigateProject) {
      onNavigateProject(task.projectId, task.taskId);
    } else {
      window.location.assign(directLink);
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case "IN_PROGRESS":
        return { text: "Đang làm", className: "bg-blue-50 text-blue-700 border-blue-200" };
      case "TODO":
        return { text: "Chưa làm", className: "bg-slate-100 text-slate-700 border-slate-200" };
      case "IN_REVIEW":
        return { text: "Đang đánh giá", className: "bg-purple-50 text-purple-700 border-purple-200" };
      case "DONE":
        return { text: "Hoàn thành", className: "bg-emerald-50 text-emerald-700 border-emerald-200" };
      default:
        return { text: status, className: "bg-slate-100 text-slate-700 border-slate-200" };
    }
  };

  const statusBadge = getStatusLabel(task.status);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="relative w-full max-w-lg rounded-2xl bg-white p-5 shadow-2xl border border-slate-200 animate-in zoom-in-95 duration-150 text-slate-800">
        {/* Header Modal */}
        <div className="flex items-start justify-between border-b border-slate-100 pb-3 mb-4">
          <div className="flex items-center gap-2">
            <span className="rounded-md bg-slate-100 px-2 py-0.5 font-mono text-xs font-bold text-slate-800 border border-slate-200">
              {task.taskCode}
            </span>
            <span
              className={`rounded-full border px-2.5 py-0.5 text-[11px] font-bold ${statusBadge.className}`}
            >
              {statusBadge.text}
            </span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
            title="Đóng"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Tên công việc & Dự án */}
        <div className="space-y-3">
          <div>
            <h3 className="text-base font-bold text-slate-900 leading-snug">
              {task.taskName}
            </h3>
            <div className="flex items-center gap-2 mt-1 text-xs text-slate-500">
              <span className="inline-flex items-center gap-1 font-medium text-indigo-600">
                <Briefcase className="h-3.5 w-3.5" />
                Dự án #{task.projectId}
              </span>
              <span>·</span>
              <span>Mã ID: #{task.taskId}</span>
            </div>
          </div>

          {/* Banner mức độ khẩn cấp */}
          <div
            className={`flex items-center gap-3 rounded-xl border p-3.5 ${urgency.badgeClass}`}
          >
            <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg bg-white/80 shadow-2xs">
              {urgency.level === "TODAY" || urgency.level === "CRITICAL" ? (
                <ShieldAlert className="h-5 w-5 text-rose-600 animate-bounce" />
              ) : urgency.level === "WARNING" ? (
                <Clock className="h-5 w-5 text-amber-600" />
              ) : (
                <Calendar className="h-5 w-5 text-sky-600" />
              )}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-bold leading-tight">
                Thời hạn hoàn thành: {urgency.text}
              </p>
              <p className="text-[11px] opacity-90 mt-0.5">
                {urgency.level === "TODAY"
                  ? "Công việc phải hoàn thành trong hôm nay. Vui lòng ưu tiên xử lý ngay!"
                  : urgency.level === "CRITICAL"
                  ? "Công việc chỉ còn 1 ngày là đến hạn. Cần đẩy nhanh tiến độ!"
                  : `Hạn chót vào ngày ${formattedDueDate} (còn ${task.daysRemaining} ngày).`}
              </p>
            </div>
          </div>

          {/* Thông số chi tiết */}
          <div className="grid grid-cols-2 gap-2 text-xs rounded-xl bg-slate-50 border border-slate-100 p-3">
            <div>
              <span className="text-slate-400 block text-[11px]">Hạn chót (Deadline)</span>
              <span className="font-bold text-slate-800 text-xs flex items-center gap-1 mt-0.5">
                <Calendar className="h-3.5 w-3.5 text-slate-500" />
                {formattedDueDate}
              </span>
            </div>
            <div>
              <span className="text-slate-400 block text-[11px]">Số ngày còn lại</span>
              <span className="font-bold text-slate-800 text-xs flex items-center gap-1 mt-0.5">
                <Clock className="h-3.5 w-3.5 text-slate-500" />
                {task.daysRemaining <= 0 ? "0 ngày (Hôm nay)" : `${task.daysRemaining} ngày`}
              </span>
            </div>
          </div>

          {/* Đường dẫn trực tiếp & Nút sao chép link */}
          <div>
            <span className="text-[11px] font-medium text-slate-500 block mb-1">
              Đường dẫn mở trực tiếp công việc:
            </span>
            <div className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-xs text-slate-700">
              <span className="font-mono text-[11px] flex-1 truncate text-slate-600">
                {directLink}
              </span>
              <button
                type="button"
                onClick={handleCopyLink}
                className="inline-flex items-center gap-1 rounded bg-white px-2 py-0.5 text-[10px] font-semibold text-slate-700 border border-slate-200 hover:bg-slate-100 transition shadow-2xs flex-shrink-0"
                title="Sao chép đường dẫn"
              >
                {copied ? (
                  <>
                    <Check className="h-3 w-3 text-emerald-600" />
                    <span className="text-emerald-700">Đã chép</span>
                  </>
                ) : (
                  <>
                    <Copy className="h-3 w-3" />
                    <span>Sao chép</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Footer actions */}
        <div className="mt-5 flex items-center justify-end gap-2 border-t border-slate-100 pt-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition cursor-pointer"
          >
            Đóng
          </button>
          <button
            type="button"
            onClick={handleGoToProject}
            className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs cursor-pointer"
          >
            <span>Đến trang Dự án</span>
            <ExternalLink className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
};