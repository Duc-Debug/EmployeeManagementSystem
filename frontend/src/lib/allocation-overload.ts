/**
 * Thư viện hàm nghiệp vụ thuần túy (pure functions) xử lý tính toán quá tải
 * và kiểm tra quyền vượt tải (NCL-06-CN-003 / QTN-11).
 */

export interface AuthUserLike {
    roleCode?: string | null;
    permissions?: string[] | null;
}

/**
 * Kiểm tra thẩm quyền phê duyệt phân bổ vượt tải (QTN-11).
 * Ưu tiên kiểm tra permission code RESOURCE_ALLOCATION_OVERLOAD_BYPASS nếu có,
 * kết hợp fallback tương thích 100% với vai trò Quản lý nguồn lực (VT-03).
 */
export function canBypassResourceOverload(user: AuthUserLike | null | undefined): boolean {
    if (!user) return false;
    if (Array.isArray(user.permissions) && user.permissions.includes('RESOURCE_ALLOCATION_OVERLOAD_BYPASS')) {
        return true;
    }
    return user.roleCode === 'VT-03';
}

export interface OverloadCalculationResult {
    totalWeeklyHours: number;
    isOverloaded: boolean;
    overloadHours: number;
    utilizationPercentage: number;
}

/**
 * Tính toán tổng số giờ phân bổ trong tuần, trạng thái quá tải và số giờ vượt.
 */
export function computeAllocationOverload(
    hours: number,
    otherProjectsHours: number,
    capacity: number
): OverloadCalculationResult {
    const totalWeeklyHours = otherProjectsHours + hours;
    const isOverloaded = totalWeeklyHours > capacity;
    const overloadHours = isOverloaded ? Math.max(0, totalWeeklyHours - capacity) : 0;
    const utilizationPercentage = capacity > 0
        ? Math.round((totalWeeklyHours / capacity) * 100)
        : (hours > 0 ? 100 : 0);

    return {
        totalWeeklyHours,
        isOverloaded,
        overloadHours,
        utilizationPercentage,
    };
}

/**
 * Invariant bảo vệ nút Submit:
 * - Vô hiệu hóa khi đang submit
 * - Vô hiệu hóa khi đang tải năng lực tuần (tránh race condition hoặc dữ liệu stale)
 * - Vô hiệu hóa khi tuần bị quá tải nhưng người dùng không có thẩm quyền phê duyệt
 */
export function isAdjustHoursSubmitDisabled(
    isSubmitting: boolean,
    isLoadingCapacity: boolean,
    isOverloaded: boolean,
    canBypass: boolean
): boolean {
    return isSubmitting || isLoadingCapacity || (isOverloaded && !canBypass);
}

export interface OverloadValidationResult {
    valid: boolean;
    error?: string;
}

/**
 * Xác thực dữ liệu gửi lên khi phân bổ giờ.
 */
export function validateOverloadSubmission(
    isOverloaded: boolean,
    canBypass: boolean,
    reason?: string | null
): OverloadValidationResult {
    if (!isOverloaded) {
        return { valid: true };
    }
    if (!canBypass) {
        return {
            valid: false,
            error: 'Chỉ Quản lý nguồn lực (RM) mới có quyền phê duyệt phân bổ vượt năng lực.',
        };
    }
    if (!reason || !reason.trim()) {
        return {
            valid: false,
            error: 'Vui lòng nhập lý do chấp nhận quá tải (QTN-11).',
        };
    }
    return { valid: true };
}
