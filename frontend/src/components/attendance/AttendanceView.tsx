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

import { useAuthUser } from "@/lib/auth-session"

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
    const user = useAuthUser()
    const roleCode = user?.roleCode?.toUpperCase().replace(/_/g, "-") || ""
    const isHR = roleCode === "VT-05" || roleCode === "VT-06"
    const isEmployee = roleCode === "VT-04"
    const isDirector = roleCode === "VT-01"

    // Lọc danh sách bản ghi chấm công: Nếu là VT-04 thì chỉ xem bản ghi của chính mình
    const currentUserIdStr = user?.id != null ? String(user.id) : ""
    const currentUserName = (user?.fullName || user?.username || "").trim().toLowerCase()
    const currentUserCode = (user?.employeeCode || "").trim().toLowerCase()

    const displayedRecords = isEmployee
        ? (() => {
              const filtered = records.filter((r) => {
                  const rId = (r.id || "").trim().toLowerCase()
                  const rName = (r.name || "").trim().toLowerCase()
                  return (
                      (currentUserIdStr && rId === currentUserIdStr.toLowerCase()) ||
                      (currentUserCode && rId === currentUserCode) ||
                      (currentUserName && (rName === currentUserName || rName.includes(currentUserName)))
                  )
              })
              if (filtered.length > 0) return filtered
              return [
                  {
                      id: user?.employeeCode || (currentUserIdStr ? `NV-${currentUserIdStr}` : "NV001"),
                      name: user?.fullName || user?.username || "Tôi (Nhân viên)",
                      dept: user?.orgUnitName || "Phòng chuyên môn",
                      inTime: "--:--",
                      outTime: "--:--",
                      hours: "0",
                      ot: "0",
                      status: "Vắng mặt" as const,
                  },
              ]
          })()
        : records

    const present = displayedRecords.filter((r) => r.status !== "Vắng mặt").length
    const late = displayedRecords.filter((r) => r.status === "Đi muộn").length
    const absent = displayedRecords.filter((r) => r.status === "Vắng mặt").length
    const totalOt = displayedRecords
        .reduce((sum, r) => sum + (Number.parseFloat(r.ot) || 0), 0)
        .toFixed(2)
        .replace(/\.?0+$/, "")

    const myRecord = displayedRecords[0]
    const isPresent = myRecord && myRecord.status !== "Vắng mặt"

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
                    <h1 className="text-2xl font-extrabold tracking-tight text-slate-900 text-balance">
                        Quản lý giờ làm việc &amp; Chấm công
                    </h1>
                    <p className="mt-1 text-xs font-semibold text-slate-500 sm:text-sm">
                        {isEmployee
                            ? "Theo dõi thời gian vào/ra và kiểm tra số giờ làm việc của bạn."
                            : "Theo dõi thời gian vào/ra, tổng số giờ làm, ca làm việc và tăng ca."}
                    </p>
                </div>
                <div className="flex items-center gap-2.5">
                    {/* Nút bấm mở Modal Cấu hình ca làm: Chỉ VT-05 (HR) hoặc Quản trị viên */}
                    {isHR && (
                        <button
                            type="button"
                            onClick={() => setIsConfigOpen(true)}
                            className="flex min-h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 shadow-xs transition hover:bg-slate-50 hover:text-slate-900 active:scale-95 cursor-pointer"
                        >
                            <SlidersHorizontal className="size-4 text-slate-500" />
                            <span>Cấu hình ca làm</span>
                        </button>
                    )}
                    <button
                        type="button"
                        onClick={() => notify("Đang trích xuất dữ liệu bảng chấm công ra tệp Excel (.xlsx)...")}
                        className="flex min-h-10 items-center gap-2 rounded-xl border border-emerald-600 bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-emerald-700 cursor-pointer"
                    >
                        <FileSpreadsheet className="size-4 text-white" />
                        <span>Xuất báo cáo</span>
                    </button>
                </div>
            </div>

            {/* Thẻ thống kê */}
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
                {isEmployee ? (
                    <>
                        <StatCard
                            icon={<Users className="size-5 text-blue-600" />}
                            tone={isPresent ? "bg-emerald-50 border border-emerald-200" : "bg-blue-50 border border-blue-200"}
                            label="Trạng thái hôm nay"
                            value={isPresent ? "Đã check-in" : "Chưa chấm công"}
                            valueClass={isPresent ? "text-emerald-700 text-sm sm:text-base font-bold" : "text-blue-700 text-sm sm:text-base font-bold"}
                        />
                        <StatCard
                            icon={<History className="size-5 text-amber-600" />}
                            tone="bg-indigo-50 border border-indigo-200"
                            label="Giờ Vào / Ra hôm nay"
                            value={`${myRecord?.inTime || "--:--"} / ${myRecord?.outTime || "--:--"}`}
                            valueClass="text-indigo-700 text-sm sm:text-base font-bold font-mono"
                        />
                        <StatCard
                            icon={<UserRoundX className="size-5 text-rose-600" />}
                            tone="bg-sky-50 border border-sky-200"
                            label="Số giờ làm việc"
                            value={`${myRecord?.hours || "0"} hrs`}
                            valueClass="text-sky-700"
                        />
                        <StatCard
                            icon={<BriefcaseBusiness className="size-5 text-emerald-600" />}
                            tone="bg-emerald-50 border border-emerald-200"
                            label="Tăng ca (OT)"
                            value={`${myRecord?.ot || "0"} hrs`}
                            valueClass="text-emerald-600"
                        />
                    </>
                ) : (
                    <>
                        <StatCard
                            icon={<Users className="size-5 text-blue-600" />}
                            tone="bg-blue-50 border border-blue-200"
                            label="Đi làm hôm nay"
                            value={`${present}/${records.length}`}
                        />
                        <StatCard
                            icon={<History className="size-5 text-amber-600" />}
                            tone="bg-amber-50 border border-amber-200"
                            label="Đi muộn / Về sớm"
                            value={String(late)}
                            valueClass="text-amber-600"
                        />
                        <StatCard
                            icon={<UserRoundX className="size-5 text-rose-600" />}
                            tone="bg-rose-50 border border-rose-200"
                            label="Vắng mặt / Nghỉ"
                            value={String(absent)}
                            valueClass="text-rose-600"
                        />
                        <StatCard
                            icon={<BriefcaseBusiness className="size-5 text-emerald-600" />}
                            tone="bg-emerald-50 border border-emerald-200"
                            label="Tổng giờ tăng ca (OT)"
                            value={`${totalOt || 0} hrs`}
                            valueClass="text-emerald-600"
                        />
                    </>
                )}
            </div>

            {/* Trạm chấm công & Quy định ca: Ban Giám Đốc (VT-01) không nộp chấm công */}
            <div className={`grid grid-cols-1 gap-5 ${isDirector ? "lg:grid-cols-1" : "lg:grid-cols-3"}`}>
                {!isDirector && (
                    <ClockStation onClockIn={onClockIn} onClockOut={onClockOut} onNotify={notify} />
                )}
                <div className={isDirector ? "w-full" : "lg:col-span-2"}>
                    <ShiftRules rules={shiftRules} />
                </div>
            </div>

            {/* Bảng chấm công */}
            <TimesheetTable
                records={displayedRecords}
                onEditRecord={onEditRecord}
                canEdit={!isEmployee}
                title={isEmployee ? "Bảng chấm công cá nhân của bạn" : "Bảng tổng hợp chấm công ngày hôm nay"}
            />

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