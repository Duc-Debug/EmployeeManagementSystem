import React, { useState, useEffect } from "react";
import { X, Calendar, Flag, CheckSquare, Square, Layers, Search, AlertCircle } from "lucide-react";
import type { MilestoneResult, CreateMilestonePayload, UpdateMilestonePayload } from "@/lib/api/milestones";
import type { TaskCategoryGroup } from "../projectData";

interface MilestoneFormModalProps {
  open: boolean;
  milestone: MilestoneResult | null; // null => Thêm mới; có object => Sửa
  categories: TaskCategoryGroup[];
  isSubmitting: boolean;
  onClose: () => void;
  onSubmit: (payload: CreateMilestonePayload | UpdateMilestonePayload) => Promise<void>;
}

export const MilestoneFormModal: React.FC<MilestoneFormModalProps> = ({
  open,
  milestone,
  categories,
  isSubmitting,
  onClose,
  onSubmit,
}) => {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [plannedDate, setPlannedDate] = useState("");
  const [actualDate, setActualDate] = useState("");
  const [selectedTaskIds, setSelectedTaskIds] = useState<number[]>([]);
  const [taskSearch, setTaskSearch] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Chuẩn bị danh sách phẳng toàn bộ task trong WBS để chọn
  const allTasks = React.useMemo(() => {
    const list: { id: number; code: string; name: string; categoryName: string; status: string }[] = [];
    categories.forEach((cat) => {
      cat.tasks.forEach((t, idx) => {
        // Parse ID thành số nếu có thể, hoặc dùng index dự phòng
        const numericId = parseInt(t.id.replace(/\D/g, ""), 10) || (idx + 1);
        list.push({
          id: numericId,
          code: t.code,
          name: t.name,
          categoryName: cat.name,
          status: t.status,
        });
      });
    });
    return list;
  }, [categories]);

  useEffect(() => {
    if (open) {
      if (milestone) {
        setName(milestone.name || "");
        setDescription(milestone.description || "");
        setPlannedDate(milestone.plannedDate ? milestone.plannedDate.substring(0, 10) : "");
        setActualDate(milestone.actualDate ? milestone.actualDate.substring(0, 10) : "");
        setSelectedTaskIds(milestone.linkedTaskIds || []);
      } else {
        setName("");
        setDescription("");
        // Mặc định ngày kế hoạch là 1 tháng sau ngày hiện tại
        const nextMonth = new Date();
        nextMonth.setMonth(nextMonth.getMonth() + 1);
        setPlannedDate(nextMonth.toISOString().substring(0, 10));
        setActualDate("");
        // Nếu có tasks, mặc định chọn task đầu tiên để thỏa mãn điều kiện WBS
        if (allTasks.length > 0) {
          setSelectedTaskIds([allTasks[0].id]);
        } else {
          setSelectedTaskIds([]);
        }
      }
      setErrors({});
      setTaskSearch("");
    }
  }, [open, milestone, allTasks]);

  if (!open) return null;

  const toggleTask = (id: number) => {
    setSelectedTaskIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleSelectAllTasks = () => {
    if (selectedTaskIds.length === allTasks.length) {
      setSelectedTaskIds([]);
    } else {
      setSelectedTaskIds(allTasks.map((t) => t.id));
    }
  };

  const filteredTasks = allTasks.filter(
    (t) =>
      t.name.toLowerCase().includes(taskSearch.toLowerCase()) ||
      t.code.toLowerCase().includes(taskSearch.toLowerCase()) ||
      t.categoryName.toLowerCase().includes(taskSearch.toLowerCase())
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const newErrors: Record<string, string> = {};

    if (!name.trim()) {
      newErrors.name = "Vui lòng nhập tên mốc tiến độ";
    } else if (name.length > 255) {
      newErrors.name = "Tên mốc tiến độ không được vượt quá 255 ký tự";
    }

    if (!plannedDate) {
      newErrors.plannedDate = "Vui lòng chọn ngày kế hoạch hoàn thành";
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    try {
      if (milestone) {
        // Cập nhật
        await onSubmit({
          name: name.trim(),
          description: description.trim() || undefined,
          plannedDate,
          actualDate: actualDate || undefined,
          linkedTaskIds: selectedTaskIds,
        });
      } else {
        // Tạo mới
        await onSubmit({
          name: name.trim(),
          description: description.trim() || undefined,
          plannedDate,
          linkedTaskIds: selectedTaskIds,
        });
      }
      onClose();
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Có lỗi xảy ra khi lưu mốc tiến độ";
      setErrors({ form: errorMessage });
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="w-full max-w-2xl rounded-2xl bg-white shadow-2xl border border-slate-100 flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <Flag className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900">
                {milestone ? "Chỉnh sửa mốc tiến độ" : "Khai báo mốc tiến độ mới"}
              </h2>
              <p className="text-xs text-slate-500">
                {milestone
                  ? "Cập nhật ngày dự kiến hoặc đánh dấu nghiệm thu mốc"
                  : "Thiết lập mốc sự kiện quan trọng và liên kết các hạng mục WBS"}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Body Form */}
        <form onSubmit={handleSubmit} className="flex flex-col flex-1 overflow-hidden">
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {errors.form && (
              <div className="flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{errors.form}</span>
              </div>
            )}

            {/* Tên mốc tiến độ */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Tên mốc tiến độ <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (errors.name) setErrors((prev) => ({ ...prev, name: "" }));
                }}
                placeholder="VD: Mốc nghiệm thu Giai đoạn 1 (Thiết kế & Phân tích)"
                className={`w-full rounded-xl border px-3.5 py-2 text-xs text-slate-800 outline-none transition ${
                  errors.name
                    ? "border-rose-400 bg-rose-50/30 focus:border-rose-500"
                    : "border-slate-200 bg-slate-50/50 focus:border-indigo-500 focus:bg-white"
                }`}
              />
              {errors.name && <p className="mt-1 text-xs text-rose-500">{errors.name}</p>}
            </div>

            {/* Mô tả chi tiết */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Mô tả mục tiêu nghiệm thu
              </label>
              <textarea
                rows={2}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Ghi chú các tiêu chí đạt được để hoàn thành mốc..."
                className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2 text-xs text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white"
              />
            </div>

            {/* Ngày kế hoạch & Ngày thực tế */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Ngày kế hoạch (Planned Date) <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <Calendar className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                  <input
                    type="date"
                    value={plannedDate}
                    onChange={(e) => {
                      setPlannedDate(e.target.value);
                      if (errors.plannedDate) setErrors((prev) => ({ ...prev, plannedDate: "" }));
                    }}
                    className={`w-full rounded-xl border py-2 pl-9 pr-3 text-xs text-slate-800 outline-none transition ${
                      errors.plannedDate
                        ? "border-rose-400 bg-rose-50/30 focus:border-rose-500"
                        : "border-slate-200 bg-slate-50/50 focus:border-indigo-500 focus:bg-white"
                    }`}
                  />
                </div>
                {errors.plannedDate && (
                  <p className="mt-1 text-xs text-rose-500">{errors.plannedDate}</p>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Ngày hoàn thành thực tế (Actual Date)
                </label>
                <div className="relative">
                  <Calendar className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                  <input
                    type="date"
                    value={actualDate}
                    onChange={(e) => setActualDate(e.target.value)}
                    className="w-full rounded-xl border border-slate-200 bg-slate-50/50 py-2 pl-9 pr-3 text-xs text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white"
                  />
                </div>
                <p className="mt-1 text-[11px] text-slate-400">
                  {actualDate ? "Mốc sẽ được tính là ĐÃ HOÀN THÀNH" : "Để trống nếu mốc đang triển khai"}
                </p>
              </div>
            </div>

            {/* Liên kết hạng mục WBS */}
            <div className="pt-2">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-1.5">
                  <Layers className="h-4 w-4 text-indigo-600" />
                  <span className="text-xs font-semibold text-slate-800">
                    Liên kết hạng mục công việc WBS ({selectedTaskIds.length} đã chọn)
                  </span>
                </div>
                {allTasks.length > 0 && (
                  <button
                    type="button"
                    onClick={handleSelectAllTasks}
                    className="text-xs font-medium text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                  >
                    {selectedTaskIds.length === allTasks.length ? "Bỏ chọn tất cả" : "Chọn tất cả"}
                  </button>
                )}
              </div>

              {allTasks.length > 0 && (
                <div className="mb-2 relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                  <input
                    type="text"
                    value={taskSearch}
                    onChange={(e) => setTaskSearch(e.target.value)}
                    placeholder="Tìm nhanh task theo tên hoặc mã..."
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 py-1.5 pl-8 pr-3 text-xs text-slate-700 outline-none focus:border-indigo-500 focus:bg-white"
                  />
                </div>
              )}

              <div className="max-h-48 overflow-y-auto rounded-xl border border-slate-200 bg-slate-50/30 p-2 space-y-1.5">
                {filteredTasks.length === 0 ? (
                  <div className="py-6 text-center text-xs text-slate-400">
                    {allTasks.length === 0
                      ? "Dự án hiện chưa có công việc WBS nào."
                      : "Không tìm thấy công việc phù hợp với từ khóa."}
                  </div>
                ) : (
                  filteredTasks.map((task) => {
                    const isSelected = selectedTaskIds.includes(task.id);
                    return (
                      <div
                        key={task.id}
                        onClick={() => toggleTask(task.id)}
                        className={`flex items-center justify-between rounded-lg p-2 text-xs transition cursor-pointer ${
                          isSelected
                            ? "bg-indigo-50/80 border border-indigo-200 text-indigo-950"
                            : "bg-white border border-slate-200/70 hover:bg-slate-50 text-slate-700"
                        }`}
                      >
                        <div className="flex items-center gap-2 min-w-0">
                          {isSelected ? (
                            <CheckSquare className="h-4 w-4 text-indigo-600 shrink-0" />
                          ) : (
                            <Square className="h-4 w-4 text-slate-300 shrink-0" />
                          )}
                          <span className="font-mono font-semibold text-slate-500 shrink-0">
                            {task.code}
                          </span>
                          <span className="font-medium truncate">{task.name}</span>
                          <span className="text-[11px] text-slate-400 hidden sm:inline truncate">
                            ({task.categoryName})
                          </span>
                        </div>
                        <span
                          className={`rounded px-1.5 py-0.5 text-[10px] font-semibold shrink-0 ${
                            task.status === "Hoàn thành" || task.status === "DONE"
                              ? "bg-emerald-100 text-emerald-700"
                              : task.status === "Đang làm" || task.status === "IN_PROGRESS"
                              ? "bg-sky-100 text-sky-700"
                              : "bg-slate-100 text-slate-600"
                          }`}
                        >
                          {task.status}
                        </span>
                      </div>
                    );
                  })
                )}
              </div>
              <p className="mt-1.5 text-[11px] text-slate-500">
                💡 Khi tất cả các công việc liên kết hoàn thành, hệ thống sẽ tự động chuyển mốc sang trạng thái <strong>COMPLETED</strong>.
              </p>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="flex items-center justify-end gap-3 border-t border-slate-100 px-6 py-4 bg-slate-50/50">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition cursor-pointer"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-2 text-xs font-semibold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 transition cursor-pointer disabled:opacity-50"
            >
              {isSubmitting ? "Đang lưu..." : milestone ? "Lưu thay đổi" : "Tạo mốc tiến độ"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
