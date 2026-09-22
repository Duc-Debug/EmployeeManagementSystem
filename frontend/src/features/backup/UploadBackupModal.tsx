"use client";

import { useState } from "react";
import { uploadBackupFile } from "@/lib/api/backup";
import type { BackupItem } from "@/lib/api/backup";
import { FileUp, Loader2, UploadCloud, AlertCircle, X } from "lucide-react";

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
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) return null;

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
      setError("Vui lòng chọn một tệp sao lưu JSON Snapshot (.json)");
      return;
    }
    if (!selectedFile.name.toLowerCase().endsWith(".json")) {
      setError("Định dạng tệp không hợp lệ. Hệ thống chỉ hỗ trợ tệp .json");
      return;
    }

    try {
      setIsUploading(true);
      setError(null);
      const backup = await uploadBackupFile(
        selectedFile,
        title.trim() || undefined,
        description.trim() || undefined
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/60">
          <div className="flex items-center space-x-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100 shadow-2xs">
              <UploadCloud className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">Tải Lên Tệp Bản Sao Lưu</h3>
              <p className="text-xs text-slate-500">Nạp bản sao lưu snapshot sẵn có vào hệ thống để quản trị hoặc phục hồi</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isUploading}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Modal Body */}
        <form id="upload-form" onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
          {error && (
            <div className="flex items-start space-x-2.5 rounded-xl bg-rose-50 p-3.5 text-xs text-rose-700 border border-rose-100 animate-in fade-in">
              <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
              <div className="flex-1 font-medium">{error}</div>
            </div>
          )}

          {/* Dropzone */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
              Chọn tệp dữ liệu sao lưu (.json) <span className="text-rose-500">*</span>
            </label>
            <label className="border-2 border-dashed border-slate-200 hover:border-indigo-500 rounded-2xl p-6 flex flex-col items-center justify-center cursor-pointer bg-slate-50/50 hover:bg-indigo-50/20 transition group">
              <UploadCloud className="w-8 h-8 text-indigo-500 mb-2 group-hover:scale-110 transition-transform" />
              {selectedFile ? (
                <div className="text-center">
                  <span className="text-xs font-bold text-slate-900 block truncate max-w-xs">
                    {selectedFile.name}
                  </span>
                  <span className="text-[11px] text-slate-500 font-mono mt-0.5 block">
                    {(selectedFile.size / 1024).toFixed(1)} KB · Sẵn sàng nạp
                  </span>
                </div>
              ) : (
                <div className="text-center">
                  <span className="text-xs font-semibold text-slate-700 block">
                    Nhấp để duyệt hoặc kéo thả tệp vào đây
                  </span>
                  <span className="text-[11px] text-slate-400 block mt-0.5">
                    Định dạng hỗ trợ: JSON Snapshot (.json) (Tối đa 50MB)
                  </span>
                </div>
              )}
              <input
                type="file"
                className="hidden"
                accept=".json,application/json"
                onChange={handleFileChange}
                disabled={isUploading}
              />
            </label>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
              Tiêu đề gợi nhớ
            </label>
            <input
              type="text"
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-medium focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 placeholder:text-slate-400"
              placeholder="Tên bản sao lưu..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={isUploading}
            />
          </div>

          <div className="rounded-xl bg-slate-50 border border-slate-200/80 p-3 text-xs text-slate-600 flex items-center justify-between">
            <span className="font-semibold text-slate-700">Phân loại phạm vi sao lưu:</span>
            <span className="inline-flex items-center gap-1 font-mono font-medium text-indigo-700 bg-indigo-50 border border-indigo-200 px-2 py-0.5 rounded-md text-[11px]">
              Tự động nhận diện từ tệp sao lưu
            </span>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
              Ghi chú & Nguồn gốc
            </label>
            <textarea
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 placeholder:text-slate-400"
              rows={2}
              placeholder="Ghi chú nguồn gốc tệp hoặc môi trường kết xuất..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={isUploading}
            />
          </div>
        </form>

        {/* Modal Footer */}
        <div className="flex items-center justify-end gap-2.5 border-t border-slate-100 bg-slate-50/60 px-6 py-3.5">
          <button
            type="button"
            className="px-4 py-2 border border-slate-200 text-slate-700 rounded-xl hover:bg-slate-100 text-xs font-semibold transition cursor-pointer shadow-2xs"
            onClick={onClose}
            disabled={isUploading}
          >
            Hủy bỏ
          </button>
          <button
            type="submit"
            form="upload-form"
            disabled={isUploading || !selectedFile}
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition shadow-xs disabled:opacity-50 cursor-pointer"
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
      </div>
    </div>
  );
}

export default UploadBackupModal;
