import React, { useState } from 'react';
import { X, Lock, AlertTriangle, AlertCircle, Loader2 } from 'lucide-react';
import { closeProject, type ProjectResult } from '@/lib/api/projects';

interface ProjectCloseModalProps {
    open: boolean;
    project: ProjectResult | null;
    unfinishedTasks?: { code?: string; name: string }[];
    isExecutive?: boolean;
    onClose: () => void;
    onSuccess: (closedProject: ProjectResult) => void;
}

export function ProjectCloseModal({
    open,
    project,
    unfinishedTasks = [],
    isExecutive = false,
    onClose,
    onSuccess,
}: ProjectCloseModalProps) {
    const [closureReason, setClosureReason] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    if (!open || !project) return null;

    const handleCloseModal = () => {
        if (isSubmitting) return;
        setClosureReason('');
        setErrorMsg('');
        onClose();
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setErrorMsg('');

        const trimmedReason = closureReason.trim();

        // Kiểm tra thẩm quyền và điều kiện lý do
        if (isExecutive && trimmedReason.length < 10) {
            setErrorMsg('Đóng dự án cấp Ban giám đốc bắt buộc phải nhập lý do chi tiết (tối thiểu 10 ký tự).');
            return;
        }

        if (trimmedReason.length > 500) {
            setErrorMsg('Lý do đóng dự án không được vượt quá 500 ký tự.');
            return;
        }

        // Cảnh báo công việc dở dang
        if (unfinishedTasks.length > 0) {
            setErrorMsg(`Không thể đóng dự án vì còn ${unfinishedTasks.length} công việc chưa hoàn thành (DONE hoặc CANCELLED).`);
            return;
        }

        setIsSubmitting(true);
        try {
            const result = await closeProject(project.id, trimmedReason || undefined);
            setClosureReason('');
            onSuccess(result);
            onClose();
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Có lỗi khi đóng dự án';
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
                            <Lock className="h-5 w-5" />
                        </div>
                        <div>
                            <h2 className="text-base font-bold text-slate-900">Đóng dự án</h2>
                            <p className="text-xs text-rose-700 font-medium">NCL-03-CN-004 & Quy tắc QTN-08</p>
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

                    {/* QTN-08 Warning Box */}
                    <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-3 text-amber-900 space-y-1.5">
                        <div className="flex items-center gap-1.5 font-bold text-amber-950">
                            <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
                            <span>Lưu ý quan trọng (Quy tắc QTN-08)</span>
                        </div>
                        <p className="text-[11px] leading-relaxed text-amber-800">
                            Khi dự án ở trạng thái <strong>ĐÃ ĐÓNG (CLOSED)</strong>, toàn bộ cây WBS và phân bổ nguồn lực sẽ bị khóa.
                            Hệ thống sẽ <strong>chặn tạo công việc mới</strong> và <strong>không cho phép điều chỉnh giờ phân bổ</strong> vào dự án này.
                        </p>
                    </div>

                    {/* Unfinished tasks alert if any */}
                    {unfinishedTasks.length > 0 && (
                        <div className="rounded-xl border border-rose-200 bg-rose-50/80 p-3 text-rose-900 space-y-1.5">
                            <div className="flex items-center gap-1.5 font-bold text-rose-800">
                                <AlertCircle className="h-4 w-4 text-rose-600 shrink-0" />
                                <span>Còn {unfinishedTasks.length} công việc chưa hoàn thành</span>
                            </div>
                            <p className="text-[11px] text-rose-700">
                                Backend yêu cầu tất cả các công việc trong dự án phải ở trạng thái <strong>Hoàn thành</strong> hoặc <strong>Đã hủy</strong> trước khi đóng:
                            </p>
                            <ul className="max-h-24 overflow-y-auto space-y-1 pl-4 list-disc text-[11px] text-rose-800">
                                {unfinishedTasks.slice(0, 5).map((t, idx) => (
                                    <li key={idx} className="truncate">
                                        {t.code ? <span className="font-mono font-bold mr-1">[{t.code}]</span> : null}
                                        {t.name}
                                    </li>
                                ))}
                                {unfinishedTasks.length > 5 && (
                                    <li className="italic text-rose-600">
                                        ...và {unfinishedTasks.length - 5} công việc khác
                                    </li>
                                )}
                            </ul>
                        </div>
                    )}

                    {/* Closure Reason Input */}
                    <div>
                        <label className="mb-1 block font-semibold text-slate-700">
                            Lý do đóng dự án {isExecutive ? <span className="text-rose-500">* (Bắt buộc tối thiểu 10 ký tự)</span> : <span className="text-slate-400 font-normal">(Tùy chọn)</span>}
                        </label>
                        <textarea
                            rows={3}
                            value={closureReason}
                            onChange={(e) => setClosureReason(e.target.value)}
                            maxLength={500}
                            placeholder={
                                isExecutive
                                    ? 'Nhập lý do đóng dự án cấp Ban giám đốc (tối thiểu 10 ký tự)...'
                                    : 'Nhập lý do đóng dự án (VD: Đã hoàn tất nghiệm thu và bàn giao khách hàng)...'
                            }
                            className="w-full rounded-xl border border-slate-300 p-2.5 text-xs font-medium text-slate-800 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                        />
                        <div className="mt-1 flex justify-between text-[10px] text-slate-400">
                            <span>{isExecutive ? 'Bắt buộc từ 10 đến 500 ký tự' : 'Tối đa 500 ký tự'}</span>
                            <span>{closureReason.length}/500</span>
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
                            disabled={isSubmitting || (isExecutive && closureReason.trim().length < 10)}
                            className="inline-flex items-center gap-2 rounded-xl bg-rose-600 px-4 py-2 font-bold text-white shadow-md shadow-rose-200 hover:bg-rose-700 transition active:scale-95 disabled:opacity-50 disabled:pointer-events-none cursor-pointer"
                        >
                            {isSubmitting ? (
                                <>
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                    <span>Đang xử lý...</span>
                                </>
                            ) : (
                                <>
                                    <Lock className="h-4 w-4" />
                                    <span>Xác nhận đóng dự án</span>
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
