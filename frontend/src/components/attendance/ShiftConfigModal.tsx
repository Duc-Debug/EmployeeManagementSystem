"use client"

import { useState, useEffect } from "react"
import { SlidersHorizontal, X } from "lucide-react"

export interface ShiftRulesData {
    startTime: string
    endTime: string
    lunchBreak: string
}

interface ShiftConfigModalProps {
    isOpen: boolean
    onClose: () => void
    rules: ShiftRulesData
    onSave: (newRules: ShiftRulesData) => void
}

export function ShiftConfigModal({ isOpen, onClose, rules, onSave }: ShiftConfigModalProps) {
    const [formData, setFormData] = useState<ShiftRulesData>(rules)

    useEffect(() => {
        setFormData(rules)
    }, [rules, isOpen])

    if (!isOpen) return null

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault()
        onSave(formData)
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm transition-all animate-in fade-in duration-200">
            {/* Modal Card - Thiết kế chuẩn theo Hình 2 */}
            <div className="w-full max-w-md overflow-hidden rounded-2xl bg-white p-6 shadow-2xl transition-all">
                {/* Header */}
                <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                    <div className="flex items-center gap-3">
                        <div className="flex size-10 items-center justify-center rounded-xl bg-emerald-100/80 text-emerald-600">
                            <SlidersHorizontal className="size-5" />
                        </div>
                        <h2 className="text-base font-bold text-slate-800">Cấu hình ca làm</h2>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                    >
                        <X className="size-4" />
                    </button>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="mt-5 space-y-4">
                    {/* Giờ bắt đầu */}
                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">
                            Giờ bắt đầu ca
                        </label>
                        <input
                            type="text"
                            value={formData.startTime}
                            onChange={(e) => setFormData({ ...formData, startTime: e.target.value })}
                            placeholder="vd: 08:00 AM"
                            className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-xs font-medium text-slate-800 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            required
                        />
                    </div>

                    {/* Giờ kết thúc */}
                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">
                            Giờ kết thúc ca
                        </label>
                        <input
                            type="text"
                            value={formData.endTime}
                            onChange={(e) => setFormData({ ...formData, endTime: e.target.value })}
                            placeholder="vd: 05:30 PM"
                            className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-xs font-medium text-slate-800 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            required
                        />
                    </div>

                    {/* Nghỉ trưa cố định */}
                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">
                            Khung giờ nghỉ trưa cố định
                        </label>
                        <input
                            type="text"
                            value={formData.lunchBreak}
                            onChange={(e) => setFormData({ ...formData, lunchBreak: e.target.value })}
                            placeholder="vd: 12:00 PM - 01:30 PM"
                            className="w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-xs font-medium text-slate-800 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                            required
                        />
                    </div>

                    {/* Action Buttons - Chuẩn theo Hình 2 */}
                    <div className="mt-6 flex items-center justify-end gap-2.5 pt-2">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 active:scale-95"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-bold text-slate-800 shadow-sm transition hover:bg-slate-50 active:scale-95"
                        >
                            Lưu thay đổi
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default ShiftConfigModal