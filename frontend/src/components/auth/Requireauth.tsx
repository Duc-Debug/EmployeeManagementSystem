import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";

export default function RequireAuth({ children }: { children: ReactNode }) {
    const token = localStorage.getItem("accessToken") || localStorage.getItem("nexushrm_auth_token");
    if (!token || token === "undefined" || token === "null" || token.trim() === "") {
        return <Navigate to="/login" replace />;
    }
    return <>{children}</>;
}