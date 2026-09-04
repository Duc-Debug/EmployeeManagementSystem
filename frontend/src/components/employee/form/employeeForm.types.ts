export interface EmployeeFormData {
    id?: string;
    employeeCode?: string;
    fullName: string;
    email: string;
    username?: string;
    password?: string;
    orgUnitId?: string;
    department: string;
    position?: string;
    roleCode?: string;
    roleName?: string;
    dataScope?: "COMPANY" | "ORGANIZATION_BRANCH" | "SELF";
    scopeOrgUnitId?: string;
    phone?: string;
    joinDate?: string;
    startDate?: string;
    contractEndDate?: string;
    standardHoursPerWeek?: number;
    standardHoursPerDay?: number;
    status?: "ACTIVE" | "LOCKED" | "active" | "locked";
    employeeId?: number;
}

export type FormErrors = Partial<Record<keyof EmployeeFormData, string>>;

export interface EmployeeProfileFormProps {
    open: boolean;
    initialData?: EmployeeFormData | null;
    onClose: () => void;
    onSave: (data: EmployeeFormData) => void;
}