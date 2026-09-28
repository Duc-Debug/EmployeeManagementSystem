import { useState, Suspense } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { cn } from "@/lib/utils";
import SideBar from "./SideBar";
import Header from "./Header";
import PageLoading from "../common/PageLoading";

export default function DashboardLayout() {
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const location = useLocation();
    const isDepartments = location.pathname.endsWith("/departments");

    return (
        <div className="relative flex h-screen w-full flex-col overflow-hidden bg-[#f8fafc] text-slate-800 antialiased">
            {/* ---------- ambient clean light backdrop ---------- */}
            <div className="pointer-events-none fixed inset-0 z-0 bg-slate-50/60" aria-hidden="true" />

            <div className="relative z-10 flex h-full w-full flex-col">
                <Header setIsSidebarOpen={setIsSidebarOpen} />

                <div className="flex flex-1 min-h-0 overflow-hidden">
                    <SideBar isOpen={isSidebarOpen} />

                    <main
                        className={cn(
                            "flex-1 min-h-0 p-6",
                            isDepartments
                                ? "overflow-hidden flex flex-col"
                                : "overflow-y-auto"
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
