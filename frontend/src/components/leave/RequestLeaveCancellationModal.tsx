"use client";

import { useState } from "react";
import { X, AlertTriangle, Send, Calendar } from "lucide-react";
import { requestCancelLeaveRequest } from "@/lib/api/leave";
import type { LeaveRequest } from "./LeaveManagementView";

interface RequestLeaveCancellationModalProps {
  request: LeaveRequest;
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export default function RequestLeaveCancellationModal({
  request,
  onClose,
  onSuccess,
}: RequestLeaveCancellationModalProps) {
  const [reason, setReason] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reason.trim()) {
      setErrorMessage("Vui lòng nhập lý do yêu cầu hủy đơn nghỉ phép.");
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMessage(null);
      await requestCancelLeaveRequest(request.id, reason.trim());
      onSuccess("Đã gửi yêu cầu hủy đơn nghỉ phép. Yêu cầu đang chờ Quản lý nguồn lực phê duyệt.");
      onClose();
    } catch (err: any) {
      setErrorMessage(err.message || "Không thể gửi yêu cầu hủy đơn lúc này.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4" role="dialog" aria-modal="true">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <Calendar className="size-5 text-indigo-600" />
            <h3 className="text-base font-bold text-slate-900">
              Yêu cầu hủy đơn nghỉ phép ({request.id})
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer"
          >
            <X className="size-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4 text-xs">
          {/* Tóm tắt thông tin đơn */}
          <div className="rounded-xl border border-slate-100 bg-slate-50/80 p-3.5 space-y-1">
            <div className="flex justify-between">
              <span className="text-slate-500 font-semibold">Khoảng thời gian:</span>
              <span className="font-bold text-slate-900">{request.startDate} → {request.endDate}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500 font-semibold">Số ngày nghỉ:</span>
              <span className="font-bold text-slate-900">{request.daysCount} ngày</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500 font-semibold">Lý do nghỉ ban đầu:</span>
              <span className="font-semibold text-slate-700 truncate max-w-[200px]" title={request.reason}>{request.reason}</span>
            </div>
          </div>

          <div className="rounded-xl border border-blue-200 bg-blue-50/80 p-3 text-blue-900">
            <p className="text-[11px] leading-relaxed">
              <strong>Lưu ý:</strong> Khi bạn gửi yêu cầu hủy, đơn sẽ chuyển sang trạng thái <em>Chờ duyệt hủy</em>. Sau khi Quản lý nguồn lực phê duyệt, giờ làm việc và ngày phép năm sẽ được hoàn trả đầy đủ cho bạn.
            </p>
          </div>

          {errorMessage && (
            <div className="flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-rose-800">
              <AlertTriangle className="size-4 text-rose-600 shrink-0 mt-0.5" />
              <p className="font-semibold">{errorMessage}</p>
            </div>
          )}

          <div>
            <label className="block font-bold text-slate-700 mb-1.5">
              Lý do xin hủy đơn <span className="text-rose-500">*</span>:
            </label>
            <textarea
              required
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Vui lòng nhập lý do thay đổi kế hoạch..."
              className="w-full rounded-xl border border-slate-200 bg-slate-50 p-2.5 text-xs text-slate-900 focus:border-indigo-500 focus:bg-white focus:outline-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2 border-t border-slate-100 pt-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 transition cursor-pointer"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-xs hover:bg-indigo-700 transition cursor-pointer disabled:opacity-50"
            >
              <Send className="size-4" />
              <span>{isSubmitting ? "Đang gửi..." : "Gửi yêu cầu hủy"}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
