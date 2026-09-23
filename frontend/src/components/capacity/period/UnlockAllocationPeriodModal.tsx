/**
 * NCL-06-CN-009: Modal mở lại kỳ kế hoạch phân bổ (TC-04)
 * Yêu cầu bắt buộc nhập lý do mở lại (tối thiểu 10 ký tự) để phục vụ kiểm toán (Audit Trail)
 * Nâng cấp UX: Phím tắt Escape, Backdrop click, Auto-focus, Reset form.
 */
import { useState, useEffect } from "react";
import { X, LockOpen, AlertTriangle, AlertCircle, Loader2 } from "lucide-react";
import {
  unlockAllocationPeriod,
  type AllocationPeriodResult,
} from "@/lib/api/allocation-periods";

interface UnlockAllocationPeriodModalProps {
  open: boolean;
  period: AllocationPeriodResult | null;
  onClose: () => void;
  onUnlocked: (updatedPeriod: AllocationPeriodResult) => void;
}

export function UnlockAllocationPeriodModal({
  open,
  period,
  onClose,
  onUnlocked,
}: UnlockAllocationPeriodModalProps) {
  const [reason, setReason] = useState<string>("");
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Phím tắt Escape để đóng modal
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [open, onClose]);

  // Reset form khi mở modal mới
  useEffect(() => {
    if (open) {
      setReason("");
      setErrorMessage(null);
    }
  }, [open]);

  if (!open || !period) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const trimmedReason = reason.trim();
    if (!trimmedReason || trimmedReason.length < 10) {
      setErrorMessage("Lý do mở lại là bắt buộc và phải có ít nhất 10 ký tự.");
      return;
    }

    try {
      setIsSubmitting(true);
      const res = await unlockAllocationPeriod(period.id, { reason: trimmedReason });
      onUnlocked(res);
      onClose();
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : "Không thể mở lại kỳ kế hoạch phân bổ. Vui lòng thử lại.";
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-amber-200 bg-amber-50/70 p-4">
          <div className="flex items-center gap-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-100 text-amber-800">
              <LockOpen className="h-5 w-5" />
            </span>
            <div>
              <h3 className="text-sm font-bold text-slate-800">
                Mở Lại Kỳ Kế Hoạch Phân Bổ (TC-04)
              </h3>
              <p className="text-[11px] text-slate-500">
                Gỡ khóa bảo vệ để cho phép điều chỉnh phân bổ nguồn lực
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
            title="Đóng (Esc)"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          {errorMessage && (
            <div className="flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-800">
              <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Cảnh báo hành động */}
          <div className="rounded-xl border border-amber-200 bg-amber-50/50 p-3.5 text-xs text-amber-900">
            <div className="flex items-center gap-1.5 font-bold mb-1">
              <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
              <span>Cảnh báo mở lại kỳ:</span>
            </div>
            <p className="text-[11px] text-amber-800 leading-relaxed">
              Bạn đang yêu cầu mở lại kỳ <strong>{period.name}</strong> (Tuần {period.startWeek} - {period.endWeek} / {period.year}).
              Sau khi mở lại, các Quản lý dự án sẽ có thể tiếp tục thêm/sửa phân bổ trong dải tuần này. Toàn bộ thông tin mở lại kèm lý do sẽ được lưu vết vào Nhật ký kiểm toán 
            </p>
          </div>

          {/* Nhập lý do bắt buộc */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Lý do mở lại kỳ <span className="text-rose-500">* (Tối thiểu 10 ký tự)</span>
            </label>
            <textarea
              required
              autoFocus
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="VD: Yêu cầu điều chỉnh phân bổ nhân sự đột xuất theo chỉ đạo của BGĐ..."
              className="w-full rounded-xl border border-slate-300 p-3 text-xs text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            <div className="mt-1 flex items-center justify-between text-[11px] text-slate-400">
              <span>Hệ thống ghi nhận vào nhật ký kiểm toán.</span>
              <span className={reason.trim().length >= 10 ? "text-emerald-600 font-semibold" : "text-rose-500"}>
                {reason.trim().length}/10 ký tự
              </span>
            </div>
          </div>

          {/* Footer actions */}
          <div className="flex items-center justify-end gap-2.5 border-t border-slate-200 pt-4 mt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting || reason.trim().length < 10}
              className="inline-flex items-center gap-1.5 rounded-xl bg-amber-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-amber-700 disabled:opacity-50 transition"
            >
              {isSubmitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
              <span>Xác nhận mở lại kỳ</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
