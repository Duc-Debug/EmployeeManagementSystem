import { useState } from "react";
import { GitBranch, Network } from "lucide-react";
import { cn } from "@/lib/utils";
import DepartmentTree from "./DepartmentTree";
import OrgChart from "./OrgChart";

export default function DepartmentsView() {
    const [departmentSubTab, setDepartmentSubTab] = useState<"list" | "tree">("list");

    return (
        <div className="flex flex-col h-full min-h-0 space-y-4 flex-1">
            <div className="shrink-0 flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 pb-4">
                <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                        Quản lý phòng ban
                    </h1>
                    <p className="text-sm text-slate-500">
                        Xem và điều chỉnh sơ đồ cơ cấu tổ chức doanh nghiệp
                    </p>
                </div>
                <div className="flex rounded-xl border border-slate-200 bg-slate-100 p-1 shadow-2xs">
                    <button
                        onClick={() => setDepartmentSubTab("list")}
                        className={cn(
                            "flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition",
                            departmentSubTab === "list"
                                ? "bg-white text-indigo-700 font-bold shadow-xs"
                                : "text-slate-600 hover:text-slate-900"
                        )}
                    >
                        <GitBranch className="h-4 w-4" />
                        Cây phân cấp đơn vị
                    </button>
                    <button
                        onClick={() => setDepartmentSubTab("tree")}
                        className={cn(
                            "flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition",
                            departmentSubTab === "tree"
                                ? "bg-white text-indigo-700 font-bold shadow-xs"
                                : "text-slate-600 hover:text-slate-900"
                        )}
                    >
                        <Network className="h-4 w-4" />
                        Sơ đồ tổ chức
                    </button>
                </div>
            </div>

            {departmentSubTab === "list" ? (
                <DepartmentTree />
            ) : (
                <div className="flex-1 min-h-0 w-full overflow-hidden rounded-2xl border border-slate-200/90 bg-white p-1 shadow-xs relative">
                    <OrgChart />
                </div>
            )}
        </div>
    );
}