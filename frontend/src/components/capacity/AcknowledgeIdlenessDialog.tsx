"use client";

import { useState, useEffect } from "react";
import { X, AlertTriangle, Loader2, Send, Sparkles } from "lucide-react";
import {
  acknowledgeProlongedIdleStaff,
  type ProlongedIdleStaffItem,
} from "@/lib/api/prolonged-idleness";

interface AcknowledgeIdlenessDialogProps {
  open: boolean;
  onClose: () => void;
  staff: ProlongedIdleStaffItem | null;
  onSuccess: () => void;
}

const PRESET_ACTIONS = [
  "Điều phối sang dự án mới",
  "Giao việc R&D / Dự án nội bộ",
  "Đào tạo nâng cao kỹ năng",
  "Thương lượng lại thời gian làm việc",
];

export function AcknowledgeIdlenessDialog({
  open,
  onClose,
  staff,
  onSuccess,
}: AcknowledgeIdlenessDialogProps) {
  const [actionTaken, setActionTaken] = useState("");
  const [notes, setNotes] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setActionTaken("");
      setNotes("");
      setErrorMessage(null);
    }
  }, [open, staff]);

  if (!open || !staff) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedAction = actionTaken.trim();
    if (!trimmedAction) {
      setErrorMessage("Vui lòng nhập hoặc chọn hành động xử lý.");
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      await acknowledgeProlongedIdleStaff({
        employeeId: staff.employeeId,
        actionTaken: trimmedAction,
        notes: notes.trim() || undefined,
      });
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Xử lý cảnh báo thất bại. Vui lòng thử lại.";
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="relative w-full max-w-lg overflow-hidden rounded-3xl bg-white shadow-2xl border border-slate-200">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-gradient-to-r from-amber-50 to-orange-50/30">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-amber-500 text-white shadow-xs">
              <AlertTriangle className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Xác nhận xử lý cảnh báo nhàn rỗi
              </h3>
              <p className="text-xs text-slate-500">
                Lưu vết kiểm toán hành động can thiệp của Quản lý nguồn lực (TC-04)
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="rounded-xl p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Content */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {errorMessage && (
            <div className="rounded-2xl border border-rose-200 bg-rose-50/80 p-3.5 text-xs text-rose-800 flex items-start gap-2.5">
              <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Thông tin nhân sự tóm tắt */}
          <div className="rounded-2xl border border-slate-200 bg-slate-50/70 p-4 space-y-2 text-xs">
            <div className="flex justify-between items-center">
              <span className="font-semibold text-slate-500">Nhân sự:</span>
              <span className="font-bold text-slate-900 text-sm">
                {staff.fullName} ({staff.employeeCode})
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-500">Phòng ban / Vị trí:</span>
              <span className="text-slate-700">
                {staff.departmentName} &bull; {staff.positionTitle}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-500">Thời gian nhàn rỗi:</span>
              <span className="inline-flex items-center gap-1.5 font-bold text-amber-700 bg-amber-100/80 px-2 py-0.5 rounded-lg">
                {staff.consecutiveIdleWeeks} tuần liên tiếp (Trống {staff.totalEmptyHours}h)
              </span>
            </div>
          </div>

          {/* Gợi ý hành động nhanh */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
              <Sparkles className="h-3.5 w-3.5 text-amber-500" />
              <span>Gợi ý phương án can thiệp nhanh:</span>
            </label>
            <div className="flex flex-wrap gap-1.5">
              {PRESET_ACTIONS.map((preset) => (
                <button
                  key={preset}
                  type="button"
                  onClick={() => setActionTaken(preset)}
                  className={`text-xs px-3 py-1.5 rounded-xl border transition ${
                    actionTaken === preset
                      ? "bg-amber-100 border-amber-400 text-amber-900 font-semibold"
                      : "bg-white border-slate-200 text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  {preset}
                </button>
              ))}
            </div>
          </div>

          {/* Nhập hành động cụ thể */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-800">
              Hành động xử lý <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              value={actionTaken}
              onChange={(e) => setActionTaken(e.target.value)}
              placeholder="VD: Điều chuyển sang hỗ trợ dự án Portal từ tuần sau"
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-xs text-slate-900 focus:border-amber-500 focus:outline-none focus:ring-2 focus:ring-amber-500/20"
            />
          </div>

          {/* Ghi chú thêm */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Ghi chú thêm (Tùy chọn)
            </label>
            <textarea
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Chi tiết thỏa thuận với PM hoặc ghi chú kế hoạch..."
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2 text-xs text-slate-900 focus:border-amber-500 focus:outline-none focus:ring-2 focus:ring-amber-500/20 resize-none"
            />
          </div>

          {/* Footer buttons */}
          <div className="flex items-center justify-end gap-2.5 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-2xs"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting || !actionTaken.trim()}
              className="inline-flex items-center gap-1.5 rounded-xl bg-amber-600 px-4 py-2 text-xs font-semibold text-white hover:bg-amber-700 transition shadow-2xs disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Đang lưu vết...</span>
                </>
              ) : (
                <>
                  <Send className="h-4 w-4" />
                  <span>Xác nhận & Lưu vết</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
