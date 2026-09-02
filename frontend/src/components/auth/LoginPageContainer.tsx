import { useNavigate } from "react-router-dom";
import LoginPage, { type LoginCredentials } from "@/components/auth/LoginPage";

export default function LoginPageContainer() {
    const navigate = useNavigate();

    const handleLogin = async ({ username, password }: LoginCredentials) => {
        await new Promise((resolve) => setTimeout(resolve, 1000));
        // Kiểm tra thông tin đăng nhập demo
        if (username === "hung" && password === "123456") {
            // Lưu thông tin giả lập vào localStorage
            localStorage.setItem("accessToken", "demo-mock-token-123456");
            localStorage.setItem("tokenType", "Bearer");
            localStorage.setItem(
                "currentUser",
                JSON.stringify({
                    userId: 1,
                    username: "admin@company.com",
                    fullName: "Quản trị viên Demo",
                    roles: ["ADMIN"],
                })
            );

            alert("Đăng nhập thành công!");
            navigate("/");
        } else {
            throw new Error("Tài khoản hoặc mật khẩu không đúng!");
        }
    };

    return <LoginPage onLogin={handleLogin} />;
}