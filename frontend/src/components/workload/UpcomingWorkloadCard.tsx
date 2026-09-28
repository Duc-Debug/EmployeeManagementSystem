"use client";

import { useState, useEffect } from "react";
import {
  CalendarRange,
  AlertTriangle,
  CheckCircle2,
  ArrowUpRight,
  RefreshCw,
} from "lucide-react";
import {
  getMyUpcomingWorkload,
  type UpcomingWorkloadResult,
} from "@/lib/api/workload";
import { cn } from "@/lib/utils";

interface UpcomingWorkloadCardProps {
  onViewDetails: () => void;
}

export default function UpcomingWorkloadCard({ onViewDetails }: UpcomingWorkloadCardProps) {
  const [data, setData] = useState<UpcomingWorkloadResult | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await getMyUpcomingWorkload(undefined, undefined, 8);
      setData(res);
    } catch (err) {
      console.error("Lỗi nạp tóm tắt khối lượng công việc:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const overloadThreshold = data?.effectiveOverloadThreshold ?? 100;
  const idleThreshold = data?.effectiveIdleThreshold ?? 70;

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-sky-50 border border-sky-100 text-sky-600">
            <CalendarRange className="h-3.5 w-3.5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">
              Khối lượng công việc 8 tuần tới
            </h3>
            <p className="text-[11px] text-slate-400">
              Mức sử dụng năng lực dự kiến theo tuần
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={loadData}
            disabled={loading}
            className="p-1 text-slate-400 hover:text-slate-600 hover:bg-slate-50 rounded-md transition disabled:opacity-40"
            title="Làm mới"
          >
            <RefreshCw className={cn("h-3.5 w-3.5", loading && "animate-spin text-sky-600")} />
          </button>
          <button
            type="button"
            onClick={onViewDetails}
            className="inline-flex items-center gap-0.5 text-xs font-semibold text-sky-600 hover:text-sky-700 transition cursor-pointer"
          >
            <span>Chi tiết</span>
            <ArrowUpRight className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      {loading && !data ? (
        <div className="py-6 flex items-center justify-center text-xs text-slate-400">
          <RefreshCw className="h-4 w-4 animate-spin mr-1.5 text-sky-600" />
          Đang tính toán mức bận 8 tuần...
        </div>
      ) : data ? (
        <div className="space-y-3">
          {/* Mini 8-Bar chart */}
          <div className="grid grid-cols-8 gap-1.5 items-end pt-2 pb-1">
            {data.weeklyWorkloads.map((week) => {
              const isOverloaded = week.status === "OVERLOADED" || week.utilizationPercentage > overloadThreshold;
              const isIdle = week.status === "IDLE" || week.utilizationPercentage < idleThreshold;
              const heightPercent = Math.min(Math.max((week.utilizationPercentage / 140) * 100, 12), 100);

              let barBg = "bg-emerald-500";
              if (isOverloaded) barBg = "bg-rose-500";
              else if (isIdle) barBg = "bg-amber-400";

              return (
                <div
                  key={`${week.year}-${week.weekNumber}`}
                  onClick={onViewDetails}
                  className="group flex flex-col items-center cursor-pointer"
                  title={`${week.weekLabel}: ${week.utilizationPercentage}% (${week.totalAllocatedHours}h/${week.netAvailableHours}h)`}
                >
                  <div className="w-full h-16 flex items-end justify-center bg-slate-50 rounded p-0.5 relative overflow-hidden group-hover:bg-slate-100 transition">
                    <div
                      className={cn("w-full rounded-t transition-all", barBg)}
                      style={{ height: `${heightPercent}%` }}
                    />
                  </div>
                  <span className="text-[10px] font-bold text-slate-600 mt-1 truncate w-full text-center">
                    T{week.weekNumber}
                  </span>
                </div>
              );
            })}
          </div>

          {/* Quick Stats Footnote */}
          <div className="flex flex-wrap items-center justify-between text-xs pt-2 border-t border-slate-100 text-slate-500">
            <div className="flex items-center gap-2">
              <span>TB: <strong className="text-slate-800">{data.summary.averageUtilizationPercentage}%</strong></span>
              <span>·</span>
              <span>Tổng: <strong className="text-slate-800">{data.summary.totalAllocatedHours}h</strong> / {data.summary.totalNetAvailableHours}h</span>
            </div>
            {data.summary.overloadedWeeksCount > 0 ? (
              <span className="inline-flex items-center gap-1 font-bold text-rose-600">
                <AlertTriangle className="h-3 w-3" />
                {data.summary.overloadedWeeksCount} tuần quá tải
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 text-emerald-600 font-semibold">
                <CheckCircle2 className="h-3 w-3" />
                Tiến độ cân bằng
              </span>
            )}
          </div>
        </div>
      ) : null}
    </div>
  );
}
