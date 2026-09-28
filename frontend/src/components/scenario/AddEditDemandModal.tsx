"use client";

import React, { useState, useEffect, useMemo } from "react";
import { X, Plus, AlertCircle, Edit3 } from "lucide-react";
import {
  addScenarioDemand,
  updateScenarioDemand,
  type ScenarioDemandResult,
  type AddDemandPayload,
  type UpdateDemandPayload,
} from "@/lib/api/simulation-scenarios";

interface WeekOption {
  year: number;
  week: number;
  label: string;
}

interface AddEditDemandModalProps {
  isOpen: boolean;
  scenarioId: number;
  scenarioStartYear: number;
  scenarioStartWeek: number;
  scenarioDurationWeeks: number;
  availableWeeks?: { year: number; weekNumber: number }[];
  initialData?: ScenarioDemandResult | null;
  onClose: () => void;
  onSuccess: (demand: ScenarioDemandResult) => void;
}

function buildTargetWeeks(
  startYear: number,
  startWeek: number,
  durationWeeks: number,
  availableWeeks?: { year: number; weekNumber: number }[]
): WeekOption[] {
  if (availableWeeks && availableWeeks.length > 0) {
    return availableWeeks.map((w) => ({
      year: w.year,
      week: w.weekNumber,
      label: `Tuần ${w.weekNumber}/${w.year} (${w.year}-W${String(w.weekNumber).padStart(2, "0")})`,
    }));
  }

  const jan4 = new Date(Date.UTC(startYear, 0, 4));
  const dayOfWeek = jan4.getUTCDay() || 7;
  const startMonday = new Date(jan4.getTime() + ((startWeek - 1) * 7 + 1 - dayOfWeek) * 86400000);

  const result: WeekOption[] = [];
  for (let i = 0; i < durationWeeks; i++) {
    const d = new Date(startMonday.getTime() + i * 7 * 86400000);
    const target = new Date(d.valueOf());
    const dayNr = (d.getUTCDay() + 6) % 7;
    target.setUTCDate(target.getUTCDate() - dayNr + 3);
    const firstThursday = target.valueOf();
    target.setUTCMonth(0, 1);
    if (target.getUTCDay() !== 4) {
      target.setUTCMonth(0, 1 + ((4 - target.getUTCDay()) + 7) % 7);
    }
    const w = 1 + Math.ceil((firstThursday - target.valueOf()) / 604800000);
    const y = new Date(firstThursday).getUTCFullYear();
    result.push({
      year: y,
      week: w,
      label: `Tuần ${w}/${y} (${y}-W${String(w).padStart(2, "0")})`,
    });
  }
  return result;
}

export const AddEditDemandModal: React.FC<AddEditDemandModalProps> = ({
  isOpen,
  scenarioId,
  scenarioStartYear,
  scenarioStartWeek,
  scenarioDurationWeeks,
  availableWeeks,
  initialData,
  onClose,
  onSuccess,
}) => {
  const [demandName, setDemandName] = useState("");
  const [headcount, setHeadcount] = useState<number | "">(1);
  const [startIndex, setStartIndex] = useState<number>(0);
  const [endIndex, setEndIndex] = useState<number>(0);
  const [hoursPerWeekPerPerson, setHoursPerWeekPerPerson] = useState<number | "">(40);
  const [skillRequirement, setSkillRequirement] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const scenarioWeeks = useMemo(() => {
    return buildTargetWeeks(
      scenarioStartYear,
      scenarioStartWeek,
      scenarioDurationWeeks,
      availableWeeks
    );
  }, [scenarioStartYear, scenarioStartWeek, scenarioDurationWeeks, availableWeeks]);

  useEffect(() => {
    if (!isOpen) return;
    setError(null);

    const maxIdx = Math.max(0, scenarioWeeks.length - 1);

    if (initialData) {
      setDemandName(initialData.demandName || initialData.roleName || "");
      setHeadcount(initialData.headcount || 1);
      setHoursPerWeekPerPerson(
        initialData.hoursPerWeekPerPerson ?? initialData.hoursPerWeek ?? 40
      );
      setSkillRequirement(
        initialData.skillRequirement || initialData.requiredSkill || ""
      );

      const targetStartYear = initialData.startYear ?? scenarioStartYear;
      const targetStartWeek = initialData.startWeek ?? initialData.weekStart ?? scenarioStartWeek;
      const targetEndYear = initialData.endYear ?? scenarioStartYear;
      const targetEndWeek = initialData.endWeek ?? initialData.weekEnd ?? scenarioStartWeek;

      const foundStartIdx = scenarioWeeks.findIndex(
        (w) => w.year === targetStartYear && w.week === targetStartWeek
      );
      const foundEndIdx = scenarioWeeks.findIndex(
        (w) => w.year === targetEndYear && w.week === targetEndWeek
      );

      setStartIndex(foundStartIdx >= 0 ? foundStartIdx : 0);
      setEndIndex(foundEndIdx >= 0 ? foundEndIdx : maxIdx);
    } else {
      setDemandName("");
      setHeadcount(1);
      setStartIndex(0);
      setEndIndex(maxIdx);
      setHoursPerWeekPerPerson(40);
      setSkillRequirement("");
    }
  }, [isOpen, initialData, scenarioWeeks, scenarioStartYear, scenarioStartWeek]);

  if (!isOpen) return null;

  const handleStartChange = (newIdx: number) => {
    setStartIndex(newIdx);
    if (endIndex < newIdx) {
      setEndIndex(newIdx);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!demandName.trim()) {
      setError("Vui lòng nhập tên vai trò hoặc vị trí");
      return;
    }

    const parsedHeadcount = typeof headcount === "number" ? headcount : parseInt(headcount, 10);
    if (isNaN(parsedHeadcount) || !Number.isInteger(parsedHeadcount) || parsedHeadcount <= 0) {
      setError("Số lượng nhân sự phải là số nguyên lớn hơn 0");
      return;
    }

    const parsedHours = typeof hoursPerWeekPerPerson === "number" ? hoursPerWeekPerPerson : parseFloat(String(hoursPerWeekPerPerson));
    if (isNaN(parsedHours) || parsedHours <= 0) {
      setError("Số giờ/tuần/người phải lớn hơn 0");
      return;
    }

    const startOption = scenarioWeeks[startIndex];
    const endOption = scenarioWeeks[endIndex];
    if (!startOption || !endOption) {
      setError("Phạm vi tuần không hợp lệ");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const payload: AddDemandPayload = {
        demandName: demandName.trim(),
        headcount: parsedHeadcount,
        startYear: startOption.year,
        startWeek: startOption.week,
        endYear: endOption.year,
        endWeek: endOption.week,
        hoursPerWeekPerPerson: parsedHours,
        skillRequirement: skillRequirement.trim() || undefined,
      };

      if (initialData) {
        const updated = await updateScenarioDemand(
          scenarioId,
          initialData.id,
          payload as UpdateDemandPayload
        );
        onSuccess(updated);
      } else {
        const created = await addScenarioDemand(scenarioId, payload);
        onSuccess(created);
      }
      onClose();
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : "Đã có lỗi xảy ra khi lưu nhu cầu giả định."
      );
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
              <p className="text-xs text-slate-500">Khai báo nhân sự cần bổ sung cho kịch bản</p>
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
              value={demandName}
              onChange={(e) => setDemandName(e.target.value)}
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
                onChange={(e) => {
                  const val = e.target.value;
                  if (val === "") {
                    setHeadcount("");
                  } else {
                    const parsed = parseInt(val, 10);
                    setHeadcount(isNaN(parsed) ? "" : parsed);
                  }
                }}
                onBlur={() => {
                  if (headcount === "" || headcount < 1) {
                    setHeadcount(1);
                  }
                }}
                placeholder="Nhập số lượng..."
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
                min={0.5}
                max={168}
                step={0.5}
                value={hoursPerWeekPerPerson}
                onChange={(e) => {
                  const val = e.target.value;
                  if (val === "") {
                    setHoursPerWeekPerPerson("");
                  } else {
                    const parsed = parseFloat(val);
                    setHoursPerWeekPerPerson(isNaN(parsed) ? "" : parsed);
                  }
                }}
                onBlur={() => {
                  if (hoursPerWeekPerPerson === "" || hoursPerWeekPerPerson <= 0) {
                    setHoursPerWeekPerPerson(40);
                  }
                }}
                placeholder="VD: 40"
                required
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Thời điểm bắt đầu <span className="text-rose-500">*</span>
              </label>
              <select
                value={startIndex}
                onChange={(e) => handleStartChange(parseInt(e.target.value, 10))}
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 bg-white"
              >
                {scenarioWeeks.map((w, idx) => (
                  <option key={`${w.year}-${w.week}`} value={idx}>
                    {w.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Thời điểm kết thúc <span className="text-rose-500">*</span>
              </label>
              <select
                value={endIndex}
                onChange={(e) => setEndIndex(parseInt(e.target.value, 10))}
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10 bg-white"
              >
                {scenarioWeeks.slice(startIndex).map((w, sliceIdx) => {
                  const actualIdx = startIndex + sliceIdx;
                  return (
                    <option key={`${w.year}-${w.week}`} value={actualIdx}>
                      {w.label}
                    </option>
                  );
                })}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Kỹ năng yêu cầu (Tùy chọn)
            </label>
            <input
              type="text"
              value={skillRequirement}
              onChange={(e) => setSkillRequirement(e.target.value)}
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              placeholder="VD: Java, Spring Boot, React..."
            />
          </div>

          {/* Quick Calc Summary */}
          <div className="rounded-xl bg-slate-50 p-3 border border-slate-200 text-xs text-slate-600 flex justify-between items-center">
            <span>Tổng giờ bổ sung mỗi tuần:</span>
            <span className="font-bold text-indigo-600 text-sm">
              {(((typeof headcount === "number" ? headcount : 0) * (typeof hoursPerWeekPerPerson === "number" ? hoursPerWeekPerPerson : 0))).toLocaleString()} giờ/tuần
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
