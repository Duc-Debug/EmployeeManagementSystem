import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { getAuthToken, getStoredUser } from "@/lib/auth-session";

interface RequireAuthProps {
    children: ReactNode;
    allowPasswordChangeOnly?: boolean;
}

export default function RequireAuth({ children, allowPasswordChangeOnly = false }: RequireAuthProps) {
    const location = useLocation();
    const token = getAuthToken();
    const user = getStoredUser();
    const isAuthenticated = Boolean(user && user.id) || Boolean(token && token !== "undefined" && token !== "null" && token.trim() !== "");

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    // Nếu người dùng bắt buộc phải đổi mật khẩu và đang truy cập route khác /change-password
    if (user?.requiresPasswordChange && !allowPasswordChangeOnly && location.pathname !== "/change-password") {
        return <Navigate to="/change-password" replace />;
    }

    return <>{children}</>;
}