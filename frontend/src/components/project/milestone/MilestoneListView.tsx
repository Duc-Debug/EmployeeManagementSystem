import React, { useState } from "react";
import {
  Flag,
  Plus,
  Search,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Calendar,
  Edit2,
  Trash2,
  Check,
  Layers,
  List,
  GitCommit,
  RefreshCw,
} from "lucide-react";
import type {
  MilestoneResult,
  MilestoneStatus,
  CreateMilestonePayload,
  UpdateMilestonePayload,
} from "@/lib/api/milestones";
import type { TaskCategoryGroup } from "../projectData";
import { MilestoneStatsSection } from "./MilestoneStatsSection";
import { MilestoneFormModal } from "./MilestoneFormModal";
import { MilestoneDeleteModal } from "./MilestoneDeleteModal";

interface MilestoneListViewProps {
  milestones: MilestoneResult[];
  categories: TaskCategoryGroup[];
  canEdit: boolean;
  isLoading: boolean;
  onRefresh: () => void;
  onCreateMilestone: (payload: CreateMilestonePayload) => Promise<void>;
  onUpdateMilestone: (id: number, payload: UpdateMilestonePayload) => Promise<void>;
  onCompleteMilestone: (id: number) => Promise<void>;
  onDeleteMilestone: (id: number) => Promise<void>;
}

export const MilestoneListView: React.FC<MilestoneListViewProps> = ({
  milestones,
  categories,
  canEdit,
  isLoading,
  onRefresh,
  onCreateMilestone,
  onUpdateMilestone,
  onCompleteMilestone,
  onDeleteMilestone,
}) => {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | MilestoneStatus>("ALL");
  const [displayLayout, setDisplayLayout] = useState<"timeline" | "table">("timeline");

  // Modal states
  const [formModalOpen, setFormModalOpen] = useState(false);
  const [selectedMilestoneForEdit, setSelectedMilestoneForEdit] = useState<MilestoneResult | null>(null);
  const [isSubmittingForm, setIsSubmittingForm] = useState(false);

  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [selectedMilestoneForDelete, setSelectedMilestoneForDelete] = useState<MilestoneResult | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Filtered list
  const filteredMilestones = milestones.filter((m) => {
    const matchesSearch =
      m.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (m.description && m.description.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesStatus = statusFilter === "ALL" || m.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  // Sort by plannedDate ascending
  const sortedMilestones = [...filteredMilestones].sort((a, b) => {
    return new Date(a.plannedDate).getTime() - new Date(b.plannedDate).getTime();
  });

  const handleOpenCreate = () => {
    setSelectedMilestoneForEdit(null);
    setFormModalOpen(true);
  };

  const handleOpenEdit = (m: MilestoneResult) => {
    setSelectedMilestoneForEdit(m);
    setFormModalOpen(true);
  };

  const handleOpenDelete = (m: MilestoneResult) => {
    setSelectedMilestoneForDelete(m);
    setDeleteModalOpen(true);
  };

  const handleFormSubmit = async (payload: CreateMilestonePayload | UpdateMilestonePayload) => {
    setIsSubmittingForm(true);
    try {
      if (selectedMilestoneForEdit) {
        await onUpdateMilestone(selectedMilestoneForEdit.id, payload as UpdateMilestonePayload);
      } else {
        await onCreateMilestone(payload as CreateMilestonePayload);
      }
      setFormModalOpen(false);
    } finally {
      setIsSubmittingForm(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!selectedMilestoneForDelete) return;
    setIsDeleting(true);
    try {
      await onDeleteMilestone(selectedMilestoneForDelete.id);
      setDeleteModalOpen(false);
      setSelectedMilestoneForDelete(null);
    } finally {
      setIsDeleting(false);
    }
  };

  const renderStatusBadge = (status: MilestoneStatus, delayDays: number) => {
    switch (status) {
      case "ON_TRACK":
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
            Đúng tiến độ
          </span>
        );
      case "DELAYED":
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-0.5 text-xs font-semibold text-rose-700">
            <span className="h-1.5 w-1.5 rounded-full bg-rose-500 animate-pulse" />
            Trễ hạn ({delayDays} ngày)
          </span>
        );
      case "COMPLETED":
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full border border-sky-200 bg-sky-50 px-2.5 py-0.5 text-xs font-semibold text-sky-700">
            <CheckCircle2 className="h-3.5 w-3.5 text-sky-600" />
            Đã hoàn thành
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* 1. Metric Stats Cards */}
      <MilestoneStatsSection milestones={milestones} />

      {/* 2. Controls & Filter Bar */}
      <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs sm:flex-row sm:items-center sm:justify-between">
        {/* Search & Status Filter */}
        <div className="flex flex-1 flex-wrap items-center gap-2">
          <div className="relative flex-1 min-w-[200px] sm:max-w-xs">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm kiếm mốc tiến độ..."
              className="w-full rounded-xl border border-slate-200 bg-slate-50 py-1.5 pl-8 pr-3 text-xs text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white"
            />
          </div>

          {/* Status Tabs */}
          <div className="inline-flex rounded-xl border border-slate-200 bg-slate-50 p-1">
            <button
              type="button"
              onClick={() => setStatusFilter("ALL")}
              className={`rounded-lg px-2.5 py-1 text-xs font-medium transition cursor-pointer ${
                statusFilter === "ALL"
                  ? "bg-white text-indigo-700 shadow-2xs font-semibold"
                  : "text-slate-600 hover:text-slate-900"
              }`}
            >
              Tất cả
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter("ON_TRACK")}
              className={`rounded-lg px-2.5 py-1 text-xs font-medium transition cursor-pointer ${
                statusFilter === "ON_TRACK"
                  ? "bg-white text-emerald-700 shadow-2xs font-semibold"
                  : "text-slate-600 hover:text-slate-900"
              }`}
            >
              Đúng hạn
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter("DELAYED")}
              className={`rounded-lg px-2.5 py-1 text-xs font-medium transition cursor-pointer ${
                statusFilter === "DELAYED"
                  ? "bg-white text-rose-700 shadow-2xs font-semibold"
                  : "text-slate-600 hover:text-slate-900"
              }`}
            >
              Trễ hạn
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter("COMPLETED")}
              className={`rounded-lg px-2.5 py-1 text-xs font-medium transition cursor-pointer ${
                statusFilter === "COMPLETED"
                  ? "bg-white text-sky-700 shadow-2xs font-semibold"
                  : "text-slate-600 hover:text-slate-900"
              }`}
            >
              Hoàn thành
            </button>
          </div>
        </div>

        {/* View Switcher & Action Buttons */}
        <div className="flex items-center gap-2">
          {/* Switch Timeline / Table */}
          <div className="inline-flex rounded-xl border border-slate-200 bg-slate-50 p-1">
            <button
              type="button"
              onClick={() => setDisplayLayout("timeline")}
              title="Xem dạng Dòng thời gian"
              className={`rounded-lg p-1.5 text-xs transition cursor-pointer ${
                displayLayout === "timeline"
                  ? "bg-white text-indigo-700 shadow-2xs"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              <GitCommit className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={() => setDisplayLayout("table")}
              title="Xem dạng Bảng chi tiết"
              className={`rounded-lg p-1.5 text-xs transition cursor-pointer ${
                displayLayout === "table"
                  ? "bg-white text-indigo-700 shadow-2xs"
                  : "text-slate-500 hover:text-slate-800"
              }`}
            >
              <List className="h-4 w-4" />
            </button>
          </div>

          <button
            type="button"
            onClick={onRefresh}
            disabled={isLoading}
            title="Tải lại danh sách"
            className="rounded-xl border border-slate-200 bg-white p-2 text-slate-600 hover:bg-slate-50 transition cursor-pointer disabled:opacity-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? "animate-spin text-indigo-600" : ""}`} />
          </button>

          {canEdit && (
            <button
              type="button"
              onClick={handleOpenCreate}
              className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3.5 py-2 text-xs font-semibold text-white shadow-md shadow-indigo-100 transition hover:bg-indigo-700 cursor-pointer"
            >
              <Plus className="h-4 w-4" />
              <span>Khai báo mốc tiến độ</span>
            </button>
          )}
        </div>
      </div>

      {/* 3. Main Content: Timeline vs Table */}
      {sortedMilestones.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white p-12 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-100 mb-3">
            <Flag className="h-7 w-7" />
          </div>
          <h3 className="text-sm font-bold text-slate-800 mb-1">
            {searchTerm || statusFilter !== "ALL"
              ? "Không tìm thấy mốc tiến độ phù hợp"
              : "Dự án chưa có mốc tiến độ nào"}
          </h3>
          <p className="text-xs text-slate-500 max-w-sm mb-4">
            {searchTerm || statusFilter !== "ALL"
              ? "Hãy thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm."
              : "Khai báo các mốc nghiệm thu quan trọng để kiểm soát tiến độ hoàn thành các hạng mục WBS."}
          </p>
          {canEdit && !searchTerm && statusFilter === "ALL" && (
            <button
              type="button"
              onClick={handleOpenCreate}
              className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 transition cursor-pointer"
            >
              <Plus className="h-4 w-4" />
              <span>Thêm mốc đầu tiên</span>
            </button>
          )}
        </div>
      ) : displayLayout === "timeline" ? (
        /* ========== TIMELINE ROADMAP VIEW ========== */
        <div className="relative pl-6 sm:pl-8 space-y-6 before:absolute before:bottom-3 before:left-3 before:top-3 before:w-0.5 before:bg-slate-200 sm:before:left-4">
          {sortedMilestones.map((milestone) => {
            const isCompleted = milestone.status === "COMPLETED";
            const isDelayed = milestone.status === "DELAYED";
            const taskRate =
              milestone.totalLinkedTasks > 0
                ? Math.round(
                    (milestone.completedLinkedTasks / milestone.totalLinkedTasks) * 100
                  )
                : isCompleted
                ? 100
                : 0;

            return (
              <div key={milestone.id} className="relative group">
                {/* Timeline node icon */}
                <div
                  className={`absolute -left-6 sm:-left-8 top-3 flex h-6 w-6 sm:h-7 sm:w-7 items-center justify-center rounded-full border-2 bg-white shadow-xs transition-transform group-hover:scale-110 ${
                    isCompleted
                      ? "border-sky-500 text-sky-600"
                      : isDelayed
                      ? "border-rose-500 text-rose-600 animate-pulse"
                      : "border-emerald-500 text-emerald-600"
                  }`}
                >
                  {isCompleted ? (
                    <Check className="h-3 w-3 sm:h-3.5 sm:w-3.5 stroke-[3]" />
                  ) : isDelayed ? (
                    <AlertTriangle className="h-3 w-3 sm:h-3.5 sm:w-3.5" />
                  ) : (
                    <Clock className="h-3 w-3 sm:h-3.5 sm:w-3.5" />
                  )}
                </div>

                {/* Milestone Card */}
                <div
                  className={`rounded-2xl border p-5 bg-white shadow-2xs transition hover:shadow-md ${
                    isDelayed
                      ? "border-rose-200/80 bg-rose-50/10"
                      : isCompleted
                      ? "border-slate-200"
                      : "border-slate-200"
                  }`}
                >
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <h4 className="text-sm font-bold text-slate-900 tracking-tight">
                          {milestone.name}
                        </h4>
                        {renderStatusBadge(milestone.status, milestone.delayDays)}
                      </div>
                      {milestone.description && (
                        <p className="mt-1.5 text-xs text-slate-600 leading-relaxed max-w-2xl">
                          {milestone.description}
                        </p>
                      )}
                    </div>

                    {/* Action buttons */}
                    {canEdit && (
                      <div className="flex items-center gap-1.5 self-end sm:self-auto shrink-0">
                        {!isCompleted && (
                          <button
                            type="button"
                            onClick={() => onCompleteMilestone(milestone.id)}
                            title="Đánh dấu hoàn thành mốc này"
                            className="inline-flex items-center gap-1 rounded-xl bg-sky-50 px-2.5 py-1.5 text-xs font-semibold text-sky-700 hover:bg-sky-100 transition cursor-pointer border border-sky-100"
                          >
                            <Check className="h-3.5 w-3.5" />
                            <span>Nghiệm thu</span>
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => handleOpenEdit(milestone)}
                          title="Sửa thông tin mốc"
                          className="rounded-xl border border-slate-200 p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition cursor-pointer"
                        >
                          <Edit2 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleOpenDelete(milestone)}
                          title="Xóa mốc"
                          className="rounded-xl border border-slate-200 p-1.5 text-slate-500 hover:bg-rose-50 hover:text-rose-600 hover:border-rose-200 transition cursor-pointer"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Date & Progress Grid */}
                  <div className="mt-4 pt-4 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-3 gap-4">
                    {/* Ngày kế hoạch */}
                    <div className="flex items-center gap-2.5">
                      <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-100 text-slate-600 shrink-0">
                        <Calendar className="h-4 w-4" />
                      </div>
                      <div>
                        <span className="block text-[11px] font-medium text-slate-400">
                          Kế hoạch nghiệm thu
                        </span>
                        <span className="text-xs font-semibold text-slate-800">
                          {milestone.plannedDate}
                        </span>
                      </div>
                    </div>

                    {/* Ngày thực tế */}
                    <div className="flex items-center gap-2.5">
                      <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-100 text-slate-600 shrink-0">
                        <CheckCircle2 className="h-4 w-4" />
                      </div>
                      <div>
                        <span className="block text-[11px] font-medium text-slate-400">
                          Ngày hoàn thành thực tế
                        </span>
                        <span className="text-xs font-semibold text-slate-800">
                          {milestone.actualDate || "Chưa nghiệm thu"}
                        </span>
                      </div>
                    </div>

                    {/* Hạng mục WBS liên kết */}
                    <div className="flex flex-col justify-center">
                      <div className="flex items-center justify-between text-[11px] mb-1">
                        <span className="font-medium text-slate-500 flex items-center gap-1">
                          <Layers className="h-3 w-3" /> WBS liên kết
                        </span>
                        <span className="font-semibold text-slate-800">
                          {milestone.completedLinkedTasks}/{milestone.totalLinkedTasks} việc ({taskRate}%)
                        </span>
                      </div>
                      <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
                        <div
                          className={`h-1.5 rounded-full transition-all duration-300 ${
                            isCompleted
                              ? "bg-sky-500"
                              : isDelayed
                              ? "bg-rose-500"
                              : "bg-emerald-500"
                          }`}
                          style={{ width: `${taskRate}%` }}
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* ========== TABLE VIEW ========== */
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-slate-200 bg-slate-50/80 font-semibold text-slate-600">
                <tr>
                  <th className="py-3 px-4">Tên mốc tiến độ</th>
                  <th className="py-3 px-4">Kế hoạch</th>
                  <th className="py-3 px-4">Thực tế</th>
                  <th className="py-3 px-4">Trạng thái</th>
                  <th className="py-3 px-4">Tiến độ WBS</th>
                  <th className="py-3 px-4">Độ trễ</th>
                  {canEdit && <th className="py-3 px-4 text-right">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {sortedMilestones.map((m) => {
                  const isCompleted = m.status === "COMPLETED";
                  const taskRate =
                    m.totalLinkedTasks > 0
                      ? Math.round((m.completedLinkedTasks / m.totalLinkedTasks) * 100)
                      : isCompleted
                      ? 100
                      : 0;

                  return (
                    <tr key={m.id} className="hover:bg-slate-50/70 transition">
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-slate-900">{m.name}</div>
                        {m.description && (
                          <div className="text-[11px] text-slate-500 truncate max-w-xs">
                            {m.description}
                          </div>
                        )}
                      </td>
                      <td className="py-3.5 px-4 font-mono font-medium text-slate-700">
                        {m.plannedDate}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-slate-600">
                        {m.actualDate || "--"}
                      </td>
                      <td className="py-3.5 px-4">
                        {renderStatusBadge(m.status, m.delayDays)}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2 min-w-[120px]">
                          <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-slate-100">
                            <div
                              className={`h-1.5 rounded-full ${
                                isCompleted
                                  ? "bg-sky-500"
                                  : m.status === "DELAYED"
                                  ? "bg-rose-500"
                                  : "bg-emerald-500"
                              }`}
                              style={{ width: `${taskRate}%` }}
                            />
                          </div>
                          <span className="text-[11px] font-semibold text-slate-600 shrink-0">
                            {m.completedLinkedTasks}/{m.totalLinkedTasks}
                          </span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        {m.delayDays > 0 ? (
                          <span className="font-semibold text-rose-600">
                            +{m.delayDays} ngày
                          </span>
                        ) : (
                          <span className="text-slate-400">0</span>
                        )}
                      </td>
                      {canEdit && (
                        <td className="py-3.5 px-4 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            {!isCompleted && (
                              <button
                                type="button"
                                onClick={() => onCompleteMilestone(m.id)}
                                title="Nghiệm thu mốc"
                                className="rounded-lg p-1.5 text-sky-600 hover:bg-sky-50 transition cursor-pointer"
                              >
                                <Check className="h-4 w-4" />
                              </button>
                            )}
                            <button
                              type="button"
                              onClick={() => handleOpenEdit(m)}
                              title="Chỉnh sửa"
                              className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition cursor-pointer"
                            >
                              <Edit2 className="h-3.5 w-3.5" />
                            </button>
                            <button
                              type="button"
                              onClick={() => handleOpenDelete(m)}
                              title="Xóa"
                              className="rounded-lg p-1.5 text-slate-500 hover:bg-rose-50 hover:text-rose-600 transition cursor-pointer"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Form Modal (Create / Edit) */}
      <MilestoneFormModal
        open={formModalOpen}
        milestone={selectedMilestoneForEdit}
        categories={categories}
        isSubmitting={isSubmittingForm}
        onClose={() => setFormModalOpen(false)}
        onSubmit={handleFormSubmit}
      />

      {/* Delete Confirmation Modal */}
      <MilestoneDeleteModal
        open={deleteModalOpen}
        milestone={selectedMilestoneForDelete}
        isDeleting={isDeleting}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={handleDeleteConfirm}
      />
    </div>
  );
};
