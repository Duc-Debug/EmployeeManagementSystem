import { ShieldAlert } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuthUser } from "@/lib/auth-session";
import { canAccessTab } from "@/components/dashboard/SideBar";

interface DashboardRouteGuardProps {
    tabId: string;
    children: React.ReactNode;
}

export default function DashboardRouteGuard({ tabId, children }: DashboardRouteGuardProps) {
    const user = useAuthUser();
    const navigate = useNavigate();
    const isAllowed = canAccessTab(user?.roleCode, tabId, user?.dataScope, user?.permissions);

    if (!isAllowed) {
        return (
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
                    onClick={() => navigate("/dashboard/overview")}
                    type="button"
                    className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-700 transition shadow-xs"
                >
                    Quay lại Trang chủ
                </button>
            </div>
        );
    }

    return <>{children}</>;
}
