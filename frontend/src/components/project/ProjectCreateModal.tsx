import React, { useState, useEffect } from 'react';
import { X, FolderPlus, Building2, Calendar, Clock, FileText } from 'lucide-react';
import { createProject, type CreateProjectPayload } from '@/lib/api/projects';
import { getOrgTree } from '@/lib/api/org-units';
import type { OrgUnitTreeNode } from '@/types/hrm';
import type { ProjectMember } from './projectData';

interface ProjectCreateModalProps {
    open: boolean;
    members: ProjectMember[];
    onClose: () => void;
    onCreated: (newProjectId: number) => void;
}

export function ProjectCreateModal({
    open,
    members,
    onClose,
    onCreated,
}: ProjectCreateModalProps) {
    const [projectName, setProjectName] = useState('');
    const [orgUnitId, setOrgUnitId] = useState<number>(1);
    const [orgUnits, setOrgUnits] = useState<{ id: number; name: string }[]>([]);
    const [managerId, setManagerId] = useState<number | undefined>(undefined);
    const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
    const [endDate, setEndDate] = useState('');
    const [estimatedHours, setEstimatedHours] = useState<number>(200);
    const [description, setDescription] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    useEffect(() => {
        if (open) {
            getOrgTree()
                .then((tree) => {
                    const flat: { id: number; name: string }[] = [];
                    const flatten = (nodes: readonly OrgUnitTreeNode[]) => {
                        for (const n of nodes) {
                            flat.push({ id: n.id, name: `${n.unitCode} - ${n.unitName}` });
                            if (n.children && n.children.length > 0) {
                                flatten(n.children);
                            }
                        }
                    };
                    flatten(tree);
                    setOrgUnits(flat);
                    if (flat.length > 0) {
                        setOrgUnitId(flat[0].id);
                    }
                })
                .catch((err) => {
                    console.warn('Failed to load org tree:', err);
                    setOrgUnits([{ id: 1, name: 'Phòng Công nghệ Thông tin (IT)' }]);
                });
        }
    }, [open]);

    if (!open) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setErrorMsg('');

        if (!projectName.trim()) {
            setErrorMsg('Vui lòng nhập tên dự án');
            return;
        }

        setIsSubmitting(true);
        try {
            const payload: CreateProjectPayload = {
                projectName: projectName.trim(),
                orgUnitId: Number(orgUnitId) || 1,
                managerId: managerId ? Number(managerId) : undefined,
                startDate: startDate || undefined,
                endDate: endDate || undefined,
                estimatedHours: Number(estimatedHours) || undefined,
                description: description.trim() || undefined,
            };

            const created = await createProject(payload);
            onCreated(created.id);
            onClose();
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Có lỗi khi tạo dự án';
            setErrorMsg(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in">
            <div className="w-full max-w-lg overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 p-4">
                    <h3 className="flex items-center gap-2 text-sm font-bold text-slate-800">
                        <FolderPlus className="h-4 w-4 text-indigo-600" />
                        Tạo Dự Án Mới (Lưu vào Database)
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
                    {errorMsg && (
                        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-rose-700 font-medium">
                            {errorMsg}
                        </div>
                    )}

                    <div>
                        <label className="mb-1 block font-semibold text-slate-700">Tên dự án *</label>
                        <input
                            type="text"
                            required
                            value={projectName}
                            onChange={(e) => setProjectName(e.target.value)}
                            placeholder="VD: Triển khai Hệ thống ERP Quản trị Nhân sự"
                            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="mb-1 flex items-center gap-1 font-semibold text-slate-700">
                                <Building2 className="h-3.5 w-3.5 text-slate-400" /> Đơn vị phụ trách *
                            </label>
                            <select
                                value={orgUnitId}
                                onChange={(e) => setOrgUnitId(Number(e.target.value))}
                                required
                                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            >
                                {orgUnits.length === 0 ? (
                                    <option value={1}>Phòng Công nghệ Thông tin</option>
                                ) : (
                                    orgUnits.map((u) => (
                                        <option key={u.id} value={u.id}>
                                            {u.name}
                                        </option>
                                    ))
                                )}
                            </select>
                        </div>
                        <div>
                            <label className="mb-1 block font-semibold text-slate-700">Quản lý dự án (PM)</label>
                            <select
                                value={managerId || ''}
                                onChange={(e) => setManagerId(e.target.value ? Number(e.target.value) : undefined)}
                                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            >
                                <option value="">-- Chưa gán PM --</option>
                                {members.map((m) => {
                                    const numId = parseInt(m.id.replace(/\D/g, ''), 10);
                                    return (
                                        <option key={m.id} value={numId || ''}>
                                            {m.name} ({m.role})
                                        </option>
                                    );
                                })}
                            </select>
                        </div>
                    </div>

                    <div className="grid grid-cols-3 gap-3">
                        <div>
                            <label className="mb-1 flex items-center gap-1 font-semibold text-slate-700">
                                <Calendar className="h-3.5 w-3.5 text-slate-400" /> Ngày bắt đầu
                            </label>
                            <input
                                type="date"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            />
                        </div>
                        <div>
                            <label className="mb-1 flex items-center gap-1 font-semibold text-slate-700">
                                <Calendar className="h-3.5 w-3.5 text-slate-400" /> Ngày kết thúc
                            </label>
                            <input
                                type="date"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            />
                        </div>
                        <div>
                            <label className="mb-1 flex items-center gap-1 font-semibold text-slate-700">
                                <Clock className="h-3.5 w-3.5 text-slate-400" /> Tổng giờ dự kiến
                            </label>
                            <input
                                type="number"
                                min={0}
                                step={10}
                                value={estimatedHours}
                                onChange={(e) => setEstimatedHours(Number(e.target.value))}
                                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="mb-1 flex items-center gap-1 font-semibold text-slate-700">
                            <FileText className="h-3.5 w-3.5 text-slate-400" /> Mô tả mục tiêu dự án
                        </label>
                        <textarea
                            rows={3}
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="Mô tả phạm vi và mục tiêu chính của dự án..."
                            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                        />
                    </div>

                    {/* Footer Actions */}
                    <div className="flex items-center justify-end gap-3 border-t border-slate-200 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 cursor-pointer"
                        >
                            Hủy bỏ
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-100 transition hover:bg-indigo-700 disabled:opacity-50 cursor-pointer"
                        >
                            {isSubmitting ? 'Đang tạo...' : 'Tạo Dự Án Thật'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
