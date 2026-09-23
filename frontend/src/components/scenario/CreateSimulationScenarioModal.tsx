"use client";

import React, { useState, useEffect } from "react";
import { X, Sparkles, AlertCircle } from "lucide-react";
import { getOrgTree } from "@/lib/api/org-units";
import { createScenario, type CreateScenarioPayload, type ScenarioResult } from "@/lib/api/simulation-scenarios";
import { useAuthUser } from "@/lib/auth-session";
import type { OrgUnitTreeNode } from "@/types/hrm";

interface CreateSimulationScenarioModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (scenario: ScenarioResult) => void;
}

export const CreateSimulationScenarioModal: React.FC<CreateSimulationScenarioModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const user = useAuthUser();

  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [orgUnitId, setOrgUnitId] = useState<number>(0);
  const [startYear, setStartYear] = useState<number>(new Date().getFullYear());
  const [startWeek, setStartWeek] = useState<number>(38);
  const [durationWeeks, setDurationWeeks] = useState<number>(8);

  const [orgUnits, setOrgUnits] = useState<{ id: number; name: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) return;

    // Default code generation
    const timestamp = Math.floor(1000 + Math.random() * 9000);
    setCode(`SCN-${new Date().getFullYear()}-${timestamp}`);
    setName("");
    setDescription("");
    setError(null);

    const loadOrgs = async () => {
      try {
        const tree = await getOrgTree();
        const flatList: { id: number; name: string }[] = [];

        const flatten = (nodes: readonly OrgUnitTreeNode[]) => {
          for (const node of nodes) {
            flatList.push({ id: node.id, name: node.unitName });
            if (node.children && node.children.length > 0) {
              flatten(node.children);
            }
          }
        };

        const findNode = (nodes: readonly OrgUnitTreeNode[], targetId: number): OrgUnitTreeNode | null => {
          for (const node of nodes) {
            if (node.id === targetId) return node;
            if (node.children && node.children.length > 0) {
              const found = findNode(node.children, targetId);
              if (found) return found;
            }
          }
          return null;
        };

        if (user?.dataScope === "ORGANIZATION_BRANCH" && user.scopeOrgUnitId) {
          const branchRoot = findNode(tree, user.scopeOrgUnitId);
          if (branchRoot) {
            flatten([branchRoot]);
          } else {
            flatten(tree);
          }
        } else {
          flatten(tree);
        }

        setOrgUnits(flatList);
        if (flatList.length > 0) {
          setOrgUnitId(flatList[0].id);
        }
      } catch (err) {
        console.error("Failed to load org tree:", err);
      }
    };

    loadOrgs();
  }, [isOpen, user]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError("Vui lòng nhập tên kịch bản");
      return;
    }
    if (!code.trim()) {
      setError("Vui lòng nhập mã kịch bản");
      return;
    }
    if (!orgUnitId) {
      setError("Vui lòng chọn phòng ban mô phỏng");
      return;
    }
    if (startWeek < 1 || startWeek > 53) {
      setError("Tuần bắt đầu phải từ 1 đến 53");
      return;
    }
    if (durationWeeks < 1 || durationWeeks > 16) {
      setError("Số tuần mô phỏng phải từ 1 đến 16 tuần");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const payload: CreateScenarioPayload = {
        code: code.trim(),
        name: name.trim(),
        description: description.trim() || undefined,
        orgUnitId,
        fromYear: startYear,
        fromWeek: startWeek,
        durationWeeks,
      };

      const result = await createScenario(payload);
      onSuccess(result);
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Đã có lỗi xảy ra khi tạo kịch bản.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/50">
          <div className="flex items-center space-x-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">Tạo Kịch Bản Mô Phỏng</h3>
              <p className="text-xs text-slate-500">Mô phỏng sandbox đánh giá năng lực nhận thêm dự án</p>
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
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
          {error && (
            <div className="flex items-start space-x-2.5 rounded-xl bg-rose-50 p-3.5 text-xs text-rose-700 border border-rose-100 animate-in fade-in">
              <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
              <div className="flex-1 font-medium">{error}</div>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Mã kịch bản <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                required
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs font-mono focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
                placeholder="SCN-2026-001"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Phòng ban / Chi nhánh <span className="text-rose-500">*</span>
              </label>
              <select
                value={orgUnitId}
                onChange={(e) => setOrgUnitId(Number(e.target.value))}
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              >
                {orgUnits.map((ou) => (
                  <option key={ou.id} value={ou.id}>
                    {ou.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Tên kịch bản <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              placeholder="Ví dụ: Đánh giá tiếp nhận dự án Core Banking Q4"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Mô tả mục tiêu kịch bản</label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
              placeholder="Ghi chú giả định tiếp nhận, quy mô dự án..."
            />
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Năm</label>
              <input
                type="number"
                min={2020}
                max={2035}
                value={startYear}
                onChange={(e) => setStartYear(Number(e.target.value))}
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Tuần bắt đầu (1-53)</label>
              <input
                type="number"
                min={1}
                max={53}
                value={startWeek}
                onChange={(e) => setStartWeek(Number(e.target.value))}
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Số tuần (1-16)</label>
              <input
                type="number"
                min={1}
                max={16}
                value={durationWeeks}
                onChange={(e) => setDurationWeeks(Number(e.target.value))}
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden"
              />
            </div>
          </div>

          <div className="rounded-xl bg-amber-50/60 p-3 border border-amber-200/60 text-xs text-amber-800 space-y-1">
            <p className="font-semibold flex items-center">
              <span className="inline-block w-1.5 h-1.5 rounded-full bg-amber-600 mr-1.5" />
              Cơ chế Sandbox an toàn:
            </p>
            <p className="text-[11px] text-amber-700 leading-relaxed">
              Hệ thống sẽ chụp ảnh snapshot phân bổ thực tế tại thời điểm bấm &quot;Khởi tạo kịch bản&quot;. Dữ liệu phân bổ thật sẽ không bị sửa đổi hay xóa.
            </p>
          </div>

          {/* Modal Footer */}
          <div className="flex items-center justify-end space-x-3 pt-4 border-t border-slate-100">
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
              className="rounded-xl bg-indigo-600 px-5 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition disabled:opacity-50 shadow-xs flex items-center space-x-1.5"
            >
              {loading ? "Đang chụp snapshot..." : "Khởi tạo kịch bản"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
