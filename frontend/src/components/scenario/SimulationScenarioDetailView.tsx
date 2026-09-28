"use client";

import React, { useState, useEffect, useCallback } from "react";
import {
  ArrowLeft,
  CalendarRange,
  Users,
  AlertTriangle,
  Plus,
  Trash2,
  Edit2,
  ChevronDown,
  ChevronRight,
  ShieldCheck,
  CheckCircle2,
  Clock,
  Briefcase,
  TrendingUp,
  Save,
  Share2,
  Eye,
  FileText,
  Loader2,
  AlertCircle,
  X,
} from "lucide-react";
import {
  getScenarioById,
  getScenarioSimulation,
  deleteScenarioDemand,
  saveScenario,
  patchScenario,
  type ScenarioDetailResult,
  type ScenarioSimulationResult,
  type ScenarioDemandResult,
} from "@/lib/api/simulation-scenarios";
import { AddEditDemandModal } from "./AddEditDemandModal";
import { ApplyScenarioModal } from "./ApplyScenarioModal";
import { ShareScenarioModal } from "./ShareScenarioModal";
import { RecruitmentScenarioSection } from "./recruitment/RecruitmentScenarioSection";
import { useAuthUser } from "@/lib/auth-session";
import { cn } from "@/lib/utils";

interface SimulationScenarioDetailViewProps {
  scenarioId: number;
  onBack: () => void;
}

export const SimulationScenarioDetailView: React.FC<SimulationScenarioDetailViewProps> = ({
  scenarioId,
  onBack,
}) => {
  const user = useAuthUser();
  const normalizedRole = user?.roleCode ? user.roleCode.toUpperCase().replace(/_/g, "-") : "";
  const isVT03 = normalizedRole === "VT-03" || normalizedRole === "ROLE-RM" || normalizedRole === "RM";
  const canReadRecruitment = Boolean(
    user?.permissions?.includes("RESOURCE_RECRUITMENT_SCENARIO_READ") ||
    ["VT-03", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalizedRole)
  ) && normalizedRole !== "VT-01";

  const [detail, setDetail] = useState<ScenarioDetailResult | null>(null);
  const [simulation, setSimulation] = useState<ScenarioSimulationResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [simulationError, setSimulationError] = useState<string | null>(null);
  const [simulationLoading, setSimulationLoading] = useState(false);

  // Demand modal state
  const [isDemandModalOpen, setIsDemandModalOpen] = useState(false);
  const [selectedDemand, setSelectedDemand] = useState<ScenarioDemandResult | null>(null);

  // Apply scenario modal state (NCL-08-CN-003)
  const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);

  // Snapshot table accordion state
  const [isSnapshotExpanded, setIsSnapshotExpanded] = useState(false);

  // Share modal state
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);

  // Save & Patch state
  const [isSaving, setIsSaving] = useState(false);
  const [isEditInfoModalOpen, setIsEditInfoModalOpen] = useState(false);
  const [nameInput, setNameInput] = useState("");
  const [noteInput, setNoteInput] = useState("");
  const [isPatching, setIsPatching] = useState(false);
  const [actionFeedback, setActionFeedback] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    setSimulationError(null);
    const [detailResult, simulationResult] = await Promise.allSettled([
      getScenarioById(scenarioId),
      getScenarioSimulation(scenarioId),
    ]);

    if (detailResult.status === "fulfilled") {
      setDetail(detailResult.value);
    } else {
      setDetail(null);
      setError(detailResult.reason instanceof Error ? detailResult.reason.message : "Không thể tải dữ liệu kịch bản.");
    }

    if (simulationResult.status === "fulfilled") {
      setSimulation(simulationResult.value);
    } else {
      setSimulation(null);
      setSimulationError(
        simulationResult.reason instanceof Error
          ? simulationResult.reason.message
          : "Không thể tải kết quả mô phỏng."
      );
    }
    setLoading(false);
  }, [scenarioId]);

  const retrySimulation = useCallback(async () => {
    setSimulationLoading(true);
    setSimulationError(null);
    try {
      setSimulation(await getScenarioSimulation(scenarioId));
    } catch (err: unknown) {
      setSimulationError(err instanceof Error ? err.message : "Không thể tải kết quả mô phỏng.");
    } finally {
      setSimulationLoading(false);
    }
  }, [scenarioId]);

  useEffect(() => {
    // Initial remote-data synchronization; state updates happen inside the async loader.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  const handleDeleteDemand = async (demandId: number) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa nhu cầu giả định này?")) return;
    try {
      await deleteScenarioDemand(scenarioId, demandId);
      await loadData();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : "Xóa nhu cầu thất bại.");
    }
  };

  const handleSaveScenario = async () => {
    setIsSaving(true);
    setActionFeedback(null);
    try {
      await saveScenario(scenarioId);
      setActionFeedback({
        type: "success",
        message: "Đã lưu kịch bản và đóng băng snapshot thành công! Trạng thái kịch bản chuyển sang ĐÃ LƯU.",
      });
      await loadData();
    } catch (err: unknown) {
      setActionFeedback({
        type: "error",
        message: err instanceof Error ? err.message : "Lưu kịch bản thất bại.",
      });
    } finally {
      setIsSaving(false);
    }
  };

  const handleShareClick = async () => {
    if (!detail) return;
    const { scenario } = detail;
    if (scenario.status !== "saved") {
      const confirmSave = window.confirm(
        "Theo quy tắc BR-05: Chỉ có thể chia sẻ kịch bản đã lưu để đảm bảo tính đóng băng của dữ liệu. Bạn có muốn Lưu kịch bản ngay để tiến hành chia sẻ?"
      );
      if (confirmSave) {
        try {
          setIsSaving(true);
          await saveScenario(scenarioId);
          await loadData();
          setIsShareModalOpen(true);
        } catch (err: unknown) {
          alert(err instanceof Error ? err.message : "Không thể lưu kịch bản.");
        } finally {
          setIsSaving(false);
        }
      }
      return;
    }
    setIsShareModalOpen(true);
  };

  const handlePatchInfo = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsPatching(true);
    setActionFeedback(null);
    try {
      await patchScenario(scenarioId, {
        name: nameInput.trim() || undefined,
        note: noteInput.trim() || undefined,
      });
      setIsEditInfoModalOpen(false);
      setActionFeedback({
        type: "success",
        message: "Cập nhật thông tin thành công! Kịch bản tự động chuyển về Bản nháp.",
      });
      await loadData();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : "Cập nhật thông tin kịch bản thất bại.");
    } finally {
      setIsPatching(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "OVERLOADED":
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 border border-rose-200">
            <AlertTriangle className="h-3 w-3 mr-1 text-rose-600" />
            Quá tải
          </span>
        );
      case "OPTIMAL":
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="h-3 w-3 mr-1 text-emerald-600" />
            Tối ưu
          </span>
        );
      case "AVAILABLE":
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-sky-100 text-sky-800 border border-sky-200">
            Khả dụng
          </span>
        );
      case "UNDERLOADED":
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200">
            Dưới tải
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-800">
            {status}
          </span>
        );
    }
  };

  const formatSnapshotDate = (dateStr: string) => {
    if (!dateStr) return "--:--";
    try {
      const d = new Date(dateStr);
      return d.toLocaleString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch {
      return dateStr;
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="flex flex-col items-center space-y-3">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-indigo-600 border-t-transparent" />
          <p className="text-xs text-slate-500 font-medium">Đang tải dữ liệu mô phỏng kịch bản...</p>
        </div>
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="p-8 max-w-2xl mx-auto text-center space-y-4">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-rose-600 mx-auto">
          <AlertTriangle className="h-6 w-6" />
        </div>
        <h3 className="text-base font-bold text-slate-900">Không thể tải kịch bản</h3>
        <p className="text-xs font-medium text-slate-600 bg-rose-50/50 p-3 rounded-xl border border-rose-100 max-w-md mx-auto">
          {error || "Kịch bản không tồn tại hoặc đã bị xóa khỏi hệ thống."}
        </p>
        <div className="flex items-center justify-center space-x-3 pt-2">
          <button
            onClick={loadData}
            className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition shadow-xs"
          >
            Thử tải lại
          </button>
          <button
            onClick={onBack}
            className="rounded-xl bg-slate-900 px-4 py-2 text-xs font-semibold text-white hover:bg-slate-800 transition shadow-xs"
          >
            Quay lại danh sách
          </button>
        </div>
      </div>
    );
  }

  const { scenario, demands } = detail;
  const isViewOnly = scenario.viewMode === "VIEW_ONLY";
  const isOwner = !isViewOnly && isVT03;
  const isSaved = scenario.status === "saved";
  const isDraft = scenario.status === "draft";
  const canEdit = isOwner;

  // Calculation summaries
  const totalSnapshotHours = simulation?.weeklyMetrics.reduce((sum, m) => sum + m.snapshotAllocatedHours, 0) || 0;
  const totalDemandHours = simulation?.weeklyMetrics.reduce((sum, m) => sum + m.demandHours, 0) || 0;
  const totalWorkloadHours = simulation?.weeklyMetrics.reduce((sum, m) => sum + (m.scenarioWorkloadHours ?? m.totalWorkloadHours ?? 0), 0) || 0;
  const totalAvailableCapacity = simulation?.weeklyMetrics.reduce((sum, m) => sum + (m.availableHours ?? m.availableCapacityHours ?? 0), 0) || 0;
  const avgUtilization = totalAvailableCapacity > 0 ? (totalWorkloadHours / totalAvailableCapacity) * 100 : 0;

  return (
    <div className="space-y-6 pb-12 animate-in fade-in duration-150">
      {/* Top Header Bar */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div className="flex items-center space-x-3">
          <button
            onClick={onBack}
            className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 transition shrink-0"
            title="Quay lại danh sách"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md border border-indigo-100">
                {scenario.code}
              </span>
              <h2 className="text-lg font-bold text-slate-900">{scenario.name}</h2>
              {isViewOnly ? (
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-sky-50 text-sky-700 border border-sky-200">
                  <Eye className="h-3 w-3 mr-1 text-sky-600" />
                  Chỉ xem
                </span>
              ) : scenario.status === "applied" ? (
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                  <CheckCircle2 className="h-3 w-3 mr-1 text-emerald-600" />
                  Đã áp dụng
                </span>
              ) : isSaved ? (
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                  <CheckCircle2 className="h-3 w-3 mr-1 text-emerald-600" />
                  Đã lưu
                </span>
              ) : (
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                  <Clock className="h-3 w-3 mr-1 text-amber-600" />
                  Bản nháp
                </span>
              )}
            </div>
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-1 text-xs text-slate-500">
              <span className="flex items-center">
                <Briefcase className="h-3.5 w-3.5 mr-1 text-slate-400" />
                {scenario.orgUnitName}
              </span>
              <span className="flex items-center">
                <CalendarRange className="h-3.5 w-3.5 mr-1 text-slate-400" />
                Tuần {scenario.fromWeek ?? scenario.startWeek} - Tuần {(scenario.fromWeek ?? scenario.startWeek ?? 1) + scenario.durationWeeks - 1}, Năm {scenario.fromYear ?? scenario.startYear} ({scenario.durationWeeks} tuần)
              </span>
              <span className="flex items-center">
                <Users className="h-3.5 w-3.5 mr-1 text-slate-400" />
                {scenario.snapshotEmployeesCount ?? scenario.totalSnapshotEmployees ?? 0} nhân sự trong snapshot
              </span>
              {scenario.status === "applied" && scenario.appliedAt && (
                <span className="flex items-center text-emerald-700 font-medium">
                  <Clock className="h-3.5 w-3.5 mr-1 text-emerald-600" />
                  Áp dụng lúc: {formatSnapshotDate(scenario.appliedAt)}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Action button toolbar */}
        <div className="flex flex-wrap items-center gap-2">
          {canEdit && (
            <>
              {/* Edit Info / Note Button */}
              <button
                onClick={() => setIsEditInfoModalOpen(true)}
                className="rounded-xl border border-slate-200 hover:bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-700 transition flex items-center space-x-1.5 shadow-xs"
                title="Sửa tên và ghi chú kịch bản"
              >
                <Edit2 className="h-3.5 w-3.5 text-slate-500" />
                <span>Sửa thông tin</span>
              </button>

              {/* Save Scenario Button */}
              <button
                disabled={isSaving}
                onClick={handleSaveScenario}
                className={cn(
                  "rounded-xl px-3.5 py-2 text-xs font-semibold transition shadow-xs flex items-center space-x-1.5 disabled:opacity-50",
                  isSaved
                    ? "bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100"
                    : "bg-emerald-600 hover:bg-emerald-700 text-white"
                )}
                title={isSaved ? "Bấm để lưu lại ảnh chụp mới nhất" : "Lưu kịch bản và đóng băng snapshot"}
              >
                {isSaving ? (
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                ) : isSaved ? (
                  <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                ) : (
                  <Save className="h-3.5 w-3.5" />
                )}
                <span>{isSaved ? "Đã lưu (Lưu lại)" : "Lưu kịch bản"}</span>
              </button>

              {/* Share Scenario Button (BR-05) */}
              <button
                onClick={handleShareClick}
                className={cn(
                  "rounded-xl px-3.5 py-2 text-xs font-semibold transition shadow-xs flex items-center space-x-1.5",
                  isSaved
                    ? "bg-indigo-600 hover:bg-indigo-700 text-white"
                    : "bg-indigo-50 text-indigo-700 border border-indigo-200 hover:bg-indigo-100"
                )}
                title={isDraft ? "Cần lưu kịch bản trước khi chia sẻ (BR-05)" : "Chia sẻ kịch bản cho các vai trò liên quan"}
              >
                <Share2 className="h-3.5 w-3.5" />
                <span>Chia sẻ</span>
              </button>

              {/* Apply Scenario Button (NCL-08-CN-003) */}
              <button
                onClick={() => setIsApplyModalOpen(true)}
                className="rounded-xl bg-emerald-600 hover:bg-emerald-700 px-3.5 py-2 text-xs font-semibold text-white transition shadow-xs flex items-center space-x-1.5"
                title="Áp dụng các phân bổ từ kịch bản này vào dự án thật (NCL-08-CN-003)"
              >
                <CheckCircle2 className="h-3.5 w-3.5" />
                <span>Áp dụng vào phân bổ thật</span>
              </button>

              {/* Add Demand Button */}
              <button
                onClick={() => {
                  setSelectedDemand(null);
                  setIsDemandModalOpen(true);
                }}
                className="rounded-xl bg-indigo-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs flex items-center space-x-1.5"
              >
                <Plus className="h-3.5 w-3.5" />
                <span>Thêm nhu cầu</span>
              </button>
            </>
          )}
        </div>
      </div>

      {/* Action feedback message */}
      {actionFeedback && (
        <div
          className={cn(
            "p-3.5 rounded-xl text-xs flex items-center justify-between shadow-xs animate-in fade-in duration-150",
            actionFeedback.type === "success"
              ? "bg-emerald-50 border border-emerald-200 text-emerald-800"
              : "bg-rose-50 border border-rose-200 text-rose-800"
          )}
        >
          <div className="flex items-center space-x-2">
            {actionFeedback.type === "success" ? (
              <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
            ) : (
              <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0" />
            )}
            <span>{actionFeedback.message}</span>
          </div>
          <button
            onClick={() => setActionFeedback(null)}
            className="text-slate-400 hover:text-slate-600 text-xs font-bold px-1"
          >
            ×
          </button>
        </div>
      )}

      {/* VIEW_ONLY Warning Banner (BR-04, BR-06, BR-08) */}
      {isViewOnly && (
        <div className="rounded-2xl bg-amber-50/90 border border-amber-200 p-4 text-amber-900 flex items-start space-x-3 shadow-xs">
          <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-amber-100 text-amber-700 shrink-0 mt-0.5 border border-amber-200">
            <Eye className="h-5 w-5" />
          </div>
          <div className="flex-1 space-y-1 text-xs">
            <div className="font-bold flex items-center space-x-2">
              <span>Chế độ chỉ xem (VIEW_ONLY) — Kịch bản được chia sẻ</span>
              <span className="px-2 py-0.2 rounded-md bg-amber-200/80 text-amber-800 text-[10px] font-mono">
                Người tạo: {scenario.createdByName}
              </span>
            </div>
            <p className="text-amber-800/90 leading-relaxed text-[11px]">
              Bạn đang xem kịch bản ở chế độ chỉ đọc.
            </p>
          </div>
        </div>
      )}

      {/* Snapshot Sandbox Alert Banner (QTN-14 & AC-02) */}
      <div className="rounded-2xl bg-sky-50/80 border border-sky-200 p-4 text-sky-900 flex items-start space-x-3 shadow-xs">
        <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-sky-100 text-sky-700 shrink-0 mt-0.5 border border-sky-200">
          <ShieldCheck className="h-5 w-5" />
        </div>
        <div className="flex-1 space-y-1 text-xs">
          <div className="font-bold flex items-center">
            <span>Dữ liệu mô phỏng</span>
            <span className="ml-2 px-2 py-0.2 rounded-md bg-sky-200/80 text-sky-800 text-[10px] font-mono">
              Snapshot: {formatSnapshotDate(scenario.baseSnapshotAt)}
            </span>
          </div>
          <p className="text-sky-800/90 leading-relaxed text-[11px]">
            Dữ liệu mô phỏng được tính toán dựa trên ảnh chụp phân bổ &amp; khả dụng thật tại thời điểm khởi tạo kịch bản.
          </p>
        </div>
      </div>

      {simulationError && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-amber-900 flex items-center justify-between gap-4">
          <div className="flex items-start space-x-3">
            <AlertCircle className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <p className="text-xs font-bold">Không thể tải kết quả mô phỏng</p>
              <p className="mt-1 text-[11px]">{simulationError}</p>
              <p className="mt-1 text-[11px] text-amber-700">Thông tin kịch bản và nhu cầu vẫn có thể xem hoặc chỉnh sửa.</p>
            </div>
          </div>
          <button
            type="button"
            onClick={retrySimulation}
            disabled={simulationLoading}
            className="shrink-0 rounded-xl border border-amber-300 bg-white px-3 py-2 text-xs font-semibold hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {simulationLoading ? "Đang tải..." : "Tải lại mô phỏng"}
          </button>
        </div>
      )}

      {/* Scenario Note Card */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs flex items-start justify-between gap-3">
        <div className="flex items-start space-x-2.5">
          <FileText className="h-4 w-4 text-slate-400 shrink-0 mt-0.5" />
          <div className="text-xs space-y-0.5">
            <span className="font-bold text-slate-700">Ghi chú kịch bản:</span>
            <p className="text-slate-600 leading-relaxed">
              {scenario.note ? scenario.note : <span className="italic text-slate-400">Chưa có ghi chú nào được thêm vào kịch bản này.</span>}
            </p>
          </div>
        </div>
        {isOwner && (
          <button
            onClick={() => setIsEditInfoModalOpen(true)}
            className="text-[11px] font-semibold text-indigo-600 hover:text-indigo-700 hover:underline shrink-0 pt-0.5"
          >
            {scenario.note ? "Sửa ghi chú" : "+ Thêm ghi chú"}
          </button>
        )}
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
          <span className="text-[11px] font-medium text-slate-500">Giờ Snapshot (Thật)</span>
          <p className="text-lg font-bold text-slate-900 mt-1 font-mono">{totalSnapshotHours.toLocaleString()}h</p>
          <span className="text-[10px] text-slate-400">Dữ liệu phân bổ cố định</span>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
          <span className="text-[11px] font-medium text-slate-500">Giờ Nhu Cầu Mới</span>
          <p className="text-lg font-bold text-indigo-600 mt-1 font-mono">{totalDemandHours.toLocaleString()}h</p>
          <span className="text-[10px] text-slate-400">{demands.length} nhu cầu giả định</span>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
          <span className="text-[11px] font-medium text-slate-500">Tổng Khối Lượng Mô Phỏng</span>
          <p className="text-lg font-bold text-slate-900 mt-1 font-mono">{totalWorkloadHours.toLocaleString()}h</p>
          <span className="text-[10px] text-slate-400">Snapshot + Nhu cầu</span>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
          <span className="text-[11px] font-medium text-slate-500">Tổng Năng Lực Khả Dụng</span>
          <p className="text-lg font-bold text-emerald-600 mt-1 font-mono">{totalAvailableCapacity.toLocaleString()}h</p>
          <span className="text-[10px] text-slate-400">Net capacity phòng ban</span>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
          <span className="text-[11px] font-medium text-slate-500">Tỷ lệ Sử dụng TB</span>
          <div className="mt-1 flex items-baseline justify-between">
            <span
              className={cn(
                "text-xl font-bold",
                avgUtilization > (simulation?.overloadThreshold || 100)
                  ? "text-rose-600"
                  : avgUtilization < (simulation?.idleThreshold || 70)
                    ? "text-amber-600"
                    : "text-emerald-600"
              )}
            >
              {Number.isFinite(avgUtilization) ? `${avgUtilization.toFixed(1)}%` : "0.0%"}
            </span>
            <span className="text-[10px] text-slate-400">
              Ngưỡng: &gt;{simulation?.overloadThreshold || 100}%
            </span>
          </div>
        </div>
      </div>

      {/* FEATURE NCL-08-CN-002: Danh sách Nhân sự Vượt Năng Lực / Vỡ Kế Hoạch */}
      <div className="bg-white rounded-2xl border border-rose-200 shadow-xs overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-rose-100 bg-rose-50/40">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="h-4 w-4 text-rose-600" />
            <h3 className="text-sm font-bold text-slate-900">
              Danh Sách Nhân Sự Vượt Năng Lực / Vỡ Kế Hoạch
            </h3>
            <span
              className={cn(
                "text-xs px-2 py-0.5 rounded-full font-bold",
                (simulation?.overloadedEmployees ?? []).length > 0
                  ? "bg-rose-100 text-rose-700 border border-rose-200"
                  : "bg-emerald-100 text-emerald-700 border border-emerald-200"
              )}
            >
              {(simulation?.overloadedEmployees ?? []).length} nhân sự
            </span>
          </div>
          <span className="text-[11px] text-slate-500 font-medium">
            Phân tích nhân sự bị vỡ kế hoạch / vượt năng lực khi áp dụng kịch bản mô phỏng
          </span>
        </div>

        {(simulation?.overloadedEmployees ?? []).length === 0 ? (
          <div className="p-6 text-center space-y-2 bg-emerald-50/20">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600 mx-auto">
              <CheckCircle2 className="h-5 w-5" />
            </div>
            <p className="text-xs font-bold text-emerald-800">
              Không có nhân sự nào bị vỡ kế hoạch / vượt năng lực trong kịch bản mô phỏng
            </p>
            <p className="text-[11px] text-slate-500">
              Khối lượng công việc mô phỏng của tất cả nhân sự trong đơn vị (Phân bổ gốc + Nhu cầu kịch bản) đều nằm trong định mức chuẩn.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-rose-100 bg-rose-50/30 text-[11px] font-semibold text-slate-600 uppercase tracking-wider">
                  <th className="px-4 py-3">Mã NV</th>
                  <th className="px-4 py-3">Họ và Tên</th>
                  <th className="px-4 py-3">Chức danh</th>
                  <th className="px-4 py-3 text-center">Tuần vi phạm</th>
                  <th className="px-4 py-3 text-right">Phân bổ / Chuẩn</th>
                  <th className="px-4 py-3 text-right font-bold text-rose-700">Giờ vượt</th>
                  <th className="px-4 py-3 text-right">Tỷ lệ</th>
                  <th className="px-4 py-3 text-center">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-rose-100/60 text-slate-700">
                {(simulation?.overloadedEmployees ?? []).map((emp, index) => (
                  <tr key={`${emp.employeeId}-${emp.year}-${emp.weekNumber}-${index}`} className="hover:bg-rose-50/40 transition">
                    <td className="px-4 py-3 font-mono font-semibold text-slate-700">{emp.employeeCode}</td>
                    <td className="px-4 py-3 font-bold text-slate-900">{emp.fullName}</td>
                    <td className="px-4 py-3 text-slate-500">{emp.professionalRole || "—"}</td>
                    <td className="px-4 py-3 text-center font-semibold text-slate-800">
                      {emp.weekLabel || `Tuần ${emp.weekNumber} (${emp.year})`}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-slate-700">
                      <span className="font-semibold text-rose-700">{emp.allocatedHours}h</span> / {emp.availableHours}h
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-bold text-rose-600">
                      +{emp.excessHours}h
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-bold text-rose-600">
                      {emp.utilizationPercentage != null ? `${emp.utilizationPercentage.toFixed(1)}%` : "N/A"}
                    </td>
                    <td className="px-4 py-3 text-center">{getStatusBadge(emp.status)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* SECTION 1: Bảng Nhu cầu Giả định (Hypothetical Demands) */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center space-x-2">
            <Users className="h-4 w-4 text-indigo-600" />
            <h3 className="text-sm font-bold text-slate-900">
              Nhu Cầu Nhân Sự Giả Định;
            </h3>
            <span className="rounded-full bg-indigo-50 px-2 py-0.5 text-[11px] font-bold text-indigo-600 border border-indigo-100">
              {demands.length}
            </span>
          </div>
          {canEdit && (
            <button
              onClick={() => {
                setSelectedDemand(null);
                setIsDemandModalOpen(true);
              }}
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 flex items-center space-x-1"
            >
              <Plus className="h-3.5 w-3.5" />
              <span>Thêm vai trò</span>
            </button>
          )}
        </div>

        {demands.length === 0 ? (
          <div className="p-8 text-center space-y-2">
            <Users className="h-8 w-8 text-slate-300 mx-auto" />
            <p className="text-xs text-slate-500 font-medium">Chưa có nhu cầu nhân sự giả định nào trong kịch bản này.</p>
            {canEdit && (
              <button
                onClick={() => {
                  setSelectedDemand(null);
                  setIsDemandModalOpen(true);
                }}
                className="mt-2 rounded-xl bg-indigo-50 px-3 py-1.5 text-xs font-semibold text-indigo-600 hover:bg-indigo-100 transition"
              >
                + Thêm nhu cầu đầu tiên
              </button>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/50 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="px-4 py-3">Vai trò / Chức danh</th>
                  <th className="px-4 py-3 text-center">Số lượng (Headcount)</th>
                  <th className="px-4 py-3 text-center">Thời gian áp dụng</th>
                  <th className="px-4 py-3 text-right">Giờ / tuần / người</th>
                  <th className="px-4 py-3 text-right">Tổng giờ / tuần</th>
                  <th className="px-4 py-3">Kỹ năng yêu cầu</th>
                  {canEdit && <th className="px-4 py-3 text-center">Thao tác</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {demands.map((demand) => (
                  <tr key={demand.id} className="hover:bg-slate-50/70 transition">
                    <td className="px-4 py-3 font-medium text-slate-900">{demand.demandName || demand.roleName}</td>
                    <td className="px-4 py-3 text-center">
                      <span className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100">
                        {demand.headcount} người
                      </span>
                    </td>
                    <td className="px-4 py-3 text-center font-mono">
                      W{demand.startWeek ?? demand.weekStart}/{demand.startYear ?? scenario.fromYear ?? scenario.startYear} → W{demand.endWeek ?? demand.weekEnd}/{demand.endYear ?? scenario.fromYear ?? scenario.startYear}
                    </td>
                    <td className="px-4 py-3 text-right font-mono">{demand.hoursPerWeekPerPerson ?? demand.hoursPerWeek}h</td>
                    <td className="px-4 py-3 text-right font-mono font-bold text-indigo-600">
                      {(demand.headcount * (demand.hoursPerWeekPerPerson ?? demand.hoursPerWeek ?? 0)).toLocaleString()}h
                    </td>
                    <td className="px-4 py-3 text-slate-500">
                      {demand.skillRequirement || demand.requiredSkill ? (
                        <span className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 text-[11px]">
                          {demand.skillRequirement || demand.requiredSkill}
                        </span>
                      ) : (
                        "—"
                      )}
                    </td>
                    {canEdit && (
                      <td className="px-4 py-3 text-center space-x-1">
                        <button
                          onClick={() => {
                            setSelectedDemand(demand);
                            setIsDemandModalOpen(true);
                          }}
                          className="p-1 rounded-lg text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition"
                          title="Chỉnh sửa"
                        >
                          <Edit2 className="h-3.5 w-3.5" />
                        </button>
                        <button
                          onClick={() => handleDeleteDemand(demand.id)}
                          className="p-1 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition"
                          title="Xóa"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* SECTION 2: Kết quả Mô phỏng Năng lực theo Tuần (Simulation Results Matrix) */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center space-x-2">
            <TrendingUp className="h-4 w-4 text-indigo-600" />
            <h3 className="text-sm font-bold text-slate-900">
              Kết Quả Mô Phỏng Năng Lực Theo Tuần
            </h3>
          </div>
          <div className="text-[11px] text-slate-500">
            Công thức: <span className="font-semibold text-slate-700">Khối lượng = Snapshot + Nhu cầu</span>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/50 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                <th className="px-4 py-3">Tuần</th>
                <th className="px-4 py-3 text-right">Giờ Snapshot</th>
                <th className="px-4 py-3 text-right">Giờ Nhu cầu</th>
                <th className="px-4 py-3 text-right font-bold text-slate-900">Khối lượng (Workload)</th>
                <th className="px-4 py-3 text-right">Khả dụng</th>
                <th className="px-4 py-3 text-right">Tỷ lệ Sử dụng</th>
                <th className="px-4 py-3 text-center">Trạng thái Cảnh báo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {simulation?.weeklyMetrics.map((metric) => (
                <tr key={`${metric.year}-${metric.weekNumber}`} className="hover:bg-slate-50/70 transition">
                  <td className="px-4 py-3 font-semibold font-mono text-slate-900">
                    Tuần {metric.weekNumber}, {metric.year}
                  </td>
                  <td className="px-4 py-3 text-right font-mono text-slate-600">{metric.snapshotAllocatedHours}h</td>
                  <td className="px-4 py-3 text-right font-mono text-indigo-600 font-medium">+{metric.demandHours}h</td>
                  <td className="px-4 py-3 text-right font-mono font-bold text-slate-900">
                    {(metric.scenarioWorkloadHours ?? metric.totalWorkloadHours ?? 0)}h
                  </td>
                  <td className="px-4 py-3 text-right font-mono text-emerald-600">
                    {(metric.availableHours ?? metric.availableCapacityHours ?? 0)}h
                  </td>
                  <td className="px-4 py-3 text-right font-mono font-semibold">
                    <span
                      className={cn(
                        "inline-block px-2 py-0.5 rounded-md",
                        metric.utilizationPercentage > (simulation?.overloadThreshold ?? 100)
                          ? "bg-rose-100 text-rose-800 font-bold"
                          : metric.utilizationPercentage < (simulation?.idleThreshold ?? 70)
                            ? "bg-amber-100 text-amber-800"
                            : "bg-emerald-50 text-emerald-700"
                      )}
                    >
                      {metric.utilizationPercentage != null ? `${metric.utilizationPercentage.toFixed(1)}%` : "N/A"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center">{getStatusBadge(metric.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* SECTION 3: Bảng Nhân sự Snapshot Phân bổ Cố định (Accordion) */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <button
          onClick={() => setIsSnapshotExpanded(!isSnapshotExpanded)}
          className="w-full flex items-center justify-between px-5 py-4 text-left hover:bg-slate-50 transition"
        >
          <div className="flex items-center space-x-2">
            <Users className="h-4 w-4 text-slate-500" />
            <h3 className="text-sm font-bold text-slate-900">
              Chi Tiết Nhân Sự Trong Snapshot ({scenario.snapshotEmployeesCount ?? scenario.totalSnapshotEmployees ?? 0} nhân sự)
            </h3>
            <span className="text-xs text-slate-400">
              (Ảnh chụp phân bổ cố định, không thay đổi)
            </span>
          </div>
          {isSnapshotExpanded ? (
            <ChevronDown className="h-4 w-4 text-slate-400" />
          ) : (
            <ChevronRight className="h-4 w-4 text-slate-400" />
          )}
        </button>

        {isSnapshotExpanded && (
          <div className="border-t border-slate-100 overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/50 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="px-4 py-2.5">Mã NV</th>
                  <th className="px-4 py-2.5">Họ và tên</th>
                  <th className="px-4 py-2.5">Chức danh</th>
                  {simulation?.weeklyMetrics.map((m) => (
                    <th key={m.weekNumber} className="px-3 py-2.5 text-center font-mono text-[10px]">
                      W{m.weekNumber} (Phân bổ / Chuẩn)
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {(simulation?.employeeSnapshots ?? simulation?.employeeRows ?? []).map((emp) => (
                  <tr key={emp.employeeId} className="hover:bg-slate-50/60 transition">
                    <td className="px-4 py-2.5 font-mono text-slate-500">{emp.employeeCode}</td>
                    <td className="px-4 py-2.5 font-medium text-slate-900">{emp.fullName ?? emp.employeeName}</td>
                    <td className="px-4 py-2.5 text-slate-500">{emp.professionalRole || "—"}</td>
                    {(emp.cells ?? emp.weeklyCells ?? []).map((cell) => (
                      <td key={cell.weekNumber} className="px-3 py-2.5 text-center font-mono text-[11px]">
                        <span className="font-semibold text-slate-800">{cell.allocatedHours ?? cell.snapshotAllocatedHours}h</span>
                        <span className="text-slate-400"> / {cell.availableHours ?? cell.snapshotAvailableHours}h</span>
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Section 4: Kịch bản tuyển thêm nhân sự (NCL-08-CN-005) */}
      {canReadRecruitment && (
        <RecruitmentScenarioSection
          scenarioId={scenarioId}
          isVT03={canEdit}
          durationWeeks={scenario.durationWeeks}
        />
      )}

      {/* Demand Add/Edit Modal */}
      {isDemandModalOpen && (
        <AddEditDemandModal
          isOpen={isDemandModalOpen}
          scenarioId={scenarioId}
          scenarioStartYear={scenario.fromYear ?? scenario.startYear}
          scenarioStartWeek={scenario.fromWeek ?? scenario.startWeek}
          scenarioDurationWeeks={scenario.durationWeeks}
          availableWeeks={simulation?.weeklyMetrics}
          initialData={selectedDemand}
          onClose={() => {
            setIsDemandModalOpen(false);
            setSelectedDemand(null);
          }}
          onSuccess={() => {
            loadData();
          }}
        />
      )}

      {/* Apply Scenario Modal (NCL-08-CN-003) */}
      {isApplyModalOpen && (
        <ApplyScenarioModal
          isOpen={isApplyModalOpen}
          scenario={scenario}
          onClose={() => setIsApplyModalOpen(false)}
          onSuccess={() => {
            loadData();
          }}
        />
      )}

      {/* Share Scenario Modal */}
      {isShareModalOpen && (
        <ShareScenarioModal
          isOpen={isShareModalOpen}
          scenarioId={scenarioId}
          scenarioCode={scenario.code}
          scenarioName={scenario.name}
          onClose={() => setIsShareModalOpen(false)}
          onShareUpdated={() => loadData()}
        />
      )}

      {/* Edit Scenario Info / Note Modal */}
      {isEditInfoModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="w-full max-w-lg bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden">
            <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/50">
              <div className="flex items-center space-x-2.5">
                <Edit2 className="h-5 w-5 text-indigo-600" />
                <h3 className="text-base font-bold text-slate-900">Sửa Tên &amp; Ghi Chú Kịch Bản</h3>
              </div>
              <button
                onClick={() => setIsEditInfoModalOpen(false)}
                className="rounded-lg p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handlePatchInfo} className="p-5 space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">Tên kịch bản</label>
                <input
                  type="text"
                  value={nameInput}
                  onChange={(e) => setNameInput(e.target.value)}
                  maxLength={255}
                  required
                  placeholder="Nhập tên kịch bản..."
                  className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">Ghi chú (Note)</label>
                <textarea
                  value={noteInput}
                  onChange={(e) => setNoteInput(e.target.value)}
                  maxLength={2000}
                  rows={4}
                  placeholder="Nhập ghi chú bối cảnh, giả định tuyển dụng hoặc lưu ý khi chia sẻ..."
                  className="w-full rounded-xl border border-slate-200 px-3.5 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 resize-none"
                />
                <span className="text-[10px] text-slate-400 block text-right">
                  {noteInput.length}/2000 ký tự
                </span>
              </div>

              <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-[11px] text-amber-800 flex items-start space-x-2">
                <Clock className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                <span>
                  Chỉnh sửa kịch bản sẽ tự động chuyển trạng thái về <strong>Bản nháp</strong>. Sau khi hoàn tất chỉnh sửa, hãy bấm &quot;Lưu kịch bản&quot; nếu muốn chia sẻ lại cho người khác.
                </span>
              </div>

              <div className="pt-3 border-t border-slate-100 flex justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setIsEditInfoModalOpen(false)}
                  className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isPatching}
                  className="rounded-xl bg-indigo-600 px-5 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition disabled:opacity-50 flex items-center space-x-1.5 shadow-xs"
                >
                  {isPatching && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                  <span>Cập nhật</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};