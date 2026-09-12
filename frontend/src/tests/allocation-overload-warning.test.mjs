import { test, describe } from "node:test";
import assert from "node:assert/strict";
import {
    canBypassResourceOverload,
    hasUserPermission,
    RESOURCE_OVERLOAD_BYPASS_PERMISSION,
    computeAllocationOverload,
    isAdjustHoursSubmitDisabled,
    validateOverloadSubmission
} from "../lib/allocation-overload.ts";

describe("Allocation Overload Warning & Bypass Frontend Logic Tests (NCL-06-CN-003)", () => {

    test("QTN-11: Cảnh báo quá tải kích hoạt khi giờ phân bổ vượt quá năng lực khả dụng", () => {
        const capacity = 40;
        const normalHours = 35;
        const overloadedHours = 45;

        const resNormal = computeAllocationOverload(normalHours, 0, capacity);
        const resExcess = computeAllocationOverload(overloadedHours, 0, capacity);

        assert.equal(resNormal.isOverloaded, false, "35h <= 40h không bị quá tải");
        assert.equal(resExcess.isOverloaded, true, "45h > 40h phải bị cảnh báo quá tải");
        assert.equal(resExcess.overloadHours, 5, "Vượt quá 5 giờ");
    });

    test("QTN-11 / Quyền hạn: Xác thực permission-based qua RESOURCE_ALLOCATION_OVERLOAD_BYPASS từ backend session", () => {
        // Source of truth là user.permissions từ backend
        assert.equal(canBypassResourceOverload({ roleCode: "VT-03", permissions: [RESOURCE_OVERLOAD_BYPASS_PERMISSION] }), true, "User có permission RESOURCE_ALLOCATION_OVERLOAD_BYPASS -> Được phép");
        assert.equal(canBypassResourceOverload({ roleCode: "VT-02", permissions: [RESOURCE_OVERLOAD_BYPASS_PERMISSION] }), true, "Bất kể roleCode nào, nếu backend cấp permission -> Được phép");
        assert.equal(canBypassResourceOverload({ roleCode: "VT-03", permissions: ["OTHER_PERMISSION"] }), false, "User có permission khác không chứa bypass -> Không có quyền");
        assert.equal(canBypassResourceOverload({ roleCode: "VT-03", permissions: [] }), false, "User có mảng rỗng -> Không có quyền");
        assert.equal(canBypassResourceOverload({ roleCode: "VT-03" }), false, "Không có hardcoded role fallback, thiếu permissions -> Không có quyền");
        assert.equal(canBypassResourceOverload({ roleCode: "VT-02" }), false, "PM không có permissions -> Không có quyền");
        assert.equal(canBypassResourceOverload(null), false, "Null user -> Không có quyền");
        assert.equal(hasUserPermission({ permissions: [RESOURCE_OVERLOAD_BYPASS_PERMISSION] }, RESOURCE_OVERLOAD_BYPASS_PERMISSION), true);
    });

    test("QTN-11 / Validation: validateOverloadSubmission bắt buộc phải có lý do khi xác nhận vượt tải", () => {
        const resultEmpty = validateOverloadSubmission(true, true, "");
        assert.equal(resultEmpty.valid, false);
        assert.equal(resultEmpty.error, "Vui lòng nhập lý do chấp nhận quá tải (QTN-11).");

        const resultWhitespace = validateOverloadSubmission(true, true, "   ");
        assert.equal(resultWhitespace.valid, false);

        const resultNonRM = validateOverloadSubmission(true, false, "Khẩn cấp");
        assert.equal(resultNonRM.valid, false);
        assert.equal(resultNonRM.error, "Chỉ Quản lý nguồn lực (RM) mới có quyền phê duyệt phân bổ vượt năng lực.");

        const resultValid = validateOverloadSubmission(true, true, "Dự án ưu tiên cao cần tăng ca giải quyết deadline");
        assert.equal(resultValid.valid, true);
    });

    test("API Payload: Gửi overloadReason khi vượt tải và bỏ qua khi không vượt tải", () => {
        const buildPayload = (employeeId, projectId, year, week, hours, capacity, reason) => {
            const isOverloaded = hours > capacity;
            return {
                employeeId,
                projectId,
                year,
                weekNumber: week,
                allocatedHours: hours,
                ...(isOverloaded && reason ? { overloadReason: reason.trim() } : {})
            };
        };

        const normalPayload = buildPayload(1, 10, 2026, 37, 30, 40, "Không cần thiết");
        assert.equal(normalPayload.allocatedHours, 30);
        assert.equal(normalPayload.overloadReason, undefined);

        const overloadPayload = buildPayload(1, 10, 2026, 37, 45, 40, "Cần tăng ca gấp");
        assert.equal(overloadPayload.allocatedHours, 45);
        assert.equal(overloadPayload.overloadReason, "Cần tăng ca gấp");
    });

    test("QTN-11 / Truth Source: netAvailableHours (đã trừ ngày nghỉ/lễ) là nguồn sự thật cho overload", () => {
        // Tình huống: Chuẩn 40h, nghỉ phép/lễ 16h => netAvailableHours = 24h
        const standardHours = 40;
        const approvedLeaveHours = 16;
        const netAvailableHours = standardHours - approvedLeaveHours; // 24h

        // User nhập 32h: 32h <= 40h chuẩn, nhưng 32h > 24h khả dụng thực tế
        const result = computeAllocationOverload(32, 0, netAvailableHours);
        assert.equal(result.isOverloaded, true, "32h > 24h phải bị coi là quá tải");
        assert.equal(result.overloadHours, 8, "Vượt quá 8 giờ so với khả dụng thực tế");
    });

    test("QTN-11 / Cross-project Allocation: Tính tổng giờ phân bổ trên mọi dự án trong tuần", () => {
        // Nhân sự đã được phân bổ 10h ở Dự án khác, tuần này khả dụng 24h
        const otherProjectsAllocated = 10;
        const netCapacity = 24;

        // Phân bổ thêm 12h: Tổng 22h <= 24h -> Hợp lệ
        const validAlloc = computeAllocationOverload(12, otherProjectsAllocated, netCapacity);
        assert.equal(validAlloc.totalWeeklyHours, 22);
        assert.equal(validAlloc.isOverloaded, false);

        // Phân bổ thêm 20h: Tổng 30h > 24h -> Quá tải 6h
        const overloadAlloc = computeAllocationOverload(20, otherProjectsAllocated, netCapacity);
        assert.equal(overloadAlloc.totalWeeklyHours, 30);
        assert.equal(overloadAlloc.isOverloaded, true);
        assert.equal(overloadAlloc.overloadHours, 6);
    });

    test("QTN-11 / Error Recovery: Parse ALLOCATION_OVERLOAD_WARNING, derive state without stale lock, và tự động mở khóa khi giảm giờ", () => {
        const backendErrorResponse = {
            status: 400,
            data: {
                code: "ALLOCATION_OVERLOAD_WARNING",
                message: "Không thể phân bổ: Tổng số giờ phân bổ (30h) vượt quá số giờ khả dụng (24h)...",
                details: {
                    availableHours: 24,
                    allocatedHours: 30,
                    overloadHours: 6
                }
            }
        };

        // Khi backend trả lỗi 400 ALLOCATION_OVERLOAD_WARNING, cập nhật state năng lực
        const currentHours = 20;
        const details = backendErrorResponse.data.details;
        const netCapacity = details.availableHours; // 24
        const otherProjectsHours = Math.max(0, details.allocatedHours - currentHours); // 30 - 20 = 10

        // Kiểm tra trạng thái ngay sau khi có lỗi (hours = 20)
        let res = computeAllocationOverload(currentHours, otherProjectsHours, netCapacity);
        assert.equal(res.isOverloaded, true, "Ngay sau khi backend báo lỗi, UI phải là quá tải");
        assert.equal(res.overloadHours, 6, "Vượt quá 6h");

        // Khi user (non-RM hoặc RM) kéo slider xuống 10h (hợp lệ):
        const reducedHours = 10;
        res = computeAllocationOverload(reducedHours, otherProjectsHours, netCapacity);

        assert.equal(res.isOverloaded, false, "Sau khi giảm giờ xuống 10h (tổng 20h <= 24h), isOverloaded phải là false");
        assert.equal(res.overloadHours, 0, "Không còn số giờ vượt");

        // Non-RM không còn bị disabled nút bấm khi giờ đã hợp lệ
        const isNonRmBlocked = isAdjustHoursSubmitDisabled(false, false, res.isOverloaded, false);
        assert.equal(isNonRmBlocked, false, "Non-RM không bị block khi đã chỉnh giờ hợp lệ");
    });

    test("QTN-11 / UX Invariant: Nút submit bị vô hiệu hóa khi đang tải năng lực tuần (isLoadingCapacity)", () => {
        // Khi đang tải năng lực: nút submit bắt buộc bị disabled dù không quá tải
        assert.equal(isAdjustHoursSubmitDisabled(false, true, false, true), true, "Phải disable nút khi isLoadingCapacity = true");
        assert.equal(isAdjustHoursSubmitDisabled(false, true, false, false), true, "Phải disable nút khi isLoadingCapacity = true (non-RM)");

        // Sau khi tải xong: hợp lệ thì được phép bấm
        assert.equal(isAdjustHoursSubmitDisabled(false, false, false, false), false, "Mở khóa nút khi đã tải xong và không quá tải");

        // Khi quá tải: RM được bấm, non-RM bị khóa
        assert.equal(isAdjustHoursSubmitDisabled(false, false, true, true), false, "RM được bấm khi quá tải (để mở khóa phê duyệt)");
        assert.equal(isAdjustHoursSubmitDisabled(false, false, true, false), true, "Non-RM bị khóa nút khi quá tải");
    });

    test("HIGH Fix: Nút submit bị vô hiệu hóa khi API capacity lỗi (hasCapacityError = true) để ngăn dùng capacity giả", () => {
        // Khi API capacity bị lỗi hoặc netCapacity == null: submit BẮT BUỘC bị disabled cho cả RM lẫn non-RM
        assert.equal(isAdjustHoursSubmitDisabled(false, false, false, true, true), true, "Phải disable nút khi hasCapacityError = true");
        assert.equal(isAdjustHoursSubmitDisabled(false, false, false, false, true), true, "Phải disable nút khi hasCapacityError = true (non-RM)");
        assert.equal(isAdjustHoursSubmitDisabled(false, false, true, true, true), true, "Dù là RM cũng bị khóa khi chưa có capacity thực tế");
    });
});

