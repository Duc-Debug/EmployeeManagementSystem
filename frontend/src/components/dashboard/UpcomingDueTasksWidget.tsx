import React, { useState } from "react";
import {
  Clock,
  AlertCircle,
  Calendar,
  RefreshCw,
  CheckCircle2,
  ExternalLink,
  Briefcase,
  Play,
} from "lucide-react";
import { useUpcomingDueTasks } from "@/hooks/useUpcomingDueTasks";
import { formatDaysRemaining, type UpcomingDueTaskResult } from "@/lib/api/task-due-reminders";

interface UpcomingDueTasksWidgetProps {
  onNavigate?: (tabId: string) => void;
  onSelectTask?: (taskId: number, projectId: number) => void;
}

export const UpcomingDueTasksWidget: React.FC<UpcomingDueTasksWidgetProps> = ({
  onNavigate,
  onSelectTask,
}) => {
  const {
    tasks,
    loading,
    refreshing,
    error,
    criticalCount,
    reload,
    triggerManualScan,
  } = useUpcomingDueTasks();

  const [scanning, setScanning] = useState<boolean>(false);
  const [scanMessage, setScanMessage] = useState<string | null>(null);

  const handleManualScan = async () => {
    setScanning(true);
    setScanMessage(null);
    try {
      const result = await triggerManualScan();
      setScanMessage(
        `Rà soát thành công: Đã quét ${result.totalScanned} việc, gửi ${result.sentCount} thông báo (Bỏ qua trùng QTN-19: ${result.skippedDuplicateCount})`
      );
      setTimeout(() => setScanMessage(null), 5000);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Rà soát thất bại";
      setScanMessage(`Lỗi: ${msg}`);
      setTimeout(() => setScanMessage(null), 5000);
    } finally {
      setScanning(false);
    }
  };

  const handleOpenTask = (task: UpcomingDueTaskResult) => {
    if (onSelectTask) {
      onSelectTask(task.taskId, task.projectId);
    } else if (onNavigate) {
      onNavigate("project");
    } else {
      window.location.href = task.directUrl || `/projects/${task.projectId}/tasks/${task.taskId}`;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "IN_PROGRESS":
        return (
          <span className="inline-flex items-center rounded-full bg-blue-50 border border-blue-200 px-2 py-0.5 text-[10px] font-semibold text-blue-700">
            Đang làm
          </span>
        );
      case "TODO":
        return (
          <span className="inline-flex items-center rounded-full bg-slate-100 border border-slate-200 px-2 py-0.5 text-[10px] font-medium text-slate-600">
            Chưa làm
          </span>
        );
      case "IN_REVIEW":
        return (
          <span className="inline-flex items-center rounded-full bg-purple-50 border border-purple-200 px-2 py-0.5 text-[10px] font-semibold text-purple-700">
            Đang đánh giá
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-600">
            {status}
          </span>
        );
    }
  };

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
      {/* Header Widget */}
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-2.5 mb-3">
        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-rose-50 border border-rose-100 text-rose-600">
            <Clock className="h-4 w-4" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-slate-900">
                Công việc sắp đến hạn (3 ngày tới)
              </h3>
              {tasks.length > 0 && (
                <span className="rounded-full bg-rose-100 px-2 py-0.5 text-[11px] font-bold text-rose-700">
                  {tasks.length}
                </span>
              )}
              {criticalCount > 0 && (
                <span className="inline-flex items-center gap-0.5 rounded-full bg-rose-600 px-2 py-0.5 text-[10px] font-bold text-white shadow-2xs">
                  <AlertCircle className="h-2.5 w-2.5" /> {criticalCount} việc khẩn
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-500">
              Rà soát tự động hằng ngày và thông báo trước 3 ngày theo quy tắc chống gửi trùng QTN-19
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5">
          {/* Nút rà soát thủ công (Manual Scan) */}
          <button
            type="button"
            onClick={handleManualScan}
            disabled={scanning || loading}
            className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 text-[11px] font-semibold text-slate-700 hover:bg-slate-100 transition cursor-pointer disabled:opacity-50"
            title="Kích hoạt rà soát toàn hệ thống ngay bây giờ (kiểm tra chống gửi trùng QTN-19)"
          >
            <Play className={`h-3 w-3 ${scanning ? "animate-pulse text-amber-600" : "text-slate-600"}`} />
            <span>{scanning ? "Đang rà soát..." : "Rà soát ngay"}</span>
          </button>

          {/* Nút làm mới */}
          <button
            type="button"
            onClick={() => reload(false)}
            disabled={loading || refreshing}
            className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-semibold text-slate-700 hover:bg-slate-50 transition cursor-pointer disabled:opacity-50"
            title="Tải lại danh sách"
          >
            <RefreshCw className={`h-3 w-3 ${refreshing || loading ? "animate-spin text-sky-600" : "text-slate-500"}`} />
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {/* Thông báo kết quả rà soát thủ công */}
      {scanMessage && (
        <div className="mb-3 rounded-lg bg-sky-50 border border-sky-200 p-2 text-xs text-sky-800 flex items-center gap-1.5 animate-in fade-in duration-200">
          <CheckCircle2 className="h-3.5 w-3.5 text-sky-600 flex-shrink-0" />
          <span>{scanMessage}</span>
        </div>
      )}

      {/* Error state */}
      {error && (
        <div className="rounded-lg bg-rose-50 border border-rose-200 p-3 text-xs text-rose-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4 text-rose-600 flex-shrink-0" />
            <span>{error}</span>
          </div>
          <button
            onClick={() => reload(false)}
            type="button"
            className="font-bold underline hover:text-rose-900 ml-2"
          >
            Thử lại
          </button>
        </div>
      )}

      {/* Loading skeleton */}
      {loading && (
        <div className="space-y-2 py-2">
          {[1, 2].map((i) => (
            <div
              key={i}
              className="animate-pulse flex items-center justify-between p-3 rounded-lg border border-slate-100 bg-slate-50/50"
            >
              <div className="space-y-1.5 flex-1">
                <div className="h-3.5 bg-slate-200 rounded w-1/3" />
                <div className="h-3 bg-slate-100 rounded w-1/2" />
              </div>
              <div className="h-6 bg-slate-200 rounded-full w-20" />
            </div>
          ))}
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && tasks.length === 0 && (
        <div className="py-7 text-center border border-dashed border-slate-200 rounded-lg">
          <div className="mx-auto mb-2 flex h-9 w-9 items-center justify-center rounded-full bg-emerald-50 text-emerald-600 border border-emerald-100">
            <CheckCircle2 className="h-5 w-5" />
          </div>
          <p className="text-xs font-bold text-slate-800">
            Tuyệt vời! Không có công việc nào sắp đến hạn
          </p>
          <p className="text-[11px] text-slate-500 mt-0.5 max-w-sm mx-auto">
            Tất cả công việc được giao của bạn đều nằm ngoài mốc 3 ngày tới hoặc đã hoàn thành.
          </p>
        </div>
      )}

      {/* Danh sách công việc sắp đến hạn */}
      {!loading && !error && tasks.length > 0 && (
        <div className="space-y-2">
          {tasks.map((task) => {
            const urgency = formatDaysRemaining(task.daysRemaining);
            return (
              <div
                key={task.taskId}
                onClick={() => handleOpenTask(task)}
                className="group flex flex-wrap items-center justify-between gap-2 p-2.5 rounded-lg border border-slate-100 hover:border-rose-200 hover:bg-slate-50/70 transition cursor-pointer"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-mono text-slate-700 font-bold border border-slate-200">
                      {task.taskCode}
                    </span>
                    <span className="text-xs font-bold text-slate-900 group-hover:text-indigo-600 transition truncate">
                      {task.taskName}
                    </span>
                    {getStatusBadge(task.status)}
                  </div>

                  <div className="flex items-center gap-3 mt-1 text-[11px] text-slate-500">
                    <span className="inline-flex items-center gap-1">
                      <Briefcase className="h-3 w-3 text-slate-400" />
                      Dự án #{task.projectId}
                    </span>
                    <span>·</span>
                    <span className="inline-flex items-center gap-1">
                      <Calendar className="h-3 w-3 text-slate-400" />
                      Hạn chót: <strong className="text-slate-700">{task.dueDate}</strong>
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {/* Badge số ngày còn lại */}
                  <span
                    className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-bold border shadow-2xs ${urgency.badgeClass}`}
                  >
                    {urgency.iconType === "ALERT" ? (
                      <AlertCircle className="h-3 w-3 text-rose-600" />
                    ) : urgency.iconType === "CLOCK" ? (
                      <Clock className="h-3 w-3 text-amber-600" />
                    ) : (
                      <Calendar className="h-3 w-3 text-sky-600" />
                    )}
                    {urgency.text}
                  </span>

                  {/* Nút mở công việc */}
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleOpenTask(task);
                    }}
                    className="inline-flex items-center gap-0.5 rounded-lg border border-slate-200 bg-white px-2 py-1 text-[11px] font-semibold text-slate-700 hover:text-indigo-600 hover:border-indigo-300 transition shadow-2xs"
                    title="Mở công việc này"
                  >
                    <span>Mở việc</span>
                    <ExternalLink className="h-2.5 w-2.5" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};