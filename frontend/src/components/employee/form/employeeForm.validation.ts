import type { EmployeeFormData, FormErrors } from "./employeeForm.types";

export function validateEmployeeForm(data: EmployeeFormData): FormErrors {
    const errors: FormErrors = {};

    if (!data.fullName.trim()) {
        errors.fullName = "Họ và tên không được để trống";
    }

    if (!data.email.trim() || !/\S+@\S+\.\S+/.test(data.email)) {
        errors.email = "Email không đúng định dạng";
    }

    if (!data.position.trim()) {
        errors.position = "Vui lòng nhập chức danh";
    }

    return errors;
}