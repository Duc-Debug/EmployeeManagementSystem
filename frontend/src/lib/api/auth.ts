"use client";

import { apiRequest } from "../api-client";
import {
  clearAuthSession,
  setAuthToken,
  setStoredUser,
  type AuthUser,
} from "../auth-session";
import type { DataScope, RoleCode, UserStatus } from "@/types/hrm";

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
  requiresPasswordChange?: boolean;
}

export interface ChangePasswordPayload {
  confirmPassword: string;
  currentPassword: string;
  newPassword: string;
}

export interface UserResultDto {
  dataScope: DataScope;
  email?: string | null;
  employeeCode?: string | null;
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
  permissions?: string[];
}

export function mapAuthUser(userRes: UserResultDto): AuthUser {
  return {
    dataScope: userRes.dataScope,
    email: userRes.email ?? null,
    employeeCode: userRes.employeeCode ?? null,
    fullName: userRes.fullName,
    id: userRes.id,
    orgUnitId: userRes.orgUnitId,
    orgUnitName: userRes.orgUnitName,
    roleCode: userRes.roleCode,
    roleName: userRes.roleName,
    scopeOrgUnitId: userRes.scopeOrgUnitId,
    status: userRes.status,
    username: userRes.username,
    permissions: userRes.permissions || [],
  };
}

export async function login(payload: LoginPayload): Promise<AuthUser> {
  const loginRes = await apiRequest<AuthTokenResponse>("/auth/login", {
    body: JSON.stringify(payload),
    method: "POST",
  });

  const token = loginRes.token;
  setAuthToken(token);

  // Fetch full user profile after login - strictly require /auth/me to succeed
  try {
    const userRes = await apiRequest<UserResultDto>("/auth/me", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      method: "GET",
    });

    const authUser = mapAuthUser(userRes);
    if (loginRes.requiresPasswordChange !== undefined) {
      authUser.requiresPasswordChange = Boolean(loginRes.requiresPasswordChange);
    }

    setStoredUser(authUser);
    return authUser;
  } catch (error) {
    clearAuthSession();
    throw error;
  }
}

export async function getCurrentUser(): Promise<AuthUser> {
  const userRes = await apiRequest<UserResultDto>("/auth/me", {
    method: "GET",
  });

  const authUser = mapAuthUser(userRes);

  setStoredUser(authUser);
  return authUser;
}

export async function changePassword(payload: ChangePasswordPayload): Promise<void> {
  await apiRequest<void>("/auth/change-password", {
    body: JSON.stringify(payload),
    method: "POST",
  });
  const current = typeof window !== "undefined" ? JSON.parse(localStorage.getItem("nexushrm_auth_user") || "null") : null;
  if (current) {
    current.requiresPasswordChange = false;
    setStoredUser(current);
  }
}

export interface ForgotPasswordPayload {
  identity: string;
}

export interface ResetPasswordPayload {
  confirmPassword: string;
  newPassword: string;
  token: string;
}

export async function forgotPassword(identity: string): Promise<string> {
  await apiRequest<void>("/auth/forgot-password", {
    body: JSON.stringify({ identity }),
    method: "POST",
  });
  return "Nếu thông tin tài khoản hợp lệ, liên kết khôi phục mật khẩu đã được gửi đến email đăng ký.";
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<string> {
  await apiRequest<void>("/auth/reset-password", {
    body: JSON.stringify(payload),
    method: "POST",
  });
  return "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.";
}

export function logout(): void {
  clearAuthSession();
  if (typeof window !== "undefined") {
    window.location.href = "/login";
  }
}
