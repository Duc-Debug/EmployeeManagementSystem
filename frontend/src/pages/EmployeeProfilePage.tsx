"use client";

import { useState, useRef, useEffect } from "react";
import { Plus, Search, ChevronDown, Check, AlertTriangle, X, User, Trash2, Users, RefreshCw } from "lucide-react";
import { cn } from "@/lib/utils";
import EmployeeCard from "../components/employee/EmployeeCard";
import EmployeeProfileForm from "../components/employee/form/EmployeeProfileForm";
import EmployeeDetailModal from "../components/employee/form/EmployeeDetailModal";
import type { EmployeeFormData } from "../components/employee/form/employeeForm.types";
import { DEFAULT_ORG_UNIT_OPTIONS, DEPARTMENT_OPTIONS } from "../components/employee/form/employeeForm.constants";
import { getUsers, createUser, updateUserRole, toggleUserStatus } from "@/lib/api/users";
import {
    getEmployeeProfile,
    getEmployeeProfileByUserId,
    updateEmployeeProfile,
    createEmployeeProfile,
    type EmployeeProfile,
} from "@/lib/api/employees";
import {
    getStoredPhone,
    saveStoredPhone,
    getStoredDates,
    saveStoredDates,
    formatToDateInput,
} from "@/lib/employee-storage";
import { getOrgTree } from "@/lib/api/org-units";
import { flattenOrgTree } from "@/lib/organization";
import type { OrgUnitOption } from "@/components/ui/OrgUnitCombobox";
import type { User as BackendUser, RoleCode, DataScope } from "@/types/hrm";

function formatDeptLabel(dept: string) {
    if (dept === "All") return "Tất cả phòng ban";
    return dept;
}

/* ========================================================================
   CUSTOM DROPDOWN COMPONENT
   ======================================================================== */
export function CustomSelectDropdown({
    value,
    onChange,
    options,
    placeholder = "Chọn phòng ban",
    labelPrefix = false,
}: {
    value: string;
    onChange: (v: string) => void;
    options: readonly string[];
    placeholder?: string;
    labelPrefix?: boolean;
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

    const getDisplayLabel = (val: string) => {
        if (!val) return placeholder;
        return labelPrefix ? formatDeptLabel(val) : val === "All" ? "Tất cả phòng ban" : val;
    };

    return (
        <div className="relative inline-block" ref={ref}>
            <button
                type="button"
                onClick={() => setOpen((o) => !o)}
                className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-xs font-semibold text-slate-700 shadow-xs transition hover:bg-slate-50 hover:border-slate-300 active:scale-95"
            >
                <span>{getDisplayLabel(value)}</span>
                <ChevronDown className={cn("size-3.5 shrink-0 text-slate-400 transition-transform duration-200", open && "rotate-180")} />
            </button>

            {open && (
                <div className="absolute right-0 z-50 mt-2 min-w-[200px] overflow-hidden rounded-2xl border border-slate-200 bg-white p-1.5 shadow-xl max-w-xs sm:w-64 animate-in fade-in zoom-in-95 duration-150">
                    <div className="max-h-60 overflow-y-auto space-y-1 custom-scrollbar">
                        {options.map((opt) => {
                            const isActive = value === opt;
                            return (
                                <button
                                    key={opt}
                                    type="button"
                                    onClick={() => {
                                        onChange(opt);
                                        setOpen(false);
                                    }}
                                    className={cn(
                                        "flex w-full items-center justify-between rounded-xl px-3.5 py-2 text-left text-xs font-semibold transition-all",
                                        isActive
                                            ? "bg-indigo-50 text-indigo-700 font-bold shadow-2xs"
                                            : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
                                    )}
                                >
                                    <span className="truncate">{labelPrefix ? formatDeptLabel(opt) : opt === "All" ? "Tất cả phòng ban" : opt}</span>
                                    {isActive && <Check className="size-3.5 shrink-0 text-indigo-600 stroke-[2.5]" />}
                                </button>
                            );
                        })}
                    </div>
                </div>
            )}
        </div>
    );
}

/* ========================================================================
   DELETE CONFIRMATION DIALOG (Thay thế window.confirm)
   ======================================================================== */
function DeleteConfirmDialog({
    target,
    onClose,
    onConfirm,
    isSubmitting = false,
    errorMessage = null,
}: {
    target: EmployeeFormData | null;
    onClose: () => void;
    onConfirm: () => void;
    isSubmitting?: boolean;
    errorMessage?: string | null;
}) {
    if (!target) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm animate-in fade-in duration-150">
            <div className="relative w-full max-w-md rounded-3xl border border-slate-200/90 bg-white p-6 shadow-2xl text-slate-800">
                {/* Header with Warning Icon */}
                <div className="flex items-start gap-4">
                    <div className="flex size-11 shrink-0 items-center justify-center rounded-2xl border border-rose-200 bg-rose-50 text-rose-600 shadow-2xs">
                        <AlertTriangle className="size-6" />
                    </div>
                    <div className="min-w-0 flex-1">
                        <h3 className="text-base font-bold text-slate-900">
                            Xác nhận xóa hồ sơ nhân viên
                        </h3>
                        <p className="mt-1 text-xs text-slate-500 leading-relaxed">
                            Hành động này sẽ xóa thông tin nhân sự và thu hồi toàn bộ quyền đăng nhập của tài khoản này khỏi hệ thống.
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        disabled={isSubmitting}
                        className="rounded-xl p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition disabled:opacity-50"
                    >
                        <X className="size-4" />
                    </button>
                </div>

                {/* Target Employee Summary Card */}
                <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50/80 p-3.5">
                    <div className="flex items-center gap-3">
                        <div className="flex size-9 items-center justify-center rounded-xl bg-white border border-slate-200 font-bold text-indigo-600 text-xs shadow-2xs">
                            <User className="size-4.5" />
                        </div>
                        <div className="min-w-0 flex-1">
                            <p className="truncate text-xs font-bold text-slate-900">
                                {target.fullName}
                            </p>
                            <p className="text-[11px] text-slate-500 font-mono">
                                {target.employeeCode || target.id} · @{target.username || "user"}
                            </p>
                        </div>
                    </div>
                    <div className="mt-2.5 pt-2 border-t border-slate-200/70 text-[11px] text-slate-600 flex items-center justify-between">
                        <span>Đơn vị:</span>
                        <span className="font-semibold text-slate-800 truncate max-w-[200px]">{target.department}</span>
                    </div>
                </div>

                {/* Hiển thị lỗi nếu API xóa thất bại */}
                {errorMessage && (
                    <div className="mt-3 flex items-start gap-2.5 rounded-2xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
                        <AlertTriangle className="size-4 shrink-0 mt-0.5 text-rose-600" />
                        <div className="flex-1">
                            <p className="font-bold">Không thể xóa nhân viên</p>
                            <p className="text-[11px] text-rose-600 mt-0.5 leading-relaxed">{errorMessage}</p>
                        </div>
                    </div>
                )}

                {/* Action Buttons */}
                <div className="mt-6 flex items-center justify-end gap-3">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isSubmitting}
                        className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 shadow-xs transition hover:bg-slate-50 disabled:opacity-50"
                    >
                        Hủy
                    </button>
                    <button
                        type="button"
                        onClick={onConfirm}
                        disabled={isSubmitting}
                        className="flex items-center gap-1.5 rounded-xl border border-rose-600 bg-rose-600 px-4 py-2 text-xs font-semibold text-white shadow-xs transition hover:bg-rose-700 active:scale-95 disabled:opacity-50"
                    >
                        <Trash2 className="size-3.5" />
                        <span>{isSubmitting ? "Đang xử lý..." : "Xác nhận xóa"}</span>
                    </button>
                </div>
            </div>
        </div>
    );
}

export default function EmployeeProfilePage() {
    const [employees, setEmployees] = useState<EmployeeFormData[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState("");
    const [selectedDept, setSelectedDept] = useState("All");

    const [isFormOpen, setIsFormOpen] = useState(false);
    const [editingEmployee, setEditingEmployee] = useState<EmployeeFormData | undefined>(undefined);
    const [viewingEmployee, setViewingEmployee] = useState<EmployeeFormData | undefined>(undefined);
    const [deleteTarget, setDeleteTarget] = useState<EmployeeFormData | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState<string | null>(null);
    const [isSaving, setIsSaving] = useState(false);
    const [formError, setFormError] = useState<string | null>(null);
    const [actionNotification, setActionNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);
    const [orgUnitOptions, setOrgUnitOptions] = useState<readonly OrgUnitOption[]>(DEFAULT_ORG_UNIT_OPTIONS);

    // Fetch users và OrgUnits từ Backend API khi trang được tải hoặc khi bấm thử lại
    const loadData = async () => {
        setIsLoading(true);
        setLoadError(null);
        try {
            const [userRes, treeRes] = await Promise.allSettled([
                getUsers(0, 100),
                getOrgTree(),
            ]);

            if (treeRes.status === "fulfilled" && treeRes.value && treeRes.value.length > 0) {
                const flat = flattenOrgTree(treeRes.value);
                if (flat.length > 0) {
                    const dynamicOptions: OrgUnitOption[] = flat.map((u) => ({
                        id: u.id,
                        unitCode: u.unitCode,
                        unitName: u.unitName,
                        unitType: u.unitType,
                        depth: u.level ?? 0,
                    }));
                    setOrgUnitOptions(dynamicOptions);
                }
            }

            if (userRes.status === "fulfilled") {
                const users = userRes.value?.content || [];
                const mapped: EmployeeFormData[] = users.map((u: BackendUser) => {
                    const empCode = u.employeeId ? `EMP-${String(u.employeeId).padStart(3, "0")}` : `EMP-${u.id}`;
                    const phone = getStoredPhone(u.id) || (u.employeeId ? getStoredPhone(u.employeeId) : undefined) || getStoredPhone(empCode);
                    const dates = getStoredDates(u.id) || (u.employeeId ? getStoredDates(u.employeeId) : undefined) || getStoredDates(empCode);
                    return {
                        id: String(u.id),
                        employeeId: u.employeeId ?? undefined,
                        employeeCode: empCode,
                        fullName: u.fullName || u.username,
                        username: u.username,
                        email: u.email || "",
                        phone: phone || undefined,
                        orgUnitId: u.orgUnitId ? String(u.orgUnitId) : undefined,
                        department: u.orgUnitName || "Chưa phân bổ",
                        position: u.roleName || "Nhân viên",
                        roleCode: u.roleCode,
                        roleName: u.roleName,
                        dataScope: u.dataScope,
                        scopeOrgUnitId: u.scopeOrgUnitId ? String(u.scopeOrgUnitId) : undefined,
                        status: u.status,
                        standardHoursPerWeek: 40,
                        joinDate: dates?.joinDate,
                        startDate: dates?.joinDate,
                        contractEndDate: dates?.contractEndDate,
                    };
                });
                setEmployees(mapped);
            } else {
                const reason = userRes.reason;
                const errMsg = reason?.message || "Không thể kết nối đến máy chủ Backend để tải danh sách nhân sự.";
                setLoadError(errMsg);
                setEmployees([]);
            }
        } catch (err: any) {
            setLoadError(err?.message || "Đã xảy ra lỗi khi tải dữ liệu nhân sự.");
            setEmployees([]);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    const nextEmployeeCode = `EMP-${String(employees.length + 1).padStart(3, "0")}`;

    const filteredEmployees = employees.filter((emp) => {
        const matchesSearch =
            emp.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
            (emp.email && emp.email.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (emp.employeeCode && emp.employeeCode.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (emp.username && emp.username.toLowerCase().includes(searchTerm.toLowerCase()));
        const matchesDept = selectedDept === "All" || emp.department === selectedDept;
        return matchesSearch && matchesDept;
    });

    const handleOpenAdd = () => {
        setEditingEmployee(undefined);
        setFormError(null);
        setIsFormOpen(true);
    };

    const handleOpenEdit = async (emp: EmployeeFormData) => {
        setEditingEmployee(emp);
        setFormError(null);
        setIsFormOpen(true);

        const numId = typeof emp.id === "number" ? emp.id : parseInt(String(emp.id).replace(/\D/g, ""), 10);
        try {
            let profile: EmployeeProfile | null = null;
            if (emp.employeeId) {
                try {
                    profile = await getEmployeeProfile(emp.employeeId);
                } catch {
                    // fallback
                }
            }
            if (!profile && !isNaN(numId)) {
                try {
                    profile = await getEmployeeProfileByUserId(numId);
                } catch {
                    profile = null;
                }
            }
            if (profile) {
                setEditingEmployee((prev) => {
                    if (!prev || (prev.id !== emp.id && prev.employeeCode !== emp.employeeCode)) return prev;
                    return {
                        ...prev,
                        fullName: profile.fullName || prev.fullName,
                        orgUnitId: String(profile.orgUnitId || prev.orgUnitId),
                        department: profile.orgUnitName || prev.department,
                        joinDate: profile.startDate || prev.joinDate,
                        startDate: profile.startDate || prev.startDate,
                        contractEndDate: profile.contractEndDate || prev.contractEndDate,
                        standardHoursPerWeek: profile.standardHoursPerWeek || prev.standardHoursPerWeek || 40,
                    };
                });
            }
        } catch {
            // Keep current values
        }
    };

    const handleOpenView = async (emp: EmployeeFormData) => {
        setViewingEmployee(emp);
        const numId = typeof emp.id === "number" ? emp.id : parseInt(String(emp.id).replace(/\D/g, ""), 10);
        try {
            let profile: EmployeeProfile | null = null;
            if (emp.employeeId) {
                try {
                    profile = await getEmployeeProfile(emp.employeeId);
                } catch {
                    // fallback
                }
            }
            if (!profile && !isNaN(numId)) {
                try {
                    profile = await getEmployeeProfileByUserId(numId);
                } catch {
                    profile = null;
                }
            }
            if (profile) {
                setViewingEmployee((prev) => {
                    if (!prev || (prev.id !== emp.id && prev.employeeCode !== emp.employeeCode)) return prev;
                    return {
                        ...prev,
                        fullName: profile.fullName || prev.fullName,
                        orgUnitId: String(profile.orgUnitId || prev.orgUnitId),
                        department: profile.orgUnitName || prev.department,
                        joinDate: profile.startDate || prev.joinDate,
                        startDate: profile.startDate || prev.startDate,
                        contractEndDate: profile.contractEndDate || prev.contractEndDate,
                        standardHoursPerWeek: profile.standardHoursPerWeek || prev.standardHoursPerWeek || 40,
                    };
                });
            }
        } catch {
            // Keep current values
        }
    };

    const handleDeleteClick = (emp: EmployeeFormData) => {
        setDeleteTarget(emp);
        setDeleteError(null);
    };

    const handleConfirmDelete = async () => {
        if (!deleteTarget) return;

        const numId = typeof deleteTarget.id === "number" ? deleteTarget.id : parseInt(String(deleteTarget.id).replace(/\D/g, ""), 10);
        if (!isNaN(numId)) {
            setIsDeleting(true);
            setDeleteError(null);
            try {
                await toggleUserStatus(numId, true);

                // CHỈ cập nhật state UI sau khi API thành công
                setEmployees((prev) => prev.filter((e) => (e.id || e.employeeCode) !== (deleteTarget.id || deleteTarget.employeeCode)));
                setActionNotification({
                    type: "success",
                    message: `Đã xóa/khóa tài khoản nhân viên ${deleteTarget.fullName} thành công.`,
                });
                setDeleteTarget(null);
            } catch (err: any) {
                console.error("Lỗi khi xóa tài khoản nhân viên:", err);
                const errorMsg = err?.message || "Máy chủ phản hồi lỗi (403/500) hoặc lỗi mạng. Thao tác xóa không thành công và dữ liệu được giữ nguyên.";
                setDeleteError(errorMsg);
                // GIỮ NGUYÊN dữ liệu hiện tại, KHÔNG xóa khỏi employees!
            } finally {
                setIsDeleting(false);
            }
        } else {
            // Đối với mock data cục bộ chưa có ID backend
            setEmployees((prev) => prev.filter((e) => (e.id || e.employeeCode) !== (deleteTarget.id || deleteTarget.employeeCode)));
            setDeleteTarget(null);
        }
    };

    const handleSave = async (data: EmployeeFormData) => {
        setIsSaving(true);
        setFormError(null);

        if (editingEmployee?.id || editingEmployee?.employeeCode) {
            const targetId = editingEmployee.id || editingEmployee.employeeCode;
            const numId = typeof editingEmployee.id === "number" ? editingEmployee.id : parseInt(String(editingEmployee.id).replace(/\D/g, ""), 10);
            if (!isNaN(numId)) {
                try {
                    // 1. Cập nhật phân quyền tài khoản (Role & DataScope)
                    const roleRes = await updateUserRole(numId, {
                        roleCode: (data.roleCode as RoleCode) || "VT-04",
                        dataScope: (data.dataScope as DataScope) || "COMPANY",
                        scopeOrgUnitId: data.scopeOrgUnitId ? Number(data.scopeOrgUnitId) : null,
                    });

                    // 2. Cập nhật trạng thái tài khoản (Status) nếu có thay đổi
                    let finalStatus = data.status;
                    if (data.status && data.status !== editingEmployee.status) {
                        const statusRes = await toggleUserStatus(numId, data.status === "LOCKED");
                        if (statusRes?.status) {
                            finalStatus = statusRes.status;
                        }
                    }

                    // 3. Cập nhật hoặc tạo mới hồ sơ nhân sự (fullName, orgUnitId, standardHours, startDate, contractEndDate) qua API /employees
                    let updatedFullName = data.fullName.trim() || editingEmployee.fullName;
                    let updatedOrgUnitId = data.orgUnitId || editingEmployee.orgUnitId;