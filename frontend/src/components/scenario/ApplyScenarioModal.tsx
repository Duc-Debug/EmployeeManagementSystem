"use client";

import React, { useState, useEffect, useCallback } from "react";
import {
  X,
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  ArrowRight,
  ShieldAlert,
  Users,
  Briefcase,
  Layers,
} from "lucide-react";
import {
  getScenarioApplyPreview,
  applyScenarioToRealAllocations,
  refreshScenarioBaseline,
  type ApplyScenarioPreviewResult,
  type ScenarioResult,
} from "@/lib/api/simulation-scenarios";
import { getProjects, type ProjectResult } from "@/lib/api/projects";
import { cn } from "@/lib/utils";

interface ApplyScenarioModalProps {
  isOpen: boolean;
  onClose: () => void;
  scenario: ScenarioResult;
  onSuccess: () => void;
}

export const ApplyScenarioModal: React.FC<ApplyScenarioModalProps> = ({
  isOpen,
  onClose,
  scenario,
  onSuccess,
}) => {
  const [projects, setProjects] = useState<ProjectResult[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<number | "">("");
  const [loadingProjects, setLoadingProjects] = useState(false);

  const [preview, setPreview] = useState<ApplyScenarioPreviewResult | null>(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [previewError, setPreviewError] = useState<string | null>(null);

  const [note, setNote] = useState("");
  const [isApplying, setIsApplying] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [applyError, setApplyError] = useState<string | null>(null);

  // Load available projects
  useEffect(() => {
    if (!isOpen) return;
    setLoadingProjects(true);
    getProjects(0, 100)
      .then((res) => {
        // Filter projects by orgUnit if relevant, or active/planned projects
        const available = res.content.filter(
          (p) => p.status !== "CLOSED" && p.status !== "CANCELLED"
        );
        setProjects(available);
        if (available.length > 0 && !selectedProjectId) {
          setSelectedProjectId(available[0].id);
        }
      })
      .catch((err: unknown) => {
        console.error("Failed to load projects", err);
      })
      .finally(() => {
        setLoadingProjects(false);
      });
  }, [isOpen]);

  // Load preview whenever selectedProjectId changes
  const loadPreview = useCallback(async (projId: number) => {
    setLoadingPreview(true);
    setPreviewError(null);
    try {
      const data = await getScenarioApplyPreview(scenario.id, projId);
      setPreview(data);
    } catch (err: unknown) {
      setPreview(null);
      setPreviewError(err instanceof Error ? err.message : "Không thể tải dữ liệu đối chiếu áp dụng kịch bản.");
    } finally {
      setLoadingPreview(false);
    }
  }, [scenario.id]);

  useEffect(() => {
    if (selectedProjectId && typeof selectedProjectId === "number") {
      loadPreview(selectedProjectId);
    }
  }, [selectedProjectId, loadPreview]);

  const handleRefreshBaseline = async () => {
    if (isRefreshing) return;
    setIsRefreshing(true);
    setApplyError(null);
    try {
      await refreshScenarioBaseline(scenario.id);
      if (selectedProjectId && typeof selectedProjectId === "number") {
        await loadPreview(selectedProjectId);
      }
    } catch (err: unknown) {
      setApplyError(err instanceof Error ? err.message : "Làm mới dữ liệu snapshot thất bại.");
    } finally {
      setIsRefreshing(false);
    }
  };

  const handleApply = async () => {
    if (!selectedProjectId || typeof selectedProjectId !== "number") {
      setApplyError("Vui lòng chọn một dự án mục tiêu.");
      return;
    }
    if (preview?.isBaselineStale) {
      setApplyError("Dữ liệu phân bổ thật đã thay đổi. Vui lòng làm mới snapshot kịch bản trước khi áp dụng.");
      return;
    }

    setIsApplying(true);
    setApplyError(null);
    try {
      await applyScenarioToRealAllocations(scenario.id, {
        targetProjectId: selectedProjectId,
        note: note.trim() || undefined,
      });
      onSuccess();
      onClose();
    } catch (err: unknown) {
      setApplyError(err instanceof Error ? err.message : "Áp dụng kịch bản vào phân bổ thật thất bại.");
    } finally {
      setIsApplying(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-5xl max-h-[92vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center space-x-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <Layers className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h3 className="text-base font-bold text-slate-900">
                  Áp dụng kịch bản vào phân bổ thật
                </h3>
                <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-100">
                  {scenario.code}
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                Chuyển đổi số giờ mô phỏng sang dữ liệu phân bổ thật trên dự án mục tiêu (QTN-14).
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="flex h-8 w-8 items-center justify-center rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Step 1: Chọn Dự án mục tiêu */}
          <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <label className="text-xs font-bold text-slate-800 flex items-center">
                <Briefcase className="h-4 w-4 mr-1.5 text-indigo-600" />
                1. Chọn Dự án Mục Tiêu tiếp nhận phân bổ
              </label>
              <span className="text-[11px] text-slate-500">
                Chỉ hiển thị các dự án đang hoạt động / kế hoạch trong bộ phận
              </span>
            </div>

            {loadingProjects ? (
              <div className="text-xs text-slate-500 py-2 flex items-center space-x-2">
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-indigo-600 border-t-transparent" />
                <span>Đang tải danh sách dự án...</span>
              </div>
            ) : (
              <select
                value={selectedProjectId}
                onChange={(e) => setSelectedProjectId(Number(e.target.value))}
                className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-xs font-semibold text-slate-800 focus:border-indigo-600 focus:outline-hidden focus:ring-1 focus:ring-indigo-600 shadow-xs"
              >
                <option value="" disabled>-- Chọn dự án mục tiêu --</option>
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    [{p.projectCode}] {p.projectName} ({p.status})
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Stale Baseline Warning (TC-02) */}
          {preview?.isBaselineStale && (
            <div className="rounded-2xl border border-amber-300 bg-amber-50 p-4 text-amber-900 space-y-2.5 shadow-xs">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-start space-x-2.5">
                  <AlertTriangle className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
                  <div>
                    <h4 className="text-xs font-bold text-amber-900">
                      Cảnh báo dữ liệu gốc đã thay đổi (TC-02)
                    </h4>
                    <p className="text-[11px] text-amber-800 mt-0.5">
                      Dữ liệu phân bổ thật của các nhân sự trong bộ phận đã bị chỉnh sửa sau thời điểm khởi tạo kịch bản.
                      Bạn cần làm mới snapshot kịch bản để đồng bộ dữ liệu mới nhất trước khi áp dụng.
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={handleRefreshBaseline}
                  disabled={isRefreshing}
                  className="rounded-xl bg-amber-600 hover:bg-amber-700 text-white px-3 py-1.5 text-xs font-bold transition flex items-center space-x-1.5 shrink-0 shadow-xs disabled:opacity-50"
                >
                  <RefreshCw className={cn("h-3.5 w-3.5", isRefreshing && "animate-spin")} />
                  <span>{isRefreshing ? "Đang đồng bộ..." : "Làm mới snapshot kịch bản"}</span>
                </button>
              </div>

              {preview.staleReasons && preview.staleReasons.length > 0 && (
                <div className="bg-white/80 p-2.5 rounded-xl border border-amber-200 text-[11px] text-amber-900 max-h-24 overflow-y-auto space-y-1">
                  <div className="font-semibold text-amber-950">Các điểm sai lệch phát hiện được:</div>
                  <ul className="list-disc pl-4 space-y-0.5 text-slate-700">
                    {preview.staleReasons.map((reason, idx) => (
                      <li key={idx}>{reason}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          {/* Step 2: Bảng so sánh thay đổi (TC-01) */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-800 flex items-center">
                <Users className="h-4 w-4 mr-1.5 text-indigo-600" />
                2. Bảng Đối Chiếu Thay Đổi Phân Bổ (Comparison Table)
              </label>
              {preview && (
                <div className="text-[11px] text-slate-500 font-medium">
                  {preview.affectedEmployeesCount} nhân sự được phân bổ thêm &bull; Tổng cộng:{" "}
                  <strong className="text-indigo-600">{preview.totalAdditionalHours}h</strong>
                </div>
              )}
            </div>

            {loadingPreview ? (
              <div className="h-48 rounded-2xl border border-slate-200 bg-slate-50 flex items-center justify-center">
                <div className="flex flex-col items-center space-y-2">
                  <div className="h-6 w-6 animate-spin rounded-full border-2 border-indigo-600 border-t-transparent" />
                  <span className="text-xs text-slate-500">Đang tính toán bảng đối chiếu...</span>
                </div>
              </div>
            ) : previewError ? (
              <div className="p-4 rounded-2xl border border-rose-200 bg-rose-50 text-rose-800 text-xs">
                {previewError}
              </div>
            ) : preview && preview.employeeComparisons.length > 0 ? (
              <div className="overflow-x-auto rounded-2xl border border-slate-200 shadow-xs">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-slate-100/80 text-slate-700 border-b border-slate-200 text-[11px] font-bold">
                      <th className="p-3 min-w-[200px] sticky left-0 bg-slate-100 z-10">Nhân sự &amp; Chức danh</th>
                      {preview.weeks.map((w) => (
                        <th key={`${w.year}_${w.weekNumber}`} className="p-3 min-w-[130px] text-center border-l border-slate-200">
                          {w.weekLabel}
                        </th>
                      ))}
                      <th className="p-3 min-w-[100px] text-center border-l border-slate-200">Tổng giờ thêm</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 bg-white">
                    {preview.employeeComparisons.map((emp) => (
                      <tr
                        key={emp.employeeId}
                        className={cn(
                          "hover:bg-slate-50/70 transition",
                          emp.totalScenarioHours > 0 ? "bg-indigo-50/20" : ""
                        )}
                      >
                        <td className="p-3 sticky left-0 bg-white z-10 shadow-[1px_0_0_0_#e2e8f0]">
                          <div className="font-bold text-slate-900">{emp.employeeName}</div>
                          <div className="text-[10px] text-slate-500 flex items-center space-x-1.5 mt-0.5">
                            <span className="font-mono bg-slate-100 px-1 rounded">{emp.employeeCode}</span>
                            <span>&bull;</span>
                            <span className="truncate max-w-[130px]">{emp.professionalRole || "Chưa rõ vai trò"}</span>
                          </div>
                        </td>

                        {emp.weeklyCells.map((cell) => {
                          const hasAdditional = cell.scenarioAdditionalHours > 0;
                          return (
                            <td
                              key={`${cell.year}_${cell.weekNumber}`}
                              className={cn(
                                "p-2.5 text-center border-l border-slate-200 align-top",
                                cell.isOverloaded ? "bg-rose-50/70" : hasAdditional ? "bg-indigo-50/40" : ""
                              )}
                            >
                              <div className="space-y-1">
                                {hasAdditional ? (
                                  <div className="flex items-center justify-center space-x-1 font-mono text-[11px]">
                                    <span className="text-slate-400 line-through">
                                      {cell.currentProjectHours}h
                                    </span>
                                    <ArrowRight className="h-3 w-3 text-slate-400" />
                                    <span className="font-bold text-indigo-700 bg-indigo-100/80 px-1.5 py-0.2 rounded">
                                      {cell.newProjectHours}h
                                    </span>
                                  </div>
                                ) : (
                                  <div className="text-slate-400 text-[11px] font-mono">
                                    {cell.currentProjectHours}h
                                  </div>
                                )}

                                <div className="text-[10px] text-slate-500">
                                  Toàn cty: <span className="font-semibold text-slate-700">{cell.newTotalAllocatedHours}h</span>/{cell.availableHours}h
                                </div>

                                {cell.isOverloaded && (
                                  <span className="inline-flex items-center px-1.5 py-0.2 rounded text-[9px] font-bold bg-rose-100 text-rose-700 border border-rose-200">
                                    <AlertTriangle className="h-2.5 w-2.5 mr-0.5" /> Quá tải
                                  </span>
                                )}
                              </div>
                            </td>
                          );
                        })}

                        <td className="p-3 text-center border-l border-slate-200 font-mono font-bold text-indigo-600 bg-indigo-50/20">
                          {emp.totalScenarioHours > 0 ? `+${emp.totalScenarioHours}h` : "--"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="p-6 text-center text-xs text-slate-500 border border-dashed border-slate-300 rounded-2xl">
                Không tìm thấy nhân sự nào thuộc phạm vi kịch bản này.
              </div>
            )}
          </div>

          {/* Ghi chú áp dụng */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-800">
              Ghi chú áp dụng (Audit Trail)
            </label>
            <input
              type="text"
              placeholder="VD: Áp dụng sau khi ký kết hợp đồng chính thức ngày 15/09..."
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2 text-xs text-slate-800 focus:border-indigo-600 focus:outline-hidden focus:ring-1 focus:ring-indigo-600 shadow-xs"
            />
          </div>

          {/* Error banner */}
          {applyError && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-rose-800 text-xs flex items-center space-x-2">
              <ShieldAlert className="h-4 w-4 shrink-0 text-rose-600" />
              <span>{applyError}</span>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-slate-100 bg-slate-50/80">
          <div className="text-[11px] text-slate-500">
            Thao tác này sẽ cập nhật dữ liệu thật và chuyển trạng thái kịch bản sang <strong>applied</strong>.
          </div>

          <div className="flex items-center space-x-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isApplying}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-xs"
            >
              Hủy bỏ
            </button>
            <button
              type="button"
              onClick={handleApply}
              disabled={isApplying || !selectedProjectId || preview?.isBaselineStale || loadingPreview}
              className={cn(
                "rounded-xl px-5 py-2 text-xs font-bold text-white transition shadow-xs flex items-center space-x-1.5",
                preview?.isBaselineStale
                  ? "bg-slate-400 cursor-not-allowed"
                  : "bg-indigo-600 hover:bg-indigo-700"
              )}
            >
              <CheckCircle2 className="h-4 w-4" />
              <span>{isApplying ? "Đang áp dụng..." : "Xác nhận áp dụng vào phân bổ thật"}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
