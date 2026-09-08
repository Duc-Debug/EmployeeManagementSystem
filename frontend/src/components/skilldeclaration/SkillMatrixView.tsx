import { useMemo, useState, useRef, useEffect } from 'react';
import { Search, ChevronDown, Check, BookOpen } from 'lucide-react';
import { MATRIX_EMPLOYEES, MATRIX_SKILL_COLUMNS } from './Types.ts';
import type { MatrixEmployee, MatrixSkillColumn } from './Types.ts';
import type { DepartmentItem } from './SkillresourceSearch.tsx';

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

/* ── Danh sách phòng ban & nhóm kỹ năng (Đồng bộ với Cây phân cấp đơn vị) ── */
const ALL_DEPTS = [
    'Khối Kỹ thuật & Công nghệ',
    'Phòng Lập trình Frontend',
    'Phòng Lập trình Backend',
    'Nhóm UI/UX & Design System',
    'Nhóm Cloud & DevOps',
    'Khối Vận hành & Nhân sự',
    'Phòng Nhân sự & Tuyển dụng',
    'Phòng Hành chính & Quản trị',
    'Khối Kinh doanh & Marketing',
    'Phòng Phát triển Kinh doanh',
    'Phòng Truyền thông & Marketing',
];
const ALL_GROUPS = ['Tất cả nhóm kỹ năng', 'Backend', 'Frontend', 'DevOps', 'Database'];


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
                className="flex items-center justify-between gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-2xs transition hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
            >
                <span className="truncate">{value}</span>
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
                                className={`flex w-full items-center justify-between gap-3 rounded-lg px-3 py-2 text-xs font-medium transition-colors ${
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

/* ── Tính stats footer ───────────────────────────────────── */
function computeStats(employees: MatrixEmployee[], cols: MatrixSkillColumn[]) {
    const total = employees.length;

    const skillTotals = cols.map((col) => ({
        name: col.name,
        total: employees.reduce((sum, e) => sum + (e.skills[col.name] ?? 0), 0),
    }));
    const strongest = skillTotals.sort((a, b) => b.total - a.total)[0];

    const gaps = cols.filter((col) =>
        employees.some((e) => (e.skills[col.name] ?? 0) < 3)
    );
    const gapText = gaps.length > 0 ? gaps.map((g) => g.name).join(' & ') + '...' : 'Không có';

    const totalCells = employees.length * cols.length;
    const readyCells = employees.reduce(
        (sum, e) => sum + cols.filter((col) => (e.skills[col.name] ?? 0) >= 3).length,
        0
    );
    const readyPct = totalCells === 0 ? 0 : Math.round((readyCells / totalCells) * 100);

    return { total, strongest: strongest?.name ?? '—', gapText, readyPct };
}

/* ── MatrixCell ─────────────────────────────────────────── */
function MatrixCell({ level }: { level: number | null | undefined }) {
    if (level == null) {
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
                title={LEVEL_LABELS[level]}
            >
                L{level}
            </span>
        </td>
    );
}

/* ── Main component ─────────────────────────────────────── */
export default function SkillMatrixView({ departments = [], onOpenCatalog }: SkillMatrixViewProps) {
    const [deptOptions, setDeptOptions] = useState<string[]>(() => {
        if (departments && departments.length > 0) {
            return departments.map((d) => d.name);
        }
        try {
            const saved = localStorage.getItem('sys_dept_units');
            if (saved) {
                const parsed = JSON.parse(saved);
                if (Array.isArray(parsed) && parsed.length > 0) return parsed;
            }
        } catch {}
        return ALL_DEPTS;
    });

    const [dept, setDept]     = useState<string>('');
    const [group, setGroup]   = useState(ALL_GROUPS[0]);
    const [search, setSearch] = useState('');

    // Đồng bộ danh sách đơn vị khi có sự kiện Thêm / Sửa / Xóa phòng ban
    useEffect(() => {
        if (departments && departments.length > 0) {
            setDeptOptions(departments.map((d) => d.name));
            return;
        }
        const handleUnitsChange = (e: Event) => {
            const customEvent = e as CustomEvent<string[]>;
            if (Array.isArray(customEvent.detail) && customEvent.detail.length > 0) {
                setDeptOptions(customEvent.detail);
            }
        };
        window.addEventListener('dept_units_changed', handleUnitsChange);
        return () => window.removeEventListener('dept_units_changed', handleUnitsChange);
    }, [departments]);

    // Tự động chọn đơn vị đầu tiên hoặc chọn lại khi đơn vị đang chọn bị xóa
    useEffect(() => {
        if (deptOptions.length > 0 && (!dept || !deptOptions.includes(dept))) {
            setDept(deptOptions[0]);
        }
    }, [deptOptions, dept]);

    const visibleCols: MatrixSkillColumn[] = useMemo(() =>
        group === ALL_GROUPS[0]
            ? MATRIX_SKILL_COLUMNS
            : MATRIX_SKILL_COLUMNS.filter((c) => c.category === group),
        [group]
    );

    const visibleEmps: MatrixEmployee[] = useMemo(() => {
        const q = search.trim().toLowerCase();
        if (!q) return MATRIX_EMPLOYEES;
        return MATRIX_EMPLOYEES.filter(
            (e) =>
                e.name.toLowerCase().includes(q) ||
                e.code.toLowerCase().includes(q) ||
                e.role.toLowerCase().includes(q)
        );
    }, [search]);

    const stats = useMemo(() => computeStats(visibleEmps, visibleCols), [visibleEmps, visibleCols]);

    return (
        <div className="space-y-5">
            {/* ── Header / Filter bar ── */}
            <div className="flex flex-wrap items-end gap-3">
                {/* Phòng ban */}
                <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Phòng ban</span>
                    <RoundedSelect value={dept} options={deptOptions} onChange={setDept} />
                </div>

                {/* Nhóm kỹ năng */}
                <div className="flex flex-col gap-1">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Nhóm kỹ năng</span>
                    <RoundedSelect value={group} options={ALL_GROUPS} onChange={setGroup} />
                </div>

                {/* Nút chuyển sang Danh mục kỹ năng (Ngang hàng) */}
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
                                    Dự án hiện tại
                                </th>
                                {visibleCols.map((col) => (
                                    <th key={col.name} className="px-3 py-2.5 text-center">
                                        <div className="flex flex-col items-center gap-1">
                                            <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-600 whitespace-nowrap">
                                                {col.name}
                                            </span>
                                            <span className={`rounded-md px-1.5 py-0.5 text-[9px] font-semibold ${CAT_BADGE[col.category] ?? 'bg-slate-100 text-slate-600'}`}>
                                                {col.category}
                                            </span>
                                        </div>
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {visibleEmps.length === 0 ? (
                                <tr>
                                    <td colSpan={2 + visibleCols.length} className="py-12 text-center text-sm text-slate-400">
                                        Không tìm thấy nhân sự phù hợp.
                                    </td>
                                </tr>
                            ) : (
                                visibleEmps.map((emp) => (
                                    <tr key={emp.id} className="transition-colors hover:bg-slate-50/60">
                                        {/* Nhân viên */}
                                        <td className="border-r border-slate-100 px-4 py-3 whitespace-nowrap">
                                            <p className="font-semibold text-slate-800">{emp.name}</p>
                                            <p className="text-[11px] text-slate-400">{emp.code} • {emp.role}</p>
                                        </td>
                                        {/* Dự án */}
                                        <td className="border-r border-slate-100 px-4 py-3 whitespace-nowrap">
                                            <span className="text-xs font-medium text-indigo-500">{emp.currentProject}</span>
                                        </td>
                                        {/* Skill cells */}
                                        {visibleCols.map((col) => (
                                            <MatrixCell key={col.name} level={emp.skills[col.name]} />
                                        ))}
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
                    <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Tổng nhân lực bộ phận</p>
                    <p className="mt-1 text-3xl font-black text-slate-800">{stats.total} <span className="text-base font-semibold">Nhân sự</span></p>
                    <p className="mt-0.5 text-[11px] text-emerald-600">100% Khả dụng</p>
                </div>

                {/* Kỹ năng mạnh nhất */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Kỹ năng mạnh nhất</p>
                    <p className="mt-1 text-lg font-black text-slate-800">{stats.strongest}</p>
                    <p className="mt-0.5 text-[11px] text-slate-400">Mức độ phổ: 83% phòng</p>
                </div>

                {/* Skill Gap */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Điểm thiếu hụt kỹ năng</p>
                    <p className="mt-1 text-sm font-bold text-amber-600 leading-snug">{stats.gapText}</p>
                    <p className="mt-0.5 text-[11px] text-amber-500">⚠ Cần đào tạo thêm</p>
                </div>

                {/* Sẵn sàng */}
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-2xs">
                    <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Chỉ số sẵn sàng dự án</p>
                    <p className="mt-1 text-3xl font-black text-slate-800">{stats.readyPct}<span className="text-base font-semibold">%</span></p>
                    <p className="mt-0.5 text-[11px] text-emerald-600">Sẵn sàng gần việc ngay</p>
                </div>
            </div>
        </div>
    );
}

