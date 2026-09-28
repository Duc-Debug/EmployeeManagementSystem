import { test, describe } from "node:test";
import assert from "node:assert/strict";

function resolveActiveTab(pathname) {
    const path = pathname.toLowerCase();
    if (path.includes("capacity") || path.includes("nang-luc")) return "capacity";
    if (
        path.includes("roles") ||
        path.includes("vai-tro") ||
        path.includes("project-role")
    ) {
        return "roles";
    }
    if (path.includes("access") || path.includes("phan-quyen")) return "access";
    if (path.includes("working-calendar") || path.includes("lich-lam-viec") || path.includes("ngay-le") || path.includes("calendar-config")) return "working-calendar";
    if (path.includes("availability") || path.includes("kha-dung") || path.includes("gio-tuan")) return "availability";
    if (path.includes("hrprofile") || path.includes("ho-so") || path.includes("employee")) return "hrprofile";
    if (path.includes("user") || path.includes("tai-khoan")) return "users";
    if (path.includes("department") || path.includes("phong-ban") || path.includes("org-unit")) return "departments";
    if (path.includes("attendance") || path.includes("cham-cong")) return "attendance";
    if (path.includes("leave") || path.includes("nghi-phep")) return "leave";
    if (path.includes("skills") || path.includes("ky-nang")) return "skills";
    if (path.includes("project") || path.includes("du-an")) return "project";
    if (path.includes("report") || path.includes("bao-cao")) return "reports";
    if (path.includes("setting")) return "settings";
    return "overview";
}

describe("Dashboard Route Resolution Tests (Fix regression roles vs access)", () => {

    test("TC-01: /roles và /project-role phải map đúng vào tab 'roles', không bị nuốt bởi 'access'", () => {
        assert.equal(resolveActiveTab("/roles"), "roles");
        assert.equal(resolveActiveTab("/project-role"), "roles");
        assert.equal(resolveActiveTab("/vai-tro"), "roles");
        assert.equal(resolveActiveTab("/settings/roles"), "roles");
    });

    test("TC-02: /access và /phan-quyen phải map đúng vào tab 'access'", () => {
        assert.equal(resolveActiveTab("/access"), "access");
        assert.equal(resolveActiveTab("/phan-quyen"), "access");
        assert.equal(resolveActiveTab("/settings/access"), "access");
    });

    test("TC-03: /capacity và /nang-luc map đúng vào tab 'capacity'", () => {
        assert.equal(resolveActiveTab("/capacity"), "capacity");
        assert.equal(resolveActiveTab("/nang-luc"), "capacity");
    });

    test("TC-04: Trang chủ hoặc URL không khớp map vào tab 'overview'", () => {
        assert.equal(resolveActiveTab("/"), "overview");
        assert.equal(resolveActiveTab("/unknown"), "overview");
    });
});
