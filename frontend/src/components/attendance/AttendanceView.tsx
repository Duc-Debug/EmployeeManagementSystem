"use client";

import { useState } from "react";
import { BriefcaseBusiness, CheckCircle2 } from "lucide-react";
import type { AttendanceRecord } from "@/lib/hr-data";
import { useAuthUser } from "@/lib/auth-session";
import WorkLogView from "../timesheet/WorkLogView";
import { TimesheetApprovalView } from "../timesheet/TimesheetApprovalView";

export function AttendanceView(_props: {
    records: AttendanceRecord[];
    onClockIn: () => string;
    onClockOut: () => boolean;
    onEditRecord: (id: string) => void;
}) {
    const user = useAuthUser();
    const role = user?.roleCode?.toUpperCase().replace(/_/g, "-");
    const canAccessWorkLog = user?.permissions
        ? user.permissions.includes("WORK_LOG_READ")
        : role === "VT-04" || role === "VT-06";
    const canApproveWorkLog = user?.permissions
        ? user.permissions.includes("WORK_LOG_APPROVE")
        : role === "VT-02";
    const [selectedTab, setSelectedTab] = useState<"work-logs" | "approvals">(
        role === "VT-02" || !canAccessWorkLog ? "approvals" : "work-logs",
    );
    const subTab = selectedTab === "approvals" && canApproveWorkLog
        ? "approvals" : canAccessWorkLog ? "work-logs" : canApproveWorkLog ? "approvals" : null;

    return (
        <section className="space-y-4">
            <div className="flex flex-wrap border-b border-slate-200">
                {canAccessWorkLog && (
                    <button type="button" onClick={() => setSelectedTab("work-logs")}
                        className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-bold ${subTab === "work-logs" ? "border-indigo-600 text-indigo-600" : "border-transparent text-slate-500"}`}>
                        <BriefcaseBusiness className="h-4 w-4" />Ghi giờ công dự án
                    </button>
                )}
                {canApproveWorkLog && (
                    <button type="button" onClick={() => setSelectedTab("approvals")}
                        className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-bold ${subTab === "approvals" ? "border-emerald-600 text-emerald-600" : "border-transparent text-slate-500"}`}>
                        <CheckCircle2 className="h-4 w-4" />Duyệt giờ công
                    </button>
                )}
            </div>
            {subTab === "work-logs" && <WorkLogView />}
            {subTab === "approvals" && <TimesheetApprovalView />}
            {!subTab && <p className="text-sm text-slate-500">Bạn không có quyền truy cập giờ công.</p>}
        </section>
    );
}

export default AttendanceView;
