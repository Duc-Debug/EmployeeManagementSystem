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
    // Ghi giờ công: CHỈ dành cho VT-04 (Nhân viên chuyên môn)
    const canAccessWorkLog = role === "VT-04";
    // Duyệt giờ công: dành cho VT-02 (PM) hoặc Admin có quyền WORK_LOG_APPROVE
    const canApproveWorkLog = user?.permissions
        ? user.permissions.includes("WORK_LOG_APPROVE")
        : role === "VT-02" || role === "VT-06";

    const [selectedTab, setSelectedTab] = useState<"work-logs" | "approvals">(
        canAccessWorkLog ? "work-logs" : "approvals",
    );
    const subTab = selectedTab === "work-logs" && canAccessWorkLog
        ? "work-logs"
        : selectedTab === "approvals" && canApproveWorkLog
        ? "approvals"
        : canAccessWorkLog
        ? "work-logs"
        : canApproveWorkLog
        ? "approvals"
        : null;

    return (
        <section className="space-y-4">
            {canAccessWorkLog && canApproveWorkLog && (
                <div className="flex flex-wrap border-b border-slate-200">
                    <button type="button" onClick={() => setSelectedTab("work-logs")}
                        className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-bold ${subTab === "work-logs" ? "border-indigo-600 text-indigo-600" : "border-transparent text-slate-500"}`}>
                        <BriefcaseBusiness className="h-4 w-4" />Ghi giờ công dự án
                    </button>
                    <button type="button" onClick={() => setSelectedTab("approvals")}
                        className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-bold ${subTab === "approvals" ? "border-emerald-600 text-emerald-600" : "border-transparent text-slate-500"}`}>
                        <CheckCircle2 className="h-4 w-4" />Duyệt giờ công
                    </button>
                </div>
            )}
            {subTab === "work-logs" && <WorkLogView />}
            {subTab === "approvals" && <TimesheetApprovalView />}
            {!subTab && <p className="text-sm text-slate-500">Bạn không có quyền truy cập giờ công.</p>}
        </section>
    );
}

export default AttendanceView;
