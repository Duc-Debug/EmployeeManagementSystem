import { useEffect, type ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { getStoredUser } from "@/lib/auth-session";
import { getCurrentUser } from "@/lib/api/auth";

interface RequireAuthProps {
    children: ReactNode;
    allowPasswordChangeOnly?: boolean;
}

export default function RequireAuth({ children, allowPasswordChangeOnly = false }: RequireAuthProps) {
    const location = useLocation();
    const user = getStoredUser();
    const isAuthenticated = Boolean(user && user.id);

    useEffect(() => {
        if (isAuthenticated) {
            // Asynchronously revalidate session against backend /auth/me (source of truth)
            // If localStorage was tampered, getCurrentUser() overwrites with genuine server data.
            // If session cookie is invalid/expired, apiRequest triggers 401 and clears session.
            getCurrentUser().catch(() => {
                // Handled by apiRequest 401 interceptor
            });
        }
    }, [isAuthenticated]);

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    // Nếu người dùng bắt buộc phải đổi mật khẩu và đang truy cập route khác /change-password
    if (user?.requiresPasswordChange && !allowPasswordChangeOnly && location.pathname !== "/change-password") {
        return <Navigate to="/change-password" replace />;
    }

    return <>{children}</>;
}