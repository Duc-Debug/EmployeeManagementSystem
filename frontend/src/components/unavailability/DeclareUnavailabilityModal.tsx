"use client";

import { useState, useMemo } from "react";
import { X, AlertCircle, Clock, Loader2, Info } from "lucide-react";
import {
  submitUnavailability,
  type UnavailabilityReasonType,
  UNAVAILABILITY_REASON_LABELS,
  type UnavailabilityDeclarationResult,
} from "@/lib/api/unavailability";

interface DeclareUnavailabilityModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (result: UnavailabilityDeclarationResult) => void;
  employeeId: number;
  employeeName?: string;
}

export function countWorkingDays(startDateStr: string, endDateStr: string): number {
  if (!startDateStr || !endDateStr) return 0;
  const start = new Date(startDateStr);
  const end = new Date(endDateStr);
  if (end < start) return 0;

  let count = 0;
  const cur = new Date(start);
  while (cur <= end) {
    const day = cur.getDay(); // 0 is Sunday, 6 is Saturday
    if (day !== 0 && day !== 6) {
      count++;
    }
    cur.setDate(cur.getDate() + 1);
  }
  return count;
}

export default function DeclareUnavailabilityModal({
  isOpen,
  onClose,
  onSuccess,
  employeeId,
  employeeName,
}: DeclareUnavailabilityModalProps) {
  const todayStr = useMemo(() => {
    const d = new Date();
    return d.toISOString().split("T")[0];
  }, []);

  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");
  const [reasonType, setReasonType] = useState<UnavailabilityReasonType>("TRAINING");
  const [reasonDetail, setReasonDetail] = useState<string>("");
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const workingDays = useMemo(() => {
    return countWorkingDays(startDate, endDate);
  }, [startDate, endDate]);

  const estimatedHours = useMemo(() => {
    return workingDays * 8;
  }, [workingDays]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!startDate || !endDate) {
      setErrorMessage("Vui lòng chọn ngày bắt đầu và ngày kết thúc.");
      return;
    }

    if (startDate < todayStr) {
      setErrorMessage("Ngày bắt đầu không được trong quá khứ.");
      return;
    }

    if (startDate > endDate) {
      setErrorMessage("Ngày bắt đầu không được sau ngày kết thúc.");
      return;
    }

    if (workingDays === 0) {
      setErrorMessage("Khoảng thời gian bạn chọn không chứa ngày làm việc nào (rơi trọn vào cuối tuần).");
      return;
    }

    try {
      setIsSubmitting(true);
      const result = await submitUnavailability({
        employeeId,
        startDate,
        endDate,
        reasonType,
        reasonDetail: reasonDetail.trim() ? reasonDetail.trim() : undefined,
      });
      onSuccess(result);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Có lỗi xảy ra khi nộp khai báo.";
      setErrorMessage(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="relative w-full max-w-lg bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div>
            <h3 className="text-base font-bold text-slate-900">Khai báo thời gian không sẵn sàng</h3>
            <p className="text-xs text-slate-500 mt-0.5">
              {employeeName ? `Nhân sự: ${employeeName}` : "Đăng ký thời gian bận/không thể nhận phân bổ"}
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {errorMessage && (
            <div className="flex items-start gap-3 p-3.5 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl text-xs">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Date Range */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Ngày bắt đầu <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <input
                  type="date"
                  min={todayStr}
                  value={startDate}
                  onChange={(e) => {
                    setStartDate(e.target.value);
                    if (endDate && e.target.value > endDate) {
                      setEndDate(e.target.value);
                    }
                  }}
                  required
                  className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition bg-white"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Ngày kết thúc <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <input
                  type="date"
                  min={startDate || todayStr}
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  required
                  className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition bg-white"
                />
              </div>
            </div>
          </div>

          {/* Live Preview of working days & hours */}
          {startDate && endDate && (
            <div className="flex items-center justify-between p-3 bg-indigo-50/70 border border-indigo-100 rounded-xl text-xs text-indigo-900">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-indigo-600 shrink-0" />
                <span>
                  Số ngày làm việc: <strong className="font-semibold">{workingDays} ngày</strong>
                </span>
              </div>
              <span className="font-bold text-indigo-700 bg-indigo-100/80 px-2.5 py-1 rounded-lg">
                Khấu trừ: {estimatedHours}h
              </span>
            </div>
          )}

          {/* Reason Type */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Lý do không sẵn sàng <span className="text-rose-500">*</span>
            </label>
            <select
              value={reasonType}
              onChange={(e) => setReasonType(e.target.value as UnavailabilityReasonType)}
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition bg-white"
            >
              <option value="TRAINING">{UNAVAILABILITY_REASON_LABELS.TRAINING}</option>
              <option value="BUSINESS_TRIP">{UNAVAILABILITY_REASON_LABELS.BUSINESS_TRIP}</option>
              <option value="PERSONAL">{UNAVAILABILITY_REASON_LABELS.PERSONAL}</option>
              <option value="OTHER">{UNAVAILABILITY_REASON_LABELS.OTHER}</option>
            </select>
          </div>

          {/* Reason Detail */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Mô tả chi tiết / Nội dung công việc
            </label>
            <textarea
              rows={3}
              value={reasonDetail}
              onChange={(e) => setReasonDetail(e.target.value)}
              placeholder="VD: Tham gia khóa đào tạo chuyên sâu về kiến trúc hệ thống, đi công tác chi nhánh..."
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-xl focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 outline-none transition resize-none placeholder:text-slate-400"
            />
          </div>

          {/* Notice */}
          <div className="flex items-start gap-2 p-3 bg-slate-50 border border-slate-200 rounded-xl text-[11px] text-slate-500 leading-relaxed">
            <Info className="w-3.5 h-3.5 text-slate-400 shrink-0 mt-0.5" />
            <span>
              Đơn sau khi nộp sẽ ở trạng thái <strong>Chờ phê duyệt</strong>. Khi được Quản lý duyệt, số giờ khả dụng trong tuần sẽ được cập nhật tự động.
            </span>
          </div>

          {/* Footer actions */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center gap-2 px-5 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs transition disabled:opacity-50"
            >
              {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
              Nộp khai báo
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}