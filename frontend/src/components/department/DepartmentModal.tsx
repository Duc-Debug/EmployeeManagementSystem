import { useState } from "react";
import { X, FolderKanban, UserCircle2, Network } from "lucide-react";
import ComboSelect from "./ComboSelect.tsx";
import type { Employee } from "./Employees data.ts";

export interface Department {
    id: string;
    name: string;
    managerId: string | null;
    managerName: string;
    parentId: string | null;
    parentName: string | null;
}

interface DepartmentModalProps {
    open: boolean;
    initialData?: Department | null;
    /** Danh sách phòng ban hiện có, dùng để chọn Đơn vị cha */
    departments: Department[];
    /** Danh sách nhân sự, dùng để chọn Người quản lý */
    managers: Employee[];
    onClose: () => void;
    onSave: (dept: Department) => void;
}

interface FormState {
    name: string;
    managerId: string;
    parentId: string;
}

const emptyForm: FormState = { name: "", managerId: "", parentId: "" };

function formFromInitialData(initialData?: Department | null): FormState {
    return initialData
        ? {
            name: initialData.name,
            managerId: initialData.managerId ?? "",
            parentId: initialData.parentId ?? "",
        }
        : emptyForm;
}

let deptIdCounter = 0;
function nextDeptId() {
    deptIdCounter += 1;
    return `dept-new-${deptIdCounter}`;
}

export default function DepartmentModal({
                                            open,
                                            initialData,
                                            departments,
                                            managers,
                                            onClose,
                                            onSave,
                                        }: DepartmentModalProps) {
    const [form, setForm] = useState<FormState>(() => formFromInitialData(initialData));
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

    // Không cho phép chọn chính phòng ban đang sửa làm đơn vị cha của chính nó
    const parentOptions = departments
        .filter((d) => d.id !== initialData?.id)
        .map((d) => ({ id: d.id, label: d.name }));

    const managerOptions = managers.map((m) => ({
        id: m.id,
        label: m.name,
        sublabel: m.position,
    }));

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!form.name.trim()) {
            setError("Vui lòng nhập tên phòng ban.");
            return;
        }
        if (!form.managerId) {
            setError("Vui lòng chọn người quản lý.");
            return;
        }

        const manager = managers.find((m) => m.id === form.managerId);
        const parent = departments.find((d) => d.id === form.parentId);

        onSave({
            id: initialData?.id ?? nextDeptId(),
            name: form.name.trim(),
            managerId: form.managerId,
            managerName: manager?.name ?? "",
            parentId: form.parentId || null,
            parentName: parent?.name ?? null,
        });
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            {/* Backdrop phủ đen nhẹ mờ */}
            <div
                className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm transition-opacity"
                onClick={onClose}
            />

            {/* Modal Card Nền Trắng */}
            <div className="relative w-full max-w-md rounded-2xl border border-slate-100 bg-white p-6 shadow-2xl transition-all">
                <div className="mb-5 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-50 border border-emerald-100">
                            <FolderKanban className="h-4.5 w-4.5 text-emerald-600" />
                        </span>
                        <h2 className="text-base font-bold text-slate-800">
                            {isEdit ? "Sửa phòng ban" : "Thêm phòng ban"}
                        </h2>
                    </div>
                    <button
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
                            Người quản lý
                        </label>
                        <ComboSelect
                            value={form.managerId || null}
                            options={managerOptions}
                            onChange={(id) => setForm({ ...form, managerId: id ?? "" })}
                            placeholder="Chọn người quản lý..."
                            searchPlaceholder="Tìm theo tên nhân sự..."
                            emptyText="Không tìm thấy nhân sự phù hợp."
                            icon={<UserCircle2 className="h-3.5 w-3.5 text-slate-400" />}
                        />
                    </div>

                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">
                            Đơn vị cha
                        </label>
                        <ComboSelect
                            value={form.parentId || null}
                            options={parentOptions}
                            onChange={(id) => setForm({ ...form, parentId: id ?? "" })}
                            placeholder="Không có (đây là đơn vị gốc)"
                            searchPlaceholder="Tìm phòng ban..."
                            emptyText="Chưa có phòng ban nào khác."
                            allowClear
                            icon={<Network className="h-3.5 w-3.5 text-slate-400" />}
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
                            className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-semibold text-slate-600 transition hover:bg-slate-50 hover:text-slate-900"
                        >
                            {isEdit ? "Lưu thay đổi" : "Thêm phòng ban"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}