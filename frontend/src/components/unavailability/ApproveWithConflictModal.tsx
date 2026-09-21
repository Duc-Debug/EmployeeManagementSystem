"use client";

import { useState, useEffect } from "react";
import { X, AlertTriangle, CheckCircle2, Loader2 } from "lucide-react";
import {
  checkUnavailabilityConflict,
  approveUnavailability,
  type UnavailabilityDeclarationResult,
  type UnavailabilityConflictCheckResult,
} from "@/lib/api/unavailability";

interface ApproveWithConflictModalProps {
  isOpen: boolean;
  declaration: UnavailabilityDeclarationResult | null;
  onClose: () => void;
  onSuccess: (result: UnavailabilityDeclarationResult) => void;
}

export default function ApproveWithConflictModal({
  isOpen,
  declaration,
  onClose,
  onSuccess,
}: ApproveWithConflictModalProps) {
  const [isChecking, setIsChecking] = useState<boolean>(true);
  const [conflictResult, setConflictResult] = useState<UnavailabilityConflictCheckResult | null>(null);
  const [checkError, setCheckError] = useState<string | null>(null);

  const [confirmConflictWarning, setConfirmConflictWarning] = useState<boolean>(false);
  const [approverComment, setApproverComment] = useState<string>("");
  const [isApproving, setIsApproving] = useState<boolean>(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen || !declaration) {
      setConflictResult(null);
      setCheckError(null);
      setConfirmConflictWarning(false);
      setApproverComment("");
      setSubmitError(null);
      return;
    }

    let isMounted = true;
    async function runCheck() {
      try {
        setIsChecking(true);
        setCheckError(null);
        const res = await checkUnavailabilityConflict(declaration!.id);
        if (isMounted) {
          setConflictResult(res);
        }
      } catch (err: unknown) {
        if (isMounted) {
          const msg = err instanceof Error ? err.message : "Không thể kiểm tra xung đột phân bổ.";
          setCheckError(msg);
        }
      } finally {
        if (isMounted) {
          setIsChecking(false);
        }
      }
    }

    runCheck();
    return () => {
      isMounted = false;
    };
  }, [isOpen, declaration]);

  const handleRetryCheck = async () => {
    if (!declaration) return;
    try {
      setIsChecking(true);
      setCheckError(null);
      const res = await checkUnavailabilityConflict(declaration.id);
      setConflictResult(res);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Không thể kiểm tra xung đột phân bổ.";
      setCheckError(msg);
    } finally {
      setIsChecking(false);
    }
  };

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !isApproving) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, isApproving, onClose]);

  if (!isOpen || !declaration) return null;

  const handleApprove = async () => {
    setSubmitError(null);
    if (conflictResult?.hasConflict && !confirmConflictWarning) {
      setSubmitError("Bạn cần xác nhận đồng ý với cảnh báo xung đột phân bổ theo quy tắc QTN-24.");
      return;
    }

    try {
      setIsApproving(true);
      const res = await approveUnavailability(declaration.id, {
        approverComment: approverComment.trim() ? approverComment.trim() : undefined,
        confirmConflictWarning: conflictResult?.hasConflict ? confirmConflictWarning : undefined,
      });
      onSuccess(res);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Phê duyệt thất bại.";
      setSubmitError(msg);
    } finally {
      setIsApproving(false);
    }
  };

  const hasConflict = conflictResult?.hasConflict === true;

  return (
    <div
      role="dialog"
      aria-modal="true"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isApproving) onClose();
      }}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150"
    >
      <div className="relative w-full max-w-lg bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div>
            <h3 className="text-base font-bold text-slate-900">Phê duyệt thời gian không sẵn sàng</h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Đơn #{declaration.id} &bull; {declaration.startDate} đến {declaration.endDate} ({declaration.totalHoursDeducted}h)
            </p>
          </div>
          <button
            onClick={onClose}
            disabled={isApproving}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 space-y-4 max-h-[80vh] overflow-y-auto">
          {submitError && (
            <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-xs">
              {submitError}
            </div>
          )}

          {/* Loading conflict check */}
          {isChecking ? (
            <div className="flex items-center justify-center gap-2 py-8 text-xs text-slate-500">
              <Loader2 className="w-4 h-4 animate-spin text-indigo-600" />
              <span>Đang kiểm tra xung đột với kế hoạch phân bổ dự án...</span>
            </div>
          ) : checkError ? (
            <div className="p-3.5 bg-amber-50 border border-amber-200 text-amber-800 rounded-xl text-xs flex items-center justify-between gap-3">
              <span>Không thể kiểm tra xung đột tự động: {checkError}</span>
              <button
                type="button"
                onClick={handleRetryCheck}
                className="px-2.5 py-1 text-xs font-semibold text-amber-900 bg-amber-200/70 hover:bg-amber-200 rounded-lg transition shrink-0"
              >
                Thử lại
              </button>
            </div>
          ) : hasConflict ? (
            /* Conflict Warning Banner per QTN-24 */
            <div className="space-y-3">
              <div className="p-4 bg-amber-50/80 border border-amber-200 rounded-2xl text-xs space-y-2.5">
                <div className="flex items-start gap-2.5 text-amber-900 font-semibold">
                  <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                  <div>
                    <p className="text-sm font-bold text-amber-900">Cảnh báo xung đột phân bổ dự án</p>
                    <p className="font-normal text-amber-800 mt-1 leading-relaxed">
                      Khoảng thời gian khai báo này trùng với{" "}
                      <strong>{conflictResult?.conflictingAllocationsCount} phân bổ dự án</strong> đã lên lịch trong tuần
                      {conflictResult?.totalConflictingHours !== undefined && (
                        <> (tổng <strong>{conflictResult.totalConflictingHours} giờ</strong>)</>
                      )}.
                    </p>
                  </div>
                </div>

                {/* Conflicting allocations list */}
                {conflictResult?.conflictingAllocations && conflictResult.conflictingAllocations.length > 0 && (
                  <div className="mt-2 space-y-1.5 bg-white/80 p-3 rounded-xl border border-amber-200/60 text-slate-700">
                    <p className="text-[11px] font-semibold text-slate-600 uppercase tracking-wider">
                      Chi tiết phân bổ bị ảnh hưởng:
                    </p>
                    <div className="divide-y divide-amber-100 text-xs">
                      {conflictResult.conflictingAllocations.map((c, idx) => (
                        <div key={idx} className="py-1.5 flex items-center justify-between">
                          <span>
                            Dự án #{c.projectId} &bull; Tuần <strong>W{c.weekNumber}/{c.year}</strong>
                          </span>
                          <span className="font-bold text-amber-800">{c.allocatedHours} giờ</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* QTN-24 Policy reminder */}
                <div className="p-2.5 bg-amber-100/60 rounded-xl text-[11px] text-amber-900 leading-relaxed">
                  <strong>Quy định nghiệp vụ QTN-24:</strong> Khi duyệt, kế hoạch phân bổ dự án{" "}
                  <strong>không bị hủy hay giảm giờ</strong>. Hệ thống sẽ tự động trừ giờ khả dụng tuần và gắn cờ{" "}
                  <span className="font-bold text-rose-700">quá tải</span> nếu tổng giờ phân bổ vượt quá năng lực.
                </div>

                {/* Confirmation Checkbox */}
                <label className="flex items-start gap-2.5 pt-1 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={confirmConflictWarning}
                    onChange={(e) => setConfirmConflictWarning(e.target.checked)}
                    className="w-4 h-4 mt-0.5 rounded text-amber-600 focus:ring-amber-500 border-slate-300"
                  />
                  <span className="text-xs font-semibold text-slate-800 leading-normal">
                    Tôi đã đọc và đồng ý phê duyệt đơn này dù có xung đột phân bổ dự án theo quy định QTN-24.
                  </span>
                </label>
              </div>
            </div>
          ) : (
            /* No conflict banner */
            <div className="flex items-center gap-3 p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-800">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
              <span>
                Không phát hiện xung đột nào với kế hoạch phân bổ dự án hiện có.
              </span>
            </div>
          )}

          {/* Approver Comment */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Ghi chú của người duyệt (Tùy chọn)
            </label>
            <textarea
              rows={2}
              value={approverComment}
              onChange={(e) => setApproverComment(e.target.value)}
              placeholder="VD: Đã đồng ý cho tham gia khóa đào tạo..."
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition resize-none placeholder:text-slate-400"
            />
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-slate-100 bg-slate-50/50">
          <button
            type="button"
            onClick={onClose}
            disabled={isApproving}
            className="px-4 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
          >
            Đóng
          </button>
          <button
            type="button"
            onClick={handleApprove}
            disabled={isChecking || isApproving || (hasConflict && !confirmConflictWarning)}
            className="flex items-center gap-2 px-5 py-2 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl shadow-xs transition disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {isApproving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            Xác nhận Phê duyệt
          </button>
        </div>
      </div>
    </div>
  );
}