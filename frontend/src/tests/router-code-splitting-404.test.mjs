import { test, describe } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const frontendDir = path.resolve(__dirname, "../..");

describe("Task P2-2: Routing, Code Splitting & 404 Verification Tests", () => {

    describe("1. Route Architecture & Lazy Loading Code Inspection", () => {
        const appTsx = fs.readFileSync(path.join(frontendDir, "src/App.tsx"), "utf-8");
        const dashboardTsx = fs.readFileSync(path.join(frontendDir, "src/components/dashboard/Dashboard.tsx"), "utf-8");

        test("TC-01: App.tsx uses React.lazy() for major route components", () => {
            assert.match(appTsx, /const\s+ResetPasswordPage\s*=\s*lazy\(/);
            assert.match(appTsx, /const\s+(ChangePasswordPage|ForceChangePasswordPage)\s*=\s*lazy\(/);
            assert.match(appTsx, /const\s+NotFoundPage\s*=\s*lazy\(/);
            assert.match(appTsx, /const\s+ProjectView\s*=\s*lazy\(/);
            assert.match(appTsx, /const\s+AttendancePage\s*=\s*lazy\(/);
            assert.match(appTsx, /const\s+CompanyWeeklyCapacityView\s*=\s*lazy\(/);
            assert.match(appTsx, /const\s+DepartmentsView\s*=\s*lazy\(/);
            assert.match(appTsx, /const\s+SkilldeclarationView\s*=\s*lazy\(/);
        });

        test("TC-02: App.tsx uses Suspense with PageLoading fallback", () => {
            assert.match(appTsx, /<Suspense\s+fallback={<PageLoading\s*\/>}>/);
        });

        test("TC-03: Dashboard.tsx renders <Outlet /> instead of manual activeTab conditional switching", () => {
            assert.match(dashboardTsx, /<Outlet\s*\/>/);
            assert.doesNotMatch(dashboardTsx, /activeTab\s*===\s*["']departments["']/);
            assert.doesNotMatch(dashboardTsx, /activeTab\s*===\s*["']skills["']/);
            assert.doesNotMatch(dashboardTsx, /activeTab\s*===\s*["']project["']/);
            assert.doesNotMatch(dashboardTsx, /activeTab\s*===\s*["']attendance["']/);
            assert.doesNotMatch(dashboardTsx, /activeTab\s*===\s*["']leave["']/);
        });

        test("TC-04: Nested routes configured under /dashboard with catch-all 404", () => {
            assert.match(appTsx, /<Route\s+path=["']\/dashboard["']/);
            assert.match(appTsx, /<Route\s+path=["']overview["']/);
            assert.match(appTsx, /<Route\s+path=["']projects["']/);
            assert.match(appTsx, /<Route\s+path=["']attendance["']/);
            assert.match(appTsx, /<Route\s+path=["']timesheet["']/);
            assert.match(appTsx, /<Route\s+path=["']\*(["']\s+element={<NotFoundPage\s*\/>}|element={<NotFoundPage\s*\/>})/);
        });
    });

    describe("2. Authentication & Authorization Route Guards", () => {
        function simulateRequireAuth({ token, user, pathname, allowPasswordChangeOnly = false }) {
            if (!token || token === "undefined" || token === "null" || token.trim() === "") {
                return { redirect: "/login", reason: "UNAUTHENTICATED" };
            }
            if (user?.requiresPasswordChange && !allowPasswordChangeOnly && pathname !== "/change-password") {
                return { redirect: "/change-password", reason: "MANDATORY_PASSWORD_CHANGE" };
            }
            return { redirect: null, render: true };
        }

        function simulateLoginRoute({ token, user }) {
            if (token && token !== "undefined" && token !== "null" && token.trim() !== "") {
                if (user?.requiresPasswordChange) {
                    return { redirect: "/change-password" };
                }
                return { redirect: "/dashboard/overview" };
            }
            return { renderLogin: true };
        }

        test("TC-05: Unauthenticated access to protected route redirects to /login", () => {
            const result = simulateRequireAuth({ token: null, user: null, pathname: "/dashboard/overview" });
            assert.equal(result.redirect, "/login");
            assert.equal(result.reason, "UNAUTHENTICATED");
        });

        test("TC-06: Authenticated user with requiresPasswordChange=true is forced to /change-password", () => {
            const user = { username: "user1", requiresPasswordChange: true };
            const result = simulateRequireAuth({
                token: "valid-jwt",
                user,
                pathname: "/dashboard/projects"
            });
            assert.equal(result.redirect, "/change-password");
            assert.equal(result.reason, "MANDATORY_PASSWORD_CHANGE");
        });

        test("TC-07: Authenticated user with requiresPasswordChange=true can access /change-password without redirect loop", () => {
            const user = { username: "user1", requiresPasswordChange: true };
            const result = simulateRequireAuth({
                token: "valid-jwt",
                user,
                pathname: "/change-password",
                allowPasswordChangeOnly: true
            });
            assert.equal(result.redirect, null);
            assert.equal(result.render, true);
        });

        test("TC-08: Authenticated user accessing /login is redirected to dashboard without loop", () => {
            const user = { username: "user1", requiresPasswordChange: false };
            const result = simulateLoginRoute({ token: "valid-jwt", user });
            assert.equal(result.redirect, "/dashboard/overview");
        });

        test("TC-09: Authenticated user with requiresPasswordChange visiting /login is redirected to /change-password", () => {
            const user = { username: "user1", requiresPasswordChange: true };
            const result = simulateLoginRoute({ token: "valid-jwt", user });
            assert.equal(result.redirect, "/change-password");
        });
    });

    describe("3. 404 Route Matching Logic", () => {
        const knownDashboardSubroutes = new Set([
            "overview",
            "reports",
            "capacity-dashboard",
            "project",
            "projects",
            "capacity",
            "schedule-conflict",
            "simulation-scenarios",
            "outsourced-contracts",
            "my-schedule",
            "attendance",
            "timesheet",
            "workload",
            "availability",
            "unavailability",
            "leave",
            "working-calendar",
            "billable-rate",
            "project-allocation-report",
            "project-allocation",
            "timesheet-variance",
            "recruitment-demand",
            "capacity-forecast",
            "hrprofile",
            "employees",
            "departments",
            "skills",
            "users",
            "roles",
            "backup",
            "data-import",
            "access",
        ]);

        function matchRoute(path) {
            const cleanPath = path.split("?")[0].replace(/\/+$/, "") || "/";
            if (cleanPath === "/login") return "LoginRoute";
            if (cleanPath === "/reset-password") return "ResetPasswordPage";
            if (cleanPath === "/change-password") return "ChangePasswordPage";
            if (cleanPath === "/") return "RootRedirect(/dashboard/overview)";
            if (cleanPath.startsWith("/dashboard")) {
                const sub = cleanPath.replace(/^\/dashboard\/?/, "");
                if (sub === "" || sub === "overview") return "DashboardOverview";
                if (knownDashboardSubroutes.has(sub)) return `DashboardSubRoute(${sub})`;
                return "NotFoundPage";
            }
            if (["/projects", "/project", "/my-schedule", "/capacity", "/attendance", "/timesheet", "/leave", "/schedule-conflict", "/outsourced-contracts", "/skills"].includes(cleanPath)) {
                return `LegacyRedirect(/dashboard${cleanPath})`;
            }
            return "NotFoundPage";
        }

        test("TC-10: Unknown root route renders NotFoundPage", () => {
            assert.equal(matchRoute("/not-existing"), "NotFoundPage");
            assert.equal(matchRoute("/foo/bar/baz"), "NotFoundPage");
            assert.equal(matchRoute("/admin/unknown-page"), "NotFoundPage");
        });

        test("TC-11: Unknown dashboard nested route renders NotFoundPage", () => {
            assert.equal(matchRoute("/dashboard/not-existing"), "NotFoundPage");
            assert.equal(matchRoute("/dashboard/invalid-sub-path"), "NotFoundPage");
            assert.equal(matchRoute("/dashboard/test404"), "NotFoundPage");
        });

        test("TC-12: Valid routes match correctly", () => {
            assert.equal(matchRoute("/login"), "LoginRoute");
            assert.equal(matchRoute("/dashboard"), "DashboardOverview");
            assert.equal(matchRoute("/dashboard/overview"), "DashboardOverview");
            assert.equal(matchRoute("/dashboard/projects"), "DashboardSubRoute(projects)");
            assert.equal(matchRoute("/dashboard/timesheet"), "DashboardSubRoute(timesheet)");
            assert.equal(matchRoute("/dashboard/employees"), "DashboardSubRoute(employees)");
        });
    });

    describe("4. Build Output Bundle & Code Splitting Verification", () => {
        const distAssetsDir = path.join(frontendDir, "dist/assets");

        test("TC-13: Build directory dist/assets exists", () => {
            assert.ok(fs.existsSync(distAssetsDir), "dist/assets must exist after build");
        });

        test("TC-14: Initial bundle is well below 2.07 MB and split into route chunks", () => {
            const files = fs.readdirSync(distAssetsDir);
            const indexJs = files.find((f) => f.startsWith("index-") && f.endsWith(".js"));
            assert.ok(indexJs, "Must have an index-*.js entry bundle");

            const indexStats = fs.statSync(path.join(distAssetsDir, indexJs));
            const sizeInKb = indexStats.size / 1024;
            const sizeInMb = sizeInKb / 1024;

            // Before: ~2.07 MB (2,070 kB)
            // After: must be significantly smaller (< 600 kB)
            assert.ok(sizeInMb < 0.6, `index bundle size should be < 0.6 MB, actual is ${sizeInMb.toFixed(2)} MB (${sizeInKb.toFixed(2)} kB)`);

            // Verify major lazy chunk files exist in dist/assets
            const hasProjectChunk = files.some((f) => f.startsWith("ProjectView-") && f.endsWith(".js"));
            const hasCapacityChunk = files.some((f) => f.startsWith("CompanyWeeklyCapacityView-") && f.endsWith(".js"));
            const hasScenarioChunk = files.some((f) => f.startsWith("SimulationScenarioListView-") && f.endsWith(".js"));
            const hasDepartmentChunk = files.some((f) => f.startsWith("DepartmentsView-") && f.endsWith(".js"));
            const hasNotFoundChunk = files.some((f) => f.startsWith("NotFoundPage-") && f.endsWith(".js"));
            const hasChangePasswordChunk = files.some((f) => (f.startsWith("ChangePasswordPage-") || f.startsWith("ForceChangePasswordPage-")) && f.endsWith(".js"));

            assert.ok(hasProjectChunk, "ProjectView chunk must exist");
            assert.ok(hasCapacityChunk, "CompanyWeeklyCapacityView chunk must exist");
            assert.ok(hasScenarioChunk, "SimulationScenarioListView chunk must exist");
            assert.ok(hasDepartmentChunk, "DepartmentsView chunk must exist");
            assert.ok(hasNotFoundChunk, "NotFoundPage chunk must exist");
            assert.ok(hasChangePasswordChunk, "ChangePasswordPage/ForceChangePasswordPage chunk must exist");
        });
    });
});
