import type { ModuleDef, RoleTheme, DataScope, Role } from "./access.types";

export interface ThemeOption {
    key: RoleTheme;
    label: string;
}

export const THEME_OPTIONS: ThemeOption[] = [
    { key: "blue", label: "Xanh dương" },
    { key: "purple", label: "Tím" },
    { key: "indigo", label: "Chàm" },
    { key: "emerald", label: "Xanh lá" },
    { key: "amber", label: "Vàng cam" },
    { key: "rose", label: "Hồng" },
    { key: "slate", label: "Xám" },
];

export const THEME_SOLID_BG: Record<RoleTheme, string> = {
    blue: "bg-blue-600",
    purple: "bg-purple-600",
    indigo: "bg-indigo-600",
    emerald: "bg-emerald-600",
    amber: "bg-amber-600",
    rose: "bg-rose-600",
    slate: "bg-slate-600",
};

export const THEME_STYLES: Record<
    RoleTheme,
    { iconBg: string; iconText: string; chipBorder: string }
> = {
    blue: { iconBg: "bg-blue-500/10", iconText: "text-blue-400", chipBorder: "border-blue-500/20" },
    purple: { iconBg: "bg-purple-500/10", iconText: "text-purple-400", chipBorder: "border-purple-500/20" },
    indigo: { iconBg: "bg-indigo-500/10", iconText: "text-indigo-400", chipBorder: "border-indigo-500/20" },
    emerald: { iconBg: "bg-emerald-500/10", iconText: "text-emerald-400", chipBorder: "border-emerald-500/20" },
    amber: { iconBg: "bg-amber-500/10", iconText: "text-amber-400", chipBorder: "border-amber-500/20" },
    rose: { iconBg: "bg-rose-500/10", iconText: "text-rose-400", chipBorder: "border-rose-500/20" },
    slate: { iconBg: "bg-slate-500/10", iconText: "text-slate-400", chipBorder: "border-slate-500/20" },
};

/** Canonical list of data-scope options (label only). Components that also
 *  need an icon per option — e.g. DataScopeSelector — keep their own icon
 *  map keyed by the same `value`, rather than duplicating this list. */
export const SCOPE_OPTIONS: { value: DataScope; label: string }[] = [
    { value: "all", label: "Toàn công ty" },
    { value: "department_managed", label: "Phòng ban quản lý" },
    { value: "department_own", label: "Phòng ban thuộc về" },
    { value: "personal", label: "Cá nhân" },
    { value: "custom_tree", label: "Cây đơn vị tùy chỉnh" },
];

export const MODULES: ModuleDef[] = [
    {
        id: "employee",
        name: "Quản lý nhân sự",
        category: "Hệ thống",
        defaultScope: "department_managed",
        defaultActions: { view: true, create: true, edit: true, delete: false, approve: false, export: true },
    },
    {
        id: "department",
        name: "Quản lý phòng ban",
        category: "Hệ thống",
        defaultScope: "all",
        defaultActions: { view: true, create: true, edit: true, delete: true, approve: true, export: true },
    },
    {
        id: "attendance",
        name: "Chấm công & Bảng công",
        category: "Vận hành",
        defaultScope: "department_own",
        defaultActions: { view: true, create: false, edit: true, delete: false, approve: true, export: true },
    },
    {
        id: "payroll",
        name: "Lương & Lợi ích",
        category: "Tài chính",
        defaultScope: "personal",
        defaultActions: { view: true, create: false, edit: false, delete: false, approve: false, export: false },
    },
];

export const ROLES: Omit<Role, "permissions">[] = [
    {
        id: "admin",
        name: "Quản trị viên",
        description: "Toàn quyền quản trị hệ thống và cấu hình phân quyền",
        isSystemRole: true,
        userCount: 3,
        theme: "purple",
    },
    {
        id: "hr_manager",
        name: "Trưởng phòng Nhân sự",
        description: "Quản lý toàn bộ hồ sơ nhân sự và quy trình tuyển dụng",
        isSystemRole: false,
        userCount: 5,
        theme: "indigo",
    },
    {
        id: "employee",
        name: "Nhân viên",
        description: "Xem thông tin cá nhân và gửi các yêu cầu nghỉ phép, chấm công",
        isSystemRole: true,
        userCount: 120,
        theme: "slate",
    },
];