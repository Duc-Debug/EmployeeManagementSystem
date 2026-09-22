"use client";

import { useState } from "react";
import { Dialog } from "@/components/ui/Dialog";
import { FormField } from "@/components/ui/FormField";
import { createBackup } from "@/lib/api/backup";
import type { BackupItem, BackupType } from "@/lib/api/backup";
import { Database, Loader2, Layers, AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";

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
      setError("Vui lòng nhập tiêu đề cho bản sao lưu");
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
      title="Tạo Bản sao lưu Dữ liệu Mới"
      description="Tạo ảnh chụp trạng thái dữ liệu hệ thống tức thời và tính toán mã kiểm tra SHA-256."
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
            form="create-backup-form"
            disabled={isSubmitting || !title.trim()}
            className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition shadow-xs disabled:opacity-50 cursor-pointer"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Đang sao lưu & tính hash...</span>
              </>
            ) : (
              <>
                <Database className="w-3.5 h-3.5" />
                <span>Tiến hành sao lưu</span>
              </>
            )}
          </button>
        </div>
      }
    >
      <form id="create-backup-form" onSubmit={handleSubmit} className="space-y-3.5">
        {error && (
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-900 text-xs flex items-start gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        <FormField id="create-backup-title" label="Tiêu đề bản sao lưu *">
          <input
            type="text"
            className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none placeholder:text-slate-400 font-medium"
            placeholder="Ví dụ: Sao lưu chốt kế hoạch tuần 38..."
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={isSubmitting}
            required
            autoFocus
          />
        </FormField>

        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
            Phạm vi dữ liệu sao lưu
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <label
              className={cn(
                "flex items-start p-3 border rounded-xl cursor-pointer transition shadow-2xs",
                backupType === "FULL"
                  ? "border-indigo-600 bg-indigo-50/50 text-indigo-950"
                  : "border-slate-200 hover:border-slate-300 bg-white"
              )}
            >
              <input
                type="radio"
                name="backupType"
                value="FULL"
                checked={backupType === "FULL"}
                onChange={() => setBackupType("FULL")}
                className="mt-0.5 text-indigo-600 focus:ring-indigo-500 cursor-pointer"
              />
              <div className="ml-2.5">
                <span className="block text-xs font-bold text-slate-900 flex items-center gap-1.5">
                  <Layers className="w-3.5 h-3.5 text-indigo-600" />
                  Toàn bộ hệ thống (FULL)
                </span>
                <span className="block text-[11px] text-slate-500 mt-0.5 leading-snug">
                  Toàn bộ tài khoản, quyền hạn, cơ cấu tổ chức, kỹ năng, dự án, chấm công và kế hoạch.
                </span>
              </div>
            </label>

            <label
              className={cn(
                "flex items-start p-3 border rounded-xl cursor-pointer transition shadow-2xs",
                backupType === "RESOURCE_PLAN"
                  ? "border-purple-600 bg-purple-50/50 text-purple-950"
                  : "border-slate-200 hover:border-slate-300 bg-white"
              )}
            >
              <input
                type="radio"
                name="backupType"
                value="RESOURCE_PLAN"
                checked={backupType === "RESOURCE_PLAN"}
                onChange={() => setBackupType("RESOURCE_PLAN")}
                className="mt-0.5 text-purple-600 focus:ring-purple-500 cursor-pointer"
              />
              <div className="ml-2.5">
                <span className="block text-xs font-bold text-slate-900 flex items-center gap-1.5">
                  <Database className="w-3.5 h-3.5 text-purple-600" />
                  Kế hoạch nguồn lực (PLAN)
                </span>
                <span className="block text-[11px] text-slate-500 mt-0.5 leading-snug">
                  Chỉ phân hệ kế hoạch nguồn lực, chấm công, dự án, phân bổ và xác nhận lịch.
                </span>
              </div>
            </label>
          </div>
        </div>

        <FormField id="create-backup-desc" label="Mô tả & Ghi chú (Tùy chọn)">
          <textarea
            className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none placeholder:text-slate-400"
            rows={2.5}
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

export default CreateBackupModal;
