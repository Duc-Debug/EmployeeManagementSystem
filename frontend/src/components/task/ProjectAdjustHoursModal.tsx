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

    useEffect(() => {
        if (member && weekKey) {
            setHours(member.weeklyHours[weekKey] ?? 0);
        }
    }, [open, member, weekKey]);

    if (!open || !member) return null;

    const pct = Math.round((hours / (member.capacity || 40)) * 100);
    const isOverloaded = hours > 40;

    const handleApply = () => {
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
                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
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
                        <div className="mb-1 flex items-center justify-between">
                            <label className="font-semibold text-slate-700">Tổng số giờ được giao:</label>
                            <span
                                className={`rounded px-2 py-0.5 text-[11px] font-bold ${
                                    isOverloaded
                                        ? 'bg-rose-100 text-rose-700'
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
                            step="2"
                            value={hours}
                            onChange={(e) => setHours(Number(e.target.value))}
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

                    {isOverloaded && (
                        <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 p-2.5 text-[11px] text-rose-700">
                            <AlertTriangle className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" />
                            <span>Số giờ vượt quá 40h/tuần. Có nguy cơ quá tải và ảnh hưởng chất lượng.</span>
                        </div>
                    )}

                    <div className="flex items-center justify-end gap-2 border-t border-slate-200 pt-3">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-lg border border-slate-300 bg-white px-3.5 py-1.5 font-medium text-slate-700 hover:bg-slate-100 transition"
                        >
                            Đóng
                        </button>
                        <button
                            type="button"
                            onClick={handleApply}
                            className="rounded-lg bg-indigo-600 px-3.5 py-1.5 font-medium text-white shadow-xs hover:bg-indigo-700 transition"
                        >
                            Áp dụng
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

