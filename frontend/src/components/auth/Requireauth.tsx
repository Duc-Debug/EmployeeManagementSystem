import { useEffect, useState, type ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { clearAuthSession, type AuthUser } from "@/lib/auth-session";
import { getCurrentUser } from "@/lib/api/auth";

interface RequireAuthProps {
    children: ReactNode;
    allowPasswordChangeOnly?: boolean;
}

export default function RequireAuth({ children, allowPasswordChangeOnly = false }: RequireAuthProps) {
    const location = useLocation();
    const [authStatus, setAuthStatus] = useState<"checking" | "authenticated" | "unauthenticated">("checking");
    const [user, setUser] = useState<AuthUser | null>(null);

    useEffect(() => {
        let isMounted = true;

        // Security architecture: Zero-trust in client-side localStorage.
        // The server-side HttpOnly cookie session via /auth/me is the single source of truth.
        getCurrentUser()
            .then((currentUser) => {
                if (!isMounted) return;
                setUser(currentUser);
                setAuthStatus("authenticated");
            })
            .catch(() => {
                if (!isMounted) return;
                clearAuthSession();
                setUser(null);
                setAuthStatus("unauthenticated");
            });

        return () => {
            isMounted = false;
        };
    }, []);

    if (authStatus === "checking") {
        return (
            <div className="flex h-screen w-full items-center justify-center bg-slate-50">
                <div className="flex flex-col items-center gap-3">
                    <div className="h-8 w-8 animate-spin rounded-full border-3 border-indigo-600 border-t-transparent" />
                    <p className="text-sm font-medium text-slate-500">Đang xác thực phiên làm việc...</p>
                </div>
            </div>
        );
    }

    if (authStatus === "unauthenticated" || !user) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    // Nếu người dùng bắt buộc phải đổi mật khẩu và đang truy cập route khác /change-password
    if (user.requiresPasswordChange && !allowPasswordChangeOnly && location.pathname !== "/change-password") {
        return <Navigate to="/change-password" replace />;
    }

    return <>{children}</>;
}