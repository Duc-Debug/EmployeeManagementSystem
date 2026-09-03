import type { EmployeeFormData } from "./employeeForm.types";
import type { OrgUnitOption } from "@/components/ui/OrgUnitCombobox";

export const DEFAULT_ORG_UNIT_OPTIONS: readonly OrgUnitOption[] = [
    { id: 1, unitCode: "CORP", unitName: "Tập đoàn Doanh nghiệp", unitType: "COMPANY", depth: 0 },
    { id: 2, unitCode: "TECH", unitName: "Khối Kỹ thuật & Công nghệ", unitType: "CENTER", depth: 1 },
    { id: 3, unitCode: "TECH-FE", unitName: "Phòng Lập trình Frontend", unitType: "DEPARTMENT", depth: 2 },
    { id: 4, unitCode: "TECH-BE", unitName: "Phòng Lập trình Backend", unitType: "DEPARTMENT", depth: 2 },
    { id: 5, unitCode: "TECH-QA", unitName: "Phòng Đảm bảo chất lượng (QA/QC)", unitType: "DEPARTMENT", depth: 2 },
    { id: 6, unitCode: "TECH-DEVOPS", unitName: "Tổ Hạ tầng & DevOps", unitType: "TEAM", depth: 3 },
    { id: 7, unitCode: "PROD", unitName: "Khối Sản phẩm & Thiết kế", unitType: "CENTER", depth: 1 },
    { id: 8, unitCode: "PROD-UIUX", unitName: "Phòng Thiết kế UI/UX", unitType: "DEPARTMENT", depth: 2 },
    { id: 9, unitCode: "PROD-PO", unitName: "Nhóm Quản trị sản phẩm (PO)", unitType: "TEAM", depth: 3 },
    { id: 10, unitCode: "BIZ", unitName: "Khối Kinh doanh & Tiếp thị", unitType: "CENTER", depth: 1 },
    { id: 11, unitCode: "BIZ-MKT", unitName: "Phòng Marketing & Truyền thông", unitType: "DEPARTMENT", depth: 2 },
    { id: 12, unitCode: "BIZ-SALES", unitName: "Phòng Phát triển thị trường", unitType: "DEPARTMENT", depth: 2 },
    { id: 13, unitCode: "OPS", unitName: "Khối Vận hành & Hành chính Nhân sự", unitType: "CENTER", depth: 1 },
    { id: 14, unitCode: "OPS-HR", unitName: "Phòng Quản trị nhân sự (HR)", unitType: "DEPARTMENT", depth: 2 },
    { id: 15, unitCode: "OPS-ACC", unitName: "Phòng Kế toán & Tài chính", unitType: "DEPARTMENT", depth: 2 },
];

export const ROLE_OPTIONS = [
    { code: "VT-01", name: "Ban giám đốc" },
    { code: "VT-02", name: "Quản lý dự án" },
    { code: "VT-03", name: "Quản lý nguồn lực" },
    { code: "VT-04", name: "Nhân viên chuyên môn" },
    { code: "VT-05", name: "Nhân sự (HR)" },
    { code: "VT-06", name: "Quản trị hệ thống (Admin)" },
];

export const DATA_SCOPE_OPTIONS = [
    { label: "Toàn công ty", value: "COMPANY" },
    { label: "Theo đơn vị", value: "ORGANIZATION_BRANCH" },
    { label: "Cá nhân", value: "SELF" },
] as const;

export const DEPARTMENT_OPTIONS = [
    "All",
    "Tập đoàn Doanh nghiệp",
    "Khối Kỹ thuật & Công nghệ",
    "Phòng Lập trình Frontend",
    "Phòng Lập trình Backend",
    "Khối Sản phẩm & Thiết kế",
    "Khối Kinh doanh & Tiếp thị",
    "Khối Vận hành & Hành chính Nhân sự",
    "Phòng Quản trị nhân sự (HR)",
];

export const DEFAULT_FORM_VALUES: EmployeeFormData = {
    fullName: "",
    email: "",
    employeeCode: "",
    username: "",
    password: "",
    orgUnitId: "",
    department: "",
    roleName: "Nhân viên chuyên môn",
    dataScope: "SELF",
    status: "ACTIVE",
};