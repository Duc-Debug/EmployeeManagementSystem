import { X, User } from "lucide-react";
import type { EmployeeFormData } from "./employeeForm.types";

interface EmployeeDetailModalProps {
    isOpen: boolean;
    employee: EmployeeFormData | null;
    onClose: () => void;
}

export default function EmployeeDetailModal({
                                                isOpen,
                                                employee,
                                                onClose,
                                            }: EmployeeDetailModalProps) {
    if (!isOpen || !employee) return null;

    const isActive = employee.status !== "LOCKED" && employee.status !== "locked";

    const getScopeLabel = (scope?: string) => {
        if (scope === "COMPANY") return "Toàn công ty";
        if (scope === "ORGANIZATION_BRANCH") return "Theo đơn vị";
        if (scope === "SELF") return "Cá nhân";
        return "Cá nhân";
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm animate-in fade-in duration-150">
            {/* Modal Card */}
            <div className="relative w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl border border-slate-200/90 text-slate-800">
                {/* Nút đóng (X) */}
                <button
                    type="button"
                    onClick={onClose}
                    className="absolute right-5 top-5 rounded-xl p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
                >
                    <X className="h-5 w-5" />
                </button>

                {/* Header: Avatar + Tên + Mã NV */}
                <div className="flex items-start gap-3.5">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border border-indigo-100 bg-indigo-50 text-indigo-600 shadow-2xs">
                        <User className="h-6 w-6" />
                    </div>
                    <div className="min-w-0 pr-6">
                        <h3 className="text-base font-bold text-slate-900 leading-snug truncate">
                            {employee.fullName || "—"}
                        </h3>
                        <p className="text-xs text-slate-500 font-mono font-medium mt-0.5">
                            {employee.employeeCode || employee.id || "EMP-001"}
                        </p>
                    </div>
                </div>

                {/* Badge Trạng thái */}
                <div className="mt-3 ml-[62px]">
                    <span
                        className={`inline-block rounded-full px-2.5 py-0.5 text-[10px] font-bold border ${
                            isActive
                                ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                                : "bg-rose-50 text-rose-700 border-rose-200"
                        }`}
                    >
                        {isActive ? "Đang hoạt động" : "Đã khóa"}
                    </span>
                </div>

                {/* Khung chứa thông tin chi tiết */}
                <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50/70 p-4 text-xs space-y-2.5 text-slate-700">
                    <div className="flex items-center justify-between">
                        <span className="font-semibold text-slate-500">Tên đăng nhập:</span>
                        <span className="font-bold text-slate-900">@{employee.username || "chưa gán"}</span>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="font-semibold text-slate-500">Email:</span>
                        <span className="font-medium text-slate-800 truncate max-w-[180px]">{employee.email || "—"}</span>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="font-semibold text-slate-500">Đơn vị tổ chức:</span>
                        <span className="font-medium text-slate-800 truncate max-w-[180px]">{employee.department || "—"}</span>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="font-semibold text-slate-500">Vai trò (Role):</span>
                        <span className="font-bold text-indigo-700">{employee.roleName || employee.roleCode || "Nhân viên chuyên môn"}</span>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="font-semibold text-slate-500">Phạm vi dữ liệu:</span>
                        <span className="font-medium text-slate-800">{getScopeLabel(employee.dataScope)}</span>
                    </div>

                    {employee.phone && (
                        <div className="flex items-center justify-between">
                            <span className="font-semibold text-slate-500">Số điện thoại:</span>
                            <span className="font-medium text-slate-800">{employee.phone}</span>
                        </div>
                    )}

                    {employee.joinDate && (
                        <div className="flex items-center justify-between">
                            <span className="font-semibold text-slate-500">Ngày tham gia:</span>
                            <span className="font-medium text-slate-800">{employee.joinDate}</span>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}