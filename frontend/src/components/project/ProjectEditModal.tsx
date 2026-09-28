import React, { useState, useEffect } from 'react';
import {
    X,
    Calendar,
    Clock,
    FileText,
    CheckCircle2,
    Loader2,
    Edit3,
} from 'lucide-react';
import {
    updateProject,
    type ProjectResult,
    type UpdateProjectPayload,
} from '@/lib/api/projects';

interface ProjectEditModalProps {
    open: boolean;
    project: ProjectResult | null;
    onClose: () => void;
    onUpdated: (updatedProject: ProjectResult) => void;
}

export function ProjectEditModal({
    open,
    project,
    onClose,
    onUpdated,
}: ProjectEditModalProps) {
    const [projectName, setProjectName] = useState('');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [estimatedHours, setEstimatedHours] = useState<number>(200);
    const [description, setDescription] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    useEffect(() => {
        if (open && project) {
            setProjectName(project.projectName || '');
            setStartDate(project.startDate || '');
            setEndDate(project.endDate || '');
            setEstimatedHours(project.estimatedHours ? Number(project.estimatedHours) : 200);
            setDescription(project.description || '');
            setErrorMsg('');
        }
    }, [open, project]);

    if (!open || !project) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setErrorMsg('');

        if (!projectName.trim()) {
            setErrorMsg('Tên dự án không được để trống.');
            return;
        }

        if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
            setErrorMsg('Ngày kết thúc không được sớm hơn ngày bắt đầu.');
            return;
        }

        setIsSubmitting(true);
        try {
            const payload: UpdateProjectPayload = {
                projectName: projectName.trim(),
                startDate: startDate || undefined,
                endDate: endDate || undefined,
                estimatedHours: estimatedHours > 0 ? estimatedHours : undefined,
                description: description.trim() || undefined,
            };

            const updated = await updateProject(project.id, payload);
            onUpdated(updated);
            onClose();
        } catch (err: any) {
            setErrorMsg(err?.message || 'Không thể cập nhật thông tin dự án.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs">
            <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl animate-in zoom-in-95 duration-150">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                    <div className="flex items-center gap-2.5">
                        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
                            <Edit3 className="h-5 w-5" />
                        </div>
                        <div>
                            <h2 className="text-base font-bold text-slate-900">
                                Cập nhật thông tin dự án
                            </h2>
                            <p className="text-xs text-slate-400 font-mono">
                                Mã: {project.projectCode}
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                {/* Error Banner */}
                {errorMsg && (
                    <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs font-semibold text-rose-700">
                        {errorMsg}
                    </div>
                )}

                {/* Form */}
                <form onSubmit={handleSubmit} className="mt-4 space-y-4">
                    <div>
                        <label className="mb-1 block text-xs font-bold text-slate-700">
                            Tên dự án <span className="text-rose-500">*</span>
                        </label>
                        <input
                            type="text"
                            value={projectName}
                            onChange={(e) => setProjectName(e.target.value)}
                            placeholder="Nhập tên dự án..."
                            required
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-2 text-xs font-semibold text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="mb-1 block text-xs font-bold text-slate-700">
                                <span className="inline-flex items-center gap-1">
                                    <Calendar className="h-3.5 w-3.5 text-slate-400" />
                                    <span>Ngày bắt đầu</span>
                                </span>
                            </label>
                            <input
                                type="date"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-2 text-xs font-semibold text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                            />
                        </div>

                        <div>
                            <label className="mb-1 block text-xs font-bold text-slate-700">
                                <span className="inline-flex items-center gap-1">
                                    <Calendar className="h-3.5 w-3.5 text-indigo-500" />
                                    <span>Ngày kết thúc</span>
                                </span>
                            </label>
                            <input
                                type="date"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-2 text-xs font-semibold text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                            />
                            <p className="mt-1 text-[10px] text-slate-400">
                                * Cần nhập để ước lượng nhu cầu nhân sự theo tuần.
                            </p>
                        </div>
                    </div>

                    <div>
                        <label className="mb-1 block text-xs font-bold text-slate-700">
                            <span className="inline-flex items-center gap-1">
                                <Clock className="h-3.5 w-3.5 text-slate-400" />
                                <span>Tổng giờ dự kiến (Hours)</span>
                            </span>
                        </label>
                        <input
                            type="number"
                            min={0}
                            value={estimatedHours}
                            onChange={(e) => setEstimatedHours(Number(e.target.value))}
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-2 text-xs font-semibold text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>

                    <div>
                        <label className="mb-1 block text-xs font-bold text-slate-700">
                            <span className="inline-flex items-center gap-1">
                                <FileText className="h-3.5 w-3.5 text-slate-400" />
                                <span>Mô tả mục tiêu dự án</span>
                            </span>
                        </label>
                        <textarea
                            rows={3}
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="Nhập mô tả dự án..."
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-2 text-xs font-medium text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>

                    {/* Actions */}
                    <div className="flex items-center justify-end gap-2.5 border-t border-slate-100 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={isSubmitting}
                            className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition cursor-pointer"
                        >
                            Hủy bỏ
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-sm hover:bg-indigo-700 transition disabled:opacity-50 cursor-pointer"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                                    <span>Đang lưu...</span>
                                </>
                            ) : (
                                <>
                                    <CheckCircle2 className="h-3.5 w-3.5" />
                                    <span>Lưu thay đổi</span>
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
