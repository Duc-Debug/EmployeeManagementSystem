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

const SESSION_USER_KEY = "nexushrm_session_user";

const listeners = new Set<() => void>();

let inMemoryToken: string | null = null;
let cachedUserSnapshot: AuthUser | null = null;

function notify() {
  listeners.forEach((listener) => listener());
}

export function subscribeAuth(callback: () => void) {
  listeners.add(callback);
  return () => listeners.delete(callback);
}

export function getAuthToken(): string | null {
  return inMemoryToken;
}

export function setAuthToken(token: string): void {
  inMemoryToken = token;
  if (typeof window !== "undefined") {
    // Purge tokens from localStorage to prevent XSS exfiltration
    localStorage.removeItem("nexushrm_auth_token");
    localStorage.removeItem("accessToken");
    localStorage.removeItem("token");
  }
  notify();
}

export function getStoredUser(): AuthUser | null {
  if (cachedUserSnapshot !== null) {
    return cachedUserSnapshot;
  }
  if (typeof window === "undefined") {
    return null;
  }
  const raw = sessionStorage.getItem(SESSION_USER_KEY) || localStorage.getItem("nexushrm_auth_user") || localStorage.getItem("currentUser");
  if (!raw) {
    cachedUserSnapshot = null;
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as AuthUser;
    if (parsed && parsed.roleCode) {
      const canonical = normalizeRoleCode(parsed.roleCode);
      if (canonical) {
        parsed.roleCode = canonical;
      }
    }
    cachedUserSnapshot = parsed;
    // Migrate to sessionStorage if it was in localStorage
    sessionStorage.setItem(SESSION_USER_KEY, JSON.stringify(cachedUserSnapshot));
    localStorage.removeItem("nexushrm_auth_user");
    localStorage.removeItem("currentUser");
    return cachedUserSnapshot;
  } catch {
    cachedUserSnapshot = null;
    return null;
  }
}

export function setStoredUser(user: AuthUser): void {
  const canonicalRole = normalizeRoleCode(user.roleCode) || user.roleCode;
  const canonicalUser: AuthUser = {
    ...user,
    roleCode: canonicalRole,
  };
  const serialized = JSON.stringify(canonicalUser);
  cachedUserSnapshot = canonicalUser;

  if (typeof window !== "undefined") {
    sessionStorage.setItem(SESSION_USER_KEY, serialized);
    localStorage.removeItem("nexushrm_auth_user");
    localStorage.removeItem("currentUser");
  }
  notify();
}

export function clearAuthSession(): void {
  inMemoryToken = null;
  cachedUserSnapshot = null;

  if (typeof window !== "undefined") {
    sessionStorage.removeItem(SESSION_USER_KEY);
    sessionStorage.clear();
    localStorage.removeItem("nexushrm_auth_token");
    localStorage.removeItem("nexushrm_auth_user");
    localStorage.removeItem("accessToken");
    localStorage.removeItem("token");
    localStorage.removeItem("currentUser");
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
