import { useState, useMemo, useEffect, lazy, Suspense } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { ShieldAlert } from "lucide-react";
import { cn } from "@/lib/utils";
import SideBar, { canAccessTab } from "./SideBar";
import { resolveActiveTab } from "./dashboard-routing";
import Header from "./Header";
import DepartmentsView from "../department/DepartmentsView";
import EmployeeProfilePage from "../../pages/EmployeeProfilePage";
import HrProfilePage from "../hrprofile/HrProfilePage";
import AttendanceView from "../attendance/AttendanceView";
import SkilldeclarationView from "../skilldeclaration/SkilldeclarationView";
import ProjectView from "../project/ProjectView";
import AccessControlView from "../access/AccessControlView";
import LeaveManagementView from "../leave/LeaveManagementView";
import WeeklyAvailabilityView from "../availability/WeeklyAvailabilityView";
import UnavailabilityView from "../unavailability/UnavailabilityView";
import WorkingCalendarConfigView from "../calendar/WorkingCalendarConfigView";
import UpcomingWorkloadView from "../workload/UpcomingWorkloadView";
import RecruitmentDemandReportView from "../reports/RecruitmentDemandReportView";
import CapacityForecastReportView from "../reports/CapacityForecastReportView";
import ProjectAllocationReportView from "../reports/ProjectAllocationReportView";
import TimesheetVarianceReportView from "../reports/TimesheetVarianceReportView";
import BillableRateReportView from "../reports/BillableRateReportView";
import CompanyWeeklyCapacityView from "../capacity/CompanyWeeklyCapacityView";
import CapacityDashboardView from "../capacity/CapacityDashboardView";
import ProjectRoleCatalogView from "../rolecatalog/ProjectRoleCatalogView";
import ScheduleConflictWarningView from "../scheduleconflict/ScheduleConflictWarningView";
import { SimulationScenarioListView } from "../scenario/SimulationScenarioListView";
import MyWeeklySchedulePage from "../../features/my-schedule/pages/MyWeeklySchedulePage";
import EmployeeImportView from "../import/EmployeeImportView";
import BackupManagementWorkspace from "@/features/backup/BackupManagementWorkspace";

const OutsourcedContractWarningView = lazy(() => import("../outsourcedcontract/OutsourcedContractWarningView"));
import AdminDashboardOverview from "./AdminDashboardOverview";
import PmDashboardOverview from "./PmDashboardOverview";
import RmDashboardOverview from "./RmDashboardOverview";
import ExecutiveDashboardOverview from "./ExecutiveDashboardOverview";
import EmployeeDashboardOverview from "./EmployeeDashboardOverview";
import HrDashboardOverview from "./HrDashboardOverview";
import type { AttendanceRecord } from "@/lib/hr-data";
import { useAuthUser } from "@/lib/auth-session";
import { getUsers } from "@/lib/api/users";

const INITIAL_ATTENDANCE_RECORDS: AttendanceRecord[] = [];

// Mã nhân viên đang thao tác ở "Trạm chấm công nhanh" — tạm gán cứng cho tới
// khi màn hình này đọc được người dùng đang đăng nhập từ auth thật.
const CURRENT_EMPLOYEE_ID = "NV001";

export default function Dashboard() {
    const location = useLocation();
    const navigate = useNavigate();
    const user = useAuthUser();

    // Đồng bộ URL trình duyệt với tab tương ứng
    const activeTab = useMemo(() => resolveActiveTab(location.pathname), [location.pathname]);

    const isTabAllowed = canAccessTab(user?.roleCode, activeTab, user?.dataScope, user?.permissions);

    const handleTabChange = (tabId: string) => {
        const targetPath = tabId === "overview" ? "/" : `/${tabId}`;
        navigate(targetPath);
    };

    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [attendanceRecords, setAttendanceRecords] = useState<AttendanceRecord[]>(
        INITIAL_ATTENDANCE_RECORDS
    );

    useEffect(() => {
        let isMounted = true;
        const normalizedRole = user?.roleCode ? user.roleCode.toUpperCase().replace(/_/g, "-") : "";
        const canFetchAllUsers = ["VT-01", "VT-05", "VT-06", "ROLE-HR", "HR", "ROLE-ADMIN", "ADMIN"].includes(normalizedRole);

        if (!canFetchAllUsers) {
            if (user) {
                setAttendanceRecords([
                    {
                        id: user.employeeCode || String(user.id),
                        name: user.fullName || user.username || "Nhân viên",
                        dept: user.orgUnitName || "Phòng ban",
                        inTime: "--:--",
                        outTime: "--:--",
                        hours: "0",
                        ot: "0",
                        status: "Đúng giờ",
                    },
                ]);
            }
            return;
        }

        async function fetchRealUsers() {
            try {
                const res = await getUsers(0, 50);
                if (!isMounted || !res?.content) return;
                const mapped: AttendanceRecord[] = res.content.map((u) => ({
                    id: String(u.id),
                    name: u.fullName || u.username,
                    dept: u.orgUnitName || "Chưa gán phòng",
                    inTime: "--:--",
                    outTime: "--:--",
                    hours: "0",
                    ot: "0",
                    status: u.status === "ACTIVE" ? "Đúng giờ" : "Vắng mặt",
                }));
                setAttendanceRecords(mapped);
            } catch {
                // User lacks permission or backend unavailable
            }
        }
        fetchRealUsers();
        return () => {
            isMounted = false;
        };
    }, [user]);

    const handleClockIn = () => {
        const time = new Date().toLocaleTimeString("en-US", {
            hour12: false,
            hour: "2-digit",
            minute: "2-digit",
        });
        const currentIdStr = user?.id != null ? String(user.id) : CURRENT_EMPLOYEE_ID;
        const currentName = user?.fullName || user?.username || "Tôi (Nhân viên)";
        const currentDept = user?.orgUnitName || "Phòng chuyên môn";

        setAttendanceRecords((prev) => {
            const index = prev.findIndex(
                (rec) =>
                    rec.id === currentIdStr ||
                    rec.id === user?.employeeCode ||
                    rec.name.toLowerCase() === currentName.toLowerCase()
            );
            if (index >= 0) {
                const next = [...prev];
                next[index] = { ...next[index], inTime: time, status: "Đúng giờ" };
                return next;
            }
            return [
                {
                    id: user?.employeeCode || currentIdStr,
                    name: currentName,
                    dept: currentDept,
                    inTime: time,
                    outTime: "--:--",
                    hours: "8.0",
                    ot: "0",
                    status: "Đúng giờ",
                },
                ...prev,
            ];
        });
        return time;
    };

    const handleClockOut = () => {
        const time = new Date().toLocaleTimeString("en-US", {
            hour12: false,
            hour: "2-digit",
            minute: "2-digit",
        });
        const currentIdStr = user?.id != null ? String(user.id) : CURRENT_EMPLOYEE_ID;
        const currentName = user?.fullName || user?.username || "Tôi (Nhân viên)";

        setAttendanceRecords((prev) =>
            prev.map((rec) =>
                rec.id === currentIdStr ||
                rec.id === user?.employeeCode ||
                rec.name.toLowerCase() === currentName.toLowerCase()
                    ? { ...rec, outTime: time }
                    : rec
            )
        );
        return true;
    };

    const handleEditRecord = (id: string) => {
        console.log("Sửa bản ghi chấm công:", id);
    };

    return (
        <div className="relative flex h-screen w-full flex-col overflow-hidden bg-[#f8fafc] text-slate-800 antialiased">
            {/* ---------- ambient clean light backdrop ---------- */}
            <div className="pointer-events-none fixed inset-0 z-0 bg-slate-50/60" aria-hidden="true" />

            <div className="relative z-10 flex h-full w-full flex-col">
                <Header setIsSidebarOpen={setIsSidebarOpen} />

                <div className="flex flex-1 min-h-0 overflow-hidden">
                    <SideBar activeTab={activeTab} setActiveTab={handleTabChange} isOpen={isSidebarOpen} />

                    <main
                        className={cn(
                            "flex-1 min-h-0 p-6",
                            activeTab === "departments" && isTabAllowed
                                ? "overflow-hidden flex flex-col"
                                : "overflow-y-auto"
                        )}
                    >
                        {!isTabAllowed ? (
                            <div className="flex flex-col items-center justify-center min-h-[400px] text-center p-8 bg-white rounded-3xl border border-slate-200 shadow-xs animate-in fade-in duration-150">
                                <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-rose-50 text-rose-600 mb-4 border border-rose-100">
                                    <ShieldAlert className="h-8 w-8" />
                                </div>
                                <h3 className="text-base font-bold text-slate-900 mb-1">
                                    Không có quyền truy cập
                                </h3>
                                <p className="text-xs text-slate-500 max-w-md mb-6 leading-relaxed">
                                    Tài khoản của bạn ({user?.roleName || user?.roleCode || "Người dùng"}) không được phân quyền truy cập chức năng này. Vui lòng liên hệ Quản trị viên nếu cần hỗ trợ.
                                </p>
                                <button
                                    onClick={() => handleTabChange("overview")}
                                    type="button"
                                    className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs"
                                >
                                    Quay lại Trang chủ
                                </button>
                            </div>
                        ) : (
                            <>
                                {activeTab === "users" && <EmployeeProfilePage />}

                                {activeTab === "hrprofile" && <HrProfilePage />}

                                {activeTab === "capacity-dashboard" && <CapacityDashboardView onNavigate={handleTabChange} />}

                                {activeTab === "capacity" && <CompanyWeeklyCapacityView />}

                                {activeTab === "my-schedule" && <MyWeeklySchedulePage />}

                                {activeTab === "availability" && <WeeklyAvailabilityView />}

                                {activeTab === "unavailability" && <UnavailabilityView />}

                                {activeTab === "workload" && (
                                    <UpcomingWorkloadView
                                        onNavigateToProjects={() => handleTabChange("project")}
                                        onNavigateToLeave={() => handleTabChange("leave")}
                                    />
                                )}

                                {activeTab === "working-calendar" && <WorkingCalendarConfigView />}

                                {activeTab === "attendance" && (
                                    <AttendanceView
                                        records={attendanceRecords}
                                        onClockIn={handleClockIn}
                                        onClockOut={handleClockOut}
                                        onEditRecord={handleEditRecord}
                                    />
                                )}

                                {activeTab === "departments" && <DepartmentsView />}

                                {activeTab === "skills" && <SkilldeclarationView />}

                                {activeTab === "project" && <ProjectView />}

                                {activeTab === "roles" && <ProjectRoleCatalogView />}

                                {activeTab === "access" && <AccessControlView />}

                                {activeTab === "leave" && <LeaveManagementView />}

                                {activeTab === "recruitment-demand" && <RecruitmentDemandReportView />}

                                {activeTab === "simulation-scenarios" && <SimulationScenarioListView />}

                                 {activeTab === "capacity-forecast" && <CapacityForecastReportView />}

                                 {activeTab === "timesheet-variance" && <TimesheetVarianceReportView />}

                                 {activeTab === "schedule-conflict" && <ScheduleConflictWarningView />}

                                 {activeTab === "outsourced-contracts" && (
                                     <Suspense
                                         fallback={
                                             <div className="flex h-64 items-center justify-center">
                                                 <div className="flex flex-col items-center gap-2 text-slate-500">
                                                     <div className="h-8 w-8 animate-spin rounded-full border-4 border-indigo-600 border-t-transparent" />
                                                     <span className="text-xs">Đang tải Hợp đồng thuê ngoài...</span>
                                                 </div>
                                             </div>
                                         }
                                     >
                                         <OutsourcedContractWarningView />
                                     </Suspense>
                                 )}

                                {activeTab === "data-import" && <EmployeeImportView />}

                                {activeTab === "backup" && <BackupManagementWorkspace />}

                                {activeTab === "project-allocation-report" && <ProjectAllocationReportView />}

                                {activeTab === "billable-rate" && <BillableRateReportView />}
                                {(activeTab === "overview" || activeTab === "reports") && (() => {
                                    const role = user?.roleCode?.toUpperCase().replace(/_/g, "-");
                                    if (role === "VT-01" || role === "ROLE-EXECUTIVE" || role === "EXECUTIVE" || role === "DIRECTOR") {
                                        return <ExecutiveDashboardOverview onNavigate={handleTabChange} />;
                                    }
                                    if (role === "VT-06" || role === "ROLE-ADMIN" || role === "ADMIN") {
                                        return <AdminDashboardOverview onNavigate={handleTabChange} />;
                                    }
                                    if (role === "VT-02" || role === "ROLE-PM" || role === "PM" || role === "PROJECT-MANAGER") {
                                        return <PmDashboardOverview onNavigate={handleTabChange} />;
                                    }
                                    if (role === "VT-03" || role === "ROLE-RM" || role === "RM" || role === "RESOURCE-MANAGER") {
                                        return <RmDashboardOverview onNavigate={handleTabChange} />;
                                    }
                                    if (role === "VT-05" || role === "ROLE-HR" || role === "HR" || role === "HR-MANAGER" || role === "HR-SPECIALIST") {
                                        return <HrDashboardOverview onNavigate={handleTabChange} />;
                                    }
                                    if (role === "VT-04" || role === "ROLE-EMPLOYEE" || role === "EMPLOYEE" || role === "MEMBER" || role === "DEVELOPER") {
                                        return <EmployeeDashboardOverview onNavigate={handleTabChange} />;
                                    }
                                    return <EmployeeDashboardOverview onNavigate={handleTabChange} />;
                                })()}
                            </>
                        )}
                    </main>
                </div>
            </div>
        </div>
    );
}
