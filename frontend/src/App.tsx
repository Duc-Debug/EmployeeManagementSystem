import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route, Navigate, useNavigate, useLocation } from "react-router-dom";
import LoginRoute from "./app/login/Page";
import RequireAuth from "./components/auth/Requireauth";
import DashboardLayout from "./components/dashboard/DashboardLayout";
import DashboardRouteGuard from "./components/auth/DashboardRouteGuard";
import PageLoading from "./components/common/PageLoading";
import { useAuthUser } from "./lib/auth-session";

// Public & Auth Lazy Loaded Pages
const ResetPasswordPage = lazy(() => import("./components/auth/ResetPasswordPage"));
const ForceChangePasswordPage = lazy(() => import("./components/auth/ForceChangePasswordPage"));
const NotFoundPage = lazy(() => import("./pages/NotFoundPage"));

// Dashboard Route Lazy Loaded Pages
const DashboardOverview = lazy(() => import("./components/dashboard/DashboardOverview"));
const CapacityDashboardView = lazy(() => import("./components/capacity/CapacityDashboardView"));
const ProjectView = lazy(() => import("./components/project/ProjectView"));
const CompanyWeeklyCapacityView = lazy(() => import("./components/capacity/CompanyWeeklyCapacityView"));
const ScheduleConflictWarningView = lazy(() => import("./components/scheduleconflict/ScheduleConflictWarningView"));
const SimulationScenarioListView = lazy(() =>
    import("./components/scenario/SimulationScenarioListView").then((m) => ({ default: m.SimulationScenarioListView }))
);
const OutsourcedContractWarningView = lazy(() => import("./components/outsourcedcontract/OutsourcedContractWarningView"));
const MyWeeklySchedulePage = lazy(() => import("./features/my-schedule/pages/MyWeeklySchedulePage"));
const AttendancePage = lazy(() => import("./pages/AttendancePage"));
const UpcomingWorkloadView = lazy(() => import("./components/workload/UpcomingWorkloadView"));
const WeeklyAvailabilityView = lazy(() => import("./components/availability/WeeklyAvailabilityView"));
const LeaveManagementView = lazy(() => import("./components/leave/LeaveManagementView"));
const WorkingCalendarConfigView = lazy(() => import("./components/calendar/WorkingCalendarConfigView"));
const BillableRateReportView = lazy(() => import("./components/reports/BillableRateReportView"));
const ProjectAllocationReportView = lazy(() => import("./components/reports/ProjectAllocationReportView"));
const TimesheetVarianceReportView = lazy(() => import("./components/reports/TimesheetVarianceReportView"));
const RecruitmentDemandReportView = lazy(() => import("./components/reports/RecruitmentDemandReportView"));
const CapacityForecastReportView = lazy(() => import("./components/reports/CapacityForecastReportView"));
const HrProfilePage = lazy(() => import("./components/hrprofile/HrProfilePage"));
const DepartmentsView = lazy(() => import("./components/department/DepartmentsView"));
const SkilldeclarationView = lazy(() => import("./components/skilldeclaration/SkilldeclarationView"));
const EmployeeProfilePage = lazy(() => import("./pages/EmployeeProfilePage"));
const ProjectRoleCatalogView = lazy(() => import("./components/rolecatalog/ProjectRoleCatalogView"));
const BackupManagementWorkspace = lazy(() => import("@/features/backup/BackupManagementWorkspace"));
const EmployeeImportView = lazy(() => import("./components/import/EmployeeImportView"));
const AccessControlView = lazy(() => import("./components/access/AccessControlView"));

function CapacityDashboardRoute() {
    const navigate = useNavigate();
    return (
        <CapacityDashboardView
            onNavigate={(tab) => navigate(tab === "overview" ? "/dashboard/overview" : `/dashboard/${tab}`)}
        />
    );
}

function WorkloadRoute() {
    const navigate = useNavigate();
    return (
        <UpcomingWorkloadView
            onNavigateToProjects={() => navigate("/dashboard/projects")}
            onNavigateToLeave={() => navigate("/dashboard/leave")}
        />
    );
}

function UnavailabilityRoute() {
    const user = useAuthUser();
    const role = user?.roleCode?.toUpperCase().replace(/_/g, "-");
    const isEmployee = role === "VT-04" || role === "ROLE-EMPLOYEE" || role === "EMPLOYEE";
    return <Navigate to={isEmployee ? "/dashboard/my-schedule" : "/dashboard/overview"} replace />;
}

function LegacyRedirect({ to }: { to: string }) {
    const location = useLocation();
    return <Navigate to={`${to}${location.search}`} replace />;
}

function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* 1. Public Routes */}
                <Route path="/login" element={<LoginRoute />} />
                <Route
                    path="/reset-password"
                    element={
                        <Suspense fallback={<PageLoading />}>
                            <ResetPasswordPage />
                        </Suspense>
                    }
                />

                {/* 2. Change Password Route (Protected, Mandatory on first login) */}
                <Route
                    path="/change-password"
                    element={
                        <RequireAuth allowPasswordChangeOnly>
                            <Suspense fallback={<PageLoading />}>
                                <ForceChangePasswordPage />
                            </Suspense>
                        </RequireAuth>
                    }
                />

                {/* 3. Root Redirect */}
                <Route
                    path="/"
                    element={
                        <RequireAuth>
                            <Navigate to="/dashboard/overview" replace />
                        </RequireAuth>
                    }
                />

                {/* 4. Protected Nested Dashboard Routes */}
                <Route
                    path="/dashboard"
                    element={
                        <RequireAuth>
                            <DashboardLayout />
                        </RequireAuth>
                    }
                >
                    <Route index element={<Navigate to="overview" replace />} />
                    <Route path="overview" element={<DashboardOverview />} />
                    <Route path="reports" element={<DashboardOverview />} />

                    <Route
                        path="capacity-dashboard"
                        element={
                            <DashboardRouteGuard tabId="capacity-dashboard">
                                <CapacityDashboardRoute />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="project"
                        element={
                            <DashboardRouteGuard tabId="project">
                                <ProjectView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="projects"
                        element={
                            <DashboardRouteGuard tabId="project">
                                <ProjectView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="capacity"
                        element={
                            <DashboardRouteGuard tabId="capacity">
                                <CompanyWeeklyCapacityView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="schedule-conflict"
                        element={
                            <DashboardRouteGuard tabId="schedule-conflict">
                                <ScheduleConflictWarningView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="simulation-scenarios"
                        element={
                            <DashboardRouteGuard tabId="simulation-scenarios">
                                <SimulationScenarioListView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="outsourced-contracts"
                        element={
                            <DashboardRouteGuard tabId="outsourced-contracts">
                                <OutsourcedContractWarningView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="my-schedule"
                        element={
                            <DashboardRouteGuard tabId="my-schedule">
                                <MyWeeklySchedulePage />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="attendance"
                        element={
                            <DashboardRouteGuard tabId="attendance">
                                <AttendancePage />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="timesheet"
                        element={
                            <DashboardRouteGuard tabId="attendance">
                                <AttendancePage />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="workload"
                        element={
                            <DashboardRouteGuard tabId="workload">
                                <WorkloadRoute />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="availability"
                        element={
                            <DashboardRouteGuard tabId="availability">
                                <WeeklyAvailabilityView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route path="unavailability" element={<UnavailabilityRoute />} />
                    <Route
                        path="leave"
                        element={
                            <DashboardRouteGuard tabId="leave">
                                <LeaveManagementView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="working-calendar"
                        element={
                            <DashboardRouteGuard tabId="working-calendar">
                                <WorkingCalendarConfigView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="billable-rate"
                        element={
                            <DashboardRouteGuard tabId="billable-rate">
                                <BillableRateReportView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="project-allocation-report"
                        element={
                            <DashboardRouteGuard tabId="project-allocation-report">
                                <ProjectAllocationReportView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="project-allocation"
                        element={
                            <DashboardRouteGuard tabId="project-allocation-report">
                                <ProjectAllocationReportView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="timesheet-variance"
                        element={
                            <DashboardRouteGuard tabId="timesheet-variance">
                                <TimesheetVarianceReportView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="recruitment-demand"
                        element={
                            <DashboardRouteGuard tabId="recruitment-demand">
                                <RecruitmentDemandReportView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="capacity-forecast"
                        element={
                            <DashboardRouteGuard tabId="capacity-forecast">
                                <CapacityForecastReportView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="hrprofile"
                        element={
                            <DashboardRouteGuard tabId="hrprofile">
                                <HrProfilePage />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="employees"
                        element={
                            <DashboardRouteGuard tabId="hrprofile">
                                <HrProfilePage />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="departments"
                        element={
                            <DashboardRouteGuard tabId="departments">
                                <DepartmentsView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="skills"
                        element={
                            <DashboardRouteGuard tabId="skills">
                                <SkilldeclarationView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="users"
                        element={
                            <DashboardRouteGuard tabId="users">
                                <EmployeeProfilePage />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="roles"
                        element={
                            <DashboardRouteGuard tabId="roles">
                                <ProjectRoleCatalogView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="backup"
                        element={
                            <DashboardRouteGuard tabId="backup">
                                <BackupManagementWorkspace />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="data-import"
                        element={
                            <DashboardRouteGuard tabId="data-import">
                                <EmployeeImportView />
                            </DashboardRouteGuard>
                        }
                    />
                    <Route
                        path="access"
                        element={
                            <DashboardRouteGuard tabId="access">
                                <AccessControlView />
                            </DashboardRouteGuard>
                        }
                    />

                    {/* Unknown route inside dashboard */}
                    <Route path="*" element={<NotFoundPage />} />
                </Route>

                {/* Legacy Top-level Redirects (Preserves search params like ?projectId=... or ?taskId=...) */}
                <Route path="/projects" element={<LegacyRedirect to="/dashboard/projects" />} />
                <Route path="/project" element={<LegacyRedirect to="/dashboard/project" />} />
                <Route path="/my-schedule" element={<LegacyRedirect to="/dashboard/my-schedule" />} />
                <Route path="/capacity" element={<LegacyRedirect to="/dashboard/capacity" />} />
                <Route path="/attendance" element={<LegacyRedirect to="/dashboard/attendance" />} />
                <Route path="/timesheet" element={<LegacyRedirect to="/dashboard/timesheet" />} />
                <Route path="/leave" element={<LegacyRedirect to="/dashboard/leave" />} />
                <Route path="/schedule-conflict" element={<LegacyRedirect to="/dashboard/schedule-conflict" />} />
                <Route path="/outsourced-contracts" element={<LegacyRedirect to="/dashboard/outsourced-contracts" />} />
                <Route path="/skills" element={<LegacyRedirect to="/dashboard/skills" />} />

                {/* 5. Catch-all 404 Route */}
                <Route
                    path="*"
                    element={
                        <Suspense fallback={<PageLoading />}>
                            <NotFoundPage />
                        </Suspense>
                    }
                />
            </Routes>
        </BrowserRouter>
    );
}

export default App;