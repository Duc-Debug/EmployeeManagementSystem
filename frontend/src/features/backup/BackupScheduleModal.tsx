"use client";

import { useEffect, useState } from "react";
import { Dialog } from "@/components/ui/Dialog";
import { FormField } from "@/components/ui/FormField";
import {
  fetchBackupSchedule,
  updateBackupSchedule,
} from "@/lib/api/backup";
import type {
  BackupFrequency,
  BackupSchedule,
  BackupType,
} from "@/lib/api/backup";
import { Clock, Loader2, Save } from "lucide-react";

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
          setSchedule(data);
          setIsEnabled(data.isEnabled);
          setFrequency(data.frequency);
          setScheduledTime(data.scheduledTime);
          setDayOfWeek(data.dayOfWeek || "MONDAY");
          setBackupType(data.backupType);
          setRetentionDays(data.retentionDays || 30);
        })
        .catch((err) => {
          setError(err instanceof Error ? err.message : "Tải cấu hình thất bại");
        })
        .finally(() => setIsLoading(false));
    }
  }, [open]);

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
    <Dialog
      open={open}
      onClose={onClose}
      title="Cấu hình lịch sao lưu tự động"
      description="Thiết lập sao lưu cơ sở dữ liệu định kỳ vào các khung giờ thấp điểm."
      footer={
        <div className="flex justify-end gap-3 w-full">
          <button
            type="button"
            className="px-4 py-2 border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-sm font-medium transition"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Hủy bỏ
          </button>
          <button
            type="submit"
            form="schedule-form"
            disabled={isSubmitting || isLoading}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium flex items-center gap-2 transition disabled:opacity-50"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Đang lưu...</span>
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                <span>Lưu cấu hình</span>
              </>
            )}
          </button>
        </div>
      }
    >
      {isLoading ? (
        <div className="flex items-center justify-center p-8 text-slate-500">
          <Loader2 className="w-6 h-6 animate-spin mr-2" />
          <span>Đang tải cấu hình...</span>
        </div>
      ) : (
        <form id="schedule-form" onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-lg text-red-700 dark:text-red-300 text-sm">
              {error}
            </div>
          )}

          <div className="flex items-center justify-between p-3.5 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl">
            <div className="space-y-0.5">
              <span className="text-sm font-semibold text-slate-900 dark:text-white">
                Kích hoạt tự động sao lưu
              </span>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Chạy tác vụ nền sao lưu theo lịch đã định sẵn
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
              <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
            </label>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <FormField id="schedule-frequency" label="Tần suất lặp lại">
              <select
                className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                value={frequency}
                onChange={(e) => setFrequency(e.target.value as BackupFrequency)}
                disabled={!isEnabled || isSubmitting}
              >
                <option value="DAILY">Hàng ngày (Daily)</option>
                <option value="WEEKLY">Hàng tuần (Weekly)</option>
              </select>
            </FormField>

            <FormField id="schedule-time" label="Giờ thực thi (24h)">
              <div className="relative">
                <input
                  type="time"
                  className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  value={scheduledTime}
                  onChange={(e) => setScheduledTime(e.target.value)}
                  disabled={!isEnabled || isSubmitting}
                />
              </div>
            </FormField>
          </div>

          {frequency === "WEEKLY" && (
            <FormField id="schedule-day" label="Ngày trong tuần">
              <select
                className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
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
            </FormField>
          )}

          <div className="grid grid-cols-2 gap-4">
            <FormField id="schedule-backup-type" label="Loại bản sao">
              <select
                className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                value={backupType}
                onChange={(e) => setBackupType(e.target.value as BackupType)}
                disabled={!isEnabled || isSubmitting}
              >
                <option value="FULL">Toàn bộ hệ thống</option>
                <option value="RESOURCE_PLAN">Kế hoạch nguồn lực</option>
              </select>
            </FormField>

            <FormField id="schedule-retention" label="Thời hạn lưu giữ (Ngày)">
              <input
                type="number"
                min={1}
                max={365}
                className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                value={retentionDays}
                onChange={(e) => setRetentionDays(Number(e.target.value))}
                disabled={!isEnabled || isSubmitting}
              />
            </FormField>
          </div>

          {schedule?.nextRunAt && isEnabled && (
            <div className="p-3 bg-indigo-50/50 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/50 rounded-xl text-xs text-indigo-900 dark:text-indigo-300 flex items-center gap-2">
              <Clock className="w-4 h-4 text-indigo-600 flex-shrink-0" />
              <span>
                Lần chạy tiếp theo: <strong>{new Date(schedule.nextRunAt).toLocaleString("vi-VN")}</strong>
              </span>
            </div>
          )}
        </form>
      )}
    </Dialog>
  );
}
