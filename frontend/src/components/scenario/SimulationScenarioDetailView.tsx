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
} from "lucide-react";
import {
  getScenarioById,
  getScenarioSimulation,
  deleteScenarioDemand,
  type ScenarioDetailResult,
  type ScenarioSimulationResult,
  type ScenarioDemandResult,
} from "@/lib/api/simulation-scenarios";
import { AddEditDemandModal } from "./AddEditDemandModal";
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

  const [detail, setDetail] = useState<ScenarioDetailResult | null>(null);
  const [simulation, setSimulation] = useState<ScenarioSimulationResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [simulationError, setSimulationError] = useState<string | null>(null);
  const [simulationLoading, setSimulationLoading] = useState(false);

  // Demand modal state
  const [isDemandModalOpen, setIsDemandModalOpen] = useState(false);
  const [selectedDemand, setSelectedDemand] = useState<ScenarioDemandResult | null>(null);

  // Snapshot table accordion state
  const [isSnapshotExpanded, setIsSnapshotExpanded] = useState(false);

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
  const isDraft = scenario.status === "draft";
  const canEdit = isVT03 && isDraft;

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
              <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                Bản nháp ({scenario.status})
              </span>
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
            </div>
          </div>
        </div>

        {/* Action button */}
        <div className="flex items-center space-x-2">
          {canEdit && (
            <button
              onClick={() => {
                setSelectedDemand(null);
                setIsDemandModalOpen(true);
              }}
              className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs flex items-center space-x-1.5"
            >
              <Plus className="h-4 w-4" />
              <span>Thêm nhu cầu nhân sự</span>
            </button>
          )}
        </div>
      </div>

      {/* Snapshot Sandbox Alert Banner (QTN-14 & AC-02) */}
      <div className="rounded-2xl bg-sky-50/80 border border-sky-200 p-4 text-sky-900 flex items-start space-x-3 shadow-xs">
        <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-sky-100 text-sky-700 shrink-0 mt-0.5 border border-sky-200">
          <ShieldCheck className="h-5 w-5" />
        </div>
        <div className="flex-1 space-y-1 text-xs">
          <div className="font-bold flex items-center">
            <span>Dữ liệu mô phỏng Sandbox độc lập (QTN-14)</span>
            <span className="ml-2 px-2 py-0.2 rounded-md bg-sky-200/80 text-sky-800 text-[10px] font-mono">
              Snapshot: {formatSnapshotDate(scenario.baseSnapshotAt)}
            </span>
          </div>
          <p className="text-sky-800/90 leading-relaxed text-[11px]">
            Dữ liệu mô phỏng được tính toán dựa trên ảnh chụp phân bổ &amp; khả dụng thật tại thời điểm khởi tạo kịch bản.
            Mọi sự thay đổi về phân bổ thật sau thời điểm này tuyệt đối <strong>không làm thay đổi kịch bản</strong>, và các thao tác thêm/sửa/xóa nhu cầu giả định tại đây <strong>không ghi đè dữ liệu thật</strong>.
          </p>
        </div>
      </div>

      {simulationError && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-amber-900 flex items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
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

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
          <span className="text-[11px] font-medium text-slate-500">Giờ Snapshot (Thật)</span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-xl font-bold text-slate-900">{totalSnapshotHours.toLocaleString()}h</span>
            <span className="text-[10px] text-slate-400">cố định</span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
          <span className="text-[11px] font-medium text-slate-500">Giờ Nhu cầu (Dự án mới)</span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-xl font-bold text-indigo-600">+{totalDemandHours.toLocaleString()}h</span>
            <span className="text-[10px] text-indigo-400">{demands.length} vị trí</span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
          <span className="text-[11px] font-medium text-slate-500">Tổng Khối lượng Giả định</span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-xl font-bold text-slate-900">{totalWorkloadHours.toLocaleString()}h</span>
            <span className="text-[10px] text-slate-400">Snapshot + Nhu cầu</span>
          </div>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
          <span className="text-[11px] font-medium text-slate-500">Năng lực Khả dụng</span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-xl font-bold text-slate-900">{totalAvailableCapacity.toLocaleString()}h</span>
            <span className="text-[10px] text-slate-400">Tổng giờ trống</span>
          </div>
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
              Danh Sách Nhân Sự Vượt Năng Lực / Vỡ Kế Hoạch (NCL-08-CN-002)
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
            Phân tích nhân sự quá tải từ phân bổ hiện trạng (Baseline Snapshot)
          </span>
        </div>

        {(simulation?.overloadedEmployees ?? []).length === 0 ? (
          <div className="p-6 text-center space-y-2 bg-emerald-50/20">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600 mx-auto">
              <CheckCircle2 className="h-5 w-5" />
            </div>
            <p className="text-xs font-bold text-emerald-800">
              Không có nhân sự nào bị vỡ kế hoạch / vượt năng lực theo phân bổ hiện trạng (Baseline)
            </p>
            <p className="text-[11px] text-slate-500">
              Tất cả nhân sự trong đơn vị đều có khối lượng phân bổ gốc nằm trong định mức chuẩn. (Lưu ý: Nhu cầu kịch bản được mô phỏng ở cấp độ tổng thể kịch bản/tuần).
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

      {/* SECTION 1: Nhu cầu nhân sự giả định (Demands) */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 bg-slate-50/50">
          <div className="flex items-center space-x-2">
            <Users className="h-4 w-4 text-indigo-600" />
            <h3 className="text-sm font-bold text-slate-900">Danh sách Nhu cầu Nhân sự Giả định</h3>
            <span className="text-xs bg-slate-200 text-slate-700 px-2 py-0.5 rounded-full font-semibold">
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
              <span>Thêm vị trí</span>
            </button>
          )}
        </div>

        {demands.length === 0 ? (
          <div className="p-8 text-center space-y-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-400 mx-auto">
              <Users className="h-5 w-5" />
            </div>
            <p className="text-xs font-medium text-slate-600">Chưa có nhu cầu nhân sự giả định nào</p>
            <p className="text-[11px] text-slate-400">
              Thêm vị trí nhân sự (headcount, tuần, số giờ) để hệ thống tính toán năng lực đáp ứng.
            </p>
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
              Kết Quả Mô Phỏng Năng Lực Theo Tuần (Simulation Matrix)
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
                <th className="px-4 py-3 text-right">Khả dụng (Capacity)</th>
                <th className="px-4 py-3 text-right">Tỷ lệ Sử dụng</th>
                <th className="px-4 py-3 text-center">Trạng thái Cảnh báo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {simulation?.weeklyMetrics.map((metric) => (
                <tr
                  key={`${metric.year}-${metric.weekNumber}`}
                  className={cn(
                    "hover:bg-slate-50/70 transition",
                    metric.status === "OVERLOADED" && "bg-rose-50/30"
                  )}
                >
                  <td className="px-4 py-3 font-bold text-slate-900">
                    Tuần {metric.weekNumber} <span className="text-[10px] text-slate-400 font-normal">({metric.year})</span>
                  </td>
                  <td className="px-4 py-3 text-right font-mono text-slate-600">
                    {metric.snapshotAllocatedHours}h
                  </td>
                  <td className="px-4 py-3 text-right font-mono font-semibold text-indigo-600">
                    +{metric.demandHours}h
                  </td>
                  <td className="px-4 py-3 text-right font-mono font-bold text-slate-900">
                    {metric.scenarioWorkloadHours ?? metric.totalWorkloadHours}h
                  </td>
                  <td className="px-4 py-3 text-right font-mono text-slate-600">
                    {metric.availableHours ?? metric.availableCapacityHours}h
                  </td>
                  <td className="px-4 py-3 text-right font-mono font-bold">
                    <span
                      className={cn(
                        metric.status === "OVERLOADED"
                          ? "text-rose-600"
                          : metric.status === "OPTIMAL"
                          ? "text-emerald-600"
                          : "text-slate-700"
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

      {/* SECTION 3: Ảnh chụp phân bổ nhân sự gốc (Collapsible Employee Snapshot Table) */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <button
          onClick={() => setIsSnapshotExpanded(!isSnapshotExpanded)}
          className="w-full flex items-center justify-between px-5 py-4 bg-slate-50/50 hover:bg-slate-50 transition border-b border-slate-100 text-left"
        >
          <div className="flex items-center space-x-2">
            {isSnapshotExpanded ? (
              <ChevronDown className="h-4 w-4 text-slate-500" />
            ) : (
              <ChevronRight className="h-4 w-4 text-slate-500" />
            )}
            <Clock className="h-4 w-4 text-indigo-600" />
            <h3 className="text-sm font-bold text-slate-900">
              Chi Tiết Ảnh Chụp Nhân Sự Tại Thời Điểm Snapshot ({(simulation?.employeeSnapshots ?? simulation?.employeeRows ?? []).length} nhân sự)
            </h3>
          </div>
          <span className="text-[11px] text-slate-400 font-medium">
            {isSnapshotExpanded ? "Thu gọn" : "Xem chi tiết từng nhân sự"}
          </span>
        </button>

        {isSnapshotExpanded && (
          <div className="overflow-x-auto p-2">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/50 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="px-4 py-2.5">Mã NV</th>
                  <th className="px-4 py-2.5">Họ và Tên</th>
                  <th className="px-4 py-2.5">Chức danh</th>
                  {simulation?.weeklyMetrics.map((m) => (
                    <th key={m.weekNumber} className="px-3 py-2.5 text-center">
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
    </div>
  );
};
