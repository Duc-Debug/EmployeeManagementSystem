import React, { useState, useEffect } from 'react';
import { X, Target, AlertTriangle, CheckCircle2, AlertCircle, Clock, ShieldAlert, Sparkles } from 'lucide-react';
import type { TaskItem, ProjectMember } from './projectData';

interface ProjectBudgetModalProps {
    open: boolean;
    task: TaskItem | null;
    member?: ProjectMember | null;
    onClose: () => void;
    onSave: (taskId: string, budgetHours: number) => void;
}

export function ProjectBudgetModal({
    open,
    task,
    member,
    onClose,
    onSave,
}: ProjectBudgetModalProps) {
    const [budgetHours, setBudgetHours] = useState<number>(0);

    useEffect(() => {
        if (task) {
            setBudgetHours(task.budgetHours !== undefined ? task.budgetHours : task.hours || 0);
        }
    }, [task, open]);

    if (!open || !task) return null;

    const actualHours = task.actualHours || 0;
    const parsedBudget = Number(budgetHours) || 0;
    const burnedPct = parsedBudget > 0 ? Math.round((actualHours / parsedBudget) * 100) : 0;
    const remainingHours = Math.max(0, parsedBudget - actualHours);
    const overBudgetHours = Math.max(0, actualHours - parsedBudget);
    const isOverBudget = parsedBudget > 0 && actualHours > parsedBudget;
    const isWarning = parsedBudget > 0 && burnedPct >= 80 && !isOverBudget;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (parsedBudget <= 0) return;
        onSave(task.id, parsedBudget);
        onClose();
    };

    const handleQuickAdd = (delta: number) => {
        setBudgetHours((prev) => Math.max(0, (Number(prev) || 0) + delta));
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
            <div className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50/80 p-4">
                    <div className="flex items-center gap-2.5">
                        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-100 text-indigo-700 shadow-xs">
                            <Target className="h-5 w-5" />
                        </span>
                        <div>
                            <h3 className="text-sm font-bold text-slate-800">
                                Đặt Ngân Sách Giờ Công Cho Công Việc
                            </h3>
                            <p className="text-[11px] text-slate-500">
                                Kiểm soát chi phí & phát hiện sớm công việc làm quá lâu
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                {/* Task Context Card */}
                <div className="border-b border-slate-100 bg-indigo-50/40 p-4">
                    <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                            <div className="flex items-center gap-2">
                                <span className="font-mono text-[11px] font-bold text-indigo-600">
                                    {task.code}
                                </span>
                                <span className="truncate text-xs font-semibold text-slate-800">
                                    {task.name}
                                </span>
                            </div>
                            <div className="mt-1 flex items-center gap-2 text-[11px] text-slate-500">
                                <span>Phụ trách: <strong className="text-slate-700">{member ? member.name : 'Chưa gán'}</strong></span>
                                <span>•</span>
                                <span>Ước lượng gốc: <strong>{task.hours}h</strong></span>
                            </div>
                        </div>

                        <div className="text-right shrink-0">
                            <span className="block text-[10px] uppercase font-bold text-slate-400">Giờ thực tế duyệt</span>
                            <span className="inline-flex items-center gap-1 font-mono text-sm font-bold text-slate-800">
                                <Clock className="h-3.5 w-3.5 text-slate-500" />
                                {actualHours}h
                            </span>
                        </div>
                    </div>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs">
                    <div>
                        <div className="flex items-center justify-between mb-1.5">
                            <label className="font-semibold text-slate-700">
                                Ngân sách giờ công định mức (Budget Hours) *
                            </label>
                            <div className="flex items-center gap-1">
                                {[5, 10, 20].map((step) => (
                                    <button
                                        key={step}
                                        type="button"
                                        onClick={() => handleQuickAdd(step)}
                                        className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-medium text-slate-600 hover:bg-indigo-100 hover:text-indigo-700 transition"
                                    >
                                        +{step}h
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="relative">
                            <input
                                type="number"
                                required
                                min="0.1"
                                step="0.5"
                                value={budgetHours}
                                onChange={(e) => setBudgetHours(parseFloat(e.target.value) || 0)}
                                placeholder="Nhập số giờ ngân sách (VD: 40)"
                                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm font-bold text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            />
                            <span className="absolute right-3.5 top-1/2 -translate-y-1/2 font-semibold text-slate-400 text-xs">
                                giờ (hours)
                            </span>
                        </div>
                        <p className="mt-1 text-[11px] text-slate-400">
                            Hạn mức thời gian tối đa để nhân sự hoàn tất công việc mà không làm thâm hụt lợi nhuận.
                        </p>
                    </div>

                    {/* Real-time Comparison Preview */}
                    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5 space-y-3">
                        <div className="flex items-center justify-between">
                            <span className="font-semibold text-slate-700 text-[11px] uppercase tracking-wide flex items-center gap-1.5">
                                <Sparkles className="h-3.5 w-3.5 text-indigo-500" />
                                Đối soát Ngân sách vs Thực tế (Burn Rate)
                            </span>
                            <span className="font-mono text-xs font-bold text-slate-800">
                                {burnedPct}% đã dùng
                            </span>
                        </div>

                        {/* Progress Bar */}
                        <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200">
                            <div
                                className={`h-2 rounded-full transition-all duration-300 ${
                                    isOverBudget
                                        ? 'bg-rose-500'
                                        : isWarning
                                        ? 'bg-amber-500'
                                        : 'bg-emerald-500'
                                }`}
                                style={{ width: `${Math.min(burnedPct, 100)}%` }}
                            />
                        </div>

                        {/* Status Alert Banner */}
                        {isOverBudget ? (
                            <div className="flex items-start gap-2.5 rounded-lg border border-rose-200 bg-rose-50 p-2.5 text-rose-800">
                                <ShieldAlert className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
                                <div>
                                    <p className="font-bold text-[11px]">
                                        CẢNH BÁO: ĐANG ĂN MÒN LỢI NHUẬN DỰ ÁN!
                                    </p>
                                    <p className="mt-0.5 text-[10px] text-rose-700">
                                        Công việc đã làm <strong>{actualHours}h</strong>, vượt ngân sách định mức{' '}
                                        <strong>{overBudgetHours}h</strong> ({burnedPct}%). Cần trao đổi với nhân sự để kiểm tra nguyên nhân bị làm quá lâu.
                                    </p>
                                </div>
                            </div>
                        ) : isWarning ? (
                            <div className="flex items-start gap-2.5 rounded-lg border border-amber-200 bg-amber-50 p-2.5 text-amber-800">
                                <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
                                <div>
                                    <p className="font-bold text-[11px]">
                                        CẢNH BÁO: TIỆM CẬN HẠN MỨC NGÂN SÁCH ({burnedPct}%)
                                    </p>
                                    <p className="mt-0.5 text-[10px] text-amber-700">
                                        Chỉ còn lại <strong>{remainingHours}h</strong> dự trữ. Cần theo dõi sát tiến độ nộp bảng chấm công.
                                    </p>
                                </div>
                            </div>
                        ) : parsedBudget > 0 ? (
                            <div className="flex items-center gap-2 rounded-lg border border-emerald-200 bg-emerald-50 p-2 text-emerald-800">
                                <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
                                <span className="text-[11px] font-medium">
                                    Ngân sách an toàn. Còn lại <strong>{remainingHours}h</strong> ({100 - burnedPct}% quỹ giờ).
                                </span>
                            </div>
                        ) : (
                            <div className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white p-2 text-slate-500">
                                <AlertCircle className="h-4 w-4 shrink-0 text-slate-400" />
                                <span className="text-[11px]">
                                    Chưa đặt ngân sách. Vui lòng nhập số giờ lớn hơn 0 để kích hoạt theo dõi lợi nhuận.
                                </span>
                            </div>
                        )}
                    </div>

                    {/* Actions */}
                    <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-xl border border-slate-300 px-4 py-2 font-semibold text-slate-600 hover:bg-slate-50 transition"
                        >
                            Hủy bỏ
                        </button>
                        <button
                            type="submit"
                            className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-5 py-2 font-bold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 transition"
                        >
                            <Target className="h-4 w-4" />
                            Lưu ngân sách giờ
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}