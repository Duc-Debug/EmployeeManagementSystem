/**
 * Pure route resolution helper mapping pathname to active dashboard tab.
 */
export function resolveActiveTab(pathname: string): string {
    const path = (pathname || "").toLowerCase();
    if (path.includes("billable-rate") || path.includes("ty-le-gio-tinh-phi") || path.includes("billable")) return "billable-rate";
    if (path.includes("workload") || path.includes("khoi-luong-cong-viec") || path.includes("muc-ban")) return "workload";
    if (path.includes("capacity-dashboard") || path.includes("bang-dieu-khien-nang-luc") || path.includes("dashboard-capacity")) return "capacity-dashboard";
    if (path.includes("capacity-forecast") || path.includes("du-bao-nang-luc") || path.includes("forecast")) return "capacity-forecast";
    if (path.includes("timesheet-variance") || path.includes("doi-chieu-gio-cong") || path.includes("variance")) return "timesheet-variance";
    if (path.includes("capacity") || path.includes("nang-luc")) return "capacity";
    if (
        path.includes("roles") ||
        path.includes("vai-tro") ||
        path.includes("project-role")
    ) {
        return "roles";
    }
    if (path.includes("my-schedule") || path.includes("my-allocations") || path.includes("lich-phan-bo")) return "my-schedule";
    if (path.includes("access") || path.includes("phan-quyen")) return "access";
    if (path.includes("working-calendar") || path.includes("lich-lam-viec") || path.includes("ngay-le") || path.includes("calendar-config")) return "working-calendar";
    if (path.includes("unavailability") || path.includes("khong-san-sang")) return "unavailability";
    if (path.includes("availability") || path.includes("kha-dung") || path.includes("gio-tuan")) return "availability";
    if (path.includes("hrprofile") || path.includes("ho-so") || path.includes("employee")) return "hrprofile";
    if (path.includes("user") || path.includes("tai-khoan")) return "users";
    if (path.includes("department") || path.includes("phong-ban") || path.includes("org-unit")) return "departments";
    if (path.includes("attendance") || path.includes("cham-cong") || path.includes("timesheet") || path.includes("work-log") || path.includes("gio-lam")) return "attendance";
    if (path.includes("leave") || path.includes("nghi-phep")) return "leave";
    if (path.includes("skills") || path.includes("ky-nang")) return "skills";
    if (path.includes("project-allocation") || path.includes("phan-bo-du-an") || path.includes("project-report")) return "project-allocation-report";
    if (path.includes("project") || path.includes("du-an")) return "project";
    if (path.includes("recruitment") || path.includes("tuyen-dung")) return "recruitment-demand";
    if (path.includes("simulation-scenario") || path.includes("mo-phong-kich-ban") || path.includes("scenarios")) return "simulation-scenarios";
    if (path.includes("schedule-conflict") || path.includes("xung-dot-lich") || path.includes("conflict")) return "schedule-conflict";
    if (path.includes("outsourced-contract") || path.includes("hop-dong-thue-ngoai")) return "outsourced-contracts";
    if (path.includes("data-import") || path.includes("nhap-du-lieu") || path.includes("employee-import") || path.includes("import")) return "data-import";
    if (path.includes("backup") || path.includes("sao-luu") || path.includes("phuc-hoi")) return "backup";
    if (path.includes("report") || path.includes("bao-cao")) return "reports";
    return "overview";
}
