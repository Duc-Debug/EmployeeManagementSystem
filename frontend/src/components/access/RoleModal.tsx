import React, { useState, useRef, useEffect } from "react";
import { ShieldCheck, X, ChevronDown, Check } from "lucide-react";
import { cn } from "@/lib/utils";
import { THEME_OPTIONS, THEME_SOLID_BG, type ThemeOption } from "./access.constants";
import type { Department, RoleBasicInfo } from "./access.types";

interface RoleModalProps {
    open: boolean;
    initialData?: RoleBasicInfo | null;
    departments: Department[];
    onClose: () => void;
    onSave: (data: RoleBasicInfo) => void;
}

const emptyForm: RoleBasicInfo = { name: "", description: "", theme: "blue", departmentId: "all", isSystemRole: false };

export default function RoleModal({ open, initialData, departments, onClose, onSave }: RoleModalProps) {
    const [form, setForm] = useState<RoleBasicInfo>(initialData ?? emptyForm);
    const [error, setError] = useState("");
    const [deptOpen, setDeptOpen] = useState(false);
    const deptRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (deptRef.current && !deptRef.current.contains(e.target as Node)) {
                setDeptOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const [wasOpen, setWasOpen] = useState(open);
    if (open !== wasOpen) {
        setWasOpen(open);
        if (open) {
            setForm(initialData ?? emptyForm);
            setError("");
            setDeptOpen(false);
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
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">Thuộc phòng ban</label>
                        <div className="relative" ref={deptRef}>
                            <button
                                type="button"
                                onClick={() => setDeptOpen((prev) => !prev)}
                                className="flex w-full items-center justify-between rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm font-medium text-slate-800 transition hover:bg-white focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                            >
                                <span className="truncate">
                                    {form.departmentId === "all"
                                        ? "Tất cả phòng ban (Toàn công ty)"
                                        : departments.find((d) => d.id === form.departmentId)?.name ?? "Tất cả phòng ban (Toàn công ty)"}
                                </span>
                                <ChevronDown className={cn("h-4 w-4 text-slate-400 shrink-0 transition-transform duration-200", deptOpen && "rotate-180 text-indigo-600")} />
                            </button>

                            {deptOpen && (
                                <div className="absolute left-0 top-full z-50 mt-1.5 max-h-60 w-full overflow-y-auto rounded-xl border border-slate-200 bg-white p-1.5 shadow-xl ring-1 ring-black/5 animate-in fade-in-50 zoom-in-95 [scrollbar-width:thin]">
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setForm({ ...form, departmentId: "all" });
                                            setDeptOpen(false);
                                        }}
                                        className={cn(
                                            "flex w-full items-center justify-between rounded-lg px-3 py-2 text-xs font-medium transition text-left",
                                            form.departmentId === "all"
                                                ? "bg-indigo-50 text-indigo-700 font-semibold"
                                                : "text-slate-700 hover:bg-slate-100 hover:text-slate-900"
                                        )}
                                    >
                                        <span>Tất cả phòng ban (Toàn công ty)</span>
                                        {form.departmentId === "all" && <Check className="h-3.5 w-3.5 text-indigo-600 shrink-0 ml-2" />}
                                    </button>

                                    {departments.map((dept) => {
                                        const isSelected = form.departmentId === dept.id;
                                        return (
                                            <button
                                                key={dept.id}
                                                type="button"
                                                onClick={() => {
                                                    setForm({ ...form, departmentId: dept.id });
                                                    setDeptOpen(false);
                                                }}
                                                className={cn(
                                                    "flex w-full items-center justify-between rounded-lg px-3 py-2 text-xs font-medium transition text-left",
                                                    isSelected
                                                        ? "bg-indigo-50 text-indigo-700 font-semibold"
                                                        : "text-slate-700 hover:bg-slate-100 hover:text-slate-900"
                                                )}
                                            >
                                                <span className="truncate">{dept.name}</span>
                                                {isSelected && <Check className="h-3.5 w-3.5 text-indigo-600 shrink-0 ml-2" />}
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
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

                    <label className="flex items-center gap-2 pt-1 text-xs font-medium text-slate-600">
                        <input
                            type="checkbox"
                            checked={Boolean(form.isSystemRole)}
                            onChange={(e) => setForm({ ...form, isSystemRole: e.target.checked })}
                            className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
                        />
                        Đánh dấu là vai trò hệ thống
                    </label>

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