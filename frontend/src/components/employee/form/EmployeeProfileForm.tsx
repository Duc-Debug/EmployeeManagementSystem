import { useState, useEffect, useRef } from "react";
import { X, AlertCircle, ChevronDown, Check, Lock, Unlock } from "lucide-react";
import type { EmployeeProfileFormProps, EmployeeFormData, FormErrors } from "./employeeForm.types";
import { DEFAULT_FORM_VALUES, DEPARTMENT_OPTIONS } from "./employeeForm.constants";
import { validateEmployeeForm } from "./employeeForm.validation";

function DepartmentSelect({
                              value,
                              onChange,
                          }: {
    value: string;
    onChange: (v: string) => void;
}) {
    const [open, setOpen] = useState(false);
    const ref = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (ref.current && !ref.current.contains(e.target as Node)) {
                setOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <div className="relative" ref={ref}>
            <button
                type="button"
                onClick={() => setOpen((o) => !o)}
                className="flex w-full items-center justify-between rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20"
            >
                <span>{value || "Chọn phòng ban"}</span>
                <ChevronDown
                    className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${
                        open ? "rotate-180" : ""
                    }`}
                />
            </button>

            <div
                className={`absolute left-0 right-0 z-30 mt-1.5 origin-top rounded-xl border border-slate-200 bg-white p-1 shadow-lg transition-all duration-150 ease-out ${
                    open
                        ? "scale-100 opacity-100 pointer-events-auto"
                        : "scale-95 opacity-0 pointer-events-none"
                }`}
            >
                {DEPARTMENT_OPTIONS.map((dept) => {
                    const isActive = value === dept;
                    return (
                        <button
                            key={dept}
                            type="button"
                            onClick={() => {
                                onChange(dept);
                                setOpen(false);
                            }}
                            className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-sm transition ${
                                isActive
                                    ? "bg-indigo-50 font-semibold text-indigo-600"
                                    : "text-slate-600 hover:bg-slate-50"
                            }`}
                        >
                            {dept}
                            {isActive && <Check className="h-3.5 w-3.5" />}
                        </button>
                    );
                })}
            </div>
        </div>
    );
}

export default function EmployeeProfileForm({
                                                open,
                                                initialData,
                                                onClose,
                                                onSave,
                                            }: EmployeeProfileFormProps) {
    const [formData, setFormData] = useState<EmployeeFormData>(DEFAULT_FORM_VALUES);
    const [errors, setErrors] = useState<FormErrors>({});

    useEffect(() => {
        if (initialData) {
            setFormData({ status: "active", ...initialData });
        } else {
            setFormData(DEFAULT_FORM_VALUES);
        }
        setErrors({});
    }, [initialData, open]);

    if (!open) return null;

    const isLocked = formData.status === "locked";

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        const validationErrors = validateEmployeeForm(formData);

        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors);
            return;
        }

        onSave(formData);
    };

    const toggleLock = () => {
        setFormData((prev) => ({
            ...prev,
            status: prev.status === "locked" ? "active" : "locked",
        }));
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            {/* Backdrop tối nhẹ mờ nền */}
            <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={onClose} />

            {/* Modal Container Light Mode */}
            <div className="relative w-full max-w-lg rounded-2xl bg-white p-6 text-slate-800 shadow-2xl">
                {/* Header Modal */}
                <div className="mb-5 flex items-center justify-between">
                    <h3 className="text-base font-bold text-slate-900">
                        {initialData ? "Chỉnh sửa hồ sơ nhân sự" : "Khai báo nhân sự mới"}
                    </h3>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                {/* Trạng thái tài khoản - chỉ hiện khi chỉnh sửa */}
                {initialData && (
                    <div
                        className={`mb-4 flex items-center justify-between rounded-xl border px-3.5 py-2.5 transition ${
                            isLocked
                                ? "border-rose-200 bg-rose-50"
                                : "border-emerald-200 bg-emerald-50"
                        }`}
                    >
                        <div className="flex items-center gap-2">
                            {isLocked ? (
                                <Lock className="h-4 w-4 text-rose-500" />
                            ) : (
                                <Unlock className="h-4 w-4 text-emerald-500" />
                            )}
                            <span
                                className={`text-xs font-semibold ${
                                    isLocked ? "text-rose-600" : "text-emerald-600"
                                }`}
                            >
                                {isLocked ? "Tài khoản đã bị khóa" : "Tài khoản đang hoạt động"}
                            </span>
                        </div>
                        <button
                            type="button"
                            onClick={toggleLock}
                            className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${
                                isLocked
                                    ? "bg-emerald-500 text-white hover:bg-emerald-600"
                                    : "bg-rose-500 text-white hover:bg-rose-600"
                            }`}
                        >
                            {isLocked ? "Mở khóa" : "Khóa tài khoản"}
                        </button>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    {/* Họ và tên */}
                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">Họ và tên</label>
                        <input
                            type="text"
                            value={formData.fullName}
                            onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                            placeholder="VD: Nguyễn Văn A"
                            className={`w-full rounded-xl border bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20 ${
                                errors.fullName ? "border-rose-500" : "border-slate-200"
                            }`}
                        />
                        {errors.fullName && (
                            <p className="mt-1 flex items-center gap-1 text-xs text-rose-500">
                                <AlertCircle className="h-3 w-3" /> {errors.fullName}
                            </p>
                        )}
                    </div>

                    {/* Email & Phòng ban */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="mb-1.5 block text-xs font-semibold text-slate-600">Email</label>
                            <input
                                type="email"
                                value={formData.email}
                                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                                placeholder="a.nguyen@company.com"
                                className={`w-full rounded-xl border bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20 ${
                                    errors.email ? "border-rose-500" : "border-slate-200"
                                }`}
                            />
                            {errors.email && (
                                <p className="mt-1 flex items-center gap-1 text-xs text-rose-500">
                                    <AlertCircle className="h-3 w-3" /> {errors.email}
                                </p>
                            )}
                        </div>

                        <div>
                            <label className="mb-1.5 block text-xs font-semibold text-slate-600">Phòng ban</label>
                            <DepartmentSelect
                                value={formData.department}
                                onChange={(dept) => setFormData({ ...formData, department: dept })}
                            />
                        </div>
                    </div>

                    {/* Chức danh */}
                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">Chức danh</label>
                        <input
                            type="text"
                            value={formData.position}
                            onChange={(e) => setFormData({ ...formData, position: e.target.value })}
                            placeholder="VD: Backend Developer"
                            className={`w-full rounded-xl border bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20 ${
                                errors.position ? "border-rose-500" : "border-slate-200"
                            }`}
                        />
                        {errors.position && (
                            <p className="mt-1 flex items-center gap-1 text-xs text-rose-500">
                                <AlertCircle className="h-3 w-3" /> {errors.position}
                            </p>
                        )}
                    </div>

                    {/* Action Buttons */}
                    <div className="flex justify-end gap-2 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-xl border border-slate-200 bg-white px-5 py-2 text-xs font-semibold text-slate-600 transition hover:bg-slate-50 hover:text-slate-800"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            className="rounded-xl border border-slate-200 bg-white px-5 py-2 text-xs font-semibold text-slate-800 transition hover:bg-slate-50 hover:text-slate-900"
                        >
                            {initialData ? "Lưu thay đổi" : "Thêm hồ sơ"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}