import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { clearAuthSession, getStoredUser, setStoredUser, type AuthUser } from "@/lib/auth-session";
import { getCurrentUser } from "@/lib/api/auth";
import PageLoading from "@/components/common/PageLoading";

interface RequireAuthProps {
    children: ReactNode;
    allowPasswordChangeOnly?: boolean;
}

export default function RequireAuth({ children, allowPasswordChangeOnly = false }: RequireAuthProps) {
    const location = useLocation();
    const storedUser = getStoredUser();
    const [user, setUser] = useState<AuthUser | null>(storedUser);
    const [loading, setLoading] = useState<boolean>(!storedUser);

    useEffect(() => {
        if (!user) {
            getCurrentUser()
                .then((fetchedUser) => {
                    setUser(fetchedUser);
                    setStoredUser(fetchedUser);
                    setLoading(false);
                })
                .catch(() => {
                    clearAuthSession();
                    setUser(null);
                    setLoading(false);
                });
        }
    }, [user]);

    if (loading) {
        return <PageLoading />;
    }

    if (!user) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    // Nếu người dùng bắt buộc phải đổi mật khẩu và đang truy cập route khác /change-password
    if (user?.requiresPasswordChange && !allowPasswordChangeOnly && location.pathname !== "/change-password") {
        return <Navigate to="/change-password" replace />;
    }

    return <>{children}</>;
}