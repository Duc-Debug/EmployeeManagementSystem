export interface EmployeeFormData {
    id?: string;
    fullName: string;
    email: string;
    department: string;
    position: string;
    status?: "active" | "locked";
}

export type FormErrors = Partial<Record<keyof EmployeeFormData, string>>;

export interface EmployeeProfileFormProps {
    open: boolean;
    initialData?: EmployeeFormData | null;
    onClose: () => void;
    onSave: (data: EmployeeFormData) => void;
}