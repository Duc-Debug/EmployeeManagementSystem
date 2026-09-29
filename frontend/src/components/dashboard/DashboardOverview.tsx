import { lazy, Suspense } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthUser } from "@/lib/auth-session";
import { normalizeRoleCode } from "@/lib/role-utils";
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

    const role = normalizeRoleCode(user?.roleCode);

    const renderRoleOverview = () => {
        switch (role) {
            case "VT-01":
                return <ExecutiveDashboardOverview onNavigate={handleNavigate} />;
            case "VT-02":
                return <PmDashboardOverview onNavigate={handleNavigate} />;
            case "VT-03":
                return <RmDashboardOverview onNavigate={handleNavigate} />;
            case "VT-05":
                return <HrDashboardOverview onNavigate={handleNavigate} />;
            case "VT-06":
                return <AdminDashboardOverview onNavigate={handleNavigate} />;
            case "VT-04":
            default:
                return <EmployeeDashboardOverview onNavigate={handleNavigate} />;
        }
    };

    return (
        <Suspense fallback={<PageLoading />}>
            {renderRoleOverview()}
        </Suspense>
    );
}
