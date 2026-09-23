import { useState } from 'react';
import { X, CheckCircle2, AlertCircle, Loader2, Sparkles, Users } from 'lucide-react';
import { approveProject, type ProjectResult } from '@/lib/api/projects';

interface ProjectApproveModalProps {
    open: boolean;
    project: ProjectResult | null;
    onClose: () => void;
    onSuccess: (approvedProject: ProjectResult) => void;
}

export function ProjectApproveModal({
    open,
    project,
    onClose,
    onSuccess,
}: ProjectApproveModalProps) {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    if (!open || !project) return null;

    const handleCloseModal = () => {
        if (isSubmitting) return;
        setErrorMsg('');
        onClose();
    };

    const handleApprove = async () => {
        setErrorMsg('');
        setIsSubmitting(true);
        try {
            const result = await approveProject(project.id);
            onSuccess(result);
            onClose();
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Có lỗi khi phê duyệt dự án';
            setErrorMsg(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 animate-in fade-in duration-200">
            <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl border border-slate-100 overflow-hidden flex flex-col max-h-[90vh]">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-emerald-50/60">
                    <div className="flex items-center gap-2.5 text-emerald-700">
                        <div className="p-2 rounded-xl bg-emerald-100 text-emerald-700">
                            <Sparkles className="h-5 w-5" />
                        </div>
                        <div>
                            <h2 className="text-base font-bold text-slate-900">Phê duyệt & Khởi động dự án</h2>
                            <p className="text-xs text-emerald-700 font-medium">Chuyển trạng thái sang Đang thực hiện (ACTIVE)</p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={handleCloseModal}
                        disabled={isSubmitting}
                        className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {/* Body */}
                <div className="p-6 space-y-4 overflow-y-auto text-xs">
                    {errorMsg && (
                        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700 flex items-start gap-2">
                            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
                            <span>{errorMsg}</span>
                        </div>
                    )}

                    {/* Target project info */}
                    <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3.5 space-y-2">
                        <div className="flex items-center justify-between">
                            <span className="text-slate-500 font-medium">Tên dự án:</span>
                            <span className="font-bold text-slate-900">{project.projectName}</span>
                        </div>
                        <div className="flex items-center justify-between">
                            <span className="text-slate-500 font-medium">Mã dự án:</span>
                            <span className="font-mono font-semibold text-indigo-700">{project.projectCode}</span>
                        </div>
                        <div className="flex items-center justify-between">
                            <span className="text-slate-500 font-medium">Thời gian:</span>
                            <span className="font-medium text-slate-700">
                                {project.startDate || 'Chưa cập nhật'} {project.endDate ? `đến ${project.endDate}` : ''}
                            </span>
                        </div>
                    </div>

                    {/* Auto Convert Notification Box */}
                    <div className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-3.5 text-emerald-950 space-y-1.5">
                        <div className="flex items-center gap-1.5 font-bold text-emerald-900">
                            <Users className="h-4 w-4 text-emerald-600 shrink-0" />
                            <span>Tự động chuyển đổi giữ chỗ</span>
                        </div>
                        <p className="text-[11px] leading-relaxed text-emerald-800">
                            Khi dự án được phê duyệt, hệ thống sẽ tự động chuyển đổi tất cả các vị trí <strong>Giữ chỗ nguồn lực</strong> đang chờ thành <strong>Phân bổ nguồn lực chính thức</strong> cho nhân sự trong dự án.
                        </p>
                    </div>

                    <p className="text-[11px] text-slate-500 italic">
                        Sau khi khởi động, nhân sự trong dự án sẽ có thể bắt đầu ghi nhận tiến độ công việc và log giờ làm.
                    </p>

                    {/* Footer Actions */}
                    <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={handleCloseModal}
                            disabled={isSubmitting}
                            className="rounded-xl border border-slate-200 px-4 py-2 font-semibold text-slate-600 hover:bg-slate-50 transition cursor-pointer"
                        >
                            Hủy bỏ
                        </button>
                        <button
                            type="button"
                            onClick={handleApprove}
                            disabled={isSubmitting}
                            className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 font-bold text-white shadow-md shadow-emerald-200 hover:bg-emerald-700 transition active:scale-95 disabled:opacity-50 cursor-pointer"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                    <span>Đang phê duyệt...</span>
                                </>
                            ) : (
                                <>
                                    <CheckCircle2 className="h-4 w-4" />
                                    <span>Xác nhận phê duyệt</span>
                                </>
                            )}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
