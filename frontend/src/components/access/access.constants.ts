import type { ModuleDef, RoleTheme, DataScope, Role, Department } from "./access.types";

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

/** Default set of departments, mirroring the reference dashboard. A role can
 *  belong to one department (or "all"), and the "Cây đơn vị tùy chỉnh" data
 *  scope lets a module be limited to a hand-picked subset of these. */
export const DEPARTMENTS: Department[] = [
    { id: "dept_tech", name: "Khối Kỹ thuật & Công nghệ" },
    { id: "dept_fe", name: "Phòng Lập trình Frontend" },
    { id: "dept_be", name: "Phòng Lập trình Backend" },
    { id: "dept_uiux", name: "Nhóm UI/UX & Design System" },
    { id: "dept_devops", name: "Nhóm Cloud & DevOps" },
    { id: "dept_ops", name: "Khối Vận hành & Nhân sự" },
    { id: "dept_hr", name: "Phòng Nhân sự & Tuyển dụng" },
    { id: "dept_admin", name: "Phòng Hành chính & Quản trị" },
    { id: "dept_biz", name: "Khối Kinh doanh & Marketing" },
    { id: "dept_sales", name: "Phòng Phát triển Kinh doanh" },
    { id: "dept_mkt", name: "Phòng Truyền thông & Marketing" },
];


export const MODULES: ModuleDef[] = [
    {
        id: "users",
        name: "Quản lý tài khoản",
        category: "Quản trị",
        defaultScope: "all",
        defaultActions: { view: true, create: true, edit: true, delete: true, approve: false, export: true },
    },
    {
        id: "organization",
        name: "Cơ cấu tổ chức",
        category: "Quản trị",
        defaultScope: "all",
        defaultActions: { view: true, create: true, edit: true, delete: true, approve: true, export: true },
    },
    {
        id: "access",
        name: "Phân quyền truy cập",
        category: "Quản trị",
        defaultScope: "all",
        defaultActions: { view: true, create: true, edit: true, delete: true, approve: true, export: true },
    },
    {
        id: "employees",
        name: "Hồ sơ nhân sự",
        category: "Nhân sự",
        defaultScope: "all",
        defaultActions: { view: true, create: true, edit: true, delete: false, approve: true, export: true },
    },
    {
        id: "projects",
        name: "Quản lý dự án & WBS",
        category: "Dự án",
        defaultScope: "department_managed",
        defaultActions: { view: true, create: true, edit: true, delete: false, approve: true, export: true },
    },
    {
        id: "resources",
        name: "Điều phối & Phân bổ nguồn lực",
        category: "Nguồn lực",
        defaultScope: "department_managed",
        defaultActions: { view: true, create: true, edit: true, delete: false, approve: true, export: true },
    },
    {
        id: "timesheets",
        name: "Chấm công & Bảng công",
        category: "Vận hành",
        defaultScope: "personal",
        defaultActions: { view: true, create: true, edit: true, delete: false, approve: true, export: true },
    },
    {
        id: "leaves",
        name: "Đơn nghỉ phép",
        category: "Vận hành",
        defaultScope: "personal",
        defaultActions: { view: true, create: true, edit: true, delete: false, approve: true, export: true },
    },
    {
        id: "reports",
        name: "Báo cáo & Mô phỏng năng lực",
        category: "Báo cáo",
        defaultScope: "all",
        defaultActions: { view: true, create: false, edit: false, delete: false, approve: false, export: true },
    },
];

export const ROLES: Omit<Role, "permissions">[] = [
    {
        id: "VT-01",
        name: "Ban giám đốc",
        description: "Điều hành toàn diện, theo dõi năng lực và mức độ tải của toàn bộ nhân sự",
        isSystemRole: true,
        userCount: 2,
        theme: "blue",
        departmentId: "all",
    },
    {
        id: "VT-02",
        name: "Quản lý dự án",
        description: "Chịu trách nhiệm các dự án được giao: Tạo WBS, giao việc, đặt ngân sách giờ",
        isSystemRole: true,
        userCount: 6,
        theme: "indigo",
        departmentId: "dept_tech",
    },
    {
        id: "VT-03",
        name: "Quản lý nguồn lực",
        description: "Trưởng bộ phận chuyên môn: Điều phối nhân sự theo tuần, giữ chỗ nguồn lực, duyệt nghỉ phép",
        isSystemRole: true,
        userCount: 8,
        theme: "emerald",
        departmentId: "all",
    },
    {
        id: "VT-04",
        name: "Nhân viên chuyên môn",
        description: "Thực hiện công việc dự án, xem lịch phân bổ cá nhân, nộp chấm công, gửi đơn nghỉ phép",
        isSystemRole: true,
        userCount: 120,
        theme: "slate",
        departmentId: "all",
    },
    {
        id: "VT-05",
        name: "Nhân sự",
        description: "Quản lý hồ sơ nhân sự, hợp đồng lao động, lịch làm việc chuẩn và ngày nghỉ lễ",
        isSystemRole: true,
        userCount: 5,
        theme: "amber",
        departmentId: "dept_hr",
    },
    {
        id: "VT-06",
        name: "Quản trị viên",
        description: "Toàn quyền quản trị kỹ thuật, quản lý tài khoản, cơ cấu tổ chức và phân quyền",
        isSystemRole: true,
        userCount: 3,
        theme: "purple",
        departmentId: "all",
    },
];