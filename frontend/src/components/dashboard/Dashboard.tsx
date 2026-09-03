import { useState } from "react";
import { cn } from "@/lib/utils";
import SideBar from "./SideBar";
import Header from "./Header";
import DashboardHeader from "./DashboardHeader";
import KpiStatsSection from "../kpi/KpiStatsSection";
import CalendarView from "../calendar/CalendarView";
import DepartmentsView from "../department/DepartmentsView";
import EmployeeProfilePage from "../../pages/EmployeeProfilePage";
import AccessControlView from "../access/AccessControlView";
import AttendanceView from "../attendance/AttendanceView";
import type { AttendanceRecord } from "@/lib/hr-data";

// Dữ liệu chấm công mẫu — thay bằng dữ liệu thật (API/store) khi có sẵn.
const INITIAL_ATTENDANCE_RECORDS: AttendanceRecord[] = [
    {
        id: "NV001",
        name: "Nguyễn Văn A",
        dept: "Kỹ thuật",
        inTime: "--:--",
        outTime: "--:--",
        hours: "0",
        ot: "0",
        status: "Vắng mặt",
    },
    {
        id: "NV002",
        name: "Trần Thị B",
        dept: "Nhân sự",
        inTime: "08:20",
        outTime: "17:30",
        hours: "8.0",
        ot: "0",
        status: "Đi muộn",
    },
    {
        id: "NV003",
        name: "Lê Văn C",
        dept: "Kinh doanh",
        inTime: "08:00",
        outTime: "18:15",
        hours: "9.0",
        ot: "1.0",
        status: "Đã điều chỉnh",
    },
];

// Mã nhân viên đang thao tác ở "Trạm chấm công nhanh" — tạm gán cứng cho tới
// khi màn hình này đọc được người dùng đang đăng nhập từ auth thật.
const CURRENT_EMPLOYEE_ID = "NV001";

export default function Dashboard() {
    const [activeTab, setActiveTab] = useState("overview");
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [attendanceRecords, setAttendanceRecords] = useState<AttendanceRecord[]>(
        INITIAL_ATTENDANCE_RECORDS
    );

    const handleClockIn = () => {
        const time = new Date().toLocaleTimeString("en-US", {
            hour12: false,
            hour: "2-digit",
            minute: "2-digit",
        });
        setAttendanceRecords((prev) =>
            prev.map((rec) =>
                rec.id === CURRENT_EMPLOYEE_ID ? { ...rec, inTime: time, status: "Đúng giờ" } : rec
            )
        );
        return time;
    };

    const handleClockOut = () => {
        const time = new Date().toLocaleTimeString("en-US", {
            hour12: false,
            hour: "2-digit",
            minute: "2-digit",
        });
        setAttendanceRecords((prev) =>
            prev.map((rec) => (rec.id === CURRENT_EMPLOYEE_ID ? { ...rec, outTime: time } : rec))
        );
        return true;
    };

    const handleEditRecord = (id: string) => {
        // TODO: mở modal/điều hướng chỉnh sửa bản ghi chấm công theo id.
        console.log("Sửa bản ghi chấm công:", id);
    };

    return (
        <div className="relative flex h-screen w-full flex-col overflow-hidden bg-[#f8fafc] text-slate-800 antialiased">
            {/* ---------- ambient clean light backdrop ---------- */}
            <div className="pointer-events-none fixed inset-0 z-0 bg-slate-50/60" aria-hidden="true" />

            <div className="relative z-10 flex h-full w-full flex-col">
                <Header setIsSidebarOpen={setIsSidebarOpen} />

                {/* min-h-0 là bắt buộc: mặc định flex item có min-height: auto,
                    khiến hàng chứa Sidebar + main tự giãn theo chiều cao nội
                    dung thay vì bị giới hạn theo chiều cao còn lại — làm cho
                    overflow-y-auto của <main> bên dưới không bao giờ kích hoạt
                    và cả trang bị đẩy tràn, không cuộn xem hết được. */}
                <div className="flex flex-1 min-h-0 overflow-hidden">
                    <SideBar activeTab={activeTab} setActiveTab={setActiveTab} isOpen={isSidebarOpen} />

                    <main
                        className={cn(
                            "flex-1 min-h-0 p-6",
                            activeTab === "departments"
                                ? "overflow-hidden flex flex-col"
                                : "overflow-y-auto"
                        )}
                    >
                        {activeTab === "employees" && <EmployeeProfilePage />}

                        {activeTab === "attendance" && (
                            <AttendanceView
                                records={attendanceRecords}
                                onClockIn={handleClockIn}
                                onClockOut={handleClockOut}
                                onEditRecord={handleEditRecord}
                            />
                        )}

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