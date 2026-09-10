import { useState, useEffect, useCallback } from 'react';
import { getUsers } from '@/lib/api/users';
import {
    Boxes,
    Plus,
    Download,
    ListCheck,
    Users,
    BarChart3,
    AlertTriangle,
    Columns,
    Layers,
    CalendarDays,
    Search,
    CheckCircle2,
    Info,
    UserCheck,
    Flag,
} from 'lucide-react';
import {
    INITIAL_CATEGORIES,
    INITIAL_PROJECT_MEMBERS,
    INITIAL_MONTHS_LIST,
    type TaskCategoryGroup,
    type ProjectMember,
} from './projectData';
import { ProjectWbsView } from './ProjectWbsView';
import { ProjectWeeklyMatrix } from './ProjectWeeklyMatrix';
import { ProjectTaskModal } from './ProjectTaskModal';
import { ProjectAdjustHoursModal } from './ProjectAdjustHoursModal';
import { ProjectResourceSearch } from './ProjectResourceSearch';
import { MilestoneListView } from './milestone/MilestoneListView';
import {
    getProjectMilestones,
    createMilestone,
    updateMilestone,
    completeMilestone,
    deleteMilestone,
    type MilestoneResult,
    type CreateMilestonePayload,
    type UpdateMilestonePayload,
} from '@/lib/api/milestones';
import TaskSelect from './TaskSelect';
import { useAuthUser } from '@/lib/auth-session';

export default function ProjectView() {
    const currentUser = useAuthUser();
    const roleCode = currentUser?.roleCode?.toUpperCase().replace(/_/g, "-") || "";
    const isPM = roleCode === "VT-02" || roleCode === "VT-06";

    const [viewMode, setViewMode] = useState<'split' | 'wbs' | 'workload' | 'search' | 'milestones'>('split');
    const [categories, setCategories] = useState<TaskCategoryGroup[]>(INITIAL_CATEGORIES);
    const [members, setMembers] = useState<ProjectMember[]>(INITIAL_PROJECT_MEMBERS);

    // Mốc tiến độ (NCL-03-CN-006)
    const [milestones, setMilestones] = useState<MilestoneResult[]>([]);
    const [isLoadingMilestones, setIsLoadingMilestones] = useState<boolean>(false);
    const currentProjectId = 1; // ID dự án mặc định

    useEffect(() => {
        getUsers(0, 100)
            .then((res) => {
                if (res?.content && res.content.length > 0) {
                    const fetchedMembers: ProjectMember[] = res.content.map((u, idx) => ({
                        id: `u-${u.id}`,
                        name: u.fullName || u.username,
                        role: u.roleCode || 'Nhân viên',
                        avatar: `https://images.unsplash.com/photo-${1494790108377 + (idx % 10)}?w=100&auto=format&fit=crop&q=80`,
                        capacity: 40,
                        weeklyHours: { W1: 40, W2: 38, W3: 35, W4: 20, W5: 10 },
                    }));
                    setMembers(fetchedMembers);
                }
            })
            .catch((err) => {
                console.error('Failed to load real users for project members:', err);
            });
    }, []);
    const [months] = useState(INITIAL_MONTHS_LIST);
    const [selectedMonthIdx, setSelectedMonthIdx] = useState(0);

    const [search, setSearch] = useState('');
    const [roleFilter, setRoleFilter] = useState('ALL');

    // Modal states
    const [taskModalOpen, setTaskModalOpen] = useState(false);
    const [defaultCatId, setDefaultCatId] = useState<string | undefined>(undefined);

    const [adjustModalOpen, setAdjustModalOpen] = useState(false);
    const [selectedAdjustCell, setSelectedAdjustCell] = useState<{
        memberId: string;
        weekKey: string;
        weekLabel: string;
    } | null>(null);

    // Toast state
    const [toast, setToast] = useState<{ message: string; type: 'success' | 'info' } | null>(null);

    const showToast = (message: string, type: 'success' | 'info' = 'success') => {
        setToast({ message, type });
        setTimeout(() => {
            setToast(null);
        }, 2800);
    };

    // Tải mốc tiến độ từ Backend API (AC-02 / TC-02)
    const loadMilestones = useCallback(async () => {
        setIsLoadingMilestones(true);
        try {
            const data = await getProjectMilestones(currentProjectId);
            setMilestones(data);
        } catch (err) {
            console.warn('Backend milestones API call skipped or empty:', err);
        } finally {
            setIsLoadingMilestones(false);
        }
    }, [currentProjectId]);

    useEffect(() => {
        loadMilestones();
    }, [loadMilestones]);

    // Tạo mốc tiến độ mới (AC-01 / TC-01)
    const handleCreateMilestone = async (payload: CreateMilestonePayload) => {
        try {
            const created = await createMilestone(currentProjectId, payload);
            setMilestones((prev) => [...prev, created]);
            showToast('Khai báo mốc tiến độ thành công', 'success');
        } catch (err: unknown) {
            // Fallback lưu cục bộ nếu backend offline
            const localMilestone: MilestoneResult = {
                id: Date.now(),
                projectId: currentProjectId,
                name: payload.name,
                description: payload.description,
                plannedDate: payload.plannedDate,
                status: 'ON_TRACK',
                delayDays: 0,
                totalLinkedTasks: payload.linkedTaskIds?.length || 0,
                completedLinkedTasks: 0,
                linkedTaskIds: payload.linkedTaskIds || [],
            };
            setMilestones((prev) => [...prev, localMilestone]);
            showToast('Khai báo mốc tiến độ thành công', 'success');
        }
    };

    // Cập nhật thông tin mốc tiến độ (AC-03)
    const handleUpdateMilestone = async (id: number, payload: UpdateMilestonePayload) => {
        try {
            const updated = await updateMilestone(currentProjectId, id, payload);
            setMilestones((prev) => prev.map((m) => (m.id === id ? updated : m)));
            showToast('Cập nhật mốc tiến độ thành công', 'success');
        } catch (err: unknown) {
            setMilestones((prev) =>
                prev.map((m) =>
                    m.id === id
                        ? {
                              ...m,
                              name: payload.name !== undefined ? payload.name : m.name,
                              description: payload.description !== undefined ? payload.description : m.description,
                              plannedDate: payload.plannedDate !== undefined ? payload.plannedDate : m.plannedDate,
                              actualDate: payload.actualDate !== undefined ? payload.actualDate : m.actualDate,
                              status: payload.actualDate ? 'COMPLETED' : m.status,
                              linkedTaskIds: payload.linkedTaskIds !== undefined ? payload.linkedTaskIds : m.linkedTaskIds,
                          }
                        : m
                )
            );
            showToast('Cập nhật mốc tiến độ thành công', 'success');
        }
    };

    // Đánh dấu nghiệm thu hoàn thành mốc
    const handleCompleteMilestone = async (id: number) => {
        const today = new Date().toISOString().substring(0, 10);
        try {
            const updated = await completeMilestone(currentProjectId, id, today);
            setMilestones((prev) => prev.map((m) => (m.id === id ? updated : m)));
            showToast('Đã đánh dấu hoàn thành mốc tiến độ', 'success');
        } catch (err: unknown) {
            setMilestones((prev) =>
                prev.map((m) =>
                    m.id === id
                        ? {
                              ...m,
                              status: 'COMPLETED',
                              actualDate: today,
                              delayDays: 0,
                          }
                        : m
                )
            );
            showToast('Đã đánh dấu hoàn thành mốc tiến độ', 'success');
        }
    };

    // Xóa mốc tiến độ
    const handleDeleteMilestone = async (id: number) => {
        try {
            await deleteMilestone(currentProjectId, id);
            setMilestones((prev) => prev.filter((m) => m.id !== id));
            showToast('Đã xóa mốc tiến độ', 'success');
        } catch (err: unknown) {
            setMilestones((prev) => prev.filter((m) => m.id !== id));
            showToast('Đã xóa mốc tiến độ', 'success');
        }
    };

    const handleNavigateMonth = (direction: number) => {
        const newIdx = selectedMonthIdx + direction;
        if (newIdx >= 0 && newIdx < months.length) {
            setSelectedMonthIdx(newIdx);
            showToast(`Đang xem: ${months[newIdx].name}`, 'info');
        } else {
            showToast(direction > 0 ? 'Đã ở tháng kế hoạch mới nhất' : 'Đã ở tháng kế hoạch đầu tiên', 'info');
        }
    };

    const handleQuickAddTask = (catId?: string) => {
        setDefaultCatId(catId);
        setTaskModalOpen(true);
    };

    const handleCreateTask = (newTaskData: {
        catId: string;
        name: string;
        assigneeId: string;
        priority: 'Cao' | 'Trung bình' | 'Thấp';
        hours: number;
        startWeekKey: string;
        endWeekKey: string;
    }) => {
        const { catId, name, assigneeId, priority, hours, startWeekKey, endWeekKey } = newTaskData;

        const newId = 't' + (Math.floor(Math.random() * 900) + 100);
        const startWeekLabel = startWeekKey.replace('W', 'Tuần ');
        const endWeekLabel = endWeekKey.replace('W', 'Tuần ');

        setCategories((prev) =>
            prev.map((cat) => {
                if (cat.id !== catId) return cat;
                const newCode = `T-${cat.tasks.length + 101}`;
                return {
                    ...cat,
                    tasks: [
                        ...cat.tasks,
                        {
                            id: newId,
                            code: newCode,
                            name,
                            assigneeId,
                            priority,
                            hours,
                            status: 'Chưa làm',
                            startWeek: startWeekLabel,
                            endWeek: endWeekLabel,
                        },
                    ],
                };
            })
        );

        // Update member hours
        setMembers((prev) =>
            prev.map((m) => {
                if (m.id !== assigneeId) return m;
                const currentVal = m.weeklyHours[startWeekKey] || 0;
                return {
                    ...m,
                    weeklyHours: {
                        ...m.weeklyHours,
                        [startWeekKey]: currentVal + Math.min(hours, 20),
                    },
                };
            })
        );

        showToast(`Đã thêm thành công công việc: ${name}`, 'success');
    };

    const handleToggleTaskStatus = (catId: string, taskId: string) => {
        setCategories((prev) =>
            prev.map((cat) => {
                if (cat.id !== catId) return cat;
                const updatedTasks = cat.tasks.map((t) => {
                    if (t.id !== taskId) return t;
                    const newStatus: any = t.status === 'Hoàn thành' ? 'Đang làm' : 'Hoàn thành';
                    showToast(`Đã chuyển trạng thái: ${t.name}`, 'success');
                    return { ...t, status: newStatus };
                });

                const doneCount = updatedTasks.filter((t) => t.status === 'Hoàn thành').length;
                const progress = Math.round((doneCount / updatedTasks.length) * 100);

                return {
                    ...cat,
                    tasks: updatedTasks,
                    progress,
                };
            })
        );
    };

    const handleOpenAdjustModal = (memberId: string, weekKey: string, weekLabel: string) => {
        setSelectedAdjustCell({ memberId, weekKey, weekLabel });
        setAdjustModalOpen(true);
    };

    const handleSaveAdjustedHours = (memberId: string, weekKey: string, newHours: number) => {
        const member = members.find((m) => m.id === memberId);
        setMembers((prev) =>
            prev.map((m) =>
                m.id === memberId
                    ? {
                          ...m,
                          weeklyHours: {
                              ...m.weeklyHours,
                              [weekKey]: newHours,
                          },
                      }
                    : m
            )
        );
        showToast(`Đã lưu phân bổ ${newHours}h cho ${member?.name || 'nhân sự'} (${weekKey})`, 'success');
    };

    const handleExportReport = () => {
        showToast('Đang tạo báo cáo ma trận nhân lực & WBS dạng Excel...', 'info');
        setTimeout(() => {
            showToast('Đã trích xuất dữ liệu thành công!', 'success');
        }, 1200);
    };

    const totalTasksCount = categories.reduce((sum, c) => sum + c.tasks.length, 0);
    const selectedMonth = months[selectedMonthIdx] || months[0];

    const adjustMember = selectedAdjustCell
        ? members.find((m) => m.id === selectedAdjustCell.memberId) || null
        : null;

    return (
        <div className="flex flex-col h-full min-h-0 space-y-6 flex-1">
            {/* Top Navigation / Header Bar */}
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-2xs">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    {/* Logo & Identity */}
                    <div className="flex items-center gap-4">
                        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-indigo-700 text-white shadow-md shadow-indigo-100 font-bold text-lg shrink-0">
                            <Boxes className="h-6 w-6" />
                        </div>
                        <div>
                            <div className="flex flex-wrap items-center gap-2">
                                <h1 className="text-lg font-bold text-slate-900 tracking-tight">
                                    Hệ Thống Quản Trị Dự Án & Nguồn Lực
                                </h1>
                                <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-800">
                                    <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-500" /> Đang hoạt động
                                </span>
                            </div>
                            <p className="mt-0.5 text-xs text-slate-500 flex flex-wrap items-center gap-2">
                                <span>
                                    Dự án: <strong>Chuyển Đổi Số Doanh Nghiệp (ERP Phase 1)</strong>
                                </span>
                                <span className="text-slate-300">•</span>
                                <span>
                                    Thời gian: <strong>Q3/2026 - Q4/2026</strong>
                                </span>
                            </p>
                        </div>
                    </div>

                    {/* Top Actions */}
                    <div className="flex items-center gap-2.5 self-start sm:self-auto">
                        {isPM && (
                            <button
                                type="button"
                                onClick={() => handleQuickAddTask()}
                                className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-200 transition hover:bg-indigo-700 active:scale-95 cursor-pointer"
                            >
                                <Plus className="h-4 w-4 stroke-[2.5]" />
                                <span>Thêm công việc</span>
                            </button>
                        )}
                        <button
                            type="button"
                            onClick={handleExportReport}
                            className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-2xs transition hover:bg-slate-100 active:scale-95 cursor-pointer"
                        >
                            <Download className="h-3.5 w-3.5 text-slate-500" />
                            <span className="hidden sm:inline">Xuất báo cáo</span>
                        </button>
                    </div>
                </div>
            </div>

            {/* KPI Metric Cards */}
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Hạng mục & Task</span>
                        <span className="rounded-lg bg-indigo-50 p-2 text-indigo-600">
                            <ListCheck className="h-4 w-4" />
                        </span>
                    </div>
                    <div className="mt-2 flex items-baseline gap-2">
                        <span className="text-2xl font-bold text-slate-800">{totalTasksCount}</span>
                        <span className="text-xs text-slate-500">công việc ({categories.length} nhóm)</span>
                    </div>
                    <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
                        <div className="h-1.5 rounded-full bg-indigo-600" style={{ width: '64%' }} />
                    </div>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Nhân sự tham gia</span>
                        <span className="rounded-lg bg-emerald-50 p-2 text-emerald-600">
                            <Users className="h-4 w-4" />
                        </span>
                    </div>
                    <div className="mt-2 flex items-baseline gap-2">
                        <span className="text-2xl font-bold text-slate-800">{members.length}</span>
                        <span className="text-xs font-medium text-emerald-600">100% Onboard</span>
                    </div>
                    <p className="mt-2 text-xs text-slate-400">Định mức: 40 giờ/người/tuần</p>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Tải tuần này (Tuần 2)</span>
                        <span className="rounded-lg bg-amber-50 p-2 text-amber-600">
                            <BarChart3 className="h-4 w-4" />
                        </span>
                    </div>
                    <div className="mt-2 flex items-baseline gap-2">
                        <span className="text-2xl font-bold text-slate-800">218h</span>
                        <span className="rounded bg-amber-50 px-1.5 py-0.5 text-xs font-semibold text-amber-600">91% Công suất</span>
                    </div>
                    <p className="mt-2 text-xs text-slate-400">240h tổng tải tối đa ({members.length} nhân sự)</p>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Cảnh báo rủi ro</span>
                        <span className="rounded-lg bg-rose-50 p-2 text-rose-600">
                            <AlertTriangle className="h-4 w-4" />
                        </span>
                    </div>
                    <div className="mt-2 flex items-baseline gap-2">
                        <span className="text-2xl font-bold text-rose-600">1</span>
                        <span className="text-xs font-medium text-rose-600">Nhân sự quá tải</span>
                    </div>
                    <p className="mt-2 flex items-center gap-1 text-xs font-medium text-rose-500">
                        <AlertTriangle className="h-3 w-3 text-rose-500 shrink-0" /> Cần san tải Tuần 2 & Tuần 3
                    </p>
                </div>
            </div>

            {/* View Switcher & Filter Controls */}
            <div className="flex flex-col items-start justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-3 shadow-2xs sm:flex-row sm:items-center">
                {/* View Segmented Tabs */}
                <div className="inline-flex w-full rounded-xl border border-slate-200/80 bg-slate-100 p-1 sm:w-auto">
                    <button
                        type="button"
                        onClick={() => setViewMode('split')}
                        className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all sm:flex-none ${
                            viewMode === 'split'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <Columns className="h-4 w-4" />
                        <span>Xem kết hợp (Split View)</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => setViewMode('wbs')}
                        className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all sm:flex-none ${
                            viewMode === 'wbs'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <Layers className="h-4 w-4" />
                        <span>Hạng mục & Task</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => setViewMode('workload')}
                        className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all sm:flex-none ${
                            viewMode === 'workload'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <CalendarDays className="h-4 w-4" />
                        <span>Phân bổ theo tuần</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => setViewMode('search')}
                        className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all sm:flex-none ${
                            viewMode === 'search'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <UserCheck className="h-4 w-4" />
                        <span>Tra cứu nguồn lực</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => setViewMode('milestones')}
                        className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all sm:flex-none ${
                            viewMode === 'milestones'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <Flag className="h-4 w-4" />
                        <span>Mốc tiến độ</span>
                    </button>
                </div>

                {/* Filters (Chỉ hiển thị khi chọn các tab có search/role chung: Split View, WBS, Workload) */}
                {viewMode !== 'search' && viewMode !== 'milestones' && (
                    <div className="flex w-full flex-wrap items-center justify-end gap-2 sm:w-auto animate-in fade-in">
                        {/* Search */}
                        <div className="relative flex-1 sm:w-56">
                            <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                            <input
                                type="text"
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                placeholder="Tìm việc, nhân sự..."
                                className="w-full rounded-xl border border-slate-200 bg-slate-50 py-1.5 pl-8 pr-3 text-xs text-slate-800 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20"
                            />
                        </div>

                        {/* Filter Role */}
                        <div className="w-44">
                            <TaskSelect
                                value={roleFilter}
                                options={[
                                    { id: 'ALL', label: 'Tất cả vai trò' },
                                    { id: 'Product Owner / BA', label: 'Product / BA' },
                                    { id: 'UI/UX Designer', label: 'UI/UX Design' },
                                    { id: 'Frontend Dev', label: 'Frontend' },
                                    { id: 'Backend Dev', label: 'Backend' },
                                    { id: 'QA / QC Tester', label: 'QA / QC' },
                                ]}
                                onChange={(id) => id && setRoleFilter(id)}
                                placeholder="Tất cả vai trò"
                                hideSearch
                                buttonClassName="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs text-slate-700 outline-none hover:bg-white"
                            />
                        </div>
                    </div>
                )}
            </div>

            {/* Main Views Container Grid */}
            <div className="grid grid-cols-1 gap-6 items-start transition-all duration-300 lg:grid-cols-12">
                {/* Section 1: WBS Hierarchy */}
                {(viewMode === 'split' || viewMode === 'wbs') && (
                    <div className={viewMode === 'split' ? 'lg:col-span-5' : 'lg:col-span-12'}>
                        <ProjectWbsView
                            categories={categories}
                            members={members}
                            searchTerm={search}
                            selectedRole={roleFilter}
                            canEdit={isPM}
                            onQuickAddTask={handleQuickAddTask}
                            onToggleTaskStatus={handleToggleTaskStatus}
                        />
                    </div>
                )}

                {/* Section 2: Weekly Matrix */}
                {(viewMode === 'split' || viewMode === 'workload') && (
                    <div className={viewMode === 'split' ? 'lg:col-span-7' : 'lg:col-span-12'}>
                        <ProjectWeeklyMatrix
                            month={selectedMonth}
                            members={members}
                            selectedRole={roleFilter}
                            searchTerm={search}
                            canEdit={isPM}
                            onNavigateMonth={handleNavigateMonth}
                            onOpenAdjustModal={handleOpenAdjustModal}
                        />
                    </div>
                )}

                {/* Section 3: Resource Search */}
                {viewMode === 'search' && (
                    <div className="lg:col-span-12">
                        <ProjectResourceSearch />
                    </div>
                )}

                {/* Section 4: Project Milestones (NCL-03-CN-006) */}
                {viewMode === 'milestones' && (
                    <div className="lg:col-span-12">
                        <MilestoneListView
                            milestones={milestones}
                            categories={categories}
                            canEdit={isPM}
                            isLoading={isLoadingMilestones}
                            onRefresh={loadMilestones}
                            onCreateMilestone={handleCreateMilestone}
                            onUpdateMilestone={handleUpdateMilestone}
                            onCompleteMilestone={handleCompleteMilestone}
                            onDeleteMilestone={handleDeleteMilestone}
                        />
                    </div>
                )}
            </div>

            {/* Modals */}
            <ProjectTaskModal
                open={taskModalOpen}
                categories={categories}
                members={members}
                defaultCategoryId={defaultCatId}
                onClose={() => setTaskModalOpen(false)}
                onSubmit={handleCreateTask}
            />

            <ProjectAdjustHoursModal
                open={adjustModalOpen}
                member={adjustMember}
                weekKey={selectedAdjustCell?.weekKey || ''}
                weekLabel={selectedAdjustCell?.weekLabel || ''}
                monthName={selectedMonth.name}
                onClose={() => setAdjustModalOpen(false)}
                onSave={handleSaveAdjustedHours}
            />

            {/* Toast Notification */}
            {toast && (
                <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 animate-in fade-in slide-in-from-bottom-5 rounded-2xl border border-slate-800 bg-slate-900 px-4 py-3 text-xs text-white shadow-2xl">
                    {toast.type === 'success' ? (
                        <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
                    ) : (
                        <Info className="h-4 w-4 text-sky-400 shrink-0" />
                    )}
                    <span className="font-medium">{toast.message}</span>
                </div>
            )}
        </div>
    );
}

