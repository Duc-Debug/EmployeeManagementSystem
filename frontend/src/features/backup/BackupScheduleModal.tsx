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
import { Clock, Loader2, Save, AlertCircle } from "lucide-react";

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
      title="Cấu hình Lịch Sao lưu Tự động"
      description="Thiết lập tác vụ nền sao lưu cơ sở dữ liệu định kỳ vào các khung giờ thấp điểm."
      footer={
        <div className="flex justify-end items-center gap-2 w-full">
          <button
            type="button"
            className="px-3.5 py-1.5 border border-slate-200 text-slate-700 rounded-xl hover:bg-slate-50 text-xs font-semibold transition cursor-pointer shadow-2xs"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Hủy bỏ
          </button>
          <button
            type="submit"
            form="schedule-form"
            disabled={isSubmitting || isLoading}
            className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition shadow-xs disabled:opacity-50 cursor-pointer"
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
      }
    >
      {isLoading ? (
        <div className="flex items-center justify-center p-8 text-slate-500">
          <Loader2 className="w-6 h-6 animate-spin mr-2 text-indigo-600" />
          <span className="text-xs font-medium">Đang nạp cấu hình lịch sao lưu...</span>
        </div>
      ) : (
        <form id="schedule-form" onSubmit={handleSubmit} className="space-y-3.5">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-900 text-xs flex items-start gap-2">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {/* Toggle Switch */}
          <div className="flex items-center justify-between p-3 bg-slate-50 border border-slate-200 rounded-xl shadow-2xs">
            <div className="space-y-0.5">
              <span className="text-xs font-bold text-slate-900 block">
                Kích hoạt Tác vụ Sao lưu Định kỳ
              </span>
              <p className="text-[11px] text-slate-500">
                Tự động tạo ảnh chụp snapshot theo chu kỳ và xóa bản sao quá hạn.
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
            <FormField id="schedule-frequency" label="Tần suất lặp lại">
              <select
                className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none disabled:opacity-50 cursor-pointer"
                value={frequency}
                onChange={(e) => setFrequency(e.target.value as BackupFrequency)}
                disabled={!isEnabled || isSubmitting}
              >
                <option value="DAILY">Hàng ngày (Daily)</option>
                <option value="WEEKLY">Hàng tuần (Weekly)</option>
              </select>
            </FormField>

            <FormField id="schedule-time" label="Giờ thực thi (24h)">
              <input
                type="time"
                className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none disabled:opacity-50"
                value={scheduledTime}
                onChange={(e) => setScheduledTime(e.target.value)}
                disabled={!isEnabled || isSubmitting}
              />
            </FormField>
          </div>

          {frequency === "WEEKLY" && (
            <FormField id="schedule-day" label="Ngày chạy trong tuần">
              <select
                className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none disabled:opacity-50 cursor-pointer"
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

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <FormField id="schedule-backup-type" label="Phạm vi sao lưu">
              <select
                className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none disabled:opacity-50 cursor-pointer"
                value={backupType}
                onChange={(e) => setBackupType(e.target.value as BackupType)}
                disabled={!isEnabled || isSubmitting}
              >
                <option value="FULL">Toàn bộ hệ thống (FULL)</option>
                <option value="RESOURCE_PLAN">Kế hoạch nguồn lực (PLAN)</option>
              </select>
            </FormField>

            <FormField id="schedule-retention" label="Thời hạn lưu giữ (Ngày)">
              <input
                type="number"
                min={1}
                max={365}
                className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none disabled:opacity-50"
                value={retentionDays}
                onChange={(e) => setRetentionDays(Number(e.target.value))}
                disabled={!isEnabled || isSubmitting}
              />
            </FormField>
          </div>

          {schedule?.nextRunAt && isEnabled && (
            <div className="p-2.5 bg-indigo-50/70 border border-indigo-100 rounded-xl text-xs text-indigo-950 flex items-center gap-2">
              <Clock className="w-3.5 h-3.5 text-indigo-600 flex-shrink-0" />
              <span>
                Lần chạy kế tiếp: <strong>{new Date(schedule.nextRunAt).toLocaleString("vi-VN")}</strong>
              </span>
            </div>
          )}
        </form>
      )}
    </Dialog>
  );
}

export default BackupScheduleModal;
