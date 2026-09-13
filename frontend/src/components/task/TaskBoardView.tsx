import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
    Kanban,
    Search,
    RefreshCw,
    UserCheck,
    AlertCircle,
    CheckCircle2,
    X,
} from 'lucide-react';
import {
    getTaskBoard,
    moveTaskBoardStatus,
    type TaskBoardData,
    type TaskBoardCard,
    type TaskStatus,
} from '@/lib/api/taskBoard';
import { getProjects, type ProjectResult } from '@/lib/api/projects';
import { getEmployees, type EmployeeProfile } from '@/lib/api/employees';
import { useAuthUser } from '@/lib/auth-session';
import TaskBoardColumn from './TaskBoardColumn';

interface TaskBoardViewProps {
    defaultProjectId?: number;
}

const STATUS_COLUMNS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED'];

const STATUS_LABELS: Record<TaskStatus, string> = {
    TODO: 'Chờ thực hiện',
    IN_PROGRESS: 'Đang thực hiện',
    IN_REVIEW: 'Đang kiểm duyệt',
    DONE: 'Hoàn thành',
    CANCELLED: 'Đã hủy',
};

export const TaskBoardView: React.FC<TaskBoardViewProps> = ({ defaultProjectId }) => {
    const currentUser = useAuthUser();

    // Data states
    const [boardData, setBoardData] = useState<TaskBoardData | null>(null);
    const [projects, setProjects] = useState<ProjectResult[]>([]);
    const [employees, setEmployees] = useState<EmployeeProfile[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    // Filters
    const [selectedProjectId, setSelectedProjectId] = useState<number | null>(
        defaultProjectId ?? null
    );
    const [selectedEmployeeId, setSelectedEmployeeId] = useState<number | null>(null);
    const [myTasksOnly, setMyTasksOnly] = useState<boolean>(false);
    const [searchQuery, setSearchQuery] = useState<string>('');

    // Toast notification
    const [toast, setToast] = useState<{
        message: string;
        type: 'success' | 'warning' | 'error';
    } | null>(null);

    const showToast = useCallback(
        (message: string, type: 'success' | 'warning' | 'error' = 'success') => {
            setToast({ message, type });
            window.setTimeout(() => {
                setToast(null);
            }, 3500);
        },
        []
    );

    // Current employee ID of logged in user
    const currentEmployeeId = useMemo(() => {
        if (!currentUser) return null;
        const found = employees.find((e) => e.userId === currentUser.id);
        return found ? found.id : null;
    }, [currentUser, employees]);

    // Load projects and employees for filter dropdowns
    useEffect(() => {
        let isMounted = true;
        const loadInitialData = async () => {
            try {
                const [projRes, empRes] = await Promise.all([
                    getProjects(0, 100).catch(() => ({ content: [] as ProjectResult[] })),
                    getEmployees(1, 100).catch(() => ({ content: [] as EmployeeProfile[] })),
                ]);
                if (isMounted) {
                    setProjects(projRes.content || []);
                    setEmployees(empRes.content || []);
                }
            } catch (err) {
                console.error('Error loading filter options:', err);
            }
        };
        loadInitialData();
        return () => {
            isMounted = false;
        };
    }, []);

    // Sync defaultProjectId when prop changes
    useEffect(() => {
        if (defaultProjectId !== undefined) {
            setSelectedProjectId(defaultProjectId);
        }
    }, [defaultProjectId]);

    // Load Task Board Data from Backend
    const fetchBoardData = useCallback(async (isRefreshAction: boolean = false) => {
        if (isRefreshAction) {
            setIsRefreshing(true);
        } else {
            setIsLoading(true);
        }
        setError(null);

        try {
            const employeeIdParam = myTasksOnly
                ? currentEmployeeId ?? undefined
                : selectedEmployeeId ?? undefined;

            const data = await getTaskBoard({
                projectId: selectedProjectId ?? undefined,
                employeeId: employeeIdParam,
            });
            setBoardData(data);
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Không thể tải bảng công việc';
            setError(msg);
        } finally {
            setIsLoading(false);
            setIsRefreshing(false);
        }
    }, [selectedProjectId, selectedEmployeeId, myTasksOnly, currentEmployeeId]);

    useEffect(() => {
        fetchBoardData(false);
    }, [fetchBoardData]);

    // Handle "Việc của tôi" toggle
    const handleToggleMyTasks = () => {
        if (!myTasksOnly) {
            setMyTasksOnly(true);
            setSelectedEmployeeId(null);
        } else {
            setMyTasksOnly(false);
        }
    };

    // Filter cards by search query
    const filterCards = useCallback(
        (cards: TaskBoardCard[]) => {
            if (!searchQuery.trim()) return cards;
            const q = searchQuery.toLowerCase().trim();
            return cards.filter(
                (card) =>
                    card.taskCode.toLowerCase().includes(q) ||
                    card.name.toLowerCase().includes(q) ||
                    (card.projectCode && card.projectCode.toLowerCase().includes(q)) ||
                    (card.projectName && card.projectName.toLowerCase().includes(q)) ||
                    card.assignees.some((a) => a.fullName.toLowerCase().includes(q))
            );
        },
        [searchQuery]
    );

    // Filtered lists for each column
    const filteredColumns = useMemo(() => {
        if (!boardData) {
            return {
                TODO: [] as TaskBoardCard[],
                IN_PROGRESS: [] as TaskBoardCard[],
                IN_REVIEW: [] as TaskBoardCard[],
                DONE: [] as TaskBoardCard[],
                CANCELLED: [] as TaskBoardCard[],
            };
        }
        return {
            TODO: filterCards(boardData.todoTasks),
            IN_PROGRESS: filterCards(boardData.inProgressTasks),
            IN_REVIEW: filterCards(boardData.inReviewTasks),
            DONE: filterCards(boardData.doneTasks),
            CANCELLED: filterCards(boardData.cancelledTasks),
        };
    }, [boardData, filterCards]);

    // Handle drag start
    const handleDragStartCard = (
        _e: React.DragEvent<HTMLDivElement>,
        _card: TaskBoardCard
    ) => {
        // Drag info is set on dataTransfer
    };

    // Handle card drop onto a column
    const handleDropCard = async (taskId: number, newStatus: TaskStatus) => {
        if (!boardData) return;

        // Find the card from boardData
        const allCards = [
            ...boardData.todoTasks,
            ...boardData.inProgressTasks,
            ...boardData.inReviewTasks,
            ...boardData.doneTasks,
            ...boardData.cancelledTasks,
        ];
        const cardToMove = allCards.find((c) => c.taskId === taskId);

        if (!cardToMove) return;

        // If dropping into the exact same column, do nothing
        if (cardToMove.status === newStatus) {
            return;
        }

        // Check canMove permission (TC-02)
        if (!cardToMove.canMove) {
            showToast(
                'Bạn không có quyền chuyển trạng thái công việc này! Chỉ người được giao việc hoặc PM mới có quyền chuyển.',
                'warning'
            );
            return;
        }

        // Save snapshot for rollback on error
        const snapshot = { ...boardData };

        // Helper to remove card from columns
        const removeCard = (list: TaskBoardCard[]) => list.filter((c) => c.taskId !== taskId);

        // Optimistic UI update
        const updatedCard: TaskBoardCard = { ...cardToMove, status: newStatus };

        const updatedTodo = removeCard(boardData.todoTasks);
        const updatedInProgress = removeCard(boardData.inProgressTasks);
        const updatedInReview = removeCard(boardData.inReviewTasks);
        const updatedDone = removeCard(boardData.doneTasks);
        const updatedCancelled = removeCard(boardData.cancelledTasks);

        if (newStatus === 'TODO') updatedTodo.push(updatedCard);
        else if (newStatus === 'IN_PROGRESS') updatedInProgress.push(updatedCard);
        else if (newStatus === 'IN_REVIEW') updatedInReview.push(updatedCard);
        else if (newStatus === 'DONE') updatedDone.push(updatedCard);
        else if (newStatus === 'CANCELLED') updatedCancelled.push(updatedCard);

        setBoardData({
            ...boardData,
            todoTasks: updatedTodo,
            inProgressTasks: updatedInProgress,
            inReviewTasks: updatedInReview,
            doneTasks: updatedDone,
            cancelledTasks: updatedCancelled,
        });

        // Call backend API
        try {
            const savedCard = await moveTaskBoardStatus(taskId, newStatus);
            showToast(`Đã chuyển công việc sang "${STATUS_LABELS[newStatus]}"`, 'success');

            // Synchronize with server response
            setBoardData((prev) => {
                if (!prev) return prev;
                const replaceCard = (list: TaskBoardCard[]) =>
                    list.map((c) => (c.taskId === taskId ? { ...c, ...savedCard } : c));
                return {
                    ...prev,
                    todoTasks: replaceCard(prev.todoTasks),
                    inProgressTasks: replaceCard(prev.inProgressTasks),
                    inReviewTasks: replaceCard(prev.inReviewTasks),
                    doneTasks: replaceCard(prev.doneTasks),
                    cancelledTasks: replaceCard(prev.cancelledTasks),
                };
            });
        } catch (err: unknown) {
            // Rollback optimistic state
            setBoardData(snapshot);
            const msg =
                err instanceof Error
                    ? err.message
                    : 'Không thể cập nhật trạng thái công việc. Vui lòng thử lại.';
            showToast(msg, 'error');
        }
    };

    return (
        <div className="relative flex flex-col gap-4 w-full">
            {/* Control Bar: Filters & Search */}
            <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs md:flex-row md:items-center md:justify-between">
                {/* Left: Title & Filter Selects */}
                <div className="flex flex-wrap items-center gap-2.5">
                    <div className="flex items-center gap-2 mr-2">
                        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
                            <Kanban className="h-5 w-5" />
                        </div>
                        <div>
                            <h2 className="text-sm font-bold text-slate-900">Bảng công việc</h2>
                            <p className="text-[11px] text-slate-500">
                                {boardData ? `${boardData.totalTasks} công việc` : 'Đang tải...'}
                            </p>
                        </div>
                    </div>

                    {/* Project Filter */}
                    <div className="relative">
                        <select
                            value={selectedProjectId ?? ''}
                            onChange={(e) =>
                                setSelectedProjectId(e.target.value ? Number(e.target.value) : null)
                            }
                            className="h-9 rounded-xl border border-slate-200 bg-slate-50 px-3 pr-8 text-xs text-slate-700 outline-none focus:border-indigo-500 focus:bg-white"
                        >
                            <option value="">Tất cả dự án ({projects.length})</option>
                            {projects.map((p) => (
                                <option key={p.id} value={p.id}>
                                    {p.projectCode} - {p.projectName}
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* Employee Filter (Disabled when "Việc của tôi" is active) */}
                    <div className="relative">
                        <select
                            disabled={myTasksOnly}
                            value={selectedEmployeeId ?? ''}
                            onChange={(e) =>
                                setSelectedEmployeeId(e.target.value ? Number(e.target.value) : null)
                            }
                            className={`h-9 rounded-xl border border-slate-200 bg-slate-50 px-3 pr-8 text-xs text-slate-700 outline-none focus:border-indigo-500 focus:bg-white ${
                                myTasksOnly ? 'opacity-50 cursor-not-allowed' : ''
                            }`}
                        >
                            <option value="">Tất cả nhân sự ({employees.length})</option>
                            {employees.map((emp) => (
                                <option key={emp.id} value={emp.id}>
                                    {emp.fullName} ({emp.employeeCode})
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* "Việc của tôi" Toggle Button */}
                    <button
                        type="button"
                        onClick={handleToggleMyTasks}
                        className={`flex h-9 items-center gap-1.5 rounded-xl px-3 text-xs font-semibold transition-all cursor-pointer ${
                            myTasksOnly
                                ? 'bg-indigo-600 text-white shadow-xs'
                                : 'border border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
                        }`}
                    >
                        <UserCheck className="h-3.5 w-3.5" />
                        <span>Việc của tôi</span>
                    </button>
                </div>

                {/* Right: Search & Refresh */}
                <div className="flex items-center gap-2">
                    <div className="relative flex-1 sm:w-60">
                        <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="Tìm mã việc, tên việc..."
                            className="h-9 w-full rounded-xl border border-slate-200 bg-slate-50 py-1.5 pl-8 pr-3 text-xs text-slate-800 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20"
                        />
                        {searchQuery && (
                            <button
                                onClick={() => setSearchQuery('')}
                                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                            >
                                <X className="h-3.5 w-3.5" />
                            </button>
                        )}
                    </div>

                    <button
                        type="button"
                        onClick={() => fetchBoardData(true)}
                        disabled={isLoading || isRefreshing}
                        title="Tải lại bảng"
                        className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors cursor-pointer"
                    >
                        <RefreshCw
                            className={`h-4 w-4 ${isRefreshing || isLoading ? 'animate-spin' : ''}`}
                        />
                    </button>
                </div>
            </div>

            {/* Error banner if any */}
            {error && (
                <div className="flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
                    <AlertCircle className="h-4 w-4 shrink-0 text-rose-500" />
                    <span>{error}</span>
                    <button
                        onClick={() => fetchBoardData(true)}
                        className="ml-auto font-semibold underline hover:text-rose-900 cursor-pointer"
                    >
                        Thử lại
                    </button>
                </div>
            )}

            {/* Board Columns Container */}
            {isLoading ? (
                <div className="flex h-96 items-center justify-center rounded-2xl border border-slate-200 bg-white">
                    <div className="flex flex-col items-center gap-2 text-slate-400">
                        <RefreshCw className="h-6 w-6 animate-spin text-indigo-500" />
                        <span className="text-xs font-medium">Đang tải bảng công việc...</span>
                    </div>
                </div>
            ) : (
                <div className="flex gap-4 overflow-x-auto pb-4 pt-1 items-start">
                    {STATUS_COLUMNS.map((status) => (
                        <TaskBoardColumn
                            key={status}
                            status={status}
                            cards={filteredColumns[status]}
                            onDropCard={handleDropCard}
                            onDragStartCard={handleDragStartCard}
                        />
                    ))}
                </div>
            )}

            {/* Floating Toast Notification */}
            {toast && (
                <div className="fixed bottom-6 right-6 z-50 animate-in fade-in slide-in-from-bottom-5 duration-200">
                    <div
                        className={`flex items-center gap-2.5 rounded-xl px-4 py-3 text-xs font-semibold shadow-lg text-white ${
                            toast.type === 'success'
                                ? 'bg-emerald-600'
                                : toast.type === 'warning'
                                ? 'bg-amber-600'
                                : 'bg-rose-600'
                        }`}
                    >
                        {toast.type === 'success' && <CheckCircle2 className="h-4 w-4 shrink-0" />}
                        {toast.type === 'warning' && <AlertCircle className="h-4 w-4 shrink-0" />}
                        {toast.type === 'error' && <AlertCircle className="h-4 w-4 shrink-0" />}
                        <span>{toast.message}</span>
                    </div>
                </div>
            )}
        </div>
    );
};

export default TaskBoardView;
