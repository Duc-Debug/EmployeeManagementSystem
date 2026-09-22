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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
            <div className="relative w-full max-w-lg rounded-xl bg-white shadow-2xl dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 dark:border-slate-800">
                    <div className="flex items-center gap-2 text-slate-900 dark:text-white">
                        <CheckCircle2 className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
                        <h3 className="font-semibold text-base">Xác Nhận Phương Án Xử Lý Cảnh Báo</h3>
                    </div>
                    <button
                        onClick={onClose}
                        disabled={submitting}
                        className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:hover:bg-slate-800 dark:hover:text-slate-300"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">
                    {error && (
                        <div className="flex items-start gap-2 rounded-lg bg-rose-50 p-3 text-xs text-rose-700 dark:bg-rose-950/40 dark:text-rose-300 border border-rose-200 dark:border-rose-900">
                            <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
                            <span>{error}</span>
                        </div>
                    )}

                    {/* Employee Contract Summary Card */}
                    <div className="rounded-lg bg-slate-50 p-3.5 dark:bg-slate-800/60 border border-slate-100 dark:border-slate-700/60 space-y-1.5 text-xs">
                        <div className="flex justify-between items-center">
                            <span className="font-medium text-slate-600 dark:text-slate-400">Nhân sự thuê ngoài:</span>
                            <span className="font-semibold text-slate-900 dark:text-white">
                                {contract.fullName} ({contract.employeeCode})
                            </span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="font-medium text-slate-600 dark:text-slate-400">Ngày hết hạn hợp đồng:</span>
                            <span className="font-medium text-rose-600 dark:text-rose-400">
                                {contract.contractEndDate} ({contract.daysRemaining < 0 ? `Quá hạn ${Math.abs(contract.daysRemaining)} ngày` : `Còn ${contract.daysRemaining} ngày`})
                            </span>
                        </div>
                        <div className="flex justify-between items-center">
                            <span className="font-medium text-slate-600 dark:text-slate-400">Đơn vị / Chi nhánh:</span>
                            <span className="text-slate-700 dark:text-slate-300">{contract.orgUnitName || "N/A"}</span>
                        </div>
                    </div>

                    {/* Action Note */}
                    <div className="space-y-1.5">
                        <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                            Phương án & Ghi chú xử lý <span className="text-rose-500">*</span>
                        </label>
                        <div className="relative">
                            <FileText className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                            <textarea
                                rows={3}
                                required
                                value={actionNote}
                                onChange={(e) => setActionNote(e.target.value)}
                                placeholder="Ví dụ: Đang làm thủ tục gia hạn hợp đồng thêm 3 tháng, hoặc sẽ chuyển giao dự án trước tuần 42..."
                                className="w-full rounded-lg border border-slate-300 pl-9 pr-3 py-2 text-xs focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                            />
                        </div>
                    </div>

                    {/* Expected Resolution Date */}
                    <div className="space-y-1.5">
                        <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                            Ngày dự kiến giải quyết xong
                        </label>
                        <div className="relative">
                            <Calendar className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                            <input
                                type="date"
                                value={expectedDate}
                                onChange={(e) => setExpectedDate(e.target.value)}
                                className="w-full rounded-lg border border-slate-300 pl-9 pr-3 py-2 text-xs focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                            />
                        </div>
                    </div>

                    {/* Actions */}
                    <div className="flex justify-end gap-2.5 pt-2">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={submitting}
                            className="rounded-lg border border-slate-300 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                        >
                            Hủy bỏ
                        </button>
                        <button
                            type="submit"
                            disabled={submitting || !actionNote.trim()}
                            className="flex items-center gap-1.5 rounded-lg bg-emerald-600 px-4 py-2 text-xs font-medium text-white hover:bg-emerald-700 disabled:opacity-50 dark:bg-emerald-500 dark:hover:bg-emerald-600"
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
