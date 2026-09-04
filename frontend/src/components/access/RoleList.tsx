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
        <div className="flex h-full flex-col rounded-2xl border border-slate-200/90 bg-white p-4 shadow-xs text-slate-800">
            <div className="mb-4 flex items-center justify-between px-1">
                <h2 className="text-sm font-bold text-slate-900">Danh sách vai trò</h2>
                <button
                    type="button"
                    onClick={onAdd}
                    className="flex items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 px-3 py-1.5 text-[11px] font-bold text-indigo-700 transition hover:bg-indigo-100"
                >
                    <Plus className="h-3.5 w-3.5" /> Thêm
                </button>
            </div>

            {/* Filter / Search */}
            <div className="mb-4 space-y-2">
                <div className="relative">
                    <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                        type="text"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        placeholder="Tìm kiếm vai trò..."
                        className="w-full rounded-full border border-slate-200 bg-slate-50 py-2 pl-10 pr-4 text-xs text-slate-800 placeholder:text-slate-400 outline-none transition focus:border-indigo-500 focus:bg-white"
                    />
                </div>

                <div className="relative flex items-center justify-between px-1 text-xs text-slate-500">
                    <span>Lọc theo phòng ban:</span>
                    <div className="relative" ref={deptDropdownRef}>
                        <button
                            type="button"
                            onClick={() => setIsDeptDropdownOpen((prev) => !prev)}
                            className="flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3.5 py-1.5 text-xs font-semibold text-slate-700 shadow-xs transition hover:bg-slate-50 active:scale-95"
                        >
                            <span>{deptFilter === "all" ? "Tất cả phòng ban" : departments.find((d) => d.id === deptFilter)?.name}</span>
                            <ChevronDown className={cn("h-3.5 w-3.5 text-slate-400 transition-transform", isDeptDropdownOpen && "rotate-180")} />
                        </button>

                        {isDeptDropdownOpen && (
                            <div className="absolute right-0 top-full z-20 mt-2 w-48 overflow-hidden rounded-2xl border border-slate-200 bg-white p-1 shadow-xl">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setDeptFilter("all");
                                        setIsDeptDropdownOpen(false);
                                    }}
                                    className={cn(
                                        "flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-xs font-semibold transition",
                                        deptFilter === "all" ? "bg-indigo-50 text-indigo-700" : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
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
                                            "flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-xs font-semibold transition",
                                            deptFilter === dept.id ? "bg-indigo-50 text-indigo-700" : "text-slate-600 hover:bg-slate-50 hover:text-slate-900"
                                        )}
                                    >
                                        {dept.name}
                                        {deptFilter === dept.id && <Check className="h-3.5 w-3.5" />}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Role Cards List */}
            <div className="max-h-[calc(100vh-380px)] flex-1 space-y-2 overflow-y-auto pr-1 [scrollbar-width:thin] [scrollbar-color:#cbd5e1_transparent] [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-thumb]:bg-slate-200 [&::-webkit-scrollbar-thumb]:rounded-full">
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
                                    ? "border-indigo-300 bg-indigo-50/70 shadow-xs"
                                    : "border-slate-200/80 bg-slate-50/50 hover:border-slate-300 hover:bg-white"
                            )}
                        >
                            <div className="flex items-start justify-between gap-2">
                                <div className="flex min-w-0 items-center gap-3">
                                    <span className={cn("flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border", theme.iconBg, theme.iconText, theme.chipBorder)}>
                                        <ShieldCheck className="h-4.5 w-4.5" />
                                    </span>
                                    <div className="min-w-0">
                                        <p className="truncate text-sm font-bold text-slate-900 group-hover:text-indigo-600">{role.name}</p>
                                        <p className="mt-0.5 flex items-center gap-1.5 text-[11px] text-slate-500">
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
                                        className="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
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
                                            className="rounded-lg p-1.5 text-slate-400 transition hover:bg-rose-50 hover:text-rose-600"
                                            aria-label="Xóa vai trò"
                                        >
                                            <Trash2 className="h-3.5 w-3.5" />
                                        </button>
                                    )}
                                </div>
                            </div>

                            {isSystem && (
                                <div className="mt-2">
                                    <span className="inline-flex whitespace-nowrap rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[10px] font-bold text-amber-700">
                                        Vai trò hệ thống
                                    </span>
                                </div>
                            )}

                            {dept && (
                                <div className="mt-2.5 flex items-center gap-1 border-t border-slate-100 pt-2">
                                    <span className="inline-flex items-center gap-1 rounded-md border border-indigo-200 bg-indigo-50 px-2 py-0.5 text-[10px] font-semibold text-indigo-700">
                                        <Building2 className="h-2.5 w-2.5" />
                                        {dept.name}
                                    </span>
                                </div>
                            )}
                        </div>
                    );
                })}

                {filteredRoles.length === 0 && (
                    <div className="py-8 text-center text-xs text-slate-400">Không tìm thấy vai trò phù hợp</div>
                )}
            </div>

            <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-400">
                <span>
                    Đang hiển thị {filteredRoles.length} / {roles.length} vai trò
                </span>
                <SlidersHorizontal className="h-3.5 w-3.5 text-slate-400" />
            </div>
        </div>
    );
}