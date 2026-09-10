"use client";

import { useState, type FormEvent } from "react";
import {
    X,
    UserCheck,
    Eye,
    EyeOff,
    ShieldCheck,
    User,
    Mail,
    BadgeAlert,
} from "lucide-react";
import { cn } from "../../../lib/utils";
import { OrgUnitCombobox, type OrgUnitOption } from "../../ui/OrgUnitCombobox";
import { formatToDateInput } from "../../../lib/employee-storage";
import { getDefaultDataScopeForRole } from "../../../lib/role-data-scope";
import type { EmployeeFormData } from "./employeeForm.types";
import {
    DEFAULT_ORG_UNIT_OPTIONS,
    ROLE_OPTIONS,
    DATA_SCOPE_OPTIONS,
    DEFAULT_FORM_VALUES,
} from "./employeeForm.constants";
import TaskSelect from "../../task/TaskSelect";

interface EmployeeProfileFormProps {
    open: boolean;
    initialData?: EmployeeFormData | null;
    onClose: () => void;
    onSave: (data: EmployeeFormData) => void;
    nextEmployeeCode?: string;
    orgUnitOptions?: readonly OrgUnitOption[];
    isSubmitting?: boolean;
    apiError?: string | null;
}

export default function EmployeeProfileForm({
    open,
    initialData,
    onClose,
    onSave,
    nextEmployeeCode = "EMP-001",
    orgUnitOptions = DEFAULT_ORG_UNIT_OPTIONS,
    isSubmitting = false,
    apiError = null,
}: EmployeeProfileFormProps) {
    const isEdit = Boolean(initialData);

    const [formData, setFormData] = useState<EmployeeFormData>(() => {
        if (initialData) {
            return {
                ...initialData,
                joinDate: formatToDateInput(initialData.joinDate || initialData.startDate) || "",
                startDate: formatToDateInput(initialData.startDate || initialData.joinDate) || "",
                contractEndDate: formatToDateInput(initialData.contractEndDate) || "",
                standardHoursPerWeek: initialData.standardHoursPerWeek ?? 40,
                status: initialData.status === "locked" ? "LOCKED" : "ACTIVE",
            };
        }
        return {
            ...DEFAULT_FORM_VALUES,
            employeeCode: nextEmployeeCode,
            status: "ACTIVE",
        };
    });

    const [syncOrgScope, setSyncOrgScope] = useState<boolean>(() => {
        if (initialData?.roleCode === "VT-03") {
            return !initialData.scopeOrgUnitId || String(initialData.scopeOrgUnitId) === String(initialData.orgUnitId);
        }
        return true;
    });
    const [prevInitialData, setPrevInitialData] = useState<EmployeeFormData | null | undefined>(initialData);
    const [prevOpen, setPrevOpen] = useState<boolean>(open);
    const [showPassword, setShowPassword] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");

    // Sync when initialData or open changes
    if (open !== prevOpen || initialData !== prevInitialData) {
        setPrevOpen(open);
        setPrevInitialData(initialData);
        setErrorMessage("");
        setShowPassword(false);
        if (initialData) {
            setFormData({
                ...initialData,
                joinDate: formatToDateInput(initialData.joinDate || initialData.startDate) || "",
                startDate: formatToDateInput(initialData.startDate || initialData.joinDate) || "",
                contractEndDate: formatToDateInput(initialData.contractEndDate) || "",
                standardHoursPerWeek: initialData.standardHoursPerWeek ?? 40,
                employeeCode: initialData.employeeCode || initialData.id || nextEmployeeCode,
                status: initialData.status === "locked" ? "LOCKED" : "ACTIVE",
            });
            setSyncOrgScope(!initialData.scopeOrgUnitId || String(initialData.scopeOrgUnitId) === String(initialData.orgUnitId));
        } else {
            setFormData({
                ...DEFAULT_FORM_VALUES,
                employeeCode: nextEmployeeCode,
                status: "ACTIVE",
            });
            setSyncOrgScope(true);
        }
    }

    if (!open) return null;

    const handleRoleChange = (roleCode: string) => {
        const found = ROLE_OPTIONS.find((r) => r.code === roleCode);
        const roleName = found?.name || "";
        const dataScope = getDefaultDataScopeForRole(roleCode);

        if (roleCode === "VT-03") {
            setSyncOrgScope(true);
        }

        setFormData((prev) => ({
            ...prev,
            roleCode,
            roleName,
            ...(dataScope ? { dataScope } : {}),
            scopeOrgUnitId: roleCode === "VT-03" ? prev.orgUnitId : (dataScope === "ORGANIZATION_BRANCH" ? prev.scopeOrgUnitId : ""),
        }));
    };

    const handleOrgUnitChange = (orgUnitId: string) => {
        const selected = orgUnitOptions.find((opt) => String(opt.id) === String(orgUnitId));
        setFormData((prev) => ({
            ...prev,
            orgUnitId,
            department: selected ? selected.unitName : prev.department,
            scopeOrgUnitId: prev.roleCode === "VT-03" && syncOrgScope ? orgUnitId : prev.scopeOrgUnitId,
        }));
    };

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        setErrorMessage("");

        // Validation
        if (!formData.fullName.trim()) {
            setErrorMessage("Vui lòng nhập họ và tên nhân viên.");
            return;
        }
        if (!formData.email.trim()) {
            setErrorMessage("Vui lòng nhập địa chỉ email.");
            return;
        }
        if (!formData.employeeCode?.trim()) {
            setErrorMessage("Vui lòng nhập hoặc để mã nhân viên tự động.");
            return;
        }
        if (!formData.username?.trim()) {
            setErrorMessage("Vui lòng nhập tên đăng nhập.");
            return;
        }
        if (!isEdit && (!formData.password || formData.password.length < 6)) {
            setErrorMessage("Mật khẩu khởi tạo phải có ít nhất 6 ký tự.");
            return;
        }
        if (!formData.orgUnitId) {
            setErrorMessage("Vui lòng chọn đơn vị tổ chức trực thuộc.");
            return;
        }

        let finalScopeOrgUnitId = formData.scopeOrgUnitId;
        if (formData.roleCode === "VT-03") {
            if (syncOrgScope) {
                finalScopeOrgUnitId = formData.orgUnitId;
            } else if (!formData.scopeOrgUnitId) {
                setErrorMessage("Vui lòng chọn đơn vị tổ chức áp dụng cho phạm vi dữ liệu.");
                return;
            }
        } else if (formData.dataScope !== "ORGANIZATION_BRANCH") {
            finalScopeOrgUnitId = "";
        }

        const selectedOrg = orgUnitOptions.find((o) => String(o.id) === String(formData.orgUnitId));

        onSave({
            ...formData,
            fullName: formData.fullName.trim(),
            email: formData.email.trim(),
            employeeCode: formData.employeeCode.trim().toUpperCase(),
            username: formData.username.trim(),
            department: formData.department || selectedOrg?.unitName || "Chưa phân bổ",
            scopeOrgUnitId: finalScopeOrgUnitId,
        });
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="relative flex max-h-[90vh] w-full max-w-2xl flex-col rounded-3xl border border-slate-200/90 bg-white shadow-2xl overflow-hidden">
                {/* Header Modal */}
                <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
                    <div className="flex items-center gap-3">
                        <div className="flex size-10 items-center justify-center rounded-2xl border border-indigo-100 bg-indigo-50 text-indigo-600 shadow-2xs">
                            <UserCheck className="size-5" />
                        </div>
                        <div>
                            <h2 className="text-base font-bold text-slate-900">
                                {isEdit ? "Chỉnh sửa tài khoản nhân viên" : "Tạo tài khoản nhân viên mới"}
                            </h2>
                            <p className="text-xs text-slate-500">
                                {isEdit
                                    ? `Cập nhật thông tin và phân quyền cho ${formData.fullName}`
                                    : "Khai báo thông tin cá nhân, tài khoản đăng nhập và phân quyền hệ thống"}
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-xl p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                        title="Đóng"
                    >
                        <X className="size-4" />
                    </button>
                </div>

                {/* Form Content - Scrollable */}
                <form
                    id="employee-form"
                    onSubmit={handleSubmit}
                    className="flex-1 overflow-y-auto px-6 py-4 space-y-6 [scrollbar-width:thin] [scrollbar-color:#cbd5e1_transparent] [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-thumb]:bg-slate-200 [&::-webkit-scrollbar-thumb]:rounded-full"
                >
                    {(errorMessage || apiError) && (
                        <div className="flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2.5 text-xs font-semibold text-rose-700">
                            <BadgeAlert className="size-4 shrink-0 text-rose-600" />
                            <span>{errorMessage || apiError}</span>
                        </div>
                    )}

                    {/* KHỐI 1: THÔNG TIN CÁ NHÂN & TÀI KHOẢN */}
                    <div className="space-y-3.5">
                        <div className="flex items-center gap-2 border-b border-slate-100 pb-2 text-xs font-bold uppercase tracking-wider text-slate-500">
                            <User className="size-4 text-indigo-600" />
                            <span>1. Thông tin cá nhân & Tài khoản</span>
                        </div>

                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            {/* Họ và tên */}
                            <div className="space-y-1.5">
                                <div className="flex items-center justify-between">
                                    <label className="text-xs font-semibold text-slate-700">Họ và tên *</label>
                                </div>
                                <input
                                    type="text"
                                    required
                                    placeholder="VD: Nguyễn Văn A"
                                    value={formData.fullName}
                                    onChange={(e) => {
                                        const name = e.target.value;
                                        setFormData((prev) => ({
                                            ...prev,
                                            fullName: name,
                                            // Gợi ý username nếu đang tạo mới và chưa nhập username
                                            username: !isEdit && !prev.username
                                                ? name.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]/g, "").slice(0, 15)
                                                : prev.username,
                                        }));
                                    }}
                                    className="w-full rounded-xl border border-slate-200 bg-slate-50/70 px-3.5 py-2 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                                />
                            </div>

                            {/* Email */}
                            <div className="space-y-1.5">
                                <div className="flex items-center justify-between">
                                    <label className="text-xs font-semibold text-slate-700">Email *</label>
                                    {isEdit && <span className="text-[10px] text-slate-400 font-medium">(Cố định theo tài khoản)</span>}
                                </div>
                                <div className="relative">
                                    <input
                                        type="email"
                                        required
                                        disabled={isEdit}
                                        placeholder="hung@company.com"
                                        value={formData.email}
                                        onChange={(e) => {
                                            if (isEdit) return;
                                            setFormData({ ...formData, email: e.target.value });
                                        }}
                                        className={cn(
                                            "w-full rounded-xl border px-3.5 py-2 text-xs font-semibold outline-none transition",
                                            isEdit
                                                ? "border-slate-200 bg-slate-100 text-slate-500 cursor-not-allowed"
                                                : "border-slate-200 bg-slate-50/70 text-slate-800 placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                                        )}
                                    />
                                    <Mail className="pointer-events-none absolute right-3.5 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                                </div>
                            </div>
                        </div>

                        {/* Hàng 2: Mã nhân viên */}
                        <div className="space-y-1.5">
                            <div className="flex items-center justify-between">
                                <label className="text-xs font-semibold text-slate-700">Mã nhân viên *</label>
                                <span className="text-[10px] text-slate-400 font-medium">
                                    {isEdit ? "(Cố định)" : "(Tự sinh hoặc tự nhập)"}
                                </span>
                            </div>
                            <input
                                type="text"
                                required
                                disabled={isEdit}
                                placeholder="VD: EMP-001"
                                value={formData.employeeCode || ""}
                                onChange={(e) => {
                                    if (isEdit) return;
                                    setFormData({ ...formData, employeeCode: e.target.value });
                                }}
                                className={cn(
                                    "w-full rounded-xl border px-3.5 py-2 text-xs font-bold uppercase outline-none transition",
                                    isEdit
                                        ? "border-slate-200 bg-slate-100 text-slate-500 cursor-not-allowed"
                                        : "border-slate-200 bg-slate-50/70 text-slate-800 placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                                )}
                            />
                        </div>

                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            {/* Tên đăng nhập */}
                            <div className={cn("space-y-1.5", isEdit && "sm:col-span-2")}>
                                <div className="flex items-center justify-between">
                                    <label className="text-xs font-semibold text-slate-700">Tên đăng nhập *</label>
                                    {isEdit && <span className="text-[10px] text-slate-400">(Không thể đổi)</span>}
                                </div>
                                <input
                                    type="text"
                                    required
                                    disabled={isEdit}
                                    placeholder="VD: hung.cv"
                                    value={formData.username || ""}
                                    onChange={(e) => setFormData({ ...formData, username: e.target.value.toLowerCase() })}
                                    className={cn(
                                        "w-full rounded-xl border px-3.5 py-2 text-xs font-semibold outline-none transition",
                                        isEdit
                                            ? "border-slate-200 bg-slate-100 text-slate-500 cursor-not-allowed"
                                            : "border-slate-200 bg-slate-50/70 text-slate-800 focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                                    )}
                                />
                            </div>

                            {/* Mật khẩu khởi tạo (Chỉ hiển thị khi tạo mới) */}
                            {!isEdit && (
                                <div className="space-y-1.5">
                                    <div className="flex items-center justify-between">
                                        <label className="text-xs font-semibold text-slate-700">
                                            Mật khẩu khởi tạo *
                                        </label>
                                    </div>
                                    <div className="relative">
                                        <input
                                            type={showPassword ? "text" : "password"}
                                            required
                                            placeholder="Tối thiểu 6 ký tự"
                                            value={formData.password || ""}
                                            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                                            className="w-full rounded-xl border border-slate-200 bg-slate-50/70 py-2 pl-3.5 pr-10 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                                        />
                                        <button
                                            type="button"
                                            onClick={() => setShowPassword((p) => !p)}
                                            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition"
                                            tabIndex={-1}
                                        >
                                            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Đơn vị tổ chức trực thuộc (CÂY COMBOBOX) */}
                        <div className="space-y-1.5">
                            <div className="flex items-center justify-between">
                                <label className="text-xs font-semibold text-slate-700">
                                    Đơn vị tổ chức trực thuộc *
                                </label>
                            </div>
                            <OrgUnitCombobox
                                id="employee-org-unit"
                                options={orgUnitOptions}
                                value={formData.orgUnitId || ""}
                                onChange={handleOrgUnitChange}
                                placeholder="Chọn phòng ban / đơn vị (dạng cây)..."
                                disallowRoot={true}
                            />
                        </div>
                    </div>

                    {/* KHỐI 2: PHÂN QUYỀN & PHẠM VI DỮ LIỆU */}
                    <div className="space-y-3.5 pt-2">
                        <div className="flex items-center gap-2 border-b border-slate-100 pb-2 text-xs font-bold uppercase tracking-wider text-slate-500">
                            <ShieldCheck className="size-4 text-indigo-600" />
                            <span>2. Phân quyền & Phạm vi dữ liệu</span>
                        </div>

                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            {/* Vai trò */}
                            <div className="space-y-1.5">
                                <label className="text-xs font-semibold text-slate-700">Vai trò (Role) *</label>
                                <TaskSelect
                                    value={formData.roleCode || "VT-04"}
                                    options={ROLE_OPTIONS.map((r) => ({ id: r.code, label: `${r.code} · ${r.name}` }))}
                                    onChange={(val) => handleRoleChange(val)}
                                    placeholder="Chọn vai trò..."
                                    hideSearch={true}
                                    buttonClassName="bg-slate-50/70 border-slate-200 py-2 rounded-xl"
                                />
                            </div>

                            {/* Phạm vi dữ liệu */}
                            <div className="space-y-1.5">
                                <div className="flex items-center justify-between">
                                    <label className="text-xs font-semibold text-slate-700">
                                        Phạm vi dữ liệu *
                                    </label>
                                    <span className="text-[10px] text-slate-400 font-medium">(Tự động theo vai trò)</span>
                                </div>
                                <TaskSelect
                                    disabled={true}
                                    value={formData.dataScope || "SELF"}
                                    options={DATA_SCOPE_OPTIONS.map((s) => ({ id: s.value, label: s.label }))}
                                    onChange={() => {}}
                                    placeholder="Chọn phạm vi..."
                                    hideSearch={true}
                                    buttonClassName="bg-slate-100 border-slate-200 py-2 rounded-xl text-slate-500 cursor-not-allowed"
                                />
                            </div>
                        </div>

                        {formData.roleCode === "VT-03" && (
                            <div className="pt-1">
                                <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer">
                                    <input
                                        type="checkbox"
                                        checked={syncOrgScope}
                                        onChange={(e) => {
                                            const checked = e.target.checked;
                                            setSyncOrgScope(checked);
                                            if (checked) {
                                                setFormData((prev) => ({ ...prev, scopeOrgUnitId: prev.orgUnitId }));
                                            }
                                        }}
                                        className="rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                                    />
                                    <span>Áp dụng phạm vi quản lý theo đơn vị trực thuộc</span>
                                </label>
                            </div>
                        )}

                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            {/* Trạng thái hoạt động */}
                            <div className="space-y-1.5">
                                <label className="text-xs font-semibold text-slate-700">Trạng thái tài khoản *</label>
                                <TaskSelect
                                    value={formData.status === "LOCKED" || formData.status === "locked" ? "LOCKED" : "ACTIVE"}
                                    options={[
                                        { id: "ACTIVE", label: "Hoạt động (ACTIVE)" },
                                        { id: "LOCKED", label: "Đã khóa (LOCKED)" },
                                    ]}
                                    onChange={(val) =>
                                        setFormData({
                                            ...formData,
                                            status: val as "ACTIVE" | "LOCKED",
                                        })
                                    }
                                    hideSearch={true}
                                    buttonClassName="bg-slate-50/70 border-slate-200 py-2 rounded-xl"
                                />
                            </div>

                            {/* Nếu chọn Theo đơn vị & bỏ tích đồng bộ: ComboBox chọn đơn vị áp dụng */}
                            {formData.dataScope === "ORGANIZATION_BRANCH" && !syncOrgScope && (
                                <div className="space-y-1.5 animate-in fade-in duration-150">
                                    <label className="text-xs font-semibold text-indigo-700">
                                        Đơn vị tổ chức áp dụng (Phân cấp) *
                                    </label>
                                    <OrgUnitCombobox
                                        id="employee-scope-org-unit"
                                        options={orgUnitOptions}
                                        value={formData.scopeOrgUnitId || ""}
                                        onChange={(val: string) => setFormData({ ...formData, scopeOrgUnitId: val })}
                                        placeholder="Chọn đơn vị áp dụng..."
                                    />
                                </div>
                            )}
                        </div>
                    </div>
                </form>

                {/* Footer Actions */}
                <div className="flex items-center justify-end gap-3 border-t border-slate-100 bg-slate-50/50 px-6 py-3.5">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isSubmitting}
                        className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-600 shadow-xs transition hover:bg-slate-50 hover:text-slate-800 disabled:opacity-50"
                    >
                        Hủy
                    </button>
                    <button
                        type="submit"
                        form="employee-form"
                        disabled={isSubmitting}
                        className="rounded-xl border border-indigo-600 bg-indigo-600 px-5 py-2 text-xs font-semibold text-white shadow-xs transition hover:bg-indigo-700 active:scale-95 disabled:opacity-50"
                    >
                        {isSubmitting ? "Đang xử lý..." : isEdit ? "Lưu thay đổi" : "Tạo tài khoản"}
                    </button>
                </div>
            </div>
        </div>
    );
}
