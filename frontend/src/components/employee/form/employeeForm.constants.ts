import type { EmployeeFormData } from "./employeeForm.types";

export const DEPARTMENT_OPTIONS = [
    "Kỹ thuật",
    "Nhân sự",
    "Kinh doanh",
    "Marketing",
    "Tài chính",
];

export const DEFAULT_FORM_VALUES: EmployeeFormData = {
    fullName: "",
    email: "",
    department: "Kỹ thuật",
    position: "",
};