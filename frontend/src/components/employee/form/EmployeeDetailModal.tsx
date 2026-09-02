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

    const isActive = employee.status !== "locked";

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm">
            {/* Modal Card */}
            <div className="relative w-full max-w-sm rounded-[24px] bg-white p-6 shadow-xl transition-all">
                {/* Nút đóng (X) */}
                <button
                    type="button"
                    onClick={onClose}
                    className="absolute right-5 top-5 text-slate-500 transition hover:text-slate-800"
                >
                    <X className="h-5 w-5" />
                </button>

                {/* Header: Avatar + Tên + Mã NV */}
                <div className="flex items-start gap-3.5">
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-[#0088CC] text-white shadow-sm">
                        <User className="h-7 w-7" />
                    </div>
                    <div>
                        <h3 className="text-base font-bold text-slate-900 leading-snug">
                            {employee.fullName || "—"}
                        </h3>
                        <p className="text-xs text-slate-500 font-medium mt-0.5">
                            {employee.id || "EMP-001"}
                        </p>
                    </div>
                </div>

                {/* Badge Trạng thái */}
                <div className="mt-3 ml-[60px]">
                    <span
                        className={`inline-block rounded-full px-3 py-1 text-[11px] font-semibold text-white ${
                            isActive ? "bg-[#4CAF50]" : "bg-rose-500"
                        }`}
                    >
                        {isActive ? "Đang hoạt động" : "Đã khóa"}
                    </span>
                </div>

                {/* Khung xám chứa thông tin chi tiết */}
                <div className="mt-5 rounded-2xl bg-[#EFEFEF] p-4 text-xs space-y-2.5 text-slate-800">
                    <div className="flex items-center justify-between">
                        <span className="font-medium text-slate-700">email:</span>
                        <span className="font-normal text-slate-900">{employee.email || "—"}</span>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="font-medium text-slate-700">Phòng ban:</span>
                        <span className="font-normal text-slate-900">{employee.department || "—"}</span>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="font-medium text-slate-700">Chức danh:</span>
                        <span className="font-normal text-slate-900">{employee.position || "—"}</span>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="font-medium text-slate-700">Số điện thoại:</span>
                        <span className="font-normal text-slate-900">{employee.phone || "0999999999"}</span>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="font-medium text-slate-700">Ngày vào làm:</span>
                        <span className="font-normal text-slate-900">{employee.joinDate || "—"}</span>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="font-medium text-slate-700">Ngày kết thúc HĐLĐ:</span>
                        <span className="font-normal text-slate-900">{employee.contractEndDate || "Không thời hạn"}</span>
                    </div>

                    <div className="flex items-center justify-between">
                        <span className="font-medium text-slate-700">Giờ chuẩn / tuần:</span>
                        <span className="font-normal text-slate-900">{employee.standardHoursPerWeek ? `${employee.standardHoursPerWeek} giờ` : "40 giờ"}</span>
                    </div>
                </div>
            </div>
        </div>
    );
}