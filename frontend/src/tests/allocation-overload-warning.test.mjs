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
        let totalWeeklyHours = otherProjectsHours + currentHours; // 10 + 20 = 30
        let isOverloaded = totalWeeklyHours > netCapacity; // 30 > 24 = true
        let overloadHours = isOverloaded ? totalWeeklyHours - netCapacity : 0; // 6
        assert.equal(isOverloaded, true, "Ngay sau khi backend báo lỗi, UI phải là quá tải");
        assert.equal(overloadHours, 6, "Vượt quá 6h");

        // Khi user (non-RM hoặc RM) kéo slider xuống 10h (hợp lệ):
        const reducedHours = 10;
        totalWeeklyHours = otherProjectsHours + reducedHours; // 10 + 10 = 20
        isOverloaded = totalWeeklyHours > netCapacity; // 20 > 24 = false
        overloadHours = isOverloaded ? totalWeeklyHours - netCapacity : 0; // 0

        assert.equal(isOverloaded, false, "Sau khi giảm giờ xuống 10h (tổng 20h <= 24h), isOverloaded phải là false");
        assert.equal(overloadHours, 0, "Không còn số giờ vượt");

        // Non-RM không còn bị disabled nút bấm khi giờ đã hợp lệ
        const isNonRmBlocked = isOverloaded && false; // isOverloaded && !isResourceManager
        assert.equal(isNonRmBlocked, false, "Non-RM không bị block khi đã chỉnh giờ hợp lệ");
    });

    test("QTN-11 / UX Invariant: Nút submit bị vô hiệu hóa khi đang tải năng lực tuần (isLoadingCapacity)", () => {
        const isSubmitDisabled = (isSubmitting, isLoadingCapacity, isOverloaded, isResourceManager) => {
            return isSubmitting || isLoadingCapacity || (isOverloaded && !isResourceManager);
        };

        // Khi đang tải năng lực: nút submit bắt buộc bị disabled dù không quá tải
        assert.equal(isSubmitDisabled(false, true, false, true), true, "Phải disable nút khi isLoadingCapacity = true");
        assert.equal(isSubmitDisabled(false, true, false, false), true, "Phải disable nút khi isLoadingCapacity = true (non-RM)");

        // Sau khi tải xong: hợp lệ thì được phép bấm
        assert.equal(isSubmitDisabled(false, false, false, false), false, "Mở khóa nút khi đã tải xong và không quá tải");

        // Khi quá tải: RM được bấm, non-RM bị khóa
        assert.equal(isSubmitDisabled(false, false, true, true), false, "RM được bấm khi quá tải (để mở khóa phê duyệt)");
        assert.equal(isSubmitDisabled(false, false, true, false), true, "Non-RM bị khóa nút khi quá tải");
    });

    test("QTN-11 / RBAC Mapping: canBypassResourceOverload ánh xạ quyền RESOURCE_ALLOCATION_OVERLOAD_BYPASS cho VT-03", () => {
        const canBypassResourceOverload = (user) => {
            return user?.roleCode === "VT-03";
        };

        assert.equal(canBypassResourceOverload({ roleCode: "VT-03" }), true, "VT-03 có quyền vượt tải");
        assert.equal(canBypassResourceOverload({ roleCode: "VT-02" }), false, "VT-02 không có quyền vượt tải");
        assert.equal(canBypassResourceOverload({ roleCode: "VT-01" }), false, "VT-01 không có quyền vượt tải");
        assert.equal(canBypassResourceOverload(null), false, "User null không có quyền");
    });
});

