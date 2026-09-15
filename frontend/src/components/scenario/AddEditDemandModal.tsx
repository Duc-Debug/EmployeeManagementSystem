"use client";

import React, { useState, useEffect } from "react";
import { X, Plus, AlertCircle, Edit3 } from "lucide-react";
import {
  addScenarioDemand,
  updateScenarioDemand,
  type ScenarioDemandResult,
  type AddDemandPayload,
  type UpdateDemandPayload,
} from "@/lib/api/simulation-scenarios";

interface AddEditDemandModalProps {
  isOpen: boolean;
  scenarioId: number;
  scenarioStartWeek: number;
  scenarioDurationWeeks: number;
  initialData?: ScenarioDemandResult | null;
  onClose: () => void;
  onSuccess: (demand: ScenarioDemandResult) => void;
}

export const AddEditDemandModal: React.FC<AddEditDemandModalProps> = ({
  isOpen,
  scenarioId,
  scenarioStartWeek,
  scenarioDurationWeeks,
  initialData,
  onClose,
  onSuccess,
}) => {
  const [roleName, setRoleName] = useState("");
  const [headcount, setHeadcount] = useState<number>(1);
  const [weekStart, setWeekStart] = useState<number>(scenarioStartWeek);
  const [weekEnd, setWeekEnd] = useState<number>(scenarioStartWeek + scenarioDurationWeeks - 1);
  const [hoursPerWeek, setHoursPerWeek] = useState<number>(40);
  const [requiredSkill, setRequiredSkill] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const scenarioEndWeek = scenarioStartWeek + scenarioDurationWeeks - 1;

  useEffect(() => {
    if (!isOpen) return;
    setError(null);
    if (initialData) {
      setRoleName(initialData.roleName);
      setHeadcount(initialData.headcount);
      setWeekStart(initialData.weekStart);
      setWeekEnd(initialData.weekEnd);
      setHoursPerWeek(initialData.hoursPerWeek);
      setRequiredSkill(initialData.requiredSkill || "");
    } else {
      setRoleName("");
      setHeadcount(1);
      setWeekStart(scenarioStartWeek);
      setWeekEnd(Math.min(53, scenarioStartWeek + scenarioDurationWeeks - 1));
      setHoursPerWeek(40);
      setRequiredSkill("");
    }
  }, [isOpen, initialData, scenarioStartWeek, scenarioDurationWeeks]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!roleName.trim()) {
      setError("Vui lòng nhập tên vai trò hoặc vị trí");
      return;
    }
    if (!Number.isInteger(headcount) || headcount <= 0) {
      setError("Số lượng nhân sự phải là số nguyên lớn hơn 0");
      return;
    }
    if (hoursPerWeek < 0) {
      setError("Số giờ/tuần không được là số âm");
      return;
    }
    if (weekStart < 1 || weekStart > 53 || weekEnd < 1 || weekEnd > 53) {
      setError("Tuần phải nằm trong khoảng từ 1 đến 53");
      return;
    }
    if (weekStart > weekEnd) {
      setError(`Tuần bắt đầu (W${weekStart}) không thể lớn hơn tuần kết thúc (W${weekEnd})`);
      return;
    }
    if (weekStart < scenarioStartWeek || weekEnd > scenarioEndWeek) {
      setError(
        `Nhu cầu phải nằm trong phạm vi kịch bản (Tuần ${scenarioStartWeek} đến Tuần ${scenarioEndWeek})`
      );
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (initialData) {
        const payload: UpdateDemandPayload = {
          roleName: roleName.trim(),
          headcount,
          weekStart,
          weekEnd,
          hoursPerWeek,
          requiredSkill: requiredSkill.trim() || undefined,
        };
        const updated = await updateScenarioDemand(scenarioId, initialData.id, payload);
        onSuccess(updated);
      } else {
        const payload: AddDemandPayload = {
          roleName: roleName.trim(),
          headcount,
          weekStart,
          weekEnd,
          hoursPerWeek,
          requiredSkill: requiredSkill.trim() || undefined,
        };
        const created = await addScenarioDemand(scenarioId, payload);
        onSuccess(created);
      }
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Đã có lỗi xảy ra khi lưu nhu cầu giả định.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden flex flex-col">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/50">
          <div className="flex items-center space-x-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              {initialData ? <Edit3 className="h-5 w-5" /> : <Plus className="h-5 w-5" />}
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                {initialData ? "Chỉnh sửa Nhu cầu Giả định" : "Thêm Nhu cầu Giả định"}
              </h3>
              <p className="text-xs text-slate-500">Khai báo nhân sự cần bổ sung cho dự án</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Modal Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="flex items-start space-x-2.5 rounded-xl bg-rose-50 p-3.5 text-xs text-rose-700 border border-rose-100 animate-in fade-in">
              <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
              <div className="flex-1 font-medium">{error}</div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Vai trò / Chức danh <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              value={roleName}
              onChange={(e) => setRoleName(e.target.value)}
              required
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              placeholder="VD: Senior Backend Developer, QC Lead..."
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Số lượng nhân sự <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                min={1}
                value={headcount}
                onChange={(e) => setHeadcount(Math.max(1, parseInt(e.target.value, 10) || 1))}
                required
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Số giờ / tuần / người <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                min={0}
                max={168}
                step={0.5}
                value={hoursPerWeek}
                onChange={(e) => setHoursPerWeek(parseFloat(e.target.value) || 0)}
                required
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Tuần bắt đầu <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                min={scenarioStartWeek}
                max={scenarioEndWeek}
                value={weekStart}
                onChange={(e) => setWeekStart(parseInt(e.target.value, 10) || scenarioStartWeek)}
                required
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              />
              <span className="text-[10px] text-slate-400">Tối thiểu: W{scenarioStartWeek}</span>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Tuần kết thúc <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                min={weekStart}
                max={scenarioEndWeek}
                value={weekEnd}
                onChange={(e) => setWeekEnd(parseInt(e.target.value, 10) || scenarioEndWeek)}
                required
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              />
              <span className="text-[10px] text-slate-400">Tối đa: W{scenarioEndWeek}</span>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Kỹ năng yêu cầu (Tùy chọn)
            </label>
            <input
              type="text"
              value={requiredSkill}
              onChange={(e) => setRequiredSkill(e.target.value)}
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              placeholder="VD: Java, Spring Boot, React..."
            />
          </div>

          {/* Quick Calc Summary */}
          <div className="rounded-xl bg-slate-50 p-3 border border-slate-200 text-xs text-slate-600 flex justify-between items-center">
            <span>Tổng giờ bổ sung mỗi tuần:</span>
            <span className="font-bold text-indigo-600 text-sm">
              {(headcount * hoursPerWeek).toLocaleString()} giờ/tuần
            </span>
          </div>

          {/* Modal Footer */}
          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-medium text-slate-600 hover:bg-slate-50 transition"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={loading}
              className="rounded-xl bg-indigo-600 px-5 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition disabled:opacity-50 shadow-xs"
            >
              {loading ? "Đang lưu..." : initialData ? "Cập nhật nhu cầu" : "Thêm nhu cầu"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
