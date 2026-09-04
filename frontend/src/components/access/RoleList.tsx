import { useEffect, useMemo, useRef, useState } from "react";
import { Building2, Check, ChevronDown, Pencil, Plus, Search, ShieldCheck, SlidersHorizontal, Trash2, Users } from "lucide-react";
import { cn } from "@/lib/utils";
import { THEME_STYLES } from "./access.constants";
import type { Department, Role, RoleTheme } from "./access.types";

interface RoleListProps {
    roles: Role[];
    departments: Department[];
    selectedId: string;
    onSelect: (id: string) => void;
    onAdd: () => void;
    onEdit: (role: Role) => void;
    onDelete: (id: string) => void;
}

export default function RoleList({ roles, departments, selectedId, onSelect, onAdd, onEdit, onDelete }: RoleListProps) {
    const [search, setSearch] = useState("");
    const [deptFilter, setDeptFilter] = useState("all");
    const [isDeptDropdownOpen, setIsDeptDropdownOpen] = useState(false);
    const deptDropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (deptDropdownRef.current && !deptDropdownRef.current.contains(e.target as Node)) {
                setIsDeptDropdownOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const filteredRoles = useMemo(() => {
        const keyword = search.trim().toLowerCase();
        return roles.filter((role) => {
            const matchesKeyword =
                !keyword ||
                role.name.toLowerCase().includes(keyword) ||
                role.description.toLowerCase().includes(keyword);
            const matchesDept =
                deptFilter === "all" || role.departmentId === "all" || role.departmentId === deptFilter;
            return matchesKeyword && matchesDept;
        });
    }, [roles, search, deptFilter]);

    return (
        <div className="flex h-full flex-col rounded-2xl border border-white/15 bg-transparent p-4">
            <div className="mb-4 flex items-center justify-between px-1">
                <h2 className="text-sm font-bold text-white">Danh sách vai trò</h2>
                <button
                    type="button"
                    onClick={onAdd}
                    className="flex items-center gap-1.5 rounded-xl border border-purple-400/30 bg-purple-500/20 px-3 py-1.5 text-[11px] font-semibold text-purple-200 transition hover:bg-purple-500/30"
                >
                    <Plus className="h-3.5 w-3.5" /> Thêm
                </button>
            </div>

            {/* Filter / Search */}
            <div className="mb-4 space-y-2">
                <div className="relative">
                    <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-white/40" />
                    <input
                        type="text"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        placeholder="Tìm kiếm vai trò..."
                        className="w-full rounded-full border border-white/15 bg-white/5 py-2.5 pl-10 pr-4 text-xs text-white placeholder:text-white/40 outline-none transition focus:border-purple-400"
                    />
                </div>

                <div className="relative flex items-center justify-between px-1 text-xs text-purple-200/70">
                    <span>Lọc theo phòng ban:</span>
                    <div className="relative" ref={deptDropdownRef}>
                        <button
                            type="button"
                            onClick={() => setIsDeptDropdownOpen((prev) => !prev)}
                            className="flex w-48 items-center justify-between gap-2 rounded-full border border-white/30 bg-white/15 px-4 py-2 text-xs font-semibold text-white backdrop-blur-sm transition hover:bg-white/25 active:scale-95"
                        >
                            <span className="truncate">{deptFilter === "all" ? "Tất cả phòng ban" : departments.find((d) => d.id === deptFilter)?.name}</span>
                            <ChevronDown className={cn("h-3.5 w-3.5 shrink-0 text-white/70 transition-transform", isDeptDropdownOpen && "rotate-180")} />
                        </button>

                        <div
                            className={cn(
                                "absolute right-0 top-full z-20 mt-2 max-h-60 w-48 origin-top-right overflow-y-auto rounded-xl border border-white/15 bg-white/10 p-1 shadow-xl backdrop-blur-xl transition-all duration-150 ease-out",
                                isDeptDropdownOpen
                                    ? "scale-100 opacity-100"
                                    : "pointer-events-none scale-95 opacity-0"
                            )}
                        >
                            <button
                                type="button"
                                onClick={() => {
                                    setDeptFilter("all");
                                    setIsDeptDropdownOpen(false);
                                }}
                                className={cn(
                                    "flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-xs font-medium transition",
                                    deptFilter === "all" ? "bg-white/20 text-white" : "text-white/60 hover:bg-white/10 hover:text-white"
                                )}
                            >
                                Tất cả phòng ban
                                {deptFilter === "all" && <Check className="h-3.5 w-3.5" />}
                            </button>
                            {departments.map((dept) => (
                                <button
                                    key={dept.id}
                                    type="button"
                                    onClick={() => {
                                        setDeptFilter(dept.id);
                                        setIsDeptDropdownOpen(false);
                                    }}
                                    className={cn(
                                        "flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-xs font-medium transition",
                                        deptFilter === dept.id ? "bg-white/20 text-white" : "text-white/60 hover:bg-white/10 hover:text-white"
                                    )}
                                >
                                    {dept.name}
                                    {deptFilter === dept.id && <Check className="h-3.5 w-3.5" />}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            {/* Role Cards List */}
            <div className="max-h-[calc(100vh-380px)] flex-1 space-y-2 overflow-y-auto pr-1">
                {filteredRoles.map((role: Role) => {
                    const currentTheme: RoleTheme = role.theme || "blue";
                    const theme = THEME_STYLES[currentTheme] || THEME_STYLES.blue;
                    const isActive = role.id === selectedId;
                    const isSystem = Boolean(role.isSystemRole);
                    const dept =
                        role.departmentId !== "all" ? departments.find((d) => d.id === role.departmentId) : null;

                    return (
                        <div
                            key={role.id}
                            onClick={() => onSelect(role.id)}
                            className={cn(
                                "group cursor-pointer rounded-xl border p-3 transition-all duration-200",
                                isActive
                                    ? "border-purple-300/40 bg-purple-500/20 shadow-[0_8px_32px_0_rgba(109,40,217,0.3)]"
                                    : "border-white/10 bg-white/[0.03] hover:border-purple-400/30 hover:bg-white/[0.08]"
                            )}
                        >
                            <div className="flex items-start justify-between gap-2">
                                <div className="flex min-w-0 items-center gap-3">
                                    <span className={cn("flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border", theme.iconBg, theme.iconText, theme.chipBorder)}>
                                        <ShieldCheck className="h-4.5 w-4.5" />
                                    </span>
                                    <div className="min-w-0">
                                        <p className="truncate text-sm font-bold text-white group-hover:text-purple-200">{role.name}</p>
                                        <p className="mt-0.5 flex items-center gap-1.5 text-[11px] text-purple-200/60">
                                            <Users className="h-3 w-3" />
                                            <span>{role.userCount} người dùng</span>
                                        </p>
                                    </div>
                                </div>

                                <div className="flex shrink-0 items-center gap-1">
                                    <button
                                        type="button"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            onEdit(role);
                                        }}
                                        className="rounded-lg p-1.5 text-purple-200/60 transition hover:bg-white/10 hover:text-white"
                                        aria-label="Sửa vai trò"
                                    >
                                        <Pencil className="h-3.5 w-3.5" />
                                    </button>
                                    {!isSystem && (
                                        <button
                                            type="button"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                onDelete(role.id);
                                            }}
                                            className="rounded-lg p-1.5 text-purple-200/60 transition hover:bg-rose-500/15 hover:text-rose-300"
                                            aria-label="Xóa vai trò"
                                        >
                                            <Trash2 className="h-3.5 w-3.5" />
                                        </button>
                                    )}
                                </div>
                            </div>

                            {isSystem && (
                                <div className="mt-2">
                                    <span className="inline-flex whitespace-nowrap rounded-full border border-amber-400/30 bg-amber-500/20 px-2 py-0.5 text-[10px] font-semibold text-amber-300">
                                        Vai trò hệ thống
                                    </span>
                                </div>
                            )}

                            {dept && (
                                <div className="mt-2.5 flex items-center gap-1 border-t border-white/10 pt-2">
                                    <span className="inline-flex items-center gap-1 rounded-md border border-indigo-400/30 bg-indigo-500/20 px-2 py-0.5 text-[10px] font-medium text-indigo-200">
                                        <Building2 className="h-2.5 w-2.5" />
                                        {dept.name}
                                    </span>
                                </div>
                            )}
                        </div>
                    );
                })}

                {filteredRoles.length === 0 && (
                    <div className="py-8 text-center text-xs text-purple-200/50">Không tìm thấy vai trò phù hợp</div>
                )}
            </div>

            <div className="mt-3 flex items-center justify-between border-t border-white/10 pt-3 text-xs text-purple-300/60">
                <span>
                    Đang hiển thị {filteredRoles.length} / {roles.length} vai trò
                </span>
                <SlidersHorizontal className="h-3.5 w-3.5 text-purple-400" />
            </div>
        </div>
    );
}