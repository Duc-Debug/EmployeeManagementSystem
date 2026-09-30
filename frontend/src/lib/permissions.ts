"use client";

import { getStoredUser, type AuthUser } from "./auth-session";
import { normalizeRoleCode } from "./role-utils";

/**
 * SECURITY ARCHITECTURE NOTICE:
 * Client-side permission checks (can, canAny, canAll, hasRole) are strictly
 * for User Experience (UX) optimizations: conditionally hiding/showing menus,
 * buttons, and routes.
 *
 * The client storage (localStorage / in-memory state) is NEVER treated as a
 * trusted authorization boundary. Backend Spring Security endpoints and domain
 * services are the authoritative source of truth and independently enforce
 * all role and permission constraints on every single API request.
 */

/**
 * Returns current user's granted fine-grained permissions.
 */
export function getEffectivePermissions(user?: AuthUser | null): string[] {
    const u = user !== undefined ? user : getStoredUser();
    return u?.permissions || [];
}

/**
 * Checks if the user has a specific fine-grained permission.
 */
export function can(permission: string, user?: AuthUser | null): boolean {
    const u = user !== undefined ? user : getStoredUser();
    if (!u || !u.permissions) return false;
    return u.permissions.includes(permission);
}

/**
 * Checks if the user has at least one of the specified permissions.
 */
export function canAny(permissions: readonly string[], user?: AuthUser | null): boolean {
    const u = user !== undefined ? user : getStoredUser();
    if (!u || !u.permissions) return false;
    return permissions.some((p) => u.permissions!.includes(p));
}

/**
 * Checks if the user has all of the specified permissions.
 */
export function canAll(permissions: readonly string[], user?: AuthUser | null): boolean {
    const u = user !== undefined ? user : getStoredUser();
    if (!u || !u.permissions) return false;
    return permissions.every((p) => u.permissions!.includes(p));
}

/**
 * Checks if the user's roleCode matches one of the specified roles.
 * Normalizes role codes and aliases (e.g. 'ROLE_VT_01' -> 'VT-01', 'DIRECTOR' -> 'VT-01').
 */
export function hasRole(roles: string | readonly string[], user?: AuthUser | null): boolean {
    const u = user !== undefined ? user : getStoredUser();
    if (!u || !u.roleCode) return false;
    const userRole = normalizeRoleCode(u.roleCode);
    if (!userRole) return false;

    const roleList = Array.isArray(roles) ? roles : [roles];
    return roleList.some((r) => normalizeRoleCode(r) === userRole);
}
