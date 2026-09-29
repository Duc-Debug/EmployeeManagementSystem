import { useEffect } from "react";
import { Navigate } from "react-router-dom";
import LoginPageContainer from "@/components/auth/LoginPageContainer";
import { getStoredUser } from "@/lib/auth-session";

export default function LoginRoute() {
    const user = getStoredUser();

    useEffect(() => {
        document.title = "Đăng nhập quản trị | Employee Management System";
    }, []);

    // Tự động chuyển hướng khi thông tin user hợp lệ
    if (user) {
        if (user.requiresPasswordChange) {
            return <Navigate to="/change-password" replace />;
        }
        return <Navigate to="/dashboard/overview" replace />;
    }

    return <LoginPageContainer />;
}