import React, { useState, useEffect, useCallback, useMemo } from "react";
import {
  Search,
  RefreshCw,
  CheckCircle2,
  Clock,
  PlayCircle,
  FileCheck2,
  Layers,
  AlertCircle,
  ArrowUpDown,
  Filter,
  Calendar,
  X,
  Sparkles,
} from "lucide-react";
import {
  getMyAssignedTasks,
  type MyAssignedTaskItem,
  type TaskProgressResult,
  PROGRESS_STATUS_METADATA,
  type SpecialistTaskProgressStatus,
} from "@/lib/api/taskProgress";
import { TaskProgressBadge } from "./TaskProgressBadge";
import { UpdateTaskProgressModal, type TaskProgressModalTarget } from "./UpdateTaskProgressModal";
import { cn } from "@/lib/utils";

type StatusFilterOption = "ALL" | SpecialistTaskProgressStatus;

export const MyTaskProgressView: React.FC = () => {
  const [tasks, setTasks] = useState<MyAssignedTaskItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Filters & Search
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [selectedStatusTab, setSelectedStatusTab] = useState<StatusFilterOption>("ALL");

  // Modal target
  const [modalTarget, setModalTarget] = useState<TaskProgressModalTarget | null>(null);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);

  // Toast
  const [toast, setToast] = useState<{
    message: string;
    type: "success" | "error" | "info";
  } | null>(null);

  const showToast = useCallback(
    (message: string, type: "success" | "error" | "info" = "success") => {
      setToast({ message, type });
      const timer = setTimeout(() => setToast(null), 4000);
      return () => clearTimeout(timer);
    },
    []
  );

  const fetchTasks = useCallback(async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    else setIsRefreshing(true);
    setError(null);

    try {
      const data = await getMyAssignedTasks();
      setTasks(Array.isArray(data) ? data : []);
    } catch (err: unknown) {
      const errObj = err as { message?: string };
      setError(errObj?.message || "Không thể tải danh sách công việc. Vui lòng thử lại.");
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  // Handle task status update success from modal
  const handleUpdateSuccess = (result: TaskProgressResult) => {
    // Optimistically update the task in state
    setTasks((prev) =>
      prev.map((t) =>
        t.taskId === result.taskId ? { ...t, status: result.currentStatus } : t
      )
    );

    const statusLabel =
      PROGRESS_STATUS_METADATA[result.currentStatus]?.label || result.currentStatus;
    showToast(
      `Đã cập nhật tiến độ công việc [${result.taskCode}] thành "${statusLabel}"`,
      "success"
    );
  };

  // KPI Statistics
  const stats = useMemo(() => {
    const total = tasks.length;
    const todo = tasks.filter((t) => t.status === "TODO").length;
    const inProgress = tasks.filter((t) => t.status === "IN_PROGRESS").length;
    const inReview = tasks.filter((t) => t.status === "IN_REVIEW").length;
    const done = tasks.filter((t) => t.status === "DONE").length;
    return { total, todo, inProgress, inReview, done };
  }, [tasks]);

  // Filtered tasks
  const filteredTasks = useMemo(() => {
    return tasks.filter((task) => {
      // Status filter
      if (selectedStatusTab !== "ALL" && task.status !== selectedStatusTab) {
        return false;
      }

      // Search query filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchCode = task.taskCode?.toLowerCase().includes(q);
        const matchName = task.taskName?.toLowerCase().includes(q);
        const matchProject = task.projectName?.toLowerCase().includes(q);
        if (!matchCode && !matchName && !matchProject) {
          return false;
        }
      }

      return true;
    });
  }, [tasks, selectedStatusTab, searchQuery]);

  const openUpdateModal = (task: MyAssignedTaskItem) => {
    setModalTarget({
      taskId: task.taskId,
      taskCode: task.taskCode,
      taskName: task.taskName,
      currentStatus: task.status,
      projectName: task.projectName,
    });
    setIsModalOpen(true);
  };

  return (
    <div className="flex flex-col h-full space-y-6">
      {/* Toast Notification */}
      {toast && (
        <div className="fixed top-6 right-6 z-50 animate-in slide-in-from-top-3 fade-in duration-200">
          <div
            className={cn(
              "flex items-center gap-2.5 px-4 py-3 rounded-2xl shadow-lg border text-xs font-medium backdrop-blur-md",
              toast.type === "success" && "bg-emerald-50/95 border-emerald-200 text-emerald-800",
              toast.type === "error" && "bg-rose-50/95 border-rose-200 text-rose-800",
              toast.type === "info" && "bg-indigo-50/95 border-indigo-200 text-indigo-800"
            )}
          >
            {toast.type === "success" && <CheckCircle2 className="w-4 h-4 text-emerald-600" />}
            {toast.type === "error" && <AlertCircle className="w-4 h-4 text-rose-600" />}
            <span>{toast.message}</span>
            <button
              onClick={() => setToast(null)}
              className="p-1 ml-2 text-slate-400 hover:text-slate-600 rounded-md"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}

      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200/80">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-slate-900 tracking-tight">
              Công việc của tôi & Cập nhật tiến độ
            </h1>
            <span className="text-xs bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full font-semibold border border-indigo-200">
              NCL-04-CN-002
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Theo dõi danh sách các hạng mục công việc được phân công và cập nhật tiến độ thực hiện
          </p>
        </div>

        <button
          type="button"
          onClick={() => fetchTasks(true)}
          disabled={isLoading || isRefreshing}
          className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-200 hover:bg-slate-50 hover:border-slate-300 rounded-xl transition shadow-2xs self-start sm:self-auto disabled:opacity-50"
        >
          <RefreshCw className={cn("w-3.5 h-3.5", isRefreshing && "animate-spin text-indigo-600")} />
          <span>Làm mới</span>
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3.5">
        <div
          onClick={() => setSelectedStatusTab("ALL")}
          className={cn(
            "p-4 rounded-2xl border transition cursor-pointer shadow-2xs",
            selectedStatusTab === "ALL"
              ? "bg-indigo-50/70 border-indigo-300 ring-2 ring-indigo-500/20"
              : "bg-white border-slate-200/80 hover:border-slate-300"
          )}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">Tất cả</span>
            <Layers className="w-4 h-4 text-indigo-600" />
          </div>
          <div className="text-2xl font-bold text-slate-900 mt-2">{stats.total}</div>
          <div className="text-[11px] text-slate-400 mt-0.5">Tổng công việc</div>
        </div>

        <div
          onClick={() => setSelectedStatusTab("TODO")}
          className={cn(
            "p-4 rounded-2xl border transition cursor-pointer shadow-2xs",
            selectedStatusTab === "TODO"
              ? "bg-slate-100 border-slate-400 ring-2 ring-slate-400/20"
              : "bg-white border-slate-200/80 hover:border-slate-300"
          )}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-600">Chờ thực hiện</span>
            <Clock className="w-4 h-4 text-slate-500" />
          </div>
          <div className="text-2xl font-bold text-slate-800 mt-2">{stats.todo}</div>
          <div className="text-[11px] text-slate-400 mt-0.5">Chưa bắt đầu</div>
        </div>

        <div
          onClick={() => setSelectedStatusTab("IN_PROGRESS")}
          className={cn(
            "p-4 rounded-2xl border transition cursor-pointer shadow-2xs",
            selectedStatusTab === "IN_PROGRESS"
              ? "bg-sky-50 border-sky-300 ring-2 ring-sky-500/20"
              : "bg-white border-slate-200/80 hover:border-slate-300"
          )}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-sky-700">Đang thực hiện</span>
            <PlayCircle className="w-4 h-4 text-sky-600" />
          </div>
          <div className="text-2xl font-bold text-sky-900 mt-2">{stats.inProgress}</div>
          <div className="text-[11px] text-sky-600/70 mt-0.5">Đang làm</div>
        </div>

        <div
          onClick={() => setSelectedStatusTab("IN_REVIEW")}
          className={cn(
            "p-4 rounded-2xl border transition cursor-pointer shadow-2xs",
            selectedStatusTab === "IN_REVIEW"
              ? "bg-amber-50 border-amber-300 ring-2 ring-amber-500/20"
              : "bg-white border-slate-200/80 hover:border-slate-300"
          )}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-amber-700">Chờ duyệt</span>
            <FileCheck2 className="w-4 h-4 text-amber-600" />
          </div>
          <div className="text-2xl font-bold text-amber-900 mt-2">{stats.inReview}</div>
          <div className="text-[11px] text-amber-600/70 mt-0.5">Nghiệm thu</div>
        </div>

        <div
          onClick={() => setSelectedStatusTab("DONE")}
          className={cn(
            "p-4 rounded-2xl border transition cursor-pointer shadow-2xs",
            selectedStatusTab === "DONE"
              ? "bg-emerald-50 border-emerald-300 ring-2 ring-emerald-500/20"
              : "bg-white border-slate-200/80 hover:border-slate-300"
          )}
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-emerald-700">Hoàn thành</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-bold text-emerald-900 mt-2">{stats.done}</div>
          <div className="text-[11px] text-emerald-600/70 mt-0.5">Đã nghiệm thu</div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white p-3 rounded-2xl border border-slate-200/80 shadow-2xs">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo mã việc, tên công việc hoặc tên dự án..."
            className="w-full pl-9 pr-8 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery("")}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {/* Tab Buttons */}
        <div className="flex items-center gap-1 overflow-x-auto pb-1 sm:pb-0">
          <span className="text-xs text-slate-400 font-medium px-2 shrink-0 flex items-center gap-1">
            <Filter className="w-3.5 h-3.5" /> Lọc:
          </span>
          {(
            [
              { key: "ALL", label: "Tất cả" },
              { key: "TODO", label: "Chờ làm" },
              { key: "IN_PROGRESS", label: "Đang làm" },
              { key: "IN_REVIEW", label: "Chờ duyệt" },
              { key: "DONE", label: "Hoàn thành" },
            ] as const
          ).map((tab) => (
            <button
              key={tab.key}
              onClick={() => setSelectedStatusTab(tab.key)}
              className={cn(
                "px-3 py-1.5 text-xs font-semibold rounded-xl whitespace-nowrap transition",
                selectedStatusTab === tab.key
                  ? "bg-slate-900 text-white shadow-2xs"
                  : "text-slate-600 hover:text-slate-900 hover:bg-slate-100"
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Main Table Content */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden flex flex-col flex-1">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20 text-slate-400 space-y-3">
            <RefreshCw className="w-8 h-8 animate-spin text-indigo-600" />
            <p className="text-xs font-medium">Đang tải danh sách công việc...</p>
          </div>
        ) : error ? (
          <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
            <AlertCircle className="w-10 h-10 text-rose-500 mb-3" />
            <h3 className="text-sm font-semibold text-slate-900">Không thể tải dữ liệu</h3>
            <p className="text-xs text-slate-500 max-w-sm mt-1 mb-4">{error}</p>
            <button
              onClick={() => fetchTasks()}
              className="px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition"
            >
              Thử lại
            </button>
          </div>
        ) : filteredTasks.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 px-4 text-center">
            <div className="w-12 h-12 rounded-2xl bg-slate-50 flex items-center justify-center text-slate-400 mb-3 border border-slate-100">
              <Sparkles className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-semibold text-slate-800">Không tìm thấy công việc nào</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-xs">
              {searchQuery || selectedStatusTab !== "ALL"
                ? "Không có công việc nào khớp với bộ lọc hiện tại. Thử xóa bộ lọc để xem toàn bộ."
                : "Bạn hiện chưa có công việc nào được phân công."}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto flex-1">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/60 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                  <th className="py-3 px-4 w-32">Mã công việc</th>
                  <th className="py-3 px-4 min-w-[200px]">Tên công việc</th>
                  <th className="py-3 px-4 min-w-[160px]">Dự án</th>
                  <th className="py-3 px-4 w-44">Kế hoạch</th>
                  <th className="py-3 px-4 w-36">Trạng thái</th>
                  <th className="py-3 px-4 w-36 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs">
                {filteredTasks.map((task) => (
                  <tr
                    key={task.taskId}
                    className="hover:bg-slate-50/70 transition-colors group"
                  >
                    <td className="py-3.5 px-4 font-mono font-semibold text-indigo-600">
                      {task.taskCode}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-slate-900 leading-snug">
                        {task.taskName}
                      </div>
                      {task.isPrimary && (
                        <span className="inline-block mt-1 text-[10px] font-semibold text-indigo-600 bg-indigo-50 border border-indigo-100 px-1.5 py-0.2 rounded">
                          Phụ trách chính
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-slate-600">
                      <span className="font-medium text-slate-800">{task.projectName}</span>
                    </td>
                    <td className="py-3.5 px-4 text-slate-500 text-[11px]">
                      {task.plannedStartDate || task.plannedEndDate ? (
                        <div className="flex items-center gap-1 text-slate-600">
                          <Calendar className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                          <span>
                            {task.plannedStartDate || "--"} → {task.plannedEndDate || "--"}
                          </span>
                        </div>
                      ) : (
                        <span className="text-slate-400 italic">Chưa đặt hạn</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4">
                      <TaskProgressBadge status={task.status} />
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        type="button"
                        onClick={() => openUpdateModal(task)}
                        disabled={task.status === "CANCELLED"}
                        className={cn(
                          "inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-xl transition shadow-2xs",
                          task.status === "CANCELLED"
                            ? "bg-slate-100 text-slate-400 cursor-not-allowed"
                            : "bg-indigo-50 text-indigo-700 hover:bg-indigo-600 hover:text-white border border-indigo-200/80 hover:border-indigo-600"
                        )}
                      >
                        <ArrowUpDown className="w-3 h-3" />
                        <span>Cập nhật</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal cập nhật tiến độ */}
      <UpdateTaskProgressModal
        isOpen={isModalOpen}
        task={modalTarget}
        onClose={() => {
          setIsModalOpen(false);
          setModalTarget(null);
        }}
        onSuccess={handleUpdateSuccess}
      />
    </div>
  );
};

export default MyTaskProgressView;
