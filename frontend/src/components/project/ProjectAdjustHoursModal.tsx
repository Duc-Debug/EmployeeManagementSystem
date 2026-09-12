import { useState, useEffect } from 'react';
import { X, Sliders, AlertTriangle, Percent, Clock } from 'lucide-react';
import type { ProjectMember } from './projectData';

interface ProjectAdjustHoursModalProps {
    open: boolean;
    member: ProjectMember | null;
    weekKey: string;
    weekLabel: string;
    monthName: string;
    onClose: () => void;
    onSave: (memberId: string, weekKey: string, newHours: number, percentage?: number) => void;
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
    const [mode, setMode] = useState<'HOURS' | 'PERCENTAGE'>('PERCENTAGE');
    const [hours, setHours] = useState(20);
    const [percentage, setPercentage] = useState(50);

    const availableHours = member?.capacity || 40;

    useEffect(() => {
        if (member && weekKey) {
            const currentHours = member.weeklyHours[weekKey] ?? 0;
            setHours(currentHours);
            if (availableHours > 0) {
                setPercentage(Math.round((currentHours / availableHours) * 100));
            } else {
                setPercentage(0);
            }
        }
    }, [open, member, weekKey, availableHours]);

    if (!open || !member) return null;

    const handlePercentageChange = (newPct: number) => {
        const clampedPct = Math.max(0, Math.min(100, newPct));
        setPercentage(clampedPct);
        const calculatedHours = Number(((availableHours * clampedPct) / 100).toFixed(2));
        setHours(calculatedHours);
    };

    const handleHoursChange = (newHours: number) => {
        const clampedHours = Math.max(0, newHours);
        setHours(clampedHours);
        if (availableHours > 0) {
            setPercentage(Math.round((clampedHours / availableHours) * 100));
        }
    };

    const isOverloaded = hours > availableHours;

    const handleApply = () => {
        onSave(member.id, weekKey, hours, percentage);
        onClose();
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
            <div className="w-full max-w-sm overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4">
                    <h3 className="flex items-center gap-2 text-sm font-bold text-slate-800">
                        <Sliders className="h-4 w-4 text-indigo-600" />
                        Điều Chỉnh Phân Bổ Nguồn Lực
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
                            Thời gian: <strong className="font-semibold text-indigo-600">{weekLabel || weekKey} - {monthName} (Khả dụng: {availableHours}h/tuần)</strong>
                        </p>
                    </div>

                    {/* Mode Toggle (NCL-06-CN-007) */}
                    <div className="flex items-center rounded-xl bg-slate-100 p-1">
                        <button
                            type="button"
                            onClick={() => setMode('PERCENTAGE')}
                            className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg py-1.5 text-xs font-semibold transition ${
                                mode === 'PERCENTAGE'
                                    ? 'bg-white text-indigo-600 shadow-2xs'
                                    : 'text-slate-600 hover:text-slate-900'
                            }`}
                        >
                            <Percent className="h-3.5 w-3.5" />
                            <span>Theo Phần Trăm (%)</span>
                        </button>
                        <button
                            type="button"
                            onClick={() => setMode('HOURS')}
                            className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg py-1.5 text-xs font-semibold transition ${
                                mode === 'HOURS'
                                    ? 'bg-white text-indigo-600 shadow-2xs'
                                    : 'text-slate-600 hover:text-slate-900'
                            }`}
                        >
                            <Clock className="h-3.5 w-3.5" />
                            <span>Theo Số Giờ (h)</span>
                        </button>
                    </div>

                    {mode === 'PERCENTAGE' ? (
                        /* Percentage Mode UI (NCL-06-CN-007) */
                        <div className="space-y-3">
                            {/* Preset Buttons */}
                            <div className="grid grid-cols-4 gap-1.5">
                                {[25, 50, 75, 100].map((preset) => (
                                    <button
                                        key={preset}
                                        type="button"
                                        onClick={() => handlePercentageChange(preset)}
                                        className={`rounded-lg py-1 text-xs font-bold border transition ${
                                            percentage === preset
                                                ? 'border-indigo-600 bg-indigo-50 text-indigo-700'
                                                : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
                                        }`}
                                    >
                                        {preset}%
                                    </button>
                                ))}
                            </div>

                            <div>
                                <div className="mb-1 flex items-center justify-between">
                                    <label className="font-semibold text-slate-700">Tỷ lệ phân bổ:</label>
                                    <span className="rounded px-2 py-0.5 text-[11px] font-bold bg-indigo-100 text-indigo-700">
                                        {percentage}%
                                    </span>
                                </div>
                                <input
                                    type="range"
                                    min="0"
                                    max="100"
                                    step="5"
                                    value={percentage}
                                    onChange={(e) => handlePercentageChange(Number(e.target.value))}
                                    className="w-full cursor-pointer accent-indigo-600"
                                />
                                <div className="mt-1 flex justify-between text-[10px] text-slate-400">
                                    <span>0%</span>
                                    <span>25%</span>
                                    <span>50%</span>
                                    <span>75%</span>
                                    <span>100%</span>
                                </div>
                            </div>

                            {/* Live Conversion Preview */}
                            <div className="rounded-xl border border-indigo-100 bg-indigo-50/70 p-2.5 text-[11px] text-indigo-900">
                                <span className="font-semibold">Quy đổi: </span>
                                <span>{percentage}% của {availableHours}h khả dụng = </span>
                                <strong className="text-indigo-700 font-bold">{hours} giờ</strong>
                            </div>
                        </div>
                    ) : (
                        /* Hours Mode UI */
                        <div>
                            <div className="mb-1 flex items-center justify-between">
                                <label className="font-semibold text-slate-700">Tổng số giờ được giao:</label>
                                <span
                                    className={`rounded px-2 py-0.5 text-[11px] font-bold ${
                                        isOverloaded
                                            ? 'bg-rose-100 text-rose-700'
                                            : hours >= availableHours * 0.75
                                            ? 'bg-emerald-100 text-emerald-700'
                                            : 'bg-slate-100 text-slate-700'
                                    }`}
                                >
                                    {hours}h ({percentage}%)
                                </span>
                            </div>
                            <input
                                type="range"
                                min="0"
                                max={Math.max(60, availableHours + 20)}
                                step="1"
                                value={hours}
                                onChange={(e) => handleHoursChange(Number(e.target.value))}
                                className="w-full cursor-pointer accent-indigo-600"
                            />
                            <div className="mt-1 flex justify-between text-[10px] text-slate-400">
                                <span>0h</span>
                                <span>{Math.round(availableHours / 2)}h</span>
                                <span className="font-bold text-slate-600">{availableHours}h (Khả dụng)</span>
                                <span className="font-bold text-rose-500">{availableHours + 10}h+</span>
                            </div>
                        </div>
                    )}

                    {isOverloaded && (
                        <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 p-2.5 text-[11px] text-rose-700">
                            <AlertTriangle className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" />
                            <span>Số giờ vượt quá {availableHours}h/tuần. Có nguy cơ quá tải và vi phạm quy tắc QTN-11.</span>
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
