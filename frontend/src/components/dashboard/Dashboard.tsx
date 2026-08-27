import { useState } from "react";
import SideBar from "./SideBar";
import Header from "./Header";
import DashboardHeader from "./DashboardHeader";
import KpiStatsSection from "../kpi/KpiStatsSection";
import CalendarView from "../calendar/CalendarView";
import DepartmentsView from "../department/DepartmentsView";
import EmployeeProfilePage from "../../pages/EmployeeProfilePage";
import AccessControlView from "../access/AccessControlView";

export default function Dashboard() {
    const [activeTab, setActiveTab] = useState("overview");
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    return (
        <div className="relative flex h-screen w-full flex-col overflow-hidden text-[#f6f4ff] antialiased">
            {/* ---------- ambient backdrop (matches login page) ---------- */}
            <div className="pointer-events-none fixed inset-0 z-0" aria-hidden="true">
                <div
                    className="absolute inset-0"
                    style={{
                        background: "linear-gradient(165deg, #a855f7 0%, #7c3aed 22%, #5b21b6 38%, #4338ca 55%, #3b82f6 78%, #60a5fa 100%)",
                    }}
                />
            </div>

            <div className="relative z-10 flex h-full w-full flex-col">
                <Header setIsSidebarOpen={setIsSidebarOpen} />

                <div className="flex flex-1 overflow-hidden">
                    <SideBar activeTab={activeTab} setActiveTab={setActiveTab} isOpen={isSidebarOpen} />

                    <main className="flex-1 overflow-y-auto p-6">
                        {/* Thêm điều kiện render theo activeTab tại đây */}
                        {activeTab === "employees" && <EmployeeProfilePage />}

                        {activeTab === "departments" && <DepartmentsView />}

                        {activeTab === "access" && <AccessControlView />}

                        {activeTab === "overview" && (
                            <div>
                                {/* Header Overview */}
                                <DashboardHeader />

                                {/* Section KPI Cards */}
                                <KpiStatsSection />

                                {/* Lịch Workspace */}
                                <CalendarView />
                            </div>
                        )}
                    </main>
                </div>
            </div>
        </div>
    );
}