"use client";

import { useState, useRef, useEffect } from "react";
import { Plus, Search, ChevronDown, Check, AlertTriangle, X, User, Trash2 } from "lucide-react";
import { cn } from "@/lib/utils";
import EmployeeCard from "../components/employee/EmployeeCard";
import EmployeeProfileForm from "../components/employee/form/EmployeeProfileForm";
import EmployeeDetailModal from "../components/employee/form/EmployeeDetailModal";
import type { EmployeeFormData } from "../components/employee/form/employeeForm.types";
import { DEFAULT_ORG_UNIT_OPTIONS, DEPARTMENT_OPTIONS } from "../components/employee/form/employeeForm.constants";
import { getUsers, createUser, updateUserRole, toggleUserStatus } from "@/lib/api/users";
import { getOrgTree } from "@/lib/api/org-units";
import { flattenOrgTree } from "@/lib/organization";
import type { OrgUnitOption } from "@/components/ui/OrgUnitCombobox";
import type { User as BackendUser, RoleCode, DataScope } from "@/types/hrm";

const INITIAL_EMPLOYEES: EmployeeFormData[] = [
    {
        id: "EMP-001",
        employeeCode: "EMP-001",
        fullName: "Chu Văn Hưng",
        username: "hung.cv",
        email: "hungwgg01@gmail.com",
        phone: "0912345678",
        orgUnitId: "3",
        department: "Phòng Lập trình Frontend",
        position: "Trưởng phòng",
        roleCode: "VT-03",
        roleName: "Quản lý nguồn lực",
        dataScope: "ORGANIZATION_BRANCH",
        scopeOrgUnitId: "3",
        status: "ACTIVE",
        joinDate: "15/01/2024",
    },
    {
        id: "EMP-002",
        employeeCode: "EMP-002",
        fullName: "Nguyễn Thị Mai",
        username: "mai.nt",
        email: "mai.nguyen@company.com",
        phone: "0999999999",
        orgUnitId: "14",
        department: "Phòng Quản trị nhân sự (HR)",
        position: "HR Specialist",
        roleCode: "VT-05",
        roleName: "Nhân sự (HR)",
        dataScope: "COMPANY",
        status: "ACTIVE",
        joinDate: "04/12/2024",
    },
    {
        id: "EMP-003",
        employeeCode: "EMP-003",
        fullName: "Trần Quốc Bảo",
        username: "bao.tq",
        email: "bao.tran@company.com",
        phone: "0988888888",
        orgUnitId: "2",
        department: "Khối Kỹ thuật & Công nghệ",
        position: "Giám đốc kỹ thuật",
        roleCode: "VT-01",
        roleName: "Ban giám đốc",
        dataScope: "COMPANY",
        status: "ACTIVE",
        joinDate: "10/08/2023",
    },
    {
        id: "EMP-004",
        employeeCode: "EMP-004",
        fullName: "Lê Hoàng Long",
        username: "long.lh",
        email: "long.le@company.com",
        phone: "0977777777",
        orgUnitId: "4",
        department: "Phòng Lập trình Backend",
        position: "Backend Developer",
        roleCode: "VT-04",
        roleName: "Nhân viên chuyên môn",
        dataScope: "SELF",
        status: "LOCKED",
        joinDate: "01/03/2025",
    },
];

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
}: {
    target: EmployeeFormData | null;
    onClose: () => void;
    onConfirm: () => void;
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
                        className="rounded-xl p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition"
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

                {/* Action Buttons */}
                <div className="mt-6 flex items-center justify-end gap-3">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 shadow-xs transition hover:bg-slate-50"
                    >
                        Hủy
                    </button>
                    <button
                        type="button"
                        onClick={onConfirm}
                        className="flex items-center gap-1.5 rounded-xl border border-rose-600 bg-rose-600 px-4 py-2 text-xs font-semibold text-white shadow-xs transition hover:bg-rose-700 active:scale-95"
                    >
                        <Trash2 className="size-3.5" />
                        <span>Xác nhận xóa</span>
                    </button>
                </div>
            </div>
        </div>
    );
}

export default function EmployeeProfilePage() {
    const [employees, setEmployees] = useState<EmployeeFormData[]>(INITIAL_EMPLOYEES);
    const [searchTerm, setSearchTerm] = useState("");
    const [selectedDept, setSelectedDept] = useState("All");

    const [isFormOpen, setIsFormOpen] = useState(false);
    const [editingEmployee, setEditingEmployee] = useState<EmployeeFormData | undefined>(undefined);
    const [viewingEmployee, setViewingEmployee] = useState<EmployeeFormData | undefined>(undefined);
    const [deleteTarget, setDeleteTarget] = useState<EmployeeFormData | null>(null);
    const [orgUnitOptions, setOrgUnitOptions] = useState<readonly OrgUnitOption[]>(DEFAULT_ORG_UNIT_OPTIONS);

    // Fetch users và OrgUnits từ Backend API khi trang được tải
    useEffect(() => {
        let isMounted = true;
        async function loadData() {
            try {
                const [userRes, treeRes] = await Promise.allSettled([
                    getUsers(0, 100),
                    getOrgTree(),
                ]);

                if (isMounted && treeRes.status === "fulfilled" && treeRes.value && treeRes.value.length > 0) {
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

                if (isMounted && userRes.status === "fulfilled" && userRes.value?.content && userRes.value.content.length > 0) {
                    const mapped: EmployeeFormData[] = userRes.value.content.map((u: BackendUser) => ({
                        id: String(u.id),
                        employeeCode: u.employeeId ? `EMP-${String(u.employeeId).padStart(3, "0")}` : `EMP-${u.id}`,
                        fullName: u.fullName || u.username,
                        username: u.username,
                        email: u.email || `${u.username}@company.com`,
                        phone: "0912345678",
                        orgUnitId: u.orgUnitId ? String(u.orgUnitId) : undefined,
                        department: u.orgUnitName || "Chưa phân bổ",
                        position: u.roleName || "Nhân viên",
                        roleCode: u.roleCode,
                        roleName: u.roleName,
                        dataScope: u.dataScope,
                        scopeOrgUnitId: u.scopeOrgUnitId ? String(u.scopeOrgUnitId) : undefined,
                        status: u.status,
                        joinDate: "01/01/2025",
                    }));
                    setEmployees(mapped);
                }
            } catch {
                // Backend offline: Tự động giữ fallback an toàn
            }
        }
        loadData();
        return () => {
            isMounted = false;
        };
    }, []);

    const nextEmployeeCode = `EMP-${String(employees.length + 1).padStart(3, "0")}`;

    const filteredEmployees = employees.filter((emp) => {
        const matchesSearch =
            emp.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
            emp.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
            (emp.employeeCode && emp.employeeCode.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (emp.username && emp.username.toLowerCase().includes(searchTerm.toLowerCase()));
        const matchesDept = selectedDept === "All" || emp.department === selectedDept;
        return matchesSearch && matchesDept;
    });

    const handleOpenAdd = () => {
        setEditingEmployee(undefined);
        setIsFormOpen(true);
    };

    const handleOpenEdit = (emp: EmployeeFormData) => {
        setEditingEmployee(emp);
        setIsFormOpen(true);
    };

    const handleDeleteClick = (emp: EmployeeFormData) => {
        setDeleteTarget(emp);
    };

    const handleConfirmDelete = async () => {
        if (deleteTarget) {
            const numId = typeof deleteTarget.id === "number" ? deleteTarget.id : parseInt(String(deleteTarget.id).replace(/\D/g, ""), 10);
            if (!isNaN(numId)) {
                try {
                    await toggleUserStatus(numId, true);
                } catch {
                    // Backend offline: Tiếp tục xử lý xóa trên state
                }
            }
            setEmployees((prev) => prev.filter((e) => (e.id || e.employeeCode) !== (deleteTarget.id || deleteTarget.employeeCode)));
            setDeleteTarget(null);
        }
    };

    const handleSave = async (data: EmployeeFormData) => {
        if (editingEmployee?.id || editingEmployee?.employeeCode) {
            const targetId = editingEmployee.id || editingEmployee.employeeCode;
            const numId = typeof editingEmployee.id === "number" ? editingEmployee.id : parseInt(String(editingEmployee.id).replace(/\D/g, ""), 10);
            if (!isNaN(numId)) {
                try {
                    await updateUserRole(numId, {
                        roleCode: (data.roleCode as RoleCode) || "VT-04",
                        dataScope: (data.dataScope as DataScope) || "COMPANY",
                        scopeOrgUnitId: data.scopeOrgUnitId ? Number(data.scopeOrgUnitId) : null,
                    });
                    if (data.status) {
                        await toggleUserStatus(numId, data.status === "LOCKED");
                    }
                } catch {
                    // Backend offline fallback
                }
            }
            setEmployees((prev) =>
                prev.map((e) => ((e.id || e.employeeCode) === targetId ? { ...data, id: e.id || data.employeeCode || String(targetId) } : e))
            );
        } else {
            let createdId: string = data.employeeCode || `EMP-${Date.now().toString().slice(-3)}`;
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
                if (res?.id) {
                    createdId = String(res.id);
                    if (data.dataScope && data.dataScope !== "COMPANY") {
                        await updateUserRole(res.id, {
                            roleCode: (data.roleCode as RoleCode) || "VT-04",
                            dataScope: (data.dataScope as DataScope) || "COMPANY",
                            scopeOrgUnitId: data.scopeOrgUnitId ? Number(data.scopeOrgUnitId) : null,
                        });
                    }
                }
            } catch {
                // Backend offline fallback
            }
            const newEmp: EmployeeFormData = {
                ...data,
                id: createdId,
            };
            setEmployees((prev) => [newEmp, ...prev]);
        }
        setIsFormOpen(false);
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
                    {filteredEmployees.map((emp) => (
                        <EmployeeCard
                            key={emp.id || emp.employeeCode}
                            employee={emp}
                            onView={(employeeData) => setViewingEmployee(employeeData)}
                            onEdit={handleOpenEdit}
                            onDelete={() => handleDeleteClick(emp)}
                        />
                    ))}

                    {filteredEmployees.length === 0 && (
                        <div className="py-12 text-center text-xs text-slate-400">
                            Không tìm thấy nhân viên nào phù hợp với bộ lọc tìm kiếm.
                        </div>
                    )}
                </div>
            </div>

            {/* MODAL THÊM MỚI / CHỈNH SỬA (OPTION A VỚI CÂY COMBOBOX & PHÂN QUYỀN) */}
            <EmployeeProfileForm
                open={isFormOpen}
                initialData={editingEmployee}
                onClose={() => setIsFormOpen(false)}
                onSave={handleSave}
                nextEmployeeCode={nextEmployeeCode}
                orgUnitOptions={orgUnitOptions}
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
                onClose={() => setDeleteTarget(null)}
                onConfirm={handleConfirmDelete}
            />
        </div>
    );
}
