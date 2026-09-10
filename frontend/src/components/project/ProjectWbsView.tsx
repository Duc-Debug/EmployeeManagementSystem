import { useState } from 'react';
import {
    ListOrdered,
    Plus,
    ChevronDown,
    CircleCheck,
    Circle,
    Clock,
    CheckCircle2,
    FolderOpen,
    Target,
    AlertTriangle,
    Copy,
    GitCommit,
    Lock,
} from 'lucide-react';
import type { TaskCategoryGroup, ProjectMember, TaskItem } from './projectData';
import type { TaskDependencyResult } from '@/lib/api/taskDependencies';

interface ProjectWbsViewProps {
    categories: TaskCategoryGroup[];
    members: ProjectMember[];
    dependencies?: TaskDependencyResult[];
    searchTerm: string;
    selectedRole: string;
    isClosed?: boolean;
    onQuickAddTask: (catId: string) => void;
    onToggleTaskStatus: (catId: string, taskId: string) => void;
    onOpenBudgetModal?: (task: TaskItem) => void;
    onOpenCloneModal?: () => void;
}

export function ProjectWbsView({
    categories,
    members,
    dependencies = [],
    searchTerm,
    selectedRole,
    isClosed = false,
    onQuickAddTask,
    onToggleTaskStatus,
    onOpenBudgetModal,
    onOpenCloneModal,
}: ProjectWbsViewProps) {
    // Accordion state: map of category id -> isOpen boolean
    const [openCategories, setOpenCategories] = useState<Record<string, boolean>>({
        'cat-1': true,
        'cat-2': true,
        'cat-3': true,
        'cat-4': true,
    });

    const toggleCategory = (catId: string) => {
        setOpenCategories((prev) => ({
            ...prev,
            [catId]: !prev[catId],
        }));
    };

    let totalTasksCount = 0;
    let totalHoursEstimate = 0;
    let totalDoneTasks = 0;
    let totalBudgetHours = 0;
    let totalActualHours = 0;
    let overBudgetCount = 0;

    const filteredCategories = categories.map((cat) => {
        const filteredTasks = cat.tasks.filter((t) => {
            const assignee = members.find((m) => m.id === t.assigneeId);
            const q = searchTerm.trim().toLowerCase();
            const matchSearch =
                !q ||
                t.name.toLowerCase().includes(q) ||
                t.code.toLowerCase().includes(q) ||
                (assignee && assignee.name.toLowerCase().includes(q)) ||
                cat.name.toLowerCase().includes(q);
            const matchRole = selectedRole === 'ALL' || (assignee && assignee.role === selectedRole);
            return matchSearch && matchRole;
        });

        totalTasksCount += filteredTasks.length;
        const catHours = cat.tasks.reduce((sum, t) => sum + t.hours, 0);
        totalHoursEstimate += catHours;
        totalDoneTasks += cat.tasks.filter((t) => t.status === 'Hoàn thành').length;

        cat.tasks.forEach((t) => {
            const budget = t.budgetHours || 0;
            const actual = t.actualHours || 0;
            totalBudgetHours += budget;
            totalActualHours += actual;
            if (budget > 0 && actual > budget) {
                overBudgetCount += 1;
            }
        });

        return {
            ...cat,
            filteredTasks,
        };
    });

    const getPriorityBadge = (priority: string) => {
        switch (priority) {
            case 'Cao':
                return <span className="rounded border border-rose-100 bg-rose-50 px-1.5 py-0.5 text-[10px] font-semibold text-rose-600">Cao</span>;
            case 'Trung bình':
                return <span className="rounded border border-amber-100 bg-amber-50 px-1.5 py-0.5 text-[10px] font-semibold text-amber-700">TB</span>;
            default:
                return <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold text-slate-600">Thấp</span>;
        }
    };

    const getStatusBadge = (status: string) => {
        switch (status) {
            case 'Hoàn thành':
                return <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-[10px] font-medium text-emerald-700">Xong</span>;
            case 'Đang làm':
                return (
                    <span className="flex items-center gap-1 rounded-full bg-indigo-50 px-2 py-0.5 text-[10px] font-medium text-indigo-700">
                        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-indigo-600" /> Làm
                    </span>
                );
            case 'Chờ duyệt':
                return <span className="rounded-full bg-purple-50 px-2 py-0.5 text-[10px] font-medium text-purple-700">Duyệt</span>;
            default:
                return <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-500">Chờ</span>;
        }
    };

    return (
        <section className="flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs transition-all">
            {/* Header */}
            <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50/70 p-4">
                <div className="flex items-center gap-2.5">
                    <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-100 text-xs font-bold text-indigo-700">
                        <ListOrdered className="h-4 w-4" />
                    </span>
                    <div>
                        <h2 className="text-sm font-bold text-slate-900">Cấu Trúc Hạng Mục & Công Việc (WBS)</h2>
                        <p className="text-[11px] text-slate-500">Phân rã giai đoạn thành các task thực thi cụ thể</p>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    {isClosed ? (
                        <span className="inline-flex items-center gap-1 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-1 text-xs font-semibold text-rose-700">
                            <Lock className="h-3 w-3" /> Chỉ đọc (Đã đóng)
                        </span>
                    ) : (
                        <>
                            {onOpenCloneModal && (
                                <button
                                    type="button"
                                    onClick={onOpenCloneModal}
                                    className="flex items-center gap-1 text-xs font-semibold text-indigo-700 bg-indigo-50 border border-indigo-200 hover:bg-indigo-100 px-2.5 py-1 rounded-lg transition cursor-pointer"
                                    title="Nhân bản cây WBS từ dự án mẫu"
                                >
                                    <Copy className="h-3.5 w-3.5 text-indigo-600" />
                                    <span>Nhân bản WBS</span>
                                </button>
                            )}
                            <button
                                type="button"
                                onClick={() => onQuickAddTask(categories[0]?.id || '')}
                                className="flex items-center gap-1 text-xs font-medium text-indigo-600 hover:text-indigo-800 hover:underline cursor-pointer"
                            >
                                <Plus className="h-3.5 w-3.5" /> Thêm việc
                            </button>
                        </>
                    )}
                </div>

            </div>

            {/* Tree Content Container */}
            <div className="max-h-[640px] overflow-y-auto p-3 space-y-3">
                {totalTasksCount === 0 && !searchTerm ? (
                    <div className="p-8 text-center text-xs text-slate-500 border border-dashed border-slate-200 rounded-xl bg-slate-50/50">
                        <FolderOpen className="mx-auto mb-2 h-8 w-8 text-slate-300" />
                        <p className="font-semibold text-slate-700">Dự án chưa có cây công việc WBS</p>
                        <p className="text-[11px] text-slate-400 mt-0.5 mb-3">Bạn có thể tạo việc mới hoặc sao chép nhanh cấu trúc từ một dự án cũ tương tự</p>
                        {!isClosed && onOpenCloneModal && (
                            <button
                                type="button"
                                onClick={onOpenCloneModal}
                                className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-bold text-white shadow-xs hover:bg-indigo-700 transition cursor-pointer"
                            >
                                <Copy className="h-3.5 w-3.5" /> Nhân bản WBS từ dự án mẫu
                            </button>
                        )}
                    </div>
                ) : totalTasksCount === 0 && searchTerm ? (
                    <div className="p-8 text-center text-xs text-slate-400">
                        <FolderOpen className="mx-auto mb-2 h-8 w-8 text-slate-300" />
                        Không tìm thấy công việc nào phù hợp với bộ lọc.
                    </div>
                ) : (
                    filteredCategories.map((cat) => {
                        if (cat.filteredTasks.length === 0 && searchTerm) return null;
                        const isOpen = openCategories[cat.id] ?? true;

                        return (
                            <div
                                key={cat.id}
                                className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xs transition hover:border-slate-300"
                            >
                                {/* Category Bar */}
                                <div
                                    onClick={() => toggleCategory(cat.id)}
                                    className="flex cursor-pointer select-none items-center justify-between border-b border-slate-200 bg-slate-50/80 p-3 transition"
                                >
                                    <div className="flex items-center gap-2.5 min-w-0">
                                        <span
                                            className={`text-slate-400 transition-transform duration-200 ${
                                                isOpen ? '' : '-rotate-90'
                                            }`}
                                        >
                                            <ChevronDown className="h-4 w-4" />
                                        </span>
                                        <span className="rounded bg-indigo-100 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-indigo-700 shrink-0">
                                            {cat.code}
                                        </span>
                                        <span className="truncate text-xs font-bold text-slate-800 hover:text-indigo-600 transition">
                                            {cat.name}
                                        </span>
                                        <span className="hidden text-[11px] font-normal text-slate-400 sm:inline shrink-0">
                                            ({cat.filteredTasks.length} task)
                                        </span>
                                    </div>

                                    <div className="flex items-center gap-3 text-xs shrink-0">
                                        <div className="hidden items-center gap-1.5 text-[11px] text-slate-500 sm:flex">
                                            <span>{cat.progress}%</span>
                                            <div className="h-1.5 w-14 overflow-hidden rounded-full bg-slate-200">
                                                <div
                                                    className="h-1.5 rounded-full bg-indigo-600"
                                                    style={{ width: `${cat.progress}%` }}
                                                />
                                            </div>
                                        </div>
                                        {!isClosed && (
                                            <button
                                                type="button"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    onQuickAddTask(cat.id);
                                                }}
                                                className="p-1 text-slate-400 hover:text-indigo-600 transition cursor-pointer"
                                                title="Thêm việc vào mục này"
                                            >
                                                <Plus className="h-3.5 w-3.5" />
                                            </button>
                                        )}
                                    </div>
                                </div>

                                {/* Task List inside Category */}
                                {isOpen && (
                                    <div className="divide-y divide-slate-100">
                                        {cat.filteredTasks.length === 0 ? (
                                            <div className="p-4 text-center text-xs italic text-slate-400">
                                                Không tìm thấy công việc phù hợp trong hạng mục này.
                                            </div>
                                        ) : (
                                            cat.filteredTasks.map((t) => {
                                                const assignee = members.find((m) => m.id === t.assigneeId);
                                                const isDone = t.status === 'Hoàn thành';
                                                const numId = Number(t.id.replace(/\D/g, ''));
                                                const predecessors = (dependencies || []).filter(
                                                    (d) => d.successorId === numId || (t.code && d.successorTaskCode === t.code)
                                                );
                                                const successors = (dependencies || []).filter(
                                                    (d) => d.predecessorId === numId || (t.code && d.predecessorTaskCode === t.code)
                                                );

                                                return (
                                                    <div
                                                        key={t.id}
                                                        className="group flex items-center justify-between gap-3 p-3 text-xs transition hover:bg-slate-50/80"
                                                    >
                                                        <div className="flex min-w-0 flex-1 items-center gap-3">
                                                            <button
                                                                type="button"
                                                                disabled={isClosed}
                                                                onClick={() => !isClosed && onToggleTaskStatus(cat.id, t.id)}
                                                                className={`text-slate-300 transition ${isClosed ? 'cursor-not-allowed opacity-50' : 'group-hover:text-slate-400 cursor-pointer'}`}
                                                                title={isClosed ? 'Dự án đã đóng, không thể thay đổi trạng thái công việc' : 'Đánh dấu hoàn tất'}
                                                            >
                                                                {isDone ? (
                                                                    <CircleCheck className="h-4 w-4 text-emerald-500" />
                                                                ) : (
                                                                    <Circle className="h-4 w-4" />
                                                                )}
                                                            </button>
                                                            <div className="min-w-0">
                                                                <div className="flex items-center gap-2">
                                                                    <span className="font-mono text-[10px] font-medium text-slate-400 shrink-0">
                                                                        {t.code}
                                                                    </span>
                                                                    <span
                                                                        className={`truncate font-medium text-slate-800 ${
                                                                            isDone ? 'text-slate-400 line-through' : ''
                                                                        }`}
                                                                    >
                                                                        {t.name}
                                                                    </span>
                                                                </div>
                                                                <div className="mt-0.5 flex flex-wrap items-center gap-2 text-[11px] text-slate-500">
                                                                    <span className="inline-flex items-center gap-1">
                                                                        <Clock className="h-3 w-3 text-slate-400" /> {t.hours}h
                                                                    </span>
                                                                    <span className="text-slate-300">•</span>
                                                                    <span className="inline-flex items-center rounded bg-indigo-50 px-1 font-mono text-[10px] font-semibold text-indigo-600">
                                                                        {t.startWeek} &rarr; {t.endWeek}
                                                                    </span>

                                                                    {/* Quan hệ Phụ thuộc công việc (NCL-04-CN-004) */}
                                                                    {predecessors.map((p) => (
                                                                        <span
                                                                            key={p.id}
                                                                            className="inline-flex items-center gap-1 rounded bg-purple-50 px-1.5 py-0.5 font-mono text-[10px] font-bold text-purple-700 border border-purple-200"
                                                                            title={`Công việc phải hoàn thành trước: [${p.predecessorTaskCode || p.predecessorId}] ${p.predecessorTaskName}`}
                                                                        >
                                                                            <GitCommit className="h-3 w-3 text-purple-600 shrink-0" />
                                                                            <span>Sau: {p.predecessorTaskCode || `#${p.predecessorId}`}</span>
                                                                        </span>
                                                                    ))}

                                                                    {successors.map((s) => (
                                                                        <span
                                                                            key={s.id}
                                                                            className="inline-flex items-center gap-1 rounded bg-indigo-50 px-1.5 py-0.5 font-mono text-[10px] font-bold text-indigo-700 border border-indigo-200"
                                                                            title={`Công việc đang chờ task này xong: [${s.successorTaskCode || s.successorId}] ${s.successorTaskName}`}
                                                                        >
                                                                            <GitCommit className="h-3 w-3 text-indigo-600 shrink-0" />
                                                                            <span>Tiền đề cho: {s.successorTaskCode || `#${s.successorId}`}</span>
                                                                        </span>
                                                                    ))}

                                                                    {/* Ngân sách giờ công & So sánh thực tế */}
                                                                    <span className="text-slate-300">•</span>
                                                                    <button
                                                                        type="button"
                                                                        disabled={isClosed}
                                                                        onClick={(e) => {
                                                                            e.stopPropagation();
                                                                            if (!isClosed) onOpenBudgetModal?.(t);
                                                                        }}
                                                                        className={`inline-flex items-center gap-1 rounded px-1.5 py-0.5 font-mono text-[10px] font-semibold transition ${
                                                                            isClosed
                                                                                ? 'bg-slate-100 text-slate-400 cursor-not-allowed'
                                                                                : 'bg-slate-100 text-slate-700 hover:bg-indigo-100 hover:text-indigo-700 cursor-pointer'
                                                                        }`}
                                                                        title={isClosed ? 'Dự án đã đóng, không thể điều chỉnh ngân sách' : 'Bấm để đặt/điều chỉnh ngân sách giờ'}
                                                                    >
                                                                        <Target className={`h-3 w-3 ${isClosed ? 'text-slate-400' : 'text-indigo-600'}`} />
                                                                        NS: <strong>{t.budgetHours !== undefined ? `${t.budgetHours}h` : 'Chưa đặt'}</strong>
                                                                        <span className="text-slate-300">|</span>
                                                                        TT: <strong>{t.actualHours || 0}h</strong>
                                                                    </button>

                                                                    {/* Badge phân tích tỷ lệ đã dùng & Cảnh báo ăn mòn lợi nhuận */}
                                                                    {t.budgetHours && t.budgetHours > 0 ? (
                                                                        (() => {
                                                                            const actual = t.actualHours || 0;
                                                                            const pct = t.burnedPercentage !== undefined 
                                                                                ? Number(t.burnedPercentage) 
                                                                                : Math.round((actual / t.budgetHours) * 100);
                                                                            
                                                                            // Ưu tiên burnStatus do backend phân định (Single Source of Truth)
                                                                            const status = t.burnStatus || (
                                                                                actual > t.budgetHours 
                                                                                    ? 'OVER_BUDGET' 
                                                                                    : (pct >= 80 ? 'WARNING' : 'SAFE')
                                                                            );
                                                                            const overHours = Math.max(0, Math.round((actual - t.budgetHours) * 100) / 100);

                                                                            if (status === 'OVER_BUDGET') {
                                                                                return (
                                                                                    <span
                                                                                        className="inline-flex items-center gap-1 rounded border border-rose-200 bg-rose-50 px-1.5 py-0.5 font-mono text-[10px] font-bold text-rose-700 animate-pulse"
                                                                                        title={`Cảnh báo: Làm quá lâu, vượt ngân sách ${overHours}h (${pct}%) đang ăn mòn lợi nhuận!`}
                                                                                    >
                                                                                        <AlertTriangle className="h-3 w-3 text-rose-600 shrink-0" />
                                                                                        Vượt {overHours}h ({pct}%)
                                                                                    </span>
                                                                                );
                                                                            }
                                                                            if (status === 'WARNING') {
                                                                                return (
                                                                                    <span
                                                                                        className="inline-flex items-center rounded border border-amber-200 bg-amber-50 px-1.5 py-0.5 font-mono text-[10px] font-semibold text-amber-700"
                                                                                        title={`Tiệm cận hạn mức ngân sách: ${pct}%`}
                                                                                    >
                                                                                        {pct}%
                                                                                    </span>
                                                                                );
                                                                            }
                                                                            return (
                                                                                <span
                                                                                    className="inline-flex items-center rounded border border-emerald-200 bg-emerald-50 px-1.5 py-0.5 font-mono text-[10px] font-medium text-emerald-700"
                                                                                    title={`Ngân sách an toàn: ${pct}%`}
                                                                                >
                                                                                    {pct}%
                                                                                </span>
                                                                            );
                                                                        })()
                                                                    ) : null}
                                                                </div>
                                                            </div>
                                                        </div>

                                                        {/* Assignee & Badges */}
                                                        <div className="flex shrink-0 items-center gap-2">
                                                            {getPriorityBadge(t.priority)}
                                                            {getStatusBadge(t.status)}
                                                            <button
                                                                type="button"
                                                                onClick={(e) => {
                                                                    e.stopPropagation();
                                                                    onOpenBudgetModal?.(t);
                                                                }}
                                                                className="rounded-lg p-1 text-slate-400 hover:bg-indigo-50 hover:text-indigo-600 transition cursor-pointer"
                                                                title="Đặt ngân sách giờ công"
                                                            >
                                                                <Target className="h-3.5 w-3.5" />
                                                            </button>
                                                            <div
                                                                className="flex items-center gap-1.5 pl-1"
                                                                title={assignee ? `${assignee.name} (${assignee.role})` : 'Chưa giao'}
                                                            >
                                                                <img
                                                                    className="h-6 w-6 rounded-full border border-slate-200 object-cover"
                                                                    src={assignee ? assignee.avatar : 'https://placehold.co/100x100?text=NA'}
                                                                    alt=""
                                                                />
                                                                <span className="hidden max-w-[80px] truncate text-[11px] font-medium text-slate-600 md:inline">
                                                                    {assignee ? assignee.name.split(' ').pop() : 'N/A'}
                                                                </span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                );
                                            })
                                        )}
                                    </div>
                                )}
                            </div>
                        );
                    })
                )}
            </div>

            {/* WBS Footer Summary */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 bg-slate-50 p-3 text-xs text-slate-500">
                <div className="flex flex-wrap items-center gap-3">
                    <span>
                        Ước tính: <strong className="text-slate-800">{totalHoursEstimate.toLocaleString()}h</strong>
                    </span>
                    <span className="text-slate-300">•</span>
                    <span>
                        Ngân sách: <strong className="text-indigo-700">{totalBudgetHours.toLocaleString()}h</strong>
                    </span>
                    <span className="text-slate-300">•</span>
                    <span>
                        Thực tế duyệt: <strong className="text-slate-800">{totalActualHours.toLocaleString()}h</strong>
                    </span>
                </div>
                <div className="flex items-center gap-3">
                    {overBudgetCount > 0 && (
                        <span className="inline-flex items-center gap-1 rounded-full border border-rose-200 bg-rose-50 px-2 py-0.5 text-[11px] font-bold text-rose-700">
                            <AlertTriangle className="h-3.5 w-3.5 text-rose-600" /> {overBudgetCount} việc vượt ngân sách
                        </span>
                    )}
                    <span className="inline-flex items-center gap-1 font-medium text-emerald-600">
                        <CheckCircle2 className="h-3.5 w-3.5" /> Đã hoàn tất {totalDoneTasks}/{totalTasksCount} việc
                    </span>
                </div>
            </div>
        </section>
    );
}

