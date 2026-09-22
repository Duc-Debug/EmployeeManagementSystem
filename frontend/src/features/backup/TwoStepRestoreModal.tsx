"use client";

import { useState } from "react";
import { Dialog } from "@/components/ui/Dialog";
import { FormField } from "@/components/ui/FormField";
import { restoreBackup } from "@/lib/api/backup";
import type { BackupItem } from "@/lib/api/backup";
import {
  AlertTriangle,
  RotateCcw,
  ShieldAlert,
  Loader2,
  ArrowRight,
  ArrowLeft,
  CheckCircle2,
} from "lucide-react";

interface TwoStepRestoreModalProps {
  backup: BackupItem | null;
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function TwoStepRestoreModal({
  backup,
  open,
  onClose,
  onSuccess,
}: TwoStepRestoreModalProps) {
  const [step, setStep] = useState<1 | 2>(1);
  const [confirmationCode, setConfirmationCode] = useState("");
  const [reason, setReason] = useState("");
  const [isRestoring, setIsRestoring] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!backup) return null;

  const handleClose = () => {
    if (isRestoring) return;
    setStep(1);
    setConfirmationCode("");
    setReason("");
    setError(null);
    onClose();
  };

  const handleNextStep = () => {
    setError(null);
    setStep(2);
  };

  const handlePrevStep = () => {
    setError(null);
    setStep(1);
  };

  const handleRestoreSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (confirmationCode.trim().toUpperCase() !== "RESTORE") {
      setError("Vui lòng nhập chính xác từ khóa 'RESTORE' để xác nhận.");
      return;
    }
    if (reason.trim().length < 10) {
      setError("Lý do phục hồi phải có độ dài tối thiểu 10 ký tự.");
      return;
    }

    try {
      setIsRestoring(true);
      setError(null);
      await restoreBackup(backup.id, "RESTORE", reason.trim());
      onSuccess();
      handleClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Phục hồi dữ liệu thất bại";
      setError(msg);
    } finally {
      setIsRestoring(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      title={`Phục hồi dữ liệu hệ thống (Bước ${step}/2)`}
      description="Quy trình xác nhận hai bước an toàn nhằm khôi phục trạng thái cơ sở dữ liệu."
      footer={
        <div className="flex justify-between items-center w-full">
          {step === 2 ? (
            <button
              type="button"
              className="px-3 py-2 text-slate-600 dark:text-slate-400 hover:text-slate-900 text-sm font-medium flex items-center gap-1 transition"
              onClick={handlePrevStep}
              disabled={isRestoring}
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Quay lại</span>
            </button>
          ) : (
            <div />
          )}

          <div className="flex gap-2">
            <button
              type="button"
              className="px-4 py-2 border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-sm font-medium transition"
              onClick={handleClose}
              disabled={isRestoring}
            >
              Hủy bỏ
            </button>

            {step === 1 ? (
              <button
                type="button"
                className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-lg text-sm font-medium flex items-center gap-2 transition"
                onClick={handleNextStep}
              >
                <span>Xác nhận rủi ro & Tiếp tục</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            ) : (
              <button
                type="submit"
                form="restore-form"
                disabled={
                  isRestoring ||
                  confirmationCode.trim().toUpperCase() !== "RESTORE" ||
                  reason.trim().length < 10
                }
                className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-medium flex items-center gap-2 transition disabled:opacity-50"
              >
                {isRestoring ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang phục hồi cơ sở dữ liệu...</span>
                  </>
                ) : (
                  <>
                    <RotateCcw className="w-4 h-4" />
                    <span>Thực hiện Phục hồi ngay</span>
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      }
    >
      {error && (
        <div className="mb-4 p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-lg text-red-700 dark:text-red-300 text-sm flex items-start gap-2">
          <AlertTriangle className="w-5 h-5 flex-shrink-0 text-red-600 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {step === 1 ? (
        <div className="space-y-4">
          <div className="p-4 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 rounded-xl">
            <div className="flex items-start gap-3">
              <ShieldAlert className="w-6 h-6 text-amber-600 flex-shrink-0 mt-0.5" />
              <div className="space-y-1">
                <h4 className="font-semibold text-amber-900 dark:text-amber-200 text-sm">
                  Cảnh báo ghi đè trạng thái hệ thống
                </h4>
                <p className="text-xs text-amber-800 dark:text-amber-300 leading-relaxed">
                  Thao tác phục hồi sẽ <strong>ghi đè toàn bộ dữ liệu hiện tại</strong> về đúng thời điểm tạo của bản sao lưu được chọn. Các thay đổi dữ liệu phát sinh sau thời điểm sao lưu sẽ bị thay thế.
                </p>
                <div className="pt-2 flex items-center gap-1.5 text-xs text-emerald-700 dark:text-emerald-400 font-medium">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Hệ thống sẽ tự động tạo một điểm an toàn (Safety Snapshot) trước khi ghi đè.</span>
                </div>
              </div>
            </div>
          </div>

          <div className="border border-slate-200 dark:border-slate-800 rounded-xl p-4 bg-slate-50/50 dark:bg-slate-900/50 space-y-2.5 text-sm">
            <h5 className="font-semibold text-slate-800 dark:text-slate-200 text-xs uppercase tracking-wider">
              Chi tiết bản sao lưu nguồn
            </h5>
            <div className="grid grid-cols-2 gap-y-2 text-xs">
              <span className="text-slate-500 dark:text-slate-400">Mã bản sao:</span>
              <span className="font-mono font-medium text-slate-900 dark:text-white">
                {backup.backupCode}
              </span>

              <span className="text-slate-500 dark:text-slate-400">Tiêu đề:</span>
              <span className="font-medium text-slate-900 dark:text-white">
                {backup.title}
              </span>

              <span className="text-slate-500 dark:text-slate-400">Loại sao lưu:</span>
              <span className="font-medium text-indigo-600 dark:text-indigo-400">
                {backup.backupTypeLabel}
              </span>

              <span className="text-slate-500 dark:text-slate-400">Thời điểm tạo:</span>
              <span className="text-slate-900 dark:text-white">
                {new Date(backup.createdAt).toLocaleString("vi-VN")}
              </span>

              <span className="text-slate-500 dark:text-slate-400">Dung lượng:</span>
              <span className="text-slate-900 dark:text-white font-mono">
                {backup.formattedFileSize}
              </span>

              <span className="text-slate-500 dark:text-slate-400">Mã Checksum SHA-256:</span>
              <span className="font-mono text-slate-600 dark:text-slate-400 truncate max-w-[200px]" title={backup.checksum}>
                {backup.checksum || "N/A"}
              </span>
            </div>
          </div>
        </div>
      ) : (
        <form id="restore-form" onSubmit={handleRestoreSubmit} className="space-y-4">
          <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 rounded-lg text-xs text-red-800 dark:text-red-300">
            Để phòng ngừa thao tác nhầm lẫn, vui lòng gõ chính xác <strong>RESTORE</strong> vào ô bên dưới và nhập lý do giải trình.
          </div>

          <FormField id="restore-confirmation-code" label="Mã xác nhận bảo mật">
            <input
              type="text"
              className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm font-mono tracking-wider focus:ring-2 focus:ring-red-500 focus:outline-none"
              placeholder="Gõ từ RESTORE để xác nhận"
              value={confirmationCode}
              onChange={(e) => setConfirmationCode(e.target.value)}
              disabled={isRestoring}
              autoFocus
              required
            />
          </FormField>

          <FormField id="restore-reason" label="Lý do giải trình phục hồi">
            <textarea
              className="w-full px-3 py-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-sm focus:ring-2 focus:ring-red-500 focus:outline-none"
              rows={3}
              placeholder="Giải trình lý do cần phục hồi (tối thiểu 10 ký tự, ví dụ: Khắc phục sự cố sai lệch kế hoạch phân bổ...)"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              disabled={isRestoring}
              required
            />
          </FormField>
        </form>
      )}
    </Dialog>
  );
}
