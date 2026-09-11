"use client";

import { useState, useEffect } from "react";
import { X, CheckCircle2, AlertTriangle, Clock, Briefcase } from "lucide-react";
import { type LeaveImpactDto, getLeaveImpact, approveLeaveRequest, rejectLeaveRequest } from "@/lib/api/leave";
import type { LeaveRequest } from "./LeaveManagementView";

interface LeaveApprovalModalProps {
  request: LeaveRequest;
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export default function LeaveApprovalModal({ request, onClose, onSuccess }: LeaveApprovalModalProps) {
  const [impact, setImpact] = useState<LeaveImpactDto | null>(null);
  const [isLoadingImpact, setIsLoadingImpact] = useState(true);
  const [comment, setComment] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    async function fetchImpact() {
      try {
        setIsLoadingImpact(true);
        const data = await getLeaveImpact(request.id);
        setImpact(data);
      } catch (err: any) {
        console.warn("Không thể tải thông tin tác động dự án:", err);
      } finally {
        setIsLoadingImpact(false);
      }
    }
    fetchImpact();
  }, [request.id]);

  const handleApprove = async () => {
    try {
      setIsProcessing(true);
      setErrorMessage(null);
      await approveLeaveRequest(request.id, comment);
      onSuccess("Đã phê duyệt đơn nghỉ phép thành công. Giờ khả dụng tuần đã được cập nhật.");
      onClose();
    } catch (err: any) {
      setErrorMessage(err.message || "Không thể phê duyệt đơn nghỉ phép lúc này.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!comment.trim()) {
      setErrorMessage("Vui lòng nhập lý do từ chối đơn nghỉ phép (bắt buộc).");
      return;
    }
    try {
      setIsProcessing(true);
      setErrorMessage(null);
      await rejectLeaveRequest(request.id, comment.trim());
      onSuccess("Đã từ chối đơn nghỉ phép.");
      onClose();
    } catch (err: any) {
      setErrorMessage(err.message || "Không thể từ chối đơn nghỉ phép lúc này.");
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4" role="dialog" aria-modal="true">
      <div className="w-full max-w-2xl rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <Clock className="size-5 text-indigo-600" />
            <h3 className="text-base font-bold text-slate-900">
              Xem xét &amp; Phê duyệt đơn nghỉ phép ({request.id})
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
                {request.startDate} → {request.endDate} ({request.daysCount} ngày)
              </p>
              <p className="text-[11px] text-slate-500">Lý do: {request.reason || "Không có lý do cụ thể"}</p>
            </div>
          </div>

          {/* TC-02: Khối cảnh báo tác động tới dự án trong tuần nghỉ */}
          {isLoadingImpact ? (
            <div className="rounded-xl border border-slate-100 p-4 text-center text-slate-400">
              Đang phân tích tác động tới dự án trong tuần nghỉ...
            </div>
          ) : impact?.hasConflict ? (
            <div className="rounded-xl border border-amber-300 bg-amber-50 p-3.5 text-amber-900">
              <div className="flex items-start gap-2.5">
                <AlertTriangle className="size-5 text-amber-600 shrink-0 mt-0.5" />
                <div className="space-y-1.5 flex-1">
                  <div className="font-bold text-amber-950">
                    Cảnh báo ảnh hưởng dự án: Nhân sự đang được phân bổ{" "}
                    <span className="text-rose-700 underline font-extrabold">
                      {impact.totalAllocatedHoursInLeavePeriod} giờ
                    </span>{" "}
                    vào các dự án trong tuần nghỉ!
                  </div>
                  <p className="text-[11px] text-amber-800">
                    Việc phê duyệt đơn này có thể làm giảm tiến độ các dự án sau. Vui lòng cân đối người thay thế trước khi duyệt.
                  </p>
                  
                  {/* Bảng chi tiết các dự án bị ảnh hưởng */}
                  <div className="mt-2 overflow-x-auto rounded-lg border border-amber-200 bg-white">
                    <table className="w-full text-left text-[11px]">
                      <thead className="border-b border-amber-100 bg-amber-50/60 font-bold text-amber-900">
                        <tr>
                          <th className="px-3 py-1.5">Tên dự án</th>
                          <th className="px-3 py-1.5">Tuần</th>
                          <th className="px-3 py-1.5 text-right">Giờ phân bổ</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-amber-50">
                        {impact.affectedProjects.map((p, idx) => (
                          <tr key={idx}>
                            <td className="px-3 py-1.5 font-medium text-slate-800 flex items-center gap-1.5">
                              <Briefcase className="size-3 text-slate-400" />
                              <span>{p.projectName}</span>
                            </td>
                            <td className="px-3 py-1.5 text-slate-600">
                              Tuần {p.weekNumber} / {p.year}
                            </td>
                            <td className="px-3 py-1.5 text-right font-bold text-amber-900">
                              {p.allocatedHours} giờ
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-3 text-emerald-800 flex items-center gap-2">
              <CheckCircle2 className="size-4 text-emerald-600 shrink-0" />
              <span>
                Không có xung đột phân bổ dự án trong khoảng thời gian nghỉ phép này (nhân sự chưa được phân bổ giờ vào dự án nào trong tuần).
              </span>
            </div>
          )}

          {/* Ô nhập ý kiến phê duyệt / lý do từ chối */}
          <div>
            <label className="block font-bold text-slate-700 mb-1">
              Ý kiến phê duyệt / Lý do từ chối <span className="text-slate-400 font-normal">(bắt buộc nếu từ chối)</span>
            </label>
            <textarea
              rows={3}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Nhập ghi chú gửi cho nhân viên hoặc nêu rõ lý do nếu từ chối đơn..."
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs text-slate-800 focus:border-indigo-500 focus:outline-none"
            />
          </div>

          {errorMessage && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-2.5 text-[11px] font-bold text-rose-700">
              {errorMessage}
            </div>
          )}

          {/* Footer nút bấm */}
          <div className="flex justify-end gap-2 border-t border-slate-100 pt-3">
            <button
              type="button"
              disabled={isProcessing}
              onClick={onClose}
              className="rounded-xl border border-slate-200 px-4 py-2 font-semibold text-slate-600 hover:bg-slate-50 transition cursor-pointer"
            >
              Đóng
            </button>
            <button
              type="button"
              disabled={isProcessing}
              onClick={handleReject}
              className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-2 font-bold text-rose-700 hover:bg-rose-100 transition cursor-pointer"
            >
              {isProcessing ? "Đang xử lý..." : "Từ chối"}
            </button>
            <button
              type="button"
              disabled={isProcessing}
              onClick={handleApprove}
              className="rounded-xl bg-indigo-600 px-4 py-2 font-bold text-white shadow-xs hover:bg-indigo-700 transition cursor-pointer"
            >
              {isProcessing ? "Đang xử lý..." : "Duyệt đơn"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
