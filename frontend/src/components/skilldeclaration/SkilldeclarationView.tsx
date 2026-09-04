import { useState } from 'react';
import { ClipboardList, Search as SearchIcon, ShieldCheck } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
    ConfirmDeleteModal,
    SkillFormModal,
    SkillsTable,
} from './Components.tsx';
import { ToastList } from './ToastNotification.tsx';
import SkillresourceSearch from './SkillresourceSearch.tsx';
import type { DepartmentItem, ResourceEmployee } from './SkillresourceSearch.tsx';
import { INITIAL_SKILLS, SKILL_CATALOG } from './Types.ts';
import type { CatalogSkill, DeclaredSkill, FormMode, SkillPayload, ToastItem } from './Types.ts';

let toastSeq = 0;

export type ModuleTab = 'declare' | 'search' | 'approve';

const MODULE_TABS: { id: ModuleTab; label: string; icon: typeof SearchIcon }[] = [
    { id: 'declare', label: 'Khai báo cá nhân', icon: ClipboardList },
    { id: 'search', label: 'Tra cứu & Tìm kiếm nhân sự', icon: SearchIcon },
    { id: 'approve', label: 'Duyệt kỹ năng', icon: ShieldCheck },
];

export interface SkilldeclarationViewProps {
    // Nhận danh sách phòng ban & nhân sự từ ứng dụng cha (đồng bộ với module Phòng ban)
    departments?: DepartmentItem[];
    employees?: ResourceEmployee[];
}

export default function SkilldeclarationView({
                                                 departments = [],
                                                 employees = [],
                                             }: SkilldeclarationViewProps) {
    const [activeTab, setActiveTab] = useState<ModuleTab>('declare');

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

    function handleApprove(skillId: number | string) {
        setSkills((prev) =>
            prev.map((s) => (s.skillId === skillId ? { ...s, status: 'approved' } : s))
        );
        pushToast('Đã phê duyệt', 'Kỹ năng đã chuyển sang trạng thái Đã xác nhận.');
    }

    function handleReject(skillId: number | string) {
        setSkills((prev) =>
            prev.map((s) => (s.skillId === skillId ? { ...s, status: 'rejected' } : s))
        );
        pushToast('Đã từ chối', 'Kỹ năng đã chuyển sang trạng thái Bị từ chối.');
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
        <div className="w-full space-y-6">
            <ToastList toasts={toasts} onDone={removeToast} />

            <div className="w-full rounded-3xl bg-gradient-to-br from-[#7c3aed] via-[#4f46e5] to-[#2563eb] p-6 text-white shadow-xl sm:p-8">

                <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                    <div>
                        <h1 className="text-2xl font-bold tracking-tight">Khai báo Kỹ năng</h1>
                        <p className="mt-1 text-sm text-white/70">
                            Quản lý hồ sơ năng lực, tra cứu nhân sự theo kỹ năng và mức độ rảnh để gán vào dự án.
                        </p>
                    </div>

                    <div className="flex flex-wrap items-center gap-1.5 rounded-2xl border border-white/20 bg-white/10 p-1.5 backdrop-blur-md">
                        {MODULE_TABS.map((tab) => {
                            const Icon = tab.icon;
                            const isActive = activeTab === tab.id;
                            return (
                                <button
                                    key={tab.id}
                                    type="button"
                                    onClick={() => setActiveTab(tab.id)}
                                    className={cn(
                                        'inline-flex items-center gap-2 rounded-xl px-3.5 py-2 text-xs font-bold transition-all',
                                        isActive
                                            ? 'bg-white text-[#4338ca] shadow-sm'
                                            : 'text-white/80 hover:bg-white/10 hover:text-white'
                                    )}
                                >
                                    <Icon className="h-4 w-4" />
                                    {tab.label}
                                </button>
                            );
                        })}
                    </div>
                </div>

                <div className="my-6 border-t border-white/15" />

                {/* Tab Khai báo: gắn quyền VT-04 (Nhân viên) */}
                {activeTab === 'declare' && (
                    <SkillsTable
                        skills={skills}
                        currentRole="VT-04"
                        highlightSkillId={highlightSkillId}
                        demoEmpty={demoEmpty}
                        onToggleDemoEmpty={toggleDemoEmpty}
                        onAdd={openCreateModal}
                        onEdit={openEditModal}
                        onDelete={handleDeleteClick}
                    />
                )}

                {/* Tab Tra cứu: truyền departments/employees để luôn đồng bộ với module Phòng ban */}
                {activeTab === 'search' && (
                    <SkillresourceSearch embedded departments={departments} employees={employees} />
                )}

                {/* Tab Duyệt: gắn quyền VT-01 (Quản lý) */}
                {activeTab === 'approve' && (
                    <SkillsTable
                        skills={skills}
                        currentRole="VT-01"
                        highlightSkillId={highlightSkillId}
                        demoEmpty={demoEmpty}
                        onToggleDemoEmpty={toggleDemoEmpty}
                        onDelete={handleDeleteClick}
                        onApprove={handleApprove}
                        onReject={handleReject}
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