"use client";

import React, { useState, useEffect } from "react";
import {
  ShieldAlert,
  Clock,
  Calendar,
  RefreshCw,
  AlertTriangle,
  X,
  CheckCircle2,
  Sliders,
} from "lucide-react";
import {
  getNotificationDedupConfig,
  updateNotificationDedupConfig,
  triggerOverloadScan,
  type NotificationDedupConfig,
  type OverloadScanResult,
} from "@/lib/api/notification-dedup";

interface NotificationDedupConfigModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function NotificationDedupConfigModal({
  isOpen,
  onClose,
}: NotificationDedupConfigModalProps) {
  const [config, setConfig] = useState<NotificationDedupConfig | null>(null);
  const [isEnabled, setIsEnabled] = useState<boolean>(true);
  const [dedupWindowDays, setDedupWindowDays] = useState<number>(7);
  const [scanIntervalMinutes, setScanIntervalMinutes] = useState<number>(60);

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [isScanning, setIsScanning] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [scanResult, setScanResult] = useState<OverloadScanResult | null>(null);

  // Modal xác nhận thao tác (TC-04)
  const [showConfirmModal, setShowConfirmModal] = useState<boolean>(false);

  const loadConfig = async () => {
    try {
      setIsLoading(true);
      setErrorMessage(null);
      const res = await getNotificationDedupConfig();
      setConfig(res);
      setIsEnabled(res.isEnabled);
      setDedupWindowDays(res.dedupWindowDays);
      setScanIntervalMinutes(res.scanIntervalMinutes);
    } catch (err: unknown) {
      const error = err as { message?: string; status?: number };
      if (error?.status === 403) {
        setErrorMessage("Từ chối truy cập: Bạn không có quyền quản trị cấu hình chống gửi trùng (Chỉ dành cho VT-06).");
      } else {
        setErrorMessage(error?.message || "Không thể tải cấu hình chống gửi trùng.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      loadConfig();
    } else {
      setErrorMessage(null);
      setSuccessMessage(null);
      setScanResult(null);
      setShowConfirmModal(false);
    }
  }, [isOpen]);

  const handleOpenConfirm = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    if (dedupWindowDays < 1 || dedupWindowDays > 90) {
      setErrorMessage("Cửa sổ chống trùng phải từ 1 đến 90 ngày.");
      return;
    }
    if (scanIntervalMinutes < 5 || scanIntervalMinutes > 1440) {
      setErrorMessage("Chu kỳ quét phải từ 5 đến 1440 phút.");
      return;
    }

    setShowConfirmModal(true);
  };

  const handleConfirmSave = async () => {
    try {
      setIsSaving(true);
      setErrorMessage(null);
      const res = await updateNotificationDedupConfig({
        isEnabled,
        dedupWindowDays,
        scanIntervalMinutes,
      });
      setConfig(res);
      setSuccessMessage("Cập nhật cấu hình chống gửi trùng thành công và đã ghi lịch sử kiểm toán.");
      setShowConfirmModal(false);
    } catch (err: unknown) {
      const error = err as { message?: string };
      setErrorMessage(error?.message || "Có lỗi xảy ra khi cập nhật cấu hình.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleManualScan = async () => {
    try {
      setIsScanning(true);
      setErrorMessage(null);
      setSuccessMessage(null);
      const res = await triggerOverloadScan();
      setScanResult(res);
      setSuccessMessage(
        `Quét hoàn tất: Quá tải ${res.overloadedCount}, gửi mới ${res.newlyAlertedCount}, bỏ qua trùng ${res.skippedDedupCount}, giải phóng ${res.resolvedCount}.`
      );
    } catch (err: unknown) {
      const error = err as { message?: string };
      setErrorMessage(error?.message || "Không thể kích hoạt tác vụ quét quá tải.");
    } finally {
      setIsScanning(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
      <div className="relative w-full max-w-xl rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-indigo-50 text-indigo-600 rounded-xl">
              <Sliders className="h-5 w-5" />
            </div>
            <div>
              <h3 className="font-bold text-slate-800 text-base">Cấu hình Chống gửi trùng thông báo</h3>
              <p className="text-xs text-slate-500">Quản trị quy tắc deduplication & tác vụ nền (NCL-11-CN-003 / QTN-19)</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
            type="button"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-5">
          {errorMessage && (
            <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs">
              <ShieldAlert className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          {successMessage && (
            <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs">
              <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0 mt-0.5" />
              <span>{successMessage}</span>
            </div>
          )}

          {scanResult && (
            <div className="p-3.5 bg-indigo-50/70 border border-indigo-100 rounded-xl text-xs space-y-1 text-indigo-900">
              <span className="font-semibold block mb-1">Kết quả rà soát quá tải chi tiết:</span>
              <p>• Tổng số nhân sự quét: <strong>{scanResult.totalScanned}</strong></p>
              <p>• Số nhân sự quá tải: <strong>{scanResult.overloadedCount}</strong></p>
              <p>• Cảnh báo mới đã gửi: <strong>{scanResult.newlyAlertedCount}</strong></p>
              <p>• Bỏ qua do trùng lặp (Dedup QTN-19): <strong>{scanResult.skippedDedupCount}</strong></p>
              <p>• Đã giải phóng (Thoát quá tải TC-02): <strong>{scanResult.resolvedCount}</strong></p>
            </div>
          )}

          {isLoading ? (
            <div className="py-12 flex flex-col items-center justify-center gap-3 text-slate-400">
              <RefreshCw className="h-6 w-6 animate-spin text-indigo-600" />
              <span className="text-xs">Đang tải cấu hình hệ thống...</span>
            </div>
          ) : (
            <form id="dedupConfigForm" onSubmit={handleOpenConfirm} className="space-y-4">
              {/* Toggle Enable */}
              <div className="flex items-center justify-between p-4 rounded-xl border border-slate-200 bg-slate-50/50">
                <div className="space-y-0.5">
                  <span className="text-sm font-semibold text-slate-800">Cơ chế chống gửi trùng (Dedup Engine)</span>
                  <p className="text-xs text-slate-500">
                    Bật để chặn gửi lại cảnh báo lặp lại cho cùng tuần và cùng đối tượng nhận (QTN-19).
                  </p>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={isEnabled}
                    onChange={(e) => setIsEnabled(e.target.checked)}
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-300 peer-focus:outline-hidden rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
                </label>
              </div>

              {/* Dedup Window Days */}
              <div className="space-y-1.5">
                <label className="block text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Calendar className="h-3.5 w-3.5 text-slate-500" />
                  Cửa sổ lưu giữ khóa chống trùng (ngày)
                </label>
                <input
                  type="number"
                  min={1}
                  max={90}
                  value={dedupWindowDays}
                  onChange={(e) => setDedupWindowDays(Number(e.target.value))}
                  className="w-full px-3 py-2 text-sm rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition"
                  placeholder="7"
                />
                <p className="text-[11px] text-slate-500">
                  Thời gian khóa có hiệu lực trước khi tự động hết hạn (từ 1 đến 90 ngày, mặc định: 7 ngày tương ứng 1 tuần).
                </p>
              </div>

              {/* Scan Interval Minutes */}
              <div className="space-y-1.5">
                <label className="block text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-slate-500" />
                  Chu kỳ tác vụ nền rà soát quá tải (phút)
                </label>
                <input
                  type="number"
                  min={5}
                  max={1440}
                  value={scanIntervalMinutes}
                  onChange={(e) => setScanIntervalMinutes(Number(e.target.value))}
                  className="w-full px-3 py-2 text-sm rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 transition"
                  placeholder="60"
                />
                <p className="text-[11px] text-slate-500">
                  Tần suất chạy ngầm rà soát công suất tuần (từ 5 đến 1440 phút, mặc định: 60 phút mỗi lần).
                </p>
              </div>

              {/* Scan Action Box */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
                <div className="text-xs text-slate-500">
                  {config?.updatedAt && (
                    <span>Lần sửa cuối: {new Date(config.updatedAt).toLocaleString("vi-VN")}</span>
                  )}
                </div>
                <button
                  type="button"
                  onClick={handleManualScan}
                  disabled={isScanning || !isEnabled}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-slate-200 bg-white text-xs font-medium text-slate-700 hover:bg-slate-50 hover:text-slate-900 disabled:opacity-50 transition"
                >
                  <RefreshCw className={`h-3.5 w-3.5 ${isScanning ? "animate-spin" : ""}`} />
                  Quét thử nghiệm ngay
                </button>
              </div>
            </form>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-2.5 px-6 py-4 border-t border-slate-100 bg-slate-50/50">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-200/60 transition"
          >
            Đóng
          </button>
          <button
            type="submit"
            form="dedupConfigForm"
            disabled={isLoading || isSaving}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 shadow-xs disabled:opacity-50 transition"
          >
            Lưu cấu hình
          </button>
        </div>
      </div>

      {/* Modal Xác nhận thao tác (TC-04) */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-60 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-amber-50 text-amber-600">
                <AlertTriangle className="h-6 w-6" />
              </div>
              <div>
                <h4 className="font-bold text-slate-800 text-sm">Xác nhận thay đổi cấu hình</h4>
                <p className="text-xs text-slate-500">Thao tác này sẽ được ghi vào nhật ký kiểm toán hệ thống.</p>
              </div>
            </div>

            <div className="p-3 bg-slate-50 rounded-xl text-xs space-y-1.5 text-slate-600">
              <p>• Trạng thái chống trùng: <strong className="text-slate-800">{isEnabled ? "Bật" : "Tắt"}</strong></p>
              <p>• Cửa sổ chống trùng: <strong className="text-slate-800">{dedupWindowDays} ngày</strong></p>
              <p>• Chu kỳ quét: <strong className="text-slate-800">{scanIntervalMinutes} phút</strong></p>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowConfirmModal(false)}
                disabled={isSaving}
                className="px-3.5 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmSave}
                disabled={isSaving}
                className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 shadow-xs disabled:opacity-50 transition"
              >
                {isSaving && <RefreshCw className="h-3 w-3 animate-spin" />}
                Xác nhận
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
