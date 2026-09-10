import React from "react";
import { AlertTriangle, X } from "lucide-react";
import type { MilestoneResult } from "@/lib/api/milestones";

interface MilestoneDeleteModalProps {
  open: boolean;
  milestone: MilestoneResult | null;
  isDeleting: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export const MilestoneDeleteModal: React.FC<MilestoneDeleteModalProps> = ({
  open,
  milestone,
  isDeleting,
  onClose,
  onConfirm,
}) => {
  if (!open || !milestone) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl border border-slate-100">
        <div className="flex items-start justify-between">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-rose-600 border border-rose-100">
            <AlertTriangle className="h-6 w-6" />
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isDeleting}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="mt-4">
          <h3 className="text-base font-bold text-slate-900">
            Xác nhận xóa mốc tiến độ?
          </h3>
          <p className="mt-2 text-xs text-slate-500 leading-relaxed">
            Bạn có chắc chắn muốn xóa mốc tiến độ{" "}
            <strong className="text-slate-800 font-semibold">"{milestone.name}"</strong>?
            Hành động này sẽ hủy liên kết giữa mốc này và các công việc WBS liên quan. Dữ liệu mốc sẽ không thể khôi phục.
          </p>
        </div>

        <div className="mt-6 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={isDeleting}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition cursor-pointer"
          >
            Hủy bỏ
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isDeleting}
            className="inline-flex items-center gap-2 rounded-xl bg-rose-600 px-4 py-2 text-xs font-semibold text-white hover:bg-rose-700 transition shadow-sm cursor-pointer disabled:opacity-50"
          >
            {isDeleting ? "Đang xóa..." : "Xóa mốc tiến độ"}
          </button>
        </div>
      </div>
    </div>
  );
};
