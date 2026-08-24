import { useState } from "react";
import { List, Network } from "lucide-react";
import { cn } from "@/lib/utils";
import DepartmentList from "./DepartmentList";
import OrgChart from "./OrgChart"; // Chuyển OrgChart.tsx vào cùng thư mục department/

const DEPARTMENTS_DATA = [
    { id: "dept-1", name: "Phòng Nhân sự", manager: "Nguyễn Minh Anh", count: 12, budget: "150M" },
    { id: "dept-2", name: "Phòng Công nghệ", manager: "Trần Quốc Bảo", count: 45, budget: "600M" },
    { id: "dept-3", name: "Phòng Marketing", manager: "Lê Thu Hà", count: 18, budget: "250M" },
    { id: "dept-4", name: "Phòng Kinh doanh", manager: "Phạm Hoàng Nam", count: 32, budget: "400M" },
    { id: "dept-5", name: "Phòng Tài chính", manager: "Võ Ngọc Linh", count: 8, budget: "120M" },
];

export default function DepartmentsView() {
    const [departmentSubTab, setDepartmentSubTab] = useState<"list" | "tree">("list");

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                        Quản lý phòng ban
                    </h1>
                    <p className="text-sm text-slate-500">
                        Xem và điều chỉnh sơ đồ cơ cấu tổ chức doanh nghiệp
                    </p>
                </div>
                <div className="flex rounded-xl bg-slate-200/70 p-1">
                    <button
                        onClick={() => setDepartmentSubTab("list")}
                        className={cn(
                            "flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-bold transition",
                            departmentSubTab === "list"
                                ? "bg-white text-[#4338ca] shadow-sm"
                                : "text-slate-600 hover:text-slate-900"
                        )}
                    >
                        <List className="h-4 w-4" />
                        Danh sách phòng ban
                    </button>
                    <button
                        onClick={() => setDepartmentSubTab("tree")}
                        className={cn(
                            "flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-bold transition",
                            departmentSubTab === "tree"
                                ? "bg-white text-[#4338ca] shadow-sm"
                                : "text-slate-600 hover:text-slate-900"
                        )}
                    >
                        <Network className="h-4 w-4" />
                        Sơ đồ tổ chức
                    </button>
                </div>
            </div>

            {departmentSubTab === "list" ? (
                <DepartmentList departments={DEPARTMENTS_DATA} />
            ) : (
                <div className="h-[750px] overflow-hidden rounded-2xl border border-slate-100 bg-white p-2 shadow-sm">
                    <OrgChart />
                </div>
            )}
        </div>
    );
}