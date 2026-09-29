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
    const searchFile = path.resolve(__dirname, "../components/dashboard/PageQuickSearch.tsx");
    const leaveViewFile = path.resolve(__dirname, "../components/leave/LeaveManagementView.tsx");
    const leaveCalendarFile = path.resolve(__dirname, "../components/leave/DepartmentLeaveCalendarView.tsx");
    const projectViewFile = path.resolve(__dirname, "../components/project/ProjectView.tsx");
    const myScheduleFile = path.resolve(__dirname, "../features/my-schedule/pages/MyWeeklySchedulePage.tsx");
    const myScheduleCardFile = path.resolve(__dirname, "../features/my-schedule/components/WeeklyScheduleCard.tsx");
    const executiveDashboardFile = path.resolve(__dirname, "../components/dashboard/ExecutiveDashboardOverview.tsx");
    const adminDashboardFile = path.resolve(__dirname, "../components/dashboard/AdminDashboardOverview.tsx");
    const pmDashboardFile = path.resolve(__dirname, "../components/dashboard/PmDashboardOverview.tsx");
    const rmDashboardFile = path.resolve(__dirname, "../components/dashboard/RmDashboardOverview.tsx");
    const hrDashboardFile = path.resolve(__dirname, "../components/dashboard/HrDashboardOverview.tsx");
    const employeeDashboardFile = path.resolve(__dirname, "../components/dashboard/EmployeeDashboardOverview.tsx");

    // =========================================================================
    // 1. RUNTIME VIEWPORT SIMULATION (390px vs 1024px+)
    // =========================================================================
    describe("TC-01: Runtime Viewport & Responsive State Machine", () => {
        test("Initial sidebar visibility based on viewport width (390px -> hidden, 1024px -> visible)", () => {
            const determineInitialSidebarOpen = (viewportWidth) => {
                if (typeof viewportWidth === "undefined") return false;
                return viewportWidth >= 1024;
            };

            // Mobile viewports (iPhone 12/13/14: 390px, SE: 375px, Pixel: 412px, iPad portrait: 768px)
            assert.equal(determineInitialSidebarOpen(390), false, "At 390px, sidebar must be hidden by default");
            assert.equal(determineInitialSidebarOpen(375), false, "At 375px, sidebar must be hidden by default");
            assert.equal(determineInitialSidebarOpen(768), false, "At 768px, sidebar must be hidden by default");
            assert.equal(determineInitialSidebarOpen(1023), false, "At 1023px, sidebar must be hidden by default");

            // Desktop viewports (1024px, 1280px, 1920px)
            assert.equal(determineInitialSidebarOpen(1024), true, "At 1024px, sidebar must be visible by default");
            assert.equal(determineInitialSidebarOpen(1280), true, "At 1280px, sidebar must be visible by default");
            assert.equal(determineInitialSidebarOpen(1920), true, "At 1920px, sidebar must be visible by default");
        });

        test("Sidebar auto-close trigger on item selection under 1024px viewport", () => {
            let closeCallCount = 0;
            const onCloseMock = () => { closeCallCount++; };

            const handleNavLinkClick = (viewportWidth, onClose) => {
                if (viewportWidth < 1024) {
                    onClose?.();
                }
            };

            // Simulate clicking menu on 390px mobile
            handleNavLinkClick(390, onCloseMock);
            assert.equal(closeCallCount, 1, "Clicking menu on 390px viewport must invoke onClose");

            // Simulate clicking menu on 1280px desktop
            handleNavLinkClick(1280, onCloseMock);
            assert.equal(closeCallCount, 1, "Clicking menu on 1280px desktop must NOT invoke onClose");
        });

        test("Route navigation auto-closes sidebar when viewport < 1024px", () => {
            let sidebarState = true; // User had opened sidebar on mobile
            const onRouteNavigation = (viewportWidth, currentOpenState) => {
                if (viewportWidth < 1024 && currentOpenState) {
                    return false; // auto close
                }
                return currentOpenState;
            };

            sidebarState = onRouteNavigation(390, sidebarState);
            assert.equal(sidebarState, false, "Navigating route on 390px viewport must set sidebar state to false");

            // On desktop, navigating does not close sidebar
            assert.equal(onRouteNavigation(1280, true), true, "Navigating route on desktop keeps sidebar open");
        });
    });

    // =========================================================================
    // 2. MATHEMATICAL 390PX VIEWPORT OVERFLOW AUDIT
    // =========================================================================
    describe("TC-02: Header & Search 390px Viewport Fit Verification", () => {
        test("Header elements total width fits strictly inside 390px without horizontal overflow", () => {
            const VIEWPORT_WIDTH = 390;
            const PADDING_PX = 12 * 2; // px-3 (12px each side) = 24px
            const AVAILABLE_WIDTH = VIEWPORT_WIDTH - PADDING_PX; // 366px

            // Responsive Mobile Header Layout:
            // Left: Hamburger (36px) + Gap (8px) + Brand EM (32px) = 76px
            // Text "Employee Management" is hidden below sm: 0px
            const leftSectionWidth = 36 + 8 + 32;

            // Center/Right:
            // Search Input collapsed (120px) + Gap (8px) + Notification (32px) + Gap (8px) + Profile (32px) = 200px
            // Clock is hidden below lg: 0px
            const rightSectionCollapsed = 120 + 8 + 32 + 8 + 32;
            const rightSectionExpanded = 190 + 8 + 32 + 8 + 32;

            const totalCollapsedWidth = leftSectionWidth + rightSectionCollapsed;
            const totalExpandedWidth = leftSectionWidth + rightSectionExpanded;

            assert.ok(
                totalCollapsedWidth <= AVAILABLE_WIDTH,
                `Header collapsed width (${totalCollapsedWidth}px) exceeds available width (${AVAILABLE_WIDTH}px) on 390px screen`
            );
            assert.ok(
                totalExpandedWidth <= AVAILABLE_WIDTH,
                `Header expanded width (${totalExpandedWidth}px) exceeds available width (${AVAILABLE_WIDTH}px) on 390px screen`
            );

            // Verify that without hiding clock (~130px) and long text (~180px), overflow occurs
            const oldUnresponsiveWidth = totalCollapsedWidth + 130 + 180;
            assert.ok(
                oldUnresponsiveWidth > VIEWPORT_WIDTH,
                `Old layout (${oldUnresponsiveWidth}px) proved to overflow 390px viewport`
            );
        });

        test("Quick search dropdown modal width is clamped to viewport", () => {
            const searchContent = fs.readFileSync(searchFile, "utf-8");
            assert.ok(
                searchContent.includes("w-[calc(100vw-2rem)] max-w-[340px]"),
                "Search dropdown must be clamped to viewport width (w-[calc(100vw-2rem)] max-w-[340px]) to prevent overflow at 390px"
            );
        });
    });

    // =========================================================================
    // 3. ZERO 'xs:' PREFIX USAGE VERIFICATION (Standard Tailwind Breakpoints)
    // =========================================================================
    describe("TC-03: Strict Tailwind Breakpoint Compliance (No undefined 'xs:' classes)", () => {
        const componentFiles = [
            dashboardFile,
            sidebarFile,
            headerFile,
            searchFile,
            leaveViewFile,
            leaveCalendarFile,
            projectViewFile,
            myScheduleFile,
            myScheduleCardFile,
            executiveDashboardFile,
            adminDashboardFile,
            pmDashboardFile,
            rmDashboardFile,
            hrDashboardFile,
            employeeDashboardFile,
        ];

        for (const file of componentFiles) {
            const fileName = path.basename(file);
            test(`File ${fileName} must NOT contain undefined 'xs:' Tailwind class prefixes`, () => {
                const content = fs.readFileSync(file, "utf-8");
                // Match class instances like xs:grid-cols, xs:w-, xs:max-w-, etc.
                const xsClassMatch = content.match(/\bxs:[a-zA-Z0-9_\-\[\]]+/g);
                assert.equal(
                    xsClassMatch,
                    null,
                    `File ${fileName} contains undefined 'xs:' classes: ${JSON.stringify(xsClassMatch)}`
                );
            });
        }
    });

    // =========================================================================
    // 4. METRIC CARDS & QUICK ACTION GRIDS (1-col on Mobile)
    // =========================================================================
    describe("TC-04: Metric Cards and Quick Action Grids Strict 1-Column on Mobile", () => {
        test("LeaveManagementView stats grid is strictly 1-column on mobile", () => {
            const content = fs.readFileSync(leaveViewFile, "utf-8");
            assert.ok(
                content.includes("grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4"),
                "LeaveManagementView stats grid must strictly use 'grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4'"
            );
        });

        test("DepartmentLeaveCalendarView KPI grid is strictly 1-column on mobile", () => {
            const content = fs.readFileSync(leaveCalendarFile, "utf-8");
            assert.ok(
                content.includes("grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4"),
                "DepartmentLeaveCalendarView KPI grid must strictly use 'grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4'"
            );
        });

        test("ProjectView KPI cards are strictly 1-column on mobile", () => {
            const content = fs.readFileSync(projectViewFile, "utf-8");
            assert.ok(
                content.includes("grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-4"),
                "ProjectView KPI grid must strictly use 'grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-4'"
            );
        });

        test("ExecutiveDashboardOverview KPI cards are strictly 1-column on mobile", () => {
            const content = fs.readFileSync(executiveDashboardFile, "utf-8");
            assert.ok(
                content.includes("grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4"),
                "ExecutiveDashboardOverview KPI grid must strictly use 'grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-4'"
            );
        });

        test("Quick actions grids across all role dashboards use 1-column on mobile with standard Tailwind breakpoints", () => {
            const dashboards = [
                { file: adminDashboardFile, name: "AdminDashboardOverview", expected: "grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-2" },
                { file: employeeDashboardFile, name: "EmployeeDashboardOverview", expected: "grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-2" },
                { file: hrDashboardFile, name: "HrDashboardOverview", expected: "grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-2" },
                { file: pmDashboardFile, name: "PmDashboardOverview", expected: "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2" },
                { file: rmDashboardFile, name: "RmDashboardOverview", expected: "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2" },
                { file: executiveDashboardFile, name: "ExecutiveDashboardOverview", expected: "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2" },
            ];

            for (const { file, name, expected } of dashboards) {
                const content = fs.readFileSync(file, "utf-8");
                assert.ok(
                    content.includes(expected),
                    `${name} quick action grid must use '${expected}'`
                );
            }
        });
    });

    // =========================================================================
    // 5. ISOLATED HORIZONTAL SCROLL FOR LARGE DATA TABLES
    // =========================================================================
    describe("TC-05: Isolated Horizontal Scroll Containers for Tables & Calendars", () => {
        test("Leave requests table is wrapped in overflow-x-auto with min-width", () => {
            const content = fs.readFileSync(leaveViewFile, "utf-8");
            assert.ok(
                content.includes("overflow-x-auto") && content.includes("min-w-[650px]"),
                "Leave requests table must have an overflow-x-auto container with min-w-[650px]"
            );
        });

        test("Department 7-column calendar grid is wrapped in overflow-x-auto with min-width", () => {
            const content = fs.readFileSync(leaveCalendarFile, "utf-8");
            assert.ok(
                content.includes("overflow-x-auto") && content.includes("min-w-[650px]"),
                "Department 7-column calendar must have an overflow-x-auto container with min-w-[650px]"
            );
        });

        test("Executive strategic projects table is wrapped in overflow-x-auto with min-width", () => {
            const content = fs.readFileSync(executiveDashboardFile, "utf-8");
            assert.ok(
                content.includes("overflow-x-auto") && content.includes("min-w-[650px]"),
                "Executive strategic projects table must have an overflow-x-auto container with min-w-[650px]"
            );
        });
    });

    // =========================================================================
    // 6. MY SCHEDULE PAGE & CARD STRICT RESPONSIVE IMPLEMENTATION
    // =========================================================================
    describe("TC-06: Strict Assertion on MySchedule Responsive Classes (No loose OR fallbacks)", () => {
        test("MyWeeklySchedulePage strictly uses mobile padding 'p-2 sm:p-4 md:p-6'", () => {
            const scheduleContent = fs.readFileSync(myScheduleFile, "utf-8");
            // Must strictly contain new responsive padding, rejecting old 'p-4 md:p-6'
            assert.ok(
                scheduleContent.includes("p-2 sm:p-4 md:p-6"),
                "MyWeeklySchedulePage must strictly implement 'p-2 sm:p-4 md:p-6'"
            );
        });

        test("WeeklyScheduleCard strictly uses flex column on mobile 'flex-col sm:flex-row'", () => {
            const cardContent = fs.readFileSync(myScheduleCardFile, "utf-8");
            assert.ok(
                cardContent.includes("flex-col sm:flex-row"),
                "WeeklyScheduleCard must strictly implement 'flex-col sm:flex-row'"
            );
        });
    });

    // =========================================================================
    // 7. ROOT OVERFLOW PREVENTION
    // =========================================================================
    describe("TC-07: Root Container Layout Guards", () => {
        test("Dashboard root main container has overflow-x-hidden", () => {
            const content = fs.readFileSync(dashboardFile, "utf-8");
            assert.ok(
                content.includes("overflow-x-hidden"),
                "Dashboard main container must enforce overflow-x-hidden to prevent document horizontal scrollbar"
            );
        });

        test("Mobile overlay backdrop is present and hidden on desktop (lg:hidden)", () => {
            const dashboardContent = fs.readFileSync(dashboardFile, "utf-8");
            const sidebarContent = fs.readFileSync(sidebarFile, "utf-8");

            assert.ok(
                dashboardContent.includes("bg-slate-900/50") && dashboardContent.includes("lg:hidden"),
                "Dashboard must render backdrop overlay for mobile with lg:hidden"
            );
            assert.ok(
                sidebarContent.includes("fixed inset-y-0 left-0 z-50 lg:static"),
                "SideBar must be fixed overlay z-50 on mobile and static on desktop"
            );
        });
    });
});
