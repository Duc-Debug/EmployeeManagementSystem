"use client";

import { useState } from "react";
import { createBackup } from "@/lib/api/backup";
import type { BackupItem, BackupType } from "@/lib/api/backup";
import { Database, Loader2, Layers, AlertCircle, X } from "lucide-react";
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

  if (!open) return null;

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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/60">
          <div className="flex items-center space-x-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100 shadow-2xs">
              <Database className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">Tạo Bản Sao Lưu Dữ Liệu Mới</h3>
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
        <form id="create-backup-form" onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
          {error && (
            <div className="flex items-start space-x-2.5 rounded-xl bg-rose-50 p-3.5 text-xs text-rose-700 border border-rose-100 animate-in fade-in">
              <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
              <div className="flex-1 font-medium">{error}</div>
            </div>
          )}

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
              Tiêu đề bản sao lưu <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-medium focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 placeholder:text-slate-400"
              placeholder="Ví dụ: Sao lưu chốt kế hoạch tuần 38..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={isSubmitting}
              required
              autoFocus
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
              Phạm vi dữ liệu sao lưu <span className="text-rose-500">*</span>
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              <label
                className={cn(
                  "flex items-start p-3 border rounded-xl cursor-pointer transition shadow-2xs",
                  backupType === "FULL"
                    ? "border-indigo-600 bg-indigo-50/50 text-indigo-950 ring-1 ring-indigo-600"
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
                    Tài khoản, quyền hạn, cơ cấu tổ chức, kỹ năng, dự án, chấm công và kế hoạch.
                  </span>
                </div>
              </label>

              <label
                className={cn(
                  "flex items-start p-3 border rounded-xl cursor-pointer transition shadow-2xs",
                  backupType === "RESOURCE_PLAN"
                    ? "border-purple-600 bg-purple-50/50 text-purple-950 ring-1 ring-purple-600"
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
                    Kế hoạch nguồn lực
                  </span>
                  <span className="block text-[11px] text-slate-500 mt-0.5 leading-snug">
                    Chỉ phân hệ kế hoạch nguồn lực, chấm công, dự án, phân bổ và xác nhận lịch.
                  </span>
                </div>
              </label>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
              Mô tả & Ghi chú (Tùy chọn)
            </label>
            <textarea
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 placeholder:text-slate-400"
              rows={3}
              placeholder="Ghi chú mục đích tạo bản sao lưu hoặc thời điểm chốt số liệu..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={isSubmitting}
            />
          </div>
        </form>

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
            form="create-backup-form"
            disabled={isSubmitting || !title.trim()}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition shadow-xs disabled:opacity-50 cursor-pointer"
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
      </div>
    </div>
  );
}

export default CreateBackupModal;
