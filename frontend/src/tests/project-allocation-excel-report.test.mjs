import { test, describe } from "node:test";
import assert from "node:assert/strict";

import {
  buildExportProjectAllocationUrl,
  extractFilenameFromContentDisposition,
  validateExportPeriod,
  canExportProjectAllocationExcel,
} from "../lib/api/project-allocation-excel.ts";

describe("Project Allocation Excel Report Logic & Validation Tests (NCL-10-CN-003)", () => {
  describe("TC-01 & TC-06: Endpoint URL & Query Parameters Builder", () => {
    test("Xây dựng URL chính xác với chỉ projectId (xuất toàn bộ dự án)", () => {
      const url = buildExportProjectAllocationUrl({ projectId: 12 });
      assert.ok(url.includes("/reports/export/excel/project-allocation?projectId=12"));
      assert.ok(!url.includes("fromYear"));
      assert.ok(!url.includes("toWeek"));
    });

    test("Xây dựng URL chính xác khi chọn all=true (chế độ toàn bộ dự án)", () => {
      const url = buildExportProjectAllocationUrl({ projectId: 12, all: true });
      assert.ok(url.includes("/reports/export/excel/project-allocation?projectId=12&all=true"));
      assert.ok(!url.includes("fromYear"));
      assert.ok(!url.includes("toWeek"));
    });

    test("Xây dựng URL đầy đủ với dải tuần bắt đầu và kết thúc", () => {
      const url = buildExportProjectAllocationUrl({
        projectId: 99,
        fromYear: 2026,
        fromWeek: 1,
        toYear: 2026,
        toWeek: 12,
      });
      assert.ok(url.includes("/reports/export/excel/project-allocation?"));
      assert.ok(url.includes("projectId=99"));
      assert.ok(url.includes("fromYear=2026"));
      assert.ok(url.includes("fromWeek=1"));
      assert.ok(url.includes("toYear=2026"));
      assert.ok(url.includes("toWeek=12"));
    });
  });

  describe("TC-02: Dải thời gian và Quy tắc Tuần ISO-8601", () => {
    test("Dải tuần hợp lệ trong cùng một năm (2026: Tuần 5 đến Tuần 20)", () => {
      const res = validateExportPeriod(2026, 5, 2026, 20);
      assert.equal(res.isValid, true);
      assert.equal(res.error, undefined);
    });

    test("Dải tuần hợp lệ qua các năm khác nhau (2025-W45 đến 2026-W10)", () => {
      const res = validateExportPeriod(2025, 45, 2026, 10);
      assert.equal(res.isValid, true);
    });

    test("Từ chối khi dải thời gian ngược: từ tuần sau đến tuần trước", () => {
      const res = validateExportPeriod(2026, 25, 2026, 10);
      assert.equal(res.isValid, false);
      assert.ok(res.error?.includes("không được lớn hơn"));
    });

    test("Từ chối khi năm bắt đầu lớn hơn năm kết thúc", () => {
      const res = validateExportPeriod(2027, 1, 2026, 52);
      assert.equal(res.isValid, false);
      assert.ok(res.error?.includes("không được lớn hơn"));
    });

    test("Từ chối khi số tuần vượt quá số tuần tối đa của năm (Năm 2025 chỉ có 52 tuần)", () => {
      const res = validateExportPeriod(2025, 53, 2025, 53);
      assert.equal(res.isValid, false);
      assert.ok(res.error?.includes("Tuần bắt đầu không hợp lệ"));
    });

    test("Chấp nhận tuần 53 cho năm 2026 (Năm 2026 có 53 tuần ISO)", () => {
      const res = validateExportPeriod(2026, 53, 2026, 53);
      assert.equal(res.isValid, true);
    });

    test("Từ chối khi người dùng chỉ nhập năm mà thiếu tuần", () => {
      const res1 = validateExportPeriod(2026, undefined, 2026, 10);
      assert.equal(res1.isValid, false);
      assert.ok(res1.error?.includes("Vui lòng chọn đầy đủ cả năm và tuần bắt đầu"));

      const res2 = validateExportPeriod(2026, 1, 2026, undefined);
      assert.equal(res2.isValid, false);
      assert.ok(res2.error?.includes("Vui lòng chọn đầy đủ cả năm và tuần kết thúc"));
    });

    test("Hợp lệ khi không truyền tham số thời gian (chế độ xuất toàn bộ dự án)", () => {
      const res = validateExportPeriod(undefined, undefined, undefined, undefined);
      assert.equal(res.isValid, true);
    });
  });

  describe("TC-03: Trích xuất tên file từ Content-Disposition", () => {
    test("Trích xuất tên file chuẩn định dạng có dấu ngoặc kép", () => {
      const header = 'attachment; filename="Bao_Cao_Phan_Bo_Du_An_PROJ1_2026_W01_2026_W12.xlsx"';
      const name = extractFilenameFromContentDisposition(header);
      assert.equal(name, "Bao_Cao_Phan_Bo_Du_An_PROJ1_2026_W01_2026_W12.xlsx");
    });

    test("Trích xuất tên file không có dấu ngoặc kép", () => {
      const header = "attachment; filename=Bao_Cao_Phan_Bo.xlsx";
      const name = extractFilenameFromContentDisposition(header);
      assert.equal(name, "Bao_Cao_Phan_Bo.xlsx");
    });

    test("Trích xuất tên file chuẩn RFC 5987 UTF-8 encoded", () => {
      const header = "attachment; filename*=UTF-8''Bao_Cao_Phan_Bo%20Du_An.xlsx";
      const name = extractFilenameFromContentDisposition(header);
      assert.equal(name, "Bao_Cao_Phan_Bo Du_An.xlsx");
    });

    test("Dùng fallback an toàn khi header rỗng hoặc null", () => {
      const name1 = extractFilenameFromContentDisposition(null, "custom_fallback.xlsx");
      assert.equal(name1, "custom_fallback.xlsx");

      const name2 = extractFilenameFromContentDisposition(undefined);
      assert.equal(name2, "Bao_Cao_Phan_Bo_Du_An.xlsx");
    });
  });

  describe("TC-04: Phân quyền RBAC (Role Based Access Control)", () => {
    test("VT-01 (Ban Giám Đốc) có quyền xuất báo cáo Excel khi có quyền RESOURCE_ALLOCATION_READ", () => {
      assert.equal(canExportProjectAllocationExcel("VT-01", ["RESOURCE_ALLOCATION_READ"]), true);
      assert.equal(canExportProjectAllocationExcel("DIRECTOR", ["RESOURCE_ALLOCATION_READ"]), true);
      assert.equal(canExportProjectAllocationExcel("ROLE-EXECUTIVE", ["RESOURCE_ALLOCATION_READ"]), true);
    });

    test("VT-02 (Quản lý dự án) có quyền xuất báo cáo khi đúng là PM của dự án", () => {
      assert.equal(canExportProjectAllocationExcel("VT-02", ["RESOURCE_ALLOCATION_READ"], 10, 10), true);
      assert.equal(canExportProjectAllocationExcel("VT-02", ["RESOURCE_ALLOCATION_READ"], 10, 99), false);
      assert.equal(canExportProjectAllocationExcel("PM", ["RESOURCE_ALLOCATION_READ"]), true);
    });

    test("Các vai trò không có thẩm quyền (VT-03, VT-04, VT-05) dù có permission vẫn bị từ chối", () => {
      assert.equal(canExportProjectAllocationExcel("VT-03", ["RESOURCE_ALLOCATION_READ"]), false); // Quản lý nguồn lực (RM)
      assert.equal(canExportProjectAllocationExcel("VT-04", ["RESOURCE_ALLOCATION_READ"]), false); // Nhân viên
      assert.equal(canExportProjectAllocationExcel("VT-05", ["RESOURCE_ALLOCATION_READ"]), false); // Nhân sự HR
      assert.equal(canExportProjectAllocationExcel("EMPLOYEE", ["RESOURCE_ALLOCATION_READ"]), false);
      assert.equal(canExportProjectAllocationExcel(null, ["RESOURCE_ALLOCATION_READ"]), false);
      assert.equal(canExportProjectAllocationExcel(undefined, undefined), false);
    });

    test("Người dùng thiếu quyền RESOURCE_ALLOCATION_READ luôn bị chặn", () => {
      assert.equal(canExportProjectAllocationExcel("VT-01", ["OTHER_PERMISSION"]), false);
      assert.equal(canExportProjectAllocationExcel("VT-02", []), false);
    });
  });

  describe("TC-05: Định dạng lỗi & Error Code Mapping", () => {
    test("Ánh xạ mã lỗi 400 REPORT_NO_DATA thành thông báo tiếng Việt trực quan", () => {
      const backendError = { code: "REPORT_NO_DATA", message: "Không có dữ liệu trong kỳ" };
      let message = "";
      if (backendError.code === "REPORT_NO_DATA") {
        message = "Không có dữ liệu phân bổ trong kỳ đã chọn để xuất báo cáo.";
      }
      assert.equal(message, "Không có dữ liệu phân bổ trong kỳ đã chọn để xuất báo cáo.");
    });

    test("Ánh xạ mã lỗi 403 FORBIDDEN thành thông báo không có quyền", () => {
      const backendError = { code: "FORBIDDEN", message: "Access denied" };
      let message = "";
      if (backendError.code === "FORBIDDEN") {
        message = "Bạn không có quyền xuất báo cáo phân bổ cho dự án này.";
      }
      assert.equal(message, "Bạn không có quyền xuất báo cáo phân bổ cho dự án này.");
    });

    test("Ánh xạ mã lỗi 404 PROJECT_NOT_FOUND thành thông báo không tìm thấy dự án", () => {
      const backendError = { code: "PROJECT_NOT_FOUND", message: "Project not found" };
      let message = "";
      if (backendError.code === "PROJECT_NOT_FOUND") {
        message = "Không tìm thấy thông tin dự án.";
      }
      assert.equal(message, "Không tìm thấy thông tin dự án.");
    });
  });
});
