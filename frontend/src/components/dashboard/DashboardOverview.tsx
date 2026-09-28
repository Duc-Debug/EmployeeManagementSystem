import { lazy, Suspense } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthUser } from "@/lib/auth-session";
import PageLoading from "../common/PageLoading";

const AdminDashboardOverview = lazy(() => import("./AdminDashboardOverview"));
const PmDashboardOverview = lazy(() => import("./PmDashboardOverview"));
const RmDashboardOverview = lazy(() => import("./RmDashboardOverview"));
const ExecutiveDashboardOverview = lazy(() => import("./ExecutiveDashboardOverview"));
const EmployeeDashboardOverview = lazy(() => import("./EmployeeDashboardOverview"));
const HrDashboardOverview = lazy(() => import("./HrDashboardOverview"));

export default function DashboardOverview() {
    const user = useAuthUser();
    const navigate = useNavigate();

    const handleNavigate = (tabId: string) => {
        const targetPath = tabId === "overview" ? "/dashboard/overview" : `/dashboard/${tabId}`;
        navigate(targetPath);
    };

    const role = user?.roleCode?.toUpperCase().replace(/_/g, "-");

    const renderRoleOverview = () => {
        if (role === "VT-01" || role === "ROLE-EXECUTIVE" || role === "EXECUTIVE" || role === "DIRECTOR") {
            return <ExecutiveDashboardOverview onNavigate={handleNavigate} />;
        }
        if (role === "VT-06" || role === "ROLE-ADMIN" || role === "ADMIN") {
            return <AdminDashboardOverview onNavigate={handleNavigate} />;
        }
        if (role === "VT-02" || role === "ROLE-PM" || role === "PM" || role === "PROJECT-MANAGER") {
            return <PmDashboardOverview onNavigate={handleNavigate} />;
        }
        if (role === "VT-03" || role === "ROLE-RM" || role === "RM" || role === "RESOURCE-MANAGER") {
            return <RmDashboardOverview onNavigate={handleNavigate} />;
        }
        if (role === "VT-05" || role === "ROLE-HR" || role === "HR" || role === "HR-MANAGER" || role === "HR-SPECIALIST") {
            return <HrDashboardOverview onNavigate={handleNavigate} />;
        }
        return <EmployeeDashboardOverview onNavigate={handleNavigate} />;
    };

    return (
        <Suspense fallback={<PageLoading />}>
            {renderRoleOverview()}
        </Suspense>
    );
}
