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
import { cn } from "@/lib/utils";

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
      setError("Vui lòng nhập chính xác từ khóa 'RESTORE' để xác nhận phục hồi.");
      return;
    }
    if (reason.trim().length < 10) {
      setError("Lý do giải trình phục hồi phải có độ dài tối thiểu 10 ký tự.");
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
      title="Quy trình Phục hồi Dữ liệu An toàn"
      description="Khôi phục trạng thái cơ sở dữ liệu về thời điểm bản sao lưu đã chọn qua 2 bước xác thực."
      footer={
        <div className="flex justify-between items-center w-full">
          {step === 2 ? (
            <button
              type="button"
              className="px-3 py-1.5 text-slate-600 hover:text-slate-900 text-xs font-semibold flex items-center gap-1.5 transition cursor-pointer"
              onClick={handlePrevStep}
              disabled={isRestoring}
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Quay lại Bước 1</span>
            </button>
          ) : (
            <div />
          )}

          <div className="flex items-center gap-2">
            <button
              type="button"
              className="px-3.5 py-1.5 border border-slate-200 text-slate-700 rounded-xl hover:bg-slate-50 text-xs font-semibold transition cursor-pointer shadow-2xs"
              onClick={handleClose}
              disabled={isRestoring}
            >
              Hủy bỏ
            </button>

            {step === 1 ? (
              <button
                type="button"
                className="px-4 py-1.5 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition shadow-xs cursor-pointer"
                onClick={handleNextStep}
              >
                <span>Tiếp tục Bước 2</span>
                <ArrowRight className="w-3.5 h-3.5" />
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
                className="px-4 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition shadow-xs disabled:opacity-50 cursor-pointer"
              >
                {isRestoring ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Đang tiến hành phục hồi...</span>
                  </>
                ) : (
                  <>
                    <RotateCcw className="w-3.5 h-3.5" />
                    <span>Thực hiện Phục hồi ngay</span>
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      }
    >
      {/* Step Progress Indicators */}
      <div className="flex items-center gap-2 mb-4">
        <div
          className={cn(
            "flex-1 flex items-center gap-2 p-2 rounded-lg border text-xs font-medium transition",
            step === 1
              ? "bg-amber-50/80 border-amber-200 text-amber-900"
              : "bg-emerald-50/60 border-emerald-200 text-emerald-800"
          )}
        >
          <div
            className={cn(
              "flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] font-bold",
              step === 1 ? "bg-amber-600 text-white" : "bg-emerald-600 text-white"
            )}
          >
            {step === 1 ? "1" : <CheckCircle2 className="w-3.5 h-3.5" />}
          </div>
          <span className="truncate">Bước 1: Cảnh báo & Điểm an toàn</span>
        </div>

        <div
          className={cn(
            "flex-1 flex items-center gap-2 p-2 rounded-lg border text-xs font-medium transition",
            step === 2
              ? "bg-rose-50/80 border-rose-200 text-rose-900"
              : "bg-slate-50 border-slate-200 text-slate-400"
          )}
        >
          <div
            className={cn(
              "flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] font-bold",
              step === 2 ? "bg-rose-600 text-white" : "bg-slate-200 text-slate-500"
            )}
          >
            2
          </div>
          <span className="truncate">Bước 2: Xác nhận & Giải trình</span>
        </div>
      </div>

      {error && (
        <div className="mb-3.5 p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-900 text-xs flex items-start gap-2">
          <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {step === 1 ? (
        <div className="space-y-3">
          <div className="p-3.5 bg-amber-50/70 border border-amber-200 rounded-xl">
            <div className="flex items-start gap-2.5">
              <ShieldAlert className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
              <div className="space-y-1">
                <h4 className="font-bold text-amber-900 text-xs">
                  Cảnh báo ghi đè trạng thái cơ sở dữ liệu
                </h4>
                <p className="text-[11px] text-amber-800 leading-relaxed">
                  Thao tác phục hồi sẽ <strong>ghi đè toàn bộ dữ liệu</strong> hiện tại về đúng thời điểm tạo của bản sao lưu được chọn. Các dữ liệu kế hoạch và phân bổ phát sinh sau thời điểm sao lưu sẽ bị thay thế.
                </p>
                <div className="pt-1.5 flex items-center gap-1.5 text-[11px] text-emerald-800 font-semibold">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Hệ thống tự động tạo snapshot an toàn (SAFETY-PRE-RESTORE-...) trước khi ghi đè.</span>
                </div>
              </div>
            </div>
          </div>

          <div className="border border-slate-200 rounded-xl p-3 bg-slate-50/50 space-y-2 text-xs">
            <h5 className="font-bold text-slate-800 text-[11px] uppercase tracking-wider">
              Thông tin bản sao lưu nguồn
            </h5>
            <div className="grid grid-cols-2 gap-y-1.5 text-xs">
              <span className="text-slate-500">Mã bản sao:</span>
              <span className="font-mono font-bold text-slate-900">{backup.backupCode}</span>

              <span className="text-slate-500">Tiêu đề:</span>
              <span className="font-semibold text-slate-900">{backup.title}</span>

              <span className="text-slate-500">Loại sao lưu:</span>
              <span className="font-semibold text-indigo-700">{backup.backupTypeLabel}</span>

              <span className="text-slate-500">Thời điểm tạo:</span>
              <span className="text-slate-900 font-medium">
                {new Date(backup.createdAt).toLocaleString("vi-VN")}
              </span>

              <span className="text-slate-500">Dung lượng:</span>
              <span className="font-mono font-semibold text-slate-900">{backup.formattedFileSize}</span>

              <span className="text-slate-500">Mã Checksum SHA-256:</span>
              <span className="font-mono text-slate-600 truncate max-w-[180px]" title={backup.checksum}>
                {backup.checksum || "N/A"}
              </span>
            </div>
          </div>
        </div>
      ) : (
        <form id="restore-form" onSubmit={handleRestoreSubmit} className="space-y-3">
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-900 leading-relaxed">
            Nhằm tránh thao tác nhầm lẫn, vui lòng nhập chính xác từ khóa <strong className="font-mono text-rose-950 font-bold">RESTORE</strong> vào ô bên dưới và nêu lý do giải trình phục hồi.
          </div>

          <FormField id="restore-confirmation-code" label="Từ khóa xác nhận bảo mật (Gõ RESTORE)">
            <input
              type="text"
              className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs font-mono tracking-wider focus:ring-2 focus:ring-rose-500 focus:outline-none placeholder:text-slate-400 font-bold"
              placeholder="Gõ chính xác từ khóa RESTORE"
              value={confirmationCode}
              onChange={(e) => setConfirmationCode(e.target.value)}
              disabled={isRestoring}
              autoFocus
              required
            />
          </FormField>

          <FormField id="restore-reason" label="Lý do giải trình phục hồi (Tối thiểu 10 ký tự)">
            <textarea
              className="w-full px-3 py-1.5 border border-slate-200 rounded-lg bg-white text-slate-900 text-xs focus:ring-2 focus:ring-rose-500 focus:outline-none placeholder:text-slate-400"
              rows={3}
              placeholder="Ví dụ: Khắc phục sự cố xung đột kế hoạch phân bổ nguồn lực tuần 38..."
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

export default TwoStepRestoreModal;
