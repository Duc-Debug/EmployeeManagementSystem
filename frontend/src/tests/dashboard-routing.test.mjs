import { test, describe } from "node:test";
import assert from "node:assert/strict";

import { resolveActiveTab } from "../components/dashboard/dashboard-routing.ts";

describe("Dashboard Route Resolution Tests (Fix regression roles vs access)", () => {

    test("/roles và /project-role phải map đúng vào tab 'roles', không bị nuốt bởi 'access'", () => {
        assert.equal(resolveActiveTab("/roles"), "roles");
        assert.equal(resolveActiveTab("/project-role"), "roles");
        assert.equal(resolveActiveTab("/vai-tro"), "roles");
        assert.equal(resolveActiveTab("/settings/roles"), "roles");
    });

    test("/access và /phan-quyen phải map đúng vào tab 'access'", () => {
        assert.equal(resolveActiveTab("/access"), "access");
        assert.equal(resolveActiveTab("/phan-quyen"), "access");
        assert.equal(resolveActiveTab("/settings/access"), "access");
    });

    test("/capacity và /nang-luc map đúng vào tab 'capacity'", () => {
        assert.equal(resolveActiveTab("/capacity"), "capacity");
        assert.equal(resolveActiveTab("/nang-luc"), "capacity");
    });

    test("Trang chủ hoặc URL không khớp map vào tab 'overview'", () => {
        assert.equal(resolveActiveTab("/"), "overview");
        assert.equal(resolveActiveTab("/unknown"), "overview");
    });
});
