import { useState } from "react";
import SideBar from "./SideBar";
import Header from "./Header";
import DashboardHeader from "./DashboardHeader";
import KpiStatsSection from "../kpi/KpiStatsSection";
import CalendarView from "../calendar/CalendarView";
import DepartmentsView from "../department/DepartmentsView";
import EmployeeProfilePage from "../../pages/EmployeeProfilePage";
import AccessControlView from "../access/AccessControlView";
import AttendanceView from "../attendance/AttendanceView";
import SkilldeclarationView from "../skilldeclaration/SkilldeclarationView";
import type { AttendanceRecord } from "@/lib/hr-data";
import { INITIAL_DEPARTMENTS } from "../department/department.constants";
import type { Department } from "../department/department.constants";
import type { ResourceEmployee } from "../skilldeclaration/SkillresourceSearch";

// Dữ liệu nhân sự mẫu — thay bằng dữ liệu thật (API/store) khi có sẵn.
// Đây là nguồn dùng chung cho cả module "Phòng ban" và "Khai báo kỹ năng"
// để 2 màn hình luôn đồng bộ với nhau.
const INITIAL_EMPLOYEES: ResourceEmployee[] = [
    {
        id: 'emp-001',
        code: 'NV-014',
        name: 'Nguyễn Văn An',
        title: 'Senior Backend Developer',
        department: 'Phòng Công nghệ',
        availability: 'busy',
        availabilityPercent: 0,
        skills: [
            { skillId: 'java', name: 'Java', level: 5 },
            { skillId: 'docker', name: 'Docker', level: 4 },
            { skillId: 'kubernetes', name: 'Kubernetes', level: 3 },
        ],
    },
    {
        id: 'emp-002',
        code: 'NV-027',
        name: 'Trần Thị Bích',
        title: 'Frontend Developer',
        department: 'Phòng Công nghệ',
        availability: 'full',
        availabilityPercent: 100,
        skills: [
            { skillId: 'react', name: 'React.js', level: 5 },
            { skillId: 'typescript', name: 'TypeScript', level: 4 },
            { skillId: 'nodejs', name: 'Node.js', level: 3 },
        ],
    },
    {
        id: 'emp-003',
        code: 'NV-055',
        name: 'Lê Hoàng Nam',
        title: 'DevOps Engineer',
        department: 'Phòng Nhân sự',
        availability: 'partial',
        availabilityPercent: 40,
        skills: [
            { skillId: 'docker', name: 'Docker', level: 5 },
            { skillId: 'kubernetes', name: 'Kubernetes', level: 5 },
            { skillId: 'aws', name: 'AWS', level: 4 },
        ],
    },
    {
        id: 'emp-004',
        code: 'NV-061',
        name: 'Phạm Thu Hà',
        title: 'QA Engineer',
        department: 'Phòng Marketing',
        availability: 'full',
        availabilityPercent: 100,
        skills: [
            { skillId: 'selenium', name: 'Selenium', level: 4 },
            { skillId: 'sql', name: 'SQL', level: 3 },
            { skillId: 'java', name: 'Java', level: 2 },
        ],
    },
    {
        id: 'emp-005',
        code: 'NV-072',
        name: 'Vũ Đức Minh',
        title: 'Fullstack Developer',
        department: 'Phòng Kinh doanh',
        availability: 'partial',
        availabilityPercent: 20,
        skills: [
            { skillId: 'react', name: 'React.js', level: 4 },
            { skillId: 'nodejs', name: 'Node.js', level: 5 },
            { skillId: 'typescript', name: 'TypeScript', level: 3 },
        ],
    },
    {
        id: 'emp-006',
        code: 'NV-088',
        name: 'Đỗ Ngọc Lan',
        title: 'Data Engineer',
        department: 'Phòng Tài chính',
        availability: 'busy',
        availabilityPercent: 0,
        skills: [
            { skillId: 'python', name: 'Python', level: 5 },
            { skillId: 'sql', name: 'SQL', level: 5 },
            { skillId: 'aws', name: 'AWS', level: 3 },
        ],
    },
];

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

    // State phòng ban & nhân sự dùng chung giữa tab "Phòng ban" và tab
    // "Khai báo kỹ năng" (mục Tra cứu & Tìm kiếm nhân sự) để 2 nơi luôn đồng bộ.
    const [departments, setDepartments] = useState<Department[]>(INITIAL_DEPARTMENTS);
    const [employees, setEmployees] = useState<ResourceEmployee[]>(INITIAL_EMPLOYEES);

    // Xử lý Thêm/Sửa phòng ban
    const handleSaveDepartment = (dept: Department) => {
        const oldDept = departments.find((d) => d.id === dept.id);

        setDepartments((prev) => {
            const exists = prev.some((d) => d.id === dept.id);
            return exists ? prev.map((d) => (d.id === dept.id ? dept : d)) : [...prev, dept];
        });

        // Nếu tên phòng ban thay đổi, cập nhật lại tên phòng ban cho các nhân sự
        // thuộc phòng đó để đồng bộ với mục "Tra cứu & Tìm kiếm nhân sự"
        if (oldDept && oldDept.name !== dept.name) {
            setEmployees((prev) =>
                prev.map((emp) =>
                    emp.department === oldDept.name ? { ...emp, department: dept.name } : emp
                )
            );
        }
    };

    // Xử lý Xóa phòng ban
    const handleDeleteDepartment = (id: string) => {
        const deletedDept = departments.find((d) => d.id === id);
        setDepartments((prev) => prev.filter((d) => d.id !== id));

        // Cập nhật lại nhân sự nếu phòng ban bị xóa
        if (deletedDept) {
            setEmployees((prev) => prev.filter((emp) => emp.department !== deletedDept.name));
        }
    };

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

    // Thêm phòng ban từ modal "Danh sách phòng ban" trong Phân quyền — tạo đủ
    // các trường (manager, count) để tương thích với dữ liệu phòng ban gốc.
    const handleAddDepartmentFromAccess = (name: string) => {
        handleSaveDepartment({
            id: `dept-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
            name,
            manager: "",
            count: 0,
        });
    };

    const handleEditRecord = (id: string) => {
        // TODO: mở modal/điều hướng chỉnh sửa bản ghi chấm công theo id.
        console.log("Sửa bản ghi chấm công:", id);
    };

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

                {/* min-h-0 là bắt buộc: mặc định flex item có min-height: auto,
                    khiến hàng chứa Sidebar + main tự giãn theo chiều cao nội
                    dung thay vì bị giới hạn theo chiều cao còn lại — làm cho
                    overflow-y-auto của <main> bên dưới không bao giờ kích hoạt
                    và cả trang bị đẩy tràn, không cuộn xem hết được. */}
                <div className="flex flex-1 min-h-0 overflow-hidden">
                    <SideBar activeTab={activeTab} setActiveTab={setActiveTab} isOpen={isSidebarOpen} />

                    <main className="flex-1 min-h-0 overflow-y-auto p-6">
                        {/* Thêm điều kiện render theo activeTab tại đây */}
                        {activeTab === "employees" && <EmployeeProfilePage />}

                        {activeTab === "attendance" && (
                            <AttendanceView
                                records={attendanceRecords}
                                onClockIn={handleClockIn}
                                onClockOut={handleClockOut}
                                onEditRecord={handleEditRecord}
                            />
                        )}

                        {activeTab === "departments" && (
                            <DepartmentsView
                                departments={departments}
                                onSaveDepartment={handleSaveDepartment}
                                onDeleteDepartment={handleDeleteDepartment}
                            />
                        )}

                        {activeTab === "access" && (
                            <AccessControlView
                                departments={departments}
                                onAddDepartment={handleAddDepartmentFromAccess}
                                onRemoveDepartment={handleDeleteDepartment}
                            />
                        )}

                        {activeTab === "skills" && (
                            <SkilldeclarationView departments={departments} employees={employees} />
                        )}

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