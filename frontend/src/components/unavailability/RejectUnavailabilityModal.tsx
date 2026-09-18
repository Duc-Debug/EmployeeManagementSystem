"use client";

import { useState, useEffect } from "react";
import { X, AlertCircle, Loader2 } from "lucide-react";
import {
  rejectUnavailability,
  type UnavailabilityDeclarationResult,
} from "@/lib/api/unavailability";

interface RejectUnavailabilityModalProps {
  isOpen: boolean;
  declaration: UnavailabilityDeclarationResult | null;
  onClose: () => void;
  onSuccess: (result: UnavailabilityDeclarationResult) => void;
}

export default function RejectUnavailabilityModal({
  isOpen,
  declaration,
  onClose,
  onSuccess,
}: RejectUnavailabilityModalProps) {
  const [rejectReason, setRejectReason] = useState<string>("");
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !isSubmitting) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, isSubmitting, onClose]);

  if (!isOpen || !declaration) return null;

  const handleReject = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!rejectReason.trim()) {
      setErrorMessage("Vui lòng nhập lý do từ chối để thông báo cho nhân viên.");
      return;
    }

    try {
      setIsSubmitting(true);
      const res = await rejectUnavailability(declaration.id, {
        rejectReason: rejectReason.trim(),
      });
      onSuccess(res);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Từ chối thất bại.";
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      onClick={(e) => {
        if (e.target === e.currentTarget && !isSubmitting) onClose();
      }}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150"
    >
      <div className="relative w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div>
            <h3 className="text-base font-bold text-slate-900">Từ chối khai báo không sẵn sàng</h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Đơn #{declaration.id} &bull; Nhân viên #{declaration.employeeId}
            </p>
          </div>
          <button
            onClick={onClose}
            disabled={isSubmitting}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleReject} className="p-6 space-y-4">
          {errorMessage && (
            <div className="flex items-start gap-2.5 p-3 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-xs">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Lý do từ chối <span className="text-rose-500">*</span>
            </label>
            <textarea
              rows={3}
              required
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="VD: Dự án đang trong giai đoạn nước rút cần tập trung toàn bộ nguồn lực..."
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-rose-500/20 focus:border-rose-500 outline-none transition resize-none placeholder:text-slate-400"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center gap-2 px-5 py-2 text-xs font-semibold text-white bg-rose-600 hover:bg-rose-700 rounded-xl shadow-xs transition disabled:opacity-50"
            >
              {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
              Xác nhận từ chối
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}