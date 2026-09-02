import { Download, Plus } from "lucide-react";
export default function DashboardHeader() {
    const userName = "Chu Văn Hưng";
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
                <p className="text-xs font-medium text-white/60">
                    {formattedDateCapitalized}
                </p>
                <h1 className="text-2xl font-bold tracking-tight text-white">
                    Xin chào, {userName}!
                </h1>
            </div>
            <div className="flex items-center gap-3">
                <button className="flex items-center gap-2 rounded-xl border border-white/20 bg-white/10 px-4 py-2 text-sm font-medium text-white backdrop-blur-xl transition hover:bg-white/30">
                    <Download className="h-4 w-4" />
                    Tải tài liệu
                </button>
                <button className="flex items-center gap-2 rounded-xl border border-white/25 bg-white/15 px-4 py-2 text-sm font-medium text-white shadow-sm backdrop-blur-xl transition hover:bg-white/30">
                    <Plus className="h-4 w-4" />
                    Thêm tài liệu
                </button>
            </div>
        </div>
    );
}