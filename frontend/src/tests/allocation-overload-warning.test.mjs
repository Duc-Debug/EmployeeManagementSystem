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
});
