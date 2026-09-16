"use client";

import React, { useState, useEffect, useCallback } from "react";
import {
  Sparkles,
  Plus,
  Search,
  Building2,
  CalendarRange,
  Users,
  Clock,
  ArrowRight,
  ShieldCheck,
  AlertCircle,
  Layers,
  CheckSquare,
  Square,
  X,
} from "lucide-react";
import { listScenarios, type ScenarioResult } from "@/lib/api/simulation-scenarios";
import { getOrgTree } from "@/lib/api/org-units";
import { CreateSimulationScenarioModal } from "./CreateSimulationScenarioModal";
import { SimulationScenarioDetailView } from "./SimulationScenarioDetailView";
import { SimulationScenarioComparisonView } from "./SimulationScenarioComparisonView";
import { useAuthUser } from "@/lib/auth-session";
import { cn } from "@/lib/utils";
import type { OrgUnitTreeNode } from "@/types/hrm";

export const SimulationScenarioListView: React.FC = () => {
  const user = useAuthUser();
  const normalizedRole = user?.roleCode ? user.roleCode.toUpperCase().replace(/_/g, "-") : "";
  const isVT03 = normalizedRole === "VT-03" || normalizedRole === "ROLE-RM" || normalizedRole === "RM";
  const isVT01 = normalizedRole === "VT-01" || normalizedRole === "ROLE-BGD" || normalizedRole === "BGD" || normalizedRole === "DIRECTOR";
  const canCompare = isVT01 || user?.permissions?.includes("RESOURCE_SCENARIO_COMPARE");

  const [scenarios, setScenarios] = useState<ScenarioResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filter states
  const [selectedOrgUnitId, setSelectedOrgUnitId] = useState<number | undefined>(undefined);
  const [searchQuery, setSearchQuery] = useState("");
  const [orgUnits, setOrgUnits] = useState<{ id: number; name: string }[]>([]);

  // Navigation / Modal states
  const [activeScenarioId, setActiveScenarioId] = useState<number | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  // Scenario Comparison states (NCL.08.CN.004)
  const [isCompareMode, setIsCompareMode] = useState(false);
  const [selectedScenarioIds, setSelectedScenarioIds] = useState<number[]>([]);
  const [isViewingComparison, setIsViewingComparison] = useState(false);

  // Load OrgUnits tree
  useEffect(() => {
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
      } catch (err) {
        console.error("Failed to load org tree:", err);
      }
    };
    loadOrgs();
  }, [user]);

  const loadScenarios = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listScenarios(selectedOrgUnitId);
      setScenarios(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Không thể tải danh sách kịch bản mô phỏng.");
    } finally {
      setLoading(false);
    }
  }, [selectedOrgUnitId]);

  useEffect(() => {
    loadScenarios();
  }, [loadScenarios]);

  const filteredScenarios = scenarios.filter((s) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      s.code.toLowerCase().includes(q) ||
      s.name.toLowerCase().includes(q) ||
      (s.orgUnitName && s.orgUnitName.toLowerCase().includes(q))
    );
  });

  const formatDate = (dateStr: string) => {
    if (!dateStr) return "--:--";
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch {
      return dateStr;
    }
  };

  // If currently viewing multi-scenario comparison (NCL.08.CN.004)
  if (isViewingComparison && selectedScenarioIds.length >= 2) {
    return (
      <SimulationScenarioComparisonView
        scenarioIds={selectedScenarioIds}
        onBack={() => {
          setIsViewingComparison(false);
          loadScenarios();
        }}
      />
    );
  }

  // If a scenario is currently selected, show detail view
  if (activeScenarioId !== null) {
    return (
      <SimulationScenarioDetailView
        scenarioId={activeScenarioId}
        onBack={() => {
          setActiveScenarioId(null);
          loadScenarios();
        }}
      />
    );
  }

  return (
    <div className="space-y-6 pb-12 animate-in fade-in duration-150">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <div className="flex items-center space-x-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-100 shadow-xs">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-slate-900">Mô Phỏng Kịch Bản Nhận Dự Án</h1>
              <p className="text-xs text-slate-500">
                Sandbox đánh giá năng lực nguồn lực tiếp nhận dự án mới (NCL-08 / QTN-14)
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center space-x-2 self-start md:self-auto">
          {canCompare && scenarios.length >= 2 && (
            <button
              onClick={() => {
                setIsCompareMode(!isCompareMode);
                setSelectedScenarioIds([]);
              }}
              className={cn(
                "rounded-xl px-4 py-2.5 text-xs font-semibold transition shadow-xs flex items-center justify-center space-x-2",
                isCompareMode
                  ? "bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-300"
                  : "bg-indigo-50 text-indigo-700 hover:bg-indigo-100 border border-indigo-200"
              )}
            >
              <Layers className="h-4 w-4" />
              <span>{isCompareMode ? "Thoát chế độ so sánh" : "So sánh kịch bản"}</span>
            </button>
          )}

          {isVT03 && (
            <button
              onClick={() => setIsCreateModalOpen(true)}
              className="rounded-xl bg-indigo-600 px-4 py-2.5 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs flex items-center justify-center space-x-2"
            >
              <Plus className="h-4 w-4" />
              <span>Tạo kịch bản mới</span>
            </button>
          )}
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center gap-3 bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
        <div className="relative flex-1 w-full">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Tìm theo mã hoặc tên kịch bản..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-xl border border-slate-200 pl-10 pr-4 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
          />
        </div>

        <div className="flex items-center space-x-2 w-full sm:w-auto">
          <Building2 className="h-4 w-4 text-slate-400 shrink-0" />
          <select
            value={selectedOrgUnitId ?? ""}
            onChange={(e) => {
              const val = e.target.value ? Number(e.target.value) : undefined;
              setSelectedOrgUnitId(val);
            }}
            className="w-full sm:w-56 rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/10"
          >
            <option value="">Tất cả phòng ban</option>
            {orgUnits.map((ou) => (
              <option key={ou.id} value={ou.id}>
                {ou.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Sandbox Isolation Notice */}
      <div className="flex items-center space-x-2.5 text-xs text-slate-500 bg-slate-50 px-4 py-3 rounded-xl border border-slate-200">
        <ShieldCheck className="h-4 w-4 text-emerald-600 shrink-0" />
        <span>
          <strong>Nguyên tắc QTN-14 Sandbox:</strong> Các kịch bản tại đây hoàn toàn độc lập với dữ liệu phân bổ thật. Bạn có thể tự do giả lập các nhu cầu tuyển dụng hoặc dự án mới mà không ảnh hưởng tới tiến độ hiện hành.
        </span>
      </div>

      {/* Scenarios Grid / List */}
      {loading ? (
        <div className="flex items-center justify-center min-h-[300px]">
          <div className="flex flex-col items-center space-y-3">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-indigo-600 border-t-transparent" />
            <p className="text-xs text-slate-500 font-medium">Đang tải danh sách kịch bản...</p>
          </div>
        </div>
      ) : error ? (
        <div className="p-8 text-center bg-white rounded-2xl border border-slate-200 space-y-3">
          <AlertCircle className="h-8 w-8 text-rose-500 mx-auto" />
          <p className="text-xs text-slate-600">{error}</p>
          <button
            onClick={loadScenarios}
            className="text-xs font-semibold text-indigo-600 hover:text-indigo-700"
          >
            Thử lại
          </button>
        </div>
      ) : filteredScenarios.length === 0 ? (
        <div className="p-12 text-center bg-white rounded-2xl border border-slate-200 space-y-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 mx-auto">
            <Sparkles className="h-6 w-6" />
          </div>
          <h3 className="text-sm font-bold text-slate-900">Chưa có kịch bản mô phỏng nào</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            {isVT03
              ? "Bấm 'Tạo kịch bản mới' để chụp ảnh snapshot phân bổ và mô phỏng nhận thêm dự án."
              : "Hiện tại chưa có kịch bản mô phỏng nào được tạo cho phòng ban này."}
          </p>
          {isVT03 && (
            <button
              onClick={() => setIsCreateModalOpen(true)}
              className="mt-2 inline-flex items-center space-x-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition"
            >
              <Plus className="h-4 w-4" />
              <span>Tạo kịch bản ngay</span>
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredScenarios.map((scn) => {
            const isSelected = selectedScenarioIds.includes(scn.id);

            return (
              <div
                key={scn.id}
                onClick={() => {
                  if (isCompareMode) {
                    if (isSelected) {
                      setSelectedScenarioIds((prev) => prev.filter((id) => id !== scn.id));
                    } else {
                      if (selectedScenarioIds.length >= 10) {
                        alert("Chỉ được chọn tối đa 10 kịch bản để so sánh.");
                        return;
                      }
                      setSelectedScenarioIds((prev) => [...prev, scn.id]);
                    }
                  } else {
                    setActiveScenarioId(scn.id);
                  }
                }}
                className={cn(
                  "group bg-white rounded-2xl border p-5 shadow-xs transition cursor-pointer flex flex-col justify-between space-y-4",
                  isCompareMode && isSelected
                    ? "border-indigo-600 ring-2 ring-indigo-500/25 shadow-md bg-indigo-50/15"
                    : "border-slate-200 hover:shadow-md hover:border-indigo-200"
                )}
              >
                <div className="space-y-2.5">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      {isCompareMode && (
                        <div className="text-indigo-600">
                          {isSelected ? (
                            <CheckSquare className="h-4 w-4 text-indigo-600" />
                          ) : (
                            <Square className="h-4 w-4 text-slate-300 group-hover:text-slate-400" />
                          )}
                        </div>
                      )}
                      <span className="font-mono text-[11px] font-bold text-indigo-600 bg-indigo-50 px-2.5 py-0.5 rounded-md border border-indigo-100">
                        {scn.code}
                      </span>
                    </div>
                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                      Bản nháp
                    </span>
                  </div>

                  <h3 className="text-sm font-bold text-slate-900 group-hover:text-indigo-600 transition line-clamp-1">
                    {scn.name}
                  </h3>

                  {scn.description && (
                    <p className="text-xs text-slate-500 line-clamp-2 leading-relaxed">
                      {scn.description}
                    </p>
                  )}

                  <div className="pt-2 space-y-1.5 text-xs text-slate-600 border-t border-slate-100">
                    <div className="flex items-center text-[11px]">
                      <Building2 className="h-3.5 w-3.5 mr-1.5 text-slate-400 shrink-0" />
                      <span className="font-medium text-slate-700 truncate">{scn.orgUnitName}</span>
                    </div>
                    <div className="flex items-center text-[11px]">
                      <CalendarRange className="h-3.5 w-3.5 mr-1.5 text-slate-400 shrink-0" />
                      <span>
                        Tuần {scn.fromWeek ?? scn.startWeek} → Tuần {(scn.fromWeek ?? scn.startWeek ?? 1) + scn.durationWeeks - 1}, {scn.fromYear ?? scn.startYear} ({scn.durationWeeks}w)
                      </span>
                    </div>
                    <div className="flex items-center text-[11px]">
                      <Users className="h-3.5 w-3.5 mr-1.5 text-slate-400 shrink-0" />
                      <span>
                        {scn.demandsCount ?? scn.demandCount ?? 0} nhu cầu giả định • {scn.snapshotEmployeesCount ?? scn.totalSnapshotEmployees ?? 0} nhân sự snapshot
                      </span>
                    </div>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
                  <div className="flex items-center">
                    <Clock className="h-3 w-3 mr-1" />
                    <span>Tạo ngày: {formatDate(scn.createdAt)}</span>
                  </div>
                  <div className="flex items-center font-semibold text-indigo-600 group-hover:translate-x-0.5 transition">
                    <span>{isCompareMode ? (isSelected ? "Đã chọn" : "Chọn so sánh") : "Mô phỏng"}</span>
                    <ArrowRight className="h-3.5 w-3.5 ml-1" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Sticky Compare Action Bar */}
      {isCompareMode && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-40 bg-slate-900/95 backdrop-blur text-white px-5 py-3 rounded-2xl shadow-xl border border-slate-700 flex items-center space-x-4 animate-in slide-in-from-bottom-5">
          <div className="flex items-center space-x-2 text-xs">
            <span className="font-bold text-indigo-400">
              Đã chọn: {selectedScenarioIds.length}/10
            </span>
            <span className="text-slate-300">(Cần tối thiểu 2 kịch bản)</span>
          </div>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => {
                if (selectedScenarioIds.length < 2) return;
                setIsViewingComparison(true);
              }}
              disabled={selectedScenarioIds.length < 2}
              className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 text-white font-semibold text-xs px-3.5 py-2 rounded-xl transition shadow-xs flex items-center space-x-1.5 cursor-pointer disabled:cursor-not-allowed"
            >
              <Layers className="h-3.5 w-3.5" />
              <span>So sánh ngay</span>
            </button>
            <button
              onClick={() => {
                setIsCompareMode(false);
                setSelectedScenarioIds([]);
              }}
              className="text-slate-400 hover:text-white p-1.5 rounded-xl hover:bg-slate-800 transition"
              title="Đóng chế độ so sánh"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}

      {/* Modal create scenario */}
      {isCreateModalOpen && (
        <CreateSimulationScenarioModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
          onSuccess={(created) => {
            setActiveScenarioId(created.id);
          }}
        />
      )}
    </div>
  );
};
