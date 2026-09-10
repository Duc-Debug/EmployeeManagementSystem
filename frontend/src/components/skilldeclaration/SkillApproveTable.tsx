import { useState } from 'react';
import { Check, X, ShieldCheck, SlidersHorizontal, AlertCircle } from 'lucide-react';
import type { PendingApprovalSkill } from './Types.ts';

/* ── Level badge ─────────────────────────────────────────── */
const LEVEL_STYLES: Record<number, { bg: string; text: string; border: string }> = {
    1: { bg: 'bg-slate-100',   text: 'text-slate-700',   border: 'border-slate-300' },
    2: { bg: 'bg-blue-50',    text: 'text-blue-700',    border: 'border-blue-200'  },
    3: { bg: 'bg-emerald-50', text: 'text-emerald-700', border: 'border-emerald-200' },
    4: { bg: 'bg-amber-50',   text: 'text-amber-700',   border: 'border-amber-200' },
    5: { bg: 'bg-rose-50',    text: 'text-rose-700',    border: 'border-rose-200'  },
};

function LevelBadge({ level }: { level: number }) {
    const s = LEVEL_STYLES[level] ?? LEVEL_STYLES[1];
    return (
        <span
            className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-bold ${s.bg} ${s.text} ${s.border}`}
        >
            Level {level}
        </span>
    );
}

/* ── Category badge ──────────────────────────────────────── */
const CAT_STYLES: Record<string, { bg: string; text: string }> = {
    Frontend: { bg: 'bg-violet-50',  text: 'text-violet-700' },
    Backend:  { bg: 'bg-sky-50',     text: 'text-sky-700'    },
    DevOps:   { bg: 'bg-orange-50',  text: 'text-orange-700' },
    Database: { bg: 'bg-teal-50',    text: 'text-teal-700'   },
    Khác:     { bg: 'bg-slate-100',  text: 'text-slate-600'  },
};

function CategoryBadge({ cat }: { cat: string }) {
    const s = CAT_STYLES[cat] ?? CAT_STYLES['Khác'];
    return (
        <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-semibold ${s.bg} ${s.text}`}>
            {cat}
        </span>
    );
}

/* ── Status badge (cho hàng đã xử lý) ───────────────────── */
function StatusBadge({ status }: { status: 'approved' | 'rejected' }) {
    if (status === 'approved') {
        return (
            <span className="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700">
                <Check className="h-3 w-3" /> Đã duyệt
            </span>
        );
    }
    return (
        <span className="inline-flex items-center gap-1 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-0.5 text-xs font-semibold text-rose-700">
            <X className="h-3 w-3" /> Đã từ chối
        </span>
    );
}

/* ── Empty state ─────────────────────────────────────────── */
function EmptyState() {
    return (
        <tr>
            <td colSpan={6}>
                <div className="flex flex-col items-center justify-center gap-3 py-16 text-slate-400">
                    <ShieldCheck className="h-10 w-10 opacity-30" />
                    <p className="text-sm font-medium">Không có yêu cầu chờ duyệt</p>
                    <p className="text-xs">Tất cả kỹ năng đã được xử lý.</p>
                </div>
            </td>
        </tr>
    );
}

/* ── Main component ──────────────────────────────────────── */
interface SkillApproveTableProps {
    requests: PendingApprovalSkill[];
    onApprove: (id: number, adjustedLevel?: number, notes?: string) => void;
    onReject: (id: number, reason?: string) => void;
}

export default function SkillApproveTable({ requests, onApprove, onReject }: SkillApproveTableProps) {
    const pendingCount = requests.filter((r) => r.status === 'pending').length;

    // Modal Điều chỉnh / Phê duyệt
    const [adjustModalTarget, setAdjustModalTarget] = useState<PendingApprovalSkill | null>(null);
    const [adjustedLevel, setAdjustedLevel] = useState<number>(3);
    const [reviewNotes, setReviewNotes] = useState<string>('');

    // Modal Từ chối
    const [rejectModalTarget, setRejectModalTarget] = useState<PendingApprovalSkill | null>(null);
    const [rejectionReason, setRejectionReason] = useState<string>('');

    const handleOpenAdjustModal = (req: PendingApprovalSkill) => {
        setAdjustModalTarget(req);
        setAdjustedLevel(req.level);
        setReviewNotes('');
    };

    const handleConfirmApprove = () => {
        if (!adjustModalTarget) return;
        onApprove(adjustModalTarget.id, adjustedLevel, reviewNotes);
        setAdjustModalTarget(null);
    };

    const handleOpenRejectModal = (req: PendingApprovalSkill) => {
        setRejectModalTarget(req);
        setRejectionReason('');
    };

    const handleConfirmReject = () => {
        if (!rejectModalTarget) return;
        onReject(rejectModalTarget.id, rejectionReason);
        setRejectModalTarget(null);
    };

    return (
        <div className="space-y-5">
            {/* ── Header ── */}
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                    <h2 className="text-lg font-bold text-slate-800">
                        Phê duyệt Kỹ năng Khai báo từ Nhân sự
                    </h2>
                    <p className="mt-0.5 text-xs text-slate-500">
                        Danh sách yêu cầu xác thực mức độ thành thạo và kinh nghiệm từ các thành viên trong bộ phận.
                    </p>
                </div>

                {pendingCount > 0 && (
                    <span className="inline-flex shrink-0 items-center gap-1.5 self-start rounded-full border border-amber-300 bg-amber-50 px-3 py-1.5 text-xs font-bold text-amber-700 shadow-xs">
                        <span className="h-2 w-2 animate-pulse rounded-full bg-amber-400" />
                        {pendingCount} Yêu cầu chờ duyệt
                    </span>
                )}
            </div>

            {/* ── Table ── */}
            <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-2xs">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="border-b border-slate-100 bg-slate-50 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
                                <th className="px-4 py-3 text-left">Nhân viên</th>
                                <th className="px-4 py-3 text-left">Kỹ năng khai báo</th>
                                <th className="px-4 py-3 text-left">Phân loại</th>
                                <th className="px-4 py-3 text-left">Mức đề xuất</th>
                                <th className="px-4 py-3 text-left">Kinh nghiệm</th>
                                <th className="px-4 py-3 text-left">Xử lý phê duyệt</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {requests.length === 0 ? (
                                <EmptyState />
                            ) : (
                                requests.map((req) => (
                                    <tr key={req.id} className="transition-colors hover:bg-slate-50/60">
                                        {/* Nhân viên */}
                                        <td className="px-4 py-3">
                                            <span className="font-semibold text-slate-800">{req.employeeName}</span>
                                        </td>

                                        {/* Kỹ năng */}
                                        <td className="px-4 py-3">
                                            <span className="font-medium text-indigo-600">{req.skillName}</span>
                                        </td>

                                        {/* Phân loại */}
                                        <td className="px-4 py-3">
                                            <CategoryBadge cat={req.category} />
                                        </td>

                                        {/* Mức đề xuất */}
                                        <td className="px-4 py-3">
                                            <LevelBadge level={req.level} />
                                        </td>

                                        {/* Kinh nghiệm */}
                                        <td className="px-4 py-3">
                                            <span className="text-slate-600 text-xs">{req.years} năm</span>
                                        </td>

                                        {/* Xử lý */}
                                        <td className="px-4 py-3">
                                            {req.status === 'pending' ? (
                                                <div className="flex items-center gap-1.5">
                                                    <button
                                                        type="button"
                                                        onClick={() => onApprove(req.id)}
                                                        className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700 transition hover:bg-emerald-100 hover:border-emerald-300 active:scale-95 cursor-pointer"
                                                        title="Duyệt nhanh giữ nguyên mức tự khai"
                                                    >
                                                        <Check className="h-3.5 w-3.5" />
                                                        Duyệt
                                                    </button>
                                                    <button
                                                        type="button"
                                                        onClick={() => handleOpenAdjustModal(req)}
                                                        className="inline-flex items-center gap-1 rounded-lg border border-indigo-200 bg-indigo-50 px-2 py-1 text-xs font-semibold text-indigo-700 transition hover:bg-indigo-100 hover:border-indigo-300 active:scale-95 cursor-pointer"
                                                        title="Điều chỉnh mức thành thạo & thêm ghi chú"
                                                    >
                                                        <SlidersHorizontal className="h-3 w-3" />
                                                        Điều chỉnh
                                                    </button>
                                                    <button
                                                        type="button"
                                                        onClick={() => handleOpenRejectModal(req)}
                                                        className="inline-flex items-center gap-1 rounded-lg border border-rose-200 bg-rose-50 px-2.5 py-1 text-xs font-semibold text-rose-700 transition hover:bg-rose-100 hover:border-rose-300 active:scale-95 cursor-pointer"
                                                    >
                                                        <X className="h-3.5 w-3.5" />
                                                        Từ chối
                                                    </button>
                                                </div>
                                            ) : (
                                                <StatusBadge status={req.status} />
                                            )}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* ── Modal Điều chỉnh & Xác nhận mức thành thạo ── */}
            {adjustModalTarget && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-xs animate-in fade-in">
                    <div className="relative w-full max-w-md rounded-2xl border border-slate-100 bg-white p-6 shadow-2xl space-y-4">
                        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                            <div>
                                <h3 className="text-base font-bold text-slate-900">
                                    Xác nhận / Điều chỉnh mức thành thạo
                                </h3>
                                <p className="text-xs text-slate-500">
                                    Nhân viên: <strong className="text-slate-800">{adjustModalTarget.employeeName}</strong> – Kỹ năng: <strong className="text-indigo-600">{adjustModalTarget.skillName}</strong>
                                </p>
                            </div>
                            <button
                                type="button"
                                onClick={() => setAdjustModalTarget(null)}
                                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 cursor-pointer"
                            >
                                <X className="h-4 w-4" />
                            </button>
                        </div>

                        <div className="space-y-3">
                            <div>
                                <label className="mb-1 block text-xs font-semibold text-slate-600">
                                    Mức thành thạo (Tự khai: Level {adjustModalTarget.level})
                                </label>
                                <div className="flex gap-2">
                                    {[1, 2, 3, 4, 5].map((lvl) => (
                                        <button
                                            key={lvl}
                                            type="button"
                                            onClick={() => setAdjustedLevel(lvl)}
                                            className={`flex-1 rounded-xl py-2 text-xs font-bold transition border cursor-pointer ${
                                                adjustedLevel === lvl
                                                    ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                                                    : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                                            }`}
                                        >
                                            Level {lvl}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div>
                                <label className="mb-1 block text-xs font-semibold text-slate-600">
                                    Ghi chú đánh giá / Lý do điều chỉnh {adjustedLevel !== adjustModalTarget.level && <span className="text-rose-500">*</span>}
                                </label>
                                <textarea
                                    rows={3}
                                    value={reviewNotes}
                                    onChange={(e) => setReviewNotes(e.target.value)}
                                    placeholder="Nhập nhận xét hoặc kết quả phỏng vấn / bài test chuyên môn..."
                                    className="w-full rounded-xl border border-slate-200 bg-slate-50 p-3 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                                />
                            </div>
                        </div>

                        <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                            <button
                                type="button"
                                onClick={() => setAdjustModalTarget(null)}
                                className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 cursor-pointer"
                            >
                                Hủy
                            </button>
                            <button
                                type="button"
                                onClick={handleConfirmApprove}
                                className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 shadow-xs cursor-pointer"
                            >
                                Xác nhận phê duyệt
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* ── Modal Xác nhận Từ chối ── */}
            {rejectModalTarget && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-xs animate-in fade-in">
                    <div className="relative w-full max-w-md rounded-2xl border border-slate-100 bg-white p-6 shadow-2xl space-y-4">
                        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                            <div className="flex items-center gap-2 text-rose-600">
                                <AlertCircle className="h-5 w-5" />
                                <h3 className="text-base font-bold text-slate-900">
                                    Từ chối yêu cầu kỹ năng
                                </h3>
                            </div>
                            <button
                                type="button"
                                onClick={() => setRejectModalTarget(null)}
                                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 cursor-pointer"
                            >
                                <X className="h-4 w-4" />
                            </button>
                        </div>

                        <p className="text-xs text-slate-600">
                            Bạn đang từ chối kỹ năng <strong className="text-indigo-600">"{rejectModalTarget.skillName}"</strong> của nhân viên <strong className="text-slate-900">{rejectModalTarget.employeeName}</strong>.
                        </p>

                        <div>
                            <label className="mb-1 block text-xs font-semibold text-slate-600">
                                Lý do từ chối (tùy chọn)
                            </label>
                            <textarea
                                rows={3}
                                value={rejectionReason}
                                onChange={(e) => setRejectionReason(e.target.value)}
                                placeholder="VD: Chưa có đủ chứng chỉ hoặc dự án thực tế chứng minh..."
                                className="w-full rounded-xl border border-slate-200 bg-slate-50 p-3 text-xs font-medium text-slate-800 outline-none focus:border-rose-500 focus:bg-white focus:ring-2 focus:ring-rose-100"
                            />
                        </div>

                        <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                            <button
                                type="button"
                                onClick={() => setRejectModalTarget(null)}
                                className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 cursor-pointer"
                            >
                                Hủy
                            </button>
                            <button
                                type="button"
                                onClick={handleConfirmReject}
                                className="rounded-xl bg-rose-600 px-4 py-2 text-xs font-semibold text-white hover:bg-rose-700 shadow-xs cursor-pointer"
                            >
                                Xác nhận từ chối
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
