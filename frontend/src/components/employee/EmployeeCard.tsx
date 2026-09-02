"use client"

import { User, Mail, Building2, Briefcase, CalendarDays, Pencil, Trash2 } from "lucide-react"
import type { EmployeeFormData } from "./form/employeeForm.types"

interface EmployeeCardProps {
    employee: EmployeeFormData
    onEdit: (employee: EmployeeFormData) => void
    onDelete: (id?: string) => void
    onView?: (employee: EmployeeFormData) => void
}

export default function EmployeeCard({ employee, onEdit, onDelete, onView }: EmployeeCardProps) {
    return (
        <div
            onClick={() => onView && onView(employee)}
            className="group flex flex-col justify-between gap-4 rounded-xl border border-white/15 bg-white/[0.05] p-3.5 backdrop-blur-md transition hover:border-white/30 hover:bg-white/10 sm:flex-row sm:items-center"
        >
            {/* Cột 1: Avatar + Tên + Mã NV */}
            <div className="flex min-w-[200px] items-center gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl border border-white/20 bg-white/15 text-white shadow-inner">
                    <User className="size-5" />
                </div>
                <div className="min-w-0">
                    <h3 className="truncate text-sm font-bold text-white group-hover:text-purple-200">
                        {employee.fullName}
                    </h3>
                    <p className="font-mono text-[11px] font-semibold text-white/60">
                        {employee.id || "EMP-001"}
                    </p>
                </div>
            </div>

            {/* Cột 2: Email, Phòng ban, Chức danh */}
            <div className="grid flex-1 grid-cols-1 gap-2 text-xs font-medium text-white/80 sm:grid-cols-3 sm:px-4">
                <span className="flex items-center gap-1.5 truncate">
                    <Mail className="size-3.5 shrink-0 text-purple-300" />
                    <span className="truncate">{employee.email}</span>
                </span>
                <span className="flex items-center gap-1.5 truncate">
                    <Building2 className="size-3.5 shrink-0 text-purple-300" />
                    <span className="truncate">{employee.department}</span>
                </span>
                <span className="flex items-center gap-1.5 truncate">
                    <Briefcase className="size-3.5 shrink-0 text-purple-300" />
                    <span className="truncate">{employee.position}</span>
                </span>
            </div>

            {/* Cột 3: Ngày tham gia + Thao tác */}
            <div className="flex items-center justify-between gap-3 sm:justify-end">
                <span className="flex items-center gap-1.5 rounded-lg border border-white/20 bg-white/10 px-2.5 py-1 text-[11px] font-bold text-white/90">
                    <CalendarDays className="size-3 text-purple-300" />
                    {employee.joinDate || "—"}
                </span>

                <div className="flex items-center gap-1">
                    <button
                        type="button"
                        onClick={(e) => {
                            e.stopPropagation()
                            onEdit(employee)
                        }}
                        className="rounded-lg p-1.5 text-white/70 transition hover:bg-white/20 hover:text-white"
                        title="Chỉnh sửa"
                    >
                        <Pencil className="size-4" />
                    </button>
                    <button
                        type="button"
                        onClick={(e) => {
                            e.stopPropagation()
                            onDelete(employee.id)
                        }}
                        className="rounded-lg p-1.5 text-rose-300/80 transition hover:bg-rose-500/20 hover:text-rose-200"
                        title="Xóa"
                    >
                        <Trash2 className="size-4" />
                    </button>
                </div>
            </div>
        </div>
    )
}