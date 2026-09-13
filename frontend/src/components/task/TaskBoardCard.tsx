import React from 'react';
import { Calendar, Clock, Lock, User } from 'lucide-react';
import type { TaskBoardCard as TaskBoardCardType } from '@/lib/api/taskBoard';

interface TaskBoardCardProps {
    card: TaskBoardCardType;
    onDragStart: (e: React.DragEvent<HTMLDivElement>, card: TaskBoardCardType) => void;
}

export const TaskBoardCard: React.FC<TaskBoardCardProps> = ({ card, onDragStart }) => {
    const handleDragStart = (e: React.DragEvent<HTMLDivElement>) => {
        if (!card.canMove) {
            e.preventDefault();
            return;
        }
        e.dataTransfer.setData('text/plain', String(card.taskId));
        e.dataTransfer.effectAllowed = 'move';
        onDragStart(e, card);
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

    return (
        <div
            draggable={card.canMove}
            onDragStart={handleDragStart}
            className={`group relative rounded-xl border bg-white p-3.5 shadow-2xs transition-all duration-200 select-none ${
                card.canMove
                    ? 'cursor-grab active:cursor-grabbing hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-md'
                    : 'cursor-not-allowed border-slate-200/80 bg-slate-50/70 opacity-90'
            }`}
        >
            {/* Header: Project Badge & Task Code */}
            <div className="flex items-center justify-between gap-2 mb-1.5">
                <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-0.5 text-[11px] font-semibold text-indigo-700">
                    {card.taskCode}
                </span>

                <div className="flex items-center gap-1.5">
                    {card.projectCode && (
                        <span className="text-[11px] font-medium text-slate-500 max-w-[120px] truncate" title={card.projectName}>
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

            {/* Meta Info: Dates & Hours */}
            <div className="flex items-center gap-3 text-[11px] text-slate-500 mb-3">
                {(card.plannedStartDate || card.plannedEndDate) && (
                    <div className="flex items-center gap-1">
                        <Calendar className="h-3 w-3 text-slate-400" />
                        <span>
                            {formatDate(card.plannedStartDate) || '...'} - {formatDate(card.plannedEndDate) || '...'}
                        </span>
                    </div>
                )}

                {(card.estimatedHours !== undefined || card.actualHours !== undefined) && (
                    <div className="flex items-center gap-1">
                        <Clock className="h-3 w-3 text-slate-400" />
                        <span>
                            {card.actualHours ?? 0}h / {card.estimatedHours ?? 0}h
                        </span>
                    </div>
                )}
            </div>

            {/* Footer: Assignees List */}
            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
                <div className="flex items-center -space-x-1.5 overflow-hidden">
                    {card.assignees && card.assignees.length > 0 ? (
                        card.assignees.slice(0, 3).map((assignee) => (
                            <div
                                key={assignee.employeeId}
                                title={`${assignee.fullName} (${assignee.employeeCode})${assignee.isPrimary ? ' - Phụ trách chính' : ''}`}
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

                <div className="text-[10px] text-slate-400 font-medium">
                    {card.canMove ? (
                        <span className="text-emerald-600">Có thể chuyển</span>
                    ) : (
                        <span className="text-slate-400">Chỉ xem</span>
                    )}
                </div>
            </div>
        </div>
    );
};

export default TaskBoardCard;
