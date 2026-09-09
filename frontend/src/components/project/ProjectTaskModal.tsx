import React, { useState, useEffect } from 'react';
import { X, Check, ClipboardCheck } from 'lucide-react';
import type { TaskCategoryGroup, ProjectMember } from './projectData';

interface ProjectTaskModalProps {
    open: boolean;
    categories: TaskCategoryGroup[];
    members: ProjectMember[];
    defaultCategoryId?: string;
    onClose: () => void;
    onSubmit: (newTask: {
        catId: string;
        name: string;
        assigneeId: string;
        priority: 'Cao' | 'Trung bình' | 'Thấp';
        hours: number;
        startWeekKey: string;
        endWeekKey: string;
        newCategoryName?: string;
    }) => void;
}

export function ProjectTaskModal({
    open,
    categories,
    members,
    defaultCategoryId,
    onClose,
    onSubmit,
}: ProjectTaskModalProps) {
    const [catId, setCatId] = useState('');
    const [name, setName] = useState('');
    const [assigneeId, setAssigneeId] = useState('');
    const [priority, setPriority] = useState<'Cao' | 'Trung bình' | 'Thấp'>('Trung bình');
    const [hours, setHours] = useState(30);
    const [startWeekKey, setStartWeekKey] = useState('W2');
    const [endWeekKey, setEndWeekKey] = useState('W3');
    const [isCreatingNewCat, setIsCreatingNewCat] = useState(false);
    const [newCatName, setNewCatName] = useState('');

    useEffect(() => {
        if (categories.length > 0) {
            setCatId(defaultCategoryId || categories[0].id);
            setIsCreatingNewCat(false);
        } else {
            setIsCreatingNewCat(true);
        }
        if (members.length > 0) {
            setAssigneeId(members[0].id);
        }
    }, [open, defaultCategoryId, categories, members]);

    if (!open) return null;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!name.trim()) return;
        if (isCreatingNewCat && !newCatName.trim()) return;

        onSubmit({
            catId: isCreatingNewCat ? '__NEW__' : catId,
            name: name.trim(),
            assigneeId,
            priority,
            hours: Number(hours) || 20,
            startWeekKey,
            endWeekKey,
            newCategoryName: isCreatingNewCat ? newCatName.trim() : undefined,
        });

        setName('');
        setNewCatName('');
        onClose();
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
            <div className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4">
                    <h3 className="flex items-center gap-2 text-sm font-bold text-slate-800">
                        <ClipboardCheck className="h-4 w-4 text-indigo-600" />
                        Thêm Công Việc Vào Hạng Mục
                    </h3>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs">
                    <div>
                        <div className="mb-1 flex items-center justify-between">
                            <label className="font-semibold text-slate-700">Hạng mục chính (Phase/Category) *</label>
                            {categories.length > 0 && (
                                <button
                                    type="button"
                                    onClick={() => setIsCreatingNewCat(!isCreatingNewCat)}
                                    className="text-[11px] font-semibold text-indigo-600 hover:underline cursor-pointer"
                                >
                                    {isCreatingNewCat ? '← Chọn mục có sẵn' : '+ Tạo mục mới'}
                                </button>
                            )}
                        </div>

                        {isCreatingNewCat || categories.length === 0 ? (
                            <input
                                type="text"
                                required
                                value={newCatName}
                                onChange={(e) => setNewCatName(e.target.value)}
                                placeholder="Nhập tên hạng mục mới (VD: Khởi động dự án, Thiết kế UI...)"
                                className="w-full rounded-lg border border-indigo-300 bg-indigo-50/40 px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20"
                            />
                        ) : (
                            <select
                                value={catId}
                                onChange={(e) => setCatId(e.target.value)}
                                required
                                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            >
                                {categories.map((c) => (
                                    <option key={c.id} value={c.id}>
                                        {c.code} - {c.name}
                                    </option>
                                ))}
                            </select>
                        )}
                    </div>

                    <div>
                        <label className="mb-1 block font-semibold text-slate-700">Tên công việc / Đầu việc *</label>
                        <input
                            type="text"
                            required
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="VD: Thiết kế giao diện chi tiết màn hình Dashboard"
                            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="mb-1 block font-semibold text-slate-700">Người phụ trách chính *</label>
                            <select
                                value={assigneeId}
                                onChange={(e) => setAssigneeId(e.target.value)}
                                required
                                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            >
                                {members.map((m) => (
                                    <option key={m.id} value={m.id}>
                                        {m.name} ({m.role})
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div>
                            <label className="mb-1 block font-semibold text-slate-700">Mức độ ưu tiên</label>
                            <select
                                value={priority}
                                onChange={(e) => setPriority(e.target.value as any)}
                                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            >
                                <option value="Cao">Cao (High)</option>
                                <option value="Trung bình">Trung bình (Medium)</option>
                                <option value="Thấp">Thấp (Low)</option>
                            </select>
                        </div>
                    </div>

                    <div className="grid grid-cols-3 gap-3">
                        <div>
                            <label className="mb-1 block font-semibold text-slate-700">Ước tính (Giờ)</label>
                            <input
                                type="number"
                                min={1}
                                max={200}
                                required
                                value={hours}
                                onChange={(e) => setHours(Number(e.target.value))}
                                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block font-semibold text-slate-700">Tuần bắt đầu</label>
                            <select
                                value={startWeekKey}
                                onChange={(e) => setStartWeekKey(e.target.value)}
                                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            >
                                <option value="W1">Tuần 1 (01/09 - 07/09)</option>
                                <option value="W2">Tuần 2 (08/09 - 14/09)</option>
                                <option value="W3">Tuần 3 (15/09 - 21/09)</option>
                                <option value="W4">Tuần 4 (22/09 - 28/09)</option>
                                <option value="W5">Tuần 5 (29/09 - 30/09)</option>
                            </select>
                        </div>
                        <div>
                            <label className="mb-1 block font-semibold text-slate-700">Tuần kết thúc</label>
                            <select
                                value={endWeekKey}
                                onChange={(e) => setEndWeekKey(e.target.value)}
                                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            >
                                <option value="W1">Tuần 1 (01/09 - 07/09)</option>
                                <option value="W2">Tuần 2 (08/09 - 14/09)</option>
                                <option value="W3">Tuần 3 (15/09 - 21/09)</option>
                                <option value="W4">Tuần 4 (22/09 - 28/09)</option>
                                <option value="W5">Tuần 5 (29/09 - 30/09)</option>
                            </select>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-2 border-t border-slate-200 pt-3">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-100 transition"
                        >
                            Hủy bỏ
                        </button>
                        <button
                            type="submit"
                            className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-indigo-700 transition"
                        >
                            <Check className="h-3.5 w-3.5" /> Lưu & Phân bổ
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

