"use client"

import { useState, useRef, useEffect } from "react"
import { Plus, Search, ChevronDown, Check, Users } from "lucide-react"
import { cn } from "@/lib/utils"
import EmployeeCard from "../components/employee/EmployeeCard"
import EmployeeProfileForm from "../components/employee/form/EmployeeProfileForm"
import EmployeeDetailModal from "../components/employee/form/EmployeeDetailModal"
import type { EmployeeFormData } from "../components/employee/form/employeeForm.types"

const INITIAL_EMPLOYEES: EmployeeFormData[] = [
    {
        id: "EMP-001",
        fullName: "Nguyễn Thị Mai",
        email: "mai.nguyen@company.com",
        phone: "0999999999",
        department: "Nhân sự",
        position: "HR Specialist",
        joinDate: "04/12/2026",
        contractEndDate: "04/12/2028",
        standardHoursPerWeek: 40,
        status: "active",
    },
]

const DEPARTMENT_OPTIONS = ["All", "Nhân sự", "Kỹ thuật", "Marketing", "Kinh doanh", "Tài chính"]

function formatDeptLabel(dept: string) {
    if (dept === "All") return "Tất cả phòng ban"
    if (dept.startsWith("Phòng")) return dept
    return `Phòng ${dept}`
}

/* ========================================================================
   CUSTOM DROPDOWN COMPONENT (Đã sửa lỗi bo góc 100% bằng rounded-2xl & overflow-hidden)
   ======================================================================== */
export function CustomSelectDropdown({
                                         value,
                                         onChange,
                                         options,
                                         placeholder = "Chọn phòng ban",
                                         labelPrefix = false,
                                     }: {
    value: string
    onChange: (v: string) => void
    options: string[]
    placeholder?: string
    labelPrefix?: boolean
}) {
    const [open, setOpen] = useState(false)
    const ref = useRef<HTMLDivElement>(null)

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (ref.current && !ref.current.contains(e.target as Node)) {
                setOpen(false)
            }
        }
        document.addEventListener("mousedown", handleClickOutside)
        return () => document.removeEventListener("mousedown", handleClickOutside)
    }, [])

    const getDisplayLabel = (val: string) => {
        if (!val) return placeholder
        return labelPrefix ? formatDeptLabel(val) : val === "All" ? "Tất cả phòng ban" : val
    }

    return (
        <div className="relative w-full sm:w-auto min-w-[180px]" ref={ref}>
            {/* Nút bấm Kích hoạt Dropdown (Bo góc mượt dạng Capsule/Custom) */}
            <button
                type="button"
                onClick={() => setOpen((o) => !o)}
                className="flex w-full items-center justify-between gap-3 rounded-2xl border border-white/25 bg-purple-500/20 px-4 py-2.5 text-xs font-bold text-white shadow-sm backdrop-blur-md transition hover:bg-purple-500/30 active:scale-95"
            >
                <span className="truncate">{getDisplayLabel(value)}</span>
                <ChevronDown className={cn("size-4 shrink-0 text-white/80 transition-transform duration-200", open && "rotate-180")} />
            </button>

            {/* Khung Menu thả xuống: Cố định bo góc rounded-2xl + overflow-hidden + Glassmorphism */}
            {open && (
                <div className="absolute left-0 right-0 z-50 mt-2 overflow-hidden rounded-2xl border border-white/15 bg-white/[0.3] px-4 py-2.5 text-sm font-medium text-white/80 backdrop-blur-xl transition hover:text-purple-200">
                    <div className="max-h-60 overflow-y-auto space-y-1 custom-scrollbar">
                        {options.map((opt) => {
                            const isActive = value === opt
                            return (
                                <button
                                    key={opt}
                                    type="button"
                                    onClick={() => {
                                        onChange(opt)
                                        setOpen(false)
                                    }}
                                    className={cn(
                                        "flex w-full items-center justify-between rounded-xl px-3.5 py-2.5 text-left text-xs font-bold transition-all",
                                        isActive
                                            ? "bg-purple-600/50 text-white shadow-sm border border-white/20"
                                            : "text-white/80 hover:bg-white/10 hover:text-white",
                                    )}
                                >
                                    <span>{labelPrefix ? formatDeptLabel(opt) : opt === "All" ? "Tất cả phòng ban" : opt}</span>
                                    {isActive && <Check className="size-3.5 text-purple-200 stroke-[2.5]" />}
                                </button>
                            )
                        })}
                    </div>
                </div>
            )}
        </div>
    )
}

export default function EmployeeProfilePage() {
    const [employees, setEmployees] = useState<EmployeeFormData[]>(INITIAL_EMPLOYEES)
    const [searchTerm, setSearchTerm] = useState("")
    const [selectedDept, setSelectedDept] = useState("All")

    const [isFormOpen, setIsFormOpen] = useState(false)
    const [editingEmployee, setEditingEmployee] = useState<EmployeeFormData | undefined>(undefined)
    const [viewingEmployee, setViewingEmployee] = useState<EmployeeFormData | undefined>(undefined)

    const filteredEmployees = employees.filter((emp) => {
        const matchesSearch =
            emp.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
            emp.email.toLowerCase().includes(searchTerm.toLowerCase())
        const matchesDept = selectedDept === "All" || emp.department === selectedDept
        return matchesSearch && matchesDept
    })

    const handleOpenAdd = () => {
        setEditingEmployee(undefined)
        setIsFormOpen(true)
    }

    const handleOpenEdit = (emp: EmployeeFormData) => {
        setEditingEmployee(emp)
        setIsFormOpen(true)
    }

    const handleDelete = (id?: string) => {
        if (!id) return
        if (confirm("Bạn có chắc chắn muốn xóa hồ sơ nhân sự này?")) {
            setEmployees(employees.filter((e) => e.id !== id))
        }
    }

    const handleSave = (data: EmployeeFormData) => {
        if (editingEmployee?.id) {
            setEmployees(
                employees.map((e) => (e.id === editingEmployee.id ? { ...data, id: editingEmployee.id } : e)),
            )
        } else {
            const newEmp: EmployeeFormData = {
                ...data,
                id: `EMP-${Date.now().toString().slice(-3)}`,
            }
            setEmployees([newEmp, ...employees])
        }
        setIsFormOpen(false)
    }

    return (
        <div className="space-y-6">
            {/* PHẦN 1: HEADER TRANG */}
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                <div>
                    <h1 className="text-2xl font-extrabold tracking-tight text-white">
                        Quản lý hồ sơ nhân sự
                    </h1>
                    <p className="mt-1 text-xs font-semibold text-white/80 sm:text-sm">
                        Khai báo, cập nhật và quản lý danh sách hồ sơ nhân sự HR.
                    </p>
                </div>
            </div>

            {/* PHẦN 2: KHUNG MAIN GLASSMORPHISM */}
            <div className="rounded-2xl border border-white/20 bg-white/[0.08] p-5 shadow-[0_8px_24px_rgba(15,10,45,0.2)] backdrop-blur-xl space-y-4">
                {/* Thanh điều khiển trên cùng */}
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                    {/* Ô tìm kiếm */}
                    <div className="relative flex-1">
                        <Search className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-white/50" />
                        <input
                            type="text"
                            placeholder="Tìm kiếm theo tên hoặc email..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full rounded-xl border border-white/20 bg-white/10 py-2.5 pl-10 pr-4 text-xs font-medium text-white placeholder:text-white/50 outline-none transition focus:border-white/40 focus:bg-white/15"
                        />
                    </div>

                    {/* Bộ lọc phòng ban (Custom Select Bo Góc) + Nút thêm mới */}
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
                            className="flex items-center gap-1.5 rounded-xl border border-indigo-400/40 bg-indigo-600/80 px-4 py-2.5 text-xs font-bold text-white shadow-lg backdrop-blur-md transition hover:bg-indigo-600 active:scale-95"
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
                            key={emp.id}
                            employee={emp}
                            onView={(employeeData) => setViewingEmployee(employeeData)}
                            onEdit={handleOpenEdit}
                            onDelete={handleDelete}
                        />
                    ))}
                    {filteredEmployees.length === 0 && (
                        <div className="py-12 text-center text-white/60">
                            <Users className="mx-auto mb-2 size-8 text-white/40" />
                            <p className="text-xs font-semibold">Không tìm thấy nhân sự phù hợp.</p>
                        </div>
                    )}
                </div>
            </div>

            {/* Modal Xem chi tiết */}
            <EmployeeDetailModal
                isOpen={!!viewingEmployee}
                employee={viewingEmployee || null}
                onClose={() => setViewingEmployee(undefined)}
            />

            {/* Modal Thêm / Chỉnh sửa */}
            <EmployeeProfileForm
                open={isFormOpen}
                initialData={editingEmployee}
                onClose={() => setIsFormOpen(false)}
                onSave={handleSave}
            />
        </div>
    )
}