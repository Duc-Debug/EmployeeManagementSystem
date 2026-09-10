import { Clock, CalendarDays, CalendarOff, CheckCircle2, AlertCircle } from "lucide-react";
import type { WeeklyAvailabilityResult } from "@/lib/api/availability";

interface CapacitySummaryCardProps {
  capacity?: WeeklyAvailabilityResult | null;
  employeeName?: string;
  weekLabel?: string;
}

export default function CapacitySummaryCard({
  capacity,
  employeeName,
  weekLabel,
}: CapacitySummaryCardProps) {
  if (!capacity) {
    return (
      <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-5 text-xs text-slate-500 shadow-2xs">
        <AlertCircle className="size-5 text-slate-400 shrink-0" />
        <span>Chưa có dữ liệu tính toán công suất cho tuần được chọn.</span>
      </div>
    );
  }

  const standard = capacity.standardHours || 0;
  const holiday = capacity.holidayHours || 0;
  const leave = Number(capacity.approvedLeaveHours) || 0;
  const net = Number(capacity.netAvailableHours) || 0;

  const netPercent = standard > 0 ? Math.max(0, Math.min(100, Math.round((net / standard) * 100))) : 0;

  return (
    <div className="rounded-2xl border border-slate-200/90 bg-white p-5 shadow-xs transition hover:border-slate-300">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between border-b border-slate-100 pb-3 mb-4">
        <div>
          <h4 className="text-sm font-bold text-slate-900">
            Năng lực khả dụng {employeeName ? `• ${employeeName}` : ""}
          </h4>
          <p className="text-[11px] font-medium text-slate-500">
            {weekLabel ? `${weekLabel} • ` : ""}Quy tắc QTN-10: Khả dụng = Giờ chuẩn - Giờ lễ - Giờ nghỉ phép đã duyệt
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-700 border border-emerald-100">
            <CheckCircle2 className="size-3.5" />
            {net}h khả dụng ({netPercent}%)
          </span>
        </div>
      </div>

      {/* 4 Cards Grid */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {/* Standard Hours */}
        <div className="rounded-xl border border-slate-100 bg-slate-50/70 p-3.5">
          <div className="flex items-center gap-2 text-slate-500 mb-1">
            <Clock className="size-4 text-indigo-600" />
            <span className="text-[11px] font-semibold">Giờ chuẩn tuần</span>
          </div>
          <p className="text-lg font-extrabold text-slate-900">{standard} <span className="text-xs font-normal text-slate-500">giờ</span></p>
          <span className="text-[10px] text-slate-400">Định mức khai báo</span>
        </div>

        {/* Holiday Hours */}
        <div className="rounded-xl border border-amber-100 bg-amber-50/50 p-3.5">
          <div className="flex items-center gap-2 text-amber-700 mb-1">
            <CalendarDays className="size-4 text-amber-600" />
            <span className="text-[11px] font-semibold">Nghỉ lễ trừ</span>
          </div>
          <p className="text-lg font-extrabold text-amber-700">-{holiday} <span className="text-xs font-normal text-amber-600">giờ</span></p>
          <span className="text-[10px] text-amber-600/80">Ngày lễ trong tuần</span>
        </div>

        {/* Approved Leave Hours */}
        <div className="rounded-xl border border-rose-100 bg-rose-50/50 p-3.5">
          <div className="flex items-center gap-2 text-rose-700 mb-1">
            <CalendarOff className="size-4 text-rose-600" />
            <span className="text-[11px] font-semibold">Nghỉ phép đã duyệt</span>
          </div>
          <p className="text-lg font-extrabold text-rose-700">-{leave} <span className="text-xs font-normal text-rose-600">giờ</span></p>
          <span className="text-[10px] text-rose-600/80">Không trừ đơn chờ duyệt</span>
        </div>

        {/* Net Available Hours */}
        <div className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-3.5">
          <div className="flex items-center gap-2 text-emerald-800 mb-1">
            <CheckCircle2 className="size-4 text-emerald-600" />
            <span className="text-[11px] font-bold">Khả dụng ròng</span>
          </div>
          <p className="text-lg font-extrabold text-emerald-800">{net} <span className="text-xs font-normal text-emerald-700">giờ</span></p>
          <span className="text-[10px] text-emerald-700/80">Sẵn sàng phân bổ</span>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="mt-4 pt-3 border-t border-slate-100">
        <div className="flex items-center justify-between text-[11px] font-medium text-slate-500 mb-1.5">
          <span>Tỷ lệ công suất sẵn sàng cho dự án</span>
          <span className="font-bold text-slate-700">{net} / {standard} giờ ({netPercent}%)</span>
        </div>
        <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
          <div
            className={`h-full transition-all duration-300 rounded-full ${
              netPercent > 60
                ? "bg-emerald-500"
                : netPercent > 30
                ? "bg-amber-500"
                : "bg-rose-500"
            }`}
            style={{ width: `${netPercent}%` }}
          />
        </div>
      </div>
    </div>
  );
}
