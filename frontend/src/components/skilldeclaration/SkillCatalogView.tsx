import { useState, useMemo, useRef, useEffect } from 'react';
import { Plus, Pencil, Lock, Unlock, BookOpen, Check, X, ChevronDown, FolderPlus, AlertCircle } from 'lucide-react';
import type { CatalogSkill } from './Types.ts';
import { SKILL_CATALOG } from './Types.ts';
import { useAuthUser } from '@/lib/auth-session';
import {
    getSkills,
    getSkillGroups,
    createSkill,
    updateSkill,
    deactivateSkill,
    createSkillGroup,
    type SkillGroupResponse,
} from '@/lib/api/skills';

const CATEGORY_BADGES: Record<string, string> = {
    Backend: 'bg-sky-50 text-sky-700 border-sky-200',
    Frontend: 'bg-violet-50 text-violet-700 border-violet-200',
    DevOps: 'bg-orange-50 text-orange-700 border-orange-200',
    'DevOps & Cloud': 'bg-orange-50 text-orange-700 border-orange-200',
    Database: 'bg-teal-50 text-teal-700 border-teal-200',
    Mobile: 'bg-pink-50 text-pink-700 border-pink-200',
    'Testing & QA': 'bg-amber-50 text-amber-700 border-amber-200',
    'UI/UX Design': 'bg-fuchsia-50 text-fuchsia-700 border-fuchsia-200',
    'Chung / Khác': 'bg-slate-100 text-slate-700 border-slate-200',
    General: 'bg-slate-100 text-slate-700 border-slate-200',
    Khác: 'bg-slate-100 text-slate-700 border-slate-200',
};

function RoundedModalSelect({
    value,
    options,
    onChange,
}: {
    value: string;
    options: string[];
    onChange: (val: string) => void;
}) {
    const [open, setOpen] = useState(false);
    const ref = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (ref.current && !ref.current.contains(e.target as Node)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div ref={ref} className="relative w-full">
            <button
                type="button"
                onClick={() => setOpen(!open)}
                className="flex w-full items-center justify-between gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-medium text-slate-800 outline-none hover:border-slate-300 focus:border-indigo-500 focus:bg-white transition"
            >
                <span>{value}</span>
                <ChevronDown className={`h-4 w-4 text-slate-400 shrink-0 transition-transform ${open ? 'rotate-180 text-indigo-600' : ''}`} />
            </button>
            {open && (
                <div className="absolute left-0 top-full z-50 mt-1 max-h-56 w-full overflow-y-auto rounded-xl border border-slate-200 bg-white p-1.5 shadow-xl ring-1 ring-slate-900/5 animate-in fade-in-50 zoom-in-95">
                    {options.map((opt) => {
                        const isSelected = opt === value;
                        return (
                            <button
                                key={opt}
                                type="button"
                                onClick={() => {
                                    onChange(opt);
                                    setOpen(false);
                                }}
                                className={`flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2 text-xs font-medium transition-colors ${
                                    isSelected
                                        ? 'bg-indigo-50 text-indigo-700 font-semibold'
                                        : 'text-slate-700 hover:bg-slate-100 hover:text-slate-900'
                                }`}
                            >
                                <span>{opt}</span>
                                {isSelected && <Check className="h-3.5 w-3.5 text-indigo-600 shrink-0" />}
                            </button>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

interface SkillCatalogViewProps {
    catalog?: CatalogSkill[];
    onUpdateCatalog?: (newCatalog: CatalogSkill[]) => void;
}

export default function SkillCatalogView({ catalog: externalCatalog, onUpdateCatalog }: SkillCatalogViewProps) {
    const currentUser = useAuthUser();
    const roleCode = currentUser?.roleCode?.toUpperCase().replace(/_/g, '-') || '';
    const canManageCatalog = roleCode === 'VT-06';

    const [localCatalog, setLocalCatalog] = useState<CatalogSkill[]>(externalCatalog || SKILL_CATALOG);
    const catalog = externalCatalog || localCatalog;

    const [groups, setGroups] = useState<SkillGroupResponse[]>([]);
    const [search, setSearch] = useState('');
    const [selectedCategory, setSelectedCategory] = useState('Tất cả nhóm');
    const [modalOpen, setModalOpen] = useState(false);
    const [editingSkill, setEditingSkill] = useState<CatalogSkill | null>(null);
    const [skillName, setSkillName] = useState('');
    const [skillCategory, setSkillCategory] = useState('Backend');
    const [lockTarget, setLockTarget] = useState<CatalogSkill | null>(null);

    // Skill Group creation state
    const [groupModalOpen, setGroupModalOpen] = useState(false);
    const [newGroupName, setNewGroupName] = useState('');
    const [newGroupDesc, setNewGroupDesc] = useState('');
    const [groupError, setGroupError] = useState('');
    const [isSubmittingGroup, setIsSubmittingGroup] = useState(false);
    const [groupSuccessMessage, setGroupSuccessMessage] = useState('');

    const handleCreateGroup = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmedName = newGroupName.trim();
        if (!trimmedName) {
            setGroupError('Vui lòng nhập tên nhóm kỹ năng');
            return;
        }

        setIsSubmittingGroup(true);
        setGroupError('');

        try {
            const created = await createSkillGroup({
                name: trimmedName,
                description: newGroupDesc.trim() || undefined,
            });

            // Reload groups from backend
            const fetchedGroups = await getSkillGroups();
            if (fetchedGroups && fetchedGroups.length > 0) {
                setGroups(fetchedGroups);
            }

            // Automatically select this newly created group in the skill modal
            setSkillCategory(created.name);

            setGroupSuccessMessage(`Đã tạo thành công nhóm kỹ năng "${created.name}"`);
            setTimeout(() => setGroupSuccessMessage(''), 4000);

            setGroupModalOpen(false);
            setNewGroupName('');
            setNewGroupDesc('');
        } catch (err: any) {
            console.error('Failed to create skill group:', err);
            const msg = err?.message || err?.error || 'Có lỗi xảy ra khi tạo nhóm kỹ năng';
            setGroupError(msg);
        } finally {
            setIsSubmittingGroup(false);
        }
    };

    const updateCatalog = (newList: CatalogSkill[]) => {
        setLocalCatalog(newList);
        onUpdateCatalog?.(newList);
    };

    // Load skills & groups from real backend API
    const loadBackendCatalog = async () => {
        try {
            const [fetchedSkills, fetchedGroups] = await Promise.all([
                getSkills(),
                getSkillGroups().catch(() => []),
            ]);

            if (fetchedGroups && fetchedGroups.length > 0) {
                setGroups(fetchedGroups);
            }

            if (fetchedSkills && fetchedSkills.length > 0) {
                const mapped: CatalogSkill[] = fetchedSkills.map((s) => ({
                    id: s.id,
                    name: s.name,
                    category: s.groupName || 'Khác',
                    groupId: s.groupId,
                    description: s.description,
                    status: s.status,
                    version: s.version,
                }));
                updateCatalog(mapped);
            }
        } catch (err) {
            console.error('Failed to load skill catalog from backend:', err);
        }
    };

    useEffect(() => {
        loadBackendCatalog();
    }, []);

    const categoryOptions = useMemo(() => {
        const set = new Set<string>(['Tất cả nhóm']);
        groups.forEach((g) => set.add(g.name));
        catalog.forEach((c) => set.add(c.category));
        return Array.from(set);
    }, [groups, catalog]);

    const modalCategoryOptions = useMemo(() => {
        const names = groups.map((g) => g.name);
        if (names.length === 0) return ['Backend', 'Frontend', 'DevOps', 'Database', 'Khác'];
        return names;
    }, [groups]);

    const filteredCatalog = useMemo(() => {
        const q = search.trim().toLowerCase();
        return catalog.filter((item) => {
            const matchesSearch = !q || item.name.toLowerCase().includes(q) || item.category.toLowerCase().includes(q);
            const matchesCat = selectedCategory === 'Tất cả nhóm' || item.category === selectedCategory;
            return matchesSearch && matchesCat;
        });
    }, [catalog, search, selectedCategory]);

    const handleOpenCreate = () => {
        setEditingSkill(null);
        setSkillName('');
        setSkillCategory(modalCategoryOptions[0] || 'Backend');
        setModalOpen(true);
    };

    const handleOpenEdit = (item: CatalogSkill) => {
        setEditingSkill(item);
        setSkillName(item.name);
        setSkillCategory(item.category);
        setModalOpen(true);
    };

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!skillName.trim()) return;

        const matchedGroup = groups.find((g) => g.name.toLowerCase() === skillCategory.toLowerCase());
        const groupId = matchedGroup?.id || (groups[0]?.id ?? 1);

        try {
            if (editingSkill) {
                await updateSkill(editingSkill.id, {
                    name: skillName.trim(),
                    description: editingSkill.description || '',
                    groupId,
                    version: editingSkill.version || 0,
                });
            } else {
                await createSkill({
                    name: skillName.trim(),
                    description: '',
                    groupId,
                });
            }
            await loadBackendCatalog();
            setModalOpen(false);
        } catch (err) {
            console.error('Failed to save skill:', err);
            // Fallback update local state
            if (editingSkill) {
                const updated = catalog.map((item) =>
                    item.id === editingSkill.id ? { ...item, name: skillName.trim(), category: skillCategory } : item
                );
                updateCatalog(updated);
            } else {
                const newSkill: CatalogSkill = {
                    id: Date.now(),
                    name: skillName.trim(),
                    category: skillCategory,
                };
                updateCatalog([...catalog, newSkill]);
            }
            setModalOpen(false);
        }
    };

    const handleConfirmToggleLock = async () => {
        if (!lockTarget) return;
        try {
            await deactivateSkill(lockTarget.id);
            await loadBackendCatalog();
        } catch (err) {
            console.error('Failed to toggle skill lock status:', err);
            const updated = catalog.map((item) =>
                item.id === lockTarget.id
                    ? { ...item, status: item.status === 'INACTIVE' ? ('ACTIVE' as const) : ('INACTIVE' as const) }
                    : item
            );
            updateCatalog(updated);
        }
        setLockTarget(null);
    };

    return (
        <div className="space-y-5">
            {/* ── Header bar ── */}
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                    <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                        <BookOpen className="h-5 w-5 text-indigo-600" />
                        Quản lý Danh mục Kỹ năng Hệ thống
                    </h2>
                    <p className="mt-0.5 text-xs text-slate-500">
                        Danh sách các kỹ năng chuẩn hóa dùng để nhân viên khai báo và lập ma trận năng lực bộ phận.
                    </p>
                </div>

                {canManageCatalog && (
                    <div className="flex flex-wrap items-center gap-2 self-start sm:self-auto">
                        <button
                            type="button"
                            onClick={() => {
                                setNewGroupName('');
                                setNewGroupDesc('');
                                setGroupError('');
                                setGroupModalOpen(true);
                            }}
                            className="inline-flex items-center gap-1.5 rounded-full border border-indigo-200 bg-indigo-50 hover:bg-indigo-100 px-4 py-2.5 text-xs font-bold text-indigo-700 shadow-2xs transition active:scale-95 cursor-pointer"
                        >
                            <FolderPlus className="h-4 w-4 text-indigo-600" />
                            Tạo nhóm kỹ năng
                        </button>
                        <button
                            type="button"
                            onClick={handleOpenCreate}
                            className="inline-flex items-center gap-1.5 rounded-full bg-indigo-600 hover:bg-indigo-700 px-5 py-2.5 text-xs font-bold text-white shadow-md shadow-indigo-200 transition active:scale-95 cursor-pointer"
                        >
                            <Plus className="h-4 w-4" />
                            Thêm kỹ năng mới
                        </button>
                    </div>
                )}
            </div>

            {/* ── Success notification banner ── */}
            {groupSuccessMessage && (
                <div className="flex items-center justify-between gap-2 rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-xs font-medium text-emerald-800 animate-in fade-in">
                    <div className="flex items-center gap-2">
                        <Check className="h-4 w-4 text-emerald-600 shrink-0" />
                        <span>{groupSuccessMessage}</span>
                    </div>
                    <button type="button" onClick={() => setGroupSuccessMessage('')} className="text-emerald-600 hover:text-emerald-800 cursor-pointer">
                        <X className="h-3.5 w-3.5" />
                    </button>
                </div>
            )}

            {/* ── Filter / Search bar ── */}
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex flex-wrap items-center gap-3">
                    <div className="relative w-64">
                        <input
                            type="text"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            placeholder="Tìm kiếm kỹ năng..."
                            className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-medium text-slate-800 placeholder-slate-400 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 transition shadow-2xs"
                        />
                        {search && (
                            <button
                                type="button"
                                onClick={() => setSearch('')}
                                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                            >
                                <X className="h-3.5 w-3.5" />
                            </button>
                        )}
                    </div>

                    <div className="w-48">
                        <RoundedModalSelect
                            value={selectedCategory}
                            options={categoryOptions}
                            onChange={setSelectedCategory}
                        />
                    </div>
                </div>

                <span className="text-xs text-slate-500 font-medium">
                    Tổng số: <strong className="text-indigo-600">{filteredCatalog.length}</strong> kỹ năng
                </span>
            </div>

            {/* ── Catalog Table ── */}
            <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-2xs">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="border-b border-slate-100 bg-slate-50 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
                                <th className="px-4 py-3 text-left">Mã SK</th>
                                <th className="px-4 py-3 text-left">Tên kỹ năng</th>
                                <th className="px-4 py-3 text-left">Nhóm kỹ năng</th>
                                <th className="px-4 py-3 text-left">Thống kê nhân sự</th>
                                <th className="px-4 py-3 text-left">Trạng thái</th>
                                {canManageCatalog && (
                                    <th className="px-4 py-3 text-right">Thao tác</th>
                                )}
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {filteredCatalog.length === 0 ? (
                                <tr>
                                    <td colSpan={canManageCatalog ? 6 : 5} className="py-12 text-center text-xs text-slate-400">
                                        Không tìm thấy kỹ năng nào trong danh mục.
                                    </td>
                                </tr>
                            ) : (
                                filteredCatalog.map((item) => {
                                    const badgeClass = CATEGORY_BADGES[item.category] || CATEGORY_BADGES['Khác'];
                                    const isLocked = item.status === 'INACTIVE';
                                    return (
                                        <tr key={item.id} className="transition-colors hover:bg-slate-50/60">
                                            <td className="px-4 py-3 font-mono text-xs text-slate-400 font-medium">
                                                SK-{String(item.id).padStart(3, '0')}
                                            </td>
                                            <td className="px-4 py-3 font-semibold text-slate-800">
                                                {item.name}
                                            </td>
                                            <td className="px-4 py-3">
                                                <span className={`inline-flex items-center rounded-md border px-2.5 py-0.5 text-[11px] font-semibold ${badgeClass}`}>
                                                    {item.category}
                                                </span>
                                            </td>
                                            <td className="px-4 py-3">
                                                <span className="text-xs text-slate-400">Xem tại Ma trận kỹ năng</span>
                                            </td>
                                            <td className="px-4 py-3">
                                                {isLocked ? (
                                                    <span className="inline-flex items-center gap-1.5 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-0.5 text-[11px] font-semibold text-rose-700 shadow-2xs">
                                                        <Lock className="h-3.5 w-3.5 text-rose-600 shrink-0" />
                                                        <span>Đã khóa</span>
                                                    </span>
                                                ) : (
                                                    <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-[11px] font-semibold text-emerald-700 shadow-2xs">
                                                        <Unlock className="h-3.5 w-3.5 text-emerald-600 shrink-0" />
                                                        <span>Đang mở</span>
                                                    </span>
                                                )}
                                            </td>
                                            {canManageCatalog && (
                                                <td className="px-4 py-3 text-right">
                                                    <div className="flex items-center justify-end gap-1.5">
                                                        <button
                                                            type="button"
                                                            onClick={() => handleOpenEdit(item)}
                                                            className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-600 transition hover:bg-indigo-50 hover:border-indigo-200 hover:text-indigo-600 cursor-pointer shadow-2xs"
                                                            title="Chỉnh sửa kỹ năng"
                                                        >
                                                            <Pencil className="h-3.5 w-3.5 text-slate-500" />
                                                            <span>Sửa</span>
                                                        </button>
                                                        {isLocked ? (
                                                            <button
                                                                type="button"
                                                                onClick={() => setLockTarget(item)}
                                                                className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700 transition hover:bg-emerald-100 hover:border-emerald-300 cursor-pointer shadow-2xs"
                                                                title="Mở khóa kỹ năng (Cho phép nhân viên khai báo)"
                                                            >
                                                                <Unlock className="h-3.5 w-3.5 text-emerald-600" />
                                                                <span>Mở khóa</span>
                                                            </button>
                                                        ) : (
                                                            <button
                                                                type="button"
                                                                onClick={() => setLockTarget(item)}
                                                                className="inline-flex items-center gap-1 rounded-lg border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700 transition hover:bg-amber-100 hover:border-amber-300 cursor-pointer shadow-2xs"
                                                                title="Khóa kỹ năng (Ngăn nhân viên khai báo)"
                                                            >
                                                                <Lock className="h-3.5 w-3.5 text-amber-600" />
                                                                <span>Khóa</span>
                                                            </button>
                                                        )}
                                                    </div>
                                                </td>
                                            )}
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* ── Modal Thêm/Sửa Kỹ năng (Chỉ hiển thị cho VT-06) ── */}
            {modalOpen && canManageCatalog && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-xs animate-in fade-in">
                    <div className="relative w-full max-w-md rounded-2xl border border-slate-100 bg-white p-6 shadow-2xl">
                        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                            <h3 className="text-base font-bold text-slate-900">
                                {editingSkill ? 'Chỉnh sửa Kỹ năng' : 'Thêm Kỹ năng mới vào Danh mục'}
                            </h3>
                            <button
                                type="button"
                                onClick={() => setModalOpen(false)}
                                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 cursor-pointer"
                            >
                                <X className="h-4 w-4" />
                            </button>
                        </div>

                        <form onSubmit={handleSave} className="mt-4 space-y-4">
                            <div>
                                <label className="mb-1 block text-xs font-semibold text-slate-600">Tên kỹ năng</label>
                                <input
                                    type="text"
                                    required
                                    value={skillName}
                                    onChange={(e) => setSkillName(e.target.value)}
                                    placeholder="VD: Spring Boot, Vue.js, Docker..."
                                    className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                                />
                            </div>

                            <div>
                                <div className="flex items-center justify-between mb-1">
                                    <label className="text-xs font-semibold text-slate-600">Nhóm kỹ năng</label>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setNewGroupName('');
                                            setNewGroupDesc('');
                                            setGroupError('');
                                            setGroupModalOpen(true);
                                        }}
                                        className="text-[11px] font-semibold text-indigo-600 hover:text-indigo-800 hover:underline cursor-pointer flex items-center gap-0.5"
                                    >
                                        <Plus className="h-3 w-3" />
                                        Tạo nhóm mới
                                    </button>
                                </div>
                                <RoundedModalSelect
                                    value={skillCategory}
                                    options={modalCategoryOptions}
                                    onChange={setSkillCategory}
                                />
                            </div>

                            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                                <button
                                    type="button"
                                    onClick={() => setModalOpen(false)}
                                    className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 cursor-pointer"
                                >
                                    Hủy
                                </button>
                                <button
                                    type="submit"
                                    className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 shadow-xs cursor-pointer"
                                >
                                    Lưu kỹ năng
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* ── Modal Tạo nhóm kỹ năng mới (Chỉ hiển thị cho VT-06) ── */}
            {groupModalOpen && canManageCatalog && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-xs animate-in fade-in">
                    <div className="relative w-full max-w-md rounded-2xl border border-slate-100 bg-white p-6 shadow-2xl">
                        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                            <div className="flex items-center gap-2">
                                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
                                    <FolderPlus className="h-4 w-4" />
                                </div>
                                <div>
                                    <h3 className="text-base font-bold text-slate-900">
                                        Tạo nhóm kỹ năng mới
                                    </h3>
                                    <p className="text-[11px] text-slate-400">Phân loại kỹ năng vào các lĩnh vực chuyên môn cụ thể</p>
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={() => setGroupModalOpen(false)}
                                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 cursor-pointer"
                            >
                                <X className="h-4 w-4" />
                            </button>
                        </div>

                        <form onSubmit={handleCreateGroup} className="mt-4 space-y-4">
                            {groupError && (
                                <div className="flex items-center gap-2 rounded-xl bg-rose-50 p-3 text-xs font-medium text-rose-700 border border-rose-200">
                                    <AlertCircle className="h-4 w-4 shrink-0" />
                                    <span>{groupError}</span>
                                </div>
                            )}

                            <div>
                                <label className="mb-1 block text-xs font-semibold text-slate-600">
                                    Tên nhóm kỹ năng <span className="text-rose-500">*</span>
                                </label>
                                <input
                                    type="text"
                                    required
                                    maxLength={100}
                                    value={newGroupName}
                                    onChange={(e) => setNewGroupName(e.target.value)}
                                    placeholder="VD: Trí tuệ nhân tạo (AI/ML), An ninh mạng, Cloud Native..."
                                    className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                                />
                            </div>

                            <div>
                                <label className="mb-1 block text-xs font-semibold text-slate-600">
                                    Mô tả nhóm kỹ năng
                                </label>
                                <textarea
                                    rows={3}
                                    maxLength={1000}
                                    value={newGroupDesc}
                                    onChange={(e) => setNewGroupDesc(e.target.value)}
                                    placeholder="Mô tả phạm vi hoặc các công nghệ điển hình thuộc nhóm..."
                                    className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-medium text-slate-800 outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100 resize-none"
                                />
                            </div>

                            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                                <button
                                    type="button"
                                    onClick={() => setGroupModalOpen(false)}
                                    className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 cursor-pointer"
                                >
                                    Hủy
                                </button>
                                <button
                                    type="submit"
                                    disabled={isSubmittingGroup || !newGroupName.trim()}
                                    className="inline-flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 shadow-xs cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {isSubmittingGroup ? 'Đang tạo...' : 'Tạo nhóm kỹ năng'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* ── Modal Xác nhận Khóa / Mở khóa kỹ năng ── */}
            {lockTarget && canManageCatalog && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-xs">
                    <div className="w-full max-w-sm rounded-2xl border border-slate-100 bg-white p-5 shadow-2xl text-center">
                        <div className={`mx-auto flex h-10 w-10 items-center justify-center rounded-full mb-3 ${
                            lockTarget.status === 'INACTIVE' ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'
                        }`}>
                            {lockTarget.status === 'INACTIVE' ? <Unlock className="h-5 w-5" /> : <Lock className="h-5 w-5" />}
                        </div>
                        <h4 className="text-sm font-bold text-slate-900">
                            {lockTarget.status === 'INACTIVE' ? 'Xác nhận mở khóa kỹ năng' : 'Xác nhận khóa kỹ năng'}
                        </h4>
                        <p className="mt-1 text-xs text-slate-500">
                            {lockTarget.status === 'INACTIVE' ? (
                                <>Bạn có chắc chắn muốn mở khóa kỹ năng "<strong>{lockTarget.name}</strong>"? Sau khi mở khóa, nhân viên có thể tiếp tục khai báo kỹ năng này.</>
                            ) : (
                                <>Bạn có chắc chắn muốn khóa kỹ năng "<strong>{lockTarget.name}</strong>"? Khi bị khóa, nhân viên sẽ không thể khai báo kỹ năng này nữa.</>
                            )}
                        </p>
                        <div className="mt-4 flex gap-2">
                            <button
                                type="button"
                                onClick={() => setLockTarget(null)}
                                className="flex-1 rounded-xl border border-slate-200 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50 cursor-pointer"
                            >
                                Hủy
                            </button>
                            <button
                                type="button"
                                onClick={handleConfirmToggleLock}
                                className={`flex-1 rounded-xl py-2 text-xs font-semibold text-white shadow-xs cursor-pointer ${
                                    lockTarget.status === 'INACTIVE'
                                        ? 'bg-emerald-600 hover:bg-emerald-700'
                                        : 'bg-amber-600 hover:bg-amber-700'
                                }`}
                            >
                                {lockTarget.status === 'INACTIVE' ? 'Mở khóa' : 'Khóa kỹ năng'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
