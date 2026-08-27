import React, { useState } from "react";
import { ShieldCheck, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { THEME_OPTIONS, THEME_SOLID_BG, type ThemeOption } from "./access.constants";
import type { RoleBasicInfo } from "./access.types";

interface RoleModalProps {
    open: boolean;
    initialData?: RoleBasicInfo | null;
    onClose: () => void;
    onSave: (data: RoleBasicInfo) => void;
}

const emptyForm: RoleBasicInfo = { name: "", description: "", theme: "blue" };

export default function RoleModal({ open, initialData, onClose, onSave }: RoleModalProps) {
    const [form, setForm] = useState<RoleBasicInfo>(initialData ?? emptyForm);
    const [error, setError] = useState("");

    // Reset the form on the closed -> open transition (rather than
    // deriving it from props on every render), so opening it twice in a
    // row with different initialData always starts from a clean state.
    // This is done during render — React's "adjusting state when a prop
    // changes" pattern — instead of a useEffect, so there's no extra
    // commit/render pass and no flash of the previous form's values.
    const [wasOpen, setWasOpen] = useState(open);
    if (open !== wasOpen) {
        setWasOpen(open);
        if (open) {
            setForm(initialData ?? emptyForm);
            setError("");
        }
    }

    const isEdit = Boolean(initialData?.id);

    if (!open) return null;

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!form.name.trim()) {
            setError("Vui lòng nhập tên vai trò.");
            return;
        }
        onSave({ ...form, id: initialData?.id, name: form.name.trim(), description: form.description.trim() });
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity" onClick={onClose} />

            <div className="relative w-full max-w-md rounded-2xl border border-slate-100 bg-white p-6 shadow-2xl transition-all">
                <div className="mb-5 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 border border-indigo-100">
                            <ShieldCheck className="h-4.5 w-4.5 text-indigo-600" />
                        </span>
                        <h2 className="text-base font-bold text-slate-800">{isEdit ? "Sửa vai trò" : "Thêm vai trò"}</h2>
                    </div>
                    <button onClick={onClose} className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600">
                        <X className="h-4 w-4" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">Tên vai trò</label>
                        <input
                            autoFocus
                            value={form.name}
                            onChange={(e) => setForm({ ...form, name: e.target.value })}
                            placeholder="VD: Trưởng phòng"
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>

                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">Mô tả</label>
                        <textarea
                            value={form.description}
                            onChange={(e) => setForm({ ...form, description: e.target.value })}
                            placeholder="Mô tả ngắn về trách nhiệm của vai trò này"
                            rows={2}
                            className="w-full resize-none rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>

                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">Màu nhận diện</label>
                        <div className="flex flex-wrap gap-2">
                            {THEME_OPTIONS.map((opt: ThemeOption) => {
                                const isActive = form.theme === opt.key;
                                return (
                                    <button
                                        key={opt.key}
                                        type="button"
                                        onClick={() => setForm({ ...form, theme: opt.key })}
                                        className={cn(
                                            "rounded-full border px-3 py-1.5 text-[11px] font-semibold transition",
                                            isActive
                                                ? `${THEME_SOLID_BG[opt.key]} border-transparent text-white`
                                                : "border-slate-200 bg-slate-50 text-slate-500 hover:border-slate-300"
                                        )}
                                    >
                                        {opt.label}
                                    </button>
                                );
                            })}
                        </div>
                    </div>

                    {!isEdit && (
                        <p className="rounded-xl border border-indigo-100 bg-indigo-50/70 px-3.5 py-2.5 text-[11px] leading-relaxed text-indigo-700">
                            Sau khi tạo, bạn có thể thiết lập phạm vi dữ liệu và ma trận quyền chi tiết cho vai trò ở màn hình chính.
                        </p>
                    )}

                    {error && <p className="text-xs font-medium text-rose-500">{error}</p>}

                    <div className="flex justify-end gap-2 pt-2">
                        <button
                            type="button"
                            onClick={onClose}
                            className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-semibold text-slate-600 transition hover:bg-slate-50 hover:text-slate-900"
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            className="rounded-xl bg-indigo-600 px-4 py-2.5 text-xs font-semibold text-white shadow-md shadow-indigo-200 transition hover:bg-indigo-500"
                        >
                            {isEdit ? "Lưu thay đổi" : "Tạo vai trò"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}