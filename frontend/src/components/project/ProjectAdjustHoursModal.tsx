import { useState, useEffect } from 'react';
import { X, Sliders, AlertTriangle, ShieldAlert, CheckCircle } from 'lucide-react';
import type { ProjectMember } from './projectData';
import { useAuthUser } from '@/lib/auth-session';

interface ProjectAdjustHoursModalProps {
    open: boolean;
    member: ProjectMember | null;
    weekKey: string;
    weekLabel: string;
    monthName: string;
    onClose: () => void;
    onSave: (memberId: string, weekKey: string, newHours: number, overloadReason?: string) => Promise<void> | void;
}

export function ProjectAdjustHoursModal({
    open,
    member,
    weekKey,
    weekLabel,
    monthName,
    onClose,
    onSave,
}: ProjectAdjustHoursModalProps) {
    const authUser = useAuthUser();
    const isResourceManager = authUser?.roleCode === 'VT-03';

    const [hours, setHours] = useState(35);
    const [overloadReason, setOverloadReason] = useState('');
    const [reasonError, setReasonError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        if (member && weekKey) {
            setHours(member.weeklyHours[weekKey] ?? 0);
            setOverloadReason('');
            setReasonError(null);
            setIsSubmitting(false);
        }
    }, [open, member, weekKey]);

    if (!open || !member) return null;

    const capacity = member.capacity || 40;
    const pct = Math.round((hours / capacity) * 100);
    const isOverloaded = hours > capacity;
    const overloadHours = isOverloaded ? hours - capacity : 0;

    const handleApply = async () => {
        if (isOverloaded) {
            if (!isResourceManager) {
                setReasonError('Chỉ Quản lý nguồn lực (RM) mới có quyền phê duyệt phân bổ vượt năng lực.');
                return;
            }
            if (!overloadReason.trim()) {
                setReasonError('Vui lòng nhập lý do chấp nhận quá tải (QTN-11).');
                return;
            }
        }

        setReasonError(null);
        setIsSubmitting(true);
        try {
            await onSave(member.id, weekKey, hours, isOverloaded ? overloadReason.trim() : undefined);
            onClose();
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Đã xảy ra lỗi khi lưu phân bổ';
            setReasonError(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
            <div className="w-full max-w-md overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4">
                    <h3 className="flex items-center gap-2 text-sm font-bold text-slate-800">
                        <Sliders className="h-4 w-4 text-indigo-600" />
                        Điều Chỉnh Giờ Phân Bổ (NCL-06-CN-003)
                    </h3>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                {/* Body */}
                <div className="p-5 space-y-4 text-xs">
                    <div className="rounded-lg bg-slate-50 p-3 border border-slate-200/80 space-y-1">
                        <p className="text-slate-600">
                            Nhân sự: <strong className="text-slate-900">{member.name} ({member.role})</strong>
                        </p>
                        <p className="text-slate-600">
                            Thời gian: <strong className="font-semibold text-indigo-700">{weekLabel || weekKey} - {monthName}</strong>
                        </p>
                        <p className="text-slate-500 text-[11px]">
                            Năng lực khả dụng: <span className="font-bold text-slate-700">{capacity}h/tuần</span>
                        </p>
                    </div>

                    <div>
                        <div className="mb-1 flex items-center justify-between">
                            <label className="font-semibold text-slate-700">Tổng số giờ được giao:</label>
                            <span
                                className={`rounded px-2 py-0.5 text-[11px] font-bold ${
                                    isOverloaded
                                        ? 'bg-rose-100 text-rose-700 ring-1 ring-rose-300'
                                        : hours >= 25
                                        ? 'bg-emerald-100 text-emerald-700'
                                        : 'bg-slate-100 text-slate-700'
                                }`}
                            >
                                {hours}h ({pct}%)
                            </span>
                        </div>
                        <input
                            type="range"
                            min="0"
                            max="60"
                            step="1"
                            value={hours}
                            onChange={(e) => {
                                setHours(Number(e.target.value));
                                setReasonError(null);
                            }}
                            className="w-full cursor-pointer accent-indigo-600"
                        />
                        <div className="mt-1 flex justify-between text-[10px] text-slate-400">
                            <span>0h</span>
                            <span>20h</span>
                            <span className="font-bold text-slate-600">{capacity}h (Chuẩn)</span>
                            <span>50h</span>
                            <span className="font-bold text-rose-500">60h</span>
                        </div>
                    </div>

                    {/* Overload Warning Box */}
                    {isOverloaded && (
                        <div className="space-y-3 rounded-xl border border-rose-200 bg-rose-50/90 p-3.5 text-[11px] text-rose-800 animate-in fade-in">
                            <div className="flex items-start gap-2 font-semibold">
                                <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
                                <div>
                                    <p className="text-rose-900 font-bold">
                                        Cảnh báo quá tải: Vượt {overloadHours} giờ so với khả dụng!
                                    </p>
                                    <p className="text-[10px] font-normal text-rose-700 mt-0.5">
                                        Tổng giờ phân bổ ({hours}h) lớn hơn số giờ khả dụng ({capacity}h) của tuần {weekLabel || weekKey}.
                                    </p>
                                </div>
                            </div>

                            {/* Case 1: RM Role -> Show Overload Reason Input */}
                            {isResourceManager ? (
                                <div className="space-y-1.5 pt-1 border-t border-rose-200/80">
                                    <label htmlFor="overloadReason" className="font-bold text-rose-900 block">
                                        Lý do chấp nhận vượt tải <span className="text-rose-600">*</span> (QTN-11):
                                    </label>
                                    <textarea
                                        id="overloadReason"
                                        rows={2}
                                        value={overloadReason}
                                        onChange={(e) => {
                                            setOverloadReason(e.target.value);
                                            setReasonError(null);
                                        }}
                                        placeholder="Nhập lý do chấp nhận phân bổ vượt giờ (ví dụ: Tiến độ gấp giai đoạn bàn giao...)"
                                        className="w-full rounded-lg border border-rose-300 bg-white p-2 text-xs text-slate-800 placeholder:text-slate-400 focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500"
                                    />
                                </div>
                            ) : (
                                /* Case 2: Non-RM Role (e.g. PM VT-02) -> Disabled with Notice */
                                <div className="flex items-start gap-2 rounded-lg bg-white/70 p-2.5 border border-rose-200 text-rose-700 text-[10px]">
                                    <ShieldAlert className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
                                    <span>
                                        <strong>Bạn không có quyền duyệt vượt tải:</strong> Chỉ Quản lý nguồn lực (RM) mới có thể xác nhận lưu phân bổ quá tải. Vui lòng giảm số giờ hoặc liên hệ RM.
                                    </span>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Error Message */}
                    {reasonError && (
                        <div className="rounded-lg bg-rose-100 p-2.5 text-[11px] font-medium text-rose-800 border border-rose-200">
                            {reasonError}
                        </div>
                    )}

                    <div className="flex items-center justify-end gap-2 border-t border-slate-200 pt-3">
                        <button
                            type="button"
                            onClick={onClose}
                            disabled={isSubmitting}
                            className="rounded-lg border border-slate-300 bg-white px-3.5 py-1.5 font-medium text-slate-700 hover:bg-slate-100 transition disabled:opacity-50"
                        >
                            Đóng
                        </button>
                        <button
                            type="button"
                            onClick={handleApply}
                            disabled={isSubmitting || (isOverloaded && !isResourceManager)}
                            className={`rounded-lg px-4 py-1.5 font-medium text-white shadow-xs transition flex items-center gap-1.5 ${
                                isOverloaded && !isResourceManager
                                    ? 'bg-slate-300 cursor-not-allowed text-slate-500'
                                    : isOverloaded
                                    ? 'bg-amber-600 hover:bg-amber-700'
                                    : 'bg-indigo-600 hover:bg-indigo-700'
                            } disabled:opacity-50`}
                        >
                            {isOverloaded ? (
                                <>
                                    <CheckCircle className="h-3.5 w-3.5" />
                                    <span>{isResourceManager ? 'Xác Nhận Vượt Tải & Lưu' : 'Bị Khóa (Chờ RM)'}</span>
                                </>
                            ) : (
                                <span>Áp Dụng</span>
                            )}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

