import React from "react";
import { Flag, CheckCircle2, AlertTriangle, Clock } from "lucide-react";
import type { MilestoneResult } from "@/lib/api/milestones";

interface MilestoneStatsSectionProps {
  milestones: MilestoneResult[];
}

export const MilestoneStatsSection: React.FC<MilestoneStatsSectionProps> = ({ milestones }) => {
  const total = milestones.length;
  const onTrack = milestones.filter((m) => m.status === "ON_TRACK").length;
  const delayed = milestones.filter((m) => m.status === "DELAYED").length;
  const completed = milestones.filter((m) => m.status === "COMPLETED").length;

  const maxDelayDays = delayed > 0 ? Math.max(...milestones.filter((m) => m.status === "DELAYED").map((m) => m.delayDays)) : 0;
  const completionRate = total > 0 ? Math.round((completed / total) * 100) : 0;

  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
      {/* 1. Tổng số mốc */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Tổng số mốc
          </span>
          <span className="rounded-lg bg-indigo-50 p-2 text-indigo-600">
            <Flag className="h-4 w-4" />
          </span>
        </div>
        <div className="mt-2 flex items-baseline gap-2">
          <span className="text-2xl font-bold text-slate-800">{total}</span>
          <span className="text-xs text-slate-500">mốc tiến độ</span>
        </div>
        <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
          <div
            className="h-1.5 rounded-full bg-indigo-600 transition-all duration-500"
            style={{ width: `${completionRate}%` }}
          />
        </div>
      </div>

      {/* 2. Đúng tiến độ */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Đúng tiến độ
          </span>
          <span className="rounded-lg bg-emerald-50 p-2 text-emerald-600">
            <Clock className="h-4 w-4" />
          </span>
        </div>
        <div className="mt-2 flex items-baseline gap-2">
          <span className="text-2xl font-bold text-emerald-600">{onTrack}</span>
          <span className="text-xs font-medium text-emerald-600">
            {total > 0 ? Math.round((onTrack / total) * 100) : 0}% tổng mốc
          </span>
        </div>
        <p className="mt-2 text-xs text-slate-400">Đang triển khai đúng kế hoạch</p>
      </div>

      {/* 3. Chậm trễ */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Trễ tiến độ
          </span>
          <span className="rounded-lg bg-rose-50 p-2 text-rose-600">
            <AlertTriangle className="h-4 w-4" />
          </span>
        </div>
        <div className="mt-2 flex items-baseline gap-2">
          <span className={`text-2xl font-bold ${delayed > 0 ? "text-rose-600" : "text-slate-800"}`}>
            {delayed}
          </span>
          {delayed > 0 ? (
            <span className="rounded bg-rose-50 px-1.5 py-0.5 text-xs font-semibold text-rose-600">
              Trễ đến {maxDelayDays} ngày
            </span>
          ) : (
            <span className="text-xs font-medium text-slate-400">Không có</span>
          )}
        </div>
        <p className="mt-2 text-xs text-slate-400">
          {delayed > 0 ? "Cần điều chỉnh nguồn lực & task" : "Kiểm soát tiến độ an toàn"}
        </p>
      </div>

      {/* 4. Đã hoàn thành */}
      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Đã hoàn thành
          </span>
          <span className="rounded-lg bg-sky-50 p-2 text-sky-600">
            <CheckCircle2 className="h-4 w-4" />
          </span>
        </div>
        <div className="mt-2 flex items-baseline gap-2">
          <span className="text-2xl font-bold text-sky-600">{completed}</span>
          <span className="rounded bg-sky-50 px-1.5 py-0.5 text-xs font-semibold text-sky-600">
            {completionRate}% hoàn tất
          </span>
        </div>
        <p className="mt-2 text-xs text-slate-400">Các mốc đã nghiệm thu</p>
      </div>
    </div>
  );
};
