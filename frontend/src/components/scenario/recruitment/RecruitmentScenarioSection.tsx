"use client";

import React, { useState, useEffect, useCallback } from "react";
import {
  UserPlus,
  AlertTriangle,
  CheckCircle2,
  TrendingUp,
  Clock,
  Briefcase,
  Users,
  Edit2,
  Trash2,
  RotateCcw,
  Sparkles,
  Layers,
  Info,
  X,
  Loader2,
} from "lucide-react";
import {
  getRecruitmentEvaluation,
  getSimulatedEmployees,
  deleteSimulatedEmployee,
  calculateCoveragePercentage,
  formatHoursDisplay,
  type RecruitmentEvaluationResponse,
  type SimulatedEmployeeResponse,
  type RoleEvaluationItem,
} from "@/lib/api/recruitment-scenarios";
import { AddEditSimulatedEmployeeModal } from "./AddEditSimulatedEmployeeModal";

interface RecruitmentScenarioSectionProps {
  scenarioId: number;
  isVT03: boolean;
  durationWeeks?: number;
}

interface ToastMessage {
  message: string;
  type: "success" | "error";
}

export const RecruitmentScenarioSection: React.FC<RecruitmentScenarioSectionProps> = ({
  scenarioId,
  isVT03,
  durationWeeks = 12,
}) => {
  const [evaluation, setEvaluation] = useState<RecruitmentEvaluationResponse | null>(null);
  const [simulatedEmployees, setSimulatedEmployees] = useState<SimulatedEmployeeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Toast feedback state
  const [toast, setToast] = useState<ToastMessage | null>(null);

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState<SimulatedEmployeeResponse | null>(null);
  const [preselectedRoleId, setPreselectedRoleId] = useState<number | undefined>(undefined);

  // Delete confirmation dialog state
  const [employeeToDelete, setEmployeeToDelete] = useState<SimulatedEmployeeResponse | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Auto-dismiss toast after 3.5s
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => {
      setToast(null);
    }, 3500);
    return () => clearTimeout(timer);
  }, [toast]);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [evalRes, empList] = await Promise.all([
        getRecruitmentEvaluation(scenarioId),
        getSimulatedEmployees(scenarioId),
      ]);
      setEvaluation(evalRes);
      setSimulatedEmployees(empList);
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : "Không thể tải dữ liệu kịch bản tuyển dụng."
      );
    } finally {
      setLoading(false);
    }
  }, [scenarioId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleOpenAddModal = (roleId?: number) => {
    setSelectedEmployee(null);
    setPreselectedRoleId(roleId);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (emp: SimulatedEmployeeResponse) => {
    setSelectedEmployee(emp);
    setPreselectedRoleId(undefined);
    setIsModalOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!employeeToDelete) return;

    setIsDeleting(true);
    try {
      await deleteSimulatedEmployee(scenarioId, employeeToDelete.id);
      setToast({
        message: `Đã xóa nhân sự giả định "${employeeToDelete.candidateName}" khỏi kịch bản.`,
        type: "success",
      });
      setEmployeeToDelete(null);
      await loadData();
    } catch (err: unknown) {
      setToast({
        message: err instanceof Error ? err.message : "Xóa nhân sự giả định thất bại.",
        type: "error",
      });
    } finally {
      setIsDeleting(false);
    }
  };

  const handleModalSuccess = (msg?: string) => {
    setToast({
      message: msg || "Thao tác thành công!",
      type: "success",
    });
    loadData();
  };

  return (
    <div className="mt-8 space-y-6 animate-in fade-in duration-200">
      {/* Toast Notification Alert */}
      {toast && (
        <div
          className={`fixed bottom-6 right-6 z-50 flex items-center space-x-3 px-4 py-3 rounded-2xl shadow-xl border text-xs font-medium animate-in slide-in-from-bottom-5 duration-200 ${
            toast.type === "success"
              ? "bg-emerald-900 text-emerald-50 border-emerald-700"
              : "bg-rose-900 text-rose-50 border-rose-700"
          }`}
        >
          {toast.type === "success" ? (
            <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
          ) : (
            <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0" />
          )}
          <span>{toast.message}</span>
          <button
            onClick={() => setToast(null)}
            className="p-1 text-slate-300 hover:text-white rounded-lg transition"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {/* Section Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between pb-4 border-b border-slate-200 gap-3">
        <div className="flex items-center space-x-3">
          <div className="p-2.5 bg-indigo-600 text-white rounded-2xl shadow-xs">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-lg font-bold text-slate-900">
                Kịch Bản Tuyển Thêm Nhân Sự
              </h2>
              <span className="px-2.5 py-0.5 text-[11px] font-semibold bg-indigo-50 text-indigo-700 rounded-full border border-indigo-200">
                Mô phỏng Sandbox
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Mô phỏng bổ sung nhân sự để bù đắp các khoảng thiếu hụt năng lực.
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2.5">
          <button
            onClick={loadData}
            disabled={loading}
            title="Tải lại đánh giá"
            className="p-2 text-slate-500 hover:text-slate-800 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl shadow-xs transition disabled:opacity-50"
          >
            <RotateCcw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          </button>
          {isVT03 && (
            <button
              onClick={() => handleOpenAddModal()}
              className="flex items-center space-x-1.5 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-xl shadow-xs hover:shadow-sm transition"
            >
              <UserPlus className="h-4 w-4" />
              <span>+ Tuyển nhân sự giả định</span>
            </button>
          )}
        </div>
      </div>

      {/* Read-only Notice if user cannot edit */}
      {!isVT03 && (
        <div className="flex items-center space-x-2.5 p-3.5 bg-slate-50 border border-slate-200 rounded-2xl text-slate-600 text-xs">
          <Info className="h-4 w-4 text-slate-400 shrink-0" />
          <span>
            <strong>Chế độ chỉ xem:</strong> Bạn đang xem dữ liệu kịch bản tuyển dụng mô phỏng. Quyền thêm, chỉnh sửa hoặc xóa nhân sự giả định chỉ dành cho Quản lý nguồn lực khi kịch bản ở trạng thái Bản nháp.
          </span>
        </div>
      )}

      {error && (
        <div className="flex items-center space-x-2 p-4 bg-rose-50 border border-rose-200 rounded-2xl text-rose-700 text-xs">
          <AlertTriangle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* ── Khối 1: KPI Cards ────────────────────────────────────── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Giờ thiếu ban đầu */}
        <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs hover:shadow-sm transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500">Giờ thiếu ban đầu</span>
            <Clock className="h-4 w-4 text-slate-400" />
          </div>
          <div className="mt-2 flex items-baseline space-x-2">
            <span className="text-2xl font-bold font-mono text-slate-900">
              {formatHoursDisplay(evaluation?.totalOriginalShortfallHours)}
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Từ nhu cầu kịch bản</p>
        </div>

        {/* Card 2: Giờ bù từ tuyển giả định */}
        <div className="p-4 bg-indigo-50/25 rounded-2xl border border-indigo-200 shadow-xs hover:shadow-sm transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-indigo-700">Giờ bù đắp tuyển mới</span>
            <TrendingUp className="h-4 w-4 text-indigo-600" />
          </div>
          <div className="mt-2 flex items-baseline space-x-2">
            <span className="text-2xl font-bold font-mono text-indigo-700">
              +{formatHoursDisplay(evaluation?.totalSimulatedCapacityHours)}
            </span>
          </div>
          <p className="text-[11px] text-indigo-600 mt-1">
            {simulatedEmployees.length} nhân sự giả định đã thêm
          </p>
        </div>

        {/* Card 3: Giờ thiếu còn lại */}
        <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs hover:shadow-sm transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500">Giờ thiếu còn lại</span>
            <Layers className="h-4 w-4 text-slate-400" />
          </div>
          <div className="mt-2 flex items-baseline space-x-2">
            <span
              className={`text-2xl font-bold font-mono ${
                (evaluation?.totalRemainingShortfallHours ?? 0) > 0
                  ? "text-rose-600"
                  : "text-emerald-600"
              }`}
            >
              {formatHoursDisplay(evaluation?.totalRemainingShortfallHours)}
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">
            {(evaluation?.totalRemainingShortfallHours ?? 0) > 0
              ? "Cần tiếp tục bổ sung thêm"
              : "Đã bù đắp hoàn toàn thiếu hụt"}
          </p>
        </div>

        {/* Card 4: Trạng thái & Gợi ý tuyển */}
        <div
          className={`p-4 rounded-2xl border shadow-xs hover:shadow-sm transition ${
            evaluation?.isPlanBroken
              ? "bg-rose-50/50 border-rose-200"
              : "bg-emerald-50/50 border-emerald-200"
          }`}
        >
          <div className="flex items-center justify-between">
            <span
              className={`text-xs font-medium ${
                evaluation?.isPlanBroken ? "text-rose-700" : "text-emerald-700"
              }`}
            >
              Trạng thái khả thi
            </span>
            {evaluation?.isPlanBroken ? (
              <AlertTriangle className="h-4 w-4 text-rose-600" />
            ) : (
              <CheckCircle2 className="h-4 w-4 text-emerald-600" />
            )}
          </div>
          <div className="mt-2">
            {evaluation?.isPlanBroken ? (
              <span className="inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold bg-rose-100 text-rose-800 border border-rose-300">
                Vỡ kế hoạch ({evaluation.overloadedRoleCount} vai trò thiếu)
              </span>
            ) : (
              <span className="inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
                Kế hoạch Đảm bảo
              </span>
            )}
          </div>
          <p className="text-[11px] text-slate-500 mt-2 font-medium">
            Gợi ý tuyển thêm:{" "}
            <span className="font-bold text-slate-900">
              {evaluation?.totalSuggestedRecruitsNeeded ?? 0} người
            </span>
          </p>
        </div>
      </div>

      {/* ── Khối 2: Phân tích theo Vai trò (Role Evaluations) ─────── */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="px-5 py-3.5 bg-slate-50/80 border-b border-slate-200 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Briefcase className="h-4 w-4 text-indigo-600" />
            <h3 className="text-sm font-bold text-slate-900">
              Phân Tích Thiếu Hụt & Đề Xuất Tuyển Dụng Theo Vai Trò
            </h3>
          </div>
          <span className="text-[11px] text-slate-400 font-medium">
            Tiến độ bù đắp dựa trên công suất 40h/tuần
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/50 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                <th className="px-4 py-3">Mã vai trò</th>
                <th className="px-4 py-3">Tên vai trò</th>
                <th className="px-4 py-3 text-right">Giờ thiếu gốc</th>
                <th className="px-4 py-3 text-right">Giờ bù giả định</th>
                <th className="px-4 py-3 text-right">Giờ còn thiếu</th>
                <th className="px-4 py-3">Tiến độ bù đắp</th>
                <th className="px-4 py-3 text-center">Đề xuất tuyển</th>
                <th className="px-4 py-3 text-center">Trạng thái</th>
                {isVT03 && <th className="px-4 py-3 text-center">Hành động</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {loading && !evaluation ? (
                <tr>
                  <td colSpan={isVT03 ? 9 : 8} className="px-4 py-8 text-center text-slate-400">
                    <Loader2 className="h-5 w-5 animate-spin mx-auto mb-2 text-indigo-600" />
                    Đang tính toán ma trận tuyển dụng...
                  </td>
                </tr>
              ) : !evaluation?.roleEvaluations || evaluation.roleEvaluations.length === 0 ? (
                <tr>
                  <td colSpan={isVT03 ? 9 : 8} className="px-4 py-8 text-center text-slate-500">
                    <div className="flex flex-col items-center justify-center space-y-2">
                      <CheckCircle2 className="h-8 w-8 text-emerald-500" />
                      <p className="font-semibold text-slate-800">
                        Toàn bộ các vai trò đều đáp ứng đủ công suất!
                      </p>
                      <p className="text-[11px] text-slate-400 max-w-md text-center">
                        Kịch bản hiện tại không có vai trò nào bị thiếu hụt giờ làm. Bạn vẫn có thể chủ động bổ sung nhân sự giả định để chuẩn bị nguồn lực dự phòng.
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                evaluation.roleEvaluations.map((item: RoleEvaluationItem) => {
                  const roleId = item.roleId ?? item.projectRoleId;
                  const roleCode = item.roleCode ?? item.projectRoleCode ?? "-";
                  const roleName = item.roleName ?? item.projectRoleName ?? "-";
                  const isRoleBroken = item.isRoleBroken ?? (item.remainingShortfallHours > 0);
                  const coveragePct = calculateCoveragePercentage(
                    item.originalShortfallHours,
                    item.simulatedCapacityHours
                  );
                  return (
                    <tr
                      key={roleId}
                      className={`hover:bg-slate-50/70 transition ${
                        isRoleBroken ? "bg-rose-50/15" : ""
                      }`}
                    >
                      <td className="px-4 py-3 font-mono font-semibold text-slate-700">
                        {roleCode}
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-900">
                        {roleName}
                      </td>
                      <td className="px-4 py-3 text-right font-mono text-slate-600">
                        {formatHoursDisplay(item.originalShortfallHours)}
                      </td>
                      <td className="px-4 py-3 text-right font-mono text-indigo-600 font-medium">
                        +{formatHoursDisplay(item.simulatedCapacityHours)}
                      </td>
                      <td className="px-4 py-3 text-right font-mono font-bold">
                        <span
                          className={
                            item.remainingShortfallHours > 0
                              ? "text-rose-600"
                              : "text-emerald-600"
                          }
                        >
                          {formatHoursDisplay(item.remainingShortfallHours)}
                        </span>
                      </td>

                      {/* Capacity Progress Bar */}
                      <td className="px-4 py-3 min-w-[140px]">
                        <div className="space-y-1">
                          <div className="flex items-center justify-between text-[10px]">
                            <span className="font-semibold text-slate-700">{coveragePct}%</span>
                            <span className="text-slate-400">
                              {item.remainingShortfallHours <= 0 ? "Đã bù đủ" : "Đang thiếu"}
                            </span>
                          </div>
                          <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
                            <div
                              className={`h-1.5 rounded-full transition-all duration-300 ${
                                coveragePct >= 100
                                  ? "bg-emerald-500"
                                  : coveragePct > 0
                                  ? "bg-amber-500"
                                  : "bg-rose-400"
                              }`}
                              style={{ width: `${coveragePct}%` }}
                            />
                          </div>
                        </div>
                      </td>

                      <td className="px-4 py-3 text-center">
                        {item.suggestedRecruitsNeeded > 0 ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800">
                            {item.suggestedRecruitsNeeded} người
                          </span>
                        ) : (
                          <span className="text-slate-400 font-mono text-[11px]">0</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-center">
                        {isRoleBroken ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-rose-100 text-rose-800 border border-rose-200">
                            <AlertTriangle className="h-3 w-3 mr-1 text-rose-600" />
                            Thiếu hụt
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200">
                            <CheckCircle2 className="h-3 w-3 mr-1 text-emerald-600" />
                            Đã bù đủ
                          </span>
                        )}
                      </td>
                      {isVT03 && (
                        <td className="px-4 py-3 text-center">
                          {item.remainingShortfallHours > 0 && (
                            <button
                              onClick={() => handleOpenAddModal(roleId)}
                              className="inline-flex items-center space-x-1 px-2.5 py-1 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-medium rounded-lg border border-indigo-200 transition shadow-2xs hover:shadow-xs"
                            >
                              <UserPlus className="h-3 w-3" />
                              <span>Tuyển vai trò này</span>
                            </button>
                          )}
                        </td>
                      )}
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Khối 3: Bảng Quản lý Danh sách Nhân sự Giả định ───────── */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="px-5 py-3.5 bg-slate-50/80 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center space-x-2">
            <Users className="h-4 w-4 text-indigo-600" />
            <h3 className="text-sm font-bold text-slate-900">
              Danh Sách Nhân Sự Giả Định Trong Kịch Bản ({simulatedEmployees.length})
            </h3>
          </div>
          <span className="text-[11px] text-slate-400 font-medium mt-1 sm:mt-0">
            Dữ liệu mô phỏng độc lập trong Sandbox kịch bản #{scenarioId}
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/50 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                <th className="px-4 py-3">Ứng viên giả định</th>
                <th className="px-4 py-3">Vai trò dự án</th>
                <th className="px-4 py-3">Kỹ năng chính</th>
                <th className="px-4 py-3 text-center">Giờ/tuần</th>
                <th className="px-4 py-3 text-center">Số tuần</th>
                <th className="px-4 py-3 text-right">Tổng năng lực</th>
                <th className="px-4 py-3">Ghi chú</th>
                {isVT03 && <th className="px-4 py-3 text-center">Thao tác</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {simulatedEmployees.length === 0 ? (
                <tr>
                  <td
                    colSpan={isVT03 ? 8 : 7}
                    className="px-4 py-8 text-center text-slate-400"
                  >
                    Chưa có nhân sự giả định nào được thêm vào kịch bản này.
                    {isVT03 && (
                      <div className="mt-2">
                        <button
                          onClick={() => handleOpenAddModal()}
                          className="text-indigo-600 hover:text-indigo-700 font-medium underline"
                        >
                          Nhấn vào đây để thêm nhân sự đầu tiên
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ) : (
                simulatedEmployees.map((emp) => (
                  <tr key={emp.id} className="hover:bg-slate-50/70 transition">
                    <td className="px-4 py-3 font-semibold text-slate-900">
                      {emp.candidateName}
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-slate-100 text-slate-800">
                        {emp.projectRoleCode ? `${emp.projectRoleCode} - ` : ""}
                        {emp.projectRoleName || "Chưa rõ"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {emp.primarySkillName || "—"}
                    </td>
                    <td className="px-4 py-3 text-center font-mono">
                      {Number(emp.standardHoursPerWeek)}h
                    </td>
                    <td className="px-4 py-3 text-center font-mono">
                      {emp.weeksCount} tuần
                    </td>
                    <td className="px-4 py-3 text-right font-mono font-bold text-indigo-700">
                      +{formatHoursDisplay(emp.totalCapacityHours)}
                    </td>
                    <td className="px-4 py-3 text-slate-500 max-w-xs truncate" title={emp.notes || ""}>
                      {emp.notes || "—"}
                    </td>
                    {isVT03 && (
                      <td className="px-4 py-3 text-center">
                        <div className="flex items-center justify-center space-x-1">
                          <button
                            onClick={() => handleOpenEditModal(emp)}
                            title="Chỉnh sửa nhân sự"
                            className="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
                          >
                            <Edit2 className="h-3.5 w-3.5" />
                          </button>
                          <button
                            onClick={() => setEmployeeToDelete(emp)}
                            title="Xóa nhân sự giả định"
                            className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Add/Edit Simulated Employee */}
      {isModalOpen && (
        <AddEditSimulatedEmployeeModal
          isOpen={isModalOpen}
          scenarioId={scenarioId}
          scenarioDurationWeeks={durationWeeks}
          initialData={selectedEmployee}
          preselectedRoleId={preselectedRoleId}
          onClose={() => {
            setIsModalOpen(false);
            setSelectedEmployee(null);
            setPreselectedRoleId(undefined);
          }}
          onSuccess={handleModalSuccess}
        />
      )}

      {/* Professional Delete Confirmation Dialog */}
      {employeeToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-md p-6 space-y-4 animate-in zoom-in-95 duration-150">
            <div className="flex items-center space-x-3 text-rose-600">
              <div className="p-2.5 bg-rose-50 rounded-xl">
                <Trash2 className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">
                  Xác nhận xóa nhân sự giả định
                </h3>
                <p className="text-[11px] text-slate-500">
                  Thao tác này sẽ cập nhật lại ma trận đánh giá kịch bản
                </p>
              </div>
            </div>

            <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 space-y-1.5 text-xs text-slate-700">
              <p>
                Ứng viên: <strong>{employeeToDelete.candidateName}</strong>
              </p>
              <p>
                Vai trò: <strong>{employeeToDelete.projectRoleName || employeeToDelete.projectRoleCode}</strong>
              </p>
              <p className="text-rose-600 font-semibold font-mono">
                Số giờ sẽ bị giảm trừ khỏi kịch bản: -{formatHoursDisplay(employeeToDelete.totalCapacityHours)}
              </p>
            </div>

            <div className="flex items-center justify-end space-x-2.5 pt-2">
              <button
                type="button"
                disabled={isDeleting}
                onClick={() => setEmployeeToDelete(null)}
                className="px-4 py-2 text-xs font-medium text-slate-600 hover:text-slate-800 hover:bg-slate-100 rounded-xl transition"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                disabled={isDeleting}
                onClick={handleConfirmDelete}
                className="flex items-center space-x-1.5 px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold rounded-xl shadow-xs hover:shadow-sm transition disabled:opacity-50"
              >
                {isDeleting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span>Đang xóa...</span>
                  </>
                ) : (
                  <span>Xóa khỏi kịch bản</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
