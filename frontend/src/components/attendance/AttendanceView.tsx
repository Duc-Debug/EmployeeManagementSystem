"use client"

import { useState } from "react"
import {
    BriefcaseBusiness,
    CheckCircle2,
    FileSpreadsheet,
    History,
    SlidersHorizontal,
    UserRoundX,
    Users,
} from "lucide-react"
import type { AttendanceRecord } from "@/lib/hr-data"
import { ClockStation } from "./ClockStation"
import { ShiftRules } from "./ShiftRules"
import { StatCard } from "./StatCard"
import { TimesheetTable } from "./TimesheetTable"
import { ShiftConfigModal, type ShiftRulesData } from "./ShiftConfigModal"

export function AttendanceView({
                                   records,
                                   onClockIn,
                                   onClockOut,
                                   onEditRecord,
                               }: {
    records: AttendanceRecord[]
    onClockIn: () => string
    onClockOut: () => boolean
    onEditRecord: (id: string) => void
}) {
    const present = records.filter((r) => r.status !== "Vắng mặt").length
    const late = records.filter((r) => r.status === "Đi muộn").length
    const absent = records.filter((r) => r.status === "Vắng mặt").length
    const totalOt = records
        .reduce((sum, r) => sum + (Number.parseFloat(r.ot) || 0), 0)
        .toFixed(2)
        .replace(/\.?0+$/, "")

    // State quản lý Modal Cấu hình ca làm
    const [isConfigOpen, setIsConfigOpen] = useState(false)
    const [shiftRules, setShiftRules] = useState<ShiftRulesData>({
        startTime: "08:00 AM",
        endTime: "05:30 PM",
        lunchBreak: "12:00 PM - 01:30 PM",
    })

    const [toast, setToast] = useState<{ message: string; visible: boolean }>({
        message: "",
        visible: false,
    })

    const notify = (message: string) => {
        setToast({ message, visible: true })
        window.setTimeout(() => setToast((prev) => ({ ...prev, visible: false })), 3000)
    }

    const handleSaveShiftRules = (newRules: ShiftRulesData) => {
        setShiftRules(newRules)
        setIsConfigOpen(false)
        notify("Đã cập nhật cấu hình ca làm việc thành công!")
    }

    return (
        <section className="space-y-6">
            {/* Header */}
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                <div>
                    <h1 className="text-2xl font-extrabold tracking-tight text-white text-balance">
                        Quản lý giờ làm việc &amp; Chấm công
                    </h1>
                    <p className="mt-1 text-xs font-semibold text-white/80 sm:text-sm">
                        Theo dõi thời gian vào/ra, tổng số giờ làm, ca làm việc và tăng ca.
                    </p>
                </div>
                <div className="flex items-center gap-2.5">
                    {/* Nút bấm mở Modal */}
                    <button
                        type="button"
                        onClick={() => setIsConfigOpen(true)}
                        className="flex min-h-10 items-center gap-2 rounded-xl border border-white/20 bg-white/10 px-4 py-2 text-xs font-bold text-white backdrop-blur-md transition hover:bg-white/20 active:scale-95"
                    >
                        <SlidersHorizontal className="size-4 text-white" />
                        <span>Cấu hình ca làm</span>
                    </button>
                    <button
                        type="button"
                        onClick={() => notify("Đang trích xuất dữ liệu bảng chấm công ra tệp Excel (.xlsx)...")}
                        className="flex min-h-10 items-center gap-2 rounded-xl border border-emerald-400/40 bg-emerald-500/30 px-4 py-2 text-xs font-bold text-emerald-200 shadow-lg backdrop-blur-md transition hover:bg-emerald-500/40 hover:text-white"
                    >
                        <FileSpreadsheet className="size-4 text-emerald-300" />
                        <span>Xuất báo cáo</span>
                    </button>
                </div>
            </div>

            {/* Thẻ thống kê */}
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
                <StatCard
                    icon={<Users className="size-5 text-blue-200" />}
                    tone="text-blue-200 bg-blue-500/30 border border-blue-400/40"
                    label="Đi làm hôm nay"
                    value={`${present}/${records.length}`}
                />
                <StatCard
                    icon={<History className="size-5 text-amber-200" />}
                    tone="text-amber-200 bg-amber-500/30 border border-amber-400/40"
                    label="Đi muộn / Về sớm"
                    value={String(late)}
                    valueClass="text-amber-300"
                />
                <StatCard
                    icon={<UserRoundX className="size-5 text-rose-200" />}
                    tone="text-rose-200 bg-rose-500/30 border border-rose-400/40"
                    label="Vắng mặt / Nghỉ"
                    value={String(absent)}
                    valueClass="text-rose-300"
                />
                <StatCard
                    icon={<BriefcaseBusiness className="size-5 text-emerald-200" />}
                    tone="text-emerald-200 bg-emerald-500/30 border border-emerald-400/40"
                    label="Tổng giờ tăng ca (OT)"
                    value={`${totalOt || 0} hrs`}
                    valueClass="text-emerald-300"
                />
            </div>

            {/* Trạm chấm công & Quy định ca */}
            <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
                <ClockStation onClockIn={onClockIn} onClockOut={onClockOut} onNotify={notify} />
                <ShiftRules rules={shiftRules} />
            </div>

            {/* Bảng chấm công */}
            <TimesheetTable records={records} onEditRecord={onEditRecord} />

            {/* Modal Cấu hình Ca làm việc */}
            <ShiftConfigModal
                isOpen={isConfigOpen}
                onClose={() => setIsConfigOpen(false)}
                rules={shiftRules}
                onSave={handleSaveShiftRules}
            />

            {/* Toast thông báo */}
            <div
                role="status"
                aria-live="polite"
                className={`pointer-events-none fixed bottom-6 right-6 z-50 transition-all duration-300 ${
                    toast.visible ? "translate-y-0 opacity-100" : "translate-y-20 opacity-0"
                }`}
            >
                <div className="flex items-center gap-3 rounded-xl border border-white/25 bg-slate-950/90 px-4 py-3 text-white shadow-2xl backdrop-blur-xl">
                    <CheckCircle2 className="size-5 text-emerald-400" />
                    <span className="text-xs font-bold">{toast.message}</span>
                </div>
            </div>
        </section>
    )
}

export default AttendanceView