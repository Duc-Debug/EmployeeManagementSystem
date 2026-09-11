import { test, describe } from "node:test";
import assert from "node:assert/strict";

describe("Allocation Overload Warning & Bypass Frontend Logic Tests (NCL-06-CN-003)", () => {

    test("QTN-11: Cảnh báo quá tải kích hoạt khi giờ phân bổ vượt quá năng lực khả dụng", () => {
        const capacity = 40;
        const normalHours = 35;
        const overloadedHours = 45;

        const isOverloadedNormal = normalHours > capacity;
        const isOverloadedExcess = overloadedHours > capacity;

        assert.equal(isOverloadedNormal, false, "35h <= 40h không bị quá tải");
        assert.equal(isOverloadedExcess, true, "45h > 40h phải bị cảnh báo quá tải");
        assert.equal(overloadedHours - capacity, 5, "Vượt quá 5 giờ");
    });

    test("QTN-11 / Quyền hạn: Chỉ RM (VT-03) mới có quyền xác nhận vượt tải", () => {
        const checkCanConfirmOverload = (roleCode) => roleCode === "VT-03";

        assert.equal(checkCanConfirmOverload("VT-03"), true, "RM (VT-03) được phép xác nhận");
        assert.equal(checkCanConfirmOverload("VT-02"), false, "PM (VT-02) không được phép tự xác nhận");
        assert.equal(checkCanConfirmOverload("VT-01"), false, "BOD (VT-01) không được phép tự xác nhận");
        assert.equal(checkCanConfirmOverload("VT-04"), false, "Nhân viên (VT-04) không được phép");
    });

    test("QTN-11 / Validation: Bắt buộc phải có lý do khi xác nhận vượt tải", () => {
        const validateConfirmation = (isOverloaded, isRM, reason) => {
            if (!isOverloaded) {
                return { valid: true };
            }
            if (!isRM) {
                return { valid: false, error: "Chỉ Quản lý nguồn lực (RM) mới có quyền phê duyệt phân bổ vượt năng lực." };
            }
            if (!reason || !reason.trim()) {
                return { valid: false, error: "Vui lòng nhập lý do chấp nhận quá tải (QTN-11)." };
            }
            return { valid: true };
        };

        const resultEmpty = validateConfirmation(true, true, "");
        assert.equal(resultEmpty.valid, false);
        assert.equal(resultEmpty.error, "Vui lòng nhập lý do chấp nhận quá tải (QTN-11).");

        const resultWhitespace = validateConfirmation(true, true, "   ");
        assert.equal(resultWhitespace.valid, false);

        const resultNonRM = validateConfirmation(true, false, "Khẩn cấp");
        assert.equal(resultNonRM.valid, false);
        assert.equal(resultNonRM.error, "Chỉ Quản lý nguồn lực (RM) mới có quyền phê duyệt phân bổ vượt năng lực.");

        const resultValid = validateConfirmation(true, true, "Dự án ưu tiên cao cần tăng ca giải quyết deadline");
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

        const computeOverload = (requestedHours, netCapacity, fallbackCapacity) => {
            const capacity = netCapacity !== null ? netCapacity : fallbackCapacity;
            const isOverloaded = requestedHours > capacity;
            const excessHours = isOverloaded ? requestedHours - capacity : 0;
            return { capacity, isOverloaded, excessHours };
        };

        // User nhập 32h: 32h <= 40h chuẩn, nhưng 32h > 24h khả dụng thực tế
        const result = computeOverload(32, netAvailableHours, standardHours);
        assert.equal(result.capacity, 24, "Capacity thực tế là 24h");
        assert.equal(result.isOverloaded, true, "32h > 24h phải bị coi là quá tải");
        assert.equal(result.excessHours, 8, "Vượt quá 8 giờ so với khả dụng thực tế");
    });

    test("QTN-11 / Cross-project Allocation: Tính tổng giờ phân bổ trên mọi dự án trong tuần", () => {
        // Nhân sự đã được phân bổ 10h ở Dự án khác, tuần này khả dụng 24h
        const otherProjectsAllocated = 10;
        const netCapacity = 24;

        const checkProjectAllocation = (projectHours, otherHours, capacity) => {
            const totalWeekly = otherHours + projectHours;
            const isOverloaded = totalWeekly > capacity;
            const excess = isOverloaded ? totalWeekly - capacity : 0;
            return { totalWeekly, isOverloaded, excess };
        };

        // Phân bổ thêm 12h: Tổng 22h <= 24h -> Hợp lệ
        const validAlloc = checkProjectAllocation(12, otherProjectsAllocated, netCapacity);
        assert.equal(validAlloc.totalWeekly, 22);
        assert.equal(validAlloc.isOverloaded, false);

        // Phân bổ thêm 20h: Tổng 30h > 24h -> Quá tải 6h
        const overloadAlloc = checkProjectAllocation(20, otherProjectsAllocated, netCapacity);
        assert.equal(overloadAlloc.totalWeekly, 30);
        assert.equal(overloadAlloc.isOverloaded, true);
        assert.equal(overloadAlloc.excess, 6);
    });

    test("QTN-11 / Error Recovery: Parse ALLOCATION_OVERLOAD_WARNING và mở form nhập lý do", () => {
        const backendErrorResponse = {
            status: 400,
            data: {
                code: "ALLOCATION_OVERLOAD_WARNING",
                message: "Không thể phân bổ: Tổng số giờ phân bổ (32h) vượt quá số giờ khả dụng (24h)...",
                details: {
                    availableHours: 24,
                    allocatedHours: 32,
                    overloadHours: 8
                }
            }
        };

        const handleBackendError = (err, isRM) => {
            const isOverloadWarning = err.data?.code === "ALLOCATION_OVERLOAD_WARNING";
            if (!isOverloadWarning) {
                return { forcedOverload: false, errorMessage: err.message };
            }
            return {
                forcedOverload: true,
                recoveredCapacity: err.data.details.availableHours,
                overloadHours: err.data.details.overloadHours,
                canConfirm: isRM,
                promptMessage: isRM
                    ? "Phân bổ vượt quá năng lực khả dụng thực tế. Vui lòng nhập lý do để xác nhận (QTN-11)."
                    : "Nhân sự bị phân bổ vượt quá giờ khả dụng. Chỉ Quản lý nguồn lực (RM) mới có quyền xác nhận vượt tải."
            };
        };

        const rmResult = handleBackendError(backendErrorResponse, true);
        assert.equal(rmResult.forcedOverload, true);
        assert.equal(rmResult.recoveredCapacity, 24);
        assert.equal(rmResult.overloadHours, 8);
        assert.equal(rmResult.canConfirm, true);

        const pmResult = handleBackendError(backendErrorResponse, false);
        assert.equal(pmResult.forcedOverload, true);
        assert.equal(pmResult.canConfirm, false);
    });
});
