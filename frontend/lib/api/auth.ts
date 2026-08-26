"use client";

import { apiRequest } from "../api-client";
import {
  clearAuthSession,
  setAuthToken,
  setStoredUser,
  type AuthUser,
} from "../auth-session";
import type { DataScope, RoleCode, UserStatus } from "@/src/types/hrm";

export interface LoginPayload {
  password: string;
  username: string;
}

export interface AuthTokenResponse {
  roleCode: string;
  token: string;
  tokenType: string;
  userId: number;
  username: string;
}

export interface ChangePasswordPayload {
  confirmPassword: string;
  currentPassword: string;
  newPassword: string;
}

export interface UserResultDto {
  dataScope: DataScope;
  employeeId: number | null;
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

export async function login(payload: LoginPayload): Promise<AuthUser> {
  const loginRes = await apiRequest<AuthTokenResponse>("/auth/login", {
    body: JSON.stringify(payload),
    method: "POST",
  });

  const token = loginRes.token;
  setAuthToken(token);

  // Fetch full user profile after login
  try {
    const userRes = await apiRequest<UserResultDto>("/auth/me", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      method: "GET",
    });

    const authUser: AuthUser = {
      dataScope: userRes.dataScope,
      email: null,
      employeeCode: userRes.employeeId ? `EMP-${userRes.employeeId}` : null,
      fullName: userRes.fullName,
      id: userRes.id,
      orgUnitId: userRes.orgUnitId,
      orgUnitName: userRes.orgUnitName,
      roleCode: userRes.roleCode,
      roleName: userRes.roleName,
      scopeOrgUnitId: userRes.scopeOrgUnitId,
      status: userRes.status,
      username: userRes.username,
    };

    setStoredUser(authUser);
    return authUser;
  } catch {
    const fallbackUser: AuthUser = {
      dataScope: "COMPANY",
      email: null,
      employeeCode: null,
      fullName: loginRes.username,
      id: loginRes.userId,
      orgUnitId: null,
      orgUnitName: null,
      roleCode: loginRes.roleCode as RoleCode,
      roleName: loginRes.roleCode === "VT-06" ? "Quản trị viên" : loginRes.roleCode,
      scopeOrgUnitId: null,
      status: "ACTIVE",
      username: loginRes.username,
    };
    setStoredUser(fallbackUser);
    return fallbackUser;
  }
}

export async function getCurrentUser(): Promise<AuthUser> {
  const userRes = await apiRequest<UserResultDto>("/auth/me", {
    method: "GET",
  });

  const authUser: AuthUser = {
    dataScope: userRes.dataScope,
    email: null,
    employeeCode: userRes.employeeId ? `EMP-${userRes.employeeId}` : null,
    fullName: userRes.fullName,
    id: userRes.id,
    orgUnitId: userRes.orgUnitId,
    orgUnitName: userRes.orgUnitName,
    roleCode: userRes.roleCode,
    roleName: userRes.roleName,
    scopeOrgUnitId: userRes.scopeOrgUnitId,
    status: userRes.status,
    username: userRes.username,
  };

  setStoredUser(authUser);
  return authUser;
}

export async function changePassword(payload: ChangePasswordPayload): Promise<void> {
  await apiRequest<void>("/auth/change-password", {
    body: JSON.stringify(payload),
    method: "POST",
  });
}

export function logout(): void {
  clearAuthSession();
  if (typeof window !== "undefined") {
    window.location.href = "/login";
  }
}
