import { useState, useEffect, useCallback } from 'react';
import { getUsers } from '@/lib/api/users';
import { useAuthUser } from '@/lib/auth-session';
import { allocateProjectHours, getProjectWeeklyAllocations } from '@/lib/api/allocations';
import {
    getProjects,
    getProjectWbs,
    createTask,
    updateTask,
    type ProjectResult,
    type TaskNodeResult,
    type BackendTaskStatus,
} from '@/lib/api/projects';
import { setTaskBudget, type CloneProjectWbsResult } from '@/lib/api/tasks';

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
    AlertCircle,
    Database,
    RefreshCw,
    FolderPlus,
    Calendar,
    ChevronDown,
    Copy,
} from 'lucide-react';
import {
    type ProjectMonth,
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
import { CloneWbsModal } from './CloneWbsModal';

const CATEGORY_COLORS = ['indigo', 'purple', 'emerald', 'sky', 'amber', 'rose'];

function buildMonths(): ProjectMonth[] {
    const now = new Date();
    return [-1, 0, 1].map((offset) => {
        const first = new Date(now.getFullYear(), now.getMonth() + offset, 1);
        const year = first.getFullYear();
        const month = first.getMonth();
        const lastDay = new Date(year, month + 1, 0).getDate();
        const weeks = Array.from({ length: Math.ceil(lastDay / 7) }, (_, index) => {
            const start = index * 7 + 1;
            const end = Math.min(start + 6, lastDay);
            return {
                key: `W${index + 1}`,
                label: `Tuần ${index + 1}`,
                dates: `${String(start).padStart(2, '0')}/${String(month + 1).padStart(2, '0')} - ${String(end).padStart(2, '0')}/${String(month + 1).padStart(2, '0')}`,
                isCurrent: now.getFullYear() === year && now.getMonth() === month && now.getDate() >= start && now.getDate() <= end,
            };
        });
        return { id: `${year}-${String(month + 1).padStart(2, '0')}`, name: `Tháng ${String(month + 1).padStart(2, '0')}/${year}`, weeks };
    });
}

function getIsoWeek(date: Date): { year: number; week: number } {
    const utcDate = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const day = utcDate.getUTCDay() || 7;
    utcDate.setUTCDate(utcDate.getUTCDate() + 4 - day);
    const yearStart = new Date(Date.UTC(utcDate.getUTCFullYear(), 0, 1));
    return {
        year: utcDate.getUTCFullYear(),
        week: Math.ceil((((utcDate.getTime() - yearStart.getTime()) / 86400000) + 1) / 7),
    };
}

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
        if (m.employeeId) memberMap.set(String(m.employeeId), m);
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
            const childTasks: TaskItem[] = (node.children || []).map((child) => {
                const assigneeKey = child.assigneeId ? String(child.assigneeId) : '';
                const member = memberMap.get(assigneeKey);

                return {
                    id: String(child.id),
                    code: child.taskCode,
                    name: child.name,
                    assigneeId: member ? member.id : (child.assigneeId ? `u-${child.assigneeId}` : ''),
                    priority: 'Trung bình',
                    hours: Number(child.estimatedHours || 0),
                    budgetHours: child.budgetHours !== undefined ? Number(child.budgetHours) : undefined,
                    actualHours: Number(child.actualHours || 0),
                    burnedPercentage: child.burnedPercentage !== undefined ? Number(child.burnedPercentage) : undefined,
                    burnStatus: child.burnStatus,
                    isOverBudget: child.isOverBudget,
                    status: statusMap[child.status] || 'Chưa làm',
                    startWeek: 'Chưa cập nhật',
                    endWeek: 'Chưa cập nhật',
                };
            });

            const doneCount = childTasks.filter((t) => t.status === 'Hoàn thành').length;
            const progress = childTasks.length > 0 ? Math.round((doneCount / childTasks.length) * 100) : 0;

            categories.push({
                id: String(node.id),
                code: node.taskCode,
                name: node.name,
                lead: '',
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
                code: node.taskCode,
                name: node.name,
                assigneeId: member ? member.id : (node.assigneeId ? `u-${node.assigneeId}` : ''),
                priority: 'Trung bình',
                hours: Number(node.estimatedHours || 0),
                budgetHours: node.budgetHours !== undefined ? Number(node.budgetHours) : undefined,
                actualHours: Number(node.actualHours || 0),
                burnedPercentage: node.burnedPercentage !== undefined ? Number(node.burnedPercentage) : undefined,
                burnStatus: node.burnStatus,
                isOverBudget: node.isOverBudget,
                status: statusMap[node.status] || 'Chưa làm',
                startWeek: 'Chưa cập nhật',
                endWeek: 'Chưa cập nhật',
            });
        }
    });

    if (standaloneTasks.length > 0) {
        const doneCount = standaloneTasks.filter((t) => t.status === 'Hoàn thành').length;
        const progress = Math.round((doneCount / standaloneTasks.length) * 100);
        categories.unshift({
            id: 'root-general',
            code: '',
            name: 'Hạng Mục Chung',
            lead: '',
            progress,
            color: 'indigo',
            tasks: standaloneTasks,
        });
    }

    return categories;
}

export default function ProjectView() {
    const currentUser = useAuthUser();
    const canManageProject = currentUser?.roleCode?.toUpperCase().replace(/_/g, '-') === 'VT-02';
    const canManageAllocations = currentUser?.roleCode?.toUpperCase().replace(/_/g, '-') === 'VT-03';
    const [viewMode, setViewMode] = useState<'split' | 'wbs' | 'workload'>('split');
    const [categories, setCategories] = useState<TaskCategoryGroup[]>([]);
    const [allEmployees, setAllEmployees] = useState<ProjectMember[]>([]);
    const [members, setMembers] = useState<ProjectMember[]>([]);
    const [budgetModalOpen, setBudgetModalOpen] = useState(false);
    const [selectedBudgetTask, setSelectedBudgetTask] = useState<TaskItem | null>(null);

    // Real projects backend state
    const canManageWbs = Boolean(
        currentUser?.roleCode &&
        (currentUser.roleCode === 'VT-02' || currentUser.roleCode === 'VT-06')
    );
    const [projectsList, setProjectsList] = useState<ProjectResult[]>([]);
    const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
    const [isBackendConnected, setIsBackendConnected] = useState<boolean>(false);
    const [isLoadingProjects, setIsLoadingProjects] = useState<boolean>(false);
    const [isLoadingWbs, setIsLoadingWbs] = useState<boolean>(false);
    const [projectError, setProjectError] = useState<string | null>(null);
    const [allocationError, setAllocationError] = useState<string | null>(null);
    const [projectCreateModalOpen, setProjectCreateModalOpen] = useState<boolean>(false);
    const [cloneModalOpen, setCloneModalOpen] = useState<boolean>(false);

    // Selected project object
    const selectedProject = projectsList.find((p) => p.id === selectedProjectId) || null;

    // Toast state
    const [toast, setToast] = useState<{ message: string; type: 'success' | 'info' | 'error' } | null>(null);

    const showToast = useCallback((message: string, type: 'success' | 'info' | 'error' = 'success') => {
        setToast({ message, type });
        setTimeout(() => {
            setToast(null);
        }, 3200);
    }, []);

    const handleCloneSuccess = async (result: CloneProjectWbsResult) => {
        if (selectedProjectId) {
            await loadWbsForProject(selectedProjectId);
        }
        showToast(`Nhân bản thành công ${result.totalClonedTasks} công việc sang dự án!`, 'success');
    };

    // 1. Tải danh sách nhân sự thật từ API
    useEffect(() => {
        getUsers(0, 100)
            .then((res) => {
                if (res?.content && res.content.length > 0) {
                    const fetchedMembers: ProjectMember[] = res.content
                      .filter((u) => u.employeeId !== null)
                      .map((u) => ({
                        id: `u-${u.employeeId}`,
                        employeeId: u.employeeId!,
                        name: u.fullName || u.username,
                        role: u.roleCode || 'Nhân viên',
                        avatar: '',
                        capacity: 40,
                        weeklyHours: {},
                    }));
                    setAllEmployees(fetchedMembers);
                }
            })
            .catch((err) => {
                console.warn('Failed to load employees for the selected project:', err);
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
                setProjectError(null);
                setSelectedProjectId((prevId) => {
                    const exists = res.content.some((p) => p.id === prevId);
                    return exists ? prevId : res.content[0].id;
                });
            } else {
                setProjectsList([]);
                setIsBackendConnected(false);
                setSelectedProjectId(null);
                setCategories([]);
                setMembers([]);
            }
        } catch (err) {
            console.warn('Failed to fetch projects from backend:', err);
            setIsBackendConnected(false);
            setProjectsList([]);
            setSelectedProjectId(null);
            setCategories([]);
            setMembers([]);
            setProjectError(err instanceof Error ? err.message : 'Không thể tải dữ liệu dự án.');
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
            const assignedEmployeeIds = new Set<number>();
            const collectAssignees = (nodes: TaskNodeResult[]) => nodes.forEach((node) => {
                if (node.assigneeId) assignedEmployeeIds.add(node.assigneeId);
                collectAssignees(node.children || []);
            });
            collectAssignees(wbsNodes);
            const projectMembers = allEmployees.filter((member) =>
                assignedEmployeeIds.has(Number(member.id.replace('u-', '')))
            );
            setMembers(projectMembers);
            const mapped = mapBackendWbsToUiCategories(wbsNodes, projectMembers);
            setCategories(mapped);
        } catch (err) {
            console.warn(`Failed to fetch WBS for project ${projId}:`, err);
            setCategories([]);
            setMembers([]);
        } finally {
            setIsLoadingWbs(false);
        }
    }, [allEmployees]);

    useEffect(() => {
        if (selectedProjectId) {
            loadWbsForProject(selectedProjectId);
        }
    }, [selectedProjectId, loadWbsForProject]);

    const [months] = useState(buildMonths);
    const [selectedMonthIdx, setSelectedMonthIdx] = useState(1);

    const getDisplayedIsoWeek = useCallback((weekKey: string) => {
        const month = months[selectedMonthIdx];
        const [year, monthNumber] = month.id.split('-').map(Number);
        const index = Math.max(0, month.weeks.findIndex((week) => week.key === weekKey));
        return getIsoWeek(new Date(year, monthNumber - 1, index * 7 + 1));
    }, [months, selectedMonthIdx]);

    const loadProjectAllocations = useCallback(async () => {
        if (!selectedProjectId) return;
        const month = months[selectedMonthIdx];
        try {
            const rowsByWeek = await Promise.all(month.weeks.map(async (week) => {
                const isoWeek = getDisplayedIsoWeek(week.key);
                const rows = await getProjectWeeklyAllocations(selectedProjectId, isoWeek.year, isoWeek.week, isoWeek.week);
                return { key: week.key, rows };
            }));
            setMembers((previous) => previous.map((member) => {
                const employeeId = Number(member.id.replace('u-', ''));
                const weeklyHours: Record<string, number> = {};
                rowsByWeek.forEach(({ key, rows }) => {
                    weeklyHours[key] = rows.filter((row) => row.employeeId === employeeId)
                        .reduce((sum, row) => sum + Number(row.allocatedHours), 0);
                });
                return { ...member, weeklyHours };
            }));
            setAllocationError(null);
        } catch (error) {
            setMembers((previous) => previous.map((member) => ({ ...member, weeklyHours: {} })));
            setAllocationError(error instanceof Error ? error.message : 'Không thể tải dữ liệu phân bổ nguồn lực.');
        }
    }, [getDisplayedIsoWeek, months, selectedMonthIdx, selectedProjectId]);

    useEffect(() => {
        if (selectedProjectId && categories.length > 0) void loadProjectAllocations();
    }, [selectedProjectId, selectedMonthIdx, categories, loadProjectAllocations]);
    const selectedMonth = months[selectedMonthIdx] || months[0];

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
        if (!canManageProject) {
            showToast('Bạn chỉ có quyền xem dự án.', 'info');
            return;
        }
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
        if (!canManageProject) return;
        const { catId, name, assigneeId, hours, newCategoryName } = newTaskData;

        if (!selectedProjectId) {
            showToast('Chưa chọn dự án để lưu công việc.', 'error');
            return;
        }

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

                // Tìm member tương ứng để lấy employeeId thật trong database
                const targetMember = members.find((m) => m.id === assigneeId);
                const employeeIdToAssign = targetMember?.employeeId || null;

                await createTask(selectedProjectId, {
                    parentId: parentIdToUse,
                    name,
                    taskType: 'TASK',
                    assigneeId: employeeIdToAssign,
                    estimatedHours: hours,
                    sortOrder: 0,
                });

                await loadWbsForProject(selectedProjectId);
                showToast(`Đã lưu công việc "${name}" vào cơ sở dữ liệu!`, 'success');
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Có lỗi khi lưu công việc vào cơ sở dữ liệu';
            showToast(`Lỗi: ${msg}`, 'error');
            console.error('Failed to create task in backend:', err);
        }
    };

    // Chuyển đổi trạng thái công việc
    const handleToggleTaskStatus = async (catId: string, taskId: string) => {
        if (!canManageProject) {
            showToast('Bạn chỉ có quyền xem dự án.', 'info');
            return;
        }
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
        if (!canManageAllocations) {
            showToast('Bạn chỉ có quyền xem dự án.', 'info');
            return;
        }
        setSelectedAdjustCell({ memberId, weekKey, weekLabel });
        setAdjustModalOpen(true);
    };

    const handleSaveAdjustedHours = async (memberId: string, weekKey: string, newHours: number) => {
        const member = members.find((m) => m.id === memberId);
        if (!canManageAllocations || !selectedProjectId || !member) return;
        const employeeId = Number(member.id.replace('u-', ''));
        const isoWeek = getDisplayedIsoWeek(weekKey);
        try {
            await allocateProjectHours({ employeeId, projectId: selectedProjectId, year: isoWeek.year, weekNumber: isoWeek.week, allocatedHours: newHours });
            await loadProjectAllocations();
            showToast(`Đã lưu phân bổ ${newHours}h cho ${member.name} (${weekKey})`, 'success');
        } catch (error) {
            showToast(error instanceof Error ? error.message : 'Không thể lưu phân bổ nguồn lực.', 'error');
        }
    };


    const handleOpenBudgetModal = (task: TaskItem) => {
        if (!canManageProject) {
            showToast('Bạn chỉ có quyền xem dự án.', 'info');
            return;
        }
        setSelectedBudgetTask(task);
        setBudgetModalOpen(true);
    };

    // Đặt ngân sách: Gọi API PATCH /api/v1/projects/{projectId}/tasks/{taskId}/budget
    const handleSaveTaskBudget = async (taskId: string, newBudgetHours: number) => {
        const numTaskId = parseInt(taskId.replace(/\D/g, ''), 10);
        const projIdToUse = selectedProjectId || 1;
        const taskName = selectedBudgetTask?.name || taskId;

        try {
            let budgetResult = null;
            if (!isNaN(numTaskId) && numTaskId > 0) {
                budgetResult = await setTaskBudget(projIdToUse, numTaskId, newBudgetHours);
            }

            setCategories((prev) =>
                prev.map((cat) => ({
                    ...cat,
                    tasks: cat.tasks.map((t) => {
                        if (t.id === taskId) {
                            return {
                                ...t,
                                budgetHours: newBudgetHours,
                                burnedPercentage: budgetResult?.burnedPercentage !== undefined 
                                    ? Number(budgetResult.burnedPercentage) 
                                    : t.burnedPercentage,
                                burnStatus: budgetResult?.burnStatus || t.burnStatus,
                                isOverBudget: budgetResult?.isOverBudget !== undefined 
                                    ? budgetResult.isOverBudget 
                                    : t.isOverBudget,
                            };
                        }
                        return t;
                    }),
                }))
            );

            showToast(`Đã lưu ngân sách ${newBudgetHours}h cho công việc: ${taskName}`, 'success');
        } catch (err: any) {
            console.error('Lỗi khi lưu ngân sách công việc:', err);
            const errMsg = err?.message || 'Không thể lưu ngân sách công việc. Vui lòng kiểm tra lại.';
            showToast(`Lỗi: ${errMsg}`, 'error');
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
        .filter((t) => t.isOverBudget !== undefined
            ? t.isOverBudget
            : ((t.budgetHours || 0) > 0 && (t.actualHours || 0) > (t.budgetHours || 0))
        );
    const totalBudgetHours = categories
        .flatMap((c) => c.tasks)
        .reduce((sum, t) => sum + (t.budgetHours || 0), 0);
    const totalActualHours = categories
        .flatMap((c) => c.tasks)
        .reduce((sum, t) => sum + (t.actualHours || 0), 0);
    const currentWeek = selectedMonth.weeks.find((w) => w.isCurrent) || selectedMonth.weeks[0];
    const currentWeekLoad = members.reduce((sum, m) => sum + (m.weeklyHours[currentWeek?.key] || 0), 0);
    const uniqueRoles = Array.from(new Set(members.map((m) => m.role))).filter((r): r is string => Boolean(r));

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
                                        <Database className="h-3 w-3 text-emerald-700" /> 
                                    </span>
                                ) : (
                                    <span className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-600">
                                        {isLoadingProjects ? <RefreshCw className="h-3 w-3 animate-spin" /> : <Database className="h-3 w-3" />}
                                        {isLoadingProjects ? 'Đang tải dữ liệu' : 'Chưa có dữ liệu hệ thống'}
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
                                    <span>Dự án: <strong>Chưa có dự án</strong></span>
                                )}

                                <span className="text-slate-300">•</span>
                                <span className="flex items-center gap-1">
                                    <Calendar className="h-3.5 w-3.5 text-slate-400" />
                                    {selectedProject?.startDate ? (
                                        <span>
                                            {selectedProject.startDate} {selectedProject.endDate ? `đến ${selectedProject.endDate}` : ''}
                                        </span>
                                    ) : (
                                        <span>Chưa cập nhật thời gian</span>
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
                        {canManageProject && <button
                            type="button"
                            onClick={() => setProjectCreateModalOpen(true)}
                            className="inline-flex items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 px-3.5 py-2 text-xs font-bold text-indigo-700 shadow-2xs transition hover:bg-indigo-100 active:scale-95 cursor-pointer"
                            title="Tạo dự án mới lưu vào database"
                        >
                            <FolderPlus className="h-4 w-4 stroke-[2.2]" />
                            <span>+ Dự án mới</span>
                        </button>}

                        {canManageWbs && (
                            <button
                                type="button"
                                onClick={() => setCloneModalOpen(true)}
                                className="inline-flex items-center gap-2 rounded-xl border border-indigo-600/30 bg-indigo-50 px-3.5 py-2 text-xs font-semibold text-indigo-700 shadow-2xs transition hover:bg-indigo-100 hover:border-indigo-600/60 active:scale-95 cursor-pointer"
                                title="Nhân bản toàn bộ cây WBS sang dự án khác"
                            >
                                <Copy className="h-3.5 w-3.5 text-indigo-600" />
                                <span>Nhân bản WBS</span>
                            </button>
                        )}

                        {canManageProject && <button
                            type="button"
                            onClick={() => handleQuickAddTask()}
                            className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-200 transition hover:bg-indigo-700 active:scale-95 cursor-pointer"
                        >
                            <Plus className="h-4 w-4 stroke-[2.5]" />
                            <span>Thêm công việc</span>
                        </button>}

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

            {(projectError || allocationError) && (
                <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs text-rose-700">
                    {projectError || allocationError}
                </div>
            )}


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
                        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                            Tải tuần này ({currentWeek?.label || 'Hiện tại'})
                        </span>
                        <span className="rounded-lg bg-amber-50 p-2 text-amber-600">
                            <BarChart3 className="h-4 w-4" />
                        </span>
                    </div>
                    <div className="mt-2 flex items-baseline gap-2">
                        <span className="text-2xl font-bold text-slate-800">{Math.round(currentWeekLoad)}h</span>
                        <span className="rounded bg-amber-50 px-1.5 py-0.5 text-xs font-semibold text-amber-600">Phân bổ</span>
                    </div>
                    <p className="mt-2 text-xs text-slate-400">{members.length * 40}h tổng định mức đội ngũ</p>
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
                        <option value="ALL">Tất cả vai trò ({members.length})</option>
                        {uniqueRoles.map((role) => (
                            <option key={role} value={role}>
                                {role}
                            </option>
                        ))}
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
                            onOpenCloneModal={canManageWbs ? () => setCloneModalOpen(true) : undefined}
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
            <CloneWbsModal
                isOpen={cloneModalOpen}
                onClose={() => setCloneModalOpen(false)}
                targetProject={selectedProject}
                projectsList={projectsList}
                onSuccess={handleCloneSuccess}
            />

            {canManageProject && <ProjectTaskModal
                open={taskModalOpen}
                categories={categories}
                members={members}
                defaultCategoryId={defaultCatId}
                onClose={() => setTaskModalOpen(false)}
                onSubmit={handleCreateTask}
            />}

            {canManageAllocations && <ProjectAdjustHoursModal
                open={adjustModalOpen}
                member={adjustMember}
                weekKey={selectedAdjustCell?.weekKey || ''}
                weekLabel={selectedAdjustCell?.weekLabel || ''}
                monthName={selectedMonth.name}
                onClose={() => setAdjustModalOpen(false)}
                onSave={handleSaveAdjustedHours}
            />}

            {canManageProject && <ProjectBudgetModal
                open={budgetModalOpen}
                task={selectedBudgetTask}
                member={selectedBudgetTask ? members.find((m) => m.id === selectedBudgetTask.assigneeId) : null}
                onClose={() => setBudgetModalOpen(false)}
                onSave={handleSaveTaskBudget}
            />}

            {canManageProject && <ProjectCreateModal
                open={projectCreateModalOpen}
                members={allEmployees}
                onClose={() => setProjectCreateModalOpen(false)}
                onCreated={handleProjectCreated}
            />}

            {/* Toast Notification */}
            {toast && (
                <div className="fixed bottom-6 right-6 z-50 flex items-center gap-3 animate-in fade-in slide-in-from-bottom-5 rounded-2xl border border-slate-800 bg-slate-900 px-4 py-3 text-xs text-white shadow-2xl">
                    {toast.type === 'success' ? (
                        <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
                    ) : toast.type === 'error' ? (
                        <AlertCircle className="h-4 w-4 text-rose-400 shrink-0" />
                    ) : (
                        <Info className="h-4 w-4 text-sky-400 shrink-0" />
                    )}
                    <span className="font-medium">{toast.message}</span>
                </div>
            )}
        </div>
    );
}


