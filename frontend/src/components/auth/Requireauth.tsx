import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";

export default function RequireAuth({ children }: { children: ReactNode }) {
    const token = localStorage.getItem("accessToken");
    console.log("Current token in RequireAuth:", token);
    if (!token || token === "undefined" || token === "null" || token.trim() === "") {
        return <Navigate to="/login" replace />;
    }
    return <>{children}</>;
}