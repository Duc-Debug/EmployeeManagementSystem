import { useAuthUser } from "@/lib/auth-session";

export default function DashboardHeader() {
    const user = useAuthUser();
    const userName = user?.fullName || user?.username || "bạn";
    const today = new Date();
    const formattedDate = today.toLocaleDateString("vi-VN", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
    });
    const formattedDateCapitalized =
        formattedDate.charAt(0).toUpperCase() + formattedDate.slice(1);
    return (
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
            <div>
                <p className="text-xs font-medium text-slate-500">
                    {formattedDateCapitalized}
                </p>
                <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                    Xin chào, {userName}!
                </h1>
            </div>
        </div>
    );
}