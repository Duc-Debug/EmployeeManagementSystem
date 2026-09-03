import { useState } from 'react';
import { Award, Check, Plus, Trash2, UserCheck, Edit3, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { PROFICIENCY_LEVELS } from './Types.ts';
import type { CatalogSkill, DeclaredSkill, FormMode, Role, SkillPayload } from './Types.ts';
import { SkillSelect } from './SkillSelect.tsx';

/* ============================= TopBar ================================ */

interface TopBarProps {
    currentRole: Role;
    onRoleChange: (role: Role) => void;
}

export function TopBar({ currentRole, onRoleChange }: TopBarProps) {
    return (
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-white/20 pb-4">
            <div>
                <h1 className="text-2xl font-bold tracking-tight text-white">
                    Khai báo Kỹ năng cá nhân
                </h1>
                <p className="text-sm text-white/70">
                    Quản lý hồ sơ năng lực — khai báo kỹ năng, mức thành thạo và theo dõi trạng thái phê duyệt.
                </p>
            </div>
            <div className="flex rounded-xl border border-white/20 bg-white/10 p-1 backdrop-blur-md">
                <button
                    onClick={() => onRoleChange('VT-04')}
                    className={cn(
                        "flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-bold transition",
                        currentRole === 'VT-04'
                            ? "bg-white text-slate-900 shadow-sm"
                            : "text-white/80 hover:text-white"
                    )}
                >
                    VT-04 · Nhân viên
                </button>
                <button
                    onClick={() => onRoleChange('OTHER')}
                    className={cn(
                        "flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-bold transition",
                        currentRole === 'OTHER'
                            ? "bg-white text-slate-900 shadow-sm"
                            : "text-white/80 hover:text-white"
                    )}
                >
                    <UserCheck className="h-4 w-4" />
                    VT-01 · Quản lý (Duyệt)
                </button>
            </div>
        </div>
    );
}

/* ============================= SkillsTable ============================= */

function Stars({ level }: { level: number }) {
    return (
        <div className="flex items-center gap-1.5">
            <span className="flex text-amber-400">
                {[1, 2, 3, 4, 5].map((i) => (
                    <span key={i} className={i <= level ? 'opacity-100' : 'opacity-25'}>★</span>
                ))}
            </span>
            <span className="text-xs font-medium text-slate-500">({level}/5)</span>
        </div>
    );
}

function StatusBadge({ status }: { status: DeclaredSkill['status'] }) {
    if (status === 'approved') {
        return (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-500"></span> Đã xác nhận
            </span>
        );
    }
    if (status === 'rejected') {
        return (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-0.5 text-xs font-medium text-rose-700">
                <span className="h-1.5 w-1.5 rounded-full bg-rose-500"></span> Bị từ chối
            </span>
        );
    }
    return (
        <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-2.5 py-0.5 text-xs font-medium text-amber-700">
            <span className="h-1.5 w-1.5 rounded-full bg-amber-500"></span> Chờ xác nhận
        </span>
    );
}

interface SkillsTableProps {
    skills: DeclaredSkill[];
    highlightSkillId: number | string | null;
    demoEmpty: boolean;
    currentRole: Role;
    onToggleDemoEmpty: (checked: boolean) => void;
    onAdd: () => void;
    onEdit: (skillId: number | string) => void;
    onDelete: (skill: DeclaredSkill) => void;
    onApprove?: (skillId: number | string) => void;
    onReject?: (skillId: number | string) => void;
}

export function SkillsTable({
                                skills,
                                currentRole,
                                onAdd,
                                onEdit,
                                onDelete,
                                onApprove,
                                onReject,
                            }: SkillsTableProps) {
    const isManager = currentRole === 'OTHER';

    return (
        <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h2 className="text-lg font-bold text-white">
                        {isManager ? 'Màn hình phê duyệt kỹ năng' : 'Kỹ năng đã khai báo'}
                    </h2>
                    <p className="text-xs text-white/70">
                        {isManager ? 'Xem và phê duyệt yêu cầu từ nhân viên' : `Bạn đã khai báo ${skills.length} kỹ năng`}
                    </p>
                </div>
                {!isManager && (
                    <button
                        onClick={onAdd}
                        className="flex items-center gap-2 rounded-xl bg-violet-600 hover:bg-violet-500 px-4 py-2 text-xs font-bold text-white transition shadow-md"
                    >
                        <Plus className="h-4 w-4" />
                        Khai báo kỹ năng mới
                    </button>
                )}
            </div>

            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-lg">
                {skills.length === 0 ? (
                    <div className="flex flex-col items-center justify-center p-12 text-center text-slate-500">
                        <Award className="h-12 w-12 mb-3 stroke-1 text-slate-400" />
                        <p className="text-sm">Chưa có kỹ năng nào trong danh sách.</p>
                    </div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm text-slate-900">
                            <thead className="border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-500 bg-slate-50/80">
                            <tr>
                                <th className="px-5 py-3.5">Tên Kỹ năng</th>
                                <th className="px-5 py-3.5">Phân loại</th>
                                <th className="px-5 py-3.5">Mức thành thạo</th>
                                <th className="px-5 py-3.5">Kinh nghiệm</th>
                                <th className="px-5 py-3.5">Trạng thái</th>
                                <th className="px-5 py-3.5 text-right">Thao tác</th>
                            </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-100">
                            {skills.map((s) => (
                                <tr key={s.skillId} className="hover:bg-slate-50/80 transition-colors">
                                    <td className="px-5 py-4">
                                        <div className="font-semibold text-slate-900">{s.name}</div>
                                        <div className="text-xs text-slate-400">{s.code}</div>
                                    </td>
                                    <td className="px-5 py-4">
                                            <span className="rounded-full border border-slate-200 bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-700">
                                                {s.cat}
                                            </span>
                                    </td>
                                    <td className="px-5 py-4">
                                        <Stars level={s.level} />
                                    </td>
                                    <td className="px-5 py-4 font-medium text-slate-700">
                                        {s.years} năm
                                    </td>
                                    <td className="px-5 py-4">
                                        <StatusBadge status={s.status} />
                                    </td>
                                    <td className="px-5 py-4 text-right">
                                        <div className="flex items-center justify-end gap-1.5">
                                            {isManager && (
                                                <>
                                                    <button
                                                        title="Phê duyệt"
                                                        onClick={() => onApprove?.(s.skillId)}
                                                        className="rounded-lg p-2 text-emerald-600 hover:bg-emerald-50 transition"
                                                    >
                                                        <Check className="h-4 w-4" />
                                                    </button>
                                                    <button
                                                        title="Từ chối"
                                                        onClick={() => onReject?.(s.skillId)}
                                                        className="rounded-lg p-2 text-rose-600 hover:bg-rose-50 transition"
                                                    >
                                                        <X className="h-4 w-4" />
                                                    </button>
                                                </>
                                            )}

                                            {!isManager && (
                                                <button
                                                    title="Sửa"
                                                    onClick={() => onEdit(s.skillId)}
                                                    className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
                                                >
                                                    <Edit3 className="h-4 w-4" />
                                                </button>
                                            )}
                                            <button
                                                title="Xóa kỹ năng"
                                                onClick={() => onDelete(s)}
                                                className="rounded-lg p-2 text-rose-500 hover:bg-rose-50 hover:text-rose-700 transition"
                                            >
                                                <Trash2 className="h-4 w-4" />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}

/* ========================== ConfirmDeleteModal ========================== */

interface ConfirmDeleteModalProps {
    open: boolean;
    skillName: string;
    onClose: () => void;
    onConfirm: () => void;
}

export function ConfirmDeleteModal({ open, skillName, onClose, onConfirm }: ConfirmDeleteModalProps) {
    if (!open) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
            <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl text-slate-900 space-y-4">
                <h3 className="text-lg font-bold">Xác nhận xóa kỹ năng</h3>
                <p className="text-sm text-slate-600">
                    Bạn có chắc chắn muốn xóa kỹ năng <strong className="text-slate-900">"{skillName}"</strong> khỏi danh sách?
                </p>

                <div className="flex justify-end gap-3 pt-2">
                    <button
                        onClick={onClose}
                        className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50 transition"
                    >
                        Hủy
                    </button>
                    <button
                        onClick={onConfirm}
                        className="rounded-xl bg-rose-600 hover:bg-rose-700 px-4 py-2 text-xs font-bold text-white transition shadow-sm"
                    >
                        Xóa kỹ năng
                    </button>
                </div>
            </div>
        </div>
    );
}

/* ============================ SkillFormModal ============================ */

interface SkillFormModalProps {
    open: boolean;
    mode: FormMode;
    catalog: CatalogSkill[];
    editingSkill?: DeclaredSkill | null;
    duplicateSkillName?: string | null;
    saving: boolean;
    onClose: () => void;
    onSave: (payload: SkillPayload) => void;
    onAddNewCatalogSkill?: (skillName: string) => number;
    onConfirmSwitchToUpdate?: () => void;
    onDismissDuplicateWarning?: () => void;
}

function SkillFormModalContent({
                                   mode,
                                   catalog,
                                   editingSkill,
                                   duplicateSkillName,
                                   saving,
                                   onClose,
                                   onSave,
                                   onAddNewCatalogSkill,
                                   onConfirmSwitchToUpdate,
                                   onDismissDuplicateWarning,
                               }: SkillFormModalProps) {
    // Khởi tạo state trực tiếp từ props khi component mount
    const [skillId, setSkillId] = useState(
        mode === 'update' && editingSkill ? String(editingSkill.skillId) : ''
    );
    const [level, setLevel] = useState<number | null>(
        mode === 'update' && editingSkill ? editingSkill.level : null
    );
    const [years, setYears] = useState(
        mode === 'update' && editingSkill ? String(editingSkill.years) : ''
    );
    const [errors, setErrors] = useState<{ skillId?: boolean; level?: boolean; years?: boolean }>({});

    const handleSave = () => {
        const numericSkillId = Number(skillId);
        const hasSkillError = !skillId || isNaN(numericSkillId);
        const hasLevelError = level === null;
        const numericYears = parseFloat(years);
        const hasYearsError = !years || isNaN(numericYears) || numericYears <= 0;

        if (hasSkillError || hasLevelError || hasYearsError) {
            setErrors({
                skillId: hasSkillError,
                level: hasLevelError,
                years: hasYearsError,
            });
            return;
        }

        onSave({
            skillId: numericSkillId,
            proficiencyLevel: level,
            yearsOfExperience: numericYears,
        });
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
            <div className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl text-slate-900 space-y-5">
                <div>
                    <h3 className="text-lg font-bold">
                        {mode === 'update' ? 'Cập nhật mức thành thạo' : 'Khai báo kỹ năng mới'}
                    </h3>
                    <p className="text-xs text-slate-500">Điền thông tin kỹ năng bạn muốn khai báo.</p>
                </div>

                {duplicateSkillName && (
                    <div className="rounded-xl border border-amber-200 bg-amber-50 p-3.5 text-xs text-amber-800 space-y-2">
                        <p className="font-medium">
                            ⚠️ Kỹ năng <strong>"{duplicateSkillName}"</strong> đã được bạn khai báo trước đó.
                        </p>
                        <div className="flex gap-2">
                            <button
                                type="button"
                                onClick={onConfirmSwitchToUpdate}
                                className="rounded-lg bg-amber-100 hover:bg-amber-200 border border-amber-300 px-3 py-1 text-xs font-bold text-amber-900 transition"
                            >
                                Chuyển sang Cập nhật
                            </button>
                            <button
                                type="button"
                                onClick={onDismissDuplicateWarning}
                                className="rounded-lg bg-white hover:bg-slate-100 border border-slate-200 px-3 py-1 text-xs font-bold text-slate-700 transition"
                            >
                                Bỏ qua
                            </button>
                        </div>
                    </div>
                )}

                {/* Tên kỹ năng */}
                <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700">
                        Tên kỹ năng <span className="text-rose-500">*</span>
                    </label>
                    <SkillSelect
                        catalog={catalog}
                        value={skillId}
                        disabled={mode === 'update'}
                        onChange={(selectedId) => {
                            setSkillId(selectedId);
                            setErrors((prev) => ({ ...prev, skillId: false }));
                            if (onDismissDuplicateWarning) onDismissDuplicateWarning();
                        }}
                        onAddNewSkill={(newSkillName) => {
                            if (onAddNewCatalogSkill) {
                                const newId = onAddNewCatalogSkill(newSkillName);
                                setSkillId(String(newId));
                                setErrors((prev) => ({ ...prev, skillId: false }));
                            }
                        }}
                    />
                    {errors.skillId && <p className="text-xs text-rose-500">Vui lòng chọn kỹ năng.</p>}
                </div>

                {/* Mức thành thạo */}
                <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700">
                        Mức thành thạo <span className="text-rose-500">*</span>
                    </label>
                    <div className="grid grid-cols-5 gap-2">
                        {PROFICIENCY_LEVELS.map((l) => (
                            <button
                                key={l.level}
                                type="button"
                                onClick={() => {
                                    setLevel(l.level);
                                    setErrors((prev) => ({ ...prev, level: false }));
                                }}
                                className={cn(
                                    "flex flex-col items-center justify-center rounded-xl border p-2.5 transition text-center",
                                    level === l.level
                                        ? "border-violet-600 bg-violet-50 text-violet-700 font-bold ring-1 ring-violet-600 shadow-sm"
                                        : "border-slate-200 bg-slate-50 text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                                )}
                            >
                                <span className="text-base font-bold">{l.level}</span>
                                <span className="text-[10px] opacity-80 mt-0.5">{l.label}</span>
                            </button>
                        ))}
                    </div>
                    {errors.level && <p className="text-xs text-rose-500">Vui lòng chọn 1 mức thành thạo từ 1 đến 5.</p>}
                </div>

                {/* Số năm kinh nghiệm */}
                <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700">
                        Số năm kinh nghiệm <span className="text-rose-500">*</span>
                    </label>
                    <div className="relative flex items-center">
                        <input
                            type="number"
                            min={0}
                            step={0.5}
                            placeholder="3.5"
                            value={years}
                            onChange={(e) => {
                                setYears(e.target.value);
                                setErrors((prev) => ({ ...prev, years: false }));
                            }}
                            className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-sm text-slate-900 placeholder-slate-400 focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                        />
                        <span className="absolute right-3.5 text-xs font-medium text-slate-400">năm</span>
                    </div>
                    {errors.years && <p className="text-xs text-rose-500">Vui lòng nhập số năm kinh nghiệm hợp lệ.</p>}
                </div>

                <div className="flex justify-end gap-3 pt-3 border-t border-slate-100">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-xs font-bold text-slate-700 hover:bg-slate-50 transition"
                    >
                        Hủy
                    </button>
                    <button
                        type="button"
                        disabled={saving}
                        onClick={handleSave}
                        className="rounded-xl bg-violet-600 hover:bg-violet-700 px-5 py-2 text-xs font-bold text-white transition shadow-sm disabled:opacity-50"
                    >
                        {saving ? 'Đang lưu...' : 'Lưu khai báo'}
                    </button>
                </div>
            </div>
        </div>
    );
}

export function SkillFormModal(props: SkillFormModalProps) {
    if (!props.open) return null;

    // Dùng key để tự động reset state mỗi khi mở modal hoặc thay đổi kỹ năng chỉnh sửa
    const key = props.mode === 'update' && props.editingSkill ? `edit-${props.editingSkill.skillId}` : 'create';

    return <SkillFormModalContent key={key} {...props} />;
}