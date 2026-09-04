import { useState } from "react";
import { X, FolderKanban } from "lucide-react";

export interface Department {
    id: string;
    name: string;
    manager: string;
    count: number;
}

interface DepartmentModalProps {
    open: boolean;
    initialData?: Department | null;
    onClose: () => void;
    onSave: (dept: Department) => void;
}

const emptyForm = { name: "", manager: "", count: "" };

function formFromInitialData(initialData?: Department | null) {
    return initialData
        ? { name: initialData.name, manager: initialData.manager, count: String(initialData.count) }
        : emptyForm;
}

export default function DepartmentModal({ open, initialData, onClose, onSave }: DepartmentModalProps) {
    const [form, setForm] = useState(() => formFromInitialData(initialData));
    const [error, setError] = useState("");
    const [prevOpen, setPrevOpen] = useState(open);

    if (open !== prevOpen) {
        setPrevOpen(open);
        if (open) {
            setForm(formFromInitialData(initialData));
            setError("");
        }
    }

    const isEdit = Boolean(initialData);

    if (!open) return null;

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!form.name.trim() || !form.manager.trim()) {
            setError("Vui lòng nhập đầy đủ tên phòng ban và trưởng phòng.");
            return;
        }

        // Tạo Unique ID chuẩn bằng crypto.randomUUID() hoặc Date.now()
        const generatedId = initialData?.id || `dept-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;

        onSave({
            id: generatedId,
            name: form.name.trim(),
            manager: form.manager.trim(),
            count: Number(form.count) || 0,
        });
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
                className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity"
                onClick={onClose}
            />

            <div className="relative w-full max-w-md rounded-2xl border border-slate-100 bg-white p-6 shadow-2xl transition-all">
                <div className="mb-5 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-violet-50 border border-violet-100">
                            <FolderKanban className="h-4.5 w-4.5 text-violet-600" />
                        </span>
                        <h2 className="text-base font-bold text-slate-800">
                            {isEdit ? "Sửa phòng ban" : "Thêm phòng ban"}
                        </h2>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">
                            Tên phòng ban
                        </label>
                        <input
                            autoFocus
                            value={form.name}
                            onChange={(e) => setForm({ ...form, name: e.target.value })}
                            placeholder="VD: Phòng Công nghệ"
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>
                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">
                            Trưởng phòng
                        </label>
                        <input
                            value={form.manager}
                            onChange={(e) => setForm({ ...form, manager: e.target.value })}
                            placeholder="VD: Trần Quốc Bảo"
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>
                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">
                            Số nhân sự
                        </label>
                        <input
                            type="number"
                            min={0}
                            value={form.count}
                            onChange={(e) => setForm({ ...form, count: e.target.value })}
                            placeholder="0"
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>

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
                            className="rounded-xl bg-[#7c3aed] px-4 py-2.5 text-xs font-semibold text-white shadow-md hover:bg-[#6d28d9] transition"
                        >
                            {isEdit ? "Lưu thay đổi" : "Thêm phòng ban"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}