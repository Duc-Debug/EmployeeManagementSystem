import { useState, useMemo, useRef, useEffect } from 'react';
import { Search, Plus, Pencil, Trash2, BookOpen, Check, X, ShieldAlert, ChevronDown } from 'lucide-react';
import type { CatalogSkill } from './Types.ts';
import { SKILL_CATALOG, MATRIX_EMPLOYEES } from './Types.ts';

const CATEGORY_OPTIONS = ['Tất cả nhóm', 'Backend', 'Frontend', 'DevOps', 'Database', 'Khác'];

const CATEGORY_BADGES: Record<string, string> = {
    Backend: 'bg-sky-50 text-sky-700 border-sky-200',
    Frontend: 'bg-violet-50 text-violet-700 border-violet-200',
    DevOps: 'bg-orange-50 text-orange-700 border-orange-200',
    Database: 'bg-teal-50 text-teal-700 border-teal-200',
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

function getSkillProficiencyStats(item: CatalogSkill) {
    const totalEmps = MATRIX_EMPLOYEES.length || 6;
    
    // Đếm số lượng nhân sự đạt mức độ L3+ (Thành thạo trở lên) cho kỹ năng này
    const proficientEmps = MATRIX_EMPLOYEES.filter((emp) => {
        const level = emp.skills[item.name];
        return level != null && level >= 3;
    });

    if (proficientEmps.length > 0) {
        const count = proficientEmps.length;
        const pct = Math.round((count / totalEmps) * 100);
        return { count, totalEmps, pct };
    }

    // Mock dữ liệu hợp lý cho các kỹ năng khác trong danh mục
    const fallbackPctMap: Record<string, number> = {
        'PostgreSQL': 33,
        'AWS': 50,
        'Vue.js': 17,
    };
    
    if (fallbackPctMap[item.name] !== undefined) {
        const pct = fallbackPctMap[item.name];
        const count = Math.round((pct / 100) * totalEmps);
        return { count, totalEmps, pct };
    }

    const count = ((item.id % 4) + 1);
    const pct = Math.round((count / totalEmps) * 100);
    return { count, totalEmps, pct };
}

interface SkillCatalogViewProps {
    catalog?: CatalogSkill[];
    onUpdateCatalog?: (newCatalog: CatalogSkill[]) => void;
}

export default function SkillCatalogView({ catalog: externalCatalog, onUpdateCatalog }: SkillCatalogViewProps) {
    const [localCatalog, setLocalCatalog] = useState<CatalogSkill[]>(externalCatalog || SKILL_CATALOG);
    const catalog = externalCatalog || localCatalog;

    const updateCatalog = (newList: CatalogSkill[]) => {
        setLocalCatalog(newList);
        onUpdateCatalog?.(newList);
    };

    const [search, setSearch] = useState('');
    const [selectedCategory, setSelectedCategory] = useState('Tất cả nhóm');
    const [modalOpen, setModalOpen] = useState(false);
    const [editingSkill, setEditingSkill] = useState<CatalogSkill | null>(null);
    const [skillName, setSkillName] = useState('');
    const [skillCategory, setSkillCategory] = useState('Backend');
    const [deleteTarget, setDeleteTarget] = useState<CatalogSkill | null>(null);

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
        setSkillCategory('Backend');
        setModalOpen(true);
    };

    const handleOpenEdit = (item: CatalogSkill) => {
        setEditingSkill(item);
        setSkillName(item.name);
        setSkillCategory(item.category);
        setModalOpen(true);
    };

    const handleSave = (e: React.FormEvent) => {
        e.preventDefault();
        if (!skillName.trim()) return;

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
    };

    const handleConfirmDelete = () => {
        if (!deleteTarget) return;
        const updated = catalog.filter((item) => item.id !== deleteTarget.id);
        updateCatalog(updated);
        setDeleteTarget(null);
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

                <button
                    type="button"
                    onClick={handleOpenCreate}
                    className="inline-flex items-center gap-1.5 self-start sm:self-auto rounded-full bg-indigo-600 hover:bg-indigo-700 px-5 py-2.5 text-xs font-bold text-white shadow-md shadow-indigo-200 transition active:scale-95 cursor-pointer"
                >
                    <Plus className="h-4 w-4 stroke-[2.5]" />
                    Thêm kỹ năng mới
                </button>
            </div>

            {/* ── Filter bar ── */}
            <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-slate-50 p-3">
                <div className="flex flex-wrap items-center gap-3">
                    {/* Search */}
                    <div className="relative flex items-center">
                        <Search className="pointer-events-none absolute left-3 h-3.5 w-3.5 text-slate-400" />
                        <input
                            type="text"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            placeholder="Tìm tên kỹ năng..."
                            className="w-56 rounded-lg border border-slate-200 bg-white py-1.5 pl-8 pr-3 text-xs text-slate-800 outline-none focus:border-indigo-400 focus:ring-1 focus:ring-indigo-100"
                        />
                    </div>

                    {/* Filter Category */}
                    <div className="flex items-center gap-1">
                        {CATEGORY_OPTIONS.map((cat) => (
                            <button
                                key={cat}
                                type="button"
                                onClick={() => setSelectedCategory(cat)}
                                className={`rounded-lg px-2.5 py-1 text-xs font-semibold transition ${
                                    selectedCategory === cat
                                        ? 'bg-indigo-600 text-white shadow-2xs'
                                        : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
                                }`}
                            >
                                {cat}
                            </button>
                        ))}
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
                                <th className="px-4 py-3 text-left">Tỉ lệ thành thạo (%)</th>
                                <th className="px-4 py-3 text-left">Trạng thái</th>
                                <th className="px-4 py-3 text-right">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {filteredCatalog.length === 0 ? (
                                <tr>
                                    <td colSpan={6} className="py-12 text-center text-xs text-slate-400">
                                        Không tìm thấy kỹ năng nào trong danh mục.
                                    </td>
                                </tr>
                            ) : (
                                filteredCatalog.map((item, index) => {
                                    const badgeClass = CATEGORY_BADGES[item.category] || CATEGORY_BADGES['Khác'];
                                    const stats = getSkillProficiencyStats(item);
                                    return (
                                        <tr key={item.id} className="transition-colors hover:bg-slate-50/60">
                                            <td className="px-4 py-3 font-mono text-xs text-slate-400 font-medium">
                                                SK-0{index + 1}
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
                                                <div className="flex flex-col gap-1 max-w-[140px]">
                                                    <div className="flex items-center justify-between text-xs">
                                                        <span className="font-bold text-slate-800">{stats.pct}%</span>
                                                        <span className="text-[11px] font-medium text-slate-400">({stats.count}/{stats.totalEmps} NV)</span>
                                                    </div>
                                                    <div className="h-1.5 w-full rounded-full bg-slate-100 overflow-hidden">
                                                        <div
                                                            className={`h-full rounded-full transition-all duration-300 ${
                                                                stats.pct >= 60
                                                                    ? 'bg-emerald-500'
                                                                    : stats.pct >= 40
                                                                    ? 'bg-indigo-500'
                                                                    : stats.pct >= 20
                                                                    ? 'bg-amber-500'
                                                                    : 'bg-rose-400'
                                                            }`}
                                                            style={{ width: `${stats.pct}%` }}
                                                        />
                                                    </div>
                                                </div>
                                            </td>
                                            <td className="px-4 py-3">
                                                <span className="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-[11px] font-semibold text-emerald-700">
                                                    <Check className="h-3 w-3" /> Hoạt động
                                                </span>
                                            </td>
                                            <td className="px-4 py-3 text-right">
                                                <div className="flex items-center justify-end gap-1.5">
                                                    <button
                                                        type="button"
                                                        onClick={() => handleOpenEdit(item)}
                                                        className="rounded-lg p-1.5 text-slate-400 transition hover:bg-indigo-50 hover:text-indigo-600"
                                                        title="Chỉnh sửa"
                                                    >
                                                        <Pencil className="h-3.5 w-3.5" />
                                                    </button>
                                                    <button
                                                        type="button"
                                                        onClick={() => setDeleteTarget(item)}
                                                        className="rounded-lg p-1.5 text-slate-400 transition hover:bg-rose-50 hover:text-rose-600"
                                                        title="Xóa"
                                                    >
                                                        <Trash2 className="h-3.5 w-3.5" />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* ── Modal Thêm/Sửa Kỹ năng ── */}
            {modalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-xs animate-in fade-in">
                    <div className="relative w-full max-w-md rounded-2xl border border-slate-100 bg-white p-6 shadow-2xl">
                        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                            <h3 className="text-base font-bold text-slate-900">
                                {editingSkill ? 'Chỉnh sửa Kỹ năng' : 'Thêm Kỹ năng mới vào Danh mục'}
                            </h3>
                            <button
                                type="button"
                                onClick={() => setModalOpen(false)}
                                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100"
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
                                <label className="mb-1 block text-xs font-semibold text-slate-600">Nhóm kỹ năng</label>
                                <RoundedModalSelect
                                    value={skillCategory}
                                    options={['Backend', 'Frontend', 'DevOps', 'Database', 'Khác']}
                                    onChange={setSkillCategory}
                                />
                            </div>

                            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                                <button
                                    type="button"
                                    onClick={() => setModalOpen(false)}
                                    className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50"
                                >
                                    Hủy
                                </button>
                                <button
                                    type="submit"
                                    className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 shadow-xs"
                                >
                                    Lưu kỹ năng
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* ── Modal Xác nhận Xóa ── */}
            {deleteTarget && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-xs">
                    <div className="w-full max-w-sm rounded-2xl border border-slate-100 bg-white p-5 shadow-2xl text-center">
                        <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-full bg-rose-50 text-rose-600 mb-3">
                            <ShieldAlert className="h-5 w-5" />
                        </div>
                        <h4 className="text-sm font-bold text-slate-900">Xác nhận xóa kỹ năng</h4>
                        <p className="mt-1 text-xs text-slate-500">
                            Bạn có chắc chắn muốn xóa "<strong>{deleteTarget.name}</strong>" khỏi danh mục hệ thống?
                        </p>
                        <div className="mt-4 flex gap-2">
                            <button
                                type="button"
                                onClick={() => setDeleteTarget(null)}
                                className="flex-1 rounded-xl border border-slate-200 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50"
                            >
                                Hủy
                            </button>
                            <button
                                type="button"
                                onClick={handleConfirmDelete}
                                className="flex-1 rounded-xl bg-rose-600 py-2 text-xs font-semibold text-white hover:bg-rose-700 shadow-xs"
                            >
                                Xóa ngay
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

