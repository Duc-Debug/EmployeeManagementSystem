"use client";

import React, { useState, useEffect } from "react";
import {
  X,
  Plus,
  Layers,
  Sparkles,
  AlertCircle,
  Loader2,
  CheckCircle2,
  FolderDown,
  Clock,
} from "lucide-react";
import {
  getRoleAllocationTemplates,
  extractRoleStructureFromProject,
  createRoleAllocationTemplate,
  type RoleAllocationTemplateSummary,
  type RoleAllocationTemplateDetail,
  getRoleAllocationTemplate,
  type ProjectRoleStructureItem,
} from "@/lib/api/role-allocation-templates";
import { getProjects, type ProjectResult } from "@/lib/api/projects";
import { getProjectRoles, type ProjectRoleResponse } from "@/lib/api/project-roles";
import { ApplyRoleAllocationTemplateModal } from "./ApplyRoleAllocationTemplateModal";

interface RoleAllocationTemplateManagementModalProps {
  open: boolean;
  onClose: () => void;
  initialSourceProjectId?: number;
  onAppliedSuccess?: () => void;
}

export function RoleAllocationTemplateManagementModal({
  open,
  onClose,
  initialSourceProjectId,
  onAppliedSuccess,
}: RoleAllocationTemplateManagementModalProps) {
  const [activeTab, setActiveTab] = useState<"list" | "create">("list");
  const [templates, setTemplates] = useState<RoleAllocationTemplateSummary[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<RoleAllocationTemplateDetail | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Form states for creating template from project
  const [projects, setProjects] = useState<ProjectResult[]>([]);
  const [sourceProjectId, setSourceProjectId] = useState<number | "">("");
  const [templateCode, setTemplateCode] = useState<string>("");
  const [templateName, setTemplateName] = useState<string>("");
  const [templateDescription, setTemplateDescription] = useState<string>("");
  const [extractedRoles, setExtractedRoles] = useState<ProjectRoleStructureItem[]>([]);
  const [loadingExtraction, setLoadingExtraction] = useState<boolean>(false);
  const [saving, setSaving] = useState<boolean>(false);
  const [catalogRoles, setCatalogRoles] = useState<ProjectRoleResponse[]>([]);
  const [selectedAddRoleId, setSelectedAddRoleId] = useState<number | "">("");
  const [addRoleHours, setAddRoleHours] = useState<number>(40);

  // Apply modal states
  const [applyingTemplateId, setApplyingTemplateId] = useState<number | null>(null);

  useEffect(() => {
    if (open) {
      loadTemplates();
      loadProjectList();
      loadCatalogRoles();
      if (initialSourceProjectId) {
        setSourceProjectId(initialSourceProjectId);
        setActiveTab("create");
        handleSelectSourceProject(initialSourceProjectId);
      }
    } else {
      setSelectedTemplate(null);
      setError(null);
      setSuccessMsg(null);
    }
  }, [open, initialSourceProjectId]);

  const loadCatalogRoles = async () => {
    try {
      const data = await getProjectRoles(false);
      setCatalogRoles(data || []);
      if (data && data.length > 0) {
        setSelectedAddRoleId(data[0].id);
      }
    } catch (err) {
      console.error("Failed to load catalog roles", err);
    }
  };

  const loadTemplates = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getRoleAllocationTemplates();
      setTemplates(data);
      if (data.length > 0 && !selectedTemplate) {
        handleViewTemplate(data[0].id);
      }
    } catch (err: any) {
      setError(err?.message || "Không thể tải danh sách mẫu phân bổ");
    } finally {
      setLoading(false);
    }
  };

  const loadProjectList = async () => {
    try {
      const res = await getProjects(0, 100);
      setProjects(res.content || []);
    } catch (err) {
      console.error("Failed to load projects", err);
    }
  };

  const handleAddRoleManually = () => {
    if (!selectedAddRoleId) return;
    const role = catalogRoles.find((r) => r.id === Number(selectedAddRoleId));
    if (!role) return;

    if (extractedRoles.some((r) => r.roleId === role.id)) {
      setError(`Vai trò "${role.name}" đã có trong danh sách cơ cấu.`);
      return;
    }

    setExtractedRoles((prev) => [
      ...prev,
      {
        roleId: role.id,
        roleCode: role.code,
        roleName: role.name,
        hoursPerWeek: addRoleHours > 0 ? addRoleHours : 40,
      },
    ]);
    setError(null);
  };

  const handleViewTemplate = async (id: number) => {
    try {
      const detail = await getRoleAllocationTemplate(id);
      setSelectedTemplate(detail);
    } catch (err: any) {
      setError(err?.message || "Không thể xem chi tiết mẫu");
    }
  };

  const handleSelectSourceProject = async (projectId: number) => {
    setSourceProjectId(projectId);
    setLoadingExtraction(true);
    setError(null);
    try {
      const roles = await extractRoleStructureFromProject(projectId);
      setExtractedRoles(roles);
      const proj = projects.find((p) => p.id === projectId);
      if (proj) {
        if (!templateCode) setTemplateCode(`TPL_${proj.projectCode.replace(/[^A-Za-z0-9]/g, "_").toUpperCase()}`);
        if (!templateName) setTemplateName(`Mẫu phân bổ cơ cấu ${proj.projectName}`);
      }
    } catch (err: any) {
      setError(err?.message || "Lỗi khi trích xuất cơ cấu vai trò từ dự án");
    } finally {
      setLoadingExtraction(false);
    }
  };

  const handleUpdateRoleHours = (roleId: number, newHours: number) => {
    setExtractedRoles((prev) =>
      prev.map((r) => (r.roleId === roleId ? { ...r, hoursPerWeek: Math.max(0.1, newHours) } : r))
    );
  };

  const handleRemoveRole = (roleId: number) => {
    setExtractedRoles((prev) => prev.filter((r) => r.roleId !== roleId));
  };

  const handleSaveTemplate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!templateCode.trim() || !templateName.trim()) {
      setError("Vui lòng nhập đầy đủ mã và tên mẫu phân bổ");
      return;
    }
    if (extractedRoles.length === 0) {
      setError("Mẫu phân bổ cần ít nhất 1 vai trò");
      return;
    }

    setSaving(true);
    setError(null);
    try {
      await createRoleAllocationTemplate({
        templateCode: templateCode.trim(),
        name: templateName.trim(),
        description: templateDescription.trim(),
        sourceProjectId: typeof sourceProjectId === "number" ? sourceProjectId : null,
        items: extractedRoles.map((r) => ({
          roleId: r.roleId,
          hoursPerWeek: r.hoursPerWeek,
        })),
      });

      setSuccessMsg("Lưu mẫu phân bổ theo vai trò thành công!");
      setActiveTab("list");
      setTemplateCode("");
      setTemplateName("");
      setTemplateDescription("");
      setExtractedRoles([]);
      setSourceProjectId("");
      await loadTemplates();
    } catch (err: any) {
      setError(err?.message || "Không thể lưu mẫu phân bổ");
    } finally {
      setSaving(false);
    }
  };

  if (!open) return null;

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-xs">
        <div className="flex w-full max-w-4xl flex-col max-h-[90vh] rounded-3xl bg-white shadow-2xl overflow-hidden border border-slate-100 animate-in fade-in zoom-in-95 duration-200">
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/50">
            <div className="flex items-center gap-2.5">
              <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 shadow-2xs">
                <Layers className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-800">
                  Mẫu phân bổ theo vai trò của dự án
                </h3>
                <p className="text-xs text-slate-500">
                  Lưu cơ cấu vai trò và áp nhanh cho các dự án cùng loại
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Navigation Tabs */}
          <div className="flex border-b border-slate-100 px-6 pt-2 bg-white gap-2">
            <button
              type="button"
              onClick={() => setActiveTab("list")}
              className={`pb-3 px-3 text-xs font-bold transition border-b-2 ${
                activeTab === "list"
                  ? "border-indigo-600 text-indigo-600"
                  : "border-transparent text-slate-500 hover:text-slate-700"
              }`}
            >
              Danh sách mẫu đã lưu ({templates.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("create")}
              className={`pb-3 px-3 text-xs font-bold transition border-b-2 flex items-center gap-1.5 ${
                activeTab === "create"
                  ? "border-indigo-600 text-indigo-600"
                  : "border-transparent text-slate-500 hover:text-slate-700"
              }`}
            >
              <Plus className="h-3.5 w-3.5" />
              <span>Lưu mẫu từ dự án nguồn</span>
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {error && (
              <div className="flex items-center gap-2 rounded-2xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {successMsg && (
              <div className="flex items-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-700">
                <CheckCircle2 className="h-4 w-4 shrink-0" />
                <span>{successMsg}</span>
              </div>
            )}

            {activeTab === "list" && (
              <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
                {/* Templates List */}
                <div className="md:col-span-5 space-y-2 border-r border-slate-100 pr-4">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                    Các mẫu hiện có
                  </h4>
                  {loading && (
                    <div className="py-8 text-center text-xs text-slate-400">
                      <Loader2 className="h-5 w-5 animate-spin mx-auto mb-2 text-indigo-500" />
                      Đang tải danh sách mẫu...
                    </div>
                  )}
                  {!loading && templates.length === 0 && (
                    <div className="rounded-2xl border border-dashed border-slate-200 p-6 text-center text-xs text-slate-400">
                      Chưa có mẫu phân bổ nào. Hãy tạo mẫu đầu tiên từ dự án có sẵn.
                    </div>
                  )}
                  {!loading &&
                    templates.map((tpl) => (
                      <div
                        key={tpl.id}
                        onClick={() => handleViewTemplate(tpl.id)}
                        className={`p-3.5 rounded-2xl border transition cursor-pointer ${
                          selectedTemplate?.id === tpl.id
                            ? "border-indigo-500 bg-indigo-50/40 shadow-xs"
                            : "border-slate-200 hover:border-slate-300 bg-white"
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-100/60 px-2 py-0.5 rounded-md">
                            {tpl.templateCode}
                          </span>
                          <span className="text-[11px] text-slate-400">
                            {tpl.itemsCount} vai trò
                          </span>
                        </div>
                        <h5 className="mt-1.5 text-xs font-bold text-slate-800 line-clamp-1">
                          {tpl.name}
                        </h5>
                        {tpl.description && (
                          <p className="mt-0.5 text-[11px] text-slate-500 line-clamp-1">
                            {tpl.description}
                          </p>
                        )}
                      </div>
                    ))}
                </div>

                {/* Template Detail View */}
                <div className="md:col-span-7 space-y-4">
                  {selectedTemplate ? (
                    <div className="space-y-4">
                      <div className="flex items-start justify-between">
                        <div>
                          <div className="flex items-center gap-2">
                            <h4 className="text-sm font-bold text-slate-900">
                              {selectedTemplate.name}
                            </h4>
                            <span className="font-mono text-xs text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-md font-bold">
                              {selectedTemplate.templateCode}
                            </span>
                          </div>
                          {selectedTemplate.description && (
                            <p className="mt-1 text-xs text-slate-600">
                              {selectedTemplate.description}
                            </p>
                          )}
                        </div>
                        <button
                          type="button"
                          onClick={() => setApplyingTemplateId(selectedTemplate.id)}
                          className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3.5 py-2 text-xs font-bold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 transition cursor-pointer"
                        >
                          <Sparkles className="h-3.5 w-3.5" />
                          <span>Áp dụng vào dự án</span>
                        </button>
                      </div>

                      <div className="rounded-2xl border border-slate-200 overflow-hidden bg-white">
                        <div className="bg-slate-50 px-4 py-2.5 border-b border-slate-100 text-xs font-bold text-slate-700">
                          Cơ cấu vai trò & số giờ yêu cầu / tuần
                        </div>
                        <div className="divide-y divide-slate-100">
                          {selectedTemplate.items?.map((item) => (
                            <div
                              key={item.id}
                              className="px-4 py-2.5 flex items-center justify-between text-xs"
                            >
                              <div className="flex items-center gap-2">
                                <span className="font-mono text-[11px] font-semibold text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded">
                                  {item.roleCode}
                                </span>
                                <span className="font-medium text-slate-800">
                                  {item.roleName}
                                </span>
                              </div>
                              <div className="flex items-center gap-1 font-mono font-bold text-indigo-600">
                                <Clock className="h-3.5 w-3.5 text-slate-400" />
                                <span>{item.hoursPerWeek}h / tuần</span>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="h-full flex items-center justify-center p-8 text-center text-xs text-slate-400">
                      Chọn một mẫu phân bổ bên trái để xem cơ cấu chi tiết
                    </div>
                  )}
                </div>
              </div>
            )}

            {activeTab === "create" && (
              <form onSubmit={handleSaveTemplate} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Select Source Project */}
                  <div className="sm:col-span-2">
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      1. Chọn dự án nguồn có cơ cấu phân bổ chuẩn <span className="text-rose-500">*</span>
                    </label>
                    <select
                      value={sourceProjectId}
                      onChange={(e) => handleSelectSourceProject(Number(e.target.value))}
                      className="w-full rounded-xl border border-slate-200 bg-white p-2.5 text-xs text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                      required
                    >
                      <option value="">-- Chọn dự án hoàn chỉnh để trích xuất cơ cấu --</option>
                      {projects.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.projectCode} - {p.projectName} ({p.status})
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Template Code */}
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      2. Mã mẫu phân bổ <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="text"
                      value={templateCode}
                      onChange={(e) => setTemplateCode(e.target.value)}
                      placeholder="VD: TPL_WEB_STANDARD"
                      className="w-full rounded-xl border border-slate-200 bg-white p-2.5 text-xs text-slate-800 font-mono outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                      required
                    />
                  </div>

                  {/* Template Name */}
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      3. Tên mẫu phân bổ <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="text"
                      value={templateName}
                      onChange={(e) => setTemplateName(e.target.value)}
                      placeholder="VD: Mẫu dự án Web tiêu chuẩn"
                      className="w-full rounded-xl border border-slate-200 bg-white p-2.5 text-xs text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                      required
                    />
                  </div>

                  {/* Description */}
                  <div className="sm:col-span-2">
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Mô tả mẫu phân bổ
                    </label>
                    <textarea
                      value={templateDescription}
                      onChange={(e) => setTemplateDescription(e.target.value)}
                      placeholder="Ghi chú về cơ cấu nhân sự, phạm vi áp dụng..."
                      rows={2}
                      className="w-full rounded-xl border border-slate-200 bg-white p-2.5 text-xs text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                    />
                  </div>
                </div>

                  {/* Extracted Roles Section */}
                  <div className="space-y-3 pt-2 border-t border-slate-100">
                    <div className="flex items-center justify-between">
                      <div>
                        <h4 className="text-xs font-bold text-slate-800">
                          4. Cơ cấu vai trò & số giờ mỗi tuần <span className="text-rose-500">*</span>
                        </h4>
                        <p className="text-[11px] text-slate-500">
                          {extractedRoles.length > 0
                            ? `Đã có ${extractedRoles.length} vai trò trong mẫu phân bổ.`
                            : "Dự án nguồn chưa có cơ cấu sẵn, bạn hãy thêm các vai trò bên dưới để hoàn tất mẫu."}
                        </p>
                      </div>
                      {loadingExtraction && (
                        <span className="text-xs text-indigo-600 flex items-center gap-1">
                          <Loader2 className="h-3.5 w-3.5 animate-spin" />
                          Đang đọc cơ cấu từ dự án...
                        </span>
                      )}
                    </div>

                    {/* Quick Add Role Section */}
                    <div className="flex flex-wrap items-end gap-2 p-3 bg-indigo-50/50 border border-indigo-100 rounded-xl">
                      <div className="flex-1 min-w-[220px]">
                        <label className="block text-[11px] font-bold text-slate-700 mb-1">
                          Thêm vai trò vào mẫu:
                        </label>
                        <select
                          value={selectedAddRoleId}
                          onChange={(e) => setSelectedAddRoleId(Number(e.target.value))}
                          className="w-full rounded-lg border border-slate-200 bg-white p-2 text-xs text-slate-800 outline-none focus:border-indigo-500"
                        >
                          {catalogRoles.length === 0 && (
                            <option value="">-- Đang tải vai trò --</option>
                          )}
                          {catalogRoles.map((r) => (
                            <option key={r.id} value={r.id}>
                              {r.code} - {r.name}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="w-28">
                        <label className="block text-[11px] font-bold text-slate-700 mb-1">
                          Số giờ / tuần:
                        </label>
                        <input
                          type="number"
                          step="0.5"
                          min="0.5"
                          max="168"
                          value={addRoleHours}
                          onChange={(e) => setAddRoleHours(parseFloat(e.target.value) || 0)}
                          className="w-full rounded-lg border border-slate-200 bg-white p-2 text-xs font-mono font-bold text-slate-800 text-right outline-none focus:border-indigo-500"
                        />
                      </div>
                      <div>
                        <button
                          type="button"
                          onClick={handleAddRoleManually}
                          disabled={!selectedAddRoleId || catalogRoles.length === 0}
                          className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3.5 py-2 text-xs font-bold text-white shadow-sm hover:bg-indigo-700 transition disabled:opacity-50 cursor-pointer"
                        >
                          <Plus className="h-3.5 w-3.5" />
                          <span>+ Thêm vai trò</span>
                        </button>
                      </div>
                    </div>

                    {extractedRoles.length === 0 && !loadingExtraction && (
                      <div className="rounded-2xl border border-dashed border-slate-200 p-5 text-center text-xs text-slate-500 bg-slate-50/50">
                        Chưa có vai trò nào trong cơ cấu mẫu. Hãy chọn vai trò ở trên và nhấn <strong>"+ Thêm vai trò"</strong> (ví dụ: Developer 40h, Tester 20h, BA 20h...).
                      </div>
                    )}

                    {extractedRoles.length > 0 && (
                      <div className="rounded-2xl border border-slate-200 overflow-hidden bg-white divide-y divide-slate-100">
                        {extractedRoles.map((role) => (
                          <div
                            key={role.roleId}
                            className="p-3 flex items-center justify-between gap-4 text-xs hover:bg-slate-50/60 transition"
                          >
                            <div className="flex items-center gap-2">
                              <span className="font-mono text-xs font-semibold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
                                {role.roleCode}
                              </span>
                              <span className="font-bold text-slate-800">
                                {role.roleName}
                              </span>
                            </div>
                            <div className="flex items-center gap-3">
                              <div className="flex items-center gap-1.5">
                                <label className="text-[11px] text-slate-500">Giờ/tuần:</label>
                                <input
                                  type="number"
                                  step="0.5"
                                  min="0.5"
                                  max="168"
                                  value={role.hoursPerWeek}
                                  onChange={(e) =>
                                    handleUpdateRoleHours(role.roleId, parseFloat(e.target.value) || 0)
                                  }
                                  className="w-20 rounded-lg border border-slate-200 px-2 py-1 text-xs font-mono font-bold text-slate-800 text-right outline-none focus:border-indigo-500"
                                />
                              </div>
                              <button
                                type="button"
                                onClick={() => handleRemoveRole(role.roleId)}
                                className="text-rose-500 hover:text-rose-700 p-1 text-xs cursor-pointer font-medium"
                                title="Loại bỏ vai trò này khỏi mẫu"
                              >
                                Xóa
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                {/* Submit button */}
                <div className="flex items-center justify-end gap-2 pt-3">
                  <button
                    type="button"
                    onClick={() => setActiveTab("list")}
                    className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 transition"
                  >
                    Hủy bỏ
                  </button>
                  <button
                    type="submit"
                    disabled={saving || extractedRoles.length === 0}
                    className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-5 py-2 text-xs font-bold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 transition disabled:opacity-50 cursor-pointer"
                  >
                    {saving ? (
                      <>
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        <span>Đang lưu...</span>
                      </>
                    ) : (
                      <>
                        <FolderDown className="h-3.5 w-3.5" />
                        <span>Lưu mẫu phân bổ</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      </div>

      {/* Apply Modal */}
      {applyingTemplateId && (
        <ApplyRoleAllocationTemplateModal
          open={Boolean(applyingTemplateId)}
          templateId={applyingTemplateId}
          onClose={() => setApplyingTemplateId(null)}
          onSuccess={() => {
            setApplyingTemplateId(null);
            onClose();
            if (onAppliedSuccess) onAppliedSuccess();
          }}
        />
      )}
    </>
  );
}
