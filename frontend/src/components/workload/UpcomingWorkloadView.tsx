"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import {
  CalendarRange,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Briefcase,
  ChevronLeft,
  ChevronRight,
  TrendingUp,
  Info,
  Layers,
  Sparkles,
  ArrowUpRight,
  ShieldAlert,
} from "lucide-react";
import {
  getMyUpcomingWorkload,
  getEmployeeUpcomingWorkload,
  type UpcomingWorkloadResult,
  type WeeklyWorkloadItemResult,
} from "@/lib/api/workload";
import { getCurrentIsoWeek, getIsoWeekDateRange } from "@/components/availability/availability.types";
import { useAuthUser } from "@/lib/auth-session";
import { cn } from "@/lib/utils";
import {
  getISOWeeksInYear,
  isSpecialistRole,
} from "./workloadUtils";

function formatDateShort(dateStr?: string): string {
  if (!dateStr) return "";
  const parts = dateStr.split("-");
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}`;
  }
  return dateStr;
}

interface UpcomingWorkloadViewProps {
  employeeId?: number;
  onNavigateToProjects?: () => void;
  onNavigateToLeave?: () => void;
}

export default function UpcomingWorkloadView({
  employeeId,
  onNavigateToProjects,
  onNavigateToLeave,
}: UpcomingWorkloadViewProps) {
  const user = useAuthUser();
  const isSpecialist = isSpecialistRole(user?.roleCode);

  const [data, setData] = useState<UpcomingWorkloadResult | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Pagination / Week offset
  const [startYear, setStartYear] = useState<number | undefined>(undefined);
  const [startWeek, setStartWeek] = useState<number | undefined>(undefined);
  const [selectedWeekIndex, setSelectedWeekIndex] = useState<number>(0);

  const fetchWorkload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let res: UpcomingWorkloadResult;
      if (employeeId) {
        res = await getEmployeeUpcomingWorkload(employeeId, startYear, startWeek, 8);
      } else {
        res = await getMyUpcomingWorkload(startYear, startWeek, 8);
      }
      setData(res);
      // Giữ tuần đang chọn trong phạm vi hợp lệ
      setSelectedWeekIndex((prev) => (prev >= res.weeklyWorkloads.length ? 0 : prev));
    } catch (err: unknown) {
      console.error("Lỗi nạp dữ liệu khối lượng công việc:", err);
      const msg = err instanceof Error ? err.message : "Không thể tải khối lượng công việc";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [employeeId, startYear, startWeek]);

  useEffect(() => {
    fetchWorkload();
  }, [fetchWorkload]);

  const handleResetToCurrent = () => {
    setStartYear(undefined);
    setStartWeek(undefined);
  };

  const handlePrevWeek = () => {
    if (!data || data.weeklyWorkloads.length === 0) return;
    const currentFirstWeek = data.weeklyWorkloads[0];
    let prevW = currentFirstWeek.weekNumber - 1;
    let prevY = currentFirstWeek.year;
    if (prevW < 1) {
      prevY -= 1;
      prevW = getISOWeeksInYear(prevY);
    }
    setStartYear(prevY);
    setStartWeek(prevW);
  };

  const handleNextWeek = () => {
    if (!data || data.weeklyWorkloads.length === 0) return;
    const currentFirstWeek = data.weeklyWorkloads[0];
    let nextW = currentFirstWeek.weekNumber + 1;
    let nextY = currentFirstWeek.year;
    const maxWeeks = getISOWeeksInYear(nextY);
    if (nextW > maxWeeks) {
      nextY += 1;
      nextW = 1;
    }
    setStartYear(nextY);
    setStartWeek(nextW);
  };

  const handleDateChange = (dateStr: string) => {
    if (!dateStr) return;
    const [y, m, d] = dateStr.split("-").map(Number);
    const iso = getCurrentIsoWeek(new Date(y, m - 1, d));
    setStartYear(iso.year);
    setStartWeek(iso.weekNumber);
  };

  const datePickerValue = useMemo(() => {
    if (data?.weeklyWorkloads && data.weeklyWorkloads.length > 0) {
      return data.weeklyWorkloads[0].startDate;
    }
    const iso = getCurrentIsoWeek();
    const { startDate } = getIsoWeekDateRange(startYear ?? iso.year, startWeek ?? iso.weekNumber);
    const y = startDate.getUTCFullYear();
    const m = String(startDate.getUTCMonth() + 1).padStart(2, "0");
    const d = String(startDate.getUTCDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
  }, [data, startYear, startWeek]);

  const selectedWeek: WeeklyWorkloadItemResult | undefined =
    data?.weeklyWorkloads[selectedWeekIndex];

  const overloadThreshold = data?.effectiveOverloadThreshold ?? 100;
  const idleThreshold = data?.effectiveIdleThreshold ?? 70;

  if (user && !isSpecialist) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center p-8 bg-white rounded-3xl border border-slate-200 shadow-xs animate-in fade-in duration-150">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-50 text-amber-600 mb-4 border border-amber-100">
          <ShieldAlert className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-slate-900 mb-1">
          Chức năng dành riêng cho Nhân viên chuyên môn
        </h3>
        <p className="text-xs text-slate-500 max-w-md mb-6 leading-relaxed">
          Chức năng "Xem khối lượng công việc sắp tới" chỉ áp dụng cho vai trò Nhân viên chuyên môn để theo dõi mức bận và chủ động phân bổ công việc.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {/* Header & Controls */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-4">
        <div>
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-sky-100 text-sky-700">
              <CalendarRange className="h-4 w-4" />
            </div>
            <h1 className="text-xl font-bold tracking-tight text-slate-900">
              Khối Lượng Công Việc Sắp Tới
            </h1>
            <span className="inline-flex items-center gap-1 rounded-full bg-sky-50 border border-sky-200 px-2.5 py-0.5 text-xs font-semibold text-sky-800">
              <Sparkles className="h-3 w-3 text-sky-600" /> 8 tuần mô phỏng
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Theo dõi mức bận và dự báo sử dụng năng lực của cá nhân để nhận biết quá tải sớm và chủ động phân bổ thời gian.
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Week navigation buttons */}
          <div className="flex items-center rounded-xl border border-slate-200 bg-white p-1 shadow-xs">
            <button
              type="button"
              onClick={handlePrevWeek}
              disabled={loading}
              className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition disabled:opacity-40 cursor-pointer"
              title="Tuần trước"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={handleResetToCurrent}
              disabled={loading}
              className="px-3 py-1 text-xs font-bold text-slate-700 hover:text-sky-600 transition disabled:opacity-50 cursor-pointer"
            >
              {startYear === undefined && startWeek === undefined ? "Tuần này" : "Tuần hiện tại"}
            </button>
            <button
              type="button"
              onClick={handleNextWeek}
              disabled={loading}
              className="rounded-lg p-1.5 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition disabled:opacity-40 cursor-pointer"
              title="Tuần sau"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>

          {/* Date Picker */}
          <div className="relative">
            <input
              type="date"
              value={datePickerValue}
              onChange={(e) => handleDateChange(e.target.value)}
              className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 shadow-xs focus:border-sky-500 focus:outline-hidden cursor-pointer"
            />
          </div>

          <button
            type="button"
            onClick={fetchWorkload}
            disabled={loading}
            className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-xs disabled:opacity-50 cursor-pointer"
          >
            <RefreshCw className={cn("h-3.5 w-3.5", loading && "animate-spin text-sky-600")} />
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800 flex items-start gap-3">
          <ShieldAlert className="h-5 w-5 text-rose-600 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold">Không thể tải dữ liệu khối lượng công việc</p>
            <p className="text-xs text-rose-700 mt-0.5">{error}</p>
            <button
              type="button"
              onClick={fetchWorkload}
              className="mt-2 text-xs font-semibold text-rose-900 underline hover:no-underline"
            >
              Thử lại ngay
            </button>
          </div>
        </div>
      )}

      {/* Summary KPI Cards */}
      {data && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {/* Card 1: Tổng giờ phân bổ */}
          <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Tổng giờ phân bổ (8 tuần)
              </span>
              <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-600">
                <Briefcase className="h-3.5 w-3.5" />
              </div>
            </div>
            <div className="mt-1.5 flex items-baseline gap-1.5">
              <span className="text-2xl font-extrabold text-slate-900">
                {data.summary.totalAllocatedHours}
              </span>
              <span className="text-xs font-medium text-slate-500">
                / {data.summary.totalNetAvailableHours}h khả dụng
              </span>
            </div>
            <p className="mt-1 text-[11px] text-slate-500">
              Chuẩn: {data.summary.totalStandardHours}h · Nghỉ lễ: {data.summary.totalHolidayHours}h · Nghỉ phép: {data.summary.totalApprovedLeaveHours}h
            </p>
          </div>

          {/* Card 2: Tỷ lệ tải trung bình */}
          <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Mức sử dụng TB
              </span>
              <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-sky-50 border border-sky-100 text-sky-600">
                <TrendingUp className="h-3.5 w-3.5" />
              </div>
            </div>
            <div className="mt-1.5 flex items-baseline gap-1.5">
              <span className={cn(
                "text-2xl font-extrabold",
                data.summary.averageUtilizationPercentage > overloadThreshold
                  ? "text-rose-600"
                  : data.summary.averageUtilizationPercentage < idleThreshold
                  ? "text-amber-600"
                  : "text-emerald-600"
              )}>
                {data.summary.averageUtilizationPercentage}%
              </span>
              <span className="text-xs font-medium text-slate-500">năng lực</span>
            </div>
            <p className="mt-1 text-[11px] text-slate-500">
              Cao nhất: <strong className="text-slate-800">{data.summary.maxUtilizationPercentage}%</strong> ({data.summary.maxUtilizationWeek})
            </p>
          </div>

          {/* Card 3: Cảnh báo quá tải */}
          <div className={cn(
            "rounded-xl border p-3.5 shadow-2xs transition",
            data.summary.overloadedWeeksCount > 0
              ? "border-rose-300 bg-rose-50/50"
              : "border-slate-200 bg-white"
          )}>
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Tuần Quá Tải (&gt;{overloadThreshold}%)
              </span>
              <div className={cn(
                "flex h-7 w-7 items-center justify-center rounded-lg border",
                data.summary.overloadedWeeksCount > 0
                  ? "bg-rose-100 border-rose-200 text-rose-700"
                  : "bg-slate-50 border-slate-200 text-slate-500"
              )}>
                <AlertTriangle className="h-3.5 w-3.5" />
              </div>
            </div>
            <div className="mt-1.5 flex items-baseline gap-1.5">
              <span className={cn(
                "text-2xl font-extrabold",
                data.summary.overloadedWeeksCount > 0 ? "text-rose-700" : "text-slate-900"
              )}>
                {data.summary.overloadedWeeksCount}
              </span>
              <span className="text-xs font-medium text-slate-500">/ 8 tuần</span>
            </div>
            <p className="mt-1 text-[11px] text-slate-500">
              {data.summary.overloadedWeeksCount > 0 ? (
                <span className="font-semibold text-rose-700">Cần lưu ý sắp xếp hoặc báo cấp trên</span>
              ) : (
                <span className="text-emerald-700">Không có tuần nào bị quá tải</span>
              )}
            </p>
          </div>

          {/* Card 4: Trạng thái cân bằng */}
          <div className="rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                Phân bố mức tải
              </span>
              <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-600">
                <CheckCircle2 className="h-3.5 w-3.5" />
              </div>
            </div>
            <div className="mt-1.5 flex items-center gap-3">
              <div>
                <span className="text-lg font-bold text-emerald-700">{data.summary.normalWeeksCount}</span>
                <p className="text-[10px] text-slate-500">Cân bằng</p>
              </div>
              <div className="h-6 w-px bg-slate-200" />
              <div>
                <span className="text-lg font-bold text-amber-600">{data.summary.idleWeeksCount}</span>
                <p className="text-[10px] text-slate-500">Nhàn rỗi</p>
              </div>
              <div className="h-6 w-px bg-slate-200" />
              <div>
                <span className="text-lg font-bold text-rose-600">{data.summary.overloadedWeeksCount}</span>
                <p className="text-[10px] text-slate-500">Quá tải</p>
              </div>
            </div>
            <p className="mt-1 text-[11px] text-slate-400">
              Ngưỡng: Nhàn rỗi &lt;{idleThreshold}% · Quá tải &gt;{overloadThreshold}%
            </p>
          </div>
        </div>
      )}

      {/* Main 8-Week Workload Interactive Chart */}
      {data && (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Layers className="h-4 w-4 text-sky-600" />
                Biểu đồ mức bận theo tuần
              </h2>
              <p className="text-xs text-slate-500">
                Nhấp vào từng cột tuần để xem danh sách phân bổ dự án và thời gian chi tiết bên dưới.
              </p>
            </div>

            {/* Legend */}
            <div className="flex flex-wrap items-center gap-3 text-xs">
              <div className="flex items-center gap-1.5">
                <span className="h-3 w-3 rounded-full bg-rose-500" />
                <span className="text-slate-600">Quá tải (&gt;{overloadThreshold}%)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="h-3 w-3 rounded-full bg-emerald-500" />
                <span className="text-slate-600">Cân bằng ({idleThreshold}% - {overloadThreshold}%)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="h-3 w-3 rounded-full bg-amber-400" />
                <span className="text-slate-600">Nhàn rỗi / Thấp (&lt;{idleThreshold}%)</span>
              </div>
            </div>
          </div>

          {/* Chart Container */}
          <div className="pt-6 pb-2">
            <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-2.5 items-end">
              {data.weeklyWorkloads.map((week, idx) => {
                const isSelected = idx === selectedWeekIndex;
                const isOverloaded = week.status === "OVERLOADED" || week.utilizationPercentage > overloadThreshold;
                const isIdle = week.status === "IDLE" || week.utilizationPercentage < idleThreshold;

                // Chiều cao biểu đồ tương đối (max 150%)
                const barHeightPercent = Math.min(Math.max((week.utilizationPercentage / 150) * 100, 10), 100);

                let barColorClass = "bg-emerald-500 hover:bg-emerald-600";
                let badgeClass = "bg-emerald-50 text-emerald-700 border-emerald-200";

                if (isOverloaded) {
                  barColorClass = "bg-rose-500 hover:bg-rose-600";
                  badgeClass = "bg-rose-50 text-rose-700 border-rose-200";
                } else if (isIdle) {
                  barColorClass = "bg-amber-400 hover:bg-amber-500";
                  badgeClass = "bg-amber-50 text-amber-700 border-amber-200";
                }

                return (
                  <div
                    key={`${week.year}-${week.weekNumber}`}
                    onClick={() => setSelectedWeekIndex(idx)}
                    className={cn(
                      "group relative flex flex-col items-center p-3 rounded-xl border transition-all cursor-pointer",
                      isSelected
                        ? "border-sky-500 bg-sky-50/40 shadow-xs ring-2 ring-sky-300/50"
                        : "border-slate-200 bg-slate-50/50 hover:bg-white hover:border-slate-300"
                    )}
                  >
                    {/* Top status badge */}
                    <div className="mb-2 text-center w-full">
                      <span className={cn("inline-block truncate max-w-full text-[10px] font-bold px-1.5 py-0.5 rounded-md border", badgeClass)}>
                        {week.utilizationPercentage}%
                      </span>
                    </div>

                    {/* Bar visualization */}
                    <div className="w-full h-36 flex items-end justify-center px-2 py-1 bg-slate-100/70 rounded-lg relative overflow-hidden">
                      {/* Overload threshold guideline */}
                      <div
                        className="absolute w-full border-t border-dashed border-rose-400 left-0 z-10 pointer-events-none opacity-60"
                        style={{ bottom: `${Math.min((overloadThreshold / 150) * 100, 100)}%` }}
                        title={`Ngưỡng quá tải ${overloadThreshold}%`}
                      />
                      {/* Idle threshold guideline */}
                      <div
                        className="absolute w-full border-t border-dashed border-amber-400 left-0 z-10 pointer-events-none opacity-60"
                        style={{ bottom: `${(idleThreshold / 150) * 100}%` }}
                        title={`Ngưỡng nhàn rỗi ${idleThreshold}%`}
                      />

                      <div
                        className={cn(
                          "w-full rounded-t-md transition-all duration-300 group-hover:opacity-90 shadow-2xs",
                          barColorClass
                        )}
                        style={{ height: `${barHeightPercent}%` }}
                      />
                    </div>

                    {/* Bottom week label with date range */}
                    <div className="mt-2.5 text-center w-full">
                      <p className="text-xs font-bold text-slate-800">{week.weekLabel}</p>
                      {week.startDate && week.endDate && (
                        <p className="text-[10px] font-medium text-slate-400 mt-0.5 whitespace-nowrap">
                          {formatDateShort(week.startDate)} - {formatDateShort(week.endDate)}
                        </p>
                      )}
                      <p className="text-[10px] text-slate-500 mt-0.5">
                        {week.totalAllocatedHours}h / {week.netAvailableHours}h
                      </p>
                      {isOverloaded && (
                        <p className="text-[9px] font-bold text-rose-600 mt-0.5">
                          +{week.overloadHours}h vượt
                        </p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Selected Week Detailed Project Allocation Breakdown */}
      {selectedWeek && (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-3">
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-slate-900">
                  Chi tiết phân bổ: {selectedWeek.weekLabel}
                </h3>
                <span className="text-xs text-slate-500">
                  ({selectedWeek.startDate} → {selectedWeek.endDate})
                </span>
                <span className={cn(
                  "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-bold border",
                  selectedWeek.status === "OVERLOADED"
                    ? "bg-rose-50 border-rose-200 text-rose-700"
                    : selectedWeek.status === "IDLE"
                    ? "bg-amber-50 border-amber-200 text-amber-700"
                    : "bg-emerald-50 border-emerald-200 text-emerald-700"
                )}>
                  {selectedWeek.status === "OVERLOADED" && <AlertTriangle className="h-3 w-3" />}
                  {selectedWeek.status === "NORMAL" && <CheckCircle2 className="h-3 w-3" />}
                  {selectedWeek.status === "IDLE" && <Clock className="h-3 w-3" />}
                  {selectedWeek.status === "OVERLOADED"
                    ? `Quá tải ${selectedWeek.utilizationPercentage}% (+${selectedWeek.overloadHours}h)`
                    : selectedWeek.status === "IDLE"
                    ? `Nhàn rỗi ${selectedWeek.utilizationPercentage}%`
                    : `Cân bằng ${selectedWeek.utilizationPercentage}%`}
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                Giờ chuẩn: <strong>{selectedWeek.standardHours}h</strong> | Nghỉ lễ: <strong>{selectedWeek.holidayHours}h</strong> | Nghỉ phép: <strong>{selectedWeek.approvedLeaveHours}h</strong> | Giờ khả dụng ròng: <strong>{selectedWeek.netAvailableHours}h</strong>
              </p>
            </div>

            <div className="flex items-center gap-2">
              {onNavigateToLeave && (
                <button
                  type="button"
                  onClick={onNavigateToLeave}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-slate-600 hover:text-slate-900 transition cursor-pointer"
                >
                  <span>Nghỉ phép</span>
                  <ArrowUpRight className="h-3 w-3" />
                </button>
              )}
              {onNavigateToProjects && (
                <button
                  type="button"
                  onClick={onNavigateToProjects}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-sky-600 hover:text-sky-700 transition cursor-pointer"
                >
                  <span>Dự án</span>
                  <ArrowUpRight className="h-3 w-3" />
                </button>
              )}
            </div>
          </div>

          {/* Project List */}
          {selectedWeek.projectAllocations.length === 0 ? (
            <div className="py-8 text-center text-slate-500 bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
              <Info className="h-8 w-8 text-slate-400 mx-auto mb-1.5" />
              <p className="text-sm font-semibold text-slate-700">Chưa có dự án nào được phân bổ trong tuần này</p>
              <p className="text-xs text-slate-400 mt-0.5">Bạn đang có {selectedWeek.netAvailableHours} giờ khả dụng sẵn sàng nhận thêm việc.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50/70 text-slate-500 uppercase tracking-wider font-semibold">
                    <th className="py-2.5 px-3">Mã & Tên Dự Án</th>
                    <th className="py-2.5 px-3">Vai Trò Đảm Nhận</th>
                    <th className="py-2.5 px-3 text-right">Số Giờ Phân Bổ</th>
                    <th className="py-2.5 px-3 text-right">Tỷ Lệ Tải</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {selectedWeek.projectAllocations.map((proj) => (
                    <tr key={proj.projectId} className="hover:bg-slate-50/60 transition">
                      <td className="py-2.5 px-3">
                        <div className="font-bold text-slate-900">{proj.projectName}</div>
                        <div className="text-[11px] text-slate-400 font-mono">{proj.projectCode}</div>
                      </td>
                      <td className="py-2.5 px-3">
                        {proj.projectRoleName ? (
                          <span className="inline-flex items-center rounded-md bg-indigo-50 px-2 py-0.5 text-[11px] font-semibold text-indigo-700 border border-indigo-100">
                            {proj.projectRoleName}
                          </span>
                        ) : (
                          <span className="text-slate-400 italic">Thành viên</span>
                        )}
                      </td>
                      <td className="py-2.5 px-3 text-right font-bold text-slate-800">
                        {proj.allocatedHours} giờ
                      </td>
                      <td className="py-2.5 px-3 text-right">
                        <span className="inline-block font-semibold text-sky-700 bg-sky-50 px-2 py-0.5 rounded-md border border-sky-100 text-[11px]">
                          {proj.allocationPercentage}%
                        </span>
                      </td>
                    </tr>
                  ))}
                  {/* Total row */}
                  <tr className="bg-slate-50 font-bold border-t border-slate-200">
                    <td colSpan={2} className="py-2.5 px-3 text-slate-700">
                      Tổng cộng phân bổ tuần {selectedWeek.weekLabel}
                    </td>
                    <td className="py-2.5 px-3 text-right text-indigo-700">
                      {selectedWeek.totalAllocatedHours} giờ
                    </td>
                    <td className="py-2.5 px-3 text-right text-sky-800">
                      {selectedWeek.utilizationPercentage}%
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
