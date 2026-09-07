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
            // 2. Nếu BE chưa bật hoặc lỗi mạng, kiểm tra tài khoản demo để fallback
            if ((username === "hung" || username === "admin") && (password === "123456" || password === "admin123")) {
                const demoUser = {
                    dataScope: "COMPANY" as const,
                    email: `${username}@hrm.local`,
                    employeeCode: "EMP-DEMO",
                    fullName: username === "admin" ? "Quản trị viên Hệ thống" : "Người dùng Demo",
                    id: 1,
                    orgUnitId: 1,
                    orgUnitName: "Ban Giám Đốc",
                    roleCode: "VT-06" as const,
                    roleName: "Quản trị viên",
                    scopeOrgUnitId: null,
                    status: "ACTIVE" as const,
                    username,
                };
                localStorage.setItem("accessToken", "demo-mock-token-123456");
                localStorage.setItem("tokenType", "Bearer");
                localStorage.setItem("nexushrm_auth_token", "demo-mock-token-123456");
                localStorage.setItem("nexushrm_auth_user", JSON.stringify(demoUser));
                localStorage.setItem("currentUser", JSON.stringify(demoUser));
                navigate("/");
                return;
            }

            const message = err instanceof Error ? err.message : "Tài khoản hoặc mật khẩu không chính xác!";
            throw new Error(message);
        }
    };

    return <LoginPage onLogin={handleLogin} />;
}