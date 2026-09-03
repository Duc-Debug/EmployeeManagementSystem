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
            className="group flex flex-col justify-between gap-4 rounded-xl border border-slate-200/80 bg-slate-50/60 p-3.5 transition hover:border-indigo-300 hover:bg-white hover:shadow-xs cursor-pointer sm:flex-row sm:items-center"
        >
            {/* Cột 1: Avatar + Tên + Mã NV */}
            <div className="flex min-w-[200px] items-center gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl border border-indigo-100 bg-indigo-50 text-indigo-600 shadow-xs">
                    <User className="size-5" />
                </div>
                <div className="min-w-0">
                    <div className="flex items-center gap-2">
                        <h3 className="truncate text-sm font-bold text-slate-900 group-hover:text-indigo-600 transition">
                            {employee.fullName}
                        </h3>
                        {employee.status === "LOCKED" || employee.status === "locked" ? (
                            <span className="rounded-md border border-rose-200 bg-rose-50 px-1.5 py-0.5 text-[10px] font-bold text-rose-700">
                                Đã khóa
                            </span>
                        ) : (
                            <span className="rounded-md border border-emerald-200 bg-emerald-50 px-1.5 py-0.5 text-[10px] font-bold text-emerald-700">
                                Hoạt động
                            </span>
                        )}
                    </div>
                    <p className="font-mono text-[11px] font-semibold text-slate-500">
                        {employee.employeeCode || employee.id || "EMP-001"}
                        {employee.username && <span className="text-slate-400 font-sans ml-1.5">(@{employee.username})</span>}
                    </p>
                </div>
            </div>

            {/* Cột 2: Email, Phòng ban, Vai trò */}
            <div className="grid flex-1 grid-cols-1 gap-2 text-xs font-medium text-slate-600 sm:grid-cols-3 sm:px-4">
                <span className="flex items-center gap-1.5 truncate">
                    <Mail className="size-3.5 shrink-0 text-slate-400" />
                    <span className="truncate">{employee.email}</span>
                </span>
                <span className="flex items-center gap-1.5 truncate">
                    <Building2 className="size-3.5 shrink-0 text-slate-400" />
                    <span className="truncate">{employee.department}</span>
                </span>
                <span className="flex items-center gap-1.5 truncate">
                    <Briefcase className="size-3.5 shrink-0 text-slate-400" />
                    <span className="truncate font-semibold text-slate-800">
                        {employee.roleName || employee.roleCode || employee.position || "Nhân viên chuyên môn"}
                    </span>
                </span>
            </div>

            {/* Cột 3: Ngày tham gia + Thao tác */}
            <div className="flex items-center justify-between gap-3 sm:justify-end">
                <span className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-semibold text-slate-600 shadow-2xs">
                    <CalendarDays className="size-3 text-slate-400" />
                    {employee.joinDate || "—"}
                </span>

                <div className="flex items-center gap-1">
                    <button
                        type="button"
                        onClick={(e) => {
                            e.stopPropagation()
                            onEdit(employee)
                        }}
                        className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
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
                        className="rounded-lg p-1.5 text-slate-400 transition hover:bg-rose-50 hover:text-rose-600"
                        title="Xóa"
                    >
                        <Trash2 className="size-4" />
                    </button>
                </div>
            </div>
        </div>
    )
}