import React, { useState, useEffect } from 'react';
import { X, Check, ClipboardCheck } from 'lucide-react';
import type { TaskCategoryGroup, ProjectMember } from './projectData';
import ComboSelect from '../department/ComboSelect';

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

    useEffect(() => {
        if (categories.length > 0) {
            setCatId(defaultCategoryId || categories[0].id);
        }
        if (members.length > 0) {
            setAssigneeId(members[0].id);
        }
    }, [open, defaultCategoryId, categories, members]);

    if (!open) return null;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!name.trim()) return;

        onSubmit({
            catId,
            name: name.trim(),
            assigneeId,
            priority,
            hours: Number(hours) || 20,
            startWeekKey,
            endWeekKey,
        });

        setName('');
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
                        <label className="mb-1 block font-semibold text-slate-700">Hạng mục chính (Phase/Category) *</label>
                        <ComboSelect
                            value={catId}
                            options={categories.map((c) => ({ id: c.id, label: `${c.code} - ${c.name}` }))}
                            onChange={(id) => id && setCatId(id)}
                            placeholder="Chọn hạng mục..."
                            hideSearch
                            buttonClassName="rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none hover:border-slate-400"
                        />
                    </div>

                    <div>
                        <label className="mb-1 block font-semibold text-slate-700">Tên công việc / Đầu việc *</label>
                        <input
                            type="text"
                            required
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="VD: Thiết kế giao diện chi tiết màn hình Dashboard"
                            className="w-full rounded-xl border border-slate-300 px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="mb-1 block font-semibold text-slate-700">Người phụ trách chính *</label>
                            <ComboSelect
                                value={assigneeId}
                                options={members.map((m) => ({ id: m.id, label: m.name, sublabel: m.role }))}
                                onChange={(id) => id && setAssigneeId(id)}
                                placeholder="Chọn người phụ trách..."
                                searchPlaceholder="Tìm tên hoặc vai trò..."
                                buttonClassName="rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none hover:border-slate-400"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block font-semibold text-slate-700">Mức độ ưu tiên</label>
                            <ComboSelect
                                value={priority}
                                options={[
                                    { id: 'Cao', label: 'Cao (High)' },
                                    { id: 'Trung bình', label: 'Trung bình (Medium)' },
                                    { id: 'Thấp', label: 'Thấp (Low)' },
                                ]}
                                onChange={(id) => id && setPriority(id as any)}
                                placeholder="Chọn mức độ..."
                                hideSearch
                                buttonClassName="rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none hover:border-slate-400"
                            />
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
                                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block font-semibold text-slate-700">Tuần bắt đầu</label>
                            <ComboSelect
                                value={startWeekKey}
                                options={[
                                    { id: 'W1', label: 'Tuần 1 (01/09 - 07/09)' },
                                    { id: 'W2', label: 'Tuần 2 (08/09 - 14/09)' },
                                    { id: 'W3', label: 'Tuần 3 (15/09 - 21/09)' },
                                    { id: 'W4', label: 'Tuần 4 (22/09 - 28/09)' },
                                    { id: 'W5', label: 'Tuần 5 (29/09 - 30/09)' },
                                ]}
                                onChange={(id) => id && setStartWeekKey(id)}
                                placeholder="Chọn tuần..."
                                hideSearch
                                buttonClassName="rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none hover:border-slate-400"
                            />
                        </div>
                        <div>
                            <label className="mb-1 block font-semibold text-slate-700">Tuần kết thúc</label>
                            <ComboSelect
                                value={endWeekKey}
                                options={[
                                    { id: 'W1', label: 'Tuần 1 (01/09 - 07/09)' },
                                    { id: 'W2', label: 'Tuần 2 (08/09 - 14/09)' },
                                    { id: 'W3', label: 'Tuần 3 (15/09 - 21/09)' },
                                    { id: 'W4', label: 'Tuần 4 (22/09 - 28/09)' },
                                    { id: 'W5', label: 'Tuần 5 (29/09 - 30/09)' },
                                ]}
                                onChange={(id) => id && setEndWeekKey(id)}
                                placeholder="Chọn tuần..."
                                hideSearch
                                buttonClassName="rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none hover:border-slate-400"
                            />
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

