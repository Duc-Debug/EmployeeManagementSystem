import React, { useState, useRef, useEffect } from 'react';
import {
    Calendar,
    Clock,
    Lock,
    User,
    X,
    MoreHorizontal,
    ArrowRight,
} from 'lucide-react';
import type { TaskBoardCard as TaskBoardCardType, TaskStatus } from '@/lib/api/taskBoard';

interface TaskBoardCardProps {
    card: TaskBoardCardType;
    onDragStart: (e: React.DragEvent<HTMLDivElement>, card: TaskBoardCardType) => void;
    onQuickMove?: (taskId: number, newStatus: TaskStatus) => void;
}

const ALL_STATUSES: { key: TaskStatus; label: string; color: string }[] = [
    { key: 'TODO', label: 'Chờ thực hiện', color: 'text-slate-700 hover:bg-slate-100' },
    { key: 'IN_PROGRESS', label: 'Đang thực hiện', color: 'text-blue-700 hover:bg-blue-50' },
    { key: 'IN_REVIEW', label: 'Đang kiểm duyệt', color: 'text-amber-700 hover:bg-amber-50' },
    { key: 'DONE', label: 'Hoàn thành', color: 'text-emerald-700 hover:bg-emerald-50' },
    { key: 'CANCELLED', label: 'Đã hủy', color: 'text-rose-700 hover:bg-rose-50' },
];

export const TaskBoardCard: React.FC<TaskBoardCardProps> = ({
    card,
    onDragStart,
    onQuickMove,
}) => {
    const [showDetails, setShowDetails] = useState(false);
    const [isDragging, setIsDragging] = useState(false);
    const [showQuickMove, setShowQuickMove] = useState(false);
    const menuRef = useRef<HTMLDivElement>(null);

    // Close menu when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                setShowQuickMove(false);
            }
        };
        if (showQuickMove) {
            document.addEventListener('mousedown', handleClickOutside);
        }
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [showQuickMove]);

    const handleDragStart = (e: React.DragEvent<HTMLDivElement>) => {
        if (!card.canMove) {
            e.preventDefault();
            return;
        }
        setIsDragging(true);
        e.dataTransfer.setData('text/plain', String(card.taskId));
        e.dataTransfer.effectAllowed = 'move';
        onDragStart(e, card);
    };

    const handleDragEnd = () => {
        setIsDragging(false);
    };

    const formatDate = (dateStr?: string) => {
        if (!dateStr) return null;
        try {
            const date = new Date(dateStr);
            return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
        } catch {
            return dateStr;
        }
    };

    const availableMoveStatuses = ALL_STATUSES.filter((s) => s.key !== card.status);

    return (
        <>
        <div
            draggable={card.canMove}
            role="button"
            tabIndex={0}
            aria-label={`Xem chi tiết ${card.name}`}
            onClick={() => { if (!isDragging) setShowDetails(true); }}
            onKeyDown={e => {
                if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) {
                    e.preventDefault(); setShowDetails(true);
                }
            }}
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
            className={`group relative rounded-xl border bg-white p-2.5 shadow-2xs transition-all duration-200 select-none ${
                isDragging
                    ? 'opacity-40 ring-2 ring-indigo-400 border-dashed scale-[0.98]'
                    : card.canMove
                    ? 'cursor-grab active:cursor-grabbing hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-md'
                    : 'cursor-pointer border-slate-200/80 bg-slate-50/70 opacity-90'
            }`}
        >
            {/* Header: Project Badge & Task Code */}
            <div className="flex items-center justify-between gap-2 mb-1.5">
                <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-0.5 text-[11px] font-semibold text-indigo-700">
                    {card.taskCode}
                </span>

                <div className="flex items-center gap-1.5">
                    {card.projectCode && (
                        <span
                            className="text-[11px] font-medium text-slate-500 max-w-[120px] truncate"
                            title={card.projectName}
                        >
                            {card.projectCode}
                        </span>
                    )}
                    {!card.canMove && (
                        <span
                            title="Bạn không có quyền chuyển trạng thái công việc này"
                            className="inline-flex items-center text-amber-500"
                        >
                            <Lock className="h-3.5 w-3.5" />
                        </span>
                    )}
                </div>
            </div>

            {/* Task Name */}
            <h4 className="text-xs font-semibold text-slate-900 leading-snug line-clamp-2 mb-2 group-hover:text-indigo-600 transition-colors">
                {card.name}
            </h4>

            {/* Footer: Assignees List & Actions */}
            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
                <div className="flex items-center -space-x-1.5 overflow-hidden">
                    {card.assignees && card.assignees.length > 0 ? (
                        card.assignees.slice(0, 3).map((assignee) => (
                            <div
                                key={assignee.employeeId}
                                title={`${assignee.fullName} (${assignee.employeeCode})${
                                    assignee.isPrimary ? ' - Phụ trách chính' : ''
                                }`}
                                className={`inline-flex h-6 w-6 items-center justify-center rounded-full text-[10px] font-bold ring-2 ring-white ${
                                    assignee.isPrimary
                                        ? 'bg-indigo-600 text-white'
                                        : 'bg-slate-200 text-slate-700'
                                }`}
                            >
                                {assignee.fullName
                                    ? assignee.fullName
                                          .split(' ')
                                          .map((n) => n[0])
                                          .slice(-2)
                                          .join('')
                                          .toUpperCase()
                                    : 'NV'}
                            </div>
                        ))
                    ) : (
                        <div
                            title="Chưa phân công"
                            className="inline-flex h-6 w-6 items-center justify-center rounded-full bg-slate-100 text-slate-400 ring-2 ring-white"
                        >
                            <User className="h-3 w-3" />
                        </div>
                    )}
                    {card.assignees && card.assignees.length > 3 && (
                        <div
                            title={`Và thêm ${card.assignees.length - 3} người khác`}
                            className="inline-flex h-6 w-6 items-center justify-center rounded-full bg-slate-100 text-[10px] font-bold text-slate-500 ring-2 ring-white"
                        >
                            +{card.assignees.length - 3}
                        </div>
                    )}
                </div>

                {/* Right side: Quick move button or status badge */}
                <div className="relative" ref={menuRef}>
                    {card.canMove && onQuickMove ? (
                        <>
                            <button
                                type="button"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    setShowQuickMove(!showQuickMove);
                                }}
                                title="Chuyển nhanh trạng thái"
                                className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 px-2 py-0.5 text-[10px] font-semibold text-slate-700 hover:bg-indigo-50 hover:text-indigo-600 hover:border-indigo-200 transition-colors cursor-pointer"
                            >
                                <span>Chuyển</span>
                                <MoreHorizontal className="h-3 w-3" />
                            </button>

                            {/* Dropdown menu */}
                            {showQuickMove && (
                                <div className="absolute right-0 bottom-full mb-1 w-36 rounded-xl border border-slate-200 bg-white p-1 shadow-lg z-30 animate-in fade-in zoom-in-95 duration-100">
                                    <div className="px-2 py-1 text-[10px] font-bold text-slate-400 border-b border-slate-100 mb-1">
                                        Chuyển đến:
                                    </div>
                                    {availableMoveStatuses.map((s) => (
                                        <button
                                            key={s.key}
                                            type="button"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setShowQuickMove(false);
                                                onQuickMove(card.taskId, s.key);
                                            }}
                                            className={`flex w-full items-center justify-between rounded-lg px-2 py-1 text-[11px] font-medium transition-colors cursor-pointer ${s.color}`}
                                        >
                                            <span>{s.label}</span>
                                            <ArrowRight className="h-3 w-3 opacity-60" />
                                        </button>
                                    ))}
                                </div>
                            )}
                        </>
                    ) : (
                        <span className="text-[10px] text-slate-400 font-medium">Chỉ xem</span>
                    )}
                </div>
            </div>
        </div>
        {showDetails && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
                onClick={() => setShowDetails(false)}>
                <section role="dialog" aria-modal="true" aria-label={`Chi tiết ${card.name}`}
                    onClick={e => e.stopPropagation()} onKeyDown={e => { if (e.key === 'Escape') setShowDetails(false); }}
                    className="max-h-[85vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white p-5 shadow-xl">
                    <div className="flex items-start justify-between gap-3">
                        <div><p className="text-xs font-semibold text-indigo-600">{card.taskCode} · {card.projectName}</p>
                            <h3 className="mt-1 text-lg font-bold text-slate-900">{card.name}</h3></div>
                        <button type="button" autoFocus aria-label="Đóng chi tiết" onClick={() => setShowDetails(false)} className="rounded p-1 hover:bg-slate-100"><X className="h-5 w-5" /></button>
                    </div>
                    <p className="mt-4 whitespace-pre-wrap text-sm text-slate-600">{card.description || 'Chưa có mô tả.'}</p>
                    <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
                        <div><dt className="text-slate-500">Trạng thái</dt><dd>{ALL_STATUSES.find(status => status.key === card.status)?.label}</dd></div>
                        <div><dt className="flex items-center gap-1 text-slate-500"><Clock className="h-3.5 w-3.5" />Giờ thực tế / dự kiến</dt><dd>{card.actualHours ?? 0}h / {card.estimatedHours ?? 0}h</dd></div>
                        <div><dt className="flex items-center gap-1 text-slate-500"><Calendar className="h-3.5 w-3.5" />Bắt đầu</dt><dd>{formatDate(card.plannedStartDate) || 'Chưa đặt'}</dd></div>
                        <div><dt className="text-slate-500">Kết thúc</dt><dd>{formatDate(card.plannedEndDate) || 'Chưa đặt'}</dd></div>
                    </dl>
                    <p className="mt-4 text-sm text-slate-700"><strong>Người thực hiện: </strong>{card.assignees?.map(person => person.fullName).join(', ') || 'Chưa phân công'}</p>
                </section>
            </div>
        )}
        </>
    );
};

export default TaskBoardCard;
