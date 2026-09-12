import { useState, useEffect, useMemo, type FormEvent } from 'react';
import { X, Users, AlertTriangle, CheckCircle2, Calendar, Loader2 } from 'lucide-react';
import { getProjectRoles, type ProjectRoleResponse } from '@/lib/api/project-roles';
import type { RoleResourceDemand } from '@/lib/api/resource-demands';

interface EstimateDemandModalProps {
    open: boolean;
    projectId: number;
    projectCode?: string;
    projectName?: string;
    projectStartDate?: string;
    projectEndDate?: string;
    projectEstimatedHours?: number;
    currentTotalDemandHours?: number;
    editingRole?: RoleResourceDemand | null;
    existingRoleDemands: RoleResourceDemand[];
    onClose: () => void;
    onSave: (roleId: number, hoursPerWeek: number) => Promise<void>;
}

export function EstimateDemandModal({
    open,
    projectId,
    projectCode,
    projectName,
    projectStartDate,
    projectEndDate,
    projectEstimatedHours = 0,
    currentTotalDemandHours = 0,
    editingRole,
    existingRoleDemands,
    onClose,
    onSave,
}: EstimateDemandModalProps) {
    const [roles, setRoles] = useState<ProjectRoleResponse[]>([]);
    const [selectedRoleId, setSelectedRoleId] = useState<number | ''>('');
    const [hoursPerWeek, setHoursPerWeek] = useState<string>('40');
    const [isLoadingRoles, setIsLoadingRoles] = useState<boolean>(false);
    const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // 1. Tải danh mục vai trò chuyên môn dự án (DEV, TEST, BA, UIUX, PM, DEVOPS)
    useEffect(() => {
        if (!open) return;
        setIsLoadingRoles(true);
        getProjectRoles(Boolean(editingRole))
            .then((data) => {
                if (Array.isArray(data)) {
                    setRoles(data);
                }
            })
            .catch((err) => {
                console.error('Failed to load project roles from backend:', err);
                setErrorMessage('Không thể tải danh sách vai trò chuyên môn từ hệ thống.');
            })
            .finally(() => {
                setIsLoadingRoles(false);
            });
    }, [open]);

    // 2. Điền dữ liệu khi ở chế độ chỉnh sửa hoặc reset khi mở mới
    useEffect(() => {
        if (open) {
            setErrorMessage(null);
            if (editingRole) {
                setSelectedRoleId(editingRole.roleId);
                const avgHours = editingRole.weeklyDemands.length > 0
                    ? editingRole.weeklyDemands[0].requiredHours
                    : Number(editingRole.totalRoleHours) || 40;
                setHoursPerWeek(String(avgHours));
            } else {
                setSelectedRoleId('');
                setHoursPerWeek('40');
            }
        }
    }, [open, editingRole]);

    // 3. Tính số tuần ISO của dự án từ startDate đến endDate
    const totalProjectWeeks = useMemo(() => {
        if (!projectStartDate || !projectEndDate) return 0;
        const start = new Date(projectStartDate);
        const end = new Date(projectEndDate);
        if (isNaN(start.getTime()) || isNaN(end.getTime()) || end < start) return 0;

        const dayOfWeek = (start.getDay() + 6) % 7;
        const firstMonday = new Date(start);
        firstMonday.setDate(start.getDate() - dayOfWeek);

        let count = 0;
        const cur = new Date(firstMonday);
        while (cur <= end) {
            count++;
            cur.setDate(cur.getDate() + 7);
        }
        return count > 0 ? count : 1;
    }, [projectStartDate, projectEndDate]);

    if (!open) return null;

    const numHours = parseFloat(hoursPerWeek) || 0;
    const estimatedRoleTotal = Math.round(numHours * totalProjectWeeks * 100) / 100;

    // Dự toán tổng nhu cầu mới sau khi lưu
    const previousRoleHours = editingRole ? Number(editingRole.totalRoleHours) || 0 : 0;
    const simulatedTotalDemand = Math.max(0, currentTotalDemandHours - previousRoleHours + estimatedRoleTotal);
    const willExceedBudget = projectEstimatedHours > 0 && simulatedTotalDemand > projectEstimatedHours;

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setErrorMessage(null);

        if (!selectedRoleId) {
            setErrorMessage('Vui lòng chọn vai trò chuyên môn.');
            return;
        }

        if (isNaN(numHours) || numHours <= 0) {
            setErrorMessage('Số giờ nhu cầu mỗi tuần phải lớn hơn 0.');
            return;
        }

        if (numHours > 168) {
            setErrorMessage('Số giờ nhu cầu mỗi tuần không được vượt quá 168 giờ.');
            return;
        }

        try {
            setIsSubmitting(true);
            await onSave(Number(selectedRoleId), numHours);
            onClose();
        } catch (err: any) {
            console.error('Lỗi khi ước lượng nhu cầu nhân sự:', err);
            setErrorMessage(err?.message || 'Có lỗi xảy ra khi lưu ước lượng.');
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleQuickHourSelect = (val: number) => {
        setHoursPerWeek(String(val));
    };

    const selectedRoleObj = roles.find((r) => r.id === selectedRoleId);

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
            <div className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50/80 p-4">
                    <div className="flex items-center gap-2.5">
                        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-100 text-indigo-700 shadow-xs">
                            <Users className="h-5 w-5" />
                        </span>
                        <div>
                            <h3 className="text-sm font-bold text-slate-800">
                                {editingRole ? 'Chỉnh Sửa Ước Lượng Nhu Cầu' : 'Ước Lượng Nhu Cầu Nhân Sự Mới'}
                            </h3>
                            <p className="text-[11px] text-slate-500">
                                Xác định số giờ làm việc cần thiết cho vai trò theo từng tuần
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isSubmitting}
                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition disabled:opacity-50"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                {/* Project Context Preview */}
                <div className="border-b border-slate-100 bg-indigo-50/40 p-4">
                    <div className="flex items-center justify-between gap-3 text-xs">
                        <div>
                            <span className="text-[11px] font-medium text-slate-500">Dự án:</span>
                            <div className="font-bold text-indigo-950">
                                {projectName || `Dự án #${projectId}`} ({projectCode || 'Mã DA'})
                            </div>
                        </div>
                        <div className="text-right">
                            <span className="text-[11px] font-medium text-slate-500">Thời gian & Số tuần:</span>
                            <div className="flex items-center justify-end gap-1 font-semibold text-slate-700">
                                <Calendar className="h-3.5 w-3.5 text-indigo-500" />
                                <span>{totalProjectWeeks} tuần</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Form Body */}
                <form onSubmit={handleSubmit} className="p-5 space-y-4">
                    {errorMessage && (
                        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700 flex items-start gap-2">
                            <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
                            <span>{errorMessage}</span>
                        </div>
                    )}

                    {/* Role Selection */}
                    <div>
                        <label className="block text-xs font-bold text-slate-700 mb-1.5">
                            Vai trò chuyên môn <span className="text-rose-500">*</span>
                        </label>
                        {isLoadingRoles ? (
                            <div className="flex items-center gap-2 text-xs text-slate-500 py-2">
                                <Loader2 className="h-4 w-4 animate-spin text-indigo-600" />
                                <span>Đang tải danh sách vai trò từ máy chủ...</span>
                            </div>
                        ) : (
                            <select
                                value={selectedRoleId}
                                onChange={(e) => setSelectedRoleId(e.target.value ? Number(e.target.value) : '')}
                                disabled={Boolean(editingRole) || isSubmitting}
                                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 disabled:bg-slate-100 disabled:cursor-not-allowed"
                            >
                                <option value="">-- Chọn vai trò chuyên môn --</option>
                                {roles.map((r) => {
                                    const alreadyEstimated = !editingRole && existingRoleDemands.some((d) => d.roleId === r.id);
                                    return (
                                        <option key={r.id} value={r.id} disabled={alreadyEstimated}>
                                            {r.code} — {r.name} {alreadyEstimated ? '(Đã ước lượng)' : ''}{r.status === 'INACTIVE' ? ' (Đã ngừng sử dụng)' : ''}
                                        </option>
                                    );
                                })}
                            </select>
                        )}
                        {selectedRoleObj?.description && (
                            <p className="mt-1 text-[11px] text-slate-500 italic">
                                {selectedRoleObj.description}
                            </p>
                        )}
                    </div>

                    {/* Hours Per Week Input */}
                    <div>
                        <div className="flex items-center justify-between mb-1.5">
                            <label className="text-xs font-bold text-slate-700">
                                Số giờ nhu cầu mỗi tuần <span className="text-rose-500">*</span>
                            </label>
                            <span className="text-[11px] text-slate-500">
                                Tối đa 168h/tuần
                            </span>
                        </div>

                        <div className="relative">
                            <input
                                type="number"
                                step="any"
                                min="0.01"
                                max="168"
                                value={hoursPerWeek}
                                onChange={(e) => setHoursPerWeek(e.target.value)}
                                disabled={isSubmitting}
                                placeholder="Ví dụ: 40.0"
                                className="w-full rounded-xl border border-slate-300 bg-white py-2 pl-3 pr-10 text-xs text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            />
                            <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-slate-400">
                                h/tuần
                            </span>
                        </div>

                        {/* Quick Presets */}
                        <div className="mt-2 flex items-center gap-1.5">
                            <span className="text-[11px] text-slate-400">Chọn nhanh:</span>
                            {[10, 20, 30, 40, 80].map((val) => (
                                <button
                                    key={val}
                                    type="button"
                                    onClick={() => handleQuickHourSelect(val)}
                                    className="rounded-lg border border-slate-200 bg-slate-50 px-2 py-0.5 text-[11px] font-semibold text-slate-600 hover:bg-indigo-50 hover:text-indigo-700 hover:border-indigo-200 transition"
                                >
                                    {val}h
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Live Calculation Preview Card */}
                    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 space-y-2 text-xs">
                        <div className="flex items-center justify-between text-slate-600">
                            <span>Ước tính cho vai trò này:</span>
                            <span className="font-bold text-slate-900">
                                {totalProjectWeeks} tuần × {numHours}h = <span className="text-indigo-600 font-mono text-sm">{estimatedRoleTotal}h</span>
                            </span>
                        </div>

                        <div className="flex items-center justify-between text-slate-600 border-t border-slate-200/80 pt-2">
                            <span>Dự báo tổng nhu cầu dự án:</span>
                            <span className="font-bold text-slate-900 font-mono">
                                {simulatedTotalDemand}h / {projectEstimatedHours > 0 ? `${projectEstimatedHours}h` : 'Chưa đặt quy mô'}
                            </span>
                        </div>

                        {projectEstimatedHours > 0 && (
                            <div className="pt-1">
                                {willExceedBudget ? (
                                    <div className="flex items-center gap-1.5 text-[11px] font-semibold text-amber-700 bg-amber-50 p-2 rounded-lg border border-amber-200">
                                        <AlertTriangle className="h-3.5 w-3.5 text-amber-600 shrink-0" />
                                        <span>Cảnh báo: Tổng nhu cầu sẽ vượt quá quy mô dự án ({simulatedTotalDemand}h &gt; {projectEstimatedHours}h).</span>
                                    </div>
                                ) : (
                                    <div className="flex items-center gap-1.5 text-[11px] font-semibold text-emerald-700 bg-emerald-50 p-2 rounded-lg border border-emerald-200">
                                        <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600 shrink-0" />
                                        <span>An toàn: Nằm trong giới hạn quy mô dự án đã duyệt.</span>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    {/* Action Buttons */}
                    <div className="flex items-center justify-end gap-2.5 pt-2">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={isSubmitting}
                            className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition disabled:opacity-50 cursor-pointer"
                        >
                            Hủy bỏ
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting || isLoadingRoles}
                            className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 transition active:scale-95 disabled:opacity-50 cursor-pointer"
                        >
                            {isSubmitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                            <span>{editingRole ? 'Cập nhật ước lượng' : 'Lưu ước lượng'}</span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
