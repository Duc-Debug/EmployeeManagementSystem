import { useState, useEffect } from 'react';
import { X, Sliders, AlertTriangle } from 'lucide-react';
import type { ProjectMember } from './projectData';

interface ProjectAdjustHoursModalProps {
    open: boolean;
    member: ProjectMember | null;
    weekKey: string;
    weekLabel: string;
    monthName: string;
    onClose: () => void;
    onSave: (memberId: string, weekKey: string, newHours: number) => void;
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
    const [hours, setHours] = useState(35);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);

    useEffect(() => {
        if (member && weekKey) {
            setHours(member.weeklyHours[weekKey] ?? 0);
            setErrorMsg(null);
        }
    }, [open, member, weekKey]);

    if (!open || !member) return null;

    const pct = Math.round((hours / (member.capacity || 40)) * 100);
    const isOverloaded = hours > 40;

    const handleApply = () => {
        if (hours < 0) {
            setErrorMsg('Số giờ phân bổ không được là số âm (TC-03).');
            return;
        }
        onSave(member.id, weekKey, hours);
        onClose();
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
            <div className="w-full max-w-sm overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4">
                    <h3 className="flex items-center gap-2 text-sm font-bold text-slate-800">
                        <Sliders className="h-4 w-4 text-indigo-600" />
                        Điều Chỉnh Giờ Phân Bổ
                    </h3>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition cursor-pointer"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                {/* Body */}
                <div className="p-5 space-y-4 text-xs">
                    <div>
                        <p className="text-slate-500">
                            Nhân sự: <strong className="text-slate-800">{member.name} ({member.role})</strong>
                        </p>
                        <p className="text-slate-500">
                            Thời gian: <strong className="font-semibold text-indigo-600">{weekLabel || weekKey} - {monthName} (Chuẩn: 40h/tuần)</strong>
                        </p>
                    </div>

                    <div>
                        <div className="mb-2 flex items-center justify-between">
                            <label className="font-semibold text-slate-700">Tổng số giờ được giao:</label>
                            <div className="flex items-center gap-1.5">
                                <input
                                    type="number"
                                    min="0"
                                    max="100"
                                    value={hours}
                                    onChange={(e) => {
                                        const val = Number(e.target.value);
                                        setHours(val);
                                        if (val < 0) {
                                            setErrorMsg('Số giờ phân bổ không được là số âm (TC-03).');
                                        } else {
                                            setErrorMsg(null);
                                        }
                                    }}
                                    className="w-16 rounded-md border border-slate-300 px-2 py-1 text-right text-xs font-bold text-slate-800 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 outline-none"
                                />
                                <span className="text-xs font-bold text-slate-600">h</span>
                            </div>
                        </div>

                        <input
                            type="range"
                            min="0"
                            max="60"
                            step="1"
                            value={Math.max(0, hours)}
                            onChange={(e) => {
                                setHours(Number(e.target.value));
                                setErrorMsg(null);
                            }}
                            className="w-full cursor-pointer accent-indigo-600"
                        />
                        <div className="mt-1 flex justify-between text-[10px] text-slate-400">
                            <span>0h</span>
                            <span>20h</span>
                            <span className="font-bold text-slate-600">40h (Chuẩn)</span>
                            <span>50h</span>
                            <span className="font-bold text-rose-500">60h+</span>
                        </div>
                    </div>

                    {errorMsg && (
                        <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 p-2.5 text-[11px] text-rose-700">
                            <AlertTriangle className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" />
                            <span>{errorMsg}</span>
                        </div>
                    )}

                    {isOverloaded && !errorMsg && (
                        <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 p-2.5 text-[11px] text-rose-700">
                            <AlertTriangle className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" />
                            <span>Số giờ vượt quá 40h/tuần ({hours}h - {pct}%). Có nguy cơ quá tải và ảnh hưởng chất lượng.</span>
                        </div>
                    )}

                    <div className="flex items-center justify-end gap-2 border-t border-slate-200 pt-3">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-lg border border-slate-300 bg-white px-3.5 py-1.5 font-medium text-slate-700 hover:bg-slate-100 transition cursor-pointer"
                        >
                            Đóng
                        </button>
                        <button
                            type="button"
                            onClick={handleApply}
                            disabled={!!errorMsg}
                            className="rounded-lg bg-indigo-600 px-3.5 py-1.5 font-medium text-white shadow-xs hover:bg-indigo-700 disabled:opacity-50 transition cursor-pointer"
                        >
                            Áp dụng
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

