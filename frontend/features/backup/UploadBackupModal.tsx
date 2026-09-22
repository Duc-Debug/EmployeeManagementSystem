"use client";

import { useState } from "react";
import { Dialog } from "@/components/ui/Dialog";
import { FormField } from "@/components/ui/FormField";
import { BackupItem, BackupType, uploadBackupFile } from "@/lib/api/backup";
import { FileUp, Loader2, UploadCloud } from "lucide-react";

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
      title="Tải lên tệp sao lưu dữ liệu"
      description="Nạp một bản sao lưu có sẵn vào hệ thống để quản trị hoặc phục hồi."
      footer={
        <div className="flex justify-end gap-3 w-full">
          <button
            type="button"
            className="px-4 py-2 border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-sm font-medium transition"
            onClick={onClose}
            disabled={isUploading}
          >
            Hủy bỏ
          </button>
          <button
            type="submit"
            form="upload-form"
            disabled={isUploading || !selectedFile}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium flex items-center gap-2 transition disabled:opacity-50"
          >
            {isUploading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Đang tải lên và tính mã băm...</span>
              </>
            ) : (
              <>
                <FileUp className="w-4 h-4" />
                <span>Tải lên hệ thống</span>
              </>
            )}
          </button>
        </div>
      }
    >
      <form id="upload-form" onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-lg text-red-700 dark:text-red-300 text-sm">
            {error}
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
            Chọn tệp sao lưu (.json, .sql)
          </label>
          <label className="border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-indigo-500 dark:hover:border-indigo-500 rounded-xl p-6 flex flex-col items-center justify-center cursor-pointer bg-slate-50/50 dark:bg-slate-900/50 transition">
            <UploadCloud className="w-8 h-8 text-indigo-500 mb-2" />
            {selectedFile ? (
              <div className="text-center">
                <span className="text-sm font-semibold text-slate-900 dark:text-white block">
                  {selectedFile.name}
                </span>
                <span className="text-xs text-slate-500 dark:text-slate-400 font-mono">
                  {(selectedFile.size / 1024).toFixed(1)} KB
                </span>
              </div>
            ) : (
              <div className="text-center">
                <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
                  Nhấp để duyệt hoặc kéo thả tệp vào đây
                </span>
                <span className="text-xs text-slate-500 dark:text-slate-400 block mt-0.5">
                  Định dạng hỗ trợ: JSON Snapshot, SQL Dump
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

        <FormField label="Tiêu đề bản sao lưu">
          <input
            type="text"
            className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            placeholder="Tên gợi nhớ cho bản sao lưu..."
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={isUploading}
          />
        </FormField>

        <FormField label="Phân loại sao lưu">
          <select
            className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            value={backupType}
            onChange={(e) => setBackupType(e.target.value as BackupType)}
            disabled={isUploading}
          >
            <option value="FULL">Toàn bộ hệ thống (FULL)</option>
            <option value="RESOURCE_PLAN">Kế hoạch nguồn lực & Chấm công (PLAN)</option>
          </select>
        </FormField>

        <FormField label="Ghi chú & Mô tả nguồn gốc">
          <textarea
            className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            rows={2}
            placeholder="Ghi chú về nguồn gốc tệp hoặc môi trường kết xuất..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={isUploading}
          />
        </FormField>
      </form>
    </Dialog>
  );
}
