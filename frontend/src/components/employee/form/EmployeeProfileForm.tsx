"use client"

import { useState, useRef, useEffect } from "react"
import { X, Calendar, ChevronDown, ChevronLeft, ChevronRight, Check, UserCheck } from "lucide-react"
import { cn } from "@/lib/utils"
import type { EmployeeFormData } from "./employeeForm.types"
import { DEPARTMENT_OPTIONS } from "./employeeForm.constants"

interface EmployeeProfileFormProps {
    open: boolean
    initialData?: EmployeeFormData | null
    onClose: () => void
    onSave: (data: EmployeeFormData) => void
}

const DEFAULT_FORM_DATA: EmployeeFormData = {
    fullName: "",
    email: "",
    phone: "",
    department: "Kỹ thuật",
    position: "",
    joinDate: "",
    contractEndDate: "",
    standardHoursPerWeek: 40,
    status: "active",
}

/* ========================================================================
   1. CUSTOM SELECT DROPDOWN
   ======================================================================== */
function CustomSelect({
                          value,
                          onChange,
                          options,
                          placeholder = "Chọn phòng ban",
                      }: {
    value: string
    onChange: (val: string) => void
    options: string[]
    placeholder?: string
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

    return (
        <div className="relative w-full" ref={ref}>
            <button
                type="button"
                onClick={() => setOpen((o) => !o)}
                className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-2.5 text-xs font-semibold text-slate-800 outline-none transition hover:bg-slate-100/70 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10"
            >
                <span className={value ? "text-slate-800" : "text-slate-400"}>
                    {value || placeholder}
                </span>
                <ChevronDown className={cn("size-4 text-slate-400 transition-transform duration-200", open && "rotate-180")} />
            </button>

            {open && (
                <div className="absolute left-0 right-0 z-50 mt-1.5 overflow-hidden rounded-2xl border border-slate-100 bg-white p-1.5 shadow-xl animate-in fade-in zoom-in-95 duration-150">
                    <div className="max-h-52 overflow-y-auto space-y-1 custom-scrollbar">
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
                                        "flex w-full items-center justify-between rounded-xl px-3.5 py-2 text-left text-xs font-semibold transition-all",
                                        isActive
                                            ? "bg-indigo-50 text-indigo-600 font-bold"
                                            : "text-slate-700 hover:bg-slate-100"
                                    )}
                                >
                                    <span>{opt}</span>
                                    {isActive && <Check className="size-3.5 text-indigo-600 stroke-[2.5]" />}
                                </button>
                            )
                        })}
                    </div>
                </div>
            )}
        </div>
    )
}

/* ========================================================================
   2. CUSTOM DATE PICKER
   ======================================================================== */
function CustomDatePicker({
                              value,
                              onChange,
                              dropUp = true,
                          }: {
    value: string
    onChange: (val: string) => void
    dropUp?: boolean
}) {
    const [open, setOpen] = useState(false)
    const ref = useRef<HTMLDivElement>(null)

    const today = new Date()
    const [viewDate, setViewDate] = useState(() => {
        if (!value) return today
        const parts = value.split("/")
        if (parts.length === 3) {
            return new Date(Number(parts[2]), Number(parts[1]) - 1, Number(parts[0]))
        }
        return today
    })

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (ref.current && !ref.current.contains(e.target as Node)) {
                setOpen(false)
            }
        }
        document.addEventListener("mousedown", handleClickOutside)
        return () => document.removeEventListener("mousedown", handleClickOutside)
    }, [])

    const year = viewDate.getFullYear()
    const month = viewDate.getMonth()

    const daysInMonth = new Date(year, month + 1, 0).getDate()
    const firstDayIndex = new Date(year, month, 1).getDay()

    const monthNames = [
        "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
        "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    ]
    const weekDays = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"]

    const handlePrevMonth = () => setViewDate(new Date(year, month - 1, 1))
    const handleNextMonth = () => setViewDate(new Date(year, month + 1, 1))

    const handleSelectDay = (day: number) => {
        const d = String(day).padStart(2, "0")
        const m = String(month + 1).padStart(2, "0")
        const formatted = `${d}/${m}/${year}`
        onChange(formatted)
        setOpen(false)
    }

    return (
        <div className="relative w-full" ref={ref}>
            <button
                type="button"
                onClick={() => setOpen((o) => !o)}
                className="flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-2.5 text-xs font-semibold text-slate-800 outline-none transition hover:bg-slate-100/70 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10"
            >
                <span className={value ? "text-slate-800" : "text-slate-400"}>
                    {value || "dd/mm/yyyy"}
                </span>
                <Calendar className="size-4 text-slate-400" />
            </button>

            {open && (
                <div
                    className={cn(
                        "absolute left-0 z-50 w-72 rounded-2xl border border-slate-100 bg-white p-4 shadow-2xl animate-in fade-in zoom-in-95 duration-150",
                        dropUp ? "bottom-full mb-2" : "top-full mt-2"
                    )}
                >
                    <div className="flex items-center justify-between mb-3 text-slate-800">
                        <button
                            type="button"
                            onClick={handlePrevMonth}
                            className="rounded-lg p-1 hover:bg-slate-100 text-slate-500 hover:text-slate-800 transition"
                        >
                            <ChevronLeft className="size-4" />
                        </button>
                        <span className="text-xs font-bold">{monthNames[month]} {year}</span>
                        <button
                            type="button"
                            onClick={handleNextMonth}
                            className="rounded-lg p-1 hover:bg-slate-100 text-slate-500 hover:text-slate-800 transition"
                        >
                            <ChevronRight className="size-4" />
                        </button>
                    </div>

                    <div className="grid grid-cols-7 gap-1 text-center text-[10px] font-bold text-slate-400 mb-2">
                        {weekDays.map((wd) => (
                            <span key={wd}>{wd}</span>
                        ))}
                    </div>

                    <div className="grid grid-cols-7 gap-1 text-center text-xs">
                        {Array.from({ length: firstDayIndex }).map((_, i) => (
                            <div key={`empty-${i}`} />
                        ))}
                        {Array.from({ length: daysInMonth }).map((_, i) => {
                            const dayNum = i + 1
                            const dStr = String(dayNum).padStart(2, "0")
                            const mStr = String(month + 1).padStart(2, "0")
                            const isSelected = value === `${dStr}/${mStr}/${year}`

                            return (
                                <button
                                    key={dayNum}
                                    type="button"
                                    onClick={() => handleSelectDay(dayNum)}
                                    className={cn(
                                        "flex size-7 items-center justify-center rounded-lg text-xs font-semibold transition-all mx-auto",
                                        isSelected
                                            ? "bg-indigo-600 text-white font-bold shadow-md shadow-indigo-200"
                                            : "text-slate-700 hover:bg-slate-100"
                                    )}
                                >
                                    {dayNum}
                                </button>
                            )}
                        )}
                    </div>
                </div>
            )}
        </div>
    )
}

/* ========================================================================
   3. MODAL FORM CHÍNH
   ======================================================================== */
export default function EmployeeProfileForm({
                                                open,
                                                initialData,
                                                onClose,
                                                onSave,
                                            }: EmployeeProfileFormProps) {
    const [formData, setFormData] = useState<EmployeeFormData>(DEFAULT_FORM_DATA)
    const [prevInitialData, setPrevInitialData] = useState<EmployeeFormData | null | undefined>(initialData)
    const [prevOpen, setPrevOpen] = useState<boolean>(open)

    // Khắc phục ESLint warning (react-hooks/set-state-in-effect):
    // Đồng bộ state trực tiếp trong quá trình render khi props initialData / open thay đổi
    if (open !== prevOpen || initialData !== prevInitialData) {
        setPrevOpen(open)
        setPrevInitialData(initialData)
        setFormData(initialData ? { ...initialData } : DEFAULT_FORM_DATA)
    }

    if (!open) return null

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault()
        onSave(formData)
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="relative w-full max-w-lg rounded-3xl border border-slate-100 bg-white p-6 shadow-2xl">
                {/* Header Modal */}
                <div className="flex items-center justify-between pb-5">
                    <div className="flex items-center gap-3">
                        <div className="flex size-10 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600">
                            <UserCheck className="size-5" />
                        </div>
                        <h2 className="text-base font-bold text-slate-800">
                            {initialData ? "Chỉnh sửa hồ sơ nhân sự" : "Thêm hồ sơ nhân sự mới"}
                        </h2>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-xl p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                    >
                        <X className="size-4" />
                    </button>
                </div>

                {/* Form Body */}
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="space-y-1.5">
                        <label className="text-xs font-medium text-slate-600">Họ và tên *</label>
                        <input
                            type="text"
                            required
                            placeholder="vd: Nguyễn Văn A"
                            value={formData.fullName}
                            onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                            className="w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-2.5 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10"
                        />
                    </div>

                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                        <div className="space-y-1.5">
                            <label className="text-xs font-medium text-slate-600">Email *</label>
                            <input
                                type="email"
                                required
                                placeholder="example@company.com"
                                value={formData.email}
                                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                                className="w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-2.5 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10"
                            />
                        </div>
                        <div className="space-y-1.5">
                            <label className="text-xs font-medium text-slate-600">Số điện thoại</label>
                            <input
                                type="text"
                                placeholder="0912 345 678"
                                value={formData.phone || ""}
                                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                                className="w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-2.5 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10"
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                        <div className="space-y-1.5">
                            <label className="text-xs font-medium text-slate-600">Phòng ban *</label>
                            <CustomSelect
                                value={formData.department}
                                onChange={(val) => setFormData({ ...formData, department: val })}
                                options={DEPARTMENT_OPTIONS.filter((d) => d !== "All")}
                            />
                        </div>

                        <div className="space-y-1.5">
                            <label className="text-xs font-medium text-slate-600">Chức danh *</label>
                            <input
                                type="text"
                                required
                                placeholder="vd: HR Specialist"
                                value={formData.position}
                                onChange={(e) => setFormData({ ...formData, position: e.target.value })}
                                className="w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-2.5 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10"
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                        <div className="space-y-1.5">
                            <label className="text-xs font-medium text-slate-600">Ngày vào làm *</label>
                            <CustomDatePicker
                                value={formData.joinDate || ""}
                                onChange={(val) => setFormData({ ...formData, joinDate: val })}
                                dropUp={true}
                            />
                        </div>

                        <div className="space-y-1.5">
                            <label className="text-xs font-medium text-slate-600">Ngày kết thúc HĐLĐ</label>
                            <CustomDatePicker
                                value={formData.contractEndDate || ""}
                                onChange={(val) => setFormData({ ...formData, contractEndDate: val })}
                                dropUp={true}
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                        <div className="space-y-1.5">
                            <label className="text-xs font-medium text-slate-600">Giờ chuẩn / tuần (giờ)</label>
                            <input
                                type="number"
                                min={1}
                                max={168}
                                placeholder="40"
                                value={formData.standardHoursPerWeek ?? 40}
                                onChange={(e) =>
                                    setFormData({
                                        ...formData,
                                        standardHoursPerWeek: Number(e.target.value) || 0,
                                    })
                                }
                                className="w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-2.5 text-xs font-semibold text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/10"
                            />
                        </div>

                        <div className="space-y-1.5">
                            <label className="text-xs font-medium text-slate-600">Trạng thái</label>
                            <CustomSelect
                                value={formData.status === "active" ? "Đang làm việc" : "Tạm khóa"}
                                onChange={(val) =>
                                    setFormData({
                                        ...formData,
                                        status: val === "Đang làm việc" ? "active" : "locked",
                                    })
                                }
                                options={["Đang làm việc", "Tạm khóa"]}
                            />
                        </div>
                    </div>

                    {/* Footer Action Buttons */}
                    <div className="flex items-center justify-end gap-3 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-2xl border border-slate-200 bg-white px-5 py-2.5 text-xs font-semibold text-slate-600 transition hover:bg-slate-50"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            className="rounded-2xl border border-slate-200 bg-white px-5 py-2.5 text-xs font-semibold text-slate-800 transition hover:bg-slate-50 hover:border-slate-300 active:scale-95"
                        >
                            {initialData ? "Lưu thay đổi" : "Tạo hồ sơ"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}