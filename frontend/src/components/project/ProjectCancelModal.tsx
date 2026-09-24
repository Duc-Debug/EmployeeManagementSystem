import { useState, type FormEvent } from 'react';
import { X, Ban, AlertTriangle, AlertCircle, Loader2 } from 'lucide-react';
import { cancelProject, type ProjectResult } from '@/lib/api/projects';

interface ProjectCancelModalProps {
    open: boolean;
    project: ProjectResult | null;
    onClose: () => void;
    onSuccess: (cancelledProject: ProjectResult) => void;
}

export function ProjectCancelModal({
    open,
    project,
    onClose,
    onSuccess,
}: ProjectCancelModalProps) {
    const [cancelReason, setCancelReason] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    if (!open || !project) return null;

    const handleCloseModal = () => {
        if (isSubmitting) return;
        setCancelReason('');
        setErrorMsg('');
        onClose();
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setErrorMsg('');

        const trimmedReason = cancelReason.trim();
        if (!trimmedReason) {
            setErrorMsg('Vui lòng nhập lý do hủy dự án.');
            return;
        }

        if (trimmedReason.length > 500) {
            setErrorMsg('Lý do hủy dự án không được vượt quá 500 ký tự.');
            return;
        }

        setIsSubmitting(true);
        try {
            const result = await cancelProject(project.id, trimmedReason);
            setCancelReason('');
            onSuccess(result);
            onClose();
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Có lỗi khi hủy dự án';
            setErrorMsg(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 animate-in fade-in duration-200">
            <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl border border-slate-100 overflow-hidden flex flex-col max-h-[90vh]">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-rose-50/50">
                    <div className="flex items-center gap-2.5 text-rose-700">
                        <div className="p-2 rounded-xl bg-rose-100 text-rose-700">
                            <Ban className="h-5 w-5" />
                        </div>
                        <div>
                            <h2 className="text-base font-bold text-slate-900">Hủy dự án dự kiến</h2>
                            <p className="text-xs text-rose-700 font-medium">Chuyển trạng thái sang Đã hủy</p>
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
                <form onSubmit={handleSubmit} className="p-6 space-y-4 overflow-y-auto text-xs">
                    {errorMsg && (
                        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700 flex items-start gap-2">
                            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
                            <span>{errorMsg}</span>
                        </div>
                    )}

                    {/* Target project info */}
                    <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3 space-y-1">
                        <div className="flex items-center justify-between">
                            <span className="text-slate-500 font-medium">Dự án:</span>
                            <span className="font-bold text-slate-900">{project.projectName}</span>
                        </div>
                        <div className="flex items-center justify-between">
                            <span className="text-slate-500 font-medium">Mã dự án:</span>
                            <span className="font-mono font-semibold text-indigo-700">{project.projectCode}</span>
                        </div>
                    </div>

                    {/* Warning Box */}
                    <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-3 text-amber-900 space-y-1.5">
                        <div className="flex items-center gap-1.5 font-bold text-amber-950">
                            <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
                            <span>Lưu ý khi hủy dự án dự kiến</span>
                        </div>
                        <p className="text-[11px] leading-relaxed text-amber-800">
                            Khi hủy dự án, toàn bộ các bản ghi <strong>Giữ chỗ nguồn lực</strong> của dự án sẽ được tự động giải phóng để nhân sự có thể tham gia dự án khác.
                        </p>
                    </div>

                    {/* Cancel Reason Input */}
                    <div>
                        <label className="mb-1 block font-semibold text-slate-700">
                            Lý do hủy dự án <span className="text-rose-500">*</span>
                        </label>
                        <textarea
                            rows={3}
                            value={cancelReason}
                            onChange={(e) => setCancelReason(e.target.value)}
                            maxLength={500}
                            placeholder="Nhập lý do hủy dự án (VD: Thay đổi định hướng kinh doanh hoặc khách hàng dừng kế hoạch)..."
                            className="w-full rounded-xl border border-slate-300 p-2.5 text-xs font-medium text-slate-800 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                            required
                        />
                        <div className="mt-1 flex justify-between text-[10px] text-slate-400">
                            <span>Tối đa 500 ký tự</span>
                            <span>{cancelReason.length}/500</span>
                        </div>
                    </div>

                    {/* Footer Actions */}
                    <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
                        <button
                            type="button"
                            onClick={handleCloseModal}
                            disabled={isSubmitting}
                            className="rounded-xl border border-slate-200 px-4 py-2 font-semibold text-slate-600 hover:bg-slate-50 transition cursor-pointer"
                        >
                            Hủy bỏ
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting || !cancelReason.trim()}
                            className="inline-flex items-center gap-2 rounded-xl bg-rose-600 px-4 py-2 font-bold text-white shadow-md shadow-rose-200 hover:bg-rose-700 transition active:scale-95 disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                    <span>Đang xử lý...</span>
                                </>
                            ) : (
                                <>
                                    <Ban className="h-4 w-4" />
                                    <span>Xác nhận hủy dự án</span>
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
