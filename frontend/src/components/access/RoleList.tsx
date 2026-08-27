import { Pencil, Plus, ShieldCheck, Trash2, Users } from "lucide-react";
import { cn } from "@/lib/utils";
import { THEME_STYLES } from "./access.constants";
import type { Role, RoleTheme } from "./access.types";

interface RoleListProps {
    roles: Role[];
    selectedId: string;
    onSelect: (id: string) => void;
    onAdd: () => void;
    onEdit: (role: Role) => void;
    onDelete: (id: string) => void;
}

export default function RoleList({ roles, selectedId, onSelect, onAdd, onEdit, onDelete }: RoleListProps) {
    return (
        <div className="rounded-2xl border border-white/15 bg-white/[0.07] p-4 backdrop-blur-xl shadow-[0_8px_24px_rgba(15,10,45,0.15)]">
            <div className="mb-3 flex items-center justify-between px-1">
                <h2 className="text-sm font-bold text-white">Danh sách vai trò</h2>
                <button
                    type="button"
                    onClick={onAdd}
                    className="flex items-center gap-1.5 rounded-xl border border-white/25 bg-white/15 px-3 py-1.5 text-[11px] font-semibold text-white backdrop-blur-xl transition hover:bg-white/25"
                >
                    <Plus className="h-3.5 w-3.5" /> Thêm
                </button>
            </div>

            <div className="space-y-2">
                {roles.map((role: Role) => {
                    const currentTheme: RoleTheme = role.theme || "blue";
                    const theme = THEME_STYLES[currentTheme] || THEME_STYLES.blue;
                    const isActive = role.id === selectedId;
                    const isSystem = Boolean(role.isSystemRole);

                    return (
                        <div
                            key={role.id}
                            onClick={() => onSelect(role.id)}
                            className={cn(
                                "group cursor-pointer rounded-xl border p-3 transition",
                                isActive
                                    ? "border-white/25 bg-white/15"
                                    : "border-white/10 bg-white/[0.03] hover:border-white/20 hover:bg-white/[0.08]"
                            )}
                        >
                            <div className="flex items-start justify-between gap-2">
                                <div className="flex min-w-0 items-center gap-2.5">
                                    <span className={cn("flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border", theme.iconBg, theme.iconText, theme.chipBorder)}>
                                        <ShieldCheck className="h-4.5 w-4.5" />
                                    </span>
                                    <div className="min-w-0">
                                        <p className="truncate text-sm font-bold text-white">{role.name}</p>
                                        <p className="flex items-center gap-1 text-[11px] text-white/50">
                                            <Users className="h-3 w-3" /> {role.userCount} người dùng
                                        </p>
                                    </div>
                                </div>

                                <div className="flex shrink-0 items-center gap-1 opacity-0 transition group-hover:opacity-100">
                                    <button
                                        type="button"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            onEdit(role);
                                        }}
                                        className="flex h-7 w-7 items-center justify-center rounded-lg border border-white/15 bg-white/10 text-white/80 transition hover:border-[#63ecc8]/50 hover:bg-[#63ecc8]/20 hover:text-[#63ecc8]"
                                        title="Sửa"
                                    >
                                        <Pencil className="h-3 w-3" />
                                    </button>
                                    {!isSystem && (
                                        <button
                                            type="button"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                onDelete(role.id);
                                            }}
                                            className="flex h-7 w-7 items-center justify-center rounded-lg border border-white/15 bg-white/10 text-white/80 transition hover:border-rose-400/50 hover:bg-rose-500/20 hover:text-rose-300"
                                            title="Xóa"
                                        >
                                            <Trash2 className="h-3 w-3" />
                                        </button>
                                    )}
                                </div>
                            </div>

                            {isSystem && (
                                <div className="mt-2.5 flex flex-wrap items-center gap-1.5">
                                    <span className="rounded-full border border-amber-400/30 bg-amber-500/15 px-2 py-0.5 text-[10px] font-semibold text-amber-300">
                                        Vai trò hệ thống
                                    </span>
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}