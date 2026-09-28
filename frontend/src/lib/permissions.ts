"use client";

import { getStoredUser, type AuthUser } from "./auth-session";

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
 * Normalizes role codes (e.g. 'ROLE_VT_01' -> 'VT-01').
 */
export function hasRole(roles: string | readonly string[], user?: AuthUser | null): boolean {
    const u = user !== undefined ? user : getStoredUser();
    if (!u || !u.roleCode) return false;
    const normalized = u.roleCode.toUpperCase().replace(/_/g, "-");
    const roleList = Array.isArray(roles) ? roles : [roles];
    return roleList.some((r) => {
        const normR = r.toUpperCase().replace(/_/g, "-");
        return normR === normalized || normR === `ROLE-${normalized}` || `ROLE-${normR}` === normalized;
    });
}
