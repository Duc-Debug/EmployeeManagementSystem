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
                    let updatedDepartment = data.department || editingEmployee.department;
                    let updatedStandardHours = Number(data.standardHoursPerWeek) || 40;

                    const empId = editingEmployee.employeeId;
                    const reqStartDate = data.joinDate ? formatToDateInput(data.joinDate) : undefined;
                    const reqContractEndDate = data.contractEndDate ? formatToDateInput(data.contractEndDate) : undefined;

                    try {
                        let profile: EmployeeProfile | null = null;
                        if (empId) {
                            try {
                                profile = await getEmployeeProfile(empId);
                            } catch {
                                // fallback to getEmployeeProfileByUserId
                            }
                        }
                        if (!profile && numId) {
                            try {
                                profile = await getEmployeeProfileByUserId(numId);
                            } catch {
                                profile = null;
                            }
                        }

                        if (profile) {
                            const newOrgId = data.orgUnitId ? Number(data.orgUnitId) : profile.orgUnitId;
                            const updatedProfile = await updateEmployeeProfile(profile.id, {
                                version: profile.version ?? 0,
                                fullName: data.fullName.trim(),
                                orgUnitId: newOrgId,
                                professionalRole: profile.professionalRole,
                                startDate: reqStartDate || profile.startDate,
                                contractEndDate: reqContractEndDate || profile.contractEndDate,
                                standardHoursPerWeek: Number(data.standardHoursPerWeek) || profile.standardHoursPerWeek || 40,
                            });
                            updatedFullName = updatedProfile.fullName;
                            updatedOrgUnitId = String(updatedProfile.orgUnitId);
                            updatedDepartment = updatedProfile.orgUnitName || data.department || editingEmployee.department;
                            updatedStandardHours = updatedProfile.standardHoursPerWeek;
                        } else if (numId && data.orgUnitId) {
                            const createdProfile = await createEmployeeProfile({
                                userId: numId,
                                orgUnitId: Number(data.orgUnitId),
                                employeeCode: data.employeeCode || `EMP-${String(numId).padStart(3, "0")}`,
                                fullName: data.fullName.trim(),
                                startDate: reqStartDate,
                                contractEndDate: reqContractEndDate,
                                standardHoursPerWeek: Number(data.standardHoursPerWeek) || 40,
                            });
                            updatedFullName = createdProfile.fullName;
                            updatedOrgUnitId = String(createdProfile.orgUnitId);
                            updatedDepartment = createdProfile.orgUnitName || data.department || editingEmployee.department;
                            updatedStandardHours = createdProfile.standardHoursPerWeek;
                        }
                    } catch (profErr: any) {
                        console.warn("Không thể đồng bộ hồ sơ nhân sự backend:", profErr);
                    }

                    // 4. Lưu số điện thoại và ngày tháng vào localStorage
                    if (data.phone !== undefined) {
                        saveStoredPhone(
                            [numId, editingEmployee.id, editingEmployee.employeeId, editingEmployee.employeeCode, data.employeeCode],
                            data.phone
                        );
                    }
                    saveStoredDates(
                        [numId, editingEmployee.id, editingEmployee.employeeId, editingEmployee.employeeCode, data.employeeCode],
                        { joinDate: data.joinDate, contractEndDate: data.contractEndDate }
                    );

                    // Cập nhật state UI với các trường đã được backend xác nhận lưu thành công
                    setEmployees((prev) =>
                        prev.map((e) =>
                            (e.id || e.employeeCode) === targetId
                                ? {
                                      ...e,
                                      fullName: updatedFullName,
                                      orgUnitId: updatedOrgUnitId,
                                      department: updatedDepartment,
                                      phone: data.phone || undefined,
                                      joinDate: data.joinDate || e.joinDate,
                                      startDate: data.joinDate || e.startDate,
                                      contractEndDate: data.contractEndDate || e.contractEndDate,
                                      standardHoursPerWeek: updatedStandardHours,
                                      roleCode: roleRes?.roleCode || data.roleCode,
                                      roleName: roleRes?.roleName || data.roleName || e.roleName,
                                      dataScope: roleRes?.dataScope || data.dataScope,
                                      scopeOrgUnitId:
                                          roleRes?.scopeOrgUnitId !== undefined
                                              ? (roleRes.scopeOrgUnitId ? String(roleRes.scopeOrgUnitId) : undefined)
                                              : data.scopeOrgUnitId,
                                      status: finalStatus || e.status,
                                  }
                                : e
                        )
                    );
                    setActionNotification({
                        type: "success",
                        message: `Cập nhật thông tin nhân viên ${updatedFullName} thành công.`,
                    });
                    setIsFormOpen(false);
                } catch (err: any) {
                    console.error("Lỗi cập nhật nhân sự:", err);
                    const msg = err?.message || "Cập nhật nhân viên thất bại do máy chủ phản hồi lỗi. Dữ liệu chưa được lưu.";
                    setFormError(msg);
                    setActionNotification({
                        type: "error",
                        message: msg,
                    });
                    // Giữ nguyên form, KHÔNG cập nhật state cục bộ
                } finally {
                    setIsSaving(false);
                }
            } else {
                if (data.phone !== undefined) {
                    saveStoredPhone([targetId, data.employeeCode], data.phone);
                }
                saveStoredDates([targetId, data.employeeCode], {
                    joinDate: data.joinDate,
                    contractEndDate: data.contractEndDate,
                });
                setEmployees((prev) =>
                    prev.map((e) =>
                        (e.id || e.employeeCode) === targetId
                            ? {
                                  ...e,
                                  fullName: data.fullName,
                                  department: data.department,
                                  phone: data.phone || undefined,
                                  joinDate: data.joinDate || e.joinDate,
                                  startDate: data.joinDate || e.startDate,
                                  contractEndDate: data.contractEndDate || e.contractEndDate,
                                  standardHoursPerWeek: Number(data.standardHoursPerWeek) || 40,
                                  roleCode: data.roleCode,
                                  roleName: data.roleName || e.roleName,
                                  dataScope: data.dataScope,
                                  scopeOrgUnitId: data.scopeOrgUnitId,
                                  status: data.status || e.status,
                              }
                            : e
                    )
                );
                setIsFormOpen(false);
                setIsSaving(false);
            }
        } else {
            // TẠO MỚI TÀI KHOẢN: GỌI API THẬT, KHÔNG FALLBACK TẠO STATE CỤC BỘ KHI THẤT BẠI
            try {
                const res = await createUser({
                    fullName: data.fullName,
                    email: data.email,
                    employeeCode: data.employeeCode,
                    username: data.username || data.fullName.toLowerCase().replace(/\s+/g, "."),
                    password: data.password || "123456",
                    orgUnitId: data.orgUnitId ? Number(data.orgUnitId) : null,
                    roleCode: (data.roleCode as RoleCode) || "VT-04",
                });

                if (!res || !res.id) {
                    throw new Error("Máy chủ phản hồi nhưng không tạo được tài khoản hợp lệ.");
                }

                if (data.dataScope && data.dataScope !== "COMPANY") {
                    await updateUserRole(res.id, {
                        roleCode: (data.roleCode as RoleCode) || "VT-04",
                        dataScope: (data.dataScope as DataScope) || "COMPANY",
                        scopeOrgUnitId: data.scopeOrgUnitId ? Number(data.scopeOrgUnitId) : null,
                    });
                }

                if (data.phone) {
                    saveStoredPhone([res.id, data.employeeCode], data.phone);
                }
                if (data.joinDate || data.contractEndDate) {
                    saveStoredDates([res.id, data.employeeCode], {
                        joinDate: data.joinDate,
                        contractEndDate: data.contractEndDate,
                    });
                }

                // CHỈ THÊM EMPLOYEE VÀO STATE SAU KHI BACKEND TRẢ VỀ SUCCESS VÀ CÓ ID THẬT
                const newEmp: EmployeeFormData = {
                    ...data,
                    id: String(res.id),
                    employeeCode: data.employeeCode,
                    phone: data.phone || undefined,
                    joinDate: data.joinDate || undefined,
                    startDate: data.joinDate || undefined,
                    contractEndDate: data.contractEndDate || undefined,
                    standardHoursPerWeek: Number(data.standardHoursPerWeek) || 40,
                };
                setEmployees((prev) => [newEmp, ...prev]);
                setActionNotification({
                    type: "success",
                    message: `Tạo mới nhân viên ${data.fullName} thành công.`,
                });
                setIsFormOpen(false);
            } catch (err: any) {
                console.error("Lỗi tạo nhân sự:", err);
                const msg = err?.message || "Tạo mới nhân viên thất bại do lỗi từ máy chủ. Dữ liệu chưa được lưu vào hệ thống.";
                setFormError(msg);
                setActionNotification({
                    type: "error",
                    message: msg,
                });
                // TUYỆT ĐỐI KHÔNG TẠO EMPLOYEE LOCAL VÀ KHÔNG ĐÓNG FORM KHI API THẤT BẠI!
            } finally {
                setIsSaving(false);
            }
        }
    };

    return (
        <div className="space-y-6">
            {/* PHẦN 1: HEADER TRANG */}
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                <div>
                    <h1 className="text-2xl font-extrabold tracking-tight text-slate-900">
                        Quản lý hồ sơ nhân sự
                    </h1>
                    <p className="mt-1 text-xs font-semibold text-slate-500 sm:text-sm">
                        Khai báo, phân quyền vai trò và quản lý danh sách hồ sơ nhân sự toàn công ty.
                    </p>
                </div>
            </div>

            {/* THÔNG BÁO KẾT QUẢ THAO TÁC */}
            {actionNotification && (
                <div
                    className={cn(
                        "flex items-center justify-between rounded-2xl border p-4 text-xs font-semibold shadow-xs transition animate-fadeIn",
                        actionNotification.type === "success"
                            ? "border-emerald-200 bg-emerald-50/90 text-emerald-800"
                            : "border-rose-200 bg-rose-50/90 text-rose-800"
                    )}
                >
                    <div className="flex items-center gap-2.5">
                        {actionNotification.type === "success" ? (
                            <Check className="size-4 shrink-0 text-emerald-600" />
                        ) : (
                            <AlertTriangle className="size-4 shrink-0 text-rose-600" />
                        )}
                        <span>{actionNotification.message}</span>
                    </div>
                    <button
                        type="button"
                        onClick={() => setActionNotification(null)}
                        className="rounded-lg p-1 hover:bg-black/5 text-slate-500 transition"
                    >
                        <X className="size-3.5" />
                    </button>
                </div>
            )}

            {/* PHẦN 2: KHUNG MAIN WHITE THEME */}
            <div className="rounded-2xl border border-slate-200/90 bg-white p-5 shadow-xs space-y-4">
                {/* Thanh điều khiển trên cùng */}
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                    {/* Ô tìm kiếm */}
                    <div className="relative flex-1">
                        <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Tìm kiếm theo tên, mã NV, email hoặc tên đăng nhập..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-4 text-xs font-medium text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>

                    {/* Bộ lọc phòng ban + Nút thêm mới */}
                    <div className="flex flex-wrap items-center justify-end gap-3">
                        <CustomSelectDropdown
                            value={selectedDept}
                            onChange={setSelectedDept}
                            options={DEPARTMENT_OPTIONS}
                            labelPrefix={true}
                        />

                        <button
                            type="button"
                            onClick={handleOpenAdd}
                            className="flex items-center gap-1.5 rounded-xl border border-indigo-600 bg-indigo-600 px-4 py-2.5 text-xs font-bold text-white shadow-xs transition hover:bg-indigo-700 active:scale-95"
                        >
                            <Plus className="size-4 stroke-[2.5]" />
                            <span>Thêm nhân sự mới</span>
                        </button>
                    </div>
                </div>

                {/* Danh sách nhân sự */}
                <div className="space-y-3 pt-1">
                    {/* Trạng thái 1: Đang tải dữ liệu (Skeleton Loading) */}
                    {isLoading && (
                        <div className="space-y-3">
                            {[1, 2, 3, 4].map((idx) => (
                                <div
                                    key={idx}
                                    className="flex flex-col justify-between gap-4 rounded-xl border border-slate-200/80 bg-slate-50/50 p-4 animate-pulse sm:flex-row sm:items-center"
                                >
                                    <div className="flex min-w-[200px] items-center gap-3">
                                        <div className="size-10 rounded-xl bg-slate-200 shrink-0" />
                                        <div className="space-y-2">
                                            <div className="h-4 w-32 rounded-md bg-slate-200" />
                                            <div className="h-3 w-20 rounded-md bg-slate-100" />
                                        </div>
                                    </div>
                                    <div className="grid flex-1 grid-cols-1 gap-2 sm:grid-cols-3 sm:px-4">
                                        <div className="h-3.5 w-32 rounded-md bg-slate-100" />
                                        <div className="h-3.5 w-24 rounded-md bg-slate-100" />
                                        <div className="h-3.5 w-28 rounded-md bg-slate-100" />
                                    </div>
                                    <div className="h-8 w-20 rounded-xl bg-slate-200/60 shrink-0" />
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Trạng thái 2: Lỗi kết nối / Máy chủ Backend phản hồi lỗi */}
                    {!isLoading && loadError && (
                        <div className="flex flex-col items-center justify-center rounded-2xl border border-rose-200 bg-rose-50/50 p-10 text-center animate-in fade-in duration-200">
                            <div className="flex size-12 items-center justify-center rounded-2xl border border-rose-200 bg-rose-100 text-rose-600 shadow-2xs">
                                <AlertTriangle className="size-6" />
                            </div>
                            <h3 className="mt-3.5 text-sm font-bold text-slate-900">
                                Không thể tải danh sách nhân sự
                            </h3>
                            <p className="mt-1 max-w-md text-xs text-slate-600 leading-relaxed">
                                {loadError}
                            </p>
                            <button
                                type="button"
                                onClick={loadData}
                                className="mt-4 flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 shadow-xs transition hover:bg-slate-50 hover:text-indigo-600 active:scale-95"
                            >
                                <RefreshCw className="size-3.5 text-indigo-600" />
                                <span>Thử lại</span>
                            </button>
                        </div>
                    )}

                    {/* Trạng thái 3: Danh sách rỗng trong CSDL (Chưa có nhân sự nào) */}
                    {!isLoading && !loadError && employees.length === 0 && (
                        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/40 p-12 text-center animate-in fade-in duration-200">
                            <div className="flex size-14 items-center justify-center rounded-2xl border border-indigo-100 bg-indigo-50 text-indigo-600 shadow-2xs">
                                <Users className="size-7" />
                            </div>
                            <h3 className="mt-4 text-base font-bold text-slate-900">
                                Chưa có hồ sơ nhân sự nào
                            </h3>
                            <p className="mt-1 max-w-sm text-xs text-slate-500 leading-relaxed">
                                Hệ thống chưa ghi nhận tài khoản nhân sự nào trong cơ sở dữ liệu. Nhấn nút bên dưới để tạo hồ sơ đầu tiên.
                            </p>
                            <button
                                type="button"
                                onClick={handleOpenAdd}
                                className="mt-5 flex items-center gap-1.5 rounded-xl border border-indigo-600 bg-indigo-600 px-4 py-2.5 text-xs font-bold text-white shadow-xs transition hover:bg-indigo-700 active:scale-95"
                            >
                                <Plus className="size-4 stroke-[2.5]" />
                                <span>Thêm nhân sự mới</span>
                            </button>
                        </div>
                    )}

                    {/* Trạng thái 4: Có nhân sự nhưng không khớp bộ lọc tìm kiếm */}
                    {!isLoading && !loadError && employees.length > 0 && filteredEmployees.length === 0 && (
                        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50/40 p-10 text-center animate-in fade-in duration-200">
                            <div className="flex size-11 items-center justify-center rounded-xl bg-slate-100 text-slate-400">
                                <Search className="size-5" />
                            </div>
                            <h3 className="mt-3 text-sm font-bold text-slate-800">
                                Không tìm thấy nhân viên phù hợp
                            </h3>
                            <p className="mt-1 text-xs text-slate-500">
                                Không có kết quả nào khớp với điều kiện tìm kiếm hoặc bộ lọc phòng ban đã chọn.
                            </p>
                            <button
                                type="button"
                                onClick={() => {
                                    setSearchTerm("");
                                    setSelectedDept("All");
                                }}
                                className="mt-3.5 rounded-xl border border-slate-200 bg-white px-3.5 py-1.5 text-xs font-semibold text-indigo-600 shadow-2xs hover:bg-slate-50"
                            >
                                Xóa bộ lọc tìm kiếm
                            </button>
                        </div>
                    )}

                    {/* Trạng thái 5: Danh sách nhân sự bình thường */}
                    {!isLoading && !loadError && filteredEmployees.length > 0 && (
                        filteredEmployees.map((emp) => (
                            <EmployeeCard
                                key={emp.id || emp.employeeCode}
                                employee={emp}
                                onView={handleOpenView}
                                onEdit={handleOpenEdit}
                                onDelete={() => handleDeleteClick(emp)}
                            />
                        ))
                    )}
                </div>
            </div>

            {/* MODAL THÊM MỚI / CHỈNH SỬA (OPTION A VỚI CÂY COMBOBOX & PHÂN QUYỀN) */}
            <EmployeeProfileForm
                open={isFormOpen}
                initialData={editingEmployee}
                onClose={() => {
                    setIsFormOpen(false);
                    setFormError(null);
                }}
                onSave={handleSave}
                nextEmployeeCode={nextEmployeeCode}
                orgUnitOptions={orgUnitOptions}
                isSubmitting={isSaving}
                apiError={formError}
            />

            {/* MODAL XEM CHI TIẾT */}
            <EmployeeDetailModal
                isOpen={Boolean(viewingEmployee)}
                employee={viewingEmployee ?? null}
                onClose={() => setViewingEmployee(undefined)}
            />

            {/* DIALOG XÁC NHẬN XÓA (TỰ TẠO - THAY THẾ WINDOW.CONFIRM) */}
            <DeleteConfirmDialog
                target={deleteTarget}
                onClose={() => {
                    setDeleteTarget(null);
                    setDeleteError(null);
                }}
                onConfirm={handleConfirmDelete}
                isSubmitting={isDeleting}
                errorMessage={deleteError}
            />
        </div>
    );
}
