import { Download, Plus } from "lucide-react";

export default function DashboardHeader() {
    return (
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
            <div>
                <p className="text-xs font-medium text-slate-500">
                    Thứ Hai, 24 tháng 8, 2026
                </p>
                <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                    Xin chào!
                </h1>
                <p className="text-sm text-slate-500">
                    Đây là tình hình nhân sự hôm nay.
                </p>
            </div>
            <div className="flex items-center gap-3">
                <button className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50">
                    <Download className="h-4 w-4" />
                    Tải tài liệu
                </button>
                <button className="flex items-center gap-2 rounded-xl bg-[#4338ca] px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-indigo-700">
                    <Plus className="h-4 w-4" />
                    Thêm tài liệu
                </button>
            </div>
        </div>
    );
}