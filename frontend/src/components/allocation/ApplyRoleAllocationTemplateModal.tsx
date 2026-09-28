import { useState, useEffect } from "react";
import {
  X,
  Sparkles,
  AlertTriangle,
  CheckCircle2,
  Loader2,
  UserCheck,
  UserX,
  AlertCircle,
} from "lucide-react";
import {
  previewRoleAllocationSuggestion,
  confirmApplyRoleAllocationTemplate,
  type PreviewRoleAllocationResult,
  type RoleSuggestionItem,
} from "@/lib/api/role-allocation-templates";
import { getProjects, type ProjectResult } from "@/lib/api/projects";

interface ApplyRoleAllocationTemplateModalProps {
  open: boolean;
  templateId: number;
  targetProjectId?: number;
  onClose: () => void;
  onSuccess: () => void;
}

export function ApplyRoleAllocationTemplateModal({
  open,
  templateId,
  targetProjectId: initialTargetProjectId,
  onClose,
  onSuccess,
}: ApplyRoleAllocationTemplateModalProps) {
  const [targetProjectId, setTargetProjectId] = useState<number | "">(
    initialTargetProjectId || ""
  );
  const [projects, setProjects] = useState<ProjectResult[]>([]);
  const [preview, setPreview] = useState<PreviewRoleAllocationResult | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [applying, setApplying] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const loadProjects = async () => {
    try {
      const res = await getProjects(0, 100);
      // Only active projects with start and end dates
      const active = (res.content || []).filter(
        (p: ProjectResult) => p.status === "ACTIVE" && p.startDate && p.endDate
      );
      setProjects(active);
    } catch (err) {
      console.error("Failed to load projects", err);
    }
  };

  const handleLoadPreview = async (projectId: number) => {
    setTargetProjectId(projectId);
    setLoading(true);
    setError(null);
    try {
      const data = await previewRoleAllocationSuggestion(templateId, projectId);
      setPreview(data);
    } catch (err: any) {
      setError(err?.message || "Không thể phân tích gợi ý phân bổ cho dự án này");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      loadProjects();
      if (targetProjectId) {
        handleLoadPreview(Number(targetProjectId));
      }
    } else {
      setPreview(null);
      setError(null);
      setSuccessMsg(null);
    }
  }, [open, templateId]);

  const handleConfirmApply = async () => {
    if (!preview || !targetProjectId) return;

    setApplying(true);
    setError(null);
    try {
      const assignments = preview.suggestions.map((s) => ({
        roleId: s.roleId,
        employeeId: s.assigned ? s.suggestedEmployeeId : null,
        hoursPerWeek: s.hoursPerWeek,
      }));

      const res = await confirmApplyRoleAllocationTemplate(
        templateId,
        Number(targetProjectId),
        { assignments }
      );

      setSuccessMsg(res.message || "Áp mẫu phân bổ thành công!");
      setTimeout(() => {
        onSuccess();
      }, 1200);
    } catch (err: any) {
      setError(err?.message || "Lỗi khi áp dụng mẫu phân bổ vào dự án");
    } finally {
      setApplying(false);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-xs">
      <div className="flex w-full max-w-3xl flex-col max-h-[90vh] rounded-3xl bg-white shadow-2xl overflow-hidden border border-slate-100 animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 shadow-2xs">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-800">
                Áp dụng mẫu phân bổ & Gợi ý nhân sự
              </h3>
              <p className="text-xs text-slate-500">
                Hệ thống tự động phân tích độ rảnh và gợi ý nhân sự phù hợp cho từng vai trò
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

          {/* Select Target Project */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">
              Dự án mục tiêu muốn áp dụng mẫu <span className="text-rose-500">*</span>
            </label>
            <select
              value={targetProjectId}
              onChange={(e) => handleLoadPreview(Number(e.target.value))}
              className="w-full rounded-xl border border-slate-200 bg-white p-2.5 text-xs text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
            >
              <option value="">-- Chọn dự án mục tiêu --</option>
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.projectCode} - {p.projectName} ({p.startDate} ~ {p.endDate})
                </option>
              ))}
            </select>
          </div>

          {loading && (
            <div className="py-12 text-center text-xs text-slate-400">
              <Loader2 className="h-6 w-6 animate-spin mx-auto mb-2 text-indigo-500" />
              Đang phân tích lịch rảnh và tìm kiếm nhân sự phù hợp...
            </div>
          )}

          {preview && !loading && (
            <div className="space-y-4">
              {/* Project & Template summary banner */}
              <div className="flex items-center justify-between rounded-2xl bg-indigo-50/60 p-4 border border-indigo-100">
                <div>
                  <span className="text-[11px] font-semibold text-indigo-600 uppercase tracking-wider">
                    Mẫu: {preview.templateName} ({preview.templateCode})
                  </span>
                  <h4 className="text-sm font-bold text-slate-900 mt-0.5">
                    Dự án đích: {preview.targetProjectName}
                  </h4>
                </div>
                <div className="text-right font-mono text-xs font-bold text-indigo-700">
                  {preview.targetTotalWeeks} tuần kế hoạch
                </div>
              </div>

              {/* Warning Alert if any role has no candidate */}
              {preview.hasUnassignedRoles && (
                <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 shadow-2xs flex items-start gap-3 animate-in fade-in">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-700 mt-0.5">
                    <AlertTriangle className="h-4 w-4" />
                  </span>
                  <div>
                    <h4 className="text-xs font-bold text-amber-900">
                      Cảnh báo thiếu hụt nhân sự rảnh
                    </h4>
                    <ul className="mt-1 space-y-0.5 text-xs text-amber-800 list-disc list-inside font-medium">
                      {preview.warnings.map((w, idx) => (
                        <li key={idx}>{w}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              )}

              {/* Table of Roles and Suggestions */}
              <div className="rounded-2xl border border-slate-200 overflow-hidden bg-white shadow-2xs">
                <div className="bg-slate-50 px-4 py-2.5 border-b border-slate-100 grid grid-cols-12 text-xs font-bold text-slate-700">
                  <div className="col-span-4">Vai trò trong mẫu</div>
                  <div className="col-span-3 text-center">Yêu cầu / tuần</div>
                  <div className="col-span-5">Nhân sự được gợi ý</div>
                </div>

                <div className="divide-y divide-slate-100">
                  {preview.suggestions.map((item: RoleSuggestionItem) => (
                    <div
                      key={item.roleId}
                      className={`px-4 py-3.5 grid grid-cols-12 items-center text-xs ${
                        !item.assigned ? "bg-amber-50/30" : ""
                      }`}
                    >
                      {/* Role column */}
                      <div className="col-span-4">
                        <div className="font-bold text-slate-800">{item.roleName}</div>
                        <span className="font-mono text-[11px] text-slate-400">
                          {item.roleCode}
                        </span>
                      </div>

                      {/* Hours column */}
                      <div className="col-span-3 text-center font-mono font-bold text-slate-700">
                        {item.hoursPerWeek}h / tuần
                      </div>

                      {/* Candidate suggestion column */}
                      <div className="col-span-5">
                        {item.assigned ? (
                          <div className="flex items-center gap-2">
                            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-bold text-emerald-800">
                              <UserCheck className="h-3 w-3" />
                              Phù hợp
                            </span>
                            <div>
                              <span className="font-bold text-slate-800">
                                {item.suggestedEmployeeName}
                              </span>
                              <span className="font-mono text-[11px] text-slate-400 ml-1.5">
                                ({item.suggestedEmployeeCode})
                              </span>
                            </div>
                          </div>
                        ) : (
                          <div className="flex flex-col gap-1">
                            <span className="inline-flex items-center gap-1 w-fit rounded-full bg-amber-100 px-2 py-0.5 text-[11px] font-bold text-amber-800">
                              <UserX className="h-3 w-3" />
                              Để trống (Chưa có người rảnh)
                            </span>
                            <span className="text-[11px] text-amber-700 font-medium">
                              {item.warningMessage}
                            </span>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-slate-100 px-6 py-4 bg-slate-50/50">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-100 transition"
          >
            Đóng
          </button>
          <button
            type="button"
            onClick={handleConfirmApply}
            disabled={applying || !preview || !targetProjectId}
            className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-5 py-2 text-xs font-bold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 transition disabled:opacity-50 cursor-pointer"
          >
            {applying ? (
              <>
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                <span>Đang áp dụng...</span>
              </>
            ) : (
              <>
                <CheckCircle2 className="h-3.5 w-3.5" />
                <span>Xác nhận áp dụng phân bổ</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
