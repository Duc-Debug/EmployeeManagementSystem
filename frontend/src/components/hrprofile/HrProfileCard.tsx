import { User, Mail, Building2, Briefcase, CalendarDays, Clock, Pencil, Trash2 } from "lucide-react";
import type { HrProfileData } from "./hrprofile.types";

interface HrProfileCardProps {
    profile: HrProfileData;
    canManage?: boolean;
    onEdit: (profile: HrProfileData) => void;
    onDelete: (id: string) => void;
}

function formatDate(dateStr?: string): string | null {
    if (!dateStr) return null;
    try {
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        return d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
    } catch {
        return dateStr;
    }
}

export default function HrProfileCard({ profile, canManage = false, onEdit, onDelete }: HrProfileCardProps) {
    const joinDateDisplay = formatDate(profile.startDate);
    const contractEndDisplay = formatDate(profile.contractEndDate);

    return (
        <div className="group flex flex-col justify-between gap-4 rounded-xl border border-slate-200/80 bg-slate-50/60 p-3.5 transition hover:border-indigo-300 hover:bg-white hover:shadow-xs sm:flex-row sm:items-center">
            {/* Col 1: Icon + Name + Code */}
            <div className="flex w-full sm:w-[220px] sm:flex-none items-center gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-xl border border-emerald-100 bg-emerald-50 text-emerald-600 shadow-xs">
                    <User className="size-5" />
                </div>
                <div className="min-w-0">
                    <h3 className="truncate text-sm font-bold text-slate-900 group-hover:text-indigo-600 transition">
                        {profile.fullName}
                    </h3>
                    <p className="font-mono text-[11px] font-semibold text-slate-500">{profile.employeeCode}</p>
                </div>
            </div>

            {/* Col 2: Email, Department, Role */}
            <div className="grid flex-1 grid-cols-1 gap-2 text-xs font-medium text-slate-600 sm:grid-cols-3 sm:px-4">
                <span className="flex items-center gap-1.5 truncate">
                    <Mail className="size-3.5 shrink-0 text-slate-400" />
                    {profile.email ? (
                        <span className="truncate font-mono">{profile.email}</span>
                    ) : (
                        <span className="italic font-normal text-slate-400">Chưa có email</span>
                    )}
                </span>
                <span className="flex items-center gap-1.5 truncate">
                    <Building2 className="size-3.5 shrink-0 text-slate-400" />
                    <span className="truncate">{profile.department}</span>
                </span>
                <span className="flex items-center gap-1.5 truncate">
                    <Briefcase className="size-3.5 shrink-0 text-slate-400" />
                    <span className="truncate font-semibold text-slate-800">
                        {profile.professionalRole || "Chưa xác định"}
                    </span>
                </span>
            </div>

            {/* Col 3: Dates + Hours */}
            <div className="flex w-full sm:w-[190px] sm:flex-none flex-col gap-1 items-start sm:items-end">
                <span className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-semibold text-slate-600 shadow-2xs">
                    <CalendarDays className="size-3 text-slate-400" />
                    {joinDateDisplay ? (
                        <span>Vào: {joinDateDisplay}</span>
                    ) : (
                        <span className="italic font-normal text-slate-400">Chưa có ngày</span>
                    )}
                </span>
                {contractEndDisplay && (
                    <span className="flex items-center gap-1.5 rounded-lg border border-amber-200 bg-amber-50 px-2.5 py-1 text-[11px] font-semibold text-amber-700">
                        <CalendarDays className="size-3" />
                        <span>HĐ đến: {contractEndDisplay}</span>
                    </span>
                )}
                <span className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-semibold text-slate-600 shadow-2xs">
                    <Clock className="size-3 text-slate-400" />
                    <span>{profile.standardHoursPerWeek}h/tuần</span>
                </span>
            </div>

            {/* Col 4: Actions (if canManage) */}
            {canManage && (
                <div className="flex w-[60px] flex-none items-center justify-end gap-1">
                    <button
                        type="button"
                        onClick={() => onEdit(profile)}
                        className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                        title="Chỉnh sửa"
                    >
                        <Pencil className="size-4" />
                    </button>
                    <button
                        type="button"
                        onClick={() => onDelete(profile.id)}
                        className="rounded-lg p-1.5 text-slate-400 transition hover:bg-rose-50 hover:text-rose-600"
                        title="Xóa"
                    >
                        <Trash2 className="size-4" />
                    </button>
                </div>
            )}
        </div>
    );
}
