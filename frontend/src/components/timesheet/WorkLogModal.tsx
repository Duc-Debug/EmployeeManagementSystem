import { useState, useEffect } from "react";
import type React from "react";
import {
  X,
  Clock,
  Briefcase,
  Calendar,
  FileText,
  DollarSign,
  AlertCircle,
  Loader2,
  CheckCircle2,
} from "lucide-react";
import {
  createWorkLog,
  updateWorkLog,
  getMyAssignedTasks,
  type AssignedTaskOptionResult,
  type WorkLogResult,
} from "@/lib/api/work-logs";

interface WorkLogModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  initialData?: WorkLogResult | null;
  defaultDate?: string;
}

export default function WorkLogModal({
  isOpen,
  onClose,
  onSuccess,
  initialData,
  defaultDate,
}: WorkLogModalProps) {
  const [tasks, setTasks] = useState<AssignedTaskOptionResult[]>([]);
  const [isLoadingTasks, setIsLoadingTasks] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [selectedTaskKey, setSelectedTaskKey] = useState<string>("");
  const [workDate, setWorkDate] = useState<string>("");
  const [hours, setHours] = useState<number>(4.0);
  const [isBillable, setIsBillable] = useState<boolean>(true);
  const [description, setDescription] = useState<string>("");

  useEffect(() => {
    if (!isOpen) return;

    setErrorMessage(null);
    let isMounted = true;
    async function loadTasks() {
      setIsLoadingTasks(true);
      try {
        const assigned = await getMyAssignedTasks();
        if (isMounted) {
          setTasks(assigned || []);
        }
      } catch (err: unknown) {
        if (isMounted) {
          console.error("Lỗi khi tải danh sách công việc:", err);
          setErrorMessage(
            err instanceof Error
              ? err.message
              : "Không thể tải danh sách công việc được phân công."
          );
        }
      } finally {
        if (isMounted) setIsLoadingTasks(false);
      }
    }

    loadTasks();

    if (initialData) {
      setSelectedTaskKey(`${initialData.projectId}_${initialData.taskId}`);
      setWorkDate(initialData.workDate);
      setHours(initialData.hours);
      setIsBillable(initialData.isBillable);
      setDescription(initialData.description || "");
    } else {
      const today = defaultDate || new Date().toISOString().split("T")[0];
      setWorkDate(today);
      setHours(4.0);
      setIsBillable(true);
      setDescription("");
      setSelectedTaskKey("");
    }

    return () => {
      isMounted = false;
    };
  }, [isOpen, initialData, defaultDate]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!selectedTaskKey) {
      setErrorMessage("Vui lòng chọn dự án và công việc cần ghi giờ.");
      return;
    }

    const [projectIdStr, taskIdStr] = selectedTaskKey.split("_");
    const projectId = Number(projectIdStr);
    const taskId = Number(taskIdStr);

    if (!hours || hours <= 0 || hours > 24) {
      setErrorMessage("Số giờ làm việc phải lớn hơn 0 và không vượt quá 24 giờ.");
      return;
    }

    if (!description.trim()) {
      setErrorMessage("Mô tả nội dung công việc là bắt buộc theo quy định.");
      return;
    }

    setIsSubmitting(true);
    try {
      if (initialData) {
        await updateWorkLog(initialData.id, {
          projectId,
          taskId,
          workDate,
          hours: Number(hours),
          isBillable,
          description: description.trim(),
        });
      } else {
        await createWorkLog({
          projectId,
          taskId,
          workDate,
          hours: Number(hours),
          isBillable,
          description: description.trim(),
        });
      }

      onSuccess();
      onClose();
    } catch (err: unknown) {
      console.error("Lỗi lưu giờ làm việc:", err);
      setErrorMessage(
        err instanceof Error
          ? err.message
          : "Không thể lưu giờ làm việc. Vui lòng kiểm tra lại."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl border border-slate-100 animate-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
              <Clock className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {initialData ? "Chỉnh sửa giờ công" : "Ghi giờ công theo công việc"}
              </h3>
            </div>
          </div>
          <button
            onClick={onClose}
            type="button"
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="mt-4 flex items-start gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-xs text-rose-800">
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
            <div className="flex-1 font-semibold">{errorMessage}</div>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          {/* Project & Task Selection */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1.5 flex items-center gap-1.5">
              <Briefcase className="h-3.5 w-3.5 text-slate-500" />
              Dự án &amp; Công việc được giao <span className="text-rose-500">*</span>
            </label>
            {isLoadingTasks ? (
              <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 p-2.5 text-xs text-slate-500">
                <Loader2 className="h-4 w-4 animate-spin text-indigo-600" />
                Đang tải danh sách công việc được phân công...
              </div>
            ) : tasks.length === 0 ? (
              <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
                Bạn chưa được phân công vào công việc nào đang hoạt động. Vui lòng liên hệ Quản lý dự án (PM).
              </div>
            ) : (
              <select
                value={selectedTaskKey}
                onChange={(e) => setSelectedTaskKey(e.target.value)}
                required
                className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-xs font-medium text-slate-800 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 cursor-pointer"
              >
                <option value="">-- Chọn công việc thực hiện --</option>
                {tasks.map((t) => (
                  <option
                    key={`${t.projectId}_${t.taskId}`}
                    value={`${t.projectId}_${t.taskId}`}
                  >
                    [{t.projectName}] - {t.taskName} ({t.taskStatus || "Đang thực hiện"})
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Work Date & Hours in a 2-column grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5 flex items-center gap-1.5">
                <Calendar className="h-3.5 w-3.5 text-slate-500" />
                Ngày làm việc <span className="text-rose-500">*</span>
              </label>
              <input
                type="date"
                value={workDate}
                onChange={(e) => setWorkDate(e.target.value)}
                required
                className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1.5 flex items-center gap-1.5">
                <Clock className="h-3.5 w-3.5 text-slate-500" />
                Số giờ làm (Hours) <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                step="any"
                min="0.1"
                max="24"
                value={hours}
                onChange={(e) => setHours(parseFloat(e.target.value) || 0)}
                required
                className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-indigo-700 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 font-mono"
              />
              {/* Quick Hours Presets */}
              <div className="mt-1.5 flex items-center gap-1.5">
                {[1, 2, 4, 8].map((h) => (
                  <button
                    key={h}
                    type="button"
                    onClick={() => setHours(h)}
                    className={`rounded-lg px-2 py-0.5 text-[11px] font-semibold transition cursor-pointer ${
                      hours === h
                        ? "bg-indigo-600 text-white"
                        : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                    }`}
                  >
                    {h}h
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Billable Checkbox */}
          <div className="flex items-center gap-2.5 rounded-xl border border-slate-200 bg-slate-50/50 p-3">
            <input
              type="checkbox"
              id="isBillable"
              checked={isBillable}
              onChange={(e) => setIsBillable(e.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer"
            />
            <label
              htmlFor="isBillable"
              className="text-xs font-bold text-slate-800 flex items-center gap-1.5 cursor-pointer"
            >
              <DollarSign className="h-3.5 w-3.5 text-emerald-600" />
              Tính phí khách hàng (Billable hours)
            </label>
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1.5 flex items-center gap-1.5">
              <FileText className="h-3.5 w-3.5 text-slate-500" />
              Mô tả chi tiết công việc đã làm <span className="text-rose-500">*</span>
            </label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Ghi rõ các đầu việc, tính năng hoặc sửa lỗi đã hoàn thành trong ngày..."
              required
              className="w-full rounded-xl border border-slate-200 bg-white p-3 text-xs font-normal text-slate-800 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
            />
            <p className="mt-1 text-[11px] text-slate-400">
              * Quy tắc QTN-09: Tổng giờ công trong một ngày của nhân viên không được vượt quá 12 giờ.
            </p>
          </div>

          {/* Footer Actions */}
          <div className="mt-6 flex items-center justify-end gap-3 border-t border-slate-100 pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-bold text-slate-700 hover:bg-slate-50 transition cursor-pointer"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting || tasks.length === 0}
              className="flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-2.5 text-xs font-bold text-white shadow-xs hover:bg-indigo-700 transition disabled:opacity-50 cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Đang ghi nhận...
                </>
              ) : (
                <>
                  <CheckCircle2 className="h-4 w-4" />
                  {initialData ? "Lưu thay đổi" : "Ghi nhận giờ công"}
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
