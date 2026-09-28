"use client";

import { useEffect, useState } from "react";
import {
  fetchBackupSchedule,
  updateBackupSchedule,
} from "@/lib/api/backup";
import type {
  BackupFrequency,
  BackupSchedule,
  BackupType,
} from "@/lib/api/backup";
import { Clock, Loader2, Save, Calendar, AlertCircle, X } from "lucide-react";

interface BackupScheduleModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (schedule: BackupSchedule) => void;
}

export function BackupScheduleModal({
  open,
  onClose,
  onSuccess,
}: BackupScheduleModalProps) {
  const [schedule, setSchedule] = useState<BackupSchedule | null>(null);
  const [isEnabled, setIsEnabled] = useState(false);
  const [frequency, setFrequency] = useState<BackupFrequency>("DAILY");
  const [scheduledTime, setScheduledTime] = useState("02:00");
  const [dayOfWeek, setDayOfWeek] = useState("MONDAY");
  const [backupType, setBackupType] = useState<BackupType>("FULL");
  const [retentionDays, setRetentionDays] = useState(30);

  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setIsLoading(true);
      setError(null);
      fetchBackupSchedule()
        .then((data) => {
          if (data) {
            setSchedule(data);
            setIsEnabled(Boolean(data.isEnabled));
            if (data.frequency) setFrequency(data.frequency);
            if (data.scheduledTime) setScheduledTime(data.scheduledTime);
            if (data.dayOfWeek) setDayOfWeek(data.dayOfWeek);
            if (data.backupType) setBackupType(data.backupType);
            if (data.retentionDays) setRetentionDays(data.retentionDays);
          }
        })
        .catch((err) => {
          setError(err instanceof Error ? err.message : "Tải cấu hình thất bại");
        })
        .finally(() => setIsLoading(false));
    }
  }, [open]);

  if (!open) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setIsSubmitting(true);
      setError(null);
      const updated = await updateBackupSchedule({
        isEnabled,
        frequency,
        scheduledTime,
        dayOfWeek,
        backupType,
        retentionDays,
      });
      onSuccess(updated);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Cập nhật lịch thất bại";
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/60">
          <div className="flex items-center space-x-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-50 text-purple-600 border border-purple-100 shadow-2xs">
              <Calendar className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">Cấu Hình Lịch Sao Lưu Tự Động</h3>
              <p className="text-xs text-slate-500">Thiết lập sao lưu định kỳ vào khung giờ thấp điểm</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Modal Body */}
        {isLoading ? (
          <div className="flex items-center justify-center p-12 text-slate-500">
            <Loader2 className="w-6 h-6 animate-spin mr-2 text-indigo-600" />
            <span className="text-xs font-medium">Đang tải cấu hình lịch sao lưu...</span>
          </div>
        ) : (
          <form id="schedule-form" onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
            {error && (
              <div className="flex items-start space-x-2.5 rounded-xl bg-rose-50 p-3.5 text-xs text-rose-700 border border-rose-100 animate-in fade-in">
                <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
                <div className="flex-1 font-medium">{error}</div>
              </div>
            )}

            {/* Toggle Switch */}
            <div className="flex items-center justify-between p-3.5 bg-slate-50 border border-slate-200 rounded-xl shadow-2xs">
              <div className="space-y-0.5">
                <span className="text-xs font-bold text-slate-900 block">
                  Kích hoạt Tác vụ Sao lưu Tự động
                </span>
                <p className="text-[11px] text-slate-500">
                  Tự động chụp snapshot theo chu kỳ và dọn dẹp bản sao quá hạn.
                </p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  className="sr-only peer"
                  checked={isEnabled}
                  onChange={(e) => setIsEnabled(e.target.checked)}
                  disabled={isSubmitting}
                />
                <div className="w-10 h-5 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-600"></div>
              </label>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Tần suất lặp lại
                </label>
                <select
                  className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-medium focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 disabled:opacity-50 cursor-pointer"
                  value={frequency}
                  onChange={(e) => setFrequency(e.target.value as BackupFrequency)}
                  disabled={!isEnabled || isSubmitting}
                >
                  <option value="DAILY">Hàng ngày (Daily)</option>
                  <option value="WEEKLY">Hàng tuần (Weekly)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Giờ thực thi (24h)
                </label>
                <input
                  type="time"
                  className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-medium focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 disabled:opacity-50"
                  value={scheduledTime}
                  onChange={(e) => setScheduledTime(e.target.value)}
                  disabled={!isEnabled || isSubmitting}
                />
              </div>
            </div>

            {frequency === "WEEKLY" && (
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Ngày chạy trong tuần
                </label>
                <select
                  className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-medium focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 disabled:opacity-50 cursor-pointer"
                  value={dayOfWeek}
                  onChange={(e) => setDayOfWeek(e.target.value)}
                  disabled={!isEnabled || isSubmitting}
                >
                  <option value="MONDAY">Thứ Hai</option>
                  <option value="TUESDAY">Thứ Ba</option>
                  <option value="WEDNESDAY">Thứ Tư</option>
                  <option value="THURSDAY">Thứ Năm</option>
                  <option value="FRIDAY">Thứ Sáu</option>
                  <option value="SATURDAY">Thứ Bảy</option>
                  <option value="SUNDAY">Chủ Nhật</option>
                </select>
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Phạm vi sao lưu
                </label>
                <select
                  className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-medium focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 disabled:opacity-50 cursor-pointer"
                  value={backupType}
                  onChange={(e) => setBackupType(e.target.value as BackupType)}
                  disabled={!isEnabled || isSubmitting}
                >
                  <option value="FULL">Toàn bộ hệ thống</option>
                  <option value="RESOURCE_PLAN">Kế hoạch nguồn lực</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Thời hạn lưu giữ (Ngày)
                </label>
                <input
                  type="number"
                  min={1}
                  max={365}
                  className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-medium focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 disabled:opacity-50"
                  value={retentionDays}
                  onChange={(e) => setRetentionDays(Number(e.target.value))}
                  disabled={!isEnabled || isSubmitting}
                />
              </div>
            </div>

            {schedule?.nextRunAt && isEnabled && (
              <div className="p-3 bg-indigo-50/70 border border-indigo-100 rounded-xl text-xs text-indigo-950 flex items-center gap-2">
                <Clock className="w-4 h-4 text-indigo-600 flex-shrink-0" />
                <span>
                  Lần chạy kế tiếp: <strong>{new Date(schedule.nextRunAt).toLocaleString("vi-VN")}</strong>
                </span>
              </div>
            )}
          </form>
        )}

        {/* Modal Footer */}
        <div className="flex items-center justify-end gap-2.5 border-t border-slate-100 bg-slate-50/60 px-6 py-3.5">
          <button
            type="button"
            className="px-4 py-2 border border-slate-200 text-slate-700 rounded-xl hover:bg-slate-100 text-xs font-semibold transition cursor-pointer shadow-2xs"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Hủy bỏ
          </button>
          <button
            type="submit"
            form="schedule-form"
            disabled={isSubmitting || isLoading}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition shadow-xs disabled:opacity-50 cursor-pointer"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Đang lưu cấu hình...</span>
              </>
            ) : (
              <>
                <Save className="w-3.5 h-3.5" />
                <span>Lưu cấu hình</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

export default BackupScheduleModal;
