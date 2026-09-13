import { useMemo, useState, useRef, useEffect } from 'react';
import {
    Building2,
    ChevronDown,
    Check,
    Eye,
    RotateCcw,
    Search,
    SendHorizontal,
    SlidersHorizontal,
    Star,
    Users,
    X,
    Loader2,
    Clock,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import {
    getSkills,
    searchResourcesBySkill,
    type ResourceSearchResultItem,
    type SkillResponse,
} from '@/lib/api/skills';
import { getOrgTree } from '@/lib/api/org-units';
import type { OrgUnitTreeNode } from '@/types/hrm';

/* ------------------------------------------------------------------ */
/* Types                                                              */
/* ------------------------------------------------------------------ */

export type AvailabilityStatus = 'full' | 'partial' | 'busy';

export interface EmployeeSkill {
    skillId: string | number;
    name: string;
    level: number;
    years?: number;
}

export interface ResourceEmployee {
    id: string | number;
    code: string;
    name: string;
    title: string;
    department: string;
    orgUnitId?: number;
    avatarUrl?: string;
    availability: AvailabilityStatus;
    availabilityPercent: number;
    totalRemainingHours?: number;
    skills: EmployeeSkill[];
    weeklyAvailabilities?: any[];
}

export interface FilterState {
    keyword: string;
    skillId: string;
    minLevel: number;
    availability: AvailabilityStatus | 'all';
    department: string;
}

export type ViewMode = 'table' | 'grid';

export interface DepartmentItem {
    id: string | number;
    name: string;
    [key: string]: any;
}

export interface SkillResourceSearchProps {
    embedded?: boolean;
    departments?: DepartmentItem[];
    employees?: ResourceEmployee[];
    onViewProfile?: (employee: ResourceEmployee) => void;
    onAssignProject?: (employee: ResourceEmployee) => void;
}

interface SelectOption<T = string | number> {
    value: T;
    label: string;
}

/* ------------------------------------------------------------------ */
/* Custom Bo Góc Select Dropdown Component                            */
/* ------------------------------------------------------------------ */

interface CustomSelectProps<T = string | number> {
    value: T;
    onChange: (value: T) => void;
    options: SelectOption<T>[];
    placeholder?: string;
    icon?: React.ReactNode;
    disabled?: boolean;
    className?: string;
}

function CustomSelect<T extends string | number>({
    value,
    onChange,
    options,
    placeholder,
    icon,
    disabled = false,
    className,
}: CustomSelectProps<T>) {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef<HTMLDivElement>(null);

    const selectedOption = options.find((opt) => String(opt.value) === String(value));

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div ref={containerRef} className={cn('relative w-full', className)}>
            <button
                type="button"
                disabled={disabled}
                onClick={() => setIsOpen((prev) => !prev)}
                className={cn(
                    'flex h-10 w-full items-center justify-between rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 transition-all outline-none',
                    'hover:border-indigo-300 focus:border-[#4338ca] focus:ring-2 focus:ring-[#4338ca]/20',
                    disabled && 'cursor-not-allowed bg-slate-50 text-slate-400 border-slate-200'
                )}
            >
                <div className="flex items-center gap-2 truncate pr-2">
                    {icon && <span className="text-slate-400 shrink-0">{icon}</span>}
                    <span className="truncate">{selectedOption ? selectedOption.label : placeholder}</span>
                </div>
                <ChevronDown
                    className={cn(
                        'h-4 w-4 text-slate-400 shrink-0 transition-transform duration-200',
                        isOpen && 'rotate-180 text-indigo-600'
                    )}
                />
            </button>

            {isOpen && !disabled && (
                <div className="absolute left-0 top-[calc(100%+6px)] z-50 max-h-60 w-full overflow-y-auto rounded-2xl border border-slate-100 bg-white p-1.5 shadow-xl ring-1 ring-slate-900/5 transition-all animate-in fade-in-50 zoom-in-95">
                    {options.map((option) => {
                        const isSelected = String(option.value) === String(value);
                        return (
                            <button
                                key={String(option.value)}
                                type="button"
                                onClick={() => {
                                    onChange(option.value);
                                    setIsOpen(false);
                                }}
                                className={cn(
                                    'flex w-full items-center justify-between rounded-xl px-3 py-2 text-xs font-medium transition-colors text-left',
                                    isSelected
                                        ? 'bg-indigo-50 text-indigo-700 font-semibold'
                                        : 'text-slate-700 hover:bg-slate-100 hover:text-slate-900'
                                )}
                            >
                                <span className="truncate">{option.label}</span>
                                {isSelected && <Check className="h-3.5 w-3.5 text-indigo-600 shrink-0 ml-2" />}
                            </button>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

/* ------------------------------------------------------------------ */
/* Options & Helpers                                                  */
/* ------------------------------------------------------------------ */

const LEVEL_OPTIONS: SelectOption<number>[] = [
    { value: 1, label: '≥ Mức 1 (Sơ cấp)' },
    { value: 2, label: '≥ Mức 2 (Trung cấp)' },
    { value: 3, label: '≥ Mức 3 (Khá)' },
    { value: 4, label: '≥ Mức 4 (Giỏi)' },
    { value: 5, label: '≥ Mức 5 (Chuyên gia)' },
];

const AVAILABILITY_LABEL: Record<AvailabilityStatus, string> = {
    full: 'Rảnh 100%',
    partial: 'Rảnh 1 phần',
    busy: 'Đang bận',
};

const AVAILABILITY_FILTER_OPTIONS: { id: FilterState['availability']; label: string }[] = [
    { id: 'all', label: 'Tất cả' },
    { id: 'full', label: 'Rảnh 100%' },
    { id: 'partial', label: 'Rảnh 1 phần' },
    { id: 'busy', label: 'Đang bận' },
];

const DEFAULT_FILTERS: FilterState = {
    keyword: '',
    skillId: 'all',
    minLevel: 1,
    availability: 'all',
    department: 'all',
};

function getCurrentYearAndWeek() {
    const now = new Date();
    const year = now.getFullYear();
    const d = new Date(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    const weekNo = Math.ceil((((d.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
    return { year, week: weekNo };
}

function flattenOrgTree(data: OrgUnitTreeNode | OrgUnitTreeNode[] | null | undefined): { id: number; name: string }[] {
    if (!data) return [];
    const list: { id: number; name: string }[] = [];

    function traverse(node: OrgUnitTreeNode) {
        if (!node) return;
        list.push({ id: node.id, name: node.unitName });
        if (Array.isArray(node.children)) {
            node.children.forEach(traverse);
        }
    }

    if (Array.isArray(data)) {
        data.forEach(traverse);
    } else {
        traverse(data);
    }
    return list;
}

/* ------------------------------------------------------------------ */
/* Sub-components                                                     */
/* ------------------------------------------------------------------ */

function SkillBadge({ name, level, years }: { name: string; level: number; years?: number }) {
    return (
        <span className="inline-flex items-center gap-1.5 rounded-full border border-indigo-100 bg-indigo-50/80 px-2.5 py-1 text-xs font-medium text-indigo-700">
            {name}
            <span className="flex items-center gap-0.5">
                {[1, 2, 3, 4, 5].map((i) => (
                    <Star
                        key={i}
                        className={cn(
                            'h-2.5 w-2.5',
                            i <= level ? 'fill-amber-400 text-amber-400' : 'fill-slate-200 text-slate-200'
                        )}
                    />
                ))}
            </span>
            {years != null && years > 0 && (
                <span className="text-[10px] text-indigo-500 font-normal">({years} năm)</span>
            )}
        </span>
    );
}

function AvailabilityBadge({ status, percent }: { status: AvailabilityStatus; percent: number }) {
    const styles: Record<AvailabilityStatus, string> = {
        full: 'bg-emerald-50 text-emerald-700 border-emerald-100',
        partial: 'bg-amber-50 text-amber-700 border-amber-100',
        busy: 'bg-rose-50 text-rose-700 border-rose-100',
    };
    const dot: Record<AvailabilityStatus, string> = {
        full: 'bg-emerald-500',
        partial: 'bg-amber-500',
        busy: 'bg-rose-500',
    };
    return (
        <span
            className={cn(
                'inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border px-3 py-1 text-xs font-semibold',
                styles[status]
            )}
        >
            <span className={cn('h-1.5 w-1.5 rounded-full', dot[status])} />
            {percent}% · {AVAILABILITY_LABEL[status]}
        </span>
    );
}

function Avatar({ name }: { name: string }) {
    const initials = name
        .trim()
        .split(/\s+/)
        .slice(-2)
        .map((part) => part[0] || '')
        .join('')
        .toUpperCase();

    return (
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-bold text-indigo-700 shadow-2xs">
            {initials || 'NV'}
        </div>
    );
}

/* ------------------------------------------------------------------ */
/* Main Component                                                     */
/* ------------------------------------------------------------------ */

export default function SkillresourceSearch({
    embedded = false,
    departments: propDepartments = [],
    employees: propEmployees = [],
    onViewProfile,
    onAssignProject,
}: SkillResourceSearchProps) {
    const [viewMode] = useState<ViewMode>('table');
    const [filters, setFilters] = useState<FilterState>(DEFAULT_FILTERS);
    const [selectedEmployee, setSelectedEmployee] = useState<ResourceEmployee | null>(null);

    // Dữ liệu thực từ backend
    const [catalogSkills, setCatalogSkills] = useState<SkillResponse[]>([]);
    const [orgUnits, setOrgUnits] = useState<{ id: number; name: string }[]>([]);
    const [realEmployees, setRealEmployees] = useState<ResourceEmployee[]>([]);
    const [loading, setLoading] = useState(false);

    // 1. Tải danh mục kỹ năng & cây phòng ban từ backend
    useEffect(() => {
        async function loadInitData() {
            try {
                const skills = await getSkills();
                setCatalogSkills(skills || []);

                const tree = await getOrgTree();
                const flattened = flattenOrgTree(tree);
                setOrgUnits(flattened);
            } catch (err) {
                console.error('Failed to load initial metadata for resource search:', err);
            }
        }
        loadInitData();
    }, []);

    // Tạo Skill Options động từ Catalog
    const skillOptions = useMemo<SelectOption<string>[]>(() => {
        const defaultOpt: SelectOption<string> = { value: 'all', label: 'Tất cả kỹ năng' };
        if (!catalogSkills || catalogSkills.length === 0) return [defaultOpt];
        return [
            defaultOpt,
            ...catalogSkills.map((s) => ({
                value: String(s.id),
                label: `${s.name} (${s.groupName || 'Khác'})`,
            })),
        ];
    }, [catalogSkills]);

    // Tạo Department Options động
    const departmentOptions = useMemo<SelectOption<string>[]>(() => {
        const defaultOpt: SelectOption<string> = { value: 'all', label: 'Tất cả phòng ban' };
        if (orgUnits.length > 0) {
            return [
                defaultOpt,
                ...orgUnits.map((u) => ({
                    value: String(u.id),
                    label: u.name,
                })),
            ];
        }
        if (propDepartments && propDepartments.length > 0) {
            return [
                defaultOpt,
                ...propDepartments.map((d) => ({
                    value: String(d.id),
                    label: d.name,
                })),
            ];
        }
        return [defaultOpt];
    }, [orgUnits, propDepartments]);

    // 2. Gọi backend tìm kiếm nhân sự theo kỹ năng & độ rảnh
    const executeSearch = async () => {
        setLoading(true);
        try {
            const { year, week } = getCurrentYearAndWeek();
            const fromYear = year;
            const fromWeek = week;
            const toYear = year;
            const toWeek = Math.min(52, week + 12); // Tìm trong 12 tuần tới

            let rawResults: ResourceSearchResultItem[] = [];

            if (filters.skillId !== 'all') {
                rawResults = await searchResourcesBySkill({
                    skillId: Number(filters.skillId),
                    minProficiencyLevel: filters.minLevel,
                    orgUnitId: filters.department !== 'all' ? Number(filters.department) : undefined,
                    fromYear,
                    fromWeek,
                    toYear,
                    toWeek,
                });
            } else {
                // Nếu chọn 'all', gom nhóm kết quả từ các kỹ năng có trong catalog
                const sampleSkills = catalogSkills.slice(0, 5);
                const promises = sampleSkills.map((s) =>
                    searchResourcesBySkill({
                        skillId: s.id,
                        minProficiencyLevel: filters.minLevel,
                        orgUnitId: filters.department !== 'all' ? Number(filters.department) : undefined,
                        fromYear,
                        fromWeek,
                        toYear,
                        toWeek,
                    }).catch(() => [])
                );
                const allBatches = await Promise.all(promises);
                const flattened = allBatches.flat();
                rawResults = flattened;
            }

            // Gom nhóm theo nhân viên để không bị trùng lặp dòng khi 1 người có nhiều kỹ năng
            const employeeMap = new Map<number, ResourceEmployee>();

            for (const item of rawResults) {
                const empId = item.employeeId;
                const totalRemHours = item.totalRemainingHours ?? 0;

                // Tính toán tỷ lệ rảnh
                let availStatus: AvailabilityStatus = 'full';
                let availPercent = 100;

                if (totalRemHours <= 0) {
                    availStatus = 'busy';
                    availPercent = 0;
                } else {
                    const hasAllocated = item.weeklyAvailabilities?.some((w) => (w.totalAllocatedHours ?? 0) > 0);
                    if (hasAllocated) {
                        availStatus = 'partial';
                        const totalCap = item.weeklyAvailabilities?.reduce((sum, w) => sum + (w.standardHours ?? 40), 0) || 1;
                        availPercent = Math.round((totalRemHours / totalCap) * 100);
                        if (availPercent > 100) availPercent = 100;
                    } else {
                        availStatus = 'full';
                        availPercent = 100;
                    }
                }

                if (!employeeMap.has(empId)) {
                    employeeMap.set(empId, {
                        id: item.employeeId,
                        code: item.employeeCode,
                        name: item.fullName,
                        title: item.jobTitle || 'Nhân sự dự án',
                        department: item.orgUnitName,
                        orgUnitId: item.orgUnitId,
                        availability: availStatus,
                        availabilityPercent: availPercent,
                        totalRemainingHours: totalRemHours,
                        skills: [
                            {
                                skillId: item.skillId,
                                name: item.skillName,
                                level: item.proficiencyLevel,
                                years: Number(item.yearsOfExperience) || 0,
                            },
                        ],
                        weeklyAvailabilities: item.weeklyAvailabilities,
                    });
                } else {
                    const existing = employeeMap.get(empId)!;
                    if (!existing.skills.some((s) => s.skillId === item.skillId)) {
                        existing.skills.push({
                            skillId: item.skillId,
                            name: item.skillName,
                            level: item.proficiencyLevel,
                            years: Number(item.yearsOfExperience) || 0,
                        });
                    }
                }
            }

            setRealEmployees(Array.from(employeeMap.values()));
        } catch (err) {
            console.error('Failed to search resources:', err);
        } finally {
            setLoading(false);
        }
    };

    // Tự động tìm kiếm khi filter thay đổi
    useEffect(() => {
        if (catalogSkills.length > 0) {
            executeSearch();
        }
    }, [filters.skillId, filters.minLevel, filters.department, catalogSkills.length]);

    function updateFilter<K extends keyof FilterState>(key: K, value: FilterState[K]) {
        setFilters((prev) => ({ ...prev, [key]: value }));
    }

    function resetFilters() {
        setFilters(DEFAULT_FILTERS);
    }

    function handleViewProfile(emp: ResourceEmployee) {
        setSelectedEmployee(emp);
        if (onViewProfile) {
            onViewProfile(emp);
        }
    }

    const dataSource = realEmployees.length > 0 ? realEmployees : propEmployees;

    const filteredEmployees = useMemo(() => {
        const keyword = filters.keyword.trim().toLowerCase();

        return dataSource.filter((emp) => {
            const matchesKeyword =
                keyword.length === 0 ||
                emp.name.toLowerCase().includes(keyword) ||
                emp.code.toLowerCase().includes(keyword) ||
                emp.title.toLowerCase().includes(keyword) ||
                emp.skills.some((s) => s.name.toLowerCase().includes(keyword));

            const matchesAvailability =
                filters.availability === 'all' || emp.availability === filters.availability;

            return matchesKeyword && matchesAvailability;
        });
    }, [filters.keyword, filters.availability, dataSource]);

    return (
        <div
            className={cn(
                'relative w-full transition-all',
                embedded
                    ? 'p-0 text-slate-900'
                    : 'rounded-3xl bg-white p-6 text-slate-900 shadow-sm border border-slate-200'
            )}
        >
            {/* Header */}
            <div className="flex flex-col justify-between gap-1 sm:flex-row sm:items-center">
                <div>
                    <h2 className="text-lg font-bold tracking-tight text-slate-900 flex items-center gap-2">
                        <Users className="h-5 w-5 text-indigo-600" />
                        Tra cứu nhân sự theo Kỹ năng & Thời gian rảnh
                    </h2>
                    <p className="mt-0.5 max-w-2xl text-xs text-slate-500">
                        Tìm kiếm nhân viên phù hợp theo kỹ năng, mức thành thạo (sao) và số giờ còn khả dụng trong tuần để phân bổ dự án.
                    </p>
                </div>

                <div className="flex items-center gap-2">
                    <span className="inline-flex items-center gap-1.5 rounded-xl border border-indigo-100 bg-indigo-50/80 px-3 py-1.5 text-xs font-semibold text-indigo-700">
                        Kết quả: {filteredEmployees.length} nhân sự
                    </span>
                </div>
            </div>

            {/* Khung Bộ Lọc Tìm Kiếm */}
            <div className="mt-3 rounded-2xl bg-white p-4 text-slate-900 shadow-sm border border-slate-200">
                <div className="flex items-center justify-between text-xs font-semibold text-slate-700 mb-3">
                    <div className="flex items-center gap-2">
                        <SlidersHorizontal className="h-4 w-4 text-indigo-600" />
                        <span>Bộ lọc tìm kiếm năng lực & độ rảnh</span>
                    </div>
                    <button
                        type="button"
                        onClick={resetFilters}
                        className="flex items-center gap-1 text-xs font-medium text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
                    >
                        <RotateCcw className="h-3 w-3" />
                        Đặt lại bộ lọc
                    </button>
                </div>

                <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4 items-center">
                    {/* Ô tìm từ khóa */}
                    <div className="relative flex items-center">
                        <Search className="pointer-events-none absolute left-3 h-4 w-4 text-slate-400" />
                        <input
                            value={filters.keyword}
                            onChange={(e) => updateFilter('keyword', e.target.value)}
                            placeholder="Tên, mã NV, vị trí, kỹ năng..."
                            className="h-10 w-full rounded-xl border border-slate-200 bg-white pl-9 pr-8 text-xs text-slate-900 placeholder:text-slate-400 focus:border-[#4338ca] focus:outline-none focus:ring-2 focus:ring-[#4338ca]/20"
                        />
                        {filters.keyword && (
                            <button
                                type="button"
                                onClick={() => updateFilter('keyword', '')}
                                className="absolute right-2.5 rounded-full p-0.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
                                title="Xóa tìm kiếm"
                            >
                                <X className="h-3.5 w-3.5" />
                            </button>
                        )}
                    </div>

                    {/* Kỹ năng & Mức sao */}
                    <div className="flex gap-2">
                        <CustomSelect
                            value={filters.skillId}
                            onChange={(val) => updateFilter('skillId', String(val))}
                            options={skillOptions}
                            placeholder="Chọn kỹ năng"
                            className="flex-1"
                        />

                        <CustomSelect
                            value={filters.minLevel}
                            onChange={(val) => updateFilter('minLevel', Number(val))}
                            options={LEVEL_OPTIONS}
                            className="w-32 shrink-0"
                        />
                    </div>

                    {/* Phòng ban / Bộ phận */}
                    <CustomSelect
                        value={filters.department}
                        onChange={(val) => updateFilter('department', String(val))}
                        options={departmentOptions}
                        icon={<Building2 className="h-4 w-4" />}
                    />

                    {/* Trạng thái sẵn sàng */}
                    <div className="flex h-10 items-center rounded-xl border border-slate-200 bg-slate-50 p-1 text-xs font-semibold overflow-hidden">
                        {AVAILABILITY_FILTER_OPTIONS.map((opt) => (
                            <button
                                key={opt.id}
                                type="button"
                                onClick={() => updateFilter('availability', opt.id)}
                                className={cn(
                                    'flex h-full flex-1 items-center justify-center rounded-lg px-1 text-[11px] font-semibold transition-all whitespace-nowrap cursor-pointer',
                                    filters.availability === opt.id
                                        ? 'bg-white text-indigo-700 shadow-2xs font-bold'
                                        : 'text-slate-500 hover:text-slate-800'
                                )}
                            >
                                {opt.label}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {/* Kết quả tìm kiếm */}
            {loading ? (
                <div className="mt-4 rounded-2xl bg-white p-12 text-center border border-slate-200 shadow-xs">
                    <Loader2 className="mx-auto h-8 w-8 text-indigo-600 animate-spin" />
                    <p className="mt-3 text-xs font-medium text-slate-500">Đang tra cứu dữ liệu nhân lực & độ rảnh từ hệ thống...</p>
                </div>
            ) : filteredEmployees.length === 0 ? (
                <div className="mt-4 rounded-2xl bg-white p-12 text-center border border-slate-200 shadow-xs text-slate-900">
                    <Users className="mx-auto h-10 w-10 text-slate-300" />
                    <p className="mt-3 text-sm font-semibold text-slate-700">
                        Không tìm thấy nhân sự phù hợp với điều kiện tra cứu
                    </p>
                    <p className="mt-1 text-xs text-slate-400 max-w-md mx-auto">
                        Thử chọn kỹ năng khác, giảm mức sao tối thiểu hoặc chọn "Tất cả phòng ban".
                    </p>
                    <button
                        type="button"
                        onClick={resetFilters}
                        className="mt-4 rounded-xl bg-indigo-50 border border-indigo-200 px-4 py-2 text-xs font-semibold text-indigo-600 hover:bg-indigo-100 transition cursor-pointer"
                    >
                        Xóa bộ lọc
                    </button>
                </div>
            ) : viewMode === 'table' ? (
                <div className="mt-4 overflow-x-auto rounded-2xl bg-white text-slate-900 shadow-xs border border-slate-200">
                    <table className="w-full text-left text-xs border-collapse">
                        <thead>
                            <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                                <th className="px-4 py-3 align-middle">Nhân viên</th>
                                <th className="px-4 py-3 align-middle">Bộ phận / Đơn vị</th>
                                <th className="px-4 py-3 align-middle">Kỹ năng phù hợp</th>
                                <th className="px-4 py-3 align-middle">Khả dụng & Giờ rảnh</th>
                                <th className="px-4 py-3 text-right align-middle">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {filteredEmployees.map((emp) => (
                                <tr key={emp.id} className="transition-colors hover:bg-slate-50/70">
                                    <td className="px-4 py-3 align-middle">
                                        <div className="flex items-center gap-3">
                                            <Avatar name={emp.name} />
                                            <div>
                                                <p className="font-semibold text-slate-900">{emp.name}</p>
                                                <p className="text-[11px] text-slate-400">
                                                    {emp.code} · {emp.title}
                                                </p>
                                            </div>
                                        </div>
                                    </td>

                                    <td className="px-4 py-3 align-middle text-slate-600">
                                        <span className="inline-flex items-center gap-1.5 text-xs font-medium">
                                            <Building2 className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                                            {emp.department}
                                        </span>
                                    </td>

                                    <td className="px-4 py-3 align-middle">
                                        <div className="flex flex-wrap items-center gap-1.5 max-w-md">
                                            {emp.skills.map((s) => (
                                                <SkillBadge key={s.skillId} name={s.name} level={s.level} years={s.years} />
                                            ))}
                                        </div>
                                    </td>

                                    <td className="px-4 py-3 align-middle">
                                        <div className="flex flex-col gap-1">
                                            <AvailabilityBadge
                                                status={emp.availability}
                                                percent={emp.availabilityPercent}
                                            />
                                            <span className="text-[10px] text-slate-400 flex items-center gap-1">
                                                <Clock className="h-3 w-3" />
                                                Tổng rảnh: <strong className="text-slate-700 font-semibold">{emp.totalRemainingHours}h</strong> trong kỳ
                                            </span>
                                        </div>
                                    </td>

                                    <td className="px-4 py-3 align-middle text-right">
                                        <div className="flex items-center justify-end gap-1">
                                            <button
                                                type="button"
                                                title="Xem chi tiết năng lực & giờ rảnh"
                                                onClick={() => handleViewProfile(emp)}
                                                className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-indigo-50 hover:text-indigo-600 cursor-pointer"
                                            >
                                                <Eye className="h-4 w-4" />
                                            </button>
                                            {onAssignProject && (
                                                <button
                                                    type="button"
                                                    title="Gán vào dự án"
                                                    onClick={() => onAssignProject?.(emp)}
                                                    className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-indigo-50 hover:text-indigo-600 cursor-pointer"
                                                >
                                                    <SendHorizontal className="h-4 w-4" />
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            ) : null}

            {/* Modal Popup Chi Tiết Hồ Sơ Nhân Viên */}
            {selectedEmployee && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm animate-in fade-in duration-200">
                    <div className="relative w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl transition-all animate-in zoom-in-95 duration-200 text-slate-900 border border-slate-100">
                        <button
                            type="button"
                            onClick={() => setSelectedEmployee(null)}
                            className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors cursor-pointer"
                        >
                            <X className="h-5 w-5" />
                        </button>

                        <div className="flex items-start justify-between gap-3 pr-8">
                            <div className="flex items-center gap-3">
                                <Avatar name={selectedEmployee.name} />
                                <div>
                                    <h3 className="text-base font-bold text-slate-900">{selectedEmployee.name}</h3>
                                    <p className="text-xs text-slate-500 font-medium">{selectedEmployee.title}</p>
                                </div>
                            </div>
                            <AvailabilityBadge
                                status={selectedEmployee.availability}
                                percent={selectedEmployee.availabilityPercent}
                            />
                        </div>

                        <div className="mt-4 flex items-center justify-between rounded-xl bg-slate-50 p-3 text-xs text-slate-600">
                            <div className="flex items-center gap-2">
                                <Building2 className="h-4 w-4 text-slate-400 shrink-0" />
                                <span>{selectedEmployee.department}</span>
                                <span className="text-slate-300">•</span>
                                <span className="font-semibold text-slate-700">{selectedEmployee.code}</span>
                            </div>
                            <div className="flex items-center gap-1 font-semibold text-indigo-700">
                                <Clock className="h-3.5 w-3.5" />
                                <span>{selectedEmployee.totalRemainingHours}h khả dụng</span>
                            </div>
                        </div>

                        <div className="mt-5">
                            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
                                Kỹ năng sở hữu ({selectedEmployee.skills.length})
                            </h4>
                            <div className="flex flex-wrap gap-2">
                                {selectedEmployee.skills.map((s) => (
                                    <SkillBadge key={s.skillId} name={s.name} level={s.level} years={s.years} />
                                ))}
                            </div>
                        </div>

                        {/* Chi tiết khả dụng các tuần */}
                        {selectedEmployee.weeklyAvailabilities && selectedEmployee.weeklyAvailabilities.length > 0 && (
                            <div className="mt-5">
                                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                                    Chi tiết số giờ khả dụng theo tuần (Tuần gần nhất)
                                </h4>
                                <div className="max-h-40 overflow-y-auto rounded-xl border border-slate-200 divide-y divide-slate-100">
                                    {selectedEmployee.weeklyAvailabilities.slice(0, 8).map((w: any, idx: number) => (
                                        <div key={idx} className="flex items-center justify-between px-3 py-2 text-xs">
                                            <span className="font-medium text-slate-700">
                                                Tuần {w.weekNumber}/{w.year}
                                            </span>
                                            <div className="flex items-center gap-3">
                                                <span className="text-slate-500 text-[11px]">Chuẩn: {w.standardHours ?? 40}h</span>
                                                <span className="text-amber-600 text-[11px]">Đã gán: {w.totalAllocatedHours ?? 0}h</span>
                                                <span className="font-bold text-emerald-600">Còn rảnh: {w.remainingHours ?? 40}h</span>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className="mt-6 flex items-center gap-2 pt-3 border-t border-slate-100">
                            <button
                                type="button"
                                onClick={() => setSelectedEmployee(null)}
                                className="flex-1 rounded-xl border border-slate-200 py-2.5 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition-colors cursor-pointer"
                            >
                                Đóng
                            </button>
                            {onAssignProject && (
                                <button
                                    type="button"
                                    onClick={() => {
                                        onAssignProject?.(selectedEmployee);
                                        setSelectedEmployee(null);
                                    }}
                                    className="flex-1 rounded-xl bg-[#4338ca] py-2.5 text-xs font-semibold text-white hover:bg-[#372fa8] transition-colors shadow-sm cursor-pointer"
                                >
                                    Phân bổ vào dự án
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}