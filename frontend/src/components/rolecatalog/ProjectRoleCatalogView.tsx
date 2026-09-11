"use client";

import React, { useState, useEffect, useMemo, useCallback } from "react";
import {
  Briefcase,
  Plus,
  Search,
  Filter,
  Edit2,
  PowerOff,
  RotateCcw,
  AlertTriangle,
  CheckCircle2,
  X,
  Loader2,
  ShieldAlert,
  Layers,
  Info,
  Check,
} from "lucide-react";
import { useAuthUser } from "@/lib/auth-session";
import {
  getProjectRoles,
  createProjectRole,
  updateProjectRole,
  deactivateProjectRole,
  activateProjectRole,
  checkProjectRoleUsage,
  type ProjectRoleResponse,
  type ProjectRoleUsageResponse,
} from "@/lib/api/project-roles";
import { getSkillGroups, type SkillGroupResponse } from "@/lib/api/skills";
import { ApiError } from "@/lib/api-client";

export default function ProjectRoleCatalogView() {
  const currentUser = useAuthUser();
  const roleCode = currentUser?.roleCode?.toUpperCase().replace(/_/g, "-") || "";
  const isAdmin = roleCode === "VT-06";

  // Data States
  const [roles, setRoles] = useState<ProjectRoleResponse[]>([]);
  const [skillGroups, setSkillGroups] = useState<SkillGroupResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [selectedGroupFilter, setSelectedGroupFilter] = useState<string>("ALL");
  const [includeInactive, setIncludeInactive] = useState<boolean>(true);

  // Notification State
  const [notification, setNotification] = useState<{
    type: "success" | "error" | "info";
    message: string;
  } | null>(null);

  const showNotification = (type: "success" | "error" | "info", message: string) => {
    setNotification({ type, message });
    setTimeout(() => {
      setNotification((prev) => (prev?.message === message ? null : prev));
    }, 5000);
  };

  // Modals State
  const [isCreateOpen, setIsCreateOpen] = useState<boolean>(false);
  const [editingRole, setEditingRole] = useState<ProjectRoleResponse | null>(null);
  const [deactivatingRole, setDeactivatingRole] = useState<ProjectRoleResponse | null>(null);
  const [usageInfo, setUsageInfo] = useState<ProjectRoleUsageResponse | null>(null);
  const [isCheckingUsage, setIsCheckingUsage] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Form Fields
  const [formCode, setFormCode] = useState<string>("");
  const [formName, setFormName] = useState<string>("");
  const [formSkillGroupId, setFormSkillGroupId] = useState<number | "">("");
  const [formDescription, setFormDescription] = useState<string>("");

  // Load Initial Data
  const loadRoles = useCallback(async (withInactive: boolean) => {
    try {
      setIsLoading(true);
      const [fetchedRoles, fetchedGroups] = await Promise.all([
        getProjectRoles(withInactive),
        getSkillGroups().catch(() => []),
      ]);
      setRoles(fetchedRoles);
      setSkillGroups(fetchedGroups.filter((g) => g.status === "ACTIVE"));
    } catch (err) {
      console.error("Failed to load project roles or skill groups:", err);
      showNotification("error", "Không thể tải danh mục vai trò chuyên môn hoặc nhóm kỹ năng.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRoles(includeInactive);
  }, [includeInactive, loadRoles]);

  // Filtered Roles
  const filteredRoles = useMemo(() => {
    return roles.filter((role) => {
      const matchSearch =
        searchQuery.trim() === "" ||
        role.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
        role.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (role.description && role.description.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchGroup =
        selectedGroupFilter === "ALL" ||
        (role.skillGroupId != null && String(role.skillGroupId) === selectedGroupFilter);

      return matchSearch && matchGroup;
    });
  }, [roles, searchQuery, selectedGroupFilter]);

  // Open Create Modal
  const handleOpenCreate = () => {
    setFormCode("");
    setFormName("");
    setFormSkillGroupId(skillGroups.length > 0 ? skillGroups[0].id : "");
    setFormDescription("");
    setFormError(null);
    setIsCreateOpen(true);
  };

  // Open Edit Modal
  const handleOpenEdit = (role: ProjectRoleResponse) => {
    setEditingRole(role);
    setFormCode(role.code);
    setFormName(role.name);
    setFormSkillGroupId(role.skillGroupId || (skillGroups.length > 0 ? skillGroups[0].id : ""));
    setFormDescription(role.description || "");
    setFormError(null);
  };

  // Open Deactivate Confirmation Modal
  const handleOpenDeactivate = async (role: ProjectRoleResponse) => {
    setDeactivatingRole(role);
    setIsCheckingUsage(true);
    setUsageInfo(null);
    try {
      const usage = await checkProjectRoleUsage(role.id);
      setUsageInfo(usage);
    } catch (err) {
      console.warn("Failed to check project role usage:", err);
      setUsageInfo({
        roleId: role.id,
        demandCount: 0,
        employeeCount: 0,
        inUse: false,
      });
    } finally {
      setIsCheckingUsage(false);
    }
  };

  // Submit Create
  const handleSubmitCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const cleanCode = formCode.trim().toUpperCase();
    const cleanName = formName.trim();

    if (!cleanCode) {
      setFormError("Vui lòng nhập mã vai trò.");
      return;
    }
    if (!cleanName) {
      setFormError("Vui lòng nhập tên vai trò chuyên môn.");
      return;
    }
    if (!formSkillGroupId) {
      setFormError("Vui lòng chọn nhóm kỹ năng tương ứng.");
      return;
    }

    try {
      setIsSubmitting(true);
      const created = await createProjectRole({
        code: cleanCode,
        name: cleanName,
        skillGroupId: Number(formSkillGroupId),
        description: formDescription.trim() || undefined,
      });
      showNotification("success", `Đã tạo thành công vai trò "${created.name}" (${created.code}).`);
      setIsCreateOpen(false);
      await loadRoles(includeInactive);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : "Có lỗi xảy ra khi tạo vai trò chuyên môn.";
      setFormError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Submit Edit
  const handleSubmitEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingRole) return;
    setFormError(null);

    const cleanName = formName.trim();
    if (!cleanName) {
      setFormError("Vui lòng nhập tên vai trò chuyên môn.");
      return;
    }
    if (!formSkillGroupId) {
      setFormError("Vui lòng chọn nhóm kỹ năng tương ứng.");
      return;
    }

    try {
      setIsSubmitting(true);
      const updated = await updateProjectRole(editingRole.id, {
        name: cleanName,
        skillGroupId: Number(formSkillGroupId),
        description: formDescription.trim() || undefined,
      });
      showNotification("success", `Đã cập nhật vai trò "${updated.name}" (${updated.code}).`);
      setEditingRole(null);
      await loadRoles(includeInactive);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : "Có lỗi xảy ra khi cập nhật vai trò chuyên môn.";
      setFormError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Confirm Deactivate
  const handleConfirmDeactivate = async () => {
    if (!deactivatingRole) return;
    try {
      setIsSubmitting(true);
      await deactivateProjectRole(deactivatingRole.id);
      showNotification(
        "success",
        `Đã ngừng sử dụng vai trò "${deactivatingRole.name}". Vai trò đã được ẩn khỏi danh sách chọn mới.`
      );
      setDeactivatingRole(null);
      setUsageInfo(null);
      await loadRoles(includeInactive);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : "Có lỗi xảy ra khi ngừng sử dụng vai trò.";
      showNotification("error", msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle Activate
  const handleActivate = async (role: ProjectRoleResponse) => {
    try {
      setIsSubmitting(true);
      await activateProjectRole(role.id);
      showNotification("success", `Đã kích hoạt lại vai trò "${role.name}" (${role.code}).`);
      await loadRoles(includeInactive);
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : "Có lỗi xảy ra khi kích hoạt lại vai trò.";
      showNotification("error", msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Check RBAC permission for VT-06
  if (!isAdmin) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center p-8 bg-white rounded-3xl border border-slate-200 shadow-xs animate-in fade-in duration-150">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-rose-50 text-rose-600 mb-4 border border-rose-100">
          <ShieldAlert className="h-8 w-8" />
        </div>
        <h3 className="text-base font-bold text-slate-900 mb-1">
          Không có quyền truy cập
        </h3>
        <p className="text-xs text-slate-500 max-w-md mb-6 leading-relaxed">
          Chức năng Quản lý danh mục vai trò chuyên môn (NCL-12-CN-001) chỉ dành riêng cho Quản trị viên (VT-06).
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div className="flex items-start gap-4">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 border border-indigo-100">
            <Briefcase className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              Danh mục vai trò chuyên môn
              <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-100">
                NCL-12-CN-001
              </span>
            </h1>
            <p className="text-xs text-slate-500 mt-1 max-w-2xl leading-relaxed">
              Quản lý chuẩn hóa danh mục vai trò kỹ thuật và dự án gắn với nhóm kỹ năng tương ứng, phục vụ việc ước lượng nhu cầu nhân lực và phân bổ chung cho toàn tổ chức.
            </p>
          </div>
        </div>

        <button
          onClick={handleOpenCreate}
          type="button"
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-xs font-semibold text-white shadow-xs hover:bg-indigo-700 active:bg-indigo-800 transition focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
        >
          <Plus className="h-4 w-4" />
          Thêm vai trò chuyên môn
        </button>
      </div>

      {/* Notification Toast */}
      {notification && (
        <div
          className={`flex items-center gap-3 p-4 rounded-xl text-xs font-medium border transition-all ${
            notification.type === "success"
              ? "bg-emerald-50 text-emerald-800 border-emerald-200"
              : notification.type === "error"
              ? "bg-rose-50 text-rose-800 border-rose-200"
              : "bg-blue-50 text-blue-800 border-blue-200"
          }`}
        >
          {notification.type === "success" && <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />}
          {notification.type === "error" && <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0" />}
          {notification.type === "info" && <Info className="h-4 w-4 text-blue-600 shrink-0" />}
          <span className="flex-1">{notification.message}</span>
          <button
            onClick={() => setNotification(null)}
            type="button"
            className="p-1 hover:opacity-75"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
        <div className="flex flex-1 flex-col sm:flex-row items-center gap-3">
          {/* Search */}
          <div className="relative w-full sm:w-80">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo mã hoặc tên vai trò..."
              className="w-full pl-9 pr-4 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition"
            />
          </div>

          {/* Filter by Skill Group */}
          <div className="flex items-center gap-2 w-full sm:w-auto">
            <Filter className="h-4 w-4 text-slate-400 shrink-0" />
            <select
              value={selectedGroupFilter}
              onChange={(e) => setSelectedGroupFilter(e.target.value)}
              className="w-full sm:w-60 py-2 px-3 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition"
            >
              <option value="ALL">Tất cả nhóm kỹ năng</option>
              {skillGroups.map((g) => (
                <option key={g.id} value={String(g.id)}>
                  {g.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Include Inactive Toggle */}
        <label className="inline-flex items-center gap-2 cursor-pointer select-none self-end md:self-center">
          <input
            type="checkbox"
            checked={includeInactive}
            onChange={(e) => setIncludeInactive(e.target.checked)}
            className="sr-only peer"
          />
          <div className="w-9 h-5 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-600 relative"></div>
          <span className="text-xs font-medium text-slate-600">
            Hiển thị cả vai trò đã ngừng sử dụng
          </span>
        </label>
      </div>

      {/* Main Table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-xs overflow-hidden">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20 text-slate-400">
            <Loader2 className="h-8 w-8 animate-spin text-indigo-500 mb-2" />
            <p className="text-xs">Đang tải danh mục vai trò chuyên môn...</p>
          </div>
        ) : filteredRoles.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-center px-4">
            <div className="h-12 w-12 rounded-2xl bg-slate-100 flex items-center justify-center text-slate-400 mb-3">
              <Briefcase className="h-6 w-6" />
            </div>
            <h3 className="text-sm font-semibold text-slate-800 mb-1">
              Không tìm thấy vai trò chuyên môn nào
            </h3>
            <p className="text-xs text-slate-500 max-w-sm mb-4">
              Không có vai trò nào khớp với bộ lọc hiện tại hoặc danh mục đang trống.
            </p>
            <button
              onClick={handleOpenCreate}
              type="button"
              className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition"
            >
              <Plus className="h-3.5 w-3.5" /> Thêm vai trò mới
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/75 text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                  <th className="py-3.5 pl-6 pr-3 w-16 text-center">STT</th>
                  <th className="py-3.5 px-4 w-32">Mã vai trò</th>
                  <th className="py-3.5 px-4 min-w-[180px]">Tên vai trò chuyên môn</th>
                  <th className="py-3.5 px-4 min-w-[180px]">Nhóm kỹ năng tương ứng</th>
                  <th className="py-3.5 px-4">Mô tả</th>
                  <th className="py-3.5 px-4 w-36 text-center">Trạng thái</th>
                  <th className="py-3.5 pl-4 pr-6 w-44 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs">
                {filteredRoles.map((role, index) => {
                  const isInactive = role.status === "INACTIVE";

                  return (
                    <tr
                      key={role.id}
                      className={`hover:bg-slate-50/60 transition ${
                        isInactive ? "bg-slate-50/30 text-slate-400" : "text-slate-700"
                      }`}
                    >
                      <td className="py-4 pl-6 pr-3 text-center text-slate-400 font-mono">
                        {index + 1}
                      </td>

                      <td className="py-4 px-4 font-mono font-bold">
                        <span
                          className={`inline-block px-2.5 py-1 rounded-md text-[11px] font-semibold tracking-wide ${
                            isInactive
                              ? "bg-slate-100 text-slate-500 border border-slate-200"
                              : "bg-indigo-50 text-indigo-700 border border-indigo-100"
                          }`}
                        >
                          {role.code}
                        </span>
                      </td>

                      <td className="py-4 px-4 font-semibold text-slate-900">
                        <div className="flex items-center gap-2">
                          <span className={isInactive ? "line-through text-slate-400" : ""}>
                            {role.name}
                          </span>
                        </div>
                      </td>

                      <td className="py-4 px-4">
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
                          <Layers className="h-3 w-3 text-slate-500" />
                          {role.skillGroupName || "Chưa gán nhóm"}
                        </span>
                      </td>

                      <td className="py-4 px-4 text-slate-500 max-w-xs truncate">
                        {role.description || <span className="italic text-slate-300">Chưa có mô tả</span>}
                      </td>

                      <td className="py-4 px-4 text-center">
                        {isInactive ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-slate-100 text-slate-600 border border-slate-200">
                            Ngừng sử dụng
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <Check className="h-3 w-3" /> Đang dùng
                          </span>
                        )}
                      </td>

                      <td className="py-4 pl-4 pr-6 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* Sửa vai trò */}
                          <button
                            onClick={() => handleOpenEdit(role)}
                            type="button"
                            title="Sửa vai trò"
                            className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition"
                          >
                            <Edit2 className="h-3.5 w-3.5" />
                            <span>Sửa</span>
                          </button>

                          {/* Ngừng sử dụng / Kích hoạt lại */}
                          {isInactive ? (
                            <button
                              onClick={() => handleActivate(role)}
                              type="button"
                              title="Kích hoạt lại vai trò"
                              disabled={isSubmitting}
                              className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold text-emerald-700 hover:bg-emerald-50 border border-emerald-200 transition disabled:opacity-50"
                            >
                              <RotateCcw className="h-3.5 w-3.5" />
                              <span>Kích hoạt lại</span>
                            </button>
                          ) : (
                            <button
                              onClick={() => handleOpenDeactivate(role)}
                              type="button"
                              title="Ngừng sử dụng vai trò"
                              className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-semibold text-rose-600 hover:bg-rose-50 border border-rose-100 transition"
                            >
                              <PowerOff className="h-3.5 w-3.5" />
                              <span>Ngừng dùng</span>
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* MODAL: TẠO VAI TRÒ MỚI */}
      {isCreateOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl border border-slate-200 flex flex-col gap-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2.5">
                <div className="h-9 w-9 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                  <Briefcase className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Tạo vai trò chuyên môn mới</h3>
                  <p className="text-[11px] text-slate-500">Gắn vai trò vào nhóm kỹ năng tương ứng</p>
                </div>
              </div>
              <button
                onClick={() => setIsCreateOpen(false)}
                type="button"
                className="p-1 rounded-lg text-slate-400 hover:bg-slate-100 transition"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {formError && (
              <div className="flex items-start gap-2.5 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium">
                <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
                <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleSubmitCreate} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Mã vai trò chuyên môn <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={formCode}
                  onChange={(e) => setFormCode(e.target.value.toUpperCase())}
                  placeholder="Ví dụ: DATA_ENG, AI_SPEC, ARCH..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 font-mono uppercase"
                  required
                />
                <p className="text-[11px] text-slate-400 mt-1">
                  Mã vai trò phải là duy nhất, dùng để định danh chuẩn trong toàn hệ thống.
                </p>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Tên vai trò chuyên môn <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="Ví dụ: Kỹ sư Dữ liệu (Data Engineer)"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                  required
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Nhóm kỹ năng tương ứng <span className="text-rose-500">*</span>
                </label>
                <select
                  value={formSkillGroupId}
                  onChange={(e) => setFormSkillGroupId(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                  required
                >
                  <option value="" disabled>-- Chọn nhóm kỹ năng --</option>
                  {skillGroups.map((g) => (
                    <option key={g.id} value={g.id}>
                      {g.name}
                    </option>
                  ))}
                </select>
                <p className="text-[11px] text-slate-400 mt-1">
                  Mỗi vai trò chuyên môn bắt buộc phải gắn với một nhóm kỹ năng tương ứng.
                </p>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Mô tả vai trò</label>
                <textarea
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  rows={3}
                  placeholder="Mô tả chức trách chuyên môn hoặc phạm vi kỹ năng của vai trò..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 resize-none"
                />
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setIsCreateOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 transition disabled:opacity-50 shadow-xs"
                >
                  {isSubmitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                  Tạo vai trò
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: SỬA VAI TRÒ */}
      {editingRole && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl border border-slate-200 flex flex-col gap-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2.5">
                <div className="h-9 w-9 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                  <Edit2 className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Sửa vai trò chuyên môn</h3>
                  <p className="text-[11px] text-slate-500">Mã vai trò: <strong className="font-mono">{editingRole.code}</strong></p>
                </div>
              </div>
              <button
                onClick={() => setEditingRole(null)}
                type="button"
                className="p-1 rounded-lg text-slate-400 hover:bg-slate-100 transition"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            {formError && (
              <div className="flex items-start gap-2.5 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium">
                <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
                <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleSubmitEdit} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Mã vai trò chuyên môn</label>
                <input
                  type="text"
                  value={formCode}
                  disabled
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-100 text-slate-500 font-mono uppercase cursor-not-allowed"
                />
                <p className="text-[11px] text-slate-400 mt-1">
                  Mã vai trò không thể thay đổi để đảm bảo tính toàn vẹn của các ước lượng nhu cầu.
                </p>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Tên vai trò chuyên môn <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                  required
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Nhóm kỹ năng tương ứng <span className="text-rose-500">*</span>
                </label>
                <select
                  value={formSkillGroupId}
                  onChange={(e) => setFormSkillGroupId(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                  required
                >
                  <option value="" disabled>-- Chọn nhóm kỹ năng --</option>
                  {skillGroups.map((g) => (
                    <option key={g.id} value={g.id}>
                      {g.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Mô tả vai trò</label>
                <textarea
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  rows={3}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 resize-none"
                />
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setEditingRole(null)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 transition disabled:opacity-50 shadow-xs"
                >
                  {isSubmitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                  Lưu thay đổi
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: XÁC NHẬN NGỪNG SỬ DỤNG (DEACTIVATE) */}
      {deactivatingRole && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-xs p-4 animate-in fade-in duration-150">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl border border-slate-200 flex flex-col gap-4">
            <div className="flex items-start gap-3 pb-3 border-b border-slate-100">
              <div className="h-10 w-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-200">
                <AlertTriangle className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">
                  Xác nhận ngừng sử dụng vai trò
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Vai trò: <strong className="text-slate-800">{deactivatingRole.name}</strong> ({deactivatingRole.code})
                </p>
              </div>
            </div>

            {isCheckingUsage ? (
              <div className="flex flex-col items-center justify-center py-6 text-slate-500">
                <Loader2 className="h-6 w-6 animate-spin text-indigo-500 mb-2" />
                <p className="text-xs">Đang kiểm tra dữ liệu sử dụng vai trò trong các dự án...</p>
              </div>
            ) : usageInfo?.inUse ? (
              /* Cảnh báo nghiêm ngặt theo yêu cầu TC-02 */
              <div className="space-y-3">
                <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 text-xs leading-relaxed">
                  <p className="font-bold flex items-center gap-1.5 text-amber-800 mb-1.5">
                    <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600" />
                    CẢNH BÁO: Vai trò đang được sử dụng!
                  </p>
                  <p>
                    Vai trò này hiện đang được liên kết trong hệ thống:
                  </p>
                  <ul className="list-disc pl-5 mt-1 space-y-0.5 font-medium">
                    <li>Nhu cầu nhân lực dự án: <strong>{usageInfo.demandCount}</strong> bản ghi</li>
                    {usageInfo.employeeCount > 0 && (
                      <li>Hồ sơ nhân sự phụ trách: <strong>{usageInfo.employeeCount}</strong> nhân viên</li>
                    )}
                  </ul>
                  <div className="mt-2.5 pt-2 border-t border-amber-200/60 text-[11px] text-amber-700">
                    <strong>Quy định hệ thống:</strong> Vai trò này sẽ được chuyển sang trạng thái <strong>NGỪNG SỬ DỤNG</strong> và ẩn khỏi danh sách chọn mới. Hệ thống tuyệt đối <em>không xóa vật lý</em> dữ liệu để đảm bảo toàn vẹn ước lượng nhu cầu các dự án hiện hành.
                  </div>
                </div>
              </div>
            ) : (
              <p className="text-xs text-slate-600 leading-relaxed">
                Vai trò này hiện chưa được sử dụng trong dự án nào. Khi ngừng sử dụng, vai trò sẽ được chuyển sang trạng thái <strong>INACTIVE</strong> và ẩn khỏi danh sách chọn mới. Bạn có thể kích hoạt lại bất kỳ lúc nào.
              </p>
            )}

            <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
              <button
                type="button"
                onClick={() => {
                  setDeactivatingRole(null);
                  setUsageInfo(null);
                }}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmDeactivate}
                disabled={isSubmitting || isCheckingUsage}
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-rose-600 hover:bg-rose-700 transition disabled:opacity-50 shadow-xs"
              >
                {isSubmitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                Xác nhận ngừng sử dụng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
