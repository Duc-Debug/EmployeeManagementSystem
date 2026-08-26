import { User, Mail, Building, Briefcase, Edit, Trash2, Lock, Unlock } from "lucide-react";
import type { EmployeeFormData } from "./form/employeeForm.types";

interface EmployeeCardProps {
    employee: EmployeeFormData;
    onEdit: (employee: EmployeeFormData) => void;
    onDelete: (id?: string) => void;
}

export default function EmployeeCard({ employee, onEdit, onDelete }: EmployeeCardProps) {
    const isLocked = employee.status === "locked";

    return (
        <div className="relative flex flex-col justify-between rounded-2xl border border-white/10 bg-slate-900/60 p-5 backdrop-blur-md transition duration-200 hover:border-[#63ecc8]/50 hover:shadow-lg">
            <div className="space-y-4">
                <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-[#63ecc8]/20 to-purple-500/20 text-[#63ecc8] border border-white/10">
                            <User className="h-6 w-6" />
                        </div>
                        <div>
                            <h3 className="font-bold text-white text-base">{employee.fullName}</h3>
                            <p className="text-xs text-white/50">{employee.id || "EMP-N/A"}</p>
                        </div>
                    </div>

                    <div className="flex items-center gap-1">
                        <button
                            type="button"
                            onClick={() => onEdit(employee)}
                            className="rounded-lg p-1.5 text-white/60 transition hover:bg-white/10 hover:text-white"
                            title="Chỉnh sửa"
                        >
                            <Edit className="h-4 w-4" />
                        </button>
                        <button
                            type="button"
                            onClick={() => onDelete(employee.id)}
                            className="rounded-lg p-1.5 text-white/60 transition hover:bg-red-500/10 hover:text-red-400"
                            title="Xóa"
                        >
                            <Trash2 className="h-4 w-4" />
                        </button>
                    </div>
                </div>

                {/* Badge trạng thái tài khoản */}
                <div>
                    <span
                        className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-semibold ${
                            isLocked
                                ? "border-rose-500/30 bg-rose-500/10 text-rose-400"
                                : "border-emerald-500/30 bg-emerald-500/10 text-emerald-400"
                        }`}
                    >
                        {isLocked ? (
                            <Lock className="h-3 w-3" />
                        ) : (
                            <Unlock className="h-3 w-3" />
                        )}
                        {isLocked ? "Đã khóa" : "Đang hoạt động"}
                    </span>
                </div>

                <div className="space-y-2 text-xs text-white/70">
                    <div className="flex items-center gap-2">
                        <Mail className="h-3.5 w-3.5 text-[#63ecc8]" />
                        <span className="truncate">{employee.email}</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <Building className="h-3.5 w-3.5 text-purple-400" />
                        <span>Phòng ban: <strong className="text-white">{employee.department}</strong></span>
                    </div>
                    <div className="flex items-center gap-2">
                        <Briefcase className="h-3.5 w-3.5 text-blue-400" />
                        <span>Chức danh: <strong className="text-white">{employee.position}</strong></span>
                    </div>
                </div>
            </div>
        </div>
    );
}