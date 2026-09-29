"use client";

import { useSyncExternalStore } from "react";
import type { DataScope, RoleCode, UserStatus } from "@/types/hrm";
import { normalizeRoleCode } from "./role-utils";

export interface AuthUser {
  dataScope: DataScope;
  email: string | null;
  employeeCode: string | null;
  fullName: string;
  id: number;
  orgUnitId: number | null;
  orgUnitName: string | null;
  roleCode: RoleCode;
  roleName: string;
  scopeOrgUnitId: number | null;
  status: UserStatus;
  username: string;
  permissions?: string[];
  requiresPasswordChange?: boolean;
}

const TOKEN_KEY = "nexushrm_auth_token";
const USER_KEY = "nexushrm_auth_user";

const listeners = new Set<() => void>();

let cachedUserRaw: string | null = null;
let cachedUserSnapshot: AuthUser | null = null;

// Security architecture: Access tokens are exclusively stored in HttpOnly SameSite cookies.
// JavaScript runtime NEVER receives, handles, or stores raw JWT tokens to completely eliminate XSS token theft.
function notify() {
  listeners.forEach((listener) => listener());
}

export function subscribeAuth(callback: () => void) {
  listeners.add(callback);
  return () => listeners.delete(callback);
}

export function getAuthToken(): string | null {
  return null;
}

export function setAuthToken(_token?: string): void {
  if (typeof window !== "undefined") {
    // Defense-in-depth: Actively eliminate JWT from localStorage to prevent XSS exfiltration
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem("accessToken");
    localStorage.removeItem("token");
  }
  notify();
}

export function getStoredUser(): AuthUser | null {
  if (typeof window === "undefined") {
    return null;
  }
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    cachedUserRaw = null;
    cachedUserSnapshot = null;
    return null;
  }
  if (raw === cachedUserRaw && cachedUserSnapshot !== null) {
    return cachedUserSnapshot;
  }
  try {
    cachedUserRaw = raw;
    const parsed = JSON.parse(raw) as AuthUser;
    if (parsed && parsed.roleCode) {
      const canonical = normalizeRoleCode(parsed.roleCode);
      if (canonical) {
        parsed.roleCode = canonical;
      }
    }
    cachedUserSnapshot = parsed;
    return cachedUserSnapshot;
  } catch {
    cachedUserRaw = null;
    cachedUserSnapshot = null;
    return null;
  }
}

export function setStoredUser(user: AuthUser): void {
  if (typeof window === "undefined") {
    return;
  }
  const canonicalRole = normalizeRoleCode(user.roleCode) || user.roleCode;
  const canonicalUser: AuthUser = {
    ...user,
    roleCode: canonicalRole,
  };
  const serialized = JSON.stringify(canonicalUser);
  cachedUserRaw = serialized;
  cachedUserSnapshot = canonicalUser;
  localStorage.setItem(USER_KEY, serialized);
  // Remove redundant legacy key if present
  localStorage.removeItem("currentUser");
  notify();
}

/**
 * Purges all authentication tokens, user state, and temporary session keys
 * from localStorage and sessionStorage.
 */
export function clearAuthSession(): void {
  cachedUserRaw = null;
  cachedUserSnapshot = null;
  if (typeof window === "undefined") {
    notify();
    return;
  }
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);

  // Defense-in-depth: Thoroughly clean legacy and duplicate credentials
  localStorage.removeItem("accessToken");
  localStorage.removeItem("currentUser");
  localStorage.removeItem("token");
  try {
    sessionStorage.removeItem("demo-session");
  } catch {
    // Ignore restricted environment errors
  }
  notify();
}

const SERVER_SNAPSHOT: AuthUser | null = null;
function getServerSnapshot(): AuthUser | null {
  return SERVER_SNAPSHOT;
}

export function useAuthUser(): AuthUser | null {
  return useSyncExternalStore(
    subscribeAuth,
    getStoredUser,
    getServerSnapshot
  );
}
