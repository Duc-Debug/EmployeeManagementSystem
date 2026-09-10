import { useState, useEffect } from 'react';
import { ClipboardList, Search as SearchIcon, ShieldCheck, LayoutGrid, BookOpen } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
    ConfirmDeleteModal,
    SkillFormModal,
    SkillsTable,
} from './Components.tsx';
import { ToastList } from './ToastNotification.tsx';
import type { DepartmentItem, ResourceEmployee } from './SkillresourceSearch.tsx';
import type { CatalogSkill, DeclaredSkill, FormMode, SkillPayload, ToastItem, PendingApprovalSkill, SkillStatus } from './Types.ts';
import SkillApproveTable from './SkillApproveTable.tsx';
import SkillMatrixView from './SkillMatrixView.tsx';
import SkillCatalogView from './SkillCatalogView.tsx';

import { useAuthUser } from '@/lib/auth-session';
import {
    getSkills,
    getMySkills,
    declareMySkill,
    deleteMySkill,
    getPendingSkills,
    approveSkill,
    rejectSkill,
} from '@/lib/api/skills';

let toastSeq = 0;

export type ModuleTab = 'declare' | 'matrix' | 'catalog' | 'approve';

const MODULE_TABS: { id: ModuleTab; label: string; icon: typeof SearchIcon; allowedRoles: string[] }[] = [
    { id: 'declare', label: 'Khai báo cá nhân', icon: ClipboardList, allowedRoles: ['VT-04'] },
    { id: 'matrix', label: 'Ma trận kỹ năng bộ phận', icon: LayoutGrid, allowedRoles: ['VT-01', 'VT-02', 'VT-03', 'VT-05', 'VT-06'] },
    { id: 'catalog', label: 'Danh mục kỹ năng', icon: BookOpen, allowedRoles: ['VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06'] },
    { id: 'approve', label: 'Duyệt kỹ năng', icon: ShieldCheck, allowedRoles: ['VT-03', 'VT-06'] },
];

export interface SkilldeclarationViewProps {
    departments?: DepartmentItem[];
    employees?: ResourceEmployee[];
    initialTab?: ModuleTab;
}

export default function SkilldeclarationView({
    departments = [],
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
            if (data && data.length > 0) {
                const mapped: CatalogSkill[] = data.map((s) => ({
                    id: s.id,
                    name: s.name,
                    category: s.groupName || 'Khác',
                    groupId: s.groupId,
                    description: s.description,
                    version: s.version,
                }));
                setCatalog(mapped);
            }
        } catch (err) {
            console.error('Failed to load skills catalog from backend:', err);
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

    function handleAddNewCatalogSkill(skillName: string) {
        const newId = Date.now();
        const newSkillItem: CatalogSkill = { id: newId, name: skillName, category: 'Khác' };
        setCatalog((prev) => [...prev, newSkillItem]);
        return newId;
    }

    const editingSkill = editingSkillId != null ? skills.find((s) => s.skillId === editingSkillId) || null : null;
    const duplicateSkillName = duplicateSkillId != null ? catalogById[duplicateSkillId]?.name ?? null : null;

    async function handleSave(payload: SkillPayload) {
        setSaving(true);
        try {
            await declareMySkill({
                skillId: payload.skillId,
                proficiencyLevel: payload.proficiencyLevel,
                yearsOfExperience: payload.yearsOfExperience,
            });

            await loadPersonalSkills();
            closeModal();
            setHighlightSkillId(payload.skillId);
            pushToast(
                formMode === 'update' ? 'Cập nhật kỹ năng thành công' : 'Khai báo kỹ năng thành công',
                'Hồ sơ đang ở trạng thái chờ duyệt.'
            );
        } catch (err: any) {
            console.error('Failed to declare skill:', err);
            const errMsg = err?.data?.message || err?.message || '';
            if (errMsg.includes('đã có trong hồ sơ') || errMsg.includes('Duplicate')) {
                setDuplicateSkillId(payload.skillId);
            } else {
                pushToast('Lỗi khai báo', errMsg || 'Không thể lưu kỹ năng.');
            }
        } finally {
            setSaving(false);
        }
    }

    function confirmSwitchToUpdate() {
        if (duplicateSkillId == null) return;
        setFormMode('update');
        setEditingSkillId(duplicateSkillId);
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

            {/* ── Tab Content ── */}
            <div className="flex-1 min-h-0 overflow-y-auto">
                {/* Tab Khai báo cá nhân (Dành riêng cho VT-04) */}
                {activeTab === 'declare' && roleCode === 'VT-04' && (
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
            </div>

            {modalOpen && (
                <SkillFormModal
                    key={modalOpen ? `modal-${formMode}-${editingSkillId ?? 'new'}` : 'closed'}
                    open={modalOpen}
                    mode={formMode}
                    catalog={catalog}
                    editingSkill={editingSkill}
                    duplicateSkillName={duplicateSkillName}
                    saving={saving}
                    onClose={closeModal}
                    onSave={handleSave}
                    onAddNewCatalogSkill={handleAddNewCatalogSkill}
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
        </div>
    );
}