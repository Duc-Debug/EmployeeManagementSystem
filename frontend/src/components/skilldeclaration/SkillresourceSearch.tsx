import { useMemo, useState, useRef, useEffect } from 'react';
import {
    Briefcase,
    Building2,
    ChevronDown,
    Check,
    Eye,
    LayoutGrid,
    RotateCcw,
    Search,
    SendHorizontal,
    SlidersHorizontal,
    Star,
    Table2,
    Users,
    X,
} from 'lucide-react';
import { cn } from '@/lib/utils';

/* ------------------------------------------------------------------ */
/* Types                                                              */
/* ------------------------------------------------------------------ */

export type AvailabilityStatus = 'full' | 'partial' | 'busy';

export interface EmployeeSkill {
    skillId: string;
    name: string;
    level: number;
}

export interface ResourceEmployee {
    id: string;
    code: string;
    name: string;
    title: string;
    department: string;
    avatarUrl?: string;
    availability: AvailabilityStatus;
    availabilityPercent: number;
    skills: EmployeeSkill[];
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
    id: string;
    name: string;
    [key: string]: any;
}

export interface SkillResourceSearchProps {
    embedded?: boolean;
    departments?: DepartmentItem[];
    employees?: ResourceEmployee[]; // Nhận danh sách nhân sự động từ MainLayout
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

    const selectedOption = options.find((opt) => opt.value === value);

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
                        const isSelected = option.value === value;
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
/* Options                                                            */
/* ------------------------------------------------------------------ */

const SKILL_OPTIONS: SelectOption<string>[] = [
    { value: 'all', label: 'Tất cả kỹ năng' },
    { value: 'java', label: 'Java' },
    { value: 'react', label: 'React.js' },
    { value: 'typescript', label: 'TypeScript' },
    { value: 'nodejs', label: 'Node.js' },
    { value: 'docker', label: 'Docker' },
    { value: 'kubernetes', label: 'Kubernetes' },
    { value: 'aws', label: 'AWS' },
    { value: 'sql', label: 'SQL' },
    { value: 'python', label: 'Python' },
    { value: 'selenium', label: 'Selenium' },
];

const LEVEL_OPTIONS: SelectOption<number>[] = [
    { value: 1, label: '≥ 1 sao' },
    { value: 2, label: '≥ 2 sao' },
    { value: 3, label: '≥ 3 sao' },
    { value: 4, label: '≥ 4 sao' },
    { value: 5, label: '≥ 5 sao' },
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

/* ------------------------------------------------------------------ */
/* Sub-components                                                     */
/* ------------------------------------------------------------------ */

function SkillBadge({ name, level }: { name: string; level: number }) {
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
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-xs font-bold text-indigo-700">
            {initials || 'NV'}
        </div>
    );
}

/* ------------------------------------------------------------------ */
/* Main Component                                                     */
/* ------------------------------------------------------------------ */

export default function SkillresourceSearch({
                                                embedded = false,
                                                departments = [],
                                                employees = [],
                                                onViewProfile,
                                                onAssignProject,
                                            }: SkillResourceSearchProps) {
    const [viewMode, setViewMode] = useState<ViewMode>('table');
    const [filters, setFilters] = useState<FilterState>(DEFAULT_FILTERS);
    const [selectedEmployee, setSelectedEmployee] = useState<ResourceEmployee | null>(null);

    // Chuyển danh sách phòng ban động từ props thành dạng Options cho CustomSelect
    const departmentOptions = useMemo<SelectOption<string>[]>(() => {
        const defaultOption: SelectOption<string> = { value: 'all', label: 'Tất cả phòng ban' };
        if (!departments || departments.length === 0) {
            return [defaultOption];
        }
        return [
            defaultOption,
            ...departments.map((d) => ({
                value: d.name,
                label: d.name,
            })),
        ];
    }, [departments]);

    // TỰ ĐỘNG BẢO VỆ: Reset lựa chọn phòng ban về 'all' nếu phòng ban đã chọn bị xóa khỏi hệ thống
    useEffect(() => {
        if (filters.department !== 'all') {
            const isExist = departments.some((d) => d.name === filters.department);
            if (!isExist) {
                setFilters((prev) => ({ ...prev, department: 'all' }));
            }
        }
    }, [departments, filters.department]);

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

    const filteredEmployees = useMemo(() => {
        const keyword = filters.keyword.trim().toLowerCase();

        return employees.filter((emp) => {
            const matchesKeyword =
                keyword.length === 0 ||
                emp.name.toLowerCase().includes(keyword) ||
                emp.code.toLowerCase().includes(keyword) ||
                emp.title.toLowerCase().includes(keyword) ||
                emp.skills.some((s) => s.name.toLowerCase().includes(keyword));

            const matchesSkill =
                filters.skillId === 'all' ||
                emp.skills.some((s) => s.skillId === filters.skillId && s.level >= filters.minLevel);

            const matchesAvailability =
                filters.availability === 'all' || emp.availability === filters.availability;

            const matchesDepartment =
                filters.department === 'all' || emp.department === filters.department;

            return matchesKeyword && matchesSkill && matchesAvailability && matchesDepartment;
        });
    }, [filters, employees]);

    return (
        <div
            className={cn(
                'relative w-full transition-all',
                embedded
                    ? 'p-0 text-slate-900'
                    : 'rounded-3xl bg-gradient-to-br from-[#7c3aed] via-[#4f46e5] to-[#2563eb] p-6 text-white shadow-xl sm:p-8'
            )}
        >
            {/* Header */}
            <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
                <div>
                    <h2 className="text-xl font-bold text-white">
                        Tra cứu & Tìm kiếm nhân sự
                    </h2>
                    <p className="mt-1 max-w-md text-sm text-white/90">
                        Tìm kiếm nhân sự theo từ khóa, kỹ năng, phòng ban và mức độ sẵn sàng. Kết quả:{' '}
                        <span className="font-bold text-white underline underline-offset-2">
                            {filteredEmployees.length}
                        </span>{' '}
                        nhân sự.
                    </p>
                </div>

                <div className="flex items-center gap-1 rounded-full border border-white/20 bg-white/10 p-1 backdrop-blur-sm">
                    <button
                        type="button"
                        onClick={() => setViewMode('table')}
                        className={cn(
                            'flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold transition-colors',
                            viewMode === 'table'
                                ? 'bg-white text-[#4338ca] shadow-sm'
                                : 'text-white/80 hover:bg-white/10 hover:text-white'
                        )}
                    >
                        <Table2 className="h-3.5 w-3.5" />
                        Bảng
                    </button>
                    <button
                        type="button"
                        onClick={() => setViewMode('grid')}
                        className={cn(
                            'flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold transition-colors',
                            viewMode === 'grid'
                                ? 'bg-white text-[#4338ca] shadow-sm'
                                : 'text-white/80 hover:bg-white/10 hover:text-white'
                        )}
                    >
                        <LayoutGrid className="h-3.5 w-3.5" />
                        Thẻ
                    </button>
                </div>
            </div>

            {/* Khung Bộ Lọc Tìm Kiếm */}
            <div className="mt-4 rounded-2xl bg-white p-4 text-slate-900 shadow-lg sm:p-5">
                <div className="flex items-center justify-between text-sm font-semibold text-slate-700">
                    <div className="flex items-center gap-2">
                        <SlidersHorizontal className="h-4 w-4 text-slate-400" />
                        Bộ lọc tìm kiếm
                    </div>
                    <button
                        type="button"
                        onClick={resetFilters}
                        className="flex items-center gap-1 text-xs font-normal text-indigo-600 hover:text-indigo-800"
                    >
                        <RotateCcw className="h-3 w-3" />
                        Đặt lại
                    </button>
                </div>

                <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4 items-center">
                    <div className="relative flex items-center">
                        <Search className="pointer-events-none absolute left-3 h-4 w-4 text-slate-400" />
                        <input
                            value={filters.keyword}
                            onChange={(e) => updateFilter('keyword', e.target.value)}
                            placeholder="Tên, mã NV, vị trí, kỹ năng..."
                            className="h-10 w-full rounded-xl border border-slate-200 bg-white pl-9 pr-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-[#4338ca] focus:outline-none focus:ring-2 focus:ring-[#4338ca]/20"
                        />
                    </div>

                    <div className="flex gap-2">
                        <CustomSelect
                            value={filters.skillId}
                            onChange={(val) => updateFilter('skillId', val)}
                            options={SKILL_OPTIONS}
                            className="flex-1"
                        />

                        <CustomSelect
                            value={filters.minLevel}
                            onChange={(val) => updateFilter('minLevel', val)}
                            options={LEVEL_OPTIONS}
                            disabled={filters.skillId === 'all'}
                            className="w-28 shrink-0"
                        />
                    </div>

                    <CustomSelect
                        value={filters.department}
                        onChange={(val) => updateFilter('department', val)}
                        options={departmentOptions}
                        icon={<Building2 className="h-4 w-4" />}
                    />

                    <div className="flex h-10 items-center rounded-xl border border-slate-200 bg-slate-50 p-1 text-xs font-semibold overflow-hidden">
                        {AVAILABILITY_FILTER_OPTIONS.map((opt) => (
                            <button
                                key={opt.id}
                                type="button"
                                onClick={() => updateFilter('availability', opt.id)}
                                className={cn(
                                    'flex h-full flex-1 items-center justify-center rounded-lg px-1.5 text-[11px] font-semibold transition-all whitespace-nowrap',
                                    filters.availability === opt.id
                                        ? 'bg-white text-[#4338ca] shadow-sm'
                                        : 'text-slate-500 hover:text-slate-700'
                                )}
                            >
                                {opt.label}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {/* Kết quả tìm kiếm */}
            {filteredEmployees.length === 0 ? (
                <div className="mt-4 rounded-2xl bg-white p-10 text-center shadow-lg text-slate-900">
                    <Users className="mx-auto h-10 w-10 text-slate-300" />
                    <p className="mt-3 text-sm font-medium text-slate-600">
                        Không tìm thấy nhân sự phù hợp với điều kiện tra cứu.
                    </p>
                    <button
                        type="button"
                        onClick={resetFilters}
                        className="mt-3 rounded-lg bg-indigo-50 px-4 py-2 text-xs font-semibold text-indigo-600 hover:bg-indigo-100"
                    >
                        Xóa bộ lọc
                    </button>
                </div>
            ) : viewMode === 'table' ? (
                <div className="mt-4 overflow-x-auto rounded-2xl bg-white text-slate-900 shadow-lg">
                    <table className="w-full text-left text-sm border-collapse">
                        <thead>
                        <tr className="bg-slate-50/80 border-b border-slate-100 text-[11px] font-bold uppercase tracking-wider text-slate-400">
                            <th className="px-5 py-3.5 align-middle">Nhân viên</th>
                            <th className="px-5 py-3.5 align-middle">Phòng ban</th>
                            <th className="px-5 py-3.5 align-middle">Kỹ năng sở hữu</th>
                            <th className="px-5 py-3.5 align-middle">Mức độ rảnh</th>
                            <th className="px-5 py-3.5 text-right align-middle">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100/80">
                        {filteredEmployees.map((emp) => (
                            <tr key={emp.id} className="transition-colors hover:bg-slate-50/70">
                                <td className="px-5 py-4 align-middle">
                                    <div className="flex items-center gap-3">
                                        <Avatar name={emp.name} />
                                        <div className="flex flex-col justify-center">
                                            <p className="font-semibold text-slate-900 leading-tight">{emp.name}</p>
                                            <p className="mt-1 text-xs text-slate-400 leading-none">
                                                {emp.code} · {emp.title}
                                            </p>
                                        </div>
                                    </div>
                                </td>

                                <td className="px-5 py-4 align-middle text-slate-600">
                                        <span className="inline-flex items-center gap-2 text-xs font-medium">
                                            <Building2 className="h-4 w-4 text-slate-400 shrink-0" />
                                            {emp.department}
                                        </span>
                                </td>

                                <td className="px-5 py-4 align-middle">
                                    <div className="flex flex-wrap items-center gap-x-1.5 gap-y-2 max-w-md">
                                        {emp.skills.map((s) => (
                                            <SkillBadge key={s.skillId} name={s.name} level={s.level} />
                                        ))}
                                    </div>
                                </td>

                                <td className="px-5 py-4 align-middle">
                                    <AvailabilityBadge
                                        status={emp.availability}
                                        percent={emp.availabilityPercent}
                                    />
                                </td>

                                <td className="px-5 py-4 align-middle text-right">
                                    <div className="flex items-center justify-end gap-1.5">
                                        <button
                                            type="button"
                                            title="Xem hồ sơ"
                                            onClick={() => handleViewProfile(emp)}
                                            className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-indigo-50 hover:text-indigo-600"
                                        >
                                            <Eye className="h-4 w-4" />
                                        </button>
                                        <button
                                            type="button"
                                            title="Gán dự án"
                                            onClick={() => onAssignProject?.(emp)}
                                            className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-indigo-50 hover:text-indigo-600"
                                        >
                                            <SendHorizontal className="h-4 w-4" />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            ) : (
                <div className="mt-4 rounded-2xl bg-white p-4 text-slate-900 shadow-lg sm:p-5">
                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
                        {filteredEmployees.map((emp) => (
                            <div
                                key={emp.id}
                                className="flex flex-col gap-3 rounded-2xl border border-slate-200 p-4 transition-shadow hover:shadow-md"
                            >
                                <div className="flex items-start justify-between gap-2">
                                    <div className="flex items-center gap-3">
                                        <Avatar name={emp.name} />
                                        <div>
                                            <p className="font-semibold text-slate-900">{emp.name}</p>
                                            <p className="text-xs text-slate-500">{emp.title}</p>
                                        </div>
                                    </div>
                                    <AvailabilityBadge
                                        status={emp.availability}
                                        percent={emp.availabilityPercent}
                                    />
                                </div>

                                <p className="flex items-center gap-1.5 text-xs text-slate-500">
                                    <Briefcase className="h-3.5 w-3.5 text-slate-400" />
                                    {emp.department} · {emp.code}
                                </p>

                                <div className="flex flex-wrap gap-1.5">
                                    {emp.skills.map((s) => (
                                        <SkillBadge key={s.skillId} name={s.name} level={s.level} />
                                    ))}
                                </div>

                                <div className="mt-auto flex w-full items-center gap-2 pt-2">
                                    <button
                                        type="button"
                                        onClick={() => handleViewProfile(emp)}
                                        className="flex-1 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 transition-colors hover:bg-slate-50"
                                    >
                                        Xem hồ sơ
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => onAssignProject?.(emp)}
                                        className="flex-1 rounded-lg bg-[#4338ca] px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-[#372fa8]"
                                    >
                                        Gán dự án
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Modal Popup Chi Tiết Hồ Sơ Nhân Viên */}
            {selectedEmployee && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm animate-in fade-in duration-200">
                    <div className="relative w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl transition-all animate-in zoom-in-95 duration-200 text-slate-900 border border-slate-100">
                        <button
                            type="button"
                            onClick={() => setSelectedEmployee(null)}
                            className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors"
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

                        <div className="mt-4 flex items-center gap-2 rounded-xl bg-slate-50 p-3 text-xs text-slate-600">
                            <Building2 className="h-4 w-4 text-slate-400 shrink-0" />
                            <span>{selectedEmployee.department}</span>
                            <span className="text-slate-300">•</span>
                            <span className="font-semibold text-slate-700">{selectedEmployee.code}</span>
                        </div>

                        <div className="mt-5">
                            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
                                Kỹ năng sở hữu ({selectedEmployee.skills.length})
                            </h4>
                            <div className="flex flex-wrap gap-2">
                                {selectedEmployee.skills.map((s) => (
                                    <SkillBadge key={s.skillId} name={s.name} level={s.level} />
                                ))}
                            </div>
                        </div>

                        <div className="mt-6 flex items-center gap-2 pt-2 border-t border-slate-100">
                            <button
                                type="button"
                                onClick={() => setSelectedEmployee(null)}
                                className="flex-1 rounded-xl border border-slate-200 py-2.5 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition-colors"
                            >
                                Đóng
                            </button>
                            <button
                                type="button"
                                onClick={() => {
                                    onAssignProject?.(selectedEmployee);
                                    setSelectedEmployee(null);
                                }}
                                className="flex-1 rounded-xl bg-[#4338ca] py-2.5 text-xs font-semibold text-white hover:bg-[#372fa8] transition-colors shadow-md shadow-indigo-100"
                            >
                                Gán dự án
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}