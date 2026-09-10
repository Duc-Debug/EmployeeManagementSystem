import React, { useState, useEffect } from 'react';
import {
    X,
    FolderPlus,
    Building2,
    Calendar,
    Clock,
    FileText,
    Layers,
    LayoutTemplate,
    Sparkles,
    CheckCircle2,
    ChevronDown,
    ChevronUp,
    ListTree,
    Loader2,
    UserCheck
} from 'lucide-react';
import { useAuthUser } from '@/lib/auth-session';
import {
    createProject,
    createProjectFromTemplate,
    getProjectTemplates,
    getProjectTemplateDetail,
    type CreateProjectPayload,
    type CreateProjectFromTemplatePayload,
    type ProjectTemplateSummary,
    type ProjectTemplateDetail
} from '@/lib/api/projects';
import { getOrgTree } from '@/lib/api/org-units';
import { getEmployeeProfileByUserId } from '@/lib/api/employees';
import type { OrgUnitTreeNode } from '@/types/hrm';
import type { AuthUser } from '@/lib/auth-session';
import type { ProjectMember } from './projectData';

interface ProjectCreateModalProps {
    open: boolean;
    members: ProjectMember[];
    currentUser: AuthUser | null;
    onClose: () => void;
    onCreated: (newProjectId: number) => void;
}

type CreateMode = 'STANDARD' | 'TEMPLATE';

export function ProjectCreateModal({
    open,
    members,
    currentUser,
    onClose,
    onCreated,
}: ProjectCreateModalProps) {
    const [mode, setMode] = useState<CreateMode>('STANDARD');

    // Standard Project form state
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

    const authUser = useAuthUser();
    const effectiveUser = currentUser || authUser;

    const [currentEmpId, setCurrentEmpId] = useState<number | undefined>(undefined);
    const [resolvedOrgUnitId, setResolvedOrgUnitId] = useState<number | undefined>(undefined);

    // Resolve employeeId và orgUnitId một cách an toàn (tránh nhầm lẫn giữa userId và employeeId)
    useEffect(() => {
        if (!open || !effectiveUser) return;

        // 1. Kiểm tra nhanh từ danh sách members (nếu có member khớp tên)
        if (effectiveUser.fullName) {
            const memberByName = members.find(
                (m) => m.name && m.name.trim().toLowerCase() === effectiveUser.fullName.trim().toLowerCase()
            );
            if (memberByName?.employeeId) {
                setCurrentEmpId(memberByName.employeeId);
                if (effectiveUser.roleCode === 'VT-02') {
                    setManagerId((prev) => prev ?? memberByName.employeeId);
                }
            }
        }

        // 2. Gọi API getEmployeeProfileByUserId để lấy Employee ID và OrgUnit ID chuẩn từ backend
        if (effectiveUser.id) {
            getEmployeeProfileByUserId(effectiveUser.id)
                .then((profile) => {
                    if (profile?.id) {
                        setCurrentEmpId(profile.id);
                        if (effectiveUser.roleCode === 'VT-02') {
                            setManagerId(profile.id);
                        }
                    }
                    if (profile?.orgUnitId) {
                        setResolvedOrgUnitId(profile.orgUnitId);
                        setOrgUnitId((prev) => {
                            // Cập nhật orgUnit nếu đang ở giá trị khởi tạo hoặc chưa có đơn vị
                            if (!effectiveUser.orgUnitId || prev === 1) {
                                return profile.orgUnitId;
                            }
                            return prev;
                        });
                    }
                })
                .catch((err) => {
                    console.warn('Không thể tải EmployeeProfile của user:', err);
                });
        }
    }, [open, effectiveUser, members]);

    // Chuẩn bị danh sách PM options (chỉ thêm effectiveUser khi đã resolve được employeeId hợp lệ)
    const pmOptions = React.useMemo(() => {
        const list = [...members];
        if (effectiveUser && currentEmpId && !list.some((m) => m.employeeId === currentEmpId)) {
            list.unshift({
                id: `u-${currentEmpId}`,
                employeeId: currentEmpId,
                name: effectiveUser.fullName || effectiveUser.username,
                role: effectiveUser.roleCode || 'Nhân viên',
                avatar: '',
                capacity: 40,
                weeklyHours: {},
            });
        }
        return list;
    }, [members, effectiveUser, currentEmpId]);

    // Templates state
    const [templates, setTemplates] = useState<ProjectTemplateSummary[]>([]);
    const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null);
    const [isLoadingTemplates, setIsLoadingTemplates] = useState(false);
    const [previewDetail, setPreviewDetail] = useState<ProjectTemplateDetail | null>(null);
    const [isLoadingPreview, setIsLoadingPreview] = useState(false);
    const [showWbsPreview, setShowWbsPreview] = useState(false);

    useEffect(() => {
        if (open) {
            setErrorMsg('');
            setProjectName('');
            setManagerId(undefined);
            setCurrentEmpId(undefined);
            setStartDate(new Date().toISOString().split('T')[0]);
            setEndDate('');
            setDescription('');

            const initialOrg = effectiveUser?.orgUnitId ?? undefined;
            if (initialOrg) {
                setOrgUnitId(initialOrg);
                setResolvedOrgUnitId(initialOrg);
            }

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

                    // Ưu tiên đơn vị của người dùng hiện tại (effectiveUser.orgUnitId), không ghi đè nếu đã có
                    setOrgUnitId((prev) => {
                        const preferred = effectiveUser?.orgUnitId;
                        if (preferred && flat.some((u) => u.id === preferred)) {
                            return preferred;
                        }
                        if (prev && flat.some((u) => u.id === prev)) {
                            return prev;
                        }
                        return flat.length > 0 ? flat[0].id : 1;
                    });
                })
                .catch((err) => {
                    console.warn('Failed to load org tree:', err);
                    setOrgUnits([{ id: 1, name: 'Phòng Công nghệ Thông tin (IT)' }]);
                });

            // Tải danh sách mẫu dự án
            setIsLoadingTemplates(true);
            getProjectTemplates()
                .then((data) => {
                    setTemplates(data || []);
                    if (data && data.length > 0) {
                        setSelectedTemplateId(data[0].id);
                    }
                })
                .catch((err) => {
                    console.warn('Failed to load project templates:', err);
                })
                .finally(() => {
                    setIsLoadingTemplates(false);
                });
        }
    }, [open, effectiveUser]);

    // Khi chọn template, tự động lấy preview và cập nhật số giờ ước tính
    useEffect(() => {
        if (selectedTemplateId) {
            const selected = templates.find((t) => t.id === selectedTemplateId);
            if (selected && selected.totalEstimatedHours) {
                setEstimatedHours(Number(selected.totalEstimatedHours));
            }

            setIsLoadingPreview(true);
            getProjectTemplateDetail(selectedTemplateId)
                .then((detail) => {
                    setPreviewDetail(detail);
                })
                .catch((err) => {
                    console.warn('Failed to load template detail preview:', err);
                })
                .finally(() => {
                    setIsLoadingPreview(false);
                });
        }
    }, [selectedTemplateId, templates]);

    if (!open) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setErrorMsg('');

        if (!projectName.trim()) {
            setErrorMsg('Vui lòng nhập tên dự án');
            return;
        }

        if (mode === 'TEMPLATE' && !selectedTemplateId) {
            setErrorMsg('Vui lòng chọn một mẫu dự án');
            return;
        }

        // Validate ngày: năm không được vượt quá 2100
        if (startDate) {
            const startYear = new Date(startDate).getFullYear();
            if (startYear > 2100) {
                setErrorMsg('Ngày bắt đầu không được vượt quá năm 2100');
                return;
            }
        }
        if (endDate) {
            const endYear = new Date(endDate).getFullYear();
            if (endYear > 2100) {
                setErrorMsg('Ngày kết thúc không được vượt quá năm 2100');
                return;
            }
        }
        if (startDate && endDate && new Date(endDate) < new Date(startDate)) {
            setErrorMsg('Ngày kết thúc không được sớm hơn ngày bắt đầu');
            return;
        }

        setIsSubmitting(true);
        try {
            if (mode === 'TEMPLATE' && selectedTemplateId) {
                const payload: CreateProjectFromTemplatePayload = {
                    templateId: selectedTemplateId,
                    projectName: projectName.trim(),
                    orgUnitId: Number(orgUnitId) || 1,
                    managerId: managerId ? Number(managerId) : undefined,
                    startDate: startDate || undefined,
                    endDate: endDate || undefined,
                    description: description.trim() || undefined,
                };

                const created = await createProjectFromTemplate(payload);
                onCreated(created.id);
                onClose();
            } else {
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
            }
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Có lỗi khi tạo dự án';
            setErrorMsg(msg);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-xs animate-in fade-in overflow-y-auto">
            <div className="w-full max-w-2xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl transition-all my-8">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-5 py-4">
                    <div className="flex items-center gap-2.5">
                        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-100 text-indigo-700">
                            {mode === 'TEMPLATE' ? (
                                <LayoutTemplate className="h-5 w-5" />
                            ) : (
                                <FolderPlus className="h-5 w-5" />
                            )}
                        </div>
                        <div>
                            <h3 className="text-sm font-bold text-slate-900">
                                {mode === 'TEMPLATE' ? 'Tạo Dự Án Từ Mẫu' : 'Tạo Dự Án Tiêu Chuẩn'}
                            </h3>
                            <p className="text-[11px] text-slate-500">
                                {mode === 'TEMPLATE'
                                    ? 'Kế thừa cấu trúc WBS và định mức giờ công sẵn có'
                                    : 'Khởi tạo dự án mới với danh mục công việc trống'}
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition cursor-pointer"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                {/* Mode Selector Tabs */}
                <div className="border-b border-slate-200 bg-slate-50/50 px-5 pt-3 pb-3">
                    <div className="flex rounded-xl bg-slate-200/70 p-1">
                        <button
                            type="button"
                            onClick={() => setMode('STANDARD')}
                            className={`flex flex-1 items-center justify-center gap-2 rounded-lg py-1.5 text-xs font-bold transition-all cursor-pointer ${
                                mode === 'STANDARD'
                                    ? 'bg-white text-indigo-700 shadow-xs'
                                    : 'text-slate-600 hover:text-slate-900'
                            }`}
                        >
                            <FolderPlus className="h-3.5 w-3.5" />
                            <span>Dự Án Tiêu Chuẩn (Trống)</span>
                        </button>
                        <button
                            type="button"
                            onClick={() => setMode('TEMPLATE')}
                            className={`flex flex-1 items-center justify-center gap-2 rounded-lg py-1.5 text-xs font-bold transition-all cursor-pointer ${
                                mode === 'TEMPLATE'
                                    ? 'bg-white text-indigo-700 shadow-xs'
                                    : 'text-slate-600 hover:text-slate-900'
                            }`}
                        >
                            <Sparkles className="h-3.5 w-3.5 text-amber-500" />
                            <span>Từ Mẫu Có Sẵn (Template)</span>
                        </button>
                    </div>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="p-5 space-y-4 text-xs max-h-[calc(85vh-140px)] overflow-y-auto">
                    {errorMsg && (
                        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-rose-700 font-medium">
                            {errorMsg}
                        </div>
                    )}

                    {/* Template Selection Section (When mode === 'TEMPLATE') */}
                    {mode === 'TEMPLATE' && (
                        <div className="space-y-3 rounded-2xl border border-indigo-100 bg-indigo-50/40 p-3.5">
                            <div className="flex items-center justify-between">
                                <label className="flex items-center gap-1.5 font-bold text-indigo-950">
                                    <Layers className="h-4 w-4 text-indigo-600" />
                                    <span>Chọn mẫu dự án phù hợp:</span>
                                </label>
                                {isLoadingTemplates && (
                                    <span className="flex items-center gap-1 text-[11px] text-indigo-600">
                                        <Loader2 className="h-3 w-3 animate-spin" /> Đang tải danh sách mẫu...
                                    </span>
                                )}
                            </div>

                            {templates.length === 0 && !isLoadingTemplates ? (
                                <p className="text-slate-500 italic">Chưa có mẫu dự án nào trong hệ thống.</p>
                            ) : (
                                <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-3">
                                    {templates.map((tpl) => {
                                        const isSelected = selectedTemplateId === tpl.id;
                                        return (
                                            <div
                                                key={tpl.id}
                                                onClick={() => setSelectedTemplateId(tpl.id)}
                                                className={`relative flex flex-col justify-between rounded-xl border p-3 transition-all cursor-pointer ${
                                                    isSelected
                                                        ? 'border-indigo-600 bg-white shadow-sm ring-2 ring-indigo-500/20'
                                                        : 'border-slate-200/80 bg-white/80 hover:border-indigo-300 hover:bg-white'
                                                }`}
                                            >
                                                <div>
                                                    <div className="flex items-center justify-between gap-1 mb-1">
                                                        <span className="font-mono text-[10px] font-bold text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded">
                                                            {tpl.templateCode}
                                                        </span>
                                                        {isSelected && (
                                                            <CheckCircle2 className="h-4 w-4 text-indigo-600 shrink-0" />
                                                        )}
                                                    </div>
                                                    <h4 className="font-bold text-slate-800 line-clamp-2 leading-tight">
                                                        {tpl.name}
                                                    </h4>
                                                    {tpl.description && (
                                                        <p className="mt-1 text-[11px] text-slate-500 line-clamp-2">
                                                            {tpl.description}
                                                        </p>
                                                    )}
                                                </div>

                                                <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-2 text-[10px] font-semibold text-slate-600">
                                                    <span>{tpl.categoriesCount} nhóm · {tpl.tasksCount} task</span>
                                                    <span className="font-bold text-indigo-700 bg-indigo-50 px-1.5 py-0.5 rounded">
                                                        {tpl.totalEstimatedHours}h
                                                    </span>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}

                            {/* WBS Preview Accordion */}
                            {previewDetail && (
                                <div className="rounded-xl border border-indigo-200/80 bg-white p-2.5 shadow-2xs">
                                    <button
                                        type="button"
                                        onClick={() => setShowWbsPreview(!showWbsPreview)}
                                        className="flex w-full items-center justify-between text-left text-xs font-semibold text-indigo-900 cursor-pointer"
                                    >
                                        <div className="flex items-center gap-1.5">
                                            <ListTree className="h-3.5 w-3.5 text-indigo-600" />
                                            <span>Xem trước cây công việc của mẫu ({previewDetail.tasks?.length || 0} công việc)</span>
                                        </div>
                                        {showWbsPreview ? (
                                            <ChevronUp className="h-3.5 w-3.5 text-slate-400" />
                                        ) : (
                                            <ChevronDown className="h-3.5 w-3.5 text-slate-400" />
                                        )}
                                    </button>

                                    {showWbsPreview && (
                                        <div className="mt-2.5 max-h-48 space-y-1 overflow-y-auto rounded-lg border border-slate-100 bg-slate-50/70 p-2 text-[11px]">
                                            {isLoadingPreview ? (
                                                <div className="flex items-center justify-center p-3 text-slate-400">
                                                    <Loader2 className="h-4 w-4 animate-spin text-indigo-600" />
                                                </div>
                                            ) : previewDetail.tasks && previewDetail.tasks.length > 0 ? (
                                                previewDetail.tasks.map((task) => (
                                                    <div
                                                        key={task.id}
                                                        className={`flex items-center justify-between py-1 px-1.5 rounded ${
                                                            task.taskType === 'CATEGORY'
                                                                ? 'font-bold text-indigo-950 bg-indigo-50/50'
                                                                : 'text-slate-700 pl-4'
                                                        }`}
                                                    >
                                                        <div className="flex items-center gap-1.5 truncate">
                                                            {task.taskType === 'CATEGORY' ? (
                                                                <Layers className="h-3 w-3 text-indigo-600 shrink-0" />
                                                            ) : (
                                                                <span className="text-slate-400 font-mono text-[10px] shrink-0">├─</span>
                                                            )}
                                                            <span className="truncate">{task.name}</span>
                                                        </div>
                                                        {task.estimatedHours > 0 && (
                                                            <span className="text-[10px] font-semibold text-indigo-600 shrink-0">
                                                                {task.estimatedHours}h
                                                            </span>
                                                        )}
                                                    </div>
                                                ))
                                            ) : (
                                                <p className="text-slate-400 italic">Mẫu này chưa có công việc con.</p>
                                            )}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    )}

                    {/* Common Project Information */}
                    <div>
                        <label className="mb-1 block font-semibold text-slate-700">Tên dự án mới *</label>
                        <input
                            type="text"
                            required
                            value={projectName}
                            onChange={(e) => setProjectName(e.target.value)}
                            placeholder={
                                mode === 'TEMPLATE'
                                    ? 'VD: Triển khai Phần mềm Quản lý Bán hàng 2026'
                                    : 'VD: Triển khai Hệ thống ERP Quản trị Nhân sự'
                            }
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
                            <div className="mb-1 flex items-center justify-between">
                                <label className="font-semibold text-slate-700">Quản lý dự án (PM)</label>
                                {currentEmpId && (
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setManagerId(currentEmpId);
                                            const targetOrg = resolvedOrgUnitId || effectiveUser?.orgUnitId;
                                            if (targetOrg) {
                                                setOrgUnitId(targetOrg);
                                            }
                                        }}
                                        className="inline-flex items-center gap-1 text-[11px] font-medium text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                                    >
                                        <UserCheck className="h-3 w-3" />
                                        <span>Chọn tôi làm PM</span>
                                    </button>
                                )}
                            </div>
                            <select
                                value={managerId || ''}
                                onChange={(e) => setManagerId(e.target.value ? Number(e.target.value) : undefined)}
                                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            >
                                <option value="">-- Chưa gán PM --</option>
                                {pmOptions.map((m) => {
                                    const numId = m.employeeId ?? (m.id && /^\d+$/.test(m.id) ? Number(m.id) : undefined);
                                    if (!numId) return null;
                                    const isMe = currentEmpId && numId === currentEmpId;
                                    return (
                                        <option key={m.id || numId} value={numId}>
                                            {m.name} {isMe ? '(Tôi)' : (m.role ? `(${m.role})` : '')}
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
                                disabled={mode === 'TEMPLATE'}
                                title={mode === 'TEMPLATE' ? 'Tổng giờ được tự động tính từ các công việc của mẫu' : undefined}
                                className={`w-full rounded-lg border border-slate-300 px-3 py-2 text-xs font-medium outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 ${
                                    mode === 'TEMPLATE' ? 'bg-slate-100 text-slate-500 cursor-not-allowed' : 'text-slate-800'
                                }`}
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
                            className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-bold text-white shadow-md shadow-indigo-100 transition hover:bg-indigo-700 disabled:opacity-50 cursor-pointer flex items-center gap-1.5"
                        >
                            {isSubmitting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                            <span>
                                {isSubmitting
                                    ? 'Đang khởi tạo...'
                                    : mode === 'TEMPLATE'
                                    ? 'Tạo Dự Án Từ Mẫu'
                                    : 'Tạo Dự Án Thật'}
                            </span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
