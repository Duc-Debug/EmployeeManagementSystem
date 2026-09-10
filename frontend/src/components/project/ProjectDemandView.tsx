import React, { useState, useMemo } from 'react';
import {
    Users,
    Plus,
    RefreshCw,
    Search,
    AlertTriangle,
    CheckCircle2,
    Calendar,
    ChevronDown,
    ChevronUp,
    Edit3,
    Trash2,
    Clock,
    Target,
    BarChart2,
    ShieldAlert,
} from 'lucide-react';
import type { ProjectResult } from '@/lib/api/projects';
import type {
    ProjectResourceDemandSummaryResult,
    RoleResourceDemand,
} from '@/lib/api/resource-demands';

interface ProjectDemandViewProps {
    project: ProjectResult | null;
    canManage: boolean;
    demandSummary: ProjectResourceDemandSummaryResult | null;
    isLoading: boolean;
    error: string | null;
    onReload: () => void;
    onOpenCreateModal: () => void;
    onOpenEditModal: (role: RoleResourceDemand) => void;
    onOpenDeleteModal: (role: RoleResourceDemand) => void;
}

export function ProjectDemandView({
    project,
    canManage,
    demandSummary,
    isLoading,
    error,
    onReload,
    onOpenCreateModal,
    onOpenEditModal,
    onOpenDeleteModal,
}: ProjectDemandViewProps) {
    const [searchTerm, setSearchTerm] = useState<string>('');
    const [expandedRoleIds, setExpandedRoleIds] = useState<Set<number>>(new Set());

    // Toggle expand xem chi tiết từng tuần của vai trò
    const toggleExpandRole = (roleId: number) => {
        setExpandedRoleIds((prev) => {
            const next = new Set(prev);
            if (next.has(roleId)) {
                next.delete(roleId);
            } else {
                next.add(roleId);
            }
            return next;
        });
    };

    const hasDates = Boolean(project?.startDate && project?.endDate);
    const isProjectActive = project?.status === 'ACTIVE';

    // Lọc danh sách vai trò theo từ khóa tìm kiếm
    const filteredRoles = useMemo(() => {
        if (!demandSummary?.demandsByRole) return [];
        if (!searchTerm.trim()) return demandSummary.demandsByRole;
        const q = searchTerm.toLowerCase().trim();
        return demandSummary.demandsByRole.filter(
            (r) =>
                r.roleName.toLowerCase().includes(q) ||
                r.roleCode.toLowerCase().includes(q)
        );
    }, [demandSummary, searchTerm]);

    const totalEstimated = demandSummary?.projectEstimatedHours
        ? Number(demandSummary.projectEstimatedHours)
        : (project?.estimatedHours || 0);

    const totalDemand = demandSummary?.totalDemandHours
        ? Number(demandSummary.totalDemandHours)
        : 0;

    const coveragePercentage = totalEstimated > 0
        ? Math.round((totalDemand / totalEstimated) * 100)
        : 0;

    const exceedsBudget = demandSummary?.exceedsEstimatedHours || (totalEstimated > 0 && totalDemand > totalEstimated);

    return (
        <div className="space-y-6">
            {/* Top Project Notice / Missing Dates / Status Warning */}
            {!hasDates && (
                <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 shadow-2xs">
                    <div className="flex items-start gap-3">
                        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-700">
                            <AlertTriangle className="h-4 w-4" />
                        </span>
                        <div>
                            <h4 className="text-xs font-bold text-amber-900">
                                Dự án chưa thiết lập ngày bắt đầu và kết thúc
                            </h4>
                            <p className="mt-0.5 text-xs text-amber-700 leading-relaxed">
                                Để ước lượng nhu cầu nhân sự theo từng tuần, dự án cần có ngày bắt đầu và ngày kết thúc hợp lệ. Vui lòng cập nhật thông tin dự án trước khi tiến hành ước lượng.
                            </p>
                        </div>
                    </div>
                </div>
            )}

            {!isProjectActive && project && (
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 shadow-2xs">
                    <div className="flex items-center gap-2.5 text-xs text-slate-600">
                        <ShieldAlert className="h-4 w-4 text-slate-500" />
                        <span>
                            Dự án đang ở trạng thái <strong>{project.status === 'CLOSED' ? 'Đã đóng' : 'Vô hiệu hóa'}</strong>. Hệ thống chỉ cho phép xem lịch sử ước lượng nhu cầu nhân sự.
                        </span>
                    </div>
                </div>
            )}

            {/* Error Banner */}
            {error && (
                <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 shadow-2xs flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2.5 text-xs text-rose-700">
                        <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0" />
                        <span>{error}</span>
                    </div>
                    <button
                        type="button"
                        onClick={onReload}
                        className="rounded-xl border border-rose-200 bg-white px-3 py-1.5 text-xs font-bold text-rose-700 hover:bg-rose-100 transition cursor-pointer"
                    >
                        Thử lại
                    </button>
                </div>
            )}

            {/* Warning Message from Backend (TC-02: Vượt quy mô dự án) */}
            {demandSummary?.warningMessage && (
                <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 shadow-2xs flex items-start gap-3 animate-in fade-in">
                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-700 mt-0.5">
                        <AlertTriangle className="h-4 w-4" />
                    </span>
                    <div>
                        <h4 className="text-xs font-bold text-amber-900">
                            Cảnh báo vượt quy mô dự án
                        </h4>
                        <p className="mt-0.5 text-xs text-amber-800 leading-relaxed font-medium">
                            {demandSummary.warningMessage}
                        </p>
                    </div>
                </div>
            )}

            {/* KPI Summary Metric Cards */}
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                {/* 1. Quy mô dự án */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                            Quy mô dự án đã duyệt
                        </span>
                        <span className="rounded-lg bg-indigo-50 p-2 text-indigo-600">
                            <Target className="h-4 w-4" />
                        </span>
                    </div>
                    <div className="mt-2 flex items-baseline gap-1.5">
                        <span className="text-2xl font-bold text-slate-900 font-mono">
                            {totalEstimated > 0 ? totalEstimated : '--'}
                        </span>
                        <span className="text-xs text-slate-500">giờ</span>
                    </div>
                    <p className="mt-1 text-[11px] text-slate-400">
                        Hạn mức tối đa được duyệt cho dự án
                    </p>
                </div>

                {/* 2. Tổng nhu cầu nhân sự */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                            Tổng nhu cầu nhân sự
                        </span>
                        <span className="rounded-lg bg-emerald-50 p-2 text-emerald-600">
                            <Clock className="h-4 w-4" />
                        </span>
                    </div>
                    <div className="mt-2 flex items-baseline gap-1.5">
                        <span className={`text-2xl font-bold font-mono ${exceedsBudget ? 'text-amber-600' : 'text-slate-900'}`}>
                            {totalDemand}
                        </span>
                        <span className="text-xs text-slate-500">giờ ({demandSummary?.demandsByRole.length || 0} vai trò)</span>
                    </div>
                    <p className="mt-1 text-[11px] text-slate-400">
                        Tổng hợp giờ theo tuần từ các vai trò
                    </p>
                </div>

                {/* 3. Tỷ lệ so với Quy mô */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                            Tỷ lệ chiếm dụng quy mô
                        </span>
                        <span className="rounded-lg bg-indigo-50 p-2 text-indigo-600">
                            <BarChart2 className="h-4 w-4" />
                        </span>
                    </div>
                    <div className="mt-2 flex items-baseline gap-1.5">
                        <span className={`text-2xl font-bold font-mono ${exceedsBudget ? 'text-amber-600' : 'text-indigo-600'}`}>
                            {coveragePercentage}%
                        </span>
                        <span className="text-xs text-slate-500">
                            {totalEstimated > 0 ? `của ${totalEstimated}h` : ''}
                        </span>
                    </div>
                    <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
                        <div
                            className={`h-1.5 rounded-full transition-all duration-500 ${
                                exceedsBudget ? 'bg-amber-500' : 'bg-indigo-600'
                            }`}
                            style={{ width: `${Math.min(coveragePercentage, 100)}%` }}
                        />
                    </div>
                </div>

                {/* 4. Trạng thái Quy mô */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                            Trạng thái ngân sách
                        </span>
                        <span className={`rounded-lg p-2 ${exceedsBudget ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600'}`}>
                            {exceedsBudget ? <AlertTriangle className="h-4 w-4" /> : <CheckCircle2 className="h-4 w-4" />}
                        </span>
                    </div>
                    <div className="mt-2 flex items-center gap-1.5">
                        {exceedsBudget ? (
                            <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-bold text-amber-800">
                                Vượt quy mô
                            </span>
                        ) : (
                            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-bold text-emerald-800">
                                Trong hạn mức
                            </span>
                        )}
                    </div>
                    <p className={`mt-2 text-[11px] ${exceedsBudget ? 'text-amber-600 font-medium' : 'text-slate-400'}`}>
                        {exceedsBudget
                            ? `Vượt ${(totalDemand - totalEstimated).toFixed(1)}h so với quy mô`
                            : `Còn lại ${(totalEstimated - totalDemand).toFixed(1)}h chưa phân bổ`}
                    </p>
                </div>
            </div>

            {/* Filter & Action Toolbar */}
            <div className="flex flex-col items-start justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-2xs sm:flex-row sm:items-center">
                {/* Search */}
                <div className="relative w-full sm:w-72">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                    <input
                        type="text"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        placeholder="Tìm theo tên vai trò, mã VT..."
                        className="w-full rounded-xl border border-slate-200 bg-slate-50 py-1.5 pl-8 pr-3 text-xs text-slate-800 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20"
                    />
                </div>

                {/* Actions */}
                <div className="flex items-center gap-2 self-end sm:self-auto">
                    {canManage && isProjectActive && hasDates && (
                        <button
                            type="button"
                            onClick={onOpenCreateModal}
                            className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3.5 py-2 text-xs font-bold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 transition active:scale-95 cursor-pointer"
                        >
                            <Plus className="h-3.5 w-3.5 stroke-[2.5]" />
                            <span>+ Ước lượng vai trò</span>
                        </button>
                    )}

                    <button
                        type="button"
                        onClick={onReload}
                        className="p-2 rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-100 transition cursor-pointer"
                        title="Làm mới bảng nhu cầu"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
                    </button>
                </div>
            </div>

            {/* Main Content: Table or Empty/Loading State */}
            {isLoading ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-2xs space-y-3">
                    <RefreshCw className="h-8 w-8 animate-spin text-indigo-600 mx-auto" />
                    <p className="text-xs font-semibold text-slate-600">Đang tải bảng ước lượng nhu cầu nhân sự...</p>
                </div>
            ) : !demandSummary || demandSummary.demandsByRole.length === 0 ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-2xs space-y-4">
                    <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600">
                        <Users className="h-7 w-7" />
                    </div>
                    <div>
                        <h3 className="text-sm font-bold text-slate-800">
                            Chưa có ước lượng nhu cầu nhân sự
                        </h3>
                        <p className="mt-1 text-xs text-slate-500 max-w-md mx-auto leading-relaxed">
                            Dự án này chưa được thiết lập nhu cầu giờ theo vai trò. Ước lượng nhu cầu giúp bạn dự báo ngân sách giờ và điều phối nhân sự hiệu quả.
                        </p>
                    </div>
                    {canManage && isProjectActive && hasDates && (
                        <button
                            type="button"
                            onClick={onOpenCreateModal}
                            className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 transition active:scale-95 cursor-pointer"
                        >
                            <Plus className="h-4 w-4" />
                            <span>Thêm ước lượng đầu tiên</span>
                        </button>
                    )}
                </div>
            ) : (
                <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xs">
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs text-slate-700">
                            <thead className="border-b border-slate-200 bg-slate-50/80 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                                <tr>
                                    <th className="py-3 pl-4 pr-2 w-10"></th>
                                    <th className="py-3 px-3">Mã VT</th>
                                    <th className="py-3 px-3">Tên vai trò chuyên môn</th>
                                    <th className="py-3 px-3 text-right">Nhu cầu / tuần</th>
                                    <th className="py-3 px-3 text-center">Số tuần áp dụng</th>
                                    <th className="py-3 px-3 text-right">Tổng giờ nhu cầu</th>
                                    <th className="py-3 px-3 text-right">Tỷ trọng</th>
                                    <th className="py-3 px-3 text-center">Trạng thái</th>
                                    {canManage && <th className="py-3 pr-4 pl-3 text-right">Thao tác</th>}
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                                {filteredRoles.map((roleDemand) => {
                                    const isExpanded = expandedRoleIds.has(roleDemand.roleId);
                                    const roleHours = Number(roleDemand.totalRoleHours) || 0;
                                    const weeksCount = roleDemand.weeklyDemands.length;
                                    const avgPerWeek = weeksCount > 0
                                        ? (roleDemand.weeklyDemands[0]?.requiredHours || (roleHours / weeksCount))
                                        : 0;
                                    const sharePercentage = totalDemand > 0
                                        ? Math.round((roleHours / totalDemand) * 100)
                                        : 0;

                                    return (
                                        <React.Fragment key={roleDemand.roleId}>
                                            <tr className="hover:bg-slate-50/80 transition-colors group">
                                                {/* Expand Button */}
                                                <td className="py-3 pl-4 pr-2">
                                                    <button
                                                        type="button"
                                                        onClick={() => toggleExpandRole(roleDemand.roleId)}
                                                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-700 transition cursor-pointer"
                                                        title={isExpanded ? 'Thu gọn phân rã tuần' : 'Xem chi tiết từng tuần'}
                                                    >
                                                        {isExpanded ? (
                                                            <ChevronUp className="h-4 w-4 text-indigo-600" />
                                                        ) : (
                                                            <ChevronDown className="h-4 w-4" />
                                                        )}
                                                    </button>
                                                </td>

                                                {/* Role Code */}
                                                <td className="py-3 px-3 font-mono font-bold">
                                                    <span className="inline-flex items-center rounded-lg bg-indigo-50 px-2 py-0.5 text-xs text-indigo-700 border border-indigo-200/60">
                                                        {roleDemand.roleCode}
                                                    </span>
                                                </td>

                                                {/* Role Name */}
                                                <td className="py-3 px-3 font-semibold text-slate-900">
                                                    {roleDemand.roleName}
                                                </td>

                                                {/* Hours per week */}
                                                <td className="py-3 px-3 text-right font-mono font-bold text-slate-800">
                                                    {Number(avgPerWeek).toFixed(1)}h
                                                    <span className="text-[10px] text-slate-400 font-normal">/tuần</span>
                                                </td>

                                                {/* Weeks count */}
                                                <td className="py-3 px-3 text-center">
                                                    <span className="inline-flex items-center gap-1 rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-600">
                                                        <Calendar className="h-3 w-3 text-slate-400" />
                                                        {weeksCount} tuần
                                                    </span>
                                                </td>

                                                {/* Total role hours */}
                                                <td className="py-3 px-3 text-right font-mono text-sm font-bold text-indigo-600">
                                                    {roleHours.toFixed(1)}h
                                                </td>

                                                {/* Share of total */}
                                                <td className="py-3 px-3 text-right text-xs">
                                                    <span className="font-mono text-slate-600">{sharePercentage}%</span>
                                                    <div className="mt-1 h-1 w-16 ml-auto overflow-hidden rounded-full bg-slate-100">
                                                        <div
                                                            className="h-1 rounded-full bg-indigo-500"
                                                            style={{ width: `${Math.min(sharePercentage, 100)}%` }}
                                                        />
                                                    </div>
                                                </td>

                                                {/* Status */}
                                                <td className="py-3 px-3 text-center">
                                                    <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-bold text-emerald-700 border border-emerald-200">
                                                        <CheckCircle2 className="h-3 w-3" />
                                                        Đã ước lượng
                                                    </span>
                                                </td>

                                                {/* Action Buttons */}
                                                {canManage && (
                                                    <td className="py-3 pr-4 pl-3 text-right">
                                                        <div className="inline-flex items-center gap-1">
                                                            {isProjectActive && (
                                                                <>
                                                                    <button
                                                                        type="button"
                                                                        onClick={() => onOpenEditModal(roleDemand)}
                                                                        className="rounded-lg p-1.5 text-slate-500 hover:bg-indigo-50 hover:text-indigo-600 transition cursor-pointer"
                                                                        title="Chỉnh sửa số giờ/tuần"
                                                                    >
                                                                        <Edit3 className="h-3.5 w-3.5" />
                                                                    </button>
                                                                    <button
                                                                        type="button"
                                                                        onClick={() => onOpenDeleteModal(roleDemand)}
                                                                        className="rounded-lg p-1.5 text-slate-500 hover:bg-rose-50 hover:text-rose-600 transition cursor-pointer"
                                                                        title="Xóa ước lượng vai trò này"
                                                                    >
                                                                        <Trash2 className="h-3.5 w-3.5" />
                                                                    </button>
                                                                </>
                                                            )}
                                                        </div>
                                                    </td>
                                                )}
                                            </tr>

                                            {/* Subrow: Chi tiết phân rã theo tuần */}
                                            {isExpanded && (
                                                <tr className="bg-slate-50/50">
                                                    <td colSpan={canManage ? 9 : 8} className="p-4 pl-12">
                                                        <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-2xs space-y-3">
                                                            <div className="flex items-center justify-between">
                                                                <h5 className="text-xs font-bold text-slate-800 flex items-center gap-2">
                                                                    <Calendar className="h-4 w-4 text-indigo-600" />
                                                                    Phân rã nhu cầu từng tuần: {roleDemand.roleName} ({roleDemand.roleCode})
                                                                </h5>
                                                                <span className="text-[11px] text-slate-400">
                                                                    {weeksCount} tuần theo lịch ISO-8601
                                                                </span>
                                                            </div>

                                                            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
                                                                {roleDemand.weeklyDemands.map((w) => (
                                                                    <div
                                                                        key={`${w.year}-${w.weekNumber}`}
                                                                        className="rounded-lg border border-slate-200 bg-slate-50/80 p-2.5 text-xs hover:bg-indigo-50/30 hover:border-indigo-200 transition"
                                                                    >
                                                                        <div className="flex items-center justify-between font-bold text-slate-800">
                                                                            <span className="text-indigo-700">T{w.weekNumber}</span>
                                                                            <span className="font-mono text-slate-900">{w.requiredHours}h</span>
                                                                        </div>
                                                                        <div className="mt-1 text-[10px] text-slate-500">
                                                                            {w.startDate} → {w.endDate}
                                                                        </div>
                                                                        <div className="mt-1.5 h-1 w-full overflow-hidden rounded-full bg-slate-200">
                                                                            <div
                                                                                className="h-1 rounded-full bg-indigo-600"
                                                                                style={{
                                                                                    width: `${Math.min(
                                                                                        (Number(w.requiredHours) / 40) * 100,
                                                                                        100
                                                                                    )}%`,
                                                                                }}
                                                                            />
                                                                        </div>
                                                                    </div>
                                                                ))}
                                                            </div>
                                                        </div>
                                                    </td>
                                                </tr>
                                            )}
                                        </React.Fragment>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
}
