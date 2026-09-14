import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import {
    ClipboardList,
    AlertTriangle,
    Clock,
    CheckCircle2,
    Search,
    RefreshCw,
    Filter,
    Layers,
    Calendar,
    ArrowRight,
    AlertCircle,
    TrendingUp,
    ShieldAlert,
    X,
    Download,
    ArrowUpDown,
} from 'lucide-react';
import {
    getProjectTaskTracking,
    type ProjectTaskTrackingResult,
    type TaskTrackingItem,
    type TaskTrackingFilterParams,
} from '@/lib/api/task-tracking';
import type { ProjectMember } from './projectData';

interface ProjectTaskTrackingViewProps {
    projectId: number;
    isProjectClosed?: boolean;
    members?: ProjectMember[];
    onNavigateToWbs?: () => void;
}

type SortOption = 'default' | 'deadline' | 'burned_desc' | 'name_asc';

export const ProjectTaskTrackingView: React.FC<ProjectTaskTrackingViewProps> = ({
    projectId,
    isProjectClosed = false,
    members = [],
    onNavigateToWbs,
}) => {
    const [data, setData] = useState<ProjectTaskTrackingResult | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    // Filter states
    const [keyword, setKeyword] = useState<string>('');
    const [debouncedKeyword, setDebouncedKeyword] = useState<string>('');
    const [employeeId, setEmployeeId] = useState<number | undefined>(undefined);
    const [statusFilter, setStatusFilter] = useState<string>('ALL');
    const [overdueOnly, setOverdueOnly] = useState<boolean>(false);
    const [groupByCategory, setGroupByCategory] = useState<boolean>(false);
    const [sortBy, setSortBy] = useState<SortOption>('default');

    // Ref to track ongoing request and abort when needed (Race Condition Prevention)
    const abortControllerRef = useRef<AbortController | null>(null);

    // Persistent cache of all known assignees so dropdown never shrinks when filtering
    const [cachedAssignees, setCachedAssignees] = useState<
        Array<{ id: number; name: string; code: string }>
    >([]);

    // Reset state when project changes to prevent stale data display
    useEffect(() => {
        setData(null);
        setKeyword('');
        setDebouncedKeyword('');
        setEmployeeId(undefined);
        setStatusFilter('ALL');
        setOverdueOnly(false);
        setSortBy('default');
    }, [projectId]);

    // Update cached assignees from passed members prop
    useEffect(() => {
        if (members && members.length > 0) {
            const list = members
                .map((m) => {
                    const numId = m.employeeId || Number(m.id.replace(/\D/g, ''));
                    return {
                        id: numId,
                        name: m.name,
                        code: m.id || String(numId),
                    };
                })
                .filter((m) => m.id > 0);

            setCachedAssignees((prev) => {
                const map = new Map<number, { id: number; name: string; code: string }>();
                prev.forEach((item) => map.set(item.id, item));
                list.forEach((item) => map.set(item.id, item));
                return Array.from(map.values());
            });
        }
    }, [members]);

    // Debounce keyword search (300ms)
    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedKeyword(keyword);
        }, 300);
        return () => clearTimeout(timer);
    }, [keyword]);

    // Load data from API with cancellation support
    const loadTrackingData = useCallback(async (isRefresh = false) => {
        if (!projectId) return;

        // Cancel previous pending request if any
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }
        const controller = new AbortController();
        abortControllerRef.current = controller;

        if (isRefresh) {
            setIsRefreshing(true);
        } else {
            setIsLoading(true);
        }
        setError(null);

        try {
            const params: TaskTrackingFilterParams = {
                employeeId,
                status: statusFilter !== 'ALL' ? statusFilter : undefined,
                overdueOnly: overdueOnly || undefined,
                keyword: debouncedKeyword.trim() || undefined,
            };

            const result = await getProjectTaskTracking(projectId, params, controller.signal);
            setData(result);

            // Populate cached assignees from tasks if not already populated
            if (result.tasks) {
                setCachedAssignees((prev) => {
                    const map = new Map<number, { id: number; name: string; code: string }>();
                    prev.forEach((item) => map.set(item.id, item));
                    result.tasks.forEach((t) => {
                        (t.assignees || []).forEach((a) => {
                            if (a.employeeId && !map.has(a.employeeId)) {
                                map.set(a.employeeId, {
                                    id: a.employeeId,
                                    name: a.fullName,
                                    code: a.employeeCode,
                                });
                            }
                        });
                    });
                    return Array.from(map.values());
                });
            }
        } catch (err: unknown) {
            if (err instanceof Error && err.name === 'AbortError') {
                return; // Silently ignore aborted requests
            }
            const errorMsg = err instanceof Error ? err.message : 'Không thể tải bảng theo dõi công việc.';
            setError(errorMsg);
        } finally {
            setIsLoading(false);
            setIsRefreshing(false);
        }
    }, [projectId, employeeId, statusFilter, overdueOnly, debouncedKeyword]);

    useEffect(() => {
        loadTrackingData();
        return () => {
            if (abortControllerRef.current) {
                abortControllerRef.current.abort();
            }
        };
    }, [loadTrackingData]);

    // Reset all filters
    const handleResetFilters = () => {
        setKeyword('');
        setDebouncedKeyword('');
        setEmployeeId(undefined);
        setStatusFilter('ALL');
        setOverdueOnly(false);
        setSortBy('default');
    };

    const hasActiveFilters = Boolean(
        keyword || employeeId || statusFilter !== 'ALL' || overdueOnly || sortBy !== 'default'
    );

    // Apply secondary sort on tasks (TC-02: Overdue tasks ALWAYS stay at top)
    const sortedTasks = useMemo(() => {
        if (!data?.tasks) return [];
        const tasks = [...data.tasks];

        if (sortBy === 'default') {
            return tasks; // Already sorted by backend (overdue first, then sortOrder)
        }

        return tasks.sort((a, b) => {
            // Priority 1: Overdue tasks always on top (TC-02)
            if (a.isOverdue && !b.isOverdue) return -1;
            if (!a.isOverdue && b.isOverdue) return 1;

            // Priority 2: Secondary sort criteria
            if (sortBy === 'deadline') {
                const dateA = a.plannedEndDate ? new Date(a.plannedEndDate).getTime() : Infinity;
                const dateB = b.plannedEndDate ? new Date(b.plannedEndDate).getTime() : Infinity;
                return dateA - dateB;
            }
            if (sortBy === 'burned_desc') {
                return (b.burnedPercentage || 0) - (a.burnedPercentage || 0);
            }
            if (sortBy === 'name_asc') {
                return a.name.localeCompare(b.name, 'vi');
            }
            return 0;
        });
    }, [data?.tasks, sortBy]);

    // Group tasks by category if enabled
    const groupedTasks = useMemo(() => {
        if (!sortedTasks) return [];
        if (!groupByCategory) {
            return [{ categoryName: 'Tất cả công việc', tasks: sortedTasks }];
        }

        const groups: Record<string, TaskTrackingItem[]> = {};
        sortedTasks.forEach((t) => {
            const cat = t.categoryName || 'Hạng mục chung';
            if (!groups[cat]) groups[cat] = [];
            groups[cat].push(t);
        });

        return Object.entries(groups).map(([categoryName, tasks]) => ({
            categoryName,
            tasks,
        }));
    }, [sortedTasks, groupByCategory]);

    // Format timeline display safely
    const formatTimeline = (start: string | null, end: string | null, isOverdue: boolean) => {
        const formatSingleDate = (d: string | null) => {
            if (!d) return '';
            const parts = d.split('-');
            return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : d;
        };

        if (!start && !end) {
            return <span className="italic text-slate-400 text-[11px]">Chưa đặt thời hạn</span>;
        }

        const startFormatted = formatSingleDate(start);
        const endFormatted = formatSingleDate(end);

        if (start && end) {
            return (
                <div className="flex items-center gap-1 text-[11px] text-slate-600">
                    <Calendar className="h-3 w-3 text-slate-400 shrink-0" />
                    <span>{startFormatted} - </span>
                    <span className={isOverdue ? 'font-bold text-rose-600' : 'text-slate-700 font-medium'}>
                        {endFormatted}
                    </span>
                </div>
            );
        }

        if (end) {
            return (
                <div className="flex items-center gap-1 text-[11px] text-slate-600">
                    <Calendar className="h-3 w-3 text-slate-400 shrink-0" />
                    <span>Hạn: </span>
                    <span className={isOverdue ? 'font-bold text-rose-600' : 'text-slate-700 font-medium'}>
                        {endFormatted}
                    </span>
                </div>
            );
        }

        return (
            <div className="flex items-center gap-1 text-[11px] text-slate-600">
                <Calendar className="h-3 w-3 text-slate-400 shrink-0" />
                <span>Bắt đầu: {startFormatted}</span>
            </div>
        );
    };

    // Status badge helper
    const renderStatusBadge = (status: string) => {
        switch (status) {
            case 'DONE':
                return (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 border border-emerald-200/60">
                        <CheckCircle2 className="h-3 w-3" />
                        Hoàn thành
                    </span>
                );
            case 'IN_PROGRESS':
                return (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-sky-50 px-2.5 py-0.5 text-xs font-semibold text-sky-700 border border-sky-200/60">
                        <Clock className="h-3 w-3 animate-spin" style={{ animationDuration: '6s' }} />
                        Đang làm
                    </span>
                );
            case 'CANCELLED':
                return (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-500 border border-slate-200">
                        <X className="h-3 w-3" />
                        Đã hủy
                    </span>
                );
            case 'TODO':
            default:
                return (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-medium text-amber-700 border border-amber-200/60">
                        <Clock className="h-3 w-3" />
                        Chờ thực hiện
                    </span>
                );
        }
    };

    // Budget burn badge & bar
    const renderBudgetBurn = (item: TaskTrackingItem) => {
        const burned = Number(item.burnedPercentage || 0);
        const actual = Number(item.actualHours || 0);
        const budget = Number(item.budgetHours || 0);
        const isOver = item.isOverBudget || item.budgetBurnStatus === 'OVER_BUDGET' || burned > 100;
        const isWarning = item.budgetBurnStatus === 'WARNING' || (burned >= 80 && burned <= 100);

        let barColor = 'bg-emerald-500';
        let badgeColor = 'text-emerald-700 bg-emerald-50 border-emerald-200';
        let label = 'An toàn';

        if (isOver) {
            barColor = 'bg-rose-500';
            badgeColor = 'text-rose-700 bg-rose-50 border-rose-200 font-bold';
            label = 'Vượt';
        } else if (isWarning) {
            barColor = 'bg-amber-500';
            badgeColor = 'text-amber-700 bg-amber-50 border-amber-200';
            label = 'Cảnh báo';
        }

        return (
            <div className="space-y-1 min-w-[120px]">
                <div className="flex items-center justify-between text-[11px]">
                    <span className="font-semibold text-slate-700">
                        {actual}h / {budget}h
                    </span>
                    <span className={`px-1.5 py-0.2 rounded text-[10px] border ${badgeColor}`}>
                        {burned.toFixed(1)}% ({label})
                    </span>
                </div>
                <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                    <div
                        className={`h-full rounded-full transition-all duration-300 ${barColor}`}
                        style={{ width: `${Math.min(burned, 100)}%` }}
                    />
                </div>
            </div>
        );
    };

    // Export tracking list to CSV (UTF-8 with BOM for Excel compatibility)
    const handleExportCsv = () => {
        if (!data?.tasks || data.tasks.length === 0) return;

        const headers = [
            'Mã công việc',
            'Tên công việc',
            'Hạng mục',
            'Người phụ trách',
            'Trạng thái',
            'Ngày bắt đầu',
            'Ngày kết thúc',
            'Quá hạn',
            'Số ngày trễ',
            'Ngân sách (giờ)',
            'Thực tế (giờ)',
            'Tiêu hao (%)',
            'Cảnh báo ngân sách',
        ];

        const rows = data.tasks.map((t) => [
            `"${t.taskCode}"`,
            `"${t.name.replace(/"/g, '""')}"`,
            `"${(t.categoryName || 'Chung').replace(/"/g, '""')}"`,
            `"${(t.assignees || []).map((a) => a.fullName).join('; ')}"`,
            `"${t.status}"`,
            `"${t.plannedStartDate || ''}"`,
            `"${t.plannedEndDate || ''}"`,
            `"${t.isOverdue ? 'CÓ' : 'KHÔNG'}"`,
            `"${t.isOverdue ? t.overdueDays : 0}"`,
            `"${t.budgetHours}"`,
            `"${t.actualHours}"`,
            `"${t.burnedPercentage}%"`,
            `"${t.budgetBurnStatus}"`,
        ]);

        const csvContent = '\uFEFF' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\r\n');
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.setAttribute('href', url);
        link.setAttribute(
            'download',
            `bang-theo-doi-cong-viec-${data.projectCode || projectId}-${new Date().toISOString().slice(0, 10)}.csv`
        );
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    return (
        <div className="space-y-5">
            {/* Banner QTN-04: Dự án đã đóng */}
            {(isProjectClosed || data?.projectStatus === 'CLOSED') && (
                <div className="rounded-xl border border-amber-200 bg-amber-50/90 p-4 shadow-xs">
                    <div className="flex items-start gap-3">
                        <ShieldAlert className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
                        <div>
                            <h4 className="text-sm font-bold text-amber-900">
                                Dự án đang ở trạng thái ĐÃ ĐÓNG (CLOSED)
                            </h4>
                            <p className="text-xs text-amber-700 mt-0.5">
                                Theo quy tắc QTN-04, bảng theo dõi công việc của dự án này đang ở chế độ xem lưu trữ lịch sử. Mọi chỉnh sửa công việc đã bị vô hiệu hóa.
                            </p>
                        </div>
                    </div>
                </div>
            )}

            {/* Error banner */}
            {error && (
                <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 shadow-xs">
                    <div className="flex items-start gap-3">
                        <AlertCircle className="h-5 w-5 text-rose-600 shrink-0 mt-0.5" />
                        <div className="flex-1">
                            <h4 className="text-sm font-bold text-rose-900">Không thể tải bảng theo dõi</h4>
                            <p className="text-xs text-rose-700 mt-0.5">{error}</p>
                        </div>
                        <button
                            type="button"
                            onClick={() => loadTrackingData(true)}
                            className="rounded-lg bg-white px-3 py-1 text-xs font-semibold text-rose-700 border border-rose-200 hover:bg-rose-50 cursor-pointer"
                        >
                            Thử lại
                        </button>
                    </div>
                </div>
            )}

            {/* KPI Cards Summary */}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3.5">
                {/* Tổng công việc */}
                <div className="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-xs transition-hover hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold text-slate-500">Tổng công việc</span>
                        <div className="rounded-xl bg-slate-100 p-2 text-slate-600">
                            <ClipboardList className="h-4 w-4" />
                        </div>
                    </div>
                    <div className="mt-2 flex items-baseline gap-2">
                        <span className="text-2xl font-black text-slate-800 tracking-tight">
                            {data?.totalTasks ?? 0}
                        </span>
                        <span className="text-[11px] font-medium text-slate-400">công việc</span>
                    </div>
                </div>

                {/* Quá hạn - Highlight Đỏ (TC-02) */}
                <div
                    className={`rounded-2xl border p-4 shadow-xs transition-hover ${
                        (data?.overdueTasks ?? 0) > 0
                            ? 'border-rose-300 bg-rose-50/40 hover:border-rose-400 ring-2 ring-rose-500/10'
                            : 'border-slate-200/80 bg-white hover:border-slate-300'
                    }`}
                >
                    <div className="flex items-center justify-between">
                        <span
                            className={`text-xs font-semibold ${
                                (data?.overdueTasks ?? 0) > 0 ? 'text-rose-700 font-bold' : 'text-slate-500'
                            }`}
                        >
                            Trễ hạn / Quá hạn
                        </span>
                        <div
                            className={`rounded-xl p-2 ${
                                (data?.overdueTasks ?? 0) > 0
                                    ? 'bg-rose-100 text-rose-700 animate-pulse'
                                    : 'bg-slate-100 text-slate-500'
                            }`}
                        >
                            <AlertTriangle className="h-4 w-4" />
                        </div>
                    </div>
                    <div className="mt-2 flex items-baseline gap-2">
                        <span
                            className={`text-2xl font-black tracking-tight ${
                                (data?.overdueTasks ?? 0) > 0 ? 'text-rose-600' : 'text-slate-800'
                            }`}
                        >
                            {data?.overdueTasks ?? 0}
                        </span>
                        <span
                            className={`text-[11px] font-medium ${
                                (data?.overdueTasks ?? 0) > 0 ? 'text-rose-500 font-bold' : 'text-slate-400'
                            }`}
                        >
                            cần xử lý gấp
                        </span>
                    </div>
                </div>

                {/* Đang làm */}
                <div className="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-xs transition-hover hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold text-slate-500">Đang thực hiện</span>
                        <div className="rounded-xl bg-sky-100 p-2 text-sky-600">
                            <Clock className="h-4 w-4" />
                        </div>
                    </div>
                    <div className="mt-2 flex items-baseline gap-2">
                        <span className="text-2xl font-black text-sky-700 tracking-tight">
                            {data?.inProgressTasks ?? 0}
                        </span>
                        <span className="text-[11px] font-medium text-slate-400">việc</span>
                    </div>
                </div>

                {/* Hoàn thành */}
                <div className="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-xs transition-hover hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold text-slate-500">Đã hoàn thành</span>
                        <div className="rounded-xl bg-emerald-100 p-2 text-emerald-600">
                            <CheckCircle2 className="h-4 w-4" />
                        </div>
                    </div>
                    <div className="mt-2 flex items-baseline gap-2">
                        <span className="text-2xl font-black text-emerald-700 tracking-tight">
                            {data?.completedTasks ?? 0}
                        </span>
                        <span className="text-[11px] font-medium text-emerald-600 font-semibold">
                            {data?.totalTasks ? Math.round((data.completedTasks / data.totalTasks) * 100) : 0}%
                        </span>
                    </div>
                </div>

                {/* Giờ công Ngân sách & Thực tế */}
                <div className="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-xs transition-hover hover:border-slate-300 col-span-2 sm:col-span-1">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold text-slate-500">Tiêu hao giờ công</span>
                        <div className="rounded-xl bg-indigo-100 p-2 text-indigo-600">
                            <TrendingUp className="h-4 w-4" />
                        </div>
                    </div>
                    <div className="mt-2 flex items-baseline gap-1.5">
                        <span className="text-xl font-black text-slate-800 tracking-tight">
                            {data?.totalActualHours ?? 0}h
                        </span>
                        <span className="text-xs font-medium text-slate-400">
                            / {data?.totalBudgetHours ?? 0}h
                        </span>
                    </div>
                    <div className="mt-1.5 h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                        <div
                            className={`h-full rounded-full transition-all duration-300 ${
                                (data?.totalActualHours || 0) > (data?.totalBudgetHours || 0)
                                    ? 'bg-rose-500'
                                    : 'bg-indigo-600'
                            }`}
                            style={{
                                width: `${
                                    data?.totalBudgetHours && data.totalBudgetHours > 0
                                        ? Math.min(
                                              Math.round(
                                                  ((data.totalActualHours || 0) / data.totalBudgetHours) * 100
                                              ),
                                              100
                                          )
                                        : 0
                                }%`,
                            }}
                        />
                    </div>
                </div>
            </div>

            {/* Filter & Toolbar */}
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200/80 bg-white p-3 shadow-xs">
                <div className="flex flex-wrap items-center gap-2.5 flex-1 min-w-[280px]">
                    {/* Search Input */}
                    <div className="relative flex-1 sm:w-60 max-w-sm">
                        <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                        <input
                            type="text"
                            value={keyword}
                            onChange={(e) => setKeyword(e.target.value)}
                            placeholder="Tìm theo mã việc, tên việc..."
                            className="w-full rounded-xl border border-slate-200 bg-slate-50 py-1.5 pl-8 pr-8 text-xs text-slate-800 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20"
                        />
                        {keyword && (
                            <button
                                type="button"
                                onClick={() => setKeyword('')}
                                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                                aria-label="Xóa từ khóa"
                            >
                                <X className="h-3.5 w-3.5" />
                            </button>
                        )}
                    </div>

                    {/* Filter Assignee (Fix: Uses cachedAssignees so dropdown never shrinks) */}
                    <select
                        value={employeeId !== undefined ? String(employeeId) : ''}
                        onChange={(e) => setEmployeeId(e.target.value ? Number(e.target.value) : undefined)}
                        className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs text-slate-700 outline-none focus:border-indigo-500 focus:bg-white cursor-pointer"
                    >
                        <option value="">Tất cả người phụ trách ({cachedAssignees.length})</option>
                        {cachedAssignees.map((a) => (
                            <option key={a.id} value={a.id}>
                                {a.name} ({a.code})
                            </option>
                        ))}
                    </select>

                    {/* Filter Status */}
                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs text-slate-700 outline-none focus:border-indigo-500 focus:bg-white cursor-pointer"
                    >
                        <option value="ALL">Tất cả trạng thái</option>
                        <option value="TODO">Chờ thực hiện (TODO)</option>
                        <option value="IN_PROGRESS">Đang thực hiện (IN_PROGRESS)</option>
                        <option value="DONE">Hoàn thành (DONE)</option>
                        <option value="CANCELLED">Đã hủy (CANCELLED)</option>
                    </select>

                    {/* Secondary Sort Selector */}
                    <div className="flex items-center gap-1.5">
                        <ArrowUpDown className="h-3.5 w-3.5 text-slate-400" />
                        <select
                            value={sortBy}
                            onChange={(e) => setSortBy(e.target.value as SortOption)}
                            className="rounded-xl border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-xs text-slate-700 outline-none focus:border-indigo-500 focus:bg-white cursor-pointer"
                        >
                            <option value="default">Sắp xếp: Trễ hạn lên đầu (Mặc định)</option>
                            <option value="deadline">Hạn chót gần nhất</option>
                            <option value="burned_desc">% Tiêu hao ngân sách cao nhất</option>
                            <option value="name_asc">Tên công việc (A - Z)</option>
                        </select>
                    </div>

                    {/* Overdue Only Filter */}
                    <button
                        type="button"
                        onClick={() => setOverdueOnly(!overdueOnly)}
                        className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold transition-colors cursor-pointer border ${
                            overdueOnly
                                ? 'bg-rose-50 border-rose-300 text-rose-700 shadow-xs'
                                : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'
                        }`}
                    >
                        <AlertTriangle className={`h-3.5 w-3.5 ${overdueOnly ? 'text-rose-600' : 'text-slate-400'}`} />
                        <span>Chỉ việc trễ hạn</span>
                    </button>

                    {/* Clear Filters */}
                    {hasActiveFilters && (
                        <button
                            type="button"
                            onClick={handleResetFilters}
                            className="inline-flex items-center gap-1 text-xs text-indigo-600 hover:text-indigo-800 font-medium px-2 py-1 cursor-pointer"
                        >
                            <X className="h-3 w-3" />
                            Xóa lọc
                        </button>
                    )}
                </div>

                <div className="flex items-center gap-2 shrink-0">
                    {/* Toggle Group by Category */}
                    <button
                        type="button"
                        onClick={() => setGroupByCategory(!groupByCategory)}
                        className={`inline-flex items-center gap-1.5 rounded-xl border px-3 py-1.5 text-xs font-semibold cursor-pointer transition-all ${
                            groupByCategory
                                ? 'bg-indigo-50 border-indigo-200 text-indigo-700'
                                : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                        }`}
                        title="Gom nhóm các công việc theo hạng mục WBS"
                    >
                        <Layers className="h-3.5 w-3.5" />
                        <span className="hidden sm:inline">Gom nhóm hạng mục</span>
                    </button>

                    {/* Export CSV Button */}
                    <button
                        type="button"
                        onClick={handleExportCsv}
                        disabled={!data?.tasks || data.tasks.length === 0}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-xs hover:bg-slate-50 hover:text-slate-900 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
                        title="Xuất danh sách công việc ra file CSV (Excel)"
                    >
                        <Download className="h-3.5 w-3.5 text-slate-500" />
                        <span className="hidden sm:inline">Xuất CSV</span>
                    </button>

                    {/* Refresh Button */}
                    <button
                        type="button"
                        onClick={() => loadTrackingData(true)}
                        disabled={isLoading || isRefreshing}
                        className="inline-flex items-center justify-center rounded-xl border border-slate-200 bg-white p-2 text-slate-600 shadow-xs hover:bg-slate-50 hover:text-slate-900 cursor-pointer disabled:opacity-50"
                        title="Tải lại dữ liệu"
                        aria-label="Tải lại dữ liệu"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${isRefreshing ? 'animate-spin text-indigo-600' : ''}`} />
                    </button>
                </div>
            </div>

            {/* Content Area */}
            {isLoading ? (
                <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center text-slate-500 shadow-xs">
                    <RefreshCw className="mx-auto h-8 w-8 animate-spin text-indigo-600 mb-3" />
                    <h3 className="text-sm font-bold text-slate-700">Đang tải bảng theo dõi công việc...</h3>
                    <p className="text-xs text-slate-400 mt-1">
                        Hệ thống đang nạp thông tin tiến độ, ngân sách và trạng thái các công việc.
                    </p>
                </div>
            ) : !data || data.totalTasks === 0 ? (
                /* Empty State (TC-03) */
                <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center text-slate-500 shadow-xs">
                    <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 mb-4">
                        <Layers className="h-7 w-7" />
                    </div>
                    <h3 className="text-base font-bold text-slate-800">Dự án chưa có công việc nào</h3>
                    <p className="text-xs text-slate-500 max-w-md mx-auto mt-1.5 leading-relaxed">
                        {data?.suggestionMessage ||
                            'Dự án này chưa được thiết lập danh sách công việc. Vui lòng tạo cấu trúc phân rã công việc (WBS) để bắt đầu theo dõi tiến độ và ngân sách.'}
                    </p>
                    {onNavigateToWbs && (
                        <div className="mt-5">
                            <button
                                type="button"
                                onClick={onNavigateToWbs}
                                className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-xs font-semibold text-white shadow-sm hover:bg-indigo-700 transition-colors cursor-pointer"
                            >
                                <span>Chuyển sang Cấu trúc WBS</span>
                                <ArrowRight className="h-3.5 w-3.5" />
                            </button>
                        </div>
                    )}
                </div>
            ) : sortedTasks.length === 0 ? (
                /* Filter result empty */
                <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-slate-500 shadow-xs">
                    <Filter className="mx-auto h-8 w-8 text-slate-300 mb-2" />
                    <h4 className="text-sm font-bold text-slate-700">Không tìm thấy công việc phù hợp</h4>
                    <p className="text-xs text-slate-400 mt-1">
                        Không có công việc nào khớp với các tiêu chí tìm kiếm và bộ lọc hiện tại.
                    </p>
                    <button
                        type="button"
                        onClick={handleResetFilters}
                        className="mt-3.5 inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 cursor-pointer"
                    >
                        <X className="h-3.5 w-3.5 text-slate-400" />
                        Xóa tất cả bộ lọc
                    </button>
                </div>
            ) : (
                /* Task Table (TC-01 & TC-02: Overdue items at top) */
                <div className="space-y-6">
                    {groupedTasks.map((group, groupIdx) => (
                        <div
                            key={groupIdx}
                            className="rounded-2xl border border-slate-200/80 bg-white shadow-xs overflow-hidden"
                        >
                            {groupByCategory && (
                                <div className="border-b border-slate-100 bg-slate-50/80 px-4 py-2.5 flex items-center justify-between">
                                    <div className="flex items-center gap-2">
                                        <Layers className="h-4 w-4 text-indigo-600" />
                                        <span className="text-xs font-bold text-slate-800">
                                            {group.categoryName}
                                        </span>
                                    </div>
                                    <span className="rounded-full bg-slate-200/70 px-2 py-0.5 text-[10px] font-bold text-slate-600">
                                        {group.tasks.length} việc
                                    </span>
                                </div>
                            )}

                            <div className="overflow-x-auto">
                                <table className="w-full text-left text-xs text-slate-600">
                                    <thead className="border-b border-slate-200/80 bg-slate-50/50 text-[11px] font-bold uppercase tracking-wider text-slate-400">
                                        <tr>
                                            <th className="py-3 px-4">Công việc</th>
                                            {!groupByCategory && <th className="py-3 px-3">Hạng mục</th>}
                                            <th className="py-3 px-3">Người phụ trách</th>
                                            <th className="py-3 px-3">Thời hạn kế hoạch</th>
                                            <th className="py-3 px-3">Trạng thái</th>
                                            <th className="py-3 px-3">Tiêu hao ngân sách</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                        {group.tasks.map((task) => {
                                            const isTaskOverdue = task.isOverdue;
                                            return (
                                                <tr
                                                    key={task.taskId}
                                                    className={`transition-colors ${
                                                        isTaskOverdue
                                                            ? 'bg-rose-50/30 hover:bg-rose-50/60'
                                                            : 'hover:bg-slate-50/70'
                                                    }`}
                                                >
                                                    {/* Công việc & Mã & Cảnh báo quá hạn (TC-02) */}
                                                    <td className="py-3 px-4">
                                                        <div className="flex flex-col gap-1">
                                                            <div className="flex items-center gap-2 flex-wrap">
                                                                <span className="inline-flex items-center rounded-md bg-slate-100 px-1.5 py-0.5 font-mono text-[10px] font-bold text-slate-700 border border-slate-200">
                                                                    {task.taskCode}
                                                                </span>
                                                                <span className="font-semibold text-slate-800 hover:text-indigo-600 transition-colors">
                                                                    {task.name}
                                                                </span>

                                                                {/* Overdue Badge */}
                                                                {isTaskOverdue && (
                                                                    <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2 py-0.5 text-[10px] font-bold text-rose-700 border border-rose-200/70 animate-pulse">
                                                                        <AlertTriangle className="h-3 w-3 text-rose-600" />
                                                                        Trễ {task.overdueDays} ngày
                                                                    </span>
                                                                )}
                                                            </div>
                                                        </div>
                                                    </td>

                                                    {/* Hạng mục (nếu chưa gom nhóm) */}
                                                    {!groupByCategory && (
                                                        <td className="py-3 px-3 text-slate-600 font-medium">
                                                            <div className="inline-flex items-center gap-1.5 rounded-lg bg-slate-50 px-2 py-1 border border-slate-150 text-[11px]">
                                                                <Layers className="h-3 w-3 text-slate-400" />
                                                                <span className="truncate max-w-[150px]" title={task.categoryName}>
                                                                    {task.categoryName || 'Chung'}
                                                                </span>
                                                            </div>
                                                        </td>
                                                    )}

                                                    {/* Người phụ trách */}
                                                    <td className="py-3 px-3">
                                                        {task.assignees && task.assignees.length > 0 ? (
                                                            <div className="flex flex-col gap-1">
                                                                {task.assignees.map((assignee) => (
                                                                    <div
                                                                        key={assignee.employeeId}
                                                                        className="flex items-center gap-1.5"
                                                                    >
                                                                        <div className="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-100 text-[9px] font-bold text-indigo-700">
                                                                            {assignee.fullName.charAt(0)}
                                                                        </div>
                                                                        <span
                                                                            className="text-xs text-slate-700 font-medium truncate max-w-[140px]"
                                                                            title={assignee.fullName}
                                                                        >
                                                                            {assignee.fullName}
                                                                        </span>
                                                                    </div>
                                                                ))}
                                                            </div>
                                                        ) : (
                                                            <span className="text-[11px] italic text-slate-400">
                                                                Chưa giao
                                                            </span>
                                                        )}
                                                    </td>

                                                    {/* Thời hạn kế hoạch */}
                                                    <td className="py-3 px-3">
                                                        {formatTimeline(
                                                            task.plannedStartDate,
                                                            task.plannedEndDate,
                                                            isTaskOverdue
                                                        )}
                                                    </td>

                                                    {/* Trạng thái */}
                                                    <td className="py-3 px-3">
                                                        {renderStatusBadge(task.status)}
                                                    </td>

                                                    {/* Tiêu hao ngân sách & cảnh báo */}
                                                    <td className="py-3 px-3">
                                                        {renderBudgetBurn(task)}
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ProjectTaskTrackingView;
