import React, { useState } from 'react';
import { Circle, Clock, AlertCircle, CheckCircle2, XCircle } from 'lucide-react';
import type { TaskBoardCard as TaskBoardCardType, TaskStatus } from '@/lib/api/taskBoard';
import TaskBoardCard from './TaskBoardCard';

interface ColumnConfig {
    title: string;
    description: string;
    icon: React.ComponentType<{ className?: string }>;
    accentColor: string;
    badgeBg: string;
    badgeText: string;
    headerBg: string;
}

const COLUMN_CONFIGS: Record<TaskStatus, ColumnConfig> = {
    TODO: {
        title: 'Chờ thực hiện',
        description: 'Công việc chuẩn bị bắt đầu',
        icon: Circle,
        accentColor: 'border-t-slate-400',
        badgeBg: 'bg-slate-100',
        badgeText: 'text-slate-700',
        headerBg: 'bg-slate-50/80',
    },
    IN_PROGRESS: {
        title: 'Đang thực hiện',
        description: 'Đang trong quá trình xử lý',
        icon: Clock,
        accentColor: 'border-t-blue-500',
        badgeBg: 'bg-blue-100',
        badgeText: 'text-blue-700',
        headerBg: 'bg-blue-50/50',
    },
    IN_REVIEW: {
        title: 'Đang kiểm duyệt',
        description: 'Chờ kiểm tra & nghiệm thu',
        icon: AlertCircle,
        accentColor: 'border-t-amber-500',
        badgeBg: 'bg-amber-100',
        badgeText: 'text-amber-800',
        headerBg: 'bg-amber-50/50',
    },
    DONE: {
        title: 'Hoàn thành',
        description: 'Đã hoàn tất nghiệm thu',
        icon: CheckCircle2,
        accentColor: 'border-t-emerald-500',
        badgeBg: 'bg-emerald-100',
        badgeText: 'text-emerald-700',
        headerBg: 'bg-emerald-50/50',
    },
    CANCELLED: {
        title: 'Đã hủy',
        description: 'Không tiếp tục thực hiện',
        icon: XCircle,
        accentColor: 'border-t-rose-400',
        badgeBg: 'bg-rose-100',
        badgeText: 'text-rose-700',
        headerBg: 'bg-rose-50/50',
    },
};

interface TaskBoardColumnProps {
    status: TaskStatus;
    cards: TaskBoardCardType[];
    onDropCard: (taskId: number, newStatus: TaskStatus) => void;
    onDragStartCard: (e: React.DragEvent<HTMLDivElement>, card: TaskBoardCardType) => void;
}

export const TaskBoardColumn: React.FC<TaskBoardColumnProps> = ({
    status,
    cards,
    onDropCard,
    onDragStartCard,
}) => {
    const [isDragOver, setIsDragOver] = useState(false);
    const config = COLUMN_CONFIGS[status];
    const IconComponent = config.icon;

    const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
        if (!isDragOver) {
            setIsDragOver(true);
        }
    };

    const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
        // Only reset if leaving the column boundary
        if (!e.currentTarget.contains(e.relatedTarget as Node)) {
            setIsDragOver(false);
        }
    };

    const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        setIsDragOver(false);
        const taskIdStr = e.dataTransfer.getData('text/plain');
        if (taskIdStr) {
            const taskId = Number(taskIdStr);
            if (!isNaN(taskId)) {
                onDropCard(taskId, status);
            }
        }
    };

    return (
        <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            className={`flex flex-col min-w-[280px] w-full max-w-[340px] shrink-0 rounded-2xl border border-slate-200/80 bg-slate-100/70 p-3 transition-all duration-200 border-t-4 ${
                config.accentColor
            } ${
                isDragOver
                    ? 'ring-2 ring-indigo-500 bg-indigo-50/40 border-dashed border-indigo-300'
                    : ''
            }`}
        >
            {/* Column Header */}
            <div className={`flex items-center justify-between rounded-xl px-2.5 py-2 mb-3 ${config.headerBg}`}>
                <div className="flex items-center gap-2">
                    <IconComponent className={`h-4 w-4 ${config.badgeText}`} />
                    <div>
                        <h3 className="text-xs font-bold text-slate-800 tracking-tight">
                            {config.title}
                        </h3>
                    </div>
                </div>

                <span
                    className={`rounded-full px-2 py-0.5 text-xs font-bold ${config.badgeBg} ${config.badgeText}`}
                >
                    {cards.length}
                </span>
            </div>

            {/* Column Cards Container */}
            <div className="flex flex-1 flex-col gap-2.5 overflow-y-auto max-h-[calc(100vh-280px)] pr-0.5">
                {cards.length === 0 ? (
                    <div
                        className={`flex h-32 flex-col items-center justify-center rounded-xl border border-dashed text-center p-3 transition-colors ${
                            isDragOver
                                ? 'border-indigo-400 bg-indigo-50/50 text-indigo-600'
                                : 'border-slate-200 bg-white/40 text-slate-400'
                        }`}
                    >
                        <p className="text-xs font-medium">
                            {isDragOver ? 'Thả để chuyển vào đây' : 'Chưa có công việc'}
                        </p>
                    </div>
                ) : (
                    cards.map((card) => (
                        <TaskBoardCard
                            key={card.taskId}
                            card={card}
                            onDragStart={onDragStartCard}
                        />
                    ))
                )}
            </div>
        </div>
    );
};

export default TaskBoardColumn;
