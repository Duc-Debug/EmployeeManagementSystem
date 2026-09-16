"use client";

import React, { useState, useEffect } from "react";
import { X, UserPlus, Edit3, AlertCircle, Loader2 } from "lucide-react";
import {
  createSimulatedEmployee,
  updateSimulatedEmployee,
  type SimulatedEmployeeResponse,
  type CreateSimulatedEmployeePayload,
} from "@/lib/api/recruitment-scenarios";
import { getProjectRoles, type ProjectRoleResponse } from "@/lib/api/project-roles";
import { getSkills, type SkillResponse } from "@/lib/api/skills";

interface AddEditSimulatedEmployeeModalProps {
  isOpen: boolean;
  scenarioId: number;
  scenarioDurationWeeks?: number;
  initialData?: SimulatedEmployeeResponse | null;
  preselectedRoleId?: number;
  onClose: () => void;
  onSuccess: () => void;
}

export const AddEditSimulatedEmployeeModal: React.FC<AddEditSimulatedEmployeeModalProps> = ({
  isOpen,
  scenarioId,
  scenarioDurationWeeks = 12,
  initialData,
  preselectedRoleId,
  onClose,
  onSuccess,
}) => {
  const isEdit = Boolean(initialData);

  const [roles, setRoles] = useState<ProjectRoleResponse[]>([]);
  const [skills, setSkills] = useState<SkillResponse[]>([]);
  const [loadingCatalogs, setLoadingCatalogs] = useState(false);

  const [candidateName, setCandidateName] = useState("");
  const [projectRoleId, setProjectRoleId] = useState<number | "">("");
  const [primarySkillId, setPrimarySkillId] = useState<number | "">("");
  const [standardHoursPerWeek, setStandardHoursPerWeek] = useState<number>(40);
  const [weeksCount, setWeeksCount] = useState<number>(scenarioDurationWeeks);
  const [notes, setNotes] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Load Catalogs (Roles & Skills)
  useEffect(() => {
    if (!isOpen) return;

    let mounted = true;
    setLoadingCatalogs(true);

    Promise.all([
      getProjectRoles(false).catch(() => []),
      getSkills({ status: "ACTIVE" }).catch(() => []),
    ])
      .then(([roleList, skillList]) => {
        if (!mounted) return;
        setRoles(roleList);
        setSkills(skillList);
      })
      .finally(() => {
        if (mounted) setLoadingCatalogs(false);
      });

    return () => {
      mounted = false;
    };
  }, [isOpen]);

  // Pre-fill form when editing or preselectedRoleId is passed
  useEffect(() => {
    if (!isOpen) return;

    if (initialData) {
      setCandidateName(initialData.candidateName);
      setProjectRoleId(initialData.projectRoleId);
      setPrimarySkillId(initialData.primarySkillId || "");
      setStandardHoursPerWeek(Number(initialData.standardHoursPerWeek) || 40);
      setWeeksCount(Number(initialData.weeksCount) || scenarioDurationWeeks);
      setNotes(initialData.notes || "");
    } else {
      setCandidateName("");
      setProjectRoleId(preselectedRoleId || "");
      setPrimarySkillId("");
      setStandardHoursPerWeek(40);
      setWeeksCount(scenarioDurationWeeks > 0 ? scenarioDurationWeeks : 12);
      setNotes("");
    }
    setErrorMessage(null);
  }, [isOpen, initialData, preselectedRoleId, scenarioDurationWeeks]);

  if (!isOpen) return null;

  const totalCalculatedCapacity =
    (Number(standardHoursPerWeek) || 0) * (Number(weeksCount) || 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const trimmedName = candidateName.trim();
    if (!trimmedName) {
      setErrorMessage("Vui lòng nhập họ và tên ứng viên giả định.");
      return;
    }
    if (!projectRoleId) {
      setErrorMessage("Vui lòng chọn vai trò dự án.");
      return;
    }

    const hours = Number(standardHoursPerWeek);
    if (isNaN(hours) || hours < 1 || hours > 80) {
      setErrorMessage("Số giờ chuẩn mỗi tuần phải từ 1.00 đến 80.00 giờ.");
      return;
    }

    const weeks = Number(weeksCount);
    if (isNaN(weeks) || weeks < 1 || weeks > 52) {
      setErrorMessage("Số tuần làm việc phải nằm trong khoảng từ 1 đến 52 tuần.");
      return;
    }

    const payload: CreateSimulatedEmployeePayload = {
      candidateName: trimmedName,
      projectRoleId: Number(projectRoleId),
      primarySkillId: primarySkillId ? Number(primarySkillId) : null,
      standardHoursPerWeek: hours,
      weeksCount: weeks,
      notes: notes.trim() || undefined,
    };

    setSubmitting(true);
    try {
      if (isEdit && initialData) {
        await updateSimulatedEmployee(scenarioId, initialData.id, payload);
      } else {
        await createSimulatedEmployee(scenarioId, payload);
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      setErrorMessage(
        err instanceof Error ? err.message : "Thao tác thất bại. Vui lòng thử lại."
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4 overflow-y-auto">
      <div className="bg-white rounded-xl shadow-2xl border border-slate-200 w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-slate-50/70">
          <div className="flex items-center space-x-2.5">
            <div className="p-2 bg-indigo-50 text-indigo-600 rounded-lg">
              {isEdit ? <Edit3 className="h-5 w-5" /> : <UserPlus className="h-5 w-5" />}
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900">
                {isEdit ? "Chỉnh sửa nhân sự giả định" : "Thêm nhân sự tuyển giả định"}
              </h2>
              <p className="text-xs text-slate-500">
                Mô phỏng nguồn lực tuyển thêm trong kịch bản (QTN-14)
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {errorMessage && (
            <div className="flex items-start space-x-2 p-3 bg-rose-50 border border-rose-200 rounded-lg text-rose-700 text-xs">
              <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Candidate Name */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Tên ứng viên / Vị trí tuyển giả định <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              maxLength={255}
              placeholder="VD: Senior Frontend Dev (Dự bị 1)"
              value={candidateName}
              onChange={(e) => setCandidateName(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>

          {/* Project Role Selection */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Vai trò dự án (Project Role) <span className="text-rose-500">*</span>
            </label>
            <select
              required
              disabled={loadingCatalogs}
              value={projectRoleId}
              onChange={(e) => setProjectRoleId(e.target.value ? Number(e.target.value) : "")}
              className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 bg-white"
            >
              <option value="">-- Chọn vai trò dự án --</option>
              {roles.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.code} - {r.name}
                </option>
              ))}
            </select>
          </div>

          {/* Primary Skill Selection */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Kỹ năng chính (Primary Skill)
            </label>
            <select
              disabled={loadingCatalogs}
              value={primarySkillId}
              onChange={(e) => setPrimarySkillId(e.target.value ? Number(e.target.value) : "")}
              className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 bg-white"
            >
              <option value="">-- Tùy chọn: Chọn kỹ năng chuyên môn --</option>
              {skills.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.groupName})
                </option>
              ))}
            </select>
          </div>

          {/* Capacity inputs: Hours per week & Weeks count */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Giờ chuẩn/tuần <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                min={1}
                max={80}
                step={0.5}
                required
                value={standardHoursPerWeek}
                onChange={(e) => setStandardHoursPerWeek(parseFloat(e.target.value) || 0)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 font-mono"
              />
              <span className="text-[11px] text-slate-400 mt-0.5 block">Hợp lệ: 1.00 - 80.00h</span>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Số tuần làm việc <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                min={1}
                max={52}
                step={1}
                required
                value={weeksCount}
                onChange={(e) => setWeeksCount(parseInt(e.target.value, 10) || 0)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 font-mono"
              />
              <span className="text-[11px] text-slate-400 mt-0.5 block">Hợp lệ: 1 - 52 tuần</span>
            </div>
          </div>

          {/* Dynamic calculated capacity box */}
          <div className="p-3 bg-indigo-50/70 border border-indigo-100 rounded-lg flex items-center justify-between">
            <span className="text-xs text-indigo-900 font-medium">
              Tổng năng lực giờ bổ sung dự kiến:
            </span>
            <span className="text-sm font-bold font-mono text-indigo-700">
              +{totalCalculatedCapacity.toLocaleString("vi-VN", { minimumFractionDigits: 1, maximumFractionDigits: 2 })} giờ
            </span>
          </div>

          {/* Notes */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Ghi chú kịch bản tuyển dụng
            </label>
            <textarea
              rows={2}
              maxLength={500}
              placeholder="VD: Dự kiến tuyển onboard vào tuần 3, thử việc 2 tháng..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <button
              type="button"
              disabled={submitting}
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-slate-600 hover:text-slate-800 hover:bg-slate-100 rounded-lg transition"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={submitting || loadingCatalogs}
              className="flex items-center space-x-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-lg shadow-sm hover:shadow transition disabled:opacity-50"
            >
              {submitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Đang lưu...</span>
                </>
              ) : (
                <span>{isEdit ? "Lưu thay đổi" : "Thêm vào kịch bản"}</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
