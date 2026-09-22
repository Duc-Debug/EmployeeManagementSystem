"use client";

import { useState } from "react";
import { Dialog } from "@/components/ui/Dialog";
import { FormField } from "@/components/ui/FormField";
import { createBackup } from "@/lib/api/backup";
import type { BackupItem, BackupType } from "@/lib/api/backup";
import { Database, Loader2 } from "lucide-react";

interface CreateBackupModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (backup: BackupItem) => void;
}

export function CreateBackupModal({ open, onClose, onSuccess }: CreateBackupModalProps) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [backupType, setBackupType] = useState<BackupType>("FULL");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      setError("Vui lòng nhập tên/tiêu đề bản sao lưu");
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      const backup = await createBackup(title.trim(), description.trim() || undefined, backupType);
      onSuccess(backup);
      onClose();
      setTitle("");
      setDescription("");
      setBackupType("FULL");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Tạo bản sao lưu thất bại";
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Tạo bản sao lưu dữ liệu mới"
      description="Tạo ảnh chụp trạng thái dữ liệu hệ thống để lưu trữ an toàn."
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
            form="create-backup-form"
            disabled={isSubmitting}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium flex items-center gap-2 transition disabled:opacity-50"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Đang sao lưu...</span>
              </>
            ) : (
              <>
                <Database className="w-4 h-4" />
                <span>Tiến hành sao lưu</span>
              </>
            )}
          </button>
        </div>
      }
    >
      <form id="create-backup-form" onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-lg text-red-700 dark:text-red-300 text-sm">
            {error}
          </div>
        )}

        <FormField id="create-backup-title" label="Tiêu đề bản sao lưu">
          <input
            type="text"
            className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            placeholder="Ví dụ: Sao lưu chốt kế hoạch Quý 4"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={isSubmitting}
            required
          />
        </FormField>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
            Phạm vi sao lưu
          </label>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <label
              className={`flex items-start p-3 border rounded-xl cursor-pointer transition ${
                backupType === "FULL"
                  ? "border-indigo-600 bg-indigo-50/40 dark:bg-indigo-950/30"
                  : "border-slate-200 dark:border-slate-800 hover:border-slate-300"
              }`}
            >
              <input
                type="radio"
                name="backupType"
                value="FULL"
                checked={backupType === "FULL"}
                onChange={() => setBackupType("FULL")}
                className="mt-0.5 text-indigo-600 focus:ring-indigo-500"
              />
              <div className="ml-3">
                <span className="block text-sm font-semibold text-slate-900 dark:text-white">
                  Toàn bộ hệ thống (FULL)
                </span>
                <span className="block text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  Tài khoản, quyền hạn, cơ cấu tổ chức, kỹ năng, dự án, chấm công và kế hoạch.
                </span>
              </div>
            </label>

            <label
              className={`flex items-start p-3 border rounded-xl cursor-pointer transition ${
                backupType === "RESOURCE_PLAN"
                  ? "border-indigo-600 bg-indigo-50/40 dark:bg-indigo-950/30"
                  : "border-slate-200 dark:border-slate-800 hover:border-slate-300"
              }`}
            >
              <input
                type="radio"
                name="backupType"
                value="RESOURCE_PLAN"
                checked={backupType === "RESOURCE_PLAN"}
                onChange={() => setBackupType("RESOURCE_PLAN")}
                className="mt-0.5 text-indigo-600 focus:ring-indigo-500"
              />
              <div className="ml-3">
                <span className="block text-sm font-semibold text-slate-900 dark:text-white">
                  Kế hoạch nguồn lực (PLAN)
                </span>
                <span className="block text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  Chỉ phân hệ kế hoạch nguồn lực, chấm công, dự án, phân bổ và xác nhận lịch.
                </span>
              </div>
            </label>
          </div>
        </div>

        <FormField id="create-backup-desc" label="Mô tả & Ghi chú (Tùy chọn)">
          <textarea
            className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            rows={3}
            placeholder="Ghi chú mục đích tạo bản sao lưu hoặc thời điểm chốt số liệu..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={isSubmitting}
          />
        </FormField>
      </form>
    </Dialog>
  );
}
