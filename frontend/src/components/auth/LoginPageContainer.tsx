import { useNavigate } from "react-router-dom";
import LoginPage, { type LoginCredentials } from "@/components/auth/LoginPage";
import { login } from "@/lib/api/auth";

export default function LoginPageContainer() {
    const navigate = useNavigate();

    const handleLogin = async ({ username, password }: LoginCredentials) => {
        try {
            // 1. Thử đăng nhập qua Backend API Spring Boot
            const user = await login({ username, password });
            localStorage.setItem("accessToken", localStorage.getItem("auth_token") || "jwt-token");
            localStorage.setItem("currentUser", JSON.stringify(user));
            navigate("/");
        } catch (err: unknown) {
            // 2. Nếu BE chưa bật hoặc lỗi mạng, kiểm tra tài khoản demo để fallback
            if (username === "hung" && password === "123456") {
                localStorage.setItem("accessToken", "demo-mock-token-123456");
                localStorage.setItem("tokenType", "Bearer");
                localStorage.setItem(
                    "currentUser",
                    JSON.stringify({
                        userId: 1,
                        username: "hung",
                        fullName: "Chu Văn Hưng",
                        roles: ["VT-06"],
                    })
                );
                navigate("/");
                return;
            }

            const message = err instanceof Error ? err.message : "Tài khoản hoặc mật khẩu không chính xác!";
            throw new Error(message);
        }
    };

    return <LoginPage onLogin={handleLogin} />;
}