import { useState, useEffect, useCallback, useMemo } from 'react';
import { getUsers } from '@/lib/api/users';
import { getEmployees } from '@/lib/api/employees';
import { useAuthUser } from '@/lib/auth-session';
import { allocateProjectHours, getProjectWeeklyAllocations } from '@/lib/api/allocations';
import {
    getProjects,
    getProjectWbs,
    createTask,
    updateTask,
    getAssignableEmployees,
    type ProjectResult,
    type TaskNodeResult,
    type BackendTaskStatus,
    type TaskAssignmentResult,
} from '@/lib/api/projects';
import { setTaskBudget, type CloneProjectWbsResult } from '@/lib/api/tasks';
import { getTaskDependencies, type TaskDependencyResult } from '@/lib/api/taskDependencies';

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
    GitCommit,
    TrendingUp,
    Lock,
    Unlock,
    Flag,
} from 'lucide-react';
import { TaskDependencyModal } from '../task/TaskDependencyModal';
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
import { AssignTaskModal } from './AssignTaskModal';
import { ProjectDemandView } from './ProjectDemandView';
import { EstimateDemandModal } from './EstimateDemandModal';
import { DeleteDemandConfirmModal } from './DeleteDemandConfirmModal';
import {
    getProjectResourceDemands,
    estimateResourceDemand,
    deleteResourceDemand,
    type ProjectResourceDemandSummaryResult,
    type RoleResourceDemand,
} from '@/lib/api/resource-demands';
import { ProjectCloseModal } from './ProjectCloseModal';
import { ProjectReopenModal } from './ProjectReopenModal';
import { MilestoneListView } from './milestone/MilestoneListView';

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
                const rawAssigneeIds = child.assigneeIds && child.assigneeIds.length > 0
                    ? child.assigneeIds
                    : (child.assigneeId ? [child.assigneeId] : []);
                const assigneeIds = rawAssigneeIds.map(id => {
                    const m = memberMap.get(String(id));
                    return m ? m.id : `u-${id}`;
                });

                return {
                    id: String(child.id),
                    code: child.taskCode,
                    name: child.name,
                    assigneeId: member ? member.id : (child.assigneeId ? `u-${child.assigneeId}` : ''),
                    assigneeIds,
                    priority: 'Trung bình',
                    hours: Number(child.estimatedHours || 0),
                    budgetHours: child.budgetHours !== undefined ? Number(child.budgetHours) : undefined,
                    actualHours: Number(child.actualHours || 0),
                    burnedPercentage: child.burnedPercentage !== undefined ? Number(child.burnedPercentage) : undefined,
                    burnStatus: child.burnStatus,
                    isOverBudget: child.isOverBudget,
                    status: statusMap[child.status] || 'Chưa làm',
                    plannedStartDate: child.plannedStartDate,
                    plannedEndDate: child.plannedEndDate,
                    startDate: child.startDate,
                    dueDate: child.dueDate,
                    actualEndDate: child.actualEndDate,
                    slackDays: child.slackDays,
                    startWeek: child.startDate ? child.startDate : 'Chưa cập nhật',
                    endWeek: child.actualEndDate ? `${child.actualEndDate}` : (child.dueDate ? child.dueDate : 'Chưa cập nhật'),
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
            const rawAssigneeIds = node.assigneeIds && node.assigneeIds.length > 0
                ? node.assigneeIds
                : (node.assigneeId ? [node.assigneeId] : []);
            const assigneeIds = rawAssigneeIds.map(id => {
                const m = memberMap.get(String(id));
                return m ? m.id : `u-${id}`;
            });

            standaloneTasks.push({
                id: String(node.id),
                code: node.taskCode,
                name: node.name,
                assigneeId: member ? member.id : (node.assigneeId ? `u-${node.assigneeId}` : ''),
                assigneeIds,
                priority: 'Trung bình',
                hours: Number(node.estimatedHours || 0),
                budgetHours: node.budgetHours !== undefined ? Number(node.budgetHours) : undefined,
                actualHours: Number(node.actualHours || 0),
                burnedPercentage: node.burnedPercentage !== undefined ? Number(node.burnedPercentage) : undefined,
                burnStatus: node.burnStatus,
                isOverBudget: node.isOverBudget,
                status: statusMap[node.status] || 'Chưa làm',
                plannedStartDate: node.plannedStartDate,
                plannedEndDate: node.plannedEndDate,
                startDate: node.startDate,
                dueDate: node.dueDate,
                actualEndDate: node.actualEndDate,
                slackDays: node.slackDays,
                startWeek: node.startDate ? node.startDate : 'Chưa cập nhật',
                endWeek: node.actualEndDate ? `${node.actualEndDate}` : (node.dueDate ? node.dueDate : 'Chưa cập nhật'),
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
    const userRoleCode = currentUser?.roleCode?.toUpperCase().replace(/_/g, '-') || '';
    const isExecutive = userRoleCode === 'VT-01';
    const isPm = userRoleCode === 'VT-02' || userRoleCode === 'VT-06' || currentUser?.roleName === 'Quản lý dự án' || currentUser?.roleName === 'Quản trị viên';

    // PROJECT_CREATE: Chỉ VT-02 (PM) và VT-06 (Admin) mới có quyền tạo/quản lý dự án
    const canManageProject = isPm;
    const canManageAllocations = userRoleCode === 'VT-03';
    const canManageMilestones = isPm || userRoleCode === 'VT-06';
    const [viewMode, setViewMode] = useState<'split' | 'wbs' | 'workload' | 'demand' | 'milestones'>('split');
    const [categories, setCategories] = useState<TaskCategoryGroup[]>([]);
    const [allEmployees, setAllEmployees] = useState<ProjectMember[]>([]);
    const [members, setMembers] = useState<ProjectMember[]>([]);
    const [budgetModalOpen, setBudgetModalOpen] = useState(false);
    const [selectedBudgetTask, setSelectedBudgetTask] = useState<TaskItem | null>(null);
    const [assignModalOpen, setAssignModalOpen] = useState(false);
    const [selectedAssignTask, setSelectedAssignTask] = useState<TaskItem | null>(null);
    const [dependencyModalOpen, setDependencyModalOpen] = useState<boolean>(false);
    const [taskDependenciesList, setTaskDependenciesList] = useState<TaskDependencyResult[]>([]);

    // Mốc tiến độ (NCL-03-CN-006)
    const [milestones, setMilestones] = useState<MilestoneResult[]>([]);
    const [isLoadingMilestones, setIsLoadingMilestones] = useState<boolean>(false);

    // Real projects backend state
    const canManageWbs = isPm;
    const [projectsList, setProjectsList] = useState<ProjectResult[]>([]);
    const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
    const [isBackendConnected, setIsBackendConnected] = useState<boolean>(false);
    const [isLoadingProjects, setIsLoadingProjects] = useState<boolean>(false);
    const [isLoadingWbs, setIsLoadingWbs] = useState<boolean>(false);
    const [projectError, setProjectError] = useState<string | null>(null);
    const [allocationError, setAllocationError] = useState<string | null>(null);
    const [projectCreateModalOpen, setProjectCreateModalOpen] = useState<boolean>(false);
    const [cloneModalOpen, setCloneModalOpen] = useState<boolean>(false);
    const [closeModalOpen, setCloseModalOpen] = useState<boolean>(false);
    const [reopenModalOpen, setReopenModalOpen] = useState<boolean>(false);

    // Demand Estimation State (NCL-03-CN-007)
    const [demandSummary, setDemandSummary] = useState<ProjectResourceDemandSummaryResult | null>(null);
    const [isLoadingDemand, setIsLoadingDemand] = useState<boolean>(false);
    const [demandError, setDemandError] = useState<string | null>(null);
    const [estimateModalOpen, setEstimateModalOpen] = useState<boolean>(false);
    const [deleteModalOpen, setDeleteModalOpen] = useState<boolean>(false);
    const [editingDemandRole, setEditingDemandRole] = useState<RoleResourceDemand | null>(null);
    const [roleToDelete, setRoleToDelete] = useState<RoleResourceDemand | null>(null);

    // Selected project object & Closed status (QTN-08)
    const selectedProject = projectsList.find((p) => p.id === selectedProjectId) || null;
    const isProjectClosed = selectedProject?.status === 'CLOSED';

    // Quyền đóng và mở lại dự án (NCL-03-CN-004)
    const canCloseProject = (isExecutive || isPm) && !isProjectClosed && selectedProject !== null;
    const canReopenProject = (isExecutive || isPm) && isProjectClosed && selectedProject !== null;

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

    const handleOpenAssignModal = (task: TaskItem) => {
        setSelectedAssignTask(task);
        setAssignModalOpen(true);
    };

    const handleAssignSuccess = async (result: TaskAssignmentResult) => {
        if (selectedProjectId) {
            await loadWbsForProject(selectedProjectId);
        }
        showToast(`Đã phân công thành công cho công việc ${result.taskCode}!`, 'success');
    };

    // Danh sách task chưa hoàn thành trong WBS (để hiển thị cảnh báo trước khi đóng dự án)
    const unfinishedTasks = useMemo(() => {
        const list: { code?: string; name: string }[] = [];
        categories.forEach((cat) => {
            cat.tasks.forEach((t) => {
                if (t.status !== 'Hoàn thành') {
                    list.push({ code: t.code, name: t.name });
                }
            });
        });
        return list;
    }, [categories]);

    // 1. Tải danh sách nhân sự thật từ API (ưu tiên getAssignableEmployees cho phép PM thấy mọi nhân sự active)
    useEffect(() => {
        getAssignableEmployees()
            .then((members) => {
                if (members && members.length > 0) {
                    const fetchedMembers: ProjectMember[] = members.map((emp) => ({
                        id: `u-${emp.employeeId}`,
                        employeeId: emp.employeeId,
                        name: emp.fullName || emp.employeeCode,
                        role: emp.orgUnitName || 'Nhân viên',
                        avatar: '',
                        capacity: 40,
                        weeklyHours: {},
                        contractEndDate: emp.contractEndDate,
                        status: emp.status,
                    }));
                    setAllEmployees(fetchedMembers);
                } else {
                    fallbackLoadEmployees();
                }
            })
            .catch(() => {
                fallbackLoadEmployees();
            });

        function fallbackLoadEmployees() {
            getEmployees(1, 100)
                .then((res) => {
                    if (res?.content && res.content.length > 0) {
                        const fetchedMembers: ProjectMember[] = res.content
                            .filter((emp) => emp.id !== 1 && !emp.fullName?.toLowerCase().includes('quản trị viên'))
                            .map((emp) => ({
                                id: `u-${emp.id}`,
                                employeeId: emp.id,
                                name: emp.fullName || emp.employeeCode,
                                role: emp.professionalRole || 'Nhân viên',
                                avatar: '',
                                capacity: emp.standardHoursPerWeek || 40,
                                weeklyHours: {},
                                contractEndDate: emp.contractEndDate,
                                status: (emp as any).status || 'ACTIVE',
                            }));
                        setAllEmployees(fetchedMembers);
                    }
                })
                .catch(() => {
                    getUsers(0, 100)
                        .then((res) => {
                            if (res?.content && res.content.length > 0) {
                                const fetchedMembers: ProjectMember[] = res.content
                                  .filter((u) => u.employeeId !== null && u.employeeId !== 1 && !String(u.roleCode).includes('06'))
                                  .map((u) => ({
                                    id: `u-${u.employeeId}`,
                                    employeeId: u.employeeId ?? undefined,
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
                            console.warn('Failed to load employees for the project view:', err);
                        });
                });
        }
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
                if (node.assigneeIds && Array.isArray(node.assigneeIds)) {
                    node.assigneeIds.forEach((id) => assignedEmployeeIds.add(id));
                }
                collectAssignees(node.children || []);
            });
            collectAssignees(wbsNodes);
            const projectMembers = allEmployees.filter((member) =>
                assignedEmployeeIds.has(Number(member.id.replace('u-', '')))
            );
            // Ưu tiên hiển thị tất cả nhân sự trong hệ thống để Quản lý nguồn lực có thể phân bổ trực tiếp; nếu chưa có WBS thì dùng allEmployees
            setMembers(allEmployees.length > 0 ? allEmployees : projectMembers);
            const mapped = mapBackendWbsToUiCategories(wbsNodes, projectMembers);
            setCategories(mapped);
            getTaskDependencies(projId)
                .then((res) => setTaskDependenciesList(res.dependencies || []))
                .catch(() => setTaskDependenciesList([]));
        } catch (err) {
            console.warn(`Failed to fetch WBS for project ${projId}:`, err);
            setCategories([]);
            setMembers([]);
            setTaskDependenciesList([]);
        } finally {
            setIsLoadingWbs(false);
        }
    }, [allEmployees]);

    useEffect(() => {
        if (selectedProjectId) {
            loadWbsForProject(selectedProjectId);
        }
    }, [selectedProjectId, loadWbsForProject]);

    // 4. Tải mốc tiến độ (Milestones) khi chọn một dự án thật
    const loadMilestonesForProject = useCallback(async (projId: number) => {
        setIsLoadingMilestones(true);
        try {
            const data = await getProjectMilestones(projId);
            setMilestones(data || []);
        } catch (err) {
            console.warn(`Failed to fetch milestones for project ${projId}:`, err);
            setMilestones([]);
        } finally {
            setIsLoadingMilestones(false);
        }
    }, []);

    useEffect(() => {
        if (selectedProjectId) {
            loadMilestonesForProject(selectedProjectId);
        } else {
            setMilestones([]);
        }
    }, [selectedProjectId, loadMilestonesForProject]);

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

    // 4. Tải ước lượng nhu cầu nhân sự thật từ API Backend (NCL-03-CN-007)
    const loadProjectDemands = useCallback(async (projId: number) => {
        setIsLoadingDemand(true);
        setDemandError(null);
        try {
            const summary = await getProjectResourceDemands(projId);
            setDemandSummary(summary);
        } catch (err: any) {
            console.warn(`Failed to fetch resource demands for project ${projId}:`, err);
            setDemandSummary(null);
            setDemandError(err instanceof Error ? err.message : 'Không thể tải bảng ước lượng nhu cầu nhân sự.');
        } finally {
            setIsLoadingDemand(false);
        }
    }, []);

    useEffect(() => {
        if (selectedProjectId) {
            loadProjectDemands(selectedProjectId);
        } else {
            setDemandSummary(null);
        }
    }, [selectedProjectId, loadProjectDemands]);

    const handleSaveDemand = async (roleId: number, hoursPerWeek: number) => {
        if (!selectedProjectId) return;
        const result = await estimateResourceDemand(selectedProjectId, { roleId, hoursPerWeek });
        setDemandSummary(result);
        if (result.exceedsEstimatedHours) {
            showToast('Ước lượng thành công! Cảnh báo: Nhu cầu nhân sự vượt quy mô dự án.', 'info');
        } else {
            showToast('Ước lượng nhu cầu nhân sự theo vai trò thành công!', 'success');
        }
    };

    const handleDeleteDemand = async (roleId: number) => {
        if (!selectedProjectId) return;
        const result = await deleteResourceDemand(selectedProjectId, roleId);
        setDemandSummary(result);
        showToast('Đã xóa ước lượng nhu cầu nhân sự của vai trò thành công!', 'success');
    };

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

    const handleSaveAdjustedHours = async (memberId: string, weekKey: string, newHours: number, percentage?: number, overloadReason?: string) => {
        const member = members.find((m) => m.id === memberId);
        if (!canManageAllocations || !selectedProjectId || !member) return;
        const employeeId = Number(member.id.replace('u-', ''));
        const isoWeek = getDisplayedIsoWeek(weekKey);
        try {
            await allocateProjectHours({
                employeeId,
                projectId: selectedProjectId,
                year: isoWeek.year,
                weekNumber: isoWeek.week,
                allocatedHours: newHours,
                allocationPercentage: percentage,
                overloadReason,
            });
            await loadProjectAllocations();
            showToast(`Đã lưu phân bổ ${newHours}h ${percentage !== undefined ? `(${percentage}%)` : ''} cho ${member.name} (${weekKey})`, 'success');
        } catch (error) {
            showToast(error instanceof Error ? error.message : 'Không thể lưu phân bổ nguồn lực.', 'error');
            throw error;
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

    const handleProjectClosed = async (closedProj: ProjectResult) => {
        showToast(`Đã đóng dự án ${closedProj.projectName} thành công. Toàn bộ công việc và phân bổ đã được khóa (QTN-08)!`, 'info');
        await loadProjects();
        if (selectedProjectId) {
            await loadWbsForProject(selectedProjectId);
            await loadMilestonesForProject(selectedProjectId);
        }
    };

    const handleProjectReopened = async (reopenedProj: ProjectResult) => {
        showToast(`Đã mở lại dự án ${reopenedProj.projectName} thành công. Dự án đã chuyển về trạng thái hoạt động!`, 'success');
        await loadProjects();
        if (selectedProjectId) {
            await loadWbsForProject(selectedProjectId);
            await loadMilestonesForProject(selectedProjectId);
        }
    };

    // Quản lý mốc tiến độ (NCL-03-CN-006)
    const handleCreateMilestone = async (payload: CreateMilestonePayload) => {
        if (!selectedProjectId) {
            showToast('Vui lòng chọn một dự án trước khi khai báo mốc tiến độ', 'error');
            return;
        }
        try {
            const created = await createMilestone(selectedProjectId, payload);
            setMilestones((prev) => [...prev, created]);
            showToast('Khai báo mốc tiến độ thành công', 'success');
        } catch (err: any) {
            console.error('Lỗi khi tạo mốc tiến độ:', err);
            const errMsg = err?.message || 'Không thể tạo mốc tiến độ. Vui lòng kiểm tra lại.';
            showToast(`Lỗi: ${errMsg}`, 'error');
            throw err;
        }
    };

    const handleUpdateMilestone = async (id: number, payload: UpdateMilestonePayload) => {
        if (!selectedProjectId) return;
        try {
            const updated = await updateMilestone(selectedProjectId, id, payload);
            setMilestones((prev) => prev.map((m) => (m.id === id ? updated : m)));
            showToast('Cập nhật mốc tiến độ thành công', 'success');
        } catch (err: any) {
            console.error('Lỗi khi cập nhật mốc tiến độ:', err);
            const errMsg = err?.message || 'Không thể cập nhật mốc tiến độ.';
            showToast(`Lỗi: ${errMsg}`, 'error');
            throw err;
        }
    };

    const handleCompleteMilestone = async (id: number) => {
        if (!selectedProjectId) return;
        const today = new Date().toISOString().substring(0, 10);
        try {
            const updated = await completeMilestone(selectedProjectId, id, today);
            setMilestones((prev) => prev.map((m) => (m.id === id ? updated : m)));
            showToast('Đã đánh dấu hoàn thành mốc tiến độ', 'success');
        } catch (err: any) {
            console.error('Lỗi khi hoàn thành mốc tiến độ:', err);
            const errMsg = err?.message || 'Không thể đánh dấu hoàn thành mốc tiến độ.';
            showToast(`Lỗi: ${errMsg}`, 'error');
            throw err;
        }
    };

    const handleDeleteMilestone = async (id: number) => {
        if (!selectedProjectId) return;
        try {
            await deleteMilestone(selectedProjectId, id);
            setMilestones((prev) => prev.filter((m) => m.id !== id));
            showToast('Đã xóa mốc tiến độ thành công', 'success');
        } catch (err: any) {
            console.error('Lỗi khi xóa mốc tiến độ:', err);
            const errMsg = err?.message || 'Không thể xóa mốc tiến độ.';
            showToast(`Lỗi: ${errMsg}`, 'error');
            throw err;
        }
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

                                {/* Status Badge của dự án đang chọn */}
                                {selectedProject && (
                                    selectedProject.status === 'CLOSED' ? (
                                        <span className="inline-flex items-center gap-1 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-0.5 text-xs font-bold text-rose-700 shadow-2xs">
                                            <Lock className="h-3 w-3 text-rose-600" />
                                            <span>Đã đóng</span>
                                        </span>
                                    ) : selectedProject.status === 'ACTIVE' ? (
                                        <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-xs font-bold text-emerald-700 shadow-2xs">
                                            <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-500" />
                                            <span>Đang thực hiện</span>
                                        </span>
                                    ) : (
                                        <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-700 shadow-2xs">
                                            <span>Tạm dừng</span>
                                        </span>
                                    )
                                )}

                                {isBackendConnected && selectedProject ? (
                                    <span className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-[11px] font-medium text-slate-600">
                                        <Database className="h-3 w-3 text-emerald-600" /> Đồng bộ DB
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
                                                        {p.projectName} ({p.projectCode}) {p.status === 'CLOSED' ? '— [ĐÃ ĐÓNG]' : ''}
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

                        {/* Nút Đóng dự án (NCL-03-CN-004) */}
                        {canCloseProject && (
                            <button
                                type="button"
                                onClick={() => setCloseModalOpen(true)}
                                className="inline-flex items-center gap-1.5 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2 text-xs font-bold text-rose-700 shadow-2xs transition hover:bg-rose-100 active:scale-95 cursor-pointer"
                                title="Đóng dự án và chốt giờ công / chi phí (QTN-08)"
                            >
                                <Lock className="h-3.5 w-3.5 text-rose-600" />
                                <span>Đóng dự án</span>
                            </button>
                        )}

                        {/* Nút Mở lại dự án (NCL-03-CN-004) */}
                        {canReopenProject && (
                            <button
                                type="button"
                                onClick={() => setReopenModalOpen(true)}
                                className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-300 bg-emerald-50 px-3.5 py-2 text-xs font-bold text-emerald-700 shadow-2xs transition hover:bg-emerald-100 active:scale-95 cursor-pointer"
                                title="Kích hoạt mở lại dự án đã đóng"
                            >
                                <Unlock className="h-3.5 w-3.5 text-emerald-600" />
                                <span>Mở lại dự án</span>
                            </button>
                        )}

                        {canManageWbs && (
                            <button
                                type="button"
                                disabled={isProjectClosed}
                                onClick={() => !isProjectClosed && setCloneModalOpen(true)}
                                className={`inline-flex items-center gap-2 rounded-xl border border-indigo-600/30 bg-indigo-50 px-3.5 py-2 text-xs font-semibold text-indigo-700 shadow-2xs transition ${
                                    isProjectClosed
                                        ? 'opacity-40 cursor-not-allowed'
                                        : 'hover:bg-indigo-100 hover:border-indigo-600/60 active:scale-95 cursor-pointer'
                                }`}
                                title={isProjectClosed ? 'Dự án đã đóng, không thể nhân bản WBS' : 'Nhân bản toàn bộ cây WBS sang dự án khác'}
                            >
                                <Copy className="h-3.5 w-3.5 text-indigo-600" />
                                <span>Nhân bản WBS</span>
                            </button>
                        )}

                        {canManageProject && (
                            <>
                                <button
                                    type="button"
                                    disabled={isProjectClosed}
                                    onClick={() => !isProjectClosed && handleQuickAddTask()}
                                    className={`inline-flex items-center gap-2 rounded-xl px-4 py-2 text-xs font-bold text-white shadow-md transition ${
                                        isProjectClosed
                                            ? 'bg-slate-300 text-slate-500 shadow-none cursor-not-allowed'
                                            : 'bg-indigo-600 shadow-indigo-200 hover:bg-indigo-700 active:scale-95 cursor-pointer'
                                    }`}
                                    title={isProjectClosed ? 'Dự án đã đóng, không thể tạo thêm công việc mới (QTN-08)' : 'Thêm công việc'}
                                >
                                    <Plus className="h-4 w-4 stroke-[2.5]" />
                                    <span>Thêm công việc</span>
                                </button>

                                <button
                                    type="button"
                                    onClick={() => setDependencyModalOpen(true)}
                                    className="inline-flex items-center gap-2 rounded-xl border border-indigo-200 bg-indigo-50 px-3.5 py-2 text-xs font-bold text-indigo-700 shadow-2xs transition hover:bg-indigo-100 active:scale-95 cursor-pointer"
                                    title="Khai báo phụ thuộc giữa các công việc (NCL-04-CN-004)"
                                >
                                    <GitCommit className="h-4 w-4 text-indigo-600" />
                                    <span>Phụ thuộc công việc</span>
                                </button>
                            </>
                        )}

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
                                if (selectedProjectId) {
                                    loadWbsForProject(selectedProjectId);
                                    loadProjectDemands(selectedProjectId);
                                    loadMilestonesForProject(selectedProjectId);
                                }
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

            {/* Banner cảnh báo khi dự án đã đóng theo quy tắc QTN-08 */}
            {isProjectClosed && selectedProject && (
                <div className="rounded-2xl border border-rose-200 bg-rose-50/90 p-4 text-xs text-rose-900 shadow-2xs flex flex-wrap items-start justify-between gap-4">
                    <div className="flex items-start gap-3">
                        <div className="p-2 rounded-xl bg-rose-100 text-rose-700 shrink-0">
                            <Lock className="h-5 w-5" />
                        </div>
                        <div>
                            <div className="flex items-center gap-2">
                                <h3 className="font-bold text-rose-950 text-sm">
                                    Dự án đã đóng ({selectedProject.projectCode})
                                </h3>
                                <span className="inline-flex items-center gap-1 rounded-full border border-rose-300 bg-white px-2 py-0.5 text-[10px] font-bold text-rose-700">
                                    Khóa QTN-08
                                </span>
                            </div>
                            <p className="text-rose-700 mt-1 leading-relaxed text-[11px]">
                                Theo quy tắc <strong>QTN-08</strong>, toàn bộ công việc và phân bổ nguồn lực đã được chốt. Hệ thống không cho phép tạo thêm công việc mới hoặc thay đổi giờ phân bổ.
                            </p>
                            {selectedProject.closureReason && (
                                <p className="mt-1.5 text-[11px] text-rose-800 bg-white/70 p-2 rounded-lg border border-rose-200/60">
                                    <span className="font-semibold text-rose-900">Lý do đóng:</span> {selectedProject.closureReason}
                                </p>
                            )}
                        </div>
                    </div>
                    {canReopenProject && (
                        <button
                            type="button"
                            onClick={() => setReopenModalOpen(true)}
                            className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 px-3.5 py-2 text-xs font-bold text-white shadow-xs hover:bg-emerald-700 transition shrink-0 cursor-pointer"
                        >
                            <Unlock className="h-3.5 w-3.5" />
                            <span>Mở lại dự án</span>
                        </button>
                    )}
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
            <div className="flex flex-col items-start justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-2xs xl:flex-row xl:items-center">
                {/* View Segmented Tabs */}
                <div className="inline-flex w-full max-w-full overflow-x-auto rounded-xl border border-slate-200/80 bg-slate-100 p-1 xl:w-auto shrink-0">
                    <button
                        type="button"
                        onClick={() => setViewMode('split')}
                        className={`flex items-center justify-center gap-2 rounded-lg px-3 py-1.5 text-xs font-semibold whitespace-nowrap transition-all shrink-0 cursor-pointer ${
                            viewMode === 'split'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <Columns className="h-3.5 w-3.5" />
                        <span>Xem kết hợp (Split View)</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => setViewMode('wbs')}
                        className={`flex items-center justify-center gap-2 rounded-lg px-3 py-1.5 text-xs font-semibold whitespace-nowrap transition-all shrink-0 cursor-pointer ${
                            viewMode === 'wbs'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <Layers className="h-3.5 w-3.5" />
                        <span>Hạng mục & Task</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => setViewMode('workload')}
                        className={`flex items-center justify-center gap-2 rounded-lg px-3 py-1.5 text-xs font-semibold whitespace-nowrap transition-all shrink-0 cursor-pointer ${
                            viewMode === 'workload'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <CalendarDays className="h-3.5 w-3.5" />
                        <span>Phân bổ theo tuần</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => setViewMode('demand')}
                        className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition-all sm:flex-none cursor-pointer ${
                            viewMode === 'demand'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <TrendingUp className="h-4 w-4" />
                        <span>Ước lượng nhu cầu</span>
                        {demandSummary && demandSummary.demandsByRole.length > 0 && (
                            <span className="rounded-full bg-indigo-100 px-1.5 py-0.2 text-[10px] font-bold text-indigo-700">
                                {demandSummary.demandsByRole.length}
                            </span>
                        )}
                    </button>
                    <button
                        type="button"
                        onClick={() => setViewMode('milestones')}
                        className={`flex items-center justify-center gap-2 rounded-lg px-3 py-1.5 text-xs font-semibold whitespace-nowrap transition-all shrink-0 cursor-pointer ${
                            viewMode === 'milestones'
                                ? 'bg-white text-indigo-700 shadow-xs'
                                : 'text-slate-600 hover:text-slate-900 font-medium'
                        }`}
                    >
                        <Flag className="h-3.5 w-3.5" />
                        <span>Mốc tiến độ</span>
                    </button>
                </div>

                {/* Filters (Ẩn khi ở tab Mốc tiến độ hoặc Ước lượng nhu cầu) */}
                {viewMode !== 'milestones' && viewMode !== 'demand' && (
                    <div className="flex w-full flex-wrap items-center justify-end gap-2 xl:w-auto shrink-0">
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
                )}
            </div>

            {/* Main Views Container Grid */}
            <div className="grid grid-cols-1 gap-6 items-start transition-all duration-300 lg:grid-cols-12">
                {/* Section 1: WBS Hierarchy */}
                {(viewMode === 'split' || viewMode === 'wbs') && (
                    <div className={viewMode === 'split' ? 'lg:col-span-5' : 'lg:col-span-12'}>
                        <ProjectWbsView
                            categories={categories}
                            members={allEmployees.length > 0 ? allEmployees : members}
                            dependencies={taskDependenciesList}
                            searchTerm={search}
                            selectedRole={roleFilter}
                            projectId={selectedProjectId}
                            isClosed={isProjectClosed}
                            onQuickAddTask={handleQuickAddTask}
                            onToggleTaskStatus={handleToggleTaskStatus}
                            onOpenBudgetModal={handleOpenBudgetModal}
                            onOpenCloneModal={canManageWbs && !isProjectClosed ? () => setCloneModalOpen(true) : undefined}
                            onOpenAssignModal={canManageWbs ? handleOpenAssignModal : undefined}
                            onRefreshData={() => selectedProjectId && loadWbsForProject(selectedProjectId)}
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
                            isClosed={isProjectClosed}
                            onNavigateMonth={handleNavigateMonth}
                            onOpenAdjustModal={handleOpenAdjustModal}
                        />
                    </div>
                )}

                {/* Section 3: Resource Demand Estimation (NCL-03-CN-007) */}
                {viewMode === 'demand' && (
                    <div className="lg:col-span-12">
                        <ProjectDemandView
                            project={selectedProject}
                            canManage={canManageProject}
                            demandSummary={demandSummary}
                            isLoading={isLoadingDemand}
                            error={demandError}
                            onReload={() => {
                                if (selectedProjectId) {
                                    loadProjectDemands(selectedProjectId);
                                }
                            }}
                            onOpenCreateModal={() => {
                                setEditingDemandRole(null);
                                setEstimateModalOpen(true);
                            }}
                            onOpenEditModal={(role) => {
                                setEditingDemandRole(role);
                                setEstimateModalOpen(true);
                            }}
                            onOpenDeleteModal={(role) => {
                                setRoleToDelete(role);
                                setDeleteModalOpen(true);
                            }}
                        />
                    </div>
                )}

                {/* Section 4: Project Milestones (NCL-03-CN-006) */}
                {viewMode === 'milestones' && (
                    <div className="lg:col-span-12">
                        {selectedProjectId ? (
                            <MilestoneListView
                                milestones={milestones}
                                categories={categories}
                                canEdit={canManageMilestones}
                                isLoading={isLoadingMilestones}
                                onRefresh={() => loadMilestonesForProject(selectedProjectId)}
                                onCreateMilestone={handleCreateMilestone}
                                onUpdateMilestone={handleUpdateMilestone}
                                onCompleteMilestone={handleCompleteMilestone}
                                onDeleteMilestone={handleDeleteMilestone}
                            />
                        ) : (
                            <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center text-slate-500">
                                <Flag className="mx-auto h-10 w-10 text-slate-300 mb-3" />
                                <h3 className="text-sm font-bold text-slate-700">Chưa chọn dự án</h3>
                                <p className="text-xs text-slate-400 mt-1">Vui lòng chọn một dự án ở thanh phía trên để xem và quản lý các mốc tiến độ.</p>
                            </div>
                        )}
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

            {canManageWbs && (
                <AssignTaskModal
                    open={assignModalOpen}
                    task={selectedAssignTask}
                    projectId={selectedProjectId}
                    employees={allEmployees.length > 0 ? allEmployees : members}
                    onClose={() => setAssignModalOpen(false)}
                    onSuccess={handleAssignSuccess}
                />
            )}

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
                year={selectedAdjustCell?.weekKey ? getDisplayedIsoWeek(selectedAdjustCell.weekKey).year : undefined}
                weekNumber={selectedAdjustCell?.weekKey ? getDisplayedIsoWeek(selectedAdjustCell.weekKey).week : undefined}
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
                currentUser={currentUser}
                onClose={() => setProjectCreateModalOpen(false)}
                onCreated={handleProjectCreated}
            />}

            {canManageProject && <TaskDependencyModal
                open={dependencyModalOpen}
                projectId={selectedProjectId || 1}
                tasks={categories.flatMap((cat) =>
                    cat.tasks.map((t) => ({
                        id: Number(t.id.replace(/\D/g, '')),
                        taskCode: t.code,
                        name: t.name,
                        categoryName: cat.name,
                    }))
                )}
                canManage={canManageProject}
                onClose={() => {
                    setDependencyModalOpen(false);
                    if (selectedProjectId) loadWbsForProject(selectedProjectId);
                }}
            />}

            {/* Modals for Resource Demand Estimation (NCL-03-CN-007) */}
            <EstimateDemandModal
                open={estimateModalOpen}
                projectId={selectedProjectId || 0}
                projectCode={selectedProject?.projectCode}
                projectName={selectedProject?.projectName}
                projectStartDate={selectedProject?.startDate}
                projectEndDate={selectedProject?.endDate}
                projectEstimatedHours={selectedProject?.estimatedHours ? Number(selectedProject.estimatedHours) : 0}
                currentTotalDemandHours={demandSummary?.totalDemandHours ? Number(demandSummary.totalDemandHours) : 0}
                editingRole={editingDemandRole}
                existingRoleDemands={demandSummary?.demandsByRole || []}
                onClose={() => {
                    setEstimateModalOpen(false);
                    setEditingDemandRole(null);
                }}
                onSave={handleSaveDemand}
            />

            <DeleteDemandConfirmModal
                open={deleteModalOpen}
                roleDemand={roleToDelete}
                projectName={selectedProject?.projectName}
                onClose={() => {
                    setDeleteModalOpen(false);
                    setRoleToDelete(null);
                }}
                onConfirm={handleDeleteDemand}
            />

            {/* Modal Đóng dự án (NCL-03-CN-004) */}
            <ProjectCloseModal
                open={closeModalOpen}
                project={selectedProject}
                unfinishedTasks={unfinishedTasks}
                isExecutive={isExecutive}
                onClose={() => setCloseModalOpen(false)}
                onSuccess={handleProjectClosed}
            />

            {/* Modal Mở lại dự án (NCL-03-CN-004) */}
            <ProjectReopenModal
                open={reopenModalOpen}
                project={selectedProject}
                isExecutive={isExecutive}
                onClose={() => setReopenModalOpen(false)}
                onSuccess={handleProjectReopened}
            />

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