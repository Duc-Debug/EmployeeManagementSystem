import LoginPageContainer from "@/components/auth/LoginPageContainer";
import { useEffect } from "react";

export default function LoginRoute() {
    useEffect(() => {
        document.title = "Đăng nhập quản trị | Employee Management System";
        localStorage.removeItem("accessToken");
        localStorage.removeItem("token");
        sessionStorage.clear();
    }, []);

    return <LoginPageContainer />;
}