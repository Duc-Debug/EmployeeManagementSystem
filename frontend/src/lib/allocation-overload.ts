/**
 * Thư viện hàm nghiệp vụ thuần túy (pure functions) xử lý tính toán quá tải
 * và kiểm tra quyền vượt tải (NCL-06-CN-003 / QTN-11).
 */

export interface AuthUserLike {
    roleCode?: string | null;
    permissions?: string[] | null;
}

export const RESOURCE_OVERLOAD_BYPASS_PERMISSION = 'RESOURCE_ALLOCATION_OVERLOAD_BYPASS';

/**
 * Danh mục quyền mặc định theo vai trò (đồng bộ với Flyway migrations trong DB: V61, V35...).
 * Giúp frontend xác thực dựa trên permission một cách nhất quán (permission-based)
 * ngay cả khi token session chưa nhúng danh sách permissions đầy đủ.
 */
export const DEFAULT_ROLE_PERMISSIONS: Record<string, string[]> = {
    'VT-03': [
        'RESOURCE_ALLOCATION_MANAGE',
        'RESOURCE_ALLOCATION_OVERLOAD_BYPASS',
        'RESOURCE_RESERVATION_CREATE',
        'RESOURCE_RESERVATION_MANAGE',
    ],
    'VT-01': [],
    'VT-02': ['PROJECT_MANAGE', 'RESOURCE_RESERVATION_CREATE'],
    'VT-04': [],
    'VT-05': [],
    'VT-06': ['USER_MANAGE', 'ROLE_MANAGE'],
};

/**
 * Kiểm tra xem người dùng có permission cụ thể hay không (Permission-based Authorization).
 * 1. Ưu tiên kiểm tra mảng permissions gắn trực tiếp trên user session.
 * 2. Nếu chưa có mảng permissions, tra cứu qua bảng phân quyền chuẩn của vai trò.
 */
export function hasUserPermission(user: AuthUserLike | null | undefined, permissionCode: string): boolean {
    if (!user) return false;
    // 1. Khi user session có mảng permissions cụ thể, kiểm tra trực tiếp trên danh sách này (Strict Permission-based)
    if (Array.isArray(user.permissions)) {
        return user.permissions.includes(permissionCode);
    }
    // 2. Chỉ khi chưa có mảng permissions (session chỉ có roleCode), tra cứu theo bảng phân quyền chuẩn của vai trò
    if (user.roleCode && DEFAULT_ROLE_PERMISSIONS[user.roleCode]?.includes(permissionCode)) {
        return true;
    }
    return false;
}

/**
 * Kiểm tra thẩm quyền phê duyệt phân bổ vượt tải (QTN-11).
 * Xác thực hoàn toàn dựa trên Permission: RESOURCE_ALLOCATION_OVERLOAD_BYPASS.
 */
export function canBypassResourceOverload(user: AuthUserLike | null | undefined): boolean {
    return hasUserPermission(user, RESOURCE_OVERLOAD_BYPASS_PERMISSION);
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
 * - Vô hiệu hóa khi đang tải năng lực tuần (tránh race condition)
 * - Vô hiệu hóa khi tải năng lực thất bại hoặc chưa có dữ liệu năng lực hợp lệ (hasCapacityError)
 * - Vô hiệu hóa khi tuần bị quá tải nhưng người dùng không có thẩm quyền phê duyệt
 */
export function isAdjustHoursSubmitDisabled(
    isSubmitting: boolean,
    isLoadingCapacity: boolean,
    isOverloaded: boolean,
    canBypass: boolean,
    hasCapacityError: boolean = false
): boolean {
    return isSubmitting || isLoadingCapacity || hasCapacityError || (isOverloaded && !canBypass);
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
