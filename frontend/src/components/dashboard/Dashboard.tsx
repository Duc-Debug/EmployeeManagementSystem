import { useState } from "react";
import SideBar from "./SideBar";
import Header from "./Header";
import DashboardHeader from "./DashboardHeader";
import KpiStatsSection from "../kpi/KpiStatsSection";
import CalendarView from "../calendar/CalendarView";
import DepartmentsView from "../department/DepartmentsView";

export default function Dashboard() {
    const [activeTab, setActiveTab] = useState("overview");
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    return (
        <div className="flex h-screen w-full flex-col bg-[#f4f5fa] text-[#1e1b4b] antialiased">
            <Header setIsSidebarOpen={setIsSidebarOpen} />

            <div className="flex flex-1 overflow-hidden">
                <SideBar activeTab={activeTab} setActiveTab={setActiveTab} isOpen={isSidebarOpen} />

                <main className="flex-1 overflow-y-auto p-6">
                    {activeTab === "departments" ? (
                        <DepartmentsView />
                    ) : (
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
    );
}