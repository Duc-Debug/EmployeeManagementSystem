import React, { useState } from 'react';
import { X, Unlock, AlertCircle, AlertTriangle, Loader2, CheckCircle2 } from 'lucide-react';
import { reopenProject, type ProjectResult } from '@/lib/api/projects';

interface ProjectReopenModalProps {
    open: boolean;
    project: ProjectResult | null;
    isExecutive?: boolean;
    onClose: () => void;
    onSuccess: (reopenedProject: ProjectResult) => void;
}

export function ProjectReopenModal({
    open,
    project,
    isExecutive = false,
    onClose,
    onSuccess,
}: ProjectReopenModalProps) {
    const [reopenReason, setReopenReason] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    if (!open || !project) return null;

    const handleCloseModal = () => {
        if (isSubmitting) return;
        setReopenReason('');
        setErrorMsg('');
        onClose();
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setErrorMsg('');

        const trimmedReason = reopenReason.trim();

        if (!trimmedReason || trimmedReason.length < 10) {
            setErrorMsg('Lý do mở lại dự án bắt buộc phải có từ 10 đến 500 ký tự.');
            return;
        }

        if (trimmedReason.length > 500) {
            setErrorMsg('Lý do mở lại dự án không được vượt quá 500 ký tự.');
            return;
        }

        setIsSubmitting(true);
        try {
            const result = await reopenProject(project.id, trimmedReason);
            setReopenReason('');
            onSuccess(result);
            onClose();
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Có lỗi khi mở lại dự án';
            setErrorMsg(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 animate-in fade-in duration-200">
            <div className="w-full max-w-lg rounded-2xl bg-white shadow-2xl border border-slate-100 overflow-hidden flex flex-col max-h-[90vh]">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-emerald-50/50">
                    <div className="flex items-center gap-2.5 text-emerald-700">
                        <div className="p-2 rounded-xl bg-emerald-100 text-emerald-700">
                            <Unlock className="h-5 w-5" />
                        </div>
                        <div>
                            <h2 className="text-base font-bold text-slate-900">Mở lại dự án</h2>
                            <p className="text-xs text-emerald-700 font-medium">NCL-03-CN-004 & Kích hoạt lại vòng đời</p>
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

                {/* Form Body */}
                <form onSubmit={handleSubmit} className="p-6 space-y-4 overflow-y-auto text-xs">
                    {errorMsg && (
                        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700 flex items-start gap-2">
                            <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
                            <span>{errorMsg}</span>
                        </div>
                    )}

                    {/* Target project info */}
                    <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-3 space-y-1.5">
                        <div className="flex items-center justify-between">
                            <span className="text-slate-500 font-medium">Dự án:</span>
                            <span className="font-bold text-slate-900">{project.projectName}</span>
                        </div>
                        <div className="flex items-center justify-between">
                            <span className="text-slate-500 font-medium">Mã dự án:</span>
                            <span className="font-mono font-semibold text-indigo-700">{project.projectCode}</span>
                        </div>
                        {project.closureReason && (
                            <div className="pt-1.5 border-t border-slate-200/60 text-slate-600">
                                <span className="text-slate-400 font-medium">Lý do đóng trước đó: </span>
                                <span className="italic">{project.closureReason}</span>
                            </div>
                        )}
                    </div>

                    {/* Reopen Info Box */}
                    <div className="rounded-xl border border-emerald-200 bg-emerald-50/80 p-3 text-emerald-900 space-y-1">
                        <div className="flex items-center gap-1.5 font-bold text-emerald-950">
                            <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                            <span>Kích hoạt lại dự án (ACTIVE)</span>
                        </div>
                        <p className="text-[11px] leading-relaxed text-emerald-800">
                            Sau khi mở lại thành công, dự án sẽ chuyển về trạng thái <strong>ĐANG THỰC HIỆN</strong>.
                            Quản lý dự án (PM) và thành viên có thể tiếp tục tạo công việc mới và cập nhật phân bổ nguồn lực.
                        </p>
                    </div>

                    {/* RBAC Notice if not executive */}
                    {!isExecutive && (
                        <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-3 text-amber-900 space-y-1">
                            <div className="flex items-center gap-1.5 font-bold text-amber-950">
                                <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
                                <span>Phân quyền Ban Giám đốc (VT-01)</span>
                            </div>
                            <p className="text-[11px] text-amber-800">
                                Theo quy tắc bảo mật hệ thống, quyền mở lại dự án đã đóng được giới hạn cho <strong>Ban Giám đốc (VT-01)</strong> nhằm đảm bảo tính toàn vẹn của chi phí và giờ công.
                            </p>
                        </div>
                    )}

                    {/* Reopen Reason Input */}
                    <div>
                        <label className="mb-1 block font-semibold text-slate-700">
                            Lý do mở lại dự án <span className="text-rose-500">* (Bắt buộc tối thiểu 10 ký tự)</span>
                        </label>
                        <textarea
                            rows={3}
                            required
                            value={reopenReason}
                            onChange={(e) => setReopenReason(e.target.value)}
                            minLength={10}
                            maxLength={500}
                            placeholder="Nhập lý do mở lại dự án chi tiết (VD: Ký kết phụ lục hợp đồng số 02 bổ sung tính năng mới)..."
                            className="w-full rounded-xl border border-slate-300 p-2.5 text-xs font-medium text-slate-800 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
                        />
                        <div className="mt-1 flex justify-between text-[10px] text-slate-400">
                            <span>Bắt buộc từ 10 đến 500 ký tự</span>
                            <span className={reopenReason.trim().length >= 10 ? 'text-emerald-600 font-semibold' : 'text-slate-400'}>
                                {reopenReason.trim().length}/500 ký tự
                            </span>
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
                            disabled={isSubmitting || reopenReason.trim().length < 10}
                            className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 font-bold text-white shadow-md shadow-emerald-200 hover:bg-emerald-700 transition active:scale-95 disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                    <span>Đang xử lý...</span>
                                </>
                            ) : (
                                <>
                                    <Unlock className="h-4 w-4" />
                                    <span>Xác nhận mở lại</span>
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
