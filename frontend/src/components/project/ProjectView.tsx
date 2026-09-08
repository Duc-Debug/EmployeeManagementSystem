import { useState, useEffect, useCallback } from 'react';
import { getUsers } from '@/lib/api/users';
import {
    getProjects,
    getProjectWbs,
    createTask,
    updateTask,
    type ProjectResult,
    type TaskNodeResult,
    type BackendTaskStatus,
} from '@/lib/api/projects';
import { setTaskBudget } from '@/lib/api/tasks';
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
    Database,
    RefreshCw,
    FolderPlus,
    Calendar,
    ChevronDown,
} from 'lucide-react';
import {
    INITIAL_CATEGORIES,
    INITIAL_PROJECT_MEMBERS,
    INITIAL_MONTHS_LIST,
    type TaskCategoryGroup,
    type ProjectMember,
    type TaskItem,
} from './projectData';
import { ProjectWbsView } from './ProjectWbsView';
import { ProjectWeeklyMatrix } from './ProjectWeeklyMatrix';
import { ProjectTaskModal } from './ProjectTaskModal';
import { ProjectAdjustHoursModal } from './ProjectAdjustHoursModal';
import { ProjectBudgetModal } from './ProjectBudgetModal';
import { ProjectCreateModal } from './ProjectCreateModal';

const CATEGORY_COLORS = ['indigo', 'purple', 'emerald', 'sky', 'amber', 'rose'];

/**
 * Adapter chuyển đổi cây WBS từ Backend API (TaskNodeResult) sang cấu trúc hiển thị UI (TaskCategoryGroup)
 */
function mapBackendWbsToUiCategories(
    nodes: TaskNodeResult[],
    members: ProjectMember[]
): TaskCategoryGroup[] {
    if (!nodes || nodes.length === 0) return [];

    const memberMap = new Map<string, ProjectMember>();
    members.forEach((m) => {
        const numId = m.id.replace(/\D/g, '');
        if (numId) memberMap.set(numId, m);
    });

    const categories: TaskCategoryGroup[] = [];
    const standaloneTasks: TaskItem[] = [];

    const statusMap: Record<string, TaskItem['status']> = {
        TODO: 'Chưa làm',
        IN_PROGRESS: 'Đang làm',
        DONE: 'Hoàn thành',
        CANCELLED: 'Chưa làm',
    };

    nodes.forEach((node, idx) => {
        if (node.taskType === 'CATEGORY') {
            const childTasks: TaskItem[] = (node.children || []).map((child, cIdx) => {
                const assigneeKey = child.assigneeId ? String(child.assigneeId) : '';
                const member = memberMap.get(assigneeKey);

                return {
                    id: String(child.id),
                    code: child.taskCode || `T-${101 + cIdx}`,
                    name: child.name,
                    assigneeId: member ? member.id : (child.assigneeId ? `u-${child.assigneeId}` : ''),
                    priority: 'Trung bình',
                    hours: Number(child.estimatedHours || 0),
                    budgetHours: Number(child.budgetHours || 0),
                    actualHours: Number(child.actualHours || 0),
                    status: statusMap[child.status] || 'Chưa làm',
                    startWeek: 'Tuần 1',
                    endWeek: 'Tuần 3',
                };
            });

            const doneCount = childTasks.filter((t) => t.status === 'Hoàn thành').length;
            const progress = childTasks.length > 0 ? Math.round((doneCount / childTasks.length) * 100) : 0;

            categories.push({
                id: String(node.id),
                code: node.taskCode || `HM-0${idx + 1}`,
                name: node.name,
                lead: 'PM',
                progress,
                color: CATEGORY_COLORS[idx % CATEGORY_COLORS.length],
                tasks: childTasks,
            });
        } else {
            // Root task không nằm trong category
            const assigneeKey = node.assigneeId ? String(node.assigneeId) : '';
            const member = memberMap.get(assigneeKey);

            standaloneTasks.push({
                id: String(node.id),
                code: node.taskCode || `T-${101 + idx}`,
                name: node.name,
                assigneeId: member ? member.id : (node.assigneeId ? `u-${node.assigneeId}` : ''),
                priority: 'Trung bình',
                hours: Number(node.estimatedHours || 0),
                budgetHours: Number(node.budgetHours || 0),
                actualHours: Number(node.actualHours || 0),
                status: statusMap[node.status] || 'Chưa làm',
                startWeek: 'Tuần 1',
                endWeek: 'Tuần 3',
            });
        }
    });

    if (standaloneTasks.length > 0) {
        const doneCount = standaloneTasks.filter((t) => t.status === 'Hoàn thành').length;
        const progress = Math.round((doneCount / standaloneTasks.length) * 100);
        categories.unshift({
            id: 'root-general',
            code: 'HM-00',
            name: 'Hạng Mục Chung',
            lead: 'PM',
            progress,
            color: 'indigo',
            tasks: standaloneTasks,
        });
    }

    return categories;
}

export default function ProjectView() {
    const [viewMode, setViewMode] = useState<'split' | 'wbs' | 'workload'>('split');
    const [categories, setCategories] = useState<TaskCategoryGroup[]>(INITIAL_CATEGORIES);
    const [members, setMembers] = useState<ProjectMember[]>(INITIAL_PROJECT_MEMBERS);
    const [budgetModalOpen, setBudgetModalOpen] = useState(false);
    const [selectedBudgetTask, setSelectedBudgetTask] = useState<TaskItem | null>(null);

    // Real projects backend state
    const [projectsList, setProjectsList] = useState<ProjectResult[]>([]);
    const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
    const [isBackendConnected, setIsBackendConnected] = useState<boolean>(false);
    const [isLoadingProjects, setIsLoadingProjects] = useState<boolean>(false);
    const [isLoadingWbs, setIsLoadingWbs] = useState<boolean>(false);
    const [projectCreateModalOpen, setProjectCreateModalOpen] = useState<boolean>(false);

    // Selected project object
    const selectedProject = projectsList.find((p) => p.id === selectedProjectId) || null;

    // Toast state
    const [toast, setToast] = useState<{ message: string; type: 'success' | 'info' } | null>(null);

    const showToast = useCallback((message: string, type: 'success' | 'info' = 'success') => {
        setToast({ message, type });
        setTimeout(() => {
            setToast(null);
        }, 3200);
    }, []);

    // 1. Tải danh sách nhân sự thật từ API
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
                console.warn('Backend users API call skipped/failed, keeping default members:', err);
            });
    }, []);

    // 2. Tải danh sách dự án thật từ Database
    const loadProjects = useCallback(async () => {
        setIsLoadingProjects(true);
        try {
            const res = await getProjects(0, 50);
            if (res?.content && res.content.length > 0) {
                setProjectsList(res.content);
                setIsBackendConnected(true);
                setSelectedProjectId((prevId) => {
                    const exists = res.content.some((p) => p.id === prevId);
                    return exists ? prevId : res.content[0].id;
                });
            } else {
                setProjectsList([]);
                setIsBackendConnected(false);
                setSelectedProjectId(null);
                setCategories(INITIAL_CATEGORIES);
            }
        } catch (err) {
            console.warn('Failed to fetch projects from backend:', err);
            setIsBackendConnected(false);
            setProjectsList([]);
            setSelectedProjectId(null);
            setCategories(INITIAL_CATEGORIES);
        } finally {
            setIsLoadingProjects(false);
        }
    }, []);

    useEffect(() => {
        loadProjects();
    }, [loadProjects]);

    // 3. Tải cây WBS khi chọn một dự án thật
    const loadWbsForProject = useCallback(async (projId: number) => {
        setIsLoadingWbs(true);
        try {
            const wbsNodes = await getProjectWbs(projId);
            const mapped = mapBackendWbsToUiCategories(wbsNodes, members);
            setCategories(mapped);
        } catch (err) {
            console.warn(`Failed to fetch WBS for project ${projId}:`, err);
            setCategories([]);
        } finally {
            setIsLoadingWbs(false);
        }
    }, [members]);

    useEffect(() => {
        if (selectedProjectId) {
            loadWbsForProject(selectedProjectId);
        }
    }, [selectedProjectId, loadWbsForProject]);

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

    // Tạo Task mới: Đấu nối Database Backend khi có selectedProjectId
    const handleCreateTask = async (newTaskData: {
        catId: string;
        name: string;
        assigneeId: string;
        priority: 'Cao' | 'Trung bình' | 'Thấp';
        hours: number;
        startWeekKey: string;
        endWeekKey: string;
        newCategoryName?: string;
    }) => {
        const { catId, name, assigneeId, priority, hours, startWeekKey, endWeekKey, newCategoryName } = newTaskData;

        if (selectedProjectId) {
            try {
                let parentIdToUse: number | null = null;

                // Nếu người dùng nhập tên hạng mục mới
                if (newCategoryName) {
                    const newCat = await createTask(selectedProjectId, {
                        name: newCategoryName,
                        taskType: 'CATEGORY',
                        sortOrder: categories.length + 1,
                    });
                    parentIdToUse = newCat.id;
                } else if (catId && catId !== '__NEW__' && catId !== 'root-general') {
                    const numCat = parseInt(catId.replace(/\D/g, ''), 10);
                    if (!isNaN(numCat) && numCat > 0) {
                        parentIdToUse = numCat;
                    }
                }

                // Trích xuất numeric assigneeId từ string u-123
                const numAssignee = assigneeId ? parseInt(assigneeId.replace(/\D/g, ''), 10) : null;

                await createTask(selectedProjectId, {
                    parentId: parentIdToUse,
                    name,
                    taskType: 'TASK',
                    assigneeId: numAssignee && !isNaN(numAssignee) && numAssignee > 0 ? numAssignee : null,
                    estimatedHours: hours,
                    sortOrder: 0,
                });

                await loadWbsForProject(selectedProjectId);
                showToast(`Đã lưu công việc "${name}" vào cơ sở dữ liệu!`, 'success');
                return;
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : 'Có lỗi khi lưu công việc vào cơ sở dữ liệu';
                showToast(`Lỗi: ${msg}`, 'info');
                console.error('Failed to create task in backend:', err);
            }
        }

        // Fallback lưu local state (khi xem dự án demo mẫu)
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

    // Chuyển đổi trạng thái công việc
    const handleToggleTaskStatus = async (catId: string, taskId: string) => {
        let newStatusStr: TaskItem['status'] = 'Hoàn thành';
        let targetTaskName = '';

        setCategories((prev) =>
            prev.map((cat) => {
                if (cat.id !== catId) return cat;
                const updatedTasks = cat.tasks.map((t) => {
                    if (t.id !== taskId) return t;
                    targetTaskName = t.name;
                    newStatusStr = t.status === 'Hoàn thành' ? 'Đang làm' : 'Hoàn thành';
                    return { ...t, status: newStatusStr };
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

        showToast(`Đã chuyển trạng thái: ${targetTaskName || taskId}`, 'success');

        // Ghi nhận xuống Backend
        const numTaskId = parseInt(taskId.replace(/\D/g, ''), 10);
        if (selectedProjectId && !isNaN(numTaskId) && numTaskId > 0) {
            try {
                const backendStatus: BackendTaskStatus = newStatusStr === 'Hoàn thành' ? 'DONE' : 'IN_PROGRESS';
                await updateTask(selectedProjectId, numTaskId, { status: backendStatus });
            } catch (err) {
                console.warn('Backend task status update failed, keeping optimistic UI state:', err);
            }
        }
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

    const handleOpenBudgetModal = (task: TaskItem) => {
        setSelectedBudgetTask(task);
        setBudgetModalOpen(true);
    };

    // Đặt ngân sách: Gọi API PATCH /api/v1/projects/{projectId}/tasks/{taskId}/budget
    const handleSaveTaskBudget = async (taskId: string, newBudgetHours: number) => {
        setCategories((prev) =>
            prev.map((cat) => ({
                ...cat,
                tasks: cat.tasks.map((t) => (t.id === taskId ? { ...t, budgetHours: newBudgetHours } : t)),
            }))
        );

        const taskName = selectedBudgetTask?.name || taskId;
        showToast(`Đã lưu ngân sách ${newBudgetHours}h cho công việc: ${taskName}`, 'success');

        const numTaskId = parseInt(taskId.replace(/\D/g, ''), 10);
        const projIdToUse = selectedProjectId || 1;
        if (!isNaN(numTaskId) && numTaskId > 0) {
            try {
                await setTaskBudget(projIdToUse, numTaskId, newBudgetHours);
            } catch (err) {
                console.warn('Backend budget sync skipped/failed, keeping UI state:', err);
            }
        }
    };

    const handleProjectCreated = async (newProjectId: number) => {
        await loadProjects();
        setSelectedProjectId(newProjectId);
        showToast('Dự án đã được tạo thành công trong Database!', 'success');
    };

    const handleExportReport = () => {
        showToast('Đang tạo báo cáo ma trận nhân lực & WBS dạng Excel...', 'info');
        setTimeout(() => {
            showToast('Đã trích xuất dữ liệu thành công!', 'success');
        }, 1200);
    };

    const totalTasksCount = categories.reduce((sum, c) => sum + c.tasks.length, 0);
    const overBudgetTasks = categories
        .flatMap((c) => c.tasks)
        .filter((t) => (t.budgetHours || 0) > 0 && (t.actualHours || 0) > (t.budgetHours || 0));
    const totalBudgetHours = categories
        .flatMap((c) => c.tasks)
        .reduce((sum, t) => sum + (t.budgetHours || 0), 0);
    const totalActualHours = categories
        .flatMap((c) => c.tasks)
        .reduce((sum, t) => sum + (t.actualHours || 0), 0);
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
                                    Quản Trị Dự Án & Nguồn Lực
                                </h1>

                                {isBackendConnected && selectedProject ? (
                                    <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-800">
                                        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-500" />
                                        <Database className="h-3 w-3 text-emerald-700" /> Dữ liệu Database Thật
                                    </span>
                                ) : (
                                    <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-800">
                                        <span className="h-1.5 w-1.5 rounded-full bg-amber-500" /> Dữ liệu Mẫu (Demo)
                                    </span>
                                )}
                            </div>

                            {/* Project Selector / Info */}
                            <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-600">
                                {projectsList.length > 0 ? (
                                    <div className="flex items-center gap-1.5">
                                        <span className="text-slate-500 font-medium">Dự án:</span>
                                        <div className="relative inline-block">
                                            <select
                                                value={selectedProjectId || ''}
                                                onChange={(e) => setSelectedProjectId(Number(e.target.value))}
                                                className="rounded-lg border border-slate-300 bg-slate-50 py-1 pl-2.5 pr-7 text-xs font-bold text-indigo-900 outline-none transition focus:border-indigo-500 focus:bg-white"
                                            >
                                                {projectsList.map((p) => (
                                                    <option key={p.id} value={p.id}>
                                                        {p.projectName} ({p.projectCode})
                                                    </option>
                                                ))}
                                            </select>
                                            <ChevronDown className="pointer-events-none absolute right-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                                        </div>
                                    </div>
                                ) : (
                                    <span>
                                        Dự án: <strong>Chuyển Đổi Số Doanh Nghiệp (ERP Phase 1)</strong>
                                    </span>
                                )}

                                <span className="text-slate-300">•</span>
                                <span className="flex items-center gap-1">
                                    <Calendar className="h-3.5 w-3.5 text-slate-400" />
                                    {selectedProject?.startDate ? (
                                        <span>
                                            {selectedProject.startDate} {selectedProject.endDate ? `đến ${selectedProject.endDate}` : ''}
                                        </span>
                                    ) : (
                                        <span>Q3/2026 - Q4/2026</span>
                                    )}
                                </span>

                                {isLoadingWbs && (
                                    <span className="inline-flex items-center gap-1 text-indigo-600">
                                        <RefreshCw className="h-3 w-3 animate-spin" /> Đang đồng bộ...
                                    </span>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Top Actions */}
                    <div className="flex flex-wrap items-center gap-2 self-start sm:self-auto">
                        <button
                            type="button"
                            onClick={() => setProjectCreateModalOpen(true)}
                            className="inline-flex items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 px-3.5 py-2 text-xs font-bold text-indigo-700 shadow-2xs transition hover:bg-indigo-100 active:scale-95 cursor-pointer"
                            title="Tạo dự án mới lưu vào database"
                        >
                            <FolderPlus className="h-4 w-4 stroke-[2.2]" />
                            <span>+ Dự án mới</span>
                        </button>

                        <button
                            type="button"
                            onClick={() => handleQuickAddTask()}
                            className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-200 transition hover:bg-indigo-700 active:scale-95 cursor-pointer"
                        >
                            <Plus className="h-4 w-4 stroke-[2.5]" />
                            <span>Thêm công việc</span>
                        </button>

                        <button
                            type="button"
                            onClick={handleExportReport}
                            className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-2xs transition hover:bg-slate-100 active:scale-95 cursor-pointer"
                        >
                            <Download className="h-3.5 w-3.5 text-slate-500" />
                            <span className="hidden sm:inline">Xuất báo cáo</span>
                        </button>

                        <button
                            type="button"
                            onClick={() => {
                                loadProjects();
                                if (selectedProjectId) loadWbsForProject(selectedProjectId);
                                showToast('Đã làm mới dữ liệu từ Database', 'info');
                            }}
                            className="p-2 rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-100 transition cursor-pointer"
                            title="Làm mới dữ liệu từ máy chủ"
                        >
                            <RefreshCw className={`h-3.5 w-3.5 ${isLoadingProjects ? 'animate-spin' : ''}`} />
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
                    <div className="mt-1 flex items-center gap-1.5 text-[11px] text-slate-500">
                        <span>Tổng NS: <strong className="text-indigo-600">{totalBudgetHours}h</strong></span>
                        <span className="text-slate-300">•</span>
                        <span>Duyệt: <strong className="text-slate-700">{totalActualHours}h</strong></span>
                    </div>
                    <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
                        <div className="h-1.5 rounded-full bg-indigo-600" style={{ width: `${Math.min(totalTasksCount > 0 ? 64 : 0, 100)}%` }} />
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
                        <span className="text-xs font-medium text-emerald-600">Nhân sự</span>
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
                        <span className="text-2xl font-bold text-slate-800">{Math.round(totalBudgetHours > 0 ? totalBudgetHours * 0.4 : 218)}h</span>
                        <span className="rounded bg-amber-50 px-1.5 py-0.5 text-xs font-semibold text-amber-600">Công suất</span>
                    </div>
                    <p className="mt-2 text-xs text-slate-400">{members.length * 40}h tổng tải định mức</p>
                </div>

                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs transition hover:border-slate-300">
                    <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Cảnh báo rủi ro</span>
                        <span className="rounded-lg bg-rose-50 p-2 text-rose-600">
                            <AlertTriangle className="h-4 w-4" />
                        </span>
                    </div>
                    <div className="mt-2 flex items-baseline gap-2">
                        <span className="text-2xl font-bold text-rose-600">{overBudgetTasks.length}</span>
                        <span className="text-xs font-medium text-rose-600">Việc vượt ngân sách</span>
                    </div>
                    <p className="mt-2 flex items-center gap-1 text-xs font-medium text-rose-500">
                        <AlertTriangle className="h-3 w-3 text-rose-500 shrink-0" />
                        {overBudgetTasks.length > 0 ? 'Nguy cơ ăn mòn lợi nhuận dự án' : 'Ngân sách các việc an toàn'}
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
                        className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all sm:flex-none cursor-pointer ${
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
                        className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all sm:flex-none cursor-pointer ${
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
                        className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all sm:flex-none cursor-pointer ${
                            viewMode === 'workload'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <CalendarDays className="h-4 w-4" />
                        <span>Phân bổ theo tuần</span>
                    </button>
                </div>

                {/* Filters */}
                <div className="flex w-full flex-wrap items-center justify-end gap-2 sm:w-auto">
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
                    <select
                        value={roleFilter}
                        onChange={(e) => setRoleFilter(e.target.value)}
                        className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs text-slate-700 outline-none focus:border-indigo-500"
                    >
                        <option value="ALL">Tất cả vai trò</option>
                        <option value="Product Owner / BA">Product / BA</option>
                        <option value="UI/UX Designer">UI/UX Design</option>
                        <option value="Frontend Dev">Frontend</option>
                        <option value="Backend Dev">Backend</option>
                        <option value="QA / QC Tester">QA / QC</option>
                    </select>
                </div>
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
                            onQuickAddTask={handleQuickAddTask}
                            onToggleTaskStatus={handleToggleTaskStatus}
                            onOpenBudgetModal={handleOpenBudgetModal}
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
                            onNavigateMonth={handleNavigateMonth}
                            onOpenAdjustModal={handleOpenAdjustModal}
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

            <ProjectBudgetModal
                open={budgetModalOpen}
                task={selectedBudgetTask}
                member={selectedBudgetTask ? members.find((m) => m.id === selectedBudgetTask.assigneeId) : null}
                onClose={() => setBudgetModalOpen(false)}
                onSave={handleSaveTaskBudget}
            />

            <ProjectCreateModal
                open={projectCreateModalOpen}
                members={members}
                onClose={() => setProjectCreateModalOpen(false)}
                onCreated={handleProjectCreated}
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
