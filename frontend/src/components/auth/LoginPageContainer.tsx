import { useNavigate } from "react-router-dom";
import LoginPage, { type LoginCredentials } from "@/components/auth/LoginPage";
import { login } from "@/lib/api/auth";

export default function LoginPageContainer() {
    const navigate = useNavigate();

    const handleLogin = async ({ username, password }: LoginCredentials) => {
        try {
            // 1. Thử đăng nhập qua Backend API Spring Boot
            const user = await login({ username, password });
            const token = localStorage.getItem("nexushrm_auth_token") || "jwt-token";
            localStorage.setItem("accessToken", token);
            localStorage.setItem("currentUser", JSON.stringify(user));
            navigate("/");
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "Đăng nhập thất bại. Vui lòng kiểm tra lại tên đăng nhập và mật khẩu!";
            throw new Error(message);
        }
    };

    return <LoginPage onLogin={handleLogin} />;
}