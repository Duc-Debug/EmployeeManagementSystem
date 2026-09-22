"use client";

import { useState } from "react";
import { Dialog } from "@/components/ui/Dialog";
import { FormField } from "@/components/ui/FormField";
import { uploadBackupFile } from "@/lib/api/backup";
import type { BackupItem, BackupType } from "@/lib/api/backup";
import { FileUp, Loader2, UploadCloud, AlertCircle } from "lucide-react";

interface UploadBackupModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (backup: BackupItem) => void;
}

export function UploadBackupModal({
  open,
  onClose,
  onSuccess,
}: UploadBackupModalProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [backupType, setBackupType] = useState<BackupType>("FULL");
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      if (!title) {
        setTitle(file.name.replace(/\.[^/.]+$/, ""));
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) {
      setError("Vui lòng chọn một tệp sao lưu (.json hoặc .sql)");
      return;
    }

    try {
      setIsUploading(true);
      setError(null);
      const backup = await uploadBackupFile(
        selectedFile,
        title.trim() || undefined,
        description.trim() || undefined,
        backupType
      );
      onSuccess(backup);
      onClose();
      setSelectedFile(null);
      setTitle("");
      setDescription("");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Tải lên tệp thất bại";
      setError(msg);
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Tải lên Tệp Bản sao lưu"
      description="Nạp một bản sao lưu snapshot có sẵn vào hệ thống để phục hồi hoặc quản trị."
      footer={
        <div className="flex justify-end items-center gap-2 w-full">
          <button
            type="button"
            className="px-3.5 py-1.5 border border-slate-200 text-slate-700 rounded-xl hover:bg-slate-50 text-xs font-semibold transition cursor-pointer shadow-2xs"
            onClick={onClose}
            disabled={isUploading}
          >
            Hủy bỏ
          </button>
          <button
            type="submit"
            form="upload-form"
            disabled={isUploading || !selectedFile}
            className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition shadow-xs disabled:opacity-50 cursor-pointer"
          >
            {isUploading ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Đang tải lên & tính SHA-256...</span>
              </>
            ) : (
              <>
                <FileUp className="w-3.5 h-3.5" />
                <span>Nạp tệp vào hệ thống</span>
              </>
            )}
          </button>
        </div>
      }
    >
      <form id="upload-form" onSubmit={handleSubmit} className="space-y-3.5">
        {error && (
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-900 text-xs flex items-start gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        {/* Dropzone */}
        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1.5">
            Chọn tệp dữ liệu sao lưu (.json, .sql)
          </label>
          <label className="border-2 border-dashed border-slate-200 hover:border-indigo-500 rounded-xl p-5 flex flex-col items-center justify-center cursor-pointer bg-slate-50/50 hover:bg-indigo-50/20 transition group">
            <UploadCloud className="w-7 h-7 text-indigo-500 mb-1.5 group-hover:scale-110 transition-transform" />
            {selectedFile ? (
              <div className="text-center">
                <span className="text-xs font-bold text-slate-900 block truncate max-w-xs">
                  {selectedFile.name}
                </span>
                <span className="text-[11px] text-slate-500 font-mono">
                  {(selectedFile.size / 1024).toFixed(1)} KB · Sẵn sàng nạp
                </span>
              </div>
            ) : (
              <div className="text-center">
                <span className="text-xs font-semibold text-slate-700 block">
                  Nhấp để duyệt hoặc kéo thả tệp vào đây
                </span>
                <span className="text-[11px] text-slate-400 block mt-0.5">
                  Định dạng hỗ trợ: JSON Snapshot, SQL Dump (Tối đa 50MB)
                </span>
              </div>
            )}
            <input
              type="file"
              className="hidden"
              accept=".json,.sql,.gz"
              onChange={handleFileChange}
              disabled={isUploading}
            />
          </label>
        </div>

        <FormField id="upload-backup-title" label="Tiêu đề gợi nhớ">
          <input
            type="text"
            className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none placeholder:text-slate-400 font-medium"
            placeholder="Tên bản sao lưu..."
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={isUploading}
          />
        </FormField>

        <FormField id="upload-backup-type" label="Phân loại phạm vi">
          <select
            className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs font-medium focus:ring-2 focus:ring-indigo-500 focus:outline-none cursor-pointer"
            value={backupType}
            onChange={(e) => setBackupType(e.target.value as BackupType)}
            disabled={isUploading}
          >
            <option value="FULL">Toàn bộ hệ thống (FULL)</option>
            <option value="RESOURCE_PLAN">Kế hoạch nguồn lực & Chấm công (PLAN)</option>
          </select>
        </FormField>

        <FormField id="upload-backup-desc" label="Ghi chú & Nguồn gốc">
          <textarea
            className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none placeholder:text-slate-400"
            rows={2}
            placeholder="Ghi chú nguồn gốc tệp hoặc môi trường kết xuất..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={isUploading}
          />
        </FormField>
      </form>
    </Dialog>
  );
}

export default UploadBackupModal;
