"use client";

import React, { useState, useEffect } from "react";
import {
  X,
  FileSpreadsheet,
  Calendar,
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  ShieldCheck,
} from "lucide-react";
import {
  exportProjectAllocationExcel,
  validateExportPeriod,
} from "@/lib/api/project-allocation-excel";
import { getIsoWeeksInYear, getIsoWeekDetails } from "@/lib/iso-week";

export interface ProjectAllocationExcelExportModalProps {
  open: boolean;
  projectId: number;
  projectCode?: string;
  projectName?: string;
  initialFromYear?: number;
  initialFromWeek?: number;
  initialToYear?: number;
  initialToWeek?: number;
  onClose: () => void;
  onSuccess?: (filename: string) => void;
}

export function ProjectAllocationExcelExportModal({
  open,
  projectId,
  projectCode,
  projectName,
  initialFromYear,
  initialFromWeek,
  initialToYear,
  initialToWeek,
  onClose,
  onSuccess,
}: ProjectAllocationExcelExportModalProps) {
  const now = new Date();
  const currentIso = getIsoWeekDetails(now);

  const [mode, setMode] = useState<"ALL" | "CUSTOM">("CUSTOM");
  const [fromYear, setFromYear] = useState<number>(initialFromYear ?? currentIso.year);
  const [fromWeek, setFromWeek] = useState<number>(initialFromWeek ?? 1);
  const [toYear, setToYear] = useState<number>(initialToYear ?? currentIso.year);
  const [toWeek, setToWeek] = useState<number>(
    initialToWeek ?? getIsoWeeksInYear(initialToYear ?? currentIso.year)
  );

  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setErrorMsg(null);
      setSuccessMsg(null);
      setIsExporting(false);
      if (initialFromYear) setFromYear(initialFromYear);
      if (initialFromWeek) setFromWeek(initialFromWeek);
      if (initialToYear) setToYear(initialToYear);
      if (initialToWeek) setToWeek(initialToWeek);
    }
  }, [open, initialFromYear, initialFromWeek, initialToYear, initialToWeek]);

  // Điều chỉnh max week khi đổi năm
  useEffect(() => {
    const maxWeek = getIsoWeeksInYear(fromYear);
    if (fromWeek > maxWeek) setFromWeek(maxWeek);
  }, [fromYear, fromWeek]);

  useEffect(() => {
    const maxWeek = getIsoWeeksInYear(toYear);
    if (toWeek > maxWeek) setToWeek(maxWeek);
  }, [toYear, toWeek]);

  if (!open) return null;

  const handleExport = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessMsg(null);

    if (mode === "CUSTOM") {
      const validation = validateExportPeriod(fromYear, fromWeek, toYear, toWeek);
      if (!validation.isValid) {
        setErrorMsg(validation.error || "Khoảng thời gian đã chọn không hợp lệ.");
        return;
      }
    }

    setIsExporting(true);
    try {
      const params = {
        projectId,
        ...(mode === "CUSTOM"
          ? { fromYear, fromWeek, toYear, toWeek, all: false }
          : { all: true }),
      };

      const result = await exportProjectAllocationExcel(params, true);
      const nowStr = new Date().toLocaleTimeString("vi-VN");
      setSuccessMsg(
        `Đã xuất file "${result.filename}" thành công lúc ${nowStr}. Hệ thống đã ghi nhận nhật ký thao tác theo QTN-02.`
      );
      if (onSuccess) {
        onSuccess(result.filename);
      }
    } catch (err) {
      setErrorMsg(
        err instanceof Error
          ? err.message
          : "Không thể xuất báo cáo phân bổ dự án. Vui lòng kiểm tra lại kết nối hoặc phân quyền."
      );
    } finally {
      setIsExporting(false);
    }
  };

  const years = Array.from({ length: 7 }, (_, i) => currentIso.year - 3 + i);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4 animate-in fade-in"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
    >
      <div className="w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl border border-slate-100 animate-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600 border border-emerald-100">
              <FileSpreadsheet className="h-6 w-6" />
            </div>
            <div>
              <h2 id="modal-title" className="text-base font-bold text-slate-900">
                Xuất báo cáo phân bổ ra file Excel
              </h2>
              <p className="text-xs text-slate-500">
                Mã nghiệp vụ: <span className="font-semibold text-slate-700">NCL-10-CN-003</span>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            disabled={isExporting}
            className="rounded-xl p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition disabled:opacity-50"
            aria-label="Đóng"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Thông tin dự án */}
        <div className="mt-4 rounded-2xl bg-slate-50 p-3.5 border border-slate-200/80">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">
            Dự án mục tiêu
          </div>
          <div className="text-sm font-bold text-slate-900">
            {projectCode ? `[${projectCode}] ` : ""}
            {projectName || `Dự án #${projectId}`}
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleExport} className="mt-4 space-y-4">
          {/* Chọn chế độ phạm vi */}
          <div>
            <label className="text-xs font-semibold text-slate-700 block mb-1.5">
              Phạm vi xuất báo cáo
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setMode("CUSTOM")}
                className={`flex items-center justify-center gap-2 rounded-xl py-2 px-3 text-xs font-medium border transition ${
                  mode === "CUSTOM"
                    ? "bg-indigo-50 border-indigo-300 text-indigo-700 font-semibold"
                    : "bg-white border-slate-200 text-slate-600 hover:bg-slate-50"
                }`}
              >
                <Calendar className="h-3.5 w-3.5" />
                Khoảng thời gian cụ thể
              </button>
              <button
                type="button"
                onClick={() => setMode("ALL")}
                className={`flex items-center justify-center gap-2 rounded-xl py-2 px-3 text-xs font-medium border transition ${
                  mode === "ALL"
                    ? "bg-indigo-50 border-indigo-300 text-indigo-700 font-semibold"
                    : "bg-white border-slate-200 text-slate-600 hover:bg-slate-50"
                }`}
              >
                <FileSpreadsheet className="h-3.5 w-3.5" />
                Toàn bộ dữ liệu dự án
              </button>
            </div>
          </div>

          {/* Dải thời gian tùy chỉnh */}
          {mode === "CUSTOM" && (
            <div className="space-y-3 rounded-2xl bg-slate-50/70 p-3.5 border border-slate-200/70">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium text-slate-600 block mb-1">
                    Từ năm
                  </label>
                  <select
                    value={fromYear}
                    onChange={(e) => setFromYear(Number(e.target.value))}
                    disabled={isExporting}
                    className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 shadow-xs focus:border-indigo-500 focus:outline-hidden"
                  >
                    {years.map((y) => (
                      <option key={y} value={y}>
                        Năm {y}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-medium text-slate-600 block mb-1">
                    Từ tuần (Tuần 1 - {getIsoWeeksInYear(fromYear)})
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={getIsoWeeksInYear(fromYear)}
                    value={fromWeek}
                    onChange={(e) => setFromWeek(Number(e.target.value))}
                    disabled={isExporting}
                    className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 shadow-xs focus:border-indigo-500 focus:outline-hidden"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium text-slate-600 block mb-1">
                    Đến năm
                  </label>
                  <select
                    value={toYear}
                    onChange={(e) => setToYear(Number(e.target.value))}
                    disabled={isExporting}
                    className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 shadow-xs focus:border-indigo-500 focus:outline-hidden"
                  >
                    {years.map((y) => (
                      <option key={y} value={y}>
                        Năm {y}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-medium text-slate-600 block mb-1">
                    Đến tuần (Tuần 1 - {getIsoWeeksInYear(toYear)})
                  </label>
                  <input
                    type="number"
                    min={1}
                    max={getIsoWeeksInYear(toYear)}
                    value={toWeek}
                    onChange={(e) => setToWeek(Number(e.target.value))}
                    disabled={isExporting}
                    className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 shadow-xs focus:border-indigo-500 focus:outline-hidden"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Audit note */}
          <div className="flex items-start gap-2 text-xs text-slate-500 bg-slate-50 p-3 rounded-2xl border border-slate-200/50">
            <ShieldCheck className="h-4 w-4 text-slate-400 shrink-0 mt-0.5" />
            <span>
              Thao tác xuất tệp sẽ được ghi nhận vào nhật ký kiểm toán hệ thống (Audit Log) theo quy trình QTN-02.
            </span>
          </div>

          {/* Error Message */}
          {errorMsg && (
            <div className="flex items-start gap-2.5 rounded-2xl bg-rose-50 p-3 text-xs text-rose-700 border border-rose-200 animate-in fade-in">
              <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
              <div className="font-medium">{errorMsg}</div>
            </div>
          )}

          {/* Success Message */}
          {successMsg && (
            <div className="flex items-start gap-2.5 rounded-2xl bg-emerald-50 p-3 text-xs text-emerald-700 border border-emerald-200 animate-in fade-in">
              <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0 mt-0.5" />
              <div className="font-medium">{successMsg}</div>
            </div>
          )}

          {/* Action buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              disabled={isExporting}
              className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition disabled:opacity-50"
            >
              Đóng
            </button>
            <button
              type="submit"
              disabled={isExporting}
              className="flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-700 shadow-xs transition disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {isExporting ? (
                <>
                  <RefreshCw className="h-4 w-4 animate-spin" />
                  Đang xuất file Excel...
                </>
              ) : (
                <>
                  <FileSpreadsheet className="h-4 w-4" />
                  Xuất file Excel (.xlsx)
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
