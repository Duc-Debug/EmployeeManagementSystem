import { useEffect } from "react";
import { Navigate } from "react-router-dom";
import LoginPageContainer from "@/components/auth/LoginPageContainer";
import { getAuthToken, getStoredUser } from "@/lib/auth-session";

export default function LoginRoute() {
    const token = getAuthToken();
    const user = getStoredUser();

    useEffect(() => {
        document.title = "Đăng nhập quản trị | Employee Management System";
    }, []);

    // Chỉ tự động chuyển hướng khi cả token và thông tin user đều hợp lệ
    if (token && token !== "undefined" && token !== "null" && token.trim() !== "" && user) {
        if (user.requiresPasswordChange) {
            return <Navigate to="/change-password" replace />;
        }
        return <Navigate to="/dashboard/overview" replace />;
    }

    return <LoginPageContainer />;
}