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
                <p className="text-xs font-medium text-slate-500">
                    {formattedDateCapitalized}
                </p>
                <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                    Xin chào, {userName}!
                </h1>
            </div>
            <div className="flex items-center gap-3">
                <button className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-xs transition hover:bg-slate-50 hover:text-slate-900">
                    <Download className="h-4 w-4" />
                    Tải tài liệu
                </button>
                <button className="flex items-center gap-2 rounded-xl border border-indigo-600 bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-xs transition hover:bg-indigo-700">
                    <Plus className="h-4 w-4" />
                    Thêm tài liệu
                </button>
            </div>
        </div>
    );
}