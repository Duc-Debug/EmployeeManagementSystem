import { useState } from "react";
import { X, Check, UserCircle2 } from "lucide-react";
import {
    ICON_OPTIONS,
    THEME_OPTIONS,
    iconKeyFor,
    type CardData,
} from "./orgNode.constants";
import ComboSelect from "./ComboSelect";
import type { Employee } from "./Employees data.ts";
import type { OrgUnitType } from "@/types/hrm";

export const ORG_UNIT_TYPE_OPTIONS: {
    value: OrgUnitType;
    label: string;
    badge: string;
    themeKey: string;
    iconKey: string;
}[] = [
    { value: "COMPANY", label: "Công ty (Company)", badge: "CÔNG TY", themeKey: "amber", iconKey: "Crown" },
    { value: "CENTER", label: "Khối / Trung tâm (Center)", badge: "KHỐI", themeKey: "blue", iconKey: "Building2" },
    { value: "DEPARTMENT", label: "Phòng ban (Department)", badge: "PHÒNG BAN", themeKey: "emerald", iconKey: "Users" },
    { value: "TEAM", label: "Bộ phận / Nhóm chuyên môn (Team)", badge: "TỔ NHÓM", themeKey: "purple", iconKey: "User" },
];

function themeKeyFor(card: CardData): string {
    return THEME_OPTIONS.find((t) => t.badgeBg === card.badgeBg && t.badgeColor === card.badgeColor)?.key ?? "neutral";
}

function hueFromClass(cls?: string): string | null {
    if (!cls) return null;
    const m = cls.match(/text-([a-z]+)-\d+/);
    return m ? m[1] : null;
}

const HUE_HEX_400: Record<string, string> = {
    amber: "#f59e0b",
    blue: "#3b82f6",
    emerald: "#10b981",
    teal: "#14b8a6",
    purple: "#a855f7",
    pink: "#ec4899",
    indigo: "#6366f1",
    rose: "#f43f5e",
    orange: "#f97316",
    cyan: "#06b6d4",
    slate: "#94a3b8",
    gray: "#9ca3af",
    zinc: "#a1a1aa",
    neutral: "#a3a3a3",
    stone: "#a8a29e",
};

function hexFor(theme: { badgeColor: string }) {
    const hue = hueFromClass(theme.badgeColor);
    return (hue && HUE_HEX_400[hue]) || "#94a3b8";
}

const EMPTY_NODE_FORM = {
    title: "",
    desc: "",
    subLeft: "",
    manager: "",
    unitType: "DEPARTMENT" as OrgUnitType,
    iconKey: "Users",
    themeKey: "emerald",
};

function formFromCard(card?: CardData | null) {
    if (!card) return EMPTY_NODE_FORM;
    const unitType = card.unitType || "DEPARTMENT";
    const typeMeta = ORG_UNIT_TYPE_OPTIONS.find((t) => t.value === unitType) || ORG_UNIT_TYPE_OPTIONS[2];
    return {
        title: card.title,
        desc: card.desc,
        subLeft: card.subLeft,
        manager: card.manager ?? "",
        unitType,
        iconKey: iconKeyFor(card.icon) || typeMeta.iconKey,
        themeKey: themeKeyFor(card) || typeMeta.themeKey,
    };
}

interface OrgNodeModalProps {
    open: boolean;
    initialData?: CardData | null;
    levelText: string;
    /** Danh sách nhân sự để chọn Người tổ chức */
    employees?: Employee[];
    onClose: () => void;
    onSave: (card: CardData) => void;
    onDelete?: () => void;
}
export default function OrgNodeModal({ open, initialData, levelText, employees, onClose, onSave, onDelete }: OrgNodeModalProps) {
    const [form, setForm] = useState(() => formFromCard(initialData));
    const [error, setError] = useState("");
    const [prevOpen, setPrevOpen] = useState(open);

    if (open !== prevOpen) {
        setPrevOpen(open);
        if (open) {
            setForm(formFromCard(initialData));
            setError("");
        }
    }

    const isEdit = Boolean(initialData);

    if (!open) return null;

    /** Danh sách option cho ComboSelect — dùng prop hoặc danh sách rỗng */
    const empList = employees ?? [];
    const employeeOptions = empList.map((e) => ({
        id: e.id,
        label: e.name,
        sublabel: e.position,
    }));

    /**
     * form.manager lưu chuỗi hiển thị tự do (ví dụ "Nguyễn Văn A (Trưởng phòng)").
     * Để ComboSelect hoạt động ta cần id. Ta ánh xạ ngược tên → id khi mở form.
     */
    const selectedManagerId =
        empList.find((e) => form.manager.startsWith(e.name))?.id ?? null;

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!form.title.trim()) {
            setError("Vui lòng nhập tên chức danh / bộ phận.");
            return;
        }
        const typeMeta = ORG_UNIT_TYPE_OPTIONS.find((t) => t.value === form.unitType) || ORG_UNIT_TYPE_OPTIONS[2];
        const theme = THEME_OPTIONS.find((t) => t.key === form.themeKey) || THEME_OPTIONS[2];
        const iconEntry = ICON_OPTIONS.find((i) => i.key === form.iconKey) || ICON_OPTIONS[4];
        onSave({
            badge: typeMeta.badge,
            unitType: form.unitType,
            title: form.title.trim(),
            desc: form.desc.trim(),
            subLeft: form.subLeft.trim(),
            manager: form.manager.trim(),
            levelText,
            badgeBg: theme.badgeBg,
            badgeColor: theme.badgeColor,
            borderColor: theme.borderColor,
            iconColor: theme.iconColor,
            icon: iconEntry.icon,
            cardBg: initialData?.cardBg ?? "bg-white",
            isDark: initialData?.isDark,
        });
    }
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={onClose} />
            <div className="relative w-full max-w-md rounded-2xl border border-slate-100 bg-white p-6 shadow-2xl max-h-[90vh] overflow-y-auto text-slate-800 animate-fadeIn">
                <div className="mb-5 flex items-center justify-between border-b border-slate-100 pb-3">
                    <h2 className="text-base font-bold text-slate-800">
                        {isEdit ? "Sửa nút sơ đồ" : "Thêm nút sơ đồ"}
                    </h2>
                    <button onClick={onClose} className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600">
                        <X className="h-4 w-4" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="mb-1 block text-xs font-semibold text-slate-600">Tên bộ phận</label>
                        <input
                            autoFocus
                            value={form.title}
                            onChange={(e) => setForm({ ...form, title: e.target.value })}
                            placeholder="VD: Quản Lý Dự Án"
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-[#4338ca] focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>
                    <div>
                        <label className="mb-1 block text-xs font-semibold text-slate-600">
                            Người tổ chức / Người đứng đầu
                        </label>
                        <ComboSelect
                            value={selectedManagerId}
                            options={employeeOptions}
                            onChange={(id) => {
                                const emp = empList.find((e) => e.id === id);
                                setForm({
                                    ...form,
                                    manager: emp
                                        ? emp.position
                                            ? `${emp.name} (${emp.position})`
                                            : emp.name
                                        : "",
                                });
                            }}
                            placeholder="Chọn người tổ chức..."
                            searchPlaceholder="Tìm theo tên nhân sự..."
                            emptyText="Không tìm thấy nhân sự phù hợp."
                            allowClear
                            icon={<UserCircle2 className="h-3.5 w-3.5 text-slate-400" />}
                        />
                    </div>
                    <div>
                        <label className="mb-1 block text-xs font-semibold text-slate-700">
                            Loại đơn vị / Loại phòng *
                        </label>
                        <select
                            value={form.unitType}
                            onChange={(e) => {
                                const newType = e.target.value as OrgUnitType;
                                const typeMeta = ORG_UNIT_TYPE_OPTIONS.find((t) => t.value === newType) || ORG_UNIT_TYPE_OPTIONS[2];
                                setForm({
                                    ...form,
                                    unitType: newType,
                                    themeKey: typeMeta.themeKey,
                                    iconKey: typeMeta.iconKey,
                                });
                            }}
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm font-semibold text-slate-800 outline-none transition focus:border-indigo-600 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        >
                            {ORG_UNIT_TYPE_OPTIONS.map((opt) => (
                                <option key={opt.value} value={opt.value}>
                                    {opt.label}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label className="mb-1 block text-xs font-semibold text-slate-600">Mô tả</label>
                        <textarea
                            value={form.desc}
                            onChange={(e) => setForm({ ...form, desc: e.target.value })}
                            placeholder="VD: Điều phối tiến độ, ngân sách & mục tiêu sản phẩm"
                            rows={2}
                            className="w-full resize-none rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-[#4338ca] focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>
                    <div>
                        <label className="mb-1 block text-xs font-semibold text-slate-600">Ghi chú báo cáo</label>
                        <input
                            value={form.subLeft}
                            onChange={(e) => setForm({ ...form, subLeft: e.target.value })}
                            placeholder="VD: Báo cáo trực tiếp BGĐ"
                            className="w-full rounded-xl border border-slate-200 bg-slate-50/50 px-3.5 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-[#4338ca] focus:bg-white focus:ring-2 focus:ring-indigo-100"
                        />
                    </div>

                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">Biểu tượng</label>
                        <div className="grid grid-cols-6 gap-2">
                            {ICON_OPTIONS.map(({ key, icon: Icon, label }) => (
                                <button
                                    type="button"
                                    key={key}
                                    title={label}
                                    onClick={() => setForm({ ...form, iconKey: key })}
                                    className={`flex items-center justify-center rounded-lg border p-2 transition ${
                                        form.iconKey === key
                                            ? "border-[#4338ca] bg-indigo-50 text-[#4338ca] shadow-sm"
                                            : "border-slate-200 bg-slate-50 text-slate-500 hover:bg-slate-100 hover:text-slate-700"
                                    }`}
                                >
                                    <Icon className="h-4 w-4" />
                                </button>
                            ))}
                        </div>
                    </div>

                    <div>
                        <label className="mb-1.5 block text-xs font-semibold text-slate-600">Màu chủ đề</label>
                        <div className="grid grid-cols-6 gap-2">
                            {THEME_OPTIONS.map((t) => {
                                const selected = form.themeKey === t.key;
                                return (
                                    <button
                                        type="button"
                                        key={t.key}
                                        title={t.label}
                                        onClick={() => setForm({ ...form, themeKey: t.key })}
                                        className="flex flex-col items-center gap-1"
                                    >
                                        <span
                                            style={{ backgroundColor: hexFor(t) }}
                                            className={`flex h-8 w-8 items-center justify-center rounded-full border-2 shadow-sm transition ${
                                                selected
                                                    ? "border-white ring-2 ring-[#4338ca] ring-offset-2"
                                                    : "border-white/70 hover:scale-110"
                                            }`}
                                        >
                                            {selected && <Check className="h-4 w-4 text-white drop-shadow" />}
                                        </span>
                                        <span className={`text-[10px] font-medium ${selected ? "text-[#4338ca]" : "text-slate-500"}`}>
                                            {t.label}
                                        </span>
                                    </button>
                                );
                            })}
                        </div>
                    </div>

                    {error && <p className="text-xs font-medium text-rose-600">{error}</p>}

                    <div className="flex items-center justify-between gap-2 border-t border-slate-100 pt-4">
                        <div>
                            {isEdit && onDelete && (
                                <button
                                    type="button"
                                    onClick={onDelete}
                                    className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-2.5 text-xs font-semibold text-rose-600 transition hover:bg-rose-100"
                                >
                                    Xóa nhánh
                                </button>
                            )}
                        </div>
                        <div className="flex gap-2">
                            <button
                                type="button"
                                onClick={onClose}
                                className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-semibold text-slate-600 transition hover:bg-slate-50"
                            >
                                Hủy
                            </button>
                            <button
                                type="submit"
                                className="rounded-xl bg-[#4338ca] hover:bg-[#3730a3] px-4 py-2.5 text-xs font-bold text-white shadow-sm transition"
                            >
                                {isEdit ? "Lưu thay đổi" : "Thêm nhánh"}
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
}