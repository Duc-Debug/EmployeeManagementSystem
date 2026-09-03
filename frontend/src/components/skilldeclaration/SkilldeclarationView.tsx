import { useState } from 'react';
import {
    ConfirmDeleteModal,
    SkillFormModal,
    SkillsTable,
    TopBar,
} from './Components.tsx';
import { ToastList } from './ToastNotification.tsx';
import { INITIAL_SKILLS, SKILL_CATALOG } from './Types.ts';
import type { CatalogSkill, DeclaredSkill, FormMode, Role, SkillPayload, ToastItem } from './Types.ts';

let toastSeq = 0;

export default function SkilldeclarationView() {
    const [currentRole, setCurrentRole] = useState<Role>('VT-04');

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
        <div className="space-y-6 w-full">
            <ToastList toasts={toasts} onDone={removeToast} />

            <TopBar currentRole={currentRole} onRoleChange={setCurrentRole} />

            <SkillsTable
                skills={skills}
                currentRole={currentRole}
                highlightSkillId={highlightSkillId}
                demoEmpty={demoEmpty}
                onToggleDemoEmpty={toggleDemoEmpty}
                onAdd={openCreateModal}
                onEdit={openEditModal}
                onDelete={handleDeleteClick}
                onApprove={handleApprove}
                onReject={handleReject}
            />

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