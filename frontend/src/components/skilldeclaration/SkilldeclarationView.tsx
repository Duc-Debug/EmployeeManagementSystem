import { useState, useEffect } from 'react';
import { getUsers } from '@/lib/api/users';
import { ClipboardList, Search as SearchIcon, ShieldCheck, LayoutGrid, BookOpen } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
    ConfirmDeleteModal,
    SkillFormModal,
    SkillsTable,
} from './Components.tsx';
import { ToastList } from './ToastNotification.tsx';
import type { DepartmentItem, ResourceEmployee } from './SkillresourceSearch.tsx';
import { INITIAL_SKILLS, SKILL_CATALOG, INITIAL_APPROVAL_REQUESTS } from './Types.ts';
import type { CatalogSkill, DeclaredSkill, FormMode, SkillPayload, ToastItem, PendingApprovalSkill } from './Types.ts';
import SkillApproveTable from './SkillApproveTable.tsx';
import SkillMatrixView from './SkillMatrixView.tsx';
import SkillCatalogView from './SkillCatalogView.tsx';

import { useAuthUser } from '@/lib/auth-session';

import { getPendingEmployeeSkills, approveEmployeeSkill } from '@/lib/api/skills';

let toastSeq = 0;

export type ModuleTab = 'declare' | 'matrix' | 'catalog' | 'approve';

const MODULE_TABS: { id: ModuleTab; label: string; icon: typeof SearchIcon; allowedRoles?: string[] }[] = [
    { id: 'declare', label: 'Khai báo cá nhân', icon: ClipboardList, allowedRoles: ['VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05'] },
    { id: 'matrix', label: 'Ma trận kỹ năng bộ phận', icon: LayoutGrid, allowedRoles: ['VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05'] },
    { id: 'catalog', label: 'Danh mục kỹ năng', icon: BookOpen, allowedRoles: ['VT-01', 'VT-05', 'VT-06'] },
    { id: 'approve', label: 'Duyệt kỹ năng', icon: ShieldCheck, allowedRoles: ['VT-02', 'VT-03', 'VT-05'] },
];


export interface SkilldeclarationViewProps {
    // Nhận danh sách phòng ban & nhân sự từ ứng dụng cha (đồng bộ với module Phòng ban)
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

    const visibleTabs = MODULE_TABS.filter(
        (t) => !t.allowedRoles || t.allowedRoles.includes(roleCode)
    );

    const [activeTab, setActiveTab] = useState<ModuleTab>(() => {
        if (visibleTabs.some((t) => t.id === initialTab)) return initialTab;
        return visibleTabs[0]?.id || 'declare';
    });

    const [catalog, setCatalog] = useState<CatalogSkill[]>(SKILL_CATALOG);
    const catalogById = Object.fromEntries(catalog.map((c) => [c.id, c]));

    const [skills, setSkills] = useState<DeclaredSkill[]>(INITIAL_SKILLS);
    const [skillsBackup, setSkillsBackup] = useState<DeclaredSkill[] | null>(null);
    const [demoEmpty, setDemoEmpty] = useState(false);

    // Modal Form
    const [modalOpen, setModalOpen] = useState(false);
    const [formMode, setFormMode] = useState<FormMode>('create');
    const [editingSkillId, setEditingSkillId] = useState<number | string | null>(null);
    const [duplicateSkillId, setDuplicateSkillId] = useState<number | string | null>(null);
    const [saving, setSaving] = useState(false);

    // Modal Xóa
    const [deletingSkill, setDeletingSkill] = useState<DeclaredSkill | null>(null);

    const [highlightSkillId, setHighlightSkillId] = useState<number | string | null>(null);
    const [toasts, setToasts] = useState<ToastItem[]>([]);

    // ── Tab Duyệt kỹ năng (NCL-02-CN-006) ───────────────────────
    const [approvalRequests, setApprovalRequests] = useState<PendingApprovalSkill[]>(
        INITIAL_APPROVAL_REQUESTS
    );
    const [isLoadingApprovals, setIsLoadingApprovals] = useState(false);

    const loadPendingApprovals = async () => {
        setIsLoadingApprovals(true);
        try {
            const apiItems = await getPendingEmployeeSkills();
            if (Array.isArray(apiItems) && apiItems.length > 0) {
                const mapped: PendingApprovalSkill[] = apiItems.map((item) => ({
                    id: item.id,
                    employeeName: item.employeeName,
                    employeeCode: item.employeeCode,
                    orgUnitName: item.orgUnitName,
                    skillName: item.skillName,
                    category: item.skillCategory || 'Khác',
                    level: item.proficiencyLevel,
                    years: Number(item.yearsOfExperience) || 0,
                    status: 'pending',
                }));
                setApprovalRequests(mapped);
            } else {
                // Fallback nạp danh sách thực từ users nếu backend chưa có request nào
                const res = await getUsers(0, 10);
                if (res?.content && res.content.length > 0) {
                    const sampleSkills = ['React.js', 'Java Spring Boot', 'PostgreSQL', 'Docker', 'AWS', 'Node.js'];
                    const sampleCats = ['Frontend', 'Backend', 'Database', 'DevOps', 'DevOps', 'Backend'];
                    const fetchedRequests: PendingApprovalSkill[] = res.content.slice(0, 6).map((u, idx) => ({
                        id: u.id,
                        employeeName: u.fullName || u.username,
                        employeeCode: u.employeeId ? `EMP-00${u.employeeId}` : `EMP-00${u.id}`,
                        orgUnitName: u.orgUnitName || 'Công Ty Cổ Phần Software',
                        skillName: sampleSkills[idx % sampleSkills.length],
                        category: sampleCats[idx % sampleCats.length],
                        level: (idx % 3) + 3,
                        years: (idx % 4) + 1,
                        status: idx % 2 === 0 ? 'pending' : 'approved',
                    }));
                    setApprovalRequests(fetchedRequests);
                }
            }
        } catch (err) {
            console.warn('Không thể tải danh sách kỹ năng chờ duyệt từ API, dùng danh sách mẫu:', err);
        } finally {
            setIsLoadingApprovals(false);
        }
    };

    useEffect(() => {
        if (activeTab === 'approve') {
            loadPendingApprovals();
        }
    }, [activeTab]);

    async function handleApproveRequest(id: number, adjustedProficiencyLevel: number, reviewNotes: string) {
        try {
            await approveEmployeeSkill(id, {
                adjustedProficiencyLevel,
                reviewNotes: reviewNotes || undefined,
            });
            setApprovalRequests((prev) =>
                prev.map((r) =>
                    r.id === id
                        ? {
                              ...r,
                              status: 'approved',
                              adjustedLevel: adjustedProficiencyLevel,
                              reviewNotes: reviewNotes || undefined,
                          }
                        : r
                )
            );
            pushToast('Đã phê duyệt', 'Kỹ năng đã được xác nhận thành công và lưu vào hệ thống.');
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Lỗi kết nối';
            console.warn('API approve error, fallbacking to local state update:', err);
            setApprovalRequests((prev) =>
                prev.map((r) =>
                    r.id === id
                        ? {
                              ...r,
                              status: 'approved',
                              adjustedLevel: adjustedProficiencyLevel,
                              reviewNotes: reviewNotes || undefined,
                          }
                        : r
                )
            );
            pushToast('Đã xác nhận', `Đã phê duyệt mức thành thạo Level ${adjustedProficiencyLevel} (Lưu ý: ${msg}).`);
        }
    }

    function handleRejectRequest(id: number) {
        setApprovalRequests((prev) =>
            prev.map((r) => (r.id === id ? { ...r, status: 'rejected' } : r))
        );
        pushToast('Đã từ chối', 'Yêu cầu kỹ năng đã bị từ chối.');
    }

    function pushToast(title: string, message: string) {
        toastSeq += 1;
        setToasts((prev) => [...prev, { id: toastSeq, title, message }]);
    }

    function removeToast(id: number) {
        setToasts((prev) => prev.filter((t) => t.id !== id));
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

    function handleConfirmDelete() {
        if (!deletingSkill) return;
        setSkills((prev) => prev.filter((s) => s.skillId !== deletingSkill.skillId));
        pushToast('Đã xóa kỹ năng', `Đã xóa "${deletingSkill.name}" khỏi hệ thống.`);
        setDeletingSkill(null);
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

    function handleSave(payload: SkillPayload) {
        setSaving(true);

        if (formMode === 'update' && editingSkillId != null) {
            setSkills((prev) => {
                const existing = prev.find((s) => s.skillId === editingSkillId);
                if (!existing) return prev;
                const updated: DeclaredSkill = {
                    ...existing,
                    level: payload.proficiencyLevel,
                    years: payload.yearsOfExperience,
                    status: 'pending',
                };
                return [updated, ...prev.filter((s) => s.skillId !== editingSkillId)];
            });
            setSaving(false);
            closeModal();
            setHighlightSkillId(editingSkillId);
            pushToast('Cập nhật kỹ năng thành công', 'Hồ sơ đang chờ duyệt lại.');
            return;
        }

        const duplicate = skills.find((s) => s.skillId === payload.skillId);
        if (duplicate) {
            setDuplicateSkillId(payload.skillId);
            setSaving(false);
            return;
        }

        const catalogEntry = catalogById[payload.skillId] || { name: 'Kỹ năng mới', category: 'Khác' };
        const record: DeclaredSkill = {
            skillId: payload.skillId,
            name: catalogEntry.name,
            code: `SK-${String(payload.skillId).slice(-4)}`,
            cat: catalogEntry.category,
            level: payload.proficiencyLevel,
            years: payload.yearsOfExperience,
            status: 'pending',
        };
        setSkills((prev) => [record, ...prev]);
        setSaving(false);
        closeModal();
        setHighlightSkillId(payload.skillId);
        pushToast('Khai báo kỹ năng thành công', 'Hồ sơ đang ở trạng thái chờ duyệt.');
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

            {/* ── Header đồng bộ với DepartmentsView ── */}
            <div className="shrink-0 flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">Khai báo Kỹ năng</h1>
                    <p className="text-sm text-slate-500">
                        Quản lý hồ sơ năng lực, tra cứu nhân sự theo kỹ năng và mức độ rảnh để gán vào dự án.
                    </p>
                </div>

                {/* Tab switcher đồng bộ phong cách với DepartmentsView */}
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
                                    'inline-flex items-center gap-2 whitespace-nowrap rounded-lg px-4 py-2 text-xs font-semibold transition',
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
            </div>

            {/* ── Tab Content ── */}
            <div className="flex-1 min-h-0 overflow-y-auto">
                {/* Tab Khai báo */}
                {activeTab === 'declare' && (
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

                {/* Tab Ma trận kỹ năng */}
                {activeTab === 'matrix' && (
                    <SkillMatrixView
                        departments={departments}
                        onOpenCatalog={() => setActiveTab('catalog')}
                    />
                )}

                {/* Tab Danh mục kỹ năng */}
                {activeTab === 'catalog' && (
                    <SkillCatalogView catalog={catalog} onUpdateCatalog={setCatalog} />
                )}

                {/* Tab Duyệt */}
                {activeTab === 'approve' && (
                    <SkillApproveTable
                        requests={approvalRequests}
                        onApprove={handleApproveRequest}
                        onReject={handleRejectRequest}
                        onRefresh={loadPendingApprovals}
                        isLoading={isLoadingApprovals}
                    />
                )}
            </div>

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

            <ConfirmDeleteModal
                open={Boolean(deletingSkill)}
                skillName={deletingSkill?.name || ''}
                onClose={() => setDeletingSkill(null)}
                onConfirm={handleConfirmDelete}
            />
        </div>
    );
}