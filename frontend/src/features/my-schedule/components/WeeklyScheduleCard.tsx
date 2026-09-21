import React from "react";
import { Calendar, Briefcase, Clock, AlertCircle } from "lucide-react";
import type { WeeklySchedule } from "../types";
import { ConfirmationStatusBadge } from "./ConfirmationStatusBadge";

interface WeeklyScheduleCardProps {
  schedule: WeeklySchedule;
  onConfirm: (weekStart: string) => void;
  isConfirming?: boolean;
}

export const WeeklyScheduleCard: React.FC<WeeklyScheduleCardProps> = ({
  schedule,
  onConfirm,
  isConfirming = false,
}) => {
  const { week_start_date, total_hours, confirmation_status, confirmed_at, allocations } = schedule;

  const formatDateRange = (mondayStr: string) => {
    try {
      const monday = new Date(mondayStr);
      const sunday = new Date(monday);
      sunday.setDate(monday.getDate() + 6);

      const dFormat = (d: Date) =>
        d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });

      return `${dFormat(monday)} — ${dFormat(sunday)}`;
    } catch {
      return mondayStr;
    }
  };

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden transition hover:shadow-md">
      {/* Header card tuần */}
      <div className="p-5 border-b border-slate-100 bg-slate-50/50 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-blue-50 text-blue-600 rounded-lg">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-semibold text-slate-900 text-base">
                Tuần bắt đầu: {week_start_date}
              </h3>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Thời gian: {formatDateRange(week_start_date)}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-4 self-end md:self-auto">
          <div className="text-right">
            <span className="text-xs text-slate-500 block">Tổng phân bổ</span>
            <span className="text-lg font-bold text-slate-800">
              {total_hours.toFixed(1)} <span className="text-xs font-normal text-slate-500">giờ</span>
            </span>
          </div>

          <div className="border-l border-slate-200 pl-4">
            <ConfirmationStatusBadge
              status={confirmation_status}
              confirmedAt={confirmed_at}
              onConfirm={() => onConfirm(week_start_date)}
              isConfirming={isConfirming}
            />
          </div>
        </div>
      </div>

      {/* Danh sách phân bổ dự án */}
      <div className="p-5">
        {allocations.length === 0 ? (
          <div className="text-center py-8 bg-slate-50/50 rounded-lg border border-dashed border-slate-200">
            <AlertCircle className="w-8 h-8 text-slate-300 mx-auto mb-2" />
            <p className="text-sm text-slate-500 font-medium">
              Chưa có phân bổ dự án nào cho tuần này.
            </p>
            <p className="text-xs text-slate-400 mt-1">
              Bạn vẫn có thể xác nhận đã xem lịch làm việc trống này.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Chi tiết phân bổ dự án ({allocations.length})
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {allocations.map((alloc) => {
                const isClosed = alloc.project_status?.toUpperCase() === "CLOSED";
                return (
                  <div
                    key={alloc.allocation_id}
                    className="p-3.5 rounded-lg border border-slate-100 bg-slate-50/30 flex items-center justify-between gap-3 transition hover:bg-slate-50"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="p-2 rounded-md bg-white border border-slate-200 text-slate-600 shrink-0">
                        <Briefcase className="w-4 h-4" />
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-slate-800 text-sm truncate">
                            {alloc.project_name}
                          </span>
                          {isClosed && (
                            <span className="shrink-0 px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-200 text-slate-600 border border-slate-300">
                              Dự án đã đóng
                            </span>
                          )}
                        </div>
                        <span className="text-[11px] text-slate-400 block mt-0.5">
                          ID: #{alloc.project_id}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0 bg-white border border-slate-200 px-2.5 py-1.5 rounded-md">
                      <Clock className="w-3.5 h-3.5 text-blue-600" />
                      <span className="text-xs font-semibold text-slate-800">
                        {alloc.allocated_hours.toFixed(1)}h
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};