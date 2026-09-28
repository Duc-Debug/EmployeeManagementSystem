import React, { useState, useEffect, useRef, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Search, ChevronRight, CornerDownLeft, X, Sparkles } from "lucide-react";
import { useAuthUser } from "@/lib/auth-session";
import { SIDEBAR_GROUPS, canAccessTab } from "./SideBar";
import type { SidebarItemDef } from "./SideBar";
import { cn } from "@/lib/utils";

interface SearchablePageItem extends SidebarItemDef {
    groupId: string;
    groupTitle: string;
}

// Remove Vietnamese accents for flexible fuzzy matching
function removeAccents(str: string): string {
    return str
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();
}

export default function PageQuickSearch() {
    const user = useAuthUser();
    const navigate = useNavigate();
    const [isOpen, setIsOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState("");
    const [selectedIndex, setSelectedIndex] = useState(0);

    const containerRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const listRef = useRef<HTMLDivElement>(null);

    const roleCode = user?.roleCode;
    const dataScope = user?.dataScope;
    const normalizedRole = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";
    const isEmployeeOnly = normalizedRole === "VT-04";

    // 1. Gather all accessible pages for this user
    const accessiblePages = useMemo<SearchablePageItem[]>(() => {
        const pages: SearchablePageItem[] = [];

        SIDEBAR_GROUPS.forEach((group) => {
            group.items.forEach((item) => {
                if (canAccessTab(roleCode, item.id, dataScope, user?.permissions)) {
                    let displayName = item.name;
                    if (item.id === "skills") {
                        displayName = isEmployeeOnly ? "Khai báo kỹ năng" : "Quản lý Năng lực & Kỹ năng";
                    } else if (item.id === "availability") {
                        displayName = isEmployeeOnly || dataScope === "SELF" ? "Giờ khả dụng của tôi" : "Quản lý Giờ khả dụng";
                    }

                    pages.push({
                        id: item.id,
                        name: displayName,
                        icon: item.icon,
                        groupId: group.id,
                        groupTitle: group.title,
                    });
                }
            });
        });

        return pages;
    }, [roleCode, dataScope, user?.permissions, isEmployeeOnly]);

    // 2. Filter pages based on search query
    const filteredPages = useMemo(() => {
        if (!searchQuery.trim()) {
            return accessiblePages;
        }

        const normalizedQuery = removeAccents(searchQuery.trim());
        return accessiblePages.filter((page) => {
            const nameMatch = removeAccents(page.name).includes(normalizedQuery);
            const groupMatch = removeAccents(page.groupTitle).includes(normalizedQuery);
            const idMatch = page.id.toLowerCase().includes(normalizedQuery);
            return nameMatch || groupMatch || idMatch;
        });
    }, [accessiblePages, searchQuery]);

    // Reset selected index when search changes
    useEffect(() => {
        setSelectedIndex(0);
    }, [filteredPages]);

    // Handle global shortcut Ctrl+K / Cmd+K
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
                e.preventDefault();
                setIsOpen(true);
                setTimeout(() => inputRef.current?.focus(), 50);
            }
        };

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, []);

    // Handle click outside to close dropdown
    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
                setIsOpen(false);
            }
        };

        if (isOpen) {
            document.addEventListener("mousedown", handleClickOutside);
        }
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, [isOpen]);

    // Handle keyboard navigation inside dropdown
    const handleInputKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (!isOpen && (e.key === "ArrowDown" || e.key === "Enter")) {
            setIsOpen(true);
            return;
        }

        if (e.key === "ArrowDown") {
            e.preventDefault();
            setSelectedIndex((prev) => (prev < filteredPages.length - 1 ? prev + 1 : 0));
        } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setSelectedIndex((prev) => (prev > 0 ? prev - 1 : filteredPages.length - 1));
        } else if (e.key === "Enter") {
            e.preventDefault();
            if (filteredPages.length > 0 && selectedIndex >= 0 && selectedIndex < filteredPages.length) {
                handleSelectPage(filteredPages[selectedIndex]);
            }
        } else if (e.key === "Escape") {
            e.preventDefault();
            setIsOpen(false);
            inputRef.current?.blur();
        }
    };

    const handleSelectPage = (page: SearchablePageItem) => {
        setIsOpen(false);
        setSearchQuery("");
        const targetPath = page.id === "overview" ? "/" : `/${page.id}`;
        navigate(targetPath);
    };

    return (
        <div ref={containerRef} className="relative">
            {/* Search Input Bar */}
            <div
                className={cn(
                    "flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 shadow-xs transition-all duration-200",
                    isOpen
                        ? "w-[260px] sm:w-[320px] bg-white border-indigo-400 ring-2 ring-indigo-100"
                        : "w-[180px] sm:w-[240px] hover:border-slate-300 hover:bg-slate-100/70"
                )}
            >
                <Search className="h-4 w-4 shrink-0 text-slate-400" />
                <input
                    ref={inputRef}
                    type="text"
                    value={searchQuery}
                    onChange={(e) => {
                        setSearchQuery(e.target.value);
                        if (!isOpen) setIsOpen(true);
                    }}
                    onFocus={() => setIsOpen(true)}
                    onKeyDown={handleInputKeyDown}
                    placeholder="Tìm trang khả dụng..."
                    className="w-full bg-transparent text-xs font-medium text-slate-800 placeholder-slate-400 focus:outline-none"
                />

                {searchQuery ? (
                    <button
                        type="button"
                        onClick={() => {
                            setSearchQuery("");
                            inputRef.current?.focus();
                        }}
                        className="rounded-full p-0.5 text-slate-400 hover:bg-slate-200 hover:text-slate-600 transition"
                    >
                        <X className="h-3 w-3" />
                    </button>
                ) : (
                    <kbd className="hidden sm:inline-flex items-center gap-0.5 rounded border border-slate-200 bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold text-slate-400">
                        <span className="text-[9px]">Ctrl</span> K
                    </kbd>
                )}
            </div>

            {/* Quick Search Results Dropdown */}
            {isOpen && (
                <div className="absolute right-0 sm:left-0 top-full mt-2 w-[320px] sm:w-[380px] rounded-2xl border border-slate-200 bg-white p-2 text-slate-700 shadow-2xl z-50 animate-in fade-in zoom-in-95 duration-150">
                    <div className="flex items-center justify-between px-3 py-1.5 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-100 mb-1">
                        <span>Trang khả dụng ({filteredPages.length})</span>
                        <span className="text-[10px] font-normal lowercase">Phân quyền theo vai trò</span>
                    </div>

                    <div ref={listRef} className="max-h-[340px] overflow-y-auto space-y-1 p-0.5 scrollbar-thin scrollbar-thumb-slate-200">
                        {filteredPages.length === 0 ? (
                            <div className="py-8 text-center text-slate-400">
                                <Sparkles className="mx-auto h-6 w-6 text-slate-300 mb-1.5" />
                                <p className="text-xs font-medium">Không tìm thấy trang nào phù hợp</p>
                                <p className="text-[11px] text-slate-400 mt-0.5">Chỉ tìm kiếm trong các trang vai trò bạn được phân quyền</p>
                            </div>
                        ) : (
                            filteredPages.map((page, index) => {
                                const Icon = page.icon;
                                const isSelected = index === selectedIndex;

                                return (
                                    <button
                                        key={page.id}
                                        type="button"
                                        onClick={() => handleSelectPage(page)}
                                        onMouseEnter={() => setSelectedIndex(index)}
                                        className={cn(
                                            "flex w-full items-center justify-between rounded-xl px-3 py-2 text-left transition-all",
                                            isSelected
                                                ? "bg-indigo-50/90 text-indigo-900 font-semibold"
                                                : "text-slate-700 hover:bg-slate-50"
                                        )}
                                    >
                                        <div className="flex items-center gap-2.5 min-w-0">
                                            <div
                                                className={cn(
                                                    "flex h-7 w-7 shrink-0 items-center justify-center rounded-lg transition-colors",
                                                    isSelected ? "bg-indigo-600 text-white shadow-xs" : "bg-slate-100 text-slate-500"
                                                )}
                                            >
                                                <Icon className="h-4 w-4" />
                                            </div>
                                            <div className="min-w-0">
                                                <p className="truncate text-xs font-semibold">{page.name}</p>
                                                <p className="truncate text-[10px] text-slate-400 font-normal">{page.groupTitle}</p>
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-1 shrink-0 ml-2">
                                            {isSelected && (
                                                <div className="flex items-center gap-1 rounded bg-indigo-100/80 px-1.5 py-0.5 text-[10px] font-medium text-indigo-700">
                                                    <span>Đi tới</span>
                                                    <CornerDownLeft className="h-2.5 w-2.5" />
                                                </div>
                                            )}
                                            {!isSelected && (
                                                <ChevronRight className="h-3.5 w-3.5 text-slate-300" />
                                            )}
                                        </div>
                                    </button>
                                );
                            })
                        )}
                    </div>

                    <div className="border-t border-slate-100 px-3 py-1.5 mt-1 flex items-center justify-between text-[11px] text-slate-400 bg-slate-50/50 rounded-b-xl">
                        <span>Dùng phím <kbd className="font-semibold text-slate-600">↑</kbd> <kbd className="font-semibold text-slate-600">↓</kbd> để di chuyển</span>
                        <span>Nhấn <kbd className="font-semibold text-slate-600">Enter</kbd> để mở</span>
                    </div>
                </div>
            )}
        </div>
    );
}