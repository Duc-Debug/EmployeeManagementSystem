"use client";

import { useState } from "react";
import { X, CheckCircle2, AlertTriangle, Clock, RefreshCw, XCircle } from "lucide-react";
import { approveCancelLeaveRequest, rejectCancelLeaveRequest } from "@/lib/api/leave";
import type { LeaveRequest } from "./LeaveManagementView";

interface LeaveCancellationReviewModalProps {
  request: LeaveRequest;
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export default function LeaveCancellationReviewModal({
  request,
  onClose,
  onSuccess,
}: LeaveCancellationReviewModalProps) {
  const [comment, setComment] = useState("");
  const [rejectReason, setRejectReason] = useState("");
  const [isRejectingMode, setIsRejectingMode] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleApprove = async () => {
    try {
      setIsProcessing(true);
      setErrorMessage(null);
      await approveCancelLeaveRequest(request.id, comment);
      onSuccess("Đã duyệt hủy đơn nghỉ phép thành công. Giờ khả dụng đã được hoàn trả lại cho kế hoạch tuần.");
      onClose();
    } catch (err: any) {
      setErrorMessage(err.message || "Không thể duyệt hủy đơn nghỉ phép lúc này.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!rejectReason.trim()) {
      setErrorMessage("Vui lòng nhập lý do từ chối yêu cầu hủy (bắt buộc).");
      return;
    }
    try {
      setIsProcessing(true);
      setErrorMessage(null);
      await rejectCancelLeaveRequest(request.id, rejectReason.trim());
      onSuccess("Đã từ chối yêu cầu hủy đơn nghỉ phép. Đơn tiếp tục duy trì trạng thái Đã duyệt.");
      onClose();
    } catch (err: any) {
      setErrorMessage(err.message || "Không thể từ chối yêu cầu hủy đơn lúc này.");
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4" role="dialog" aria-modal="true">
      <div className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <RefreshCw className="size-5 text-amber-600" />
            <h3 className="text-base font-bold text-slate-900">
              Xem xét yêu cầu hủy đơn nghỉ phép ({request.id})
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

        <div className="mt-4 space-y-4 text-xs">
          {/* Thông tin người làm đơn */}
          <div className="grid grid-cols-2 gap-3 rounded-xl border border-slate-100 bg-slate-50/80 p-3.5">
            <div>
              <span className="text-[11px] font-semibold text-slate-400">Nhân viên:</span>
              <p className="text-sm font-bold text-slate-900">{request.employeeName}</p>
              <p className="text-[11px] text-slate-500">{request.department}</p>
            </div>
            <div>
              <span className="text-[11px] font-semibold text-slate-400">Thời gian xin nghỉ:</span>
              <p className="font-bold text-slate-900">
                {request.startDate} → {request.endDate}
              </p>
              <p className="text-[11px] text-slate-600 font-semibold">{request.daysCount} ngày làm việc</p>
            </div>
          </div>

          {/* Lý do xin hủy của nhân viên */}
          <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-3.5 text-amber-950">
            <div className="flex items-center gap-1.5 font-bold text-amber-900 mb-1">
              <AlertTriangle className="size-4 text-amber-600 shrink-0" />
              <span>Lý do nhân viên yêu cầu hủy đơn:</span>
            </div>
            <p className="text-xs font-semibold pl-5">
              {request.cancellationReason || "Không có lý do chi tiết"}
            </p>
          </div>

          {/* Thông tin hoàn trả giờ */}
          <div className="rounded-xl border border-blue-200 bg-blue-50/80 p-3.5 text-blue-950">
            <div className="flex items-center gap-1.5 font-bold text-blue-900 mb-1">
              <Clock className="size-4 text-blue-600 shrink-0" />
              <span>Khôi phục kế hoạch nguồn lực:</span>
            </div>
            <p className="text-xs text-blue-800 pl-5">
              Khi bạn phê duyệt hủy đơn, hệ thống sẽ tự động <strong>cộng lại số giờ khả dụng</strong> vào kế hoạch của các tuần liên quan và <strong>hoàn trả ngày phép năm</strong> cho nhân sự.
            </p>
          </div>

          {/* Hiển thị lỗi nếu có */}
          {errorMessage && (
            <div className="flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-rose-800">
              <AlertTriangle className="size-4 text-rose-600 shrink-0 mt-0.5" />
              <p className="font-semibold">{errorMessage}</p>
            </div>
          )}

          {/* Form nhập ghi chú / lý do từ chối */}
          {!isRejectingMode ? (
            <div>
              <label className="block font-bold text-slate-700 mb-1.5">
                Ghi chú phê duyệt (Tùy chọn):
              </label>
              <textarea
                rows={2}
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Nhập ghi chú cho nhân sự (nếu có)..."
                className="w-full rounded-xl border border-slate-200 bg-slate-50 p-2.5 text-xs text-slate-900 focus:border-indigo-500 focus:bg-white focus:outline-none"
              />
            </div>
          ) : (
            <div>
              <label className="block font-bold text-rose-700 mb-1.5">
                Lý do từ chối yêu cầu hủy <span className="text-rose-500">*</span>:
              </label>
              <textarea
                rows={2}
                required
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Nhập lý do không thể duyệt hủy đơn (bắt buộc)..."
                className="w-full rounded-xl border border-rose-300 bg-rose-50/30 p-2.5 text-xs text-slate-900 focus:border-rose-500 focus:bg-white focus:outline-none"
              />
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-3">
          {!isRejectingMode ? (
            <>
              <button
                type="button"
                onClick={() => setIsRejectingMode(true)}
                disabled={isProcessing}
                className="flex items-center gap-1.5 rounded-xl border border-slate-200 px-3 py-2 text-xs font-bold text-rose-600 hover:bg-rose-50 transition cursor-pointer"
              >
                <XCircle className="size-4 text-rose-600" />
                <span>Từ chối hủy</span>
              </button>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={onClose}
                  disabled={isProcessing}
                  className="rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 transition cursor-pointer"
                >
                  Đóng
                </button>
                <button
                  type="button"
                  onClick={handleApprove}
                  disabled={isProcessing}
                  className="flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow-xs hover:bg-emerald-700 transition cursor-pointer disabled:opacity-50"
                >
                  <CheckCircle2 className="size-4" />
                  <span>{isProcessing ? "Đang xử lý..." : "Duyệt hủy đơn"}</span>
                </button>
              </div>
            </>
          ) : (
            <>
              <button
                type="button"
                onClick={() => setIsRejectingMode(false)}
                disabled={isProcessing}
                className="rounded-xl border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 transition cursor-pointer"
              >
                Quay lại
              </button>

              <button
                type="button"
                onClick={handleReject}
                disabled={isProcessing}
                className="flex items-center gap-1.5 rounded-xl bg-rose-600 px-4 py-2 text-xs font-bold text-white shadow-xs hover:bg-rose-700 transition cursor-pointer disabled:opacity-50"
              >
                <XCircle className="size-4" />
                <span>{isProcessing ? "Đang xử lý..." : "Xác nhận từ chối hủy"}</span>
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
