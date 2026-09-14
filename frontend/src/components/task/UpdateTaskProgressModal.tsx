import React, { useState, useEffect } from "react";
import {
  X,
  CheckCircle2,
  Clock,
  PlayCircle,
  FileCheck2,
  AlertCircle,
  Loader2,
  Info,
  ShieldAlert,
} from "lucide-react";
import {
  SPECIALIST_TASK_STATUSES,
  PROGRESS_STATUS_METADATA,
  updateTaskProgress,
  type SpecialistTaskProgressStatus,
  type TaskProgressResult,
} from "@/lib/api/taskProgress";
import { ApiError } from "@/lib/api-client";
import { TaskProgressBadge } from "./TaskProgressBadge";
import { cn } from "@/lib/utils";

export interface TaskProgressModalTarget {
  taskId: number;
  taskCode: string;
  taskName: string;
  currentStatus: string;
  projectCode?: string;
  projectName?: string;
}

interface UpdateTaskProgressModalProps {
  isOpen: boolean;
  task: TaskProgressModalTarget | null;
  onClose: () => void;
  onSuccess: (result: TaskProgressResult) => void;
}

export const UpdateTaskProgressModal: React.FC<UpdateTaskProgressModalProps> = ({
  isOpen,
  task,
  onClose,
  onSuccess,
}) => {
  const [selectedStatus, setSelectedStatus] = useState<SpecialistTaskProgressStatus>("IN_PROGRESS");
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Sync selected status with initial task status
  useEffect(() => {
    if (task) {
      if (SPECIALIST_TASK_STATUSES.includes(task.currentStatus as SpecialistTaskProgressStatus)) {
        setSelectedStatus(task.currentStatus as SpecialistTaskProgressStatus);
      } else {
        setSelectedStatus("TODO");
      }
      setErrorMessage(null);
    }
  }, [task]);

  // Handle keyboard navigation: ESC to close, ArrowUp/ArrowDown to change selection
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!isOpen || isSubmitting) return;

      if (e.key === "Escape") {
        onClose();
        return;
      }

      if (e.key === "ArrowDown" || e.key === "ArrowUp") {
        e.preventDefault();
        const currentIndex = SPECIALIST_TASK_STATUSES.indexOf(selectedStatus);
        if (currentIndex === -1) return;

        if (e.key === "ArrowDown") {
          const nextIndex = (currentIndex + 1) % SPECIALIST_TASK_STATUSES.length;
          setSelectedStatus(SPECIALIST_TASK_STATUSES[nextIndex]);
        } else {
          const prevIndex = (currentIndex - 1 + SPECIALIST_TASK_STATUSES.length) % SPECIALIST_TASK_STATUSES.length;
          setSelectedStatus(SPECIALIST_TASK_STATUSES[prevIndex]);
        }
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, isSubmitting, onClose, selectedStatus]);

  if (!isOpen || !task) return null;

  const isCancelled = task.currentStatus === "CANCELLED";
  const isSameStatus = selectedStatus === task.currentStatus;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isCancelled || isSameStatus || isSubmitting) return;

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const result = await updateTaskProgress(task.taskId, selectedStatus);
      onSuccess(result);
      onClose();
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 403) {
          setErrorMessage("Bạn không được phân công thực hiện công việc này. Vui lòng liên hệ PM để kiểm tra.");
        } else if (err.status === 400) {
          setErrorMessage(
            err.message || "Dự án đã đóng hoặc kết thúc, không được phép cập nhật tiến độ công việc."
          );
        } else {
          setErrorMessage(
            err.message || "Không thể cập nhật tiến độ công việc. Vui lòng kiểm tra kết nối và thử lại."
          );
        }
      } else {
        const fallbackMsg = err instanceof Error ? err.message : "Đã xảy ra lỗi không xác định.";
        setErrorMessage(fallbackMsg);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const getStatusIcon = (status: SpecialistTaskProgressStatus) => {
    switch (status) {
      case "TODO":
        return <Clock className="w-4 h-4 text-slate-500" />;
      case "IN_PROGRESS":
        return <PlayCircle className="w-4 h-4 text-sky-600" />;
      case "IN_REVIEW":
        return <FileCheck2 className="w-4 h-4 text-amber-600" />;
      case "DONE":
        return <CheckCircle2 className="w-4 h-4 text-emerald-600" />;
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-200"
      aria-modal="true"
      role="dialog"
      aria-labelledby="task-progress-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isSubmitting) {
          onClose();
        }
      }}
    >
      <div
        className="relative w-full max-w-lg overflow-hidden bg-white rounded-2xl shadow-xl border border-slate-200 animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div>
            <h2 id="task-progress-modal-title" className="text-base font-semibold text-slate-800">Cập nhật tiến độ công việc</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              NCL-04-CN-002 • Dành cho chuyên viên được phân công
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}

            className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors disabled:opacity-50"
            title="Đóng (Esc)"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {/* Task Info Summary Card */}
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-2">
            <div className="flex items-center justify-between gap-2">
              <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-100">
                {task.taskCode}
              </span>
              <div className="flex items-center gap-1.5 text-xs text-slate-500">
                <span>Hiện tại:</span>
                <TaskProgressBadge status={task.currentStatus} size="xs" />
              </div>
            </div>

            <h3 className="text-sm font-semibold text-slate-900 leading-snug">
              {task.taskName}
            </h3>

            {(task.projectName || task.projectCode) && (
              <p className="text-xs text-slate-500 flex items-center gap-1">
                <span className="font-medium text-slate-600">Dự án:</span>
                {task.projectCode ? `[${task.projectCode}] ` : ""}
                {task.projectName}
              </p>
            )}
          </div>

          {/* Cancellation Notice */}
          {isCancelled ? (
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-rose-800 text-xs leading-relaxed">
              <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">Công việc đã bị hủy (CANCELLED)</p>
                <p className="mt-0.5 text-rose-700">
                  Công việc này đã bị hủy bỏ bởi Quản lý dự án. Chuyên viên không thể cập nhật tiến độ công việc đã hủy.
                </p>
              </div>
            </div>
          ) : (
            <>
              {/* Status Selection Cards */}
              <div className="space-y-2">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Chọn trạng thái tiến độ mới
                </label>

                <div className="grid grid-cols-1 gap-2.5">
                  {SPECIALIST_TASK_STATUSES.map((status) => {
                    const meta = PROGRESS_STATUS_METADATA[status];
                    const isSelected = selectedStatus === status;
                    const isCurrent = task.currentStatus === status;

                    return (
                      <button
                        key={status}
                        type="button"
                        onClick={() => setSelectedStatus(status)}
                        disabled={isSubmitting}
                        className={cn(
                          "relative flex items-start p-3 text-left rounded-xl border transition-all cursor-pointer",
                          isSelected
                            ? cn("ring-2 ring-indigo-500 border-indigo-500 bg-indigo-50/40")
                            : "border-slate-200 hover:border-slate-300 hover:bg-slate-50/50 bg-white"
                        )}
                      >
                        <div className="pt-0.5 pr-3">{getStatusIcon(status)}</div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2">
                            <span
                              className={cn(
                                "text-sm font-semibold",
                                isSelected ? "text-indigo-950" : "text-slate-800"
                              )}
                            >
                              {meta.label}
                            </span>
                            {isCurrent && (
                              <span className="text-[10px] bg-slate-200/80 text-slate-700 px-1.5 py-0.2 rounded font-medium">
                                Đang áp dụng
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-slate-500 mt-0.5 leading-relaxed">
                            {meta.description}
                          </p>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Specialist Permission Tip */}
              <div className="flex items-start gap-2 p-3 rounded-xl bg-amber-50/60 border border-amber-200/60 text-amber-800 text-[11px] leading-relaxed">
                <Info className="w-3.5 h-3.5 text-amber-600 shrink-0 mt-0.5" />
                <span>
                  <strong>Phạm vi quyền hạn:</strong> Chuyên viên chỉ cập nhật tiến độ các công việc được giao. Thao tác hủy công việc (CANCELLED) thuộc quyền hạn độc quyền của Quản lý dự án (PM).
                </span>
              </div>
            </>
          )}

          {/* Error Message Alert */}
          {errorMessage && (
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2 text-rose-800 text-xs animate-in fade-in duration-150">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="font-semibold">Lỗi cập nhật tiến độ</p>
                <p className="mt-0.5 text-rose-700 leading-relaxed">{errorMessage}</p>
              </div>
            </div>
          )}

          {/* Footer Buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition-colors disabled:opacity-50"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isCancelled || isSameStatus || isSubmitting}
              className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 rounded-xl transition-colors shadow-xs disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  Đang lưu...
                </>
              ) : isSameStatus ? (
                "Chưa thay đổi"
              ) : (
                "Lưu tiến độ mới"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UpdateTaskProgressModal;
