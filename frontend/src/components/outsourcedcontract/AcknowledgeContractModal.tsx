import { useState } from "react";
import { X, CheckCircle2, Calendar, FileText, AlertTriangle, Loader2 } from "lucide-react";
import type { ExpiringOutsourcedContract } from "@/lib/api/outsourced-contracts";
import { acknowledgeOutsourcedContractWarning } from "@/lib/api/outsourced-contracts";

interface AcknowledgeContractModalProps {
    contract: ExpiringOutsourcedContract;
    isOpen: boolean;
    onClose: () => void;
    onSuccess: (employeeId: number, message: string) => void;
}

export default function AcknowledgeContractModal({
    contract,
    isOpen,
    onClose,
    onSuccess,
}: AcknowledgeContractModalProps) {
    const [actionNote, setActionNote] = useState<string>("");
    const [expectedDate, setExpectedDate] = useState<string>("");
    const [submitting, setSubmitting] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSubmitting(true);
        setError(null);

        try {
            const res = await acknowledgeOutsourcedContractWarning(contract.employeeId, {
                actionNote: actionNote.trim() || "Đã xác nhận phương án theo dõi thời hạn hợp đồng",
                expectedResolutionDate: expectedDate || null,
            });
            onSuccess(contract.employeeId, res.message || "Đã ghi nhận phương án xử lý thành công.");
            onClose();
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : "Có lỗi xảy ra khi xác nhận phương án.";
            setError(msg);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-in fade-in duration-150">
            <div className="relative w-full max-w-lg rounded-2xl bg-white shadow-2xl dark:bg-slate-900 border border-slate-200 dark:border-slate-800 animate-in zoom-in-95 duration-150 overflow-hidden">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 dark:border-slate-800 bg-slate-50/50">
                    <div className="flex items-center gap-2 text-slate-900 dark:text-white">
                        <CheckCircle2 className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
                        <h3 className="font-bold text-base">Xác Nhận Phương Án Xử Lý Cảnh Báo</h3>
                    </div>
                    <button
                        onClick={onClose}
                        disabled={submitting}
                        className="rounded-xl p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-300 transition"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">
                    {error && (
                        <div className="flex items-start gap-2 rounded-xl bg-rose-50 p-3 text-xs text-rose-700 dark:bg-rose-950/40 dark:text-rose-300 border border-rose-200 dark:border-rose-900">
                            <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
                            <span>{error}</span>
                        </div>
                    )}

                    {/* Employee Contract Summary Card */}
                    <div className="rounded-xl bg-slate-50/80 p-4 dark:bg-slate-800/60 border border-slate-200/80 dark:border-slate-700/60 space-y-2 text-xs shadow-2xs">
                        <div className="flex justify-between items-center">
                            <span className="font-medium text-slate-500 dark:text-slate-400">Nhân sự thuê ngoài:</span>
                            <span className="font-bold text-slate-900 dark:text-white">
                                {contract.fullName} <span className="font-mono text-[11px] text-slate-500 font-normal">({contract.employeeCode})</span>
                            </span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="font-medium text-slate-500 dark:text-slate-400">Ngày hết hạn hợp đồng:</span>
                            <span className="font-semibold text-rose-600 dark:text-rose-400">
                                {contract.contractEndDate} ({contract.daysRemaining < 0 ? `Quá hạn ${Math.abs(contract.daysRemaining)} ngày` : `Còn ${contract.daysRemaining} ngày`})
                            </span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="font-medium text-slate-500 dark:text-slate-400">Đơn vị / Chi nhánh:</span>
                            <span className="text-slate-700 dark:text-slate-300 font-medium">{contract.orgUnitName || "N/A"}</span>
                        </div>
                    </div>

                    {/* Action Note */}
                    <div className="space-y-1.5">
                        <label className="flex items-center gap-1.5 text-xs font-semibold text-slate-700 dark:text-slate-300">
                            <FileText className="h-3.5 w-3.5 text-indigo-500" />
                            <span>Phương án & Ghi chú xử lý</span>
                            <span className="text-rose-500">*</span>
                        </label>
                        <textarea
                            rows={3}
                            required
                            value={actionNote}
                            onChange={(e) => setActionNote(e.target.value)}
                            placeholder="Ví dụ: Đang làm thủ tục gia hạn hợp đồng thêm 3 tháng, hoặc sẽ chuyển giao dự án trước tuần 42..."
                            className="w-full rounded-xl border border-slate-200 p-3 text-xs focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-100 dark:border-slate-700 dark:bg-slate-800 dark:text-white transition shadow-2xs"
                        />
                    </div>

                    {/* Expected Resolution Date */}
                    <div className="space-y-1.5">
                        <label className="flex items-center gap-1.5 text-xs font-semibold text-slate-700 dark:text-slate-300">
                            <Calendar className="h-3.5 w-3.5 text-indigo-500" />
                            <span>Ngày dự kiến giải quyết xong</span>
                        </label>
                        <input
                            type="date"
                            value={expectedDate}
                            onChange={(e) => setExpectedDate(e.target.value)}
                            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-100 dark:border-slate-700 dark:bg-slate-800 dark:text-white transition shadow-2xs"
                        />
                    </div>

                    {/* Actions */}
                    <div className="flex justify-end gap-2.5 pt-2">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={submitting}
                            className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800 transition"
                        >
                            Hủy bỏ
                        </button>
                        <button
                            type="submit"
                            disabled={submitting || !actionNote.trim()}
                            className="flex items-center gap-1.5 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-50 dark:bg-emerald-500 dark:hover:bg-emerald-600 transition shadow-xs"
                        >
                            {submitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                            Lưu Phương Án
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
