import { Check, LoaderCircle, ShieldCheck, X } from 'lucide-react';
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
    onApprove: (id: number) => void;
    loading?: boolean;
    error?: string | null;
    approvingId?: number | null;
}

export default function SkillApproveTable({ requests, onApprove, loading = false, error, approvingId }: SkillApproveTableProps) {
    const pendingCount = requests.filter((r) => r.status === 'pending').length;

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
            {error && <p className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-semibold text-rose-700">{error}</p>}
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
                            {loading ? (
                                <tr><td colSpan={6} className="py-12 text-center text-slate-500"><LoaderCircle className="mr-2 inline h-4 w-4 animate-spin" />Đang tải dữ liệu...</td></tr>
                            ) : requests.length === 0 ? (
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
                                                        disabled={approvingId === req.id}
                                                        className="inline-flex items-center gap-1.5 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700 transition hover:bg-emerald-100 hover:border-emerald-300 active:scale-95 cursor-pointer"
                                                    >
                                                        {approvingId === req.id ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" /> : <Check className="h-3.5 w-3.5" />}
                                                        {approvingId === req.id ? 'Đang duyệt...' : 'Duyệt'}
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

        </div>
    );
}

