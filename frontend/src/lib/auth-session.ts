"use client";

import { useSyncExternalStore } from "react";
import type { DataScope, RoleCode, UserStatus } from "@/types/hrm";

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
}

const TOKEN_KEY = "nexushrm_auth_token";
const USER_KEY = "nexushrm_auth_user";

const listeners = new Set<() => void>();

let cachedUserRaw: string | null = null;
let cachedUserSnapshot: AuthUser | null = null;

function notify() {
  listeners.forEach((listener) => listener());
}

export function subscribeAuth(callback: () => void) {
  listeners.add(callback);
  return () => listeners.delete(callback);
}

export function getAuthToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return localStorage.getItem(TOKEN_KEY);
}

export function setAuthToken(token: string): void {
  if (typeof window === "undefined") {
    return;
  }
  localStorage.setItem(TOKEN_KEY, token);
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
    cachedUserSnapshot = JSON.parse(raw) as AuthUser;
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
  const serialized = JSON.stringify(user);
  cachedUserRaw = serialized;
  cachedUserSnapshot = user;
  localStorage.setItem(USER_KEY, serialized);
  notify();
}

export function clearAuthSession(): void {
  if (typeof window === "undefined") {
    return;
  }
  cachedUserRaw = null;
  cachedUserSnapshot = null;
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
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
