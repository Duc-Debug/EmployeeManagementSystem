import { useState, useEffect } from 'react';
import { ClipboardList, Search as SearchIcon, ShieldCheck, LayoutGrid, BookOpen, CalendarClock, Clock, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
    ConfirmDeleteModal,
    SkillFormModal,
    SkillsTable,
} from './Components.tsx';
import { ToastList } from './ToastNotification.tsx';
import SkillresourceSearch, { type DepartmentItem, type ResourceEmployee } from './SkillresourceSearch.tsx';
import type { CatalogSkill, DeclaredSkill, FormMode, SkillPayload, ToastItem, PendingApprovalSkill, SkillStatus } from './Types.ts';
import SkillApproveTable from './SkillApproveTable.tsx';
import SkillMatrixView from './SkillMatrixView.tsx';
import SkillCatalogView from './SkillCatalogView.tsx';

import { useAuthUser } from '@/lib/auth-session';
import {
    getSkills,
    getMySkills,
    declareMySkill,
    updateMySkill,
    deleteMySkill,
    getPendingSkills,
    approveSkill,
    rejectSkill,
} from '@/lib/api/skills';

let toastSeq = 0;

export type ModuleTab = 'declare' | 'matrix' | 'catalog' | 'approve' | 'search';

const MODULE_TABS: { id: ModuleTab; label: string; icon: typeof SearchIcon; allowedRoles: string[] }[] = [
    { id: 'declare', label: 'Khai báo cá nhân', icon: ClipboardList, allowedRoles: ['VT-04', 'VT-06'] },
    { id: 'matrix', label: 'Ma trận kỹ năng bộ phận', icon: LayoutGrid, allowedRoles: ['VT-01', 'VT-02', 'VT-03', 'VT-05', 'VT-06'] },
    { id: 'catalog', label: 'Danh mục kỹ năng', icon: BookOpen, allowedRoles: ['VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06'] },
    { id: 'approve', label: 'Duyệt kỹ năng', icon: ShieldCheck, allowedRoles: ['VT-03', 'VT-06'] },
    { id: 'search', label: 'Tra cứu nhân lực', icon: SearchIcon, allowedRoles: ['VT-02', 'VT-03', 'VT-06'] },
];

interface SkillCampaign {
    title: string;
    cycle: string;
    cycleLabel: string;
    deadline: string;
    status: 'open' | 'closed';
    notes: string;
}

const DEFAULT_CAMPAIGN: SkillCampaign = {
    title: 'Đợt rà soát kỹ năng định kỳ Q3/2026',
    cycle: '6_months',
    cycleLabel: 'Định kỳ 6 tháng',
    deadline: '2026-09-30',
    status: 'open',
    notes: 'Tất cả nhân viên chuyên môn vui lòng cập nhật kỹ năng và số năm kinh nghiệm mới nhất trước hạn chót.',
};

const EMPTY_DEPARTMENTS: DepartmentItem[] = [];

export interface SkilldeclarationViewProps {
    departments?: DepartmentItem[];
    employees?: ResourceEmployee[];
    initialTab?: ModuleTab;
}

export default function SkilldeclarationView({
    departments = EMPTY_DEPARTMENTS,
    employees: _employees = [],
    initialTab = 'declare',
}: SkilldeclarationViewProps) {
    const currentUser = useAuthUser();
    const roleCode = currentUser?.roleCode?.toUpperCase().replace(/_/g, '-') || '';

    // Lọc các Tab nghiêm ngặt theo vai trò của tài khoản (Fail-closed)
    const visibleTabs = MODULE_TABS.filter((t) => t.allowedRoles.includes(roleCode));

    const [activeTab, setActiveTab] = useState<ModuleTab>(() => {
        if (visibleTabs.some((t) => t.id === initialTab)) return initialTab;
        return visibleTabs[0]?.id || 'catalog';
    });

    // Quản lý kỳ cập nhật kỹ năng định kỳ (Story 9)
    const [campaign, setCampaign] = useState<SkillCampaign>(() => {
        try {
            const saved = localStorage.getItem('skill_assessment_campaign');
            return saved ? JSON.parse(saved) : DEFAULT_CAMPAIGN;
        } catch {
            return DEFAULT_CAMPAIGN;
        }
    });
    const [campaignModalOpen, setCampaignModalOpen] = useState(false);
    const [editingCampaign, setEditingCampaign] = useState<SkillCampaign>(campaign);

    function handleSaveCampaign(e: React.FormEvent) {
        e.preventDefault();
        setCampaign(editingCampaign);
        localStorage.setItem('skill_assessment_campaign', JSON.stringify(editingCampaign));
        setCampaignModalOpen(false);
        pushToast('Cập nhật kỳ đánh giá', 'Đã lưu thiết lập thời hạn cập nhật kỹ năng định kỳ thành công.');
    }

    // Khi roleCode thay đổi, đồng bộ tab hợp lệ
    useEffect(() => {
        if (visibleTabs.length > 0 && !visibleTabs.some((t) => t.id === activeTab)) {
            setActiveTab(visibleTabs[0].id);
        }
    }, [roleCode, visibleTabs, activeTab]);

    const [catalog, setCatalog] = useState<CatalogSkill[]>([]);
    const catalogById = Object.fromEntries(catalog.map((c) => [c.id, c]));

    const [skills, setSkills] = useState<DeclaredSkill[]>([]);
    const [skillsBackup, setSkillsBackup] = useState<DeclaredSkill[] | null>(null);
    const [demoEmpty, setDemoEmpty] = useState(false);

    // Modal Form Khai báo
    const [modalOpen, setModalOpen] = useState(false);
    const [formMode, setFormMode] = useState<FormMode>('create');
    const [editingSkillId, setEditingSkillId] = useState<number | string | null>(null);
    const [duplicateSkillId, setDuplicateSkillId] = useState<number | string | null>(null);
    const [saving, setSaving] = useState(false);

    // Modal Xóa
    const [deletingSkill, setDeletingSkill] = useState<DeclaredSkill | null>(null);

    const [highlightSkillId, setHighlightSkillId] = useState<number | string | null>(null);
    const [toasts, setToasts] = useState<ToastItem[]>([]);

    // ── Tab Duyệt kỹ năng ──
    const [approvalRequests, setApprovalRequests] = useState<PendingApprovalSkill[]>([]);

    function pushToast(title: string, message: string) {
        toastSeq += 1;
        setToasts((prev) => [...prev, { id: toastSeq, title, message }]);
    }

    function removeToast(id: number) {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }

    // 1. Tải danh mục kỹ năng chuẩn từ Backend
    const loadCatalog = async () => {
        try {
            const data = await getSkills();
            const mapped: CatalogSkill[] = (data || []).map((s) => ({
                id: s.id,
                name: s.name,
                category: s.groupName || 'Khác',
                groupId: s.groupId,
                description: s.description,
                version: s.version,
            }));
            setCatalog(mapped);
        } catch (err) {
            console.error('Failed to load skills catalog from backend:', err);
            setCatalog([]);
        }
    };

    // 2. Tải danh sách kỹ năng cá nhân đã khai báo (cho VT-04)
    const loadPersonalSkills = async () => {
        if (roleCode !== 'VT-04') return;
        try {
            const data = await getMySkills();
            const mapped: DeclaredSkill[] = data.map((es) => ({
                skillId: es.skillId,
                name: es.skillName,
                code: es.skillCode || `SK-${String(es.skillId).padStart(3, '0')}`,
                cat: es.skillCategory || 'Khác',
                level: es.proficiencyLevel,
                years: es.yearsOfExperience,
                status: (es.status?.toLowerCase() as SkillStatus) || 'pending',
            }));
            setSkills(mapped);
        } catch (err) {
            console.error('Failed to load personal skills from backend:', err);
        }
    };

    // 3. Tải danh sách yêu cầu chờ duyệt (cho VT-03 và VT-06)
    const loadApprovals = async () => {
        if (roleCode !== 'VT-03' && roleCode !== 'VT-06') return;
        try {
            const data = await getPendingSkills();
            const mapped: PendingApprovalSkill[] = data.map((p) => ({
                id: p.id,
                employeeName: p.employeeName,
                skillName: p.skillName,
                category: p.skillCategory || 'Khác',
                level: p.proficiencyLevel,
                years: p.yearsOfExperience,
                status: (p.status?.toLowerCase() as any) || 'pending',
            }));
            setApprovalRequests(mapped);
        } catch (err) {
            console.error('Failed to load pending approvals from backend:', err);
        }
    };

    useEffect(() => {
        loadCatalog();
        loadPersonalSkills();
        loadApprovals();
    }, [roleCode]);

    async function handleApproveRequest(id: number, adjustedLevel?: number, notes?: string) {
        try {
            await approveSkill(id, {
                adjustedProficiencyLevel: adjustedLevel,
                reviewNotes: notes,
            });
            pushToast('Đã phê duyệt', 'Kỹ năng đã được xác nhận thành công.');
            await loadApprovals();
        } catch (err: any) {
            console.error('Failed to approve skill:', err);
            pushToast('Lỗi phê duyệt', err?.message || 'Không thể xác nhận kỹ năng.');
        }
    }

    async function handleRejectRequest(id: number, reason?: string) {
        try {
            await rejectSkill(id, reason);
            pushToast('Đã từ chối', 'Yêu cầu kỹ năng đã được từ chối.');
            await loadApprovals();
        } catch (err: any) {
            console.error('Failed to reject skill:', err);
            pushToast('Lỗi từ chối', err?.message || 'Không thể từ chối kỹ năng.');
        }
    }

    function toggleDemoEmpty(checked: boolean) {
        if (checked) {
            setSkillsBackup(skills);
            setSkills([]);
        } else {
            setSkills(skillsBackup || skills);
        }
        setDemoEmpty(checked);
    }

    function handleDeleteClick(skill: DeclaredSkill) {
        setDeletingSkill(skill);
    }

    async function handleConfirmDelete() {
        if (!deletingSkill) return;
        try {
            await deleteMySkill(deletingSkill.skillId);
            pushToast('Đã xóa kỹ năng', `Đã xóa "${deletingSkill.name}" khỏi hồ sơ cá nhân.`);
            await loadPersonalSkills();
        } catch (err: any) {
            console.error('Failed to delete skill:', err);
            pushToast('Lỗi khi xóa', err?.message || 'Không thể xóa kỹ năng.');
        } finally {
            setDeletingSkill(null);
        }
    }

    function openCreateModal() {
        setFormMode('create');
        setEditingSkillId(null);
        setDuplicateSkillId(null);
        setModalOpen(true);
    }

    function openEditModal(skillId: number | string) {
        setFormMode('update');
        setEditingSkillId(skillId);
        setDuplicateSkillId(null);
        setModalOpen(true);
    }

    function closeModal() {
        setModalOpen(false);
        setDuplicateSkillId(null);
    }

    const editingSkill: DeclaredSkill | null = editingSkillId != null
        ? (skills.find((s) => String(s.skillId) === String(editingSkillId)) || (
            catalogById[Number(editingSkillId)]
                ? {
                    skillId: Number(editingSkillId),
                    name: catalogById[Number(editingSkillId)].name,
                    code: `SK-${String(editingSkillId).padStart(3, '0')}`,
                    cat: catalogById[Number(editingSkillId)].category,
                    level: 1,
                    years: 1,
                    status: 'pending',
                }
                : null
        ))
        : null;

    const duplicateSkillName = duplicateSkillId != null ? catalogById[duplicateSkillId]?.name ?? null : null;

    async function handleSave(payload: SkillPayload) {
        setSaving(true);
        try {
            if (!payload.skillId || isNaN(Number(payload.skillId))) {
                pushToast('Lỗi khai báo', 'Vui lòng chọn một kỹ năng hợp lệ từ danh mục CSDL.');
                setSaving(false);
                return;
            }
            if (formMode === 'update') {
                await updateMySkill(payload.skillId, {
                    skillId: payload.skillId,
                    proficiencyLevel: payload.proficiencyLevel,
                    yearsOfExperience: payload.yearsOfExperience,
                });
            } else {
                await declareMySkill({
                    skillId: payload.skillId,
                    proficiencyLevel: payload.proficiencyLevel,
                    yearsOfExperience: payload.yearsOfExperience,
                });
            }

            await loadPersonalSkills();
            closeModal();
            setHighlightSkillId(payload.skillId);
            pushToast(
                formMode === 'update' ? 'Cập nhật kỹ năng thành công' : 'Khai báo kỹ năng thành công',
                'Hồ sơ đang ở trạng thái chờ duyệt.'
            );
        } catch (err: any) {
            console.error('Failed to save skill:', err);
            const errMsg = err?.data?.message || err?.message || '';
            if (errMsg.includes('đã có trong hồ sơ') || errMsg.includes('Duplicate')) {
                setDuplicateSkillId(payload.skillId);
            } else {
                pushToast(formMode === 'update' ? 'Lỗi cập nhật' : 'Lỗi khai báo', errMsg || 'Không thể lưu kỹ năng.');
            }
        } finally {
            setSaving(false);
        }
    }

    function confirmSwitchToUpdate() {
        if (duplicateSkillId == null) return;
        const targetId = duplicateSkillId;
        setFormMode('update');
        setEditingSkillId(targetId);
        setDuplicateSkillId(null);
    }

    return (
        <div className="flex flex-col h-full min-h-0 space-y-4 flex-1">
            <ToastList toasts={toasts} onDone={removeToast} />

            {/* ── Header ── */}
            <div className="shrink-0 flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">Khai báo Kỹ năng</h1>
                    <p className="text-sm text-slate-500">
                        Quản lý hồ sơ năng lực, tra cứu nhân sự theo kỹ năng và mức độ rảnh để gán vào dự án.
                    </p>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                    {/* Nút thiết lập thời hạn định kỳ cho Quản lý nguồn lực VT-03 & Admin VT-06 (User Story 9) */}
                    {(roleCode === 'VT-03' || roleCode === 'VT-06') && (
                        <button
                            type="button"
                            onClick={() => {
                                setEditingCampaign(campaign);
                                setCampaignModalOpen(true);
                            }}
                            className="inline-flex items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 px-3 py-2 text-xs font-semibold text-indigo-700 hover:bg-indigo-100 transition shadow-2xs cursor-pointer"
                        >
                            <CalendarClock className="h-4 w-4 text-indigo-600" />
                            Thiết lập thời hạn cập nhật
                        </button>
                    )}

                    {/* Tab switcher hiển thị động theo vai trò */}
                    {visibleTabs.length > 0 && (
                        <div className="flex flex-wrap rounded-xl border border-slate-200 bg-slate-100 p-1 shadow-2xs gap-0.5">
                            {visibleTabs.map((tab) => {
                                const Icon = tab.icon;
                                const isActive = activeTab === tab.id;
                                return (
                                    <button
                                        key={tab.id}
                                        type="button"
                                        onClick={() => setActiveTab(tab.id)}
                                        className={cn(
                                            'inline-flex items-center gap-2 whitespace-nowrap rounded-lg px-4 py-2 text-xs font-semibold transition cursor-pointer',
                                            isActive
                                                ? 'bg-white text-indigo-700 font-bold shadow-xs'
                                                : 'text-slate-600 hover:text-slate-900'
                                        )}
                                    >
                                        <Icon className="h-4 w-4" />
                                        {tab.label}
                                    </button>
                                );
                            })}
                        </div>
                    )}
                </div>
            </div>

            {/* ── Tab Content ── */}
            <div className="flex-1 min-h-0 overflow-y-auto">
                {/* Tab Khai báo cá nhân (Dành cho VT-04 và VT-06) */}
                {activeTab === 'declare' && (roleCode === 'VT-04' || roleCode === 'VT-06') && (
                    <div className="space-y-4">
                        {/* Banner thông báo đợt cập nhật kỹ năng định kỳ (User Story 9) */}
                        <div className="rounded-2xl border border-indigo-100 bg-gradient-to-r from-indigo-50/80 via-sky-50/50 to-white p-4 text-slate-800 shadow-2xs">
                            <div className="flex flex-wrap items-center justify-between gap-3">
                                <div className="flex items-center gap-3">
                                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-xs">
                                        <CalendarClock className="h-5 w-5" />
                                    </div>
                                    <div>
                                        <h3 className="text-sm font-bold text-slate-900">{campaign.title}</h3>
                                        <p className="text-xs text-slate-500 mt-0.5">{campaign.notes}</p>
                                    </div>
                                </div>
                                <div className="flex flex-wrap items-center gap-2 text-xs">
                                    <span className="rounded-lg border border-indigo-200 bg-white px-2.5 py-1 font-medium text-indigo-700">
                                        {campaign.cycleLabel}
                                    </span>
                                    <span className="flex items-center gap-1 rounded-lg border border-amber-200 bg-amber-50 px-2.5 py-1 font-semibold text-amber-800">
                                        <Clock className="h-3.5 w-3.5 text-amber-600" />
                                        Hạn chót: {campaign.deadline}
                                    </span>
                                    <span
                                        className={cn(
                                            'rounded-lg px-2.5 py-1 font-semibold',
                                            campaign.status === 'open'
                                                ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                                                : 'bg-rose-100 text-rose-800 border border-rose-200'
                                        )}
                                    >
                                        {campaign.status === 'open' ? 'Đang mở tiếp nhận' : 'Đã kết thúc'}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <SkillsTable
                            skills={skills}
                            currentRole={roleCode as any}
                            highlightSkillId={highlightSkillId}
                            demoEmpty={demoEmpty}
                            onToggleDemoEmpty={toggleDemoEmpty}
                            onAdd={openCreateModal}
                            onEdit={openEditModal}
                            onDelete={handleDeleteClick}
                        />
                    </div>
                )}

                {/* Tab Ma trận kỹ năng (Dành cho Quản lý & Lãnh đạo) */}
                {activeTab === 'matrix' && ['VT-01', 'VT-02', 'VT-03', 'VT-05', 'VT-06'].includes(roleCode) && (
                    <SkillMatrixView
                        departments={departments}
                        onOpenCatalog={() => setActiveTab('catalog')}
                    />
                )}

                {/* Tab Danh mục kỹ năng (Mọi role có thể xem, chỉ VT-06 sửa/xóa/thêm) */}
                {activeTab === 'catalog' && (
                    <SkillCatalogView
                        catalog={catalog}
                        onUpdateCatalog={setCatalog}
                    />
                )}

                {/* Tab Duyệt kỹ năng (Dành cho VT-03 & VT-06) */}
                {activeTab === 'approve' && (roleCode === 'VT-03' || roleCode === 'VT-06') && (
                    <SkillApproveTable
                        requests={approvalRequests}
                        onApprove={handleApproveRequest}
                        onReject={handleRejectRequest}
                    />
                )}

                {/* Tab Tra cứu nhân lực theo kỹ năng & độ rảnh (Dành cho VT-02, VT-03, VT-06 - User Story 15) */}
                {activeTab === 'search' && ['VT-02', 'VT-03', 'VT-06'].includes(roleCode) && (
                    <SkillresourceSearch
                        embedded
                        departments={departments}
                    />
                )}
            </div>

            {modalOpen && (
                <SkillFormModal
                    key={modalOpen ? `modal-${formMode}-${editingSkillId ?? duplicateSkillId ?? 'new'}` : 'closed'}
                    open={modalOpen}
                    mode={formMode}
                    catalog={catalog}
                    editingSkill={editingSkill}
                    targetSkillId={editingSkillId ?? duplicateSkillId}
                    duplicateSkillName={duplicateSkillName}
                    saving={saving}
                    onClose={closeModal}
                    onSave={handleSave}
                    onConfirmSwitchToUpdate={confirmSwitchToUpdate}
                    onDismissDuplicateWarning={() => setDuplicateSkillId(null)}
                />
            )}

            <ConfirmDeleteModal
                open={Boolean(deletingSkill)}
                skillName={deletingSkill?.name || ''}
                onClose={() => setDeletingSkill(null)}
                onConfirm={handleConfirmDelete}
            />

            {/* Modal Thiết lập thời hạn cập nhật kỹ năng định kỳ (User Story 9) */}
            {campaignModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm animate-in fade-in duration-200">
                    <div className="relative w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl transition-all animate-in zoom-in-95 duration-200 text-slate-900 border border-slate-100">
                        <button
                            type="button"
                            onClick={() => setCampaignModalOpen(false)}
                            className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition cursor-pointer"
                        >
                            <X className="h-5 w-5" />
                        </button>

                        <div className="flex items-center gap-3 mb-4">
                            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
                                <CalendarClock className="h-5 w-5" />
                            </div>
                            <div>
                                <h3 className="text-base font-bold text-slate-900">Thiết lập kỳ cập nhật kỹ năng</h3>
                                <p className="text-xs text-slate-500">Quy định hạn chót và chu kỳ cập nhật năng lực toàn công ty.</p>
                            </div>
                        </div>

                        <form onSubmit={handleSaveCampaign} className="space-y-4">
                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">Tên đợt rà soát</label>
                                <input
                                    type="text"
                                    required
                                    value={editingCampaign.title}
                                    onChange={(e) => setEditingCampaign({ ...editingCampaign, title: e.target.value })}
                                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-600 focus:outline-none"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="block text-xs font-bold text-slate-700 mb-1">Chu kỳ cập nhật</label>
                                    <select
                                        value={editingCampaign.cycle}
                                        onChange={(e) => {
                                            const val = e.target.value;
                                            const labels: Record<string, string> = {
                                                '3_months': 'Hàng quý (3 tháng)',
                                                '6_months': 'Định kỳ 6 tháng',
                                                '12_months': 'Hàng năm (12 tháng)',
                                                'custom': 'Tùy chỉnh',
                                            };
                                            setEditingCampaign({
                                                ...editingCampaign,
                                                cycle: val,
                                                cycleLabel: labels[val] || 'Định kỳ',
                                            });
                                        }}
                                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-600 focus:outline-none"
                                    >
                                        <option value="6_months">Định kỳ 6 tháng</option>
                                        <option value="3_months">Hàng quý (3 tháng)</option>
                                        <option value="12_months">Hàng năm (12 tháng)</option>
                                        <option value="custom">Tùy chỉnh</option>
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-xs font-bold text-slate-700 mb-1">Hạn chót cập nhật</label>
                                    <input
                                        type="date"
                                        required
                                        value={editingCampaign.deadline}
                                        onChange={(e) => setEditingCampaign({ ...editingCampaign, deadline: e.target.value })}
                                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-600 focus:outline-none"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">Trạng thái đợt</label>
                                <select
                                    value={editingCampaign.status}
                                    onChange={(e) => setEditingCampaign({ ...editingCampaign, status: e.target.value as 'open' | 'closed' })}
                                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-600 focus:outline-none"
                                >
                                    <option value="open">Đang mở tiếp nhận</option>
                                    <option value="closed">Đã kết thúc / Đóng đợt</option>
                                </select>
                            </div>

                            <div>
                                <label className="block text-xs font-bold text-slate-700 mb-1">Thông điệp gửi nhân sự</label>
                                <textarea
                                    rows={2}
                                    value={editingCampaign.notes}
                                    onChange={(e) => setEditingCampaign({ ...editingCampaign, notes: e.target.value })}
                                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs focus:border-indigo-600 focus:outline-none"
                                />
                            </div>

                            <div className="flex items-center gap-2 pt-2 border-t border-slate-100">
                                <button
                                    type="button"
                                    onClick={() => setCampaignModalOpen(false)}
                                    className="flex-1 rounded-xl border border-slate-200 py-2.5 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition cursor-pointer"
                                >
                                    Hủy
                                </button>
                                <button
                                    type="submit"
                                    className="flex-1 rounded-xl bg-indigo-600 py-2.5 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs cursor-pointer"
                                >
                                    Lưu cấu hình
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
