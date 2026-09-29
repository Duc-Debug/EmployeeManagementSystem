import { test, describe } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

describe("P1-5: Mobile Responsive Layout & Behavior Tests (390px Viewport)", () => {
    const dashboardFile = path.resolve(__dirname, "../components/dashboard/Dashboard.tsx");
    const sidebarFile = path.resolve(__dirname, "../components/dashboard/SideBar.tsx");
    const headerFile = path.resolve(__dirname, "../components/dashboard/Header.tsx");
    const leaveViewFile = path.resolve(__dirname, "../components/leave/LeaveManagementView.tsx");
    const leaveCalendarFile = path.resolve(__dirname, "../components/leave/DepartmentLeaveCalendarView.tsx");
    const projectViewFile = path.resolve(__dirname, "../components/project/ProjectView.tsx");
    const myScheduleFile = path.resolve(__dirname, "../features/my-schedule/pages/MyWeeklySchedulePage.tsx");
    const myScheduleCardFile = path.resolve(__dirname, "../features/my-schedule/components/WeeklyScheduleCard.tsx");
    const executiveDashboardFile = path.resolve(__dirname, "../components/dashboard/ExecutiveDashboardOverview.tsx");

    test("TC-01: Dưới 1024px, sidebar ẩn mặc định (P1-5)", () => {
        const content = fs.readFileSync(dashboardFile, "utf-8");
        // Kiểm tra logic khởi tạo sidebar theo window.innerWidth >= 1024
        assert.ok(
            content.includes("window.innerWidth >= 1024"),
            "Dashboard.tsx phải khởi tạo sidebar dựa trên ngưỡng 1024px (ẩn khi < 1024px)"
        );
        // Không được hardcode useState(true) gây chiếm 240/390px trên mobile
        assert.ok(
            !content.includes("useState(true)"),
            "Dashboard.tsx không được đặt useState(true) cố định"
        );
    });

    test("TC-02: Dưới 1024px, sidebar mở dạng overlay và có backdrop che mờ", () => {
        const dashboardContent = fs.readFileSync(dashboardFile, "utf-8");
        const sidebarContent = fs.readFileSync(sidebarFile, "utf-8");

        // Kiểm tra backdrop overlay trong Dashboard.tsx
        assert.ok(
            dashboardContent.includes("bg-slate-900/50") && dashboardContent.includes("lg:hidden"),
            "Dashboard.tsx phải render backdrop overlay khi sidebar mở trên màn hình < 1024px"
        );

        // Kiểm tra SideBar.tsx sử dụng fixed inset-y-0 left-0 z-50 trên mobile và lg:static trên desktop
        assert.ok(
            sidebarContent.includes("fixed inset-y-0 left-0 z-50") && sidebarContent.includes("lg:static"),
            "SideBar.tsx phải là fixed overlay z-50 trên mobile và static trên desktop"
        );
    });

    test("TC-03: Sidebar tự đóng khi chọn menu hoặc chuyển route trên màn hình nhỏ", () => {
        const dashboardContent = fs.readFileSync(dashboardFile, "utf-8");
        const sidebarContent = fs.readFileSync(sidebarFile, "utf-8");

        // Tự đóng khi location.pathname thay đổi
        assert.ok(
            dashboardContent.includes("setIsSidebarOpen(false)") && dashboardContent.includes("location.pathname"),
            "Dashboard.tsx phải tự động đóng sidebar khi location.pathname thay đổi trên màn hình < 1024px"
        );

        // Tự đóng khi click vào NavLink trong SideBar
        assert.ok(
            sidebarContent.includes("onClose?.()") && sidebarContent.includes("window.innerWidth < 1024"),
            "SideBar.tsx phải gọi onClose() khi chọn NavLink trên màn hình < 1024px"
        );
    });

    test("TC-04: Header không bị tràn ngang ở 390px (ẩn các phần tử phụ trên mobile)", () => {
        const headerContent = fs.readFileSync(headerFile, "utf-8");

        // Đồng hồ hệ thống phải ẩn dưới lg
        assert.ok(
            headerContent.includes("hidden lg:flex") && headerContent.includes("currentTime"),
            "Header.tsx phải ẩn đồng hồ trên màn hình nhỏ để tránh tràn ngang ở 390px"
        );

        // Tên ứng dụng dài phải ẩn dưới sm
        assert.ok(
            headerContent.includes("hidden sm:inline-block"),
            "Header.tsx phải ẩn text dài trên mobile và chỉ hiển thị logo EM"
        );
    });

    test("TC-05: Grid thẻ số liệu 1 cột trên mobile cho các trang chính (Leave, Project, Executive)", () => {
        const leaveContent = fs.readFileSync(leaveViewFile, "utf-8");
        const leaveCalendarContent = fs.readFileSync(leaveCalendarFile, "utf-8");
        const projectContent = fs.readFileSync(projectViewFile, "utf-8");
        const executiveContent = fs.readFileSync(executiveDashboardFile, "utf-8");

        // LeaveManagementView: grid-cols-1 trên mobile, sm:grid-cols-2, lg:grid-cols-4
        assert.ok(
            leaveContent.includes("grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4"),
            "LeaveManagementView thẻ số liệu phải là 1 cột trên mobile (grid-cols-1)"
        );

        // DepartmentLeaveCalendarView: grid-cols-1 trên mobile
        assert.ok(
            leaveCalendarContent.includes("grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4"),
            "DepartmentLeaveCalendarView KPI thẻ phải là 1 cột trên mobile (grid-cols-1)"
        );

        // ProjectView KPI cards: grid-cols-1 trên mobile
        assert.ok(
            projectContent.includes("grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-4"),
            "ProjectView thẻ KPI phải là 1 cột trên mobile (grid-cols-1)"
        );

        // ExecutiveDashboardOverview KPI cards: grid-cols-1 trên mobile
        assert.ok(
            executiveContent.includes("grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4"),
            "ExecutiveDashboardOverview thẻ KPI phải là 1 cột trên mobile (grid-cols-1)"
        );
    });

    test("TC-06: Bảng rộng cuộn ngang trong khung riêng (overflow-x-auto với min-width an toàn)", () => {
        const leaveContent = fs.readFileSync(leaveViewFile, "utf-8");
        const leaveCalendarContent = fs.readFileSync(leaveCalendarFile, "utf-8");
        const executiveContent = fs.readFileSync(executiveDashboardFile, "utf-8");

        // Bảng đơn nghỉ phép có overflow-x-auto và min-w-[650px]
        assert.ok(
            leaveContent.includes("overflow-x-auto") && leaveContent.includes("min-w-[650px]"),
            "LeaveManagementView bảng đơn nghỉ phép phải cuộn ngang trong khung riêng và có min-width"
        );

        // Lưới lịch 7 cột tháng có overflow-x-auto và min-w-[650px]
        assert.ok(
            leaveCalendarContent.includes("overflow-x-auto") && leaveCalendarContent.includes("min-w-[650px]"),
            "DepartmentLeaveCalendarView lưới lịch 7 cột phải cuộn ngang trong khung riêng"
        );

        // Bảng danh mục dự án chiến lược có overflow-x-auto và min-w-[650px]
        assert.ok(
            executiveContent.includes("overflow-x-auto") && executiveContent.includes("min-w-[650px]"),
            "ExecutiveDashboardOverview bảng dự án phải cuộn ngang trong khung riêng"
        );
    });

    test("TC-07: Trang Lịch phân bổ tuần (/my-schedule) responsive trên 390px", () => {
        const scheduleContent = fs.readFileSync(myScheduleFile, "utf-8");
        const cardContent = fs.readFileSync(myScheduleCardFile, "utf-8");

        assert.ok(
            scheduleContent.includes("p-2 sm:p-4 md:p-6") || scheduleContent.includes("p-4 md:p-6"),
            "MyWeeklySchedulePage phải có padding co giãn theo màn hình"
        );

        assert.ok(
            cardContent.includes("flex-col sm:flex-row") || cardContent.includes("flex-col md:flex-row"),
            "WeeklyScheduleCard header phải hỗ trợ chuyển sang dạng cột khi màn hình hẹp"
        );
    });

    test("TC-08: Main content container hỗ trợ overflow-x-hidden để triệt tiêu hiện tượng tràn ngang toàn trang", () => {
        const dashboardContent = fs.readFileSync(dashboardFile, "utf-8");
        assert.ok(
            dashboardContent.includes("overflow-x-hidden"),
            "Dashboard.tsx main container phải có overflow-x-hidden để bảo đảm ở 390px không tràn ngang"
        );
    });
});
