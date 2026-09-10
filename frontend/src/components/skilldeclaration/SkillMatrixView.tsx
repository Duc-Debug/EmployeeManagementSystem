import { useMemo, useState, useRef, useEffect } from 'react';
import { Search, ChevronDown, Check, BookOpen, AlertTriangle, Users, Award, ShieldAlert } from 'lucide-react';
import type { DepartmentItem } from './SkillresourceSearch.tsx';
import { getDepartmentSkillMatrix, type DepartmentSkillMatrixResponse } from '@/lib/api/skills';
import { getOrgTree } from '@/lib/api/org-units';
import type { OrgUnitTreeNode } from '@/types/hrm';

export interface SkillMatrixViewProps {
    departments?: DepartmentItem[];
    onOpenCatalog?: () => void;
}

/* ── Hằng số màu theo level ─────────────────────────────── */
const LEVEL_STYLE: Record<number, { badge: string; text: string }> = {
    1: { badge: 'bg-slate-100 border border-slate-200', text: 'text-slate-700' },
    2: { badge: 'bg-blue-50 border border-blue-200',   text: 'text-blue-700' },
    3: { badge: 'bg-emerald-50 border border-emerald-200', text: 'text-emerald-700' },
    4: { badge: 'bg-amber-50 border border-amber-200',  text: 'text-amber-700' },
    5: { badge: 'bg-rose-50 border border-rose-200',   text: 'text-rose-700' },
};
const LEVEL_LABELS = ['', 'Sơ cấp', 'Trung cấp', 'Khá', 'Giỏi', 'Chuyên gia'];
const LEVEL_LEGEND_COLORS = [
    'bg-slate-100 text-slate-700 border border-slate-200',
    'bg-blue-50 text-blue-700 border border-blue-200',
    'bg-emerald-50 text-emerald-700 border border-emerald-200',
    'bg-amber-50 text-amber-700 border border-amber-200',
    'bg-rose-50 text-rose-700 border border-rose-200',
];

const CAT_BADGE: Record<string, string> = {
    Backend:  'bg-sky-50 text-sky-700 border border-sky-100',
    Frontend: 'bg-violet-50 text-violet-700 border border-violet-100',
    DevOps:   'bg-orange-50 text-orange-700 border border-orange-100',
    Database: 'bg-teal-50 text-teal-700 border border-teal-100',
};

/* ── RoundedSelect Component (Bo góc hoàn toàn cả menu popup) ── */
function RoundedSelect({
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
        <div ref={ref} className="relative inline-block">
            <button
                type="button"
                onClick={() => setOpen(!open)}
                className="flex items-center justify-between gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-2xs transition hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 cursor-pointer"
            >
                <span className="truncate">{value || 'Chọn...'}</span>
                <ChevronDown className={`h-3.5 w-3.5 text-slate-400 shrink-0 transition-transform ${open ? 'rotate-180 text-indigo-600' : ''}`} />
            </button>
            {open && (
                <div className="absolute left-0 top-full z-50 mt-1 max-h-60 min-w-full overflow-y-auto whitespace-nowrap rounded-xl border border-slate-200 bg-white p-1.5 shadow-xl ring-1 ring-slate-900/5 animate-in fade-in-50 zoom-in-95 [scrollbar-width:thin]">
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
                                className={`flex w-full items-center justify-between gap-3 rounded-lg px-3 py-2 text-xs font-medium transition-colors cursor-pointer ${
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

/* ── MatrixCell ─────────────────────────────────────────── */
function MatrixCell({ level, notes }: { level?: number | null; notes?: string }) {
    if (level == null || level <= 0) {
        return (
            <td className="border-b border-r border-slate-100 px-4 py-3 text-center">
                <span className="text-sm font-medium text-slate-300">–</span>
            </td>
        );
    }
    const s = LEVEL_STYLE[level] ?? LEVEL_STYLE[1];
    return (
        <td className="border-b border-r border-slate-100 px-4 py-3 text-center">
            <span
                className={`inline-flex items-center justify-center rounded-lg px-2.5 py-0.5 text-xs font-bold ${s.badge} ${s.text}`}
                title={notes ? `${LEVEL_LABELS[level]} - ${notes}` : LEVEL_LABELS[level]}
            >
                L{level}
            </span>
        </td>
    );
}

function flattenOrgTree(nodes: readonly OrgUnitTreeNode[]): { id: number; name: string }[] {
    const list: { id: number; name: string }[] = [];
    const traverse = (items: readonly OrgUnitTreeNode[]) => {
        for (const item of items) {
            list.push({ id: item.id, name: item.unitName });
            if (item.children && item.children.length > 0) {
                traverse(item.children);
            }
        }
    };
    traverse(nodes);
    return list;
}

export default function SkillMatrixView({ departments = [], onOpenCatalog }: SkillMatrixViewProps) {
    const [deptList, setDeptList] = useState<{ id: number; name: string }[]>([]);
    const [selectedDeptId, setSelectedDeptId] = useState<number | null>(null);
    const [selectedGroupName, setSelectedGroupName] = useState('Tất cả nhóm kỹ năng');
    const [search, setSearch] = useState('');
    const [loading, setLoading] = useState(false);
    const [matrixData, setMatrixData] = useState<DepartmentSkillMatrixResponse | null>(null);

    // 1. Tải danh sách phòng ban thật từ API hoặc props
    useEffect(() => {
        if (departments && departments.length > 0) {
            const parsed = departments.map((d) => ({
                id: Number(d.id),
                name: d.name,
            }));
            setDeptList(parsed);
            if (parsed.length > 0 && selectedDeptId == null) {
                setSelectedDeptId(parsed[0].id);
            }
            return;
        }

        getOrgTree()
            .then((tree) => {
                const flat = flattenOrgTree(tree);
                setDeptList(flat);
                if (flat.length > 0 && selectedDeptId == null) {
                    setSelectedDeptId(flat[0].id);
                }
            })
            .catch((err) => {
                console.error('Failed to load org tree for skill matrix:', err);
            });
    }, [departments]);

    // 2. Tải ma trận kỹ năng khi phòng ban được chọn thay đổi
    useEffect(() => {
        if (!selectedDeptId) return;

        setLoading(true);
        getDepartmentSkillMatrix(selectedDeptId)
            .then((res) => {
                setMatrixData(res);
            })
            .catch((err) => {
                console.error('Failed to load department skill matrix:', err);
                setMatrixData(null);
            })
            .finally(() => {
                setLoading(false);
            });
    }, [selectedDeptId]);

    const deptOptions = useMemo(() => deptList.map((d) => d.name), [deptList]);
    const currentDeptName = useMemo(() => {
        const d = deptList.find((item) => item.id === selectedDeptId);
        return d ? d.name : deptOptions[0] || '';
    }, [deptList, selectedDeptId, deptOptions]);

    const handleDeptChange = (name: string) => {
        const found = deptList.find((d) => d.name === name);
        if (found) {
            setSelectedDeptId(found.id);
        }
    };

    // Danh sách nhóm kỹ năng có trong ma trận
    const groupOptions = useMemo(() => {
        const set = new Set<string>(['Tất cả nhóm kỹ năng']);
        if (matrixData?.skills) {
            matrixData.skills.forEach((s) => {
                if (s.category) set.add(s.category);
            });
        }
        return Array.from(set);
    }, [matrixData]);

    // Lọc cột kỹ năng theo nhóm
    const visibleSkills = useMemo(() => {
        if (!matrixData?.skills) return [];
        if (selectedGroupName === 'Tất cả nhóm kỹ năng') return matrixData.skills;
        return matrixData.skills.filter((s) => s.category === selectedGroupName);
    }, [matrixData, selectedGroupName]);

    // Lọc hàng nhân sự theo từ khóa tìm kiếm
    const visibleRows = useMemo(() => {
        if (!matrixData?.rows) return [];
        const q = search.trim().toLowerCase();
        if (!q) return matrixData.rows;
        return matrixData.rows.filter(
            (r) =>
                r.fullName.toLowerCase().includes(q) ||
                r.employeeCode.toLowerCase().includes(q) ||
                (r.professionalRole && r.professionalRole.toLowerCase().includes(q))
        );
    }, [matrixData, search]);

    return (
        <div className="space-y-5">
            {/* ── Header / Filter bar ── */}
            <div className="flex flex-wrap items-end gap-3">
                {/* Phòng ban */}
                <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Phòng ban</span>
                    <RoundedSelect
                        value={currentDeptName}
                        options={deptOptions}
                        onChange={handleDeptChange}
                    />
                </div>

                {/* Nhóm kỹ năng */}
                <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Nhóm kỹ năng</span>
                    <RoundedSelect
                        value={selectedGroupName}
                        options={groupOptions}
                        onChange={setSelectedGroupName}
                    />
                </div>

                {/* Nút chuyển sang Danh mục kỹ năng */}
                {onOpenCatalog && (
                    <div className="flex flex-col gap-1">
                        <span className="text-[10px] opacity-0 select-none">Action</span>
                        <button
                            type="button"
                            onClick={onOpenCatalog}
                            className="flex items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 px-3.5 py-2 text-xs font-bold text-indigo-700 shadow-2xs transition hover:bg-indigo-100 active:scale-95 cursor-pointer"
                        >
                            <BookOpen className="h-3.5 w-3.5 text-indigo-600" />
                            <span>Quản lý danh mục kỹ năng</span>
                        </button>
                    </div>
                )}

                {/* Tìm nhân sự */}
                <div className="flex flex-col gap-1 ml-auto">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Tìm nhân sự</span>
                    <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-1.5 shadow-2xs focus-within:border-indigo-400 focus-within:ring-2 focus-within:ring-indigo-100">
                        <Search className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                        <input
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            placeholder="Tên hoặc mã NV..."
                            className="w-40 bg-transparent text-xs text-slate-700 placeholder:text-slate-400 outline-none"
                        />
                    </div>
                </div>
            </div>

            {/* ── Level legend ── */}
            <div className="flex flex-wrap items-center gap-2">
                <span className="text-[11px] font-semibold text-slate-400">Chú thích level:</span>
                {[1, 2, 3, 4, 5].map((lv) => (
                    <span
                        key={lv}
                        className={`rounded-full px-2.5 py-0.5 text-[11px] font-bold ${LEVEL_LEGEND_COLORS[lv - 1]}`}
                    >
                        {lv}-{LEVEL_LABELS[lv]}
                    </span>
                ))}
            </div>

            {/* ── Matrix table ── */}
            <div className="rounded-xl border border-slate-200 bg-white shadow-2xs overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="border-b border-slate-100 bg-slate-50">
                                <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-slate-400 whitespace-nowrap">
                                    Nhân viên
                                </th>
                                <th className="px-4 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-slate-400 whitespace-nowrap">
                                    Chuyên môn
                                </th>
                                {visibleSkills.map((col) => (
                                    <th key={col.id} className="px-3 py-2.5 text-center">
                                        <div className="flex flex-col items-center gap-1">
                                            <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-600 whitespace-nowrap">
                                                {col.name}
                                            </span>
                                            <span className={`rounded-md px-1.5 py-0.5 text-[9px] font-semibold ${CAT_BADGE[col.category] ?? 'bg-slate-100 text-slate-600'}`}>
                                                {col.category}
                                            </span>
                                            {col.singlePersonRisk && (
                                                <span className="text-[9px] text-amber-600 font-bold" title="Rủi ro chỉ 1 người thành thạo">
                                                    ⚠ 1 NV
                                                </span>
                                            )}
                                        </div>
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {loading ? (
                                <tr>
                                    <td colSpan={2 + Math.max(visibleSkills.length, 1)} className="py-12 text-center text-sm text-slate-400">
                                        Đang tải ma trận kỹ năng...
                                    </td>
                                </tr>
                            ) : visibleRows.length === 0 ? (
                                <tr>
                                    <td colSpan={2 + Math.max(visibleSkills.length, 1)} className="py-12 text-center text-sm text-slate-400">
                                        Không tìm thấy nhân sự hoặc phòng ban chưa có kỹ năng được xác nhận.
                                    </td>
                                </tr>
                            ) : (
                                visibleRows.map((emp) => (
                                    <tr key={emp.employeeId} className="transition-colors hover:bg-slate-50/60">
                                        {/* Nhân viên */}
                                        <td className="border-r border-slate-100 px-4 py-3 whitespace-nowrap">
                                            <p className="font-semibold text-slate-800">{emp.fullName}</p>
                                            <p className="text-[11px] text-slate-400">{emp.employeeCode}</p>
                                        </td>
                                        {/* Chuyên môn */}
                                        <td className="border-r border-slate-100 px-4 py-3 whitespace-nowrap">
                                            <span className="text-xs font-medium text-slate-600">{emp.professionalRole || 'Chuyên viên'}</span>
                                        </td>
                                        {/* Skill cells */}
                                        {visibleSkills.map((col) => {
                                            const cell = emp.skills ? emp.skills[col.id] || emp.skills[String(col.id)] : undefined;
                                            return (
                                                <MatrixCell
                                                    key={col.id}
                                                    level={cell?.proficiencyLevel}
                                                    notes={cell?.reviewNotes}
                                                />
                                            );
                                        })}
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* ── Footer stats ── */}
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                {/* Tổng nhân lực */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <div className="flex items-center gap-1.5 text-slate-400">
                        <Users className="h-3.5 w-3.5" />
                        <p className="text-[11px] font-semibold uppercase tracking-wider">Tổng nhân sự</p>
                    </div>
                    <p className="mt-1 text-3xl font-black text-slate-800">
                        {matrixData?.summary?.totalEmployees ?? 0} <span className="text-base font-semibold">Nhân sự</span>
                    </p>
                    <p className="mt-0.5 text-[11px] text-emerald-600">Đã kích hoạt trong bộ phận</p>
                </div>

                {/* Tổng số kỹ năng */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <div className="flex items-center gap-1.5 text-slate-400">
                        <Award className="h-3.5 w-3.5" />
                        <p className="text-[11px] font-semibold uppercase tracking-wider">Tổng số kỹ năng</p>
                    </div>
                    <p className="mt-1 text-3xl font-black text-slate-800">
                        {matrixData?.summary?.totalSkills ?? 0} <span className="text-base font-semibold">Kỹ năng</span>
                    </p>
                    <p className="mt-0.5 text-[11px] text-slate-400">Trong danh mục hệ thống</p>
                </div>

                {/* Rủi ro phụ thuộc 1 người */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <div className="flex items-center gap-1.5 text-amber-500">
                        <ShieldAlert className="h-3.5 w-3.5" />
                        <p className="text-[11px] font-semibold uppercase tracking-wider">Rủi ro phụ thuộc 1 người</p>
                    </div>
                    <p className="mt-1 text-3xl font-black text-amber-600">
                        {matrixData?.summary?.singlePersonRiskSkillCount ?? 0} <span className="text-base font-semibold">Kỹ năng</span>
                    </p>
                    <p className="mt-0.5 text-[11px] text-amber-500">⚠ Chỉ có 1 nhân sự phụ trách</p>
                </div>

                {/* Kỹ năng thiếu người */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <div className="flex items-center gap-1.5 text-rose-500">
                        <AlertTriangle className="h-3.5 w-3.5" />
                        <p className="text-[11px] font-semibold uppercase tracking-wider">Kỹ năng thiếu người</p>
                    </div>
                    <p className="mt-1 text-3xl font-black text-rose-600">
                        {matrixData?.summary?.unstaffedSkillCount ?? 0} <span className="text-base font-semibold">Kỹ năng</span>
                    </p>
                    <p className="mt-0.5 text-[11px] text-rose-500">Chưa có nhân sự nào đạt</p>
                </div>
            </div>
        </div>
    );
}
