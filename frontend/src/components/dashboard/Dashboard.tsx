import { useState, useEffect, Suspense } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { cn } from "@/lib/utils";
import SideBar from "./SideBar";
import Header from "./Header";
import PageLoading from "../common/PageLoading";

export default function DashboardLayout() {
    // Dưới 1024px: sidebar ẩn mặc định (P1-5)
    const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(() => {
        if (typeof window !== "undefined") {
            return window.innerWidth >= 1024;
        }
        return false;
    });

    const location = useLocation();
    const isDepartments = location.pathname.endsWith("/departments");

    // Tự động đóng sidebar khi chuyển trang trên thiết bị di động / màn hình nhỏ (< 1024px)
    useEffect(() => {
        if (typeof window !== "undefined" && window.innerWidth < 1024) {
            setIsSidebarOpen(false);
        }
    }, [location.pathname]);

    // Lắng nghe thay đổi kích thước màn hình để tự động cập nhật trạng thái hiển thị
    useEffect(() => {
        if (typeof window === "undefined") return;
        const mql = window.matchMedia("(min-width: 1024px)");
        const handleMediaChange = (e: MediaQueryListEvent) => {
            setIsSidebarOpen(e.matches);
        };
        mql.addEventListener("change", handleMediaChange);
        return () => mql.removeEventListener("change", handleMediaChange);
    }, []);

    return (
        <div className="relative flex h-screen w-full flex-col overflow-hidden bg-[#f8fafc] text-slate-800 antialiased">
            {/* ---------- ambient clean light backdrop ---------- */}
            <div className="pointer-events-none fixed inset-0 z-0 bg-slate-50/60" aria-hidden="true" />

            <div className="relative z-10 flex h-full w-full flex-col">
                <Header setIsSidebarOpen={setIsSidebarOpen} />

                <div className="flex flex-1 min-h-0 overflow-hidden relative">
                    {/* Lớp phủ Backdrop khi mở sidebar dạng overlay trên mobile (< 1024px) */}
                    {isSidebarOpen && (
                        <div
                            className="fixed inset-0 z-40 bg-slate-900/50 backdrop-blur-xs lg:hidden transition-opacity duration-200"
                            onClick={() => setIsSidebarOpen(false)}
                            aria-label="Đóng thanh điều hướng"
                        />
                    )}

                    <SideBar
                        isOpen={isSidebarOpen}
                        onClose={() => setIsSidebarOpen(false)}
                    />

                    <main
                        className={cn(
                            "flex-1 min-h-0 w-full max-w-full p-3 sm:p-4 lg:p-6",
                            isDepartments
                                ? "overflow-hidden flex flex-col"
                                : "overflow-y-auto overflow-x-hidden"
                        )}
                    >
                        <Suspense fallback={<PageLoading />}>
                            <Outlet />
                        </Suspense>
                    </main>
                </div>
            </div>
        </div>
    );
}

export { DashboardLayout as Dashboard };

