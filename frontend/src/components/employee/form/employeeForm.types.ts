export interface EmployeeFormData {
    id?: string;
    fullName: string;
    email: string;
    phone?: string;
    department: string;
    position: string;
    joinDate?: string;
    standardHoursPerDay?: number;
    status?: "active" | "locked";
}

export type FormErrors = Partial<Record<keyof EmployeeFormData, string>>;

export interface EmployeeProfileFormProps {
    open: boolean;
    initialData?: EmployeeFormData | null;
    onClose: () => void;
    onSave: (data: EmployeeFormData) => void;
}