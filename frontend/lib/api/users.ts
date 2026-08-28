"use client";

import { apiRequest } from "../api-client";
import type { DataScope, RoleCode, User } from "@/src/types/hrm";

export interface CreateUserPayload {
  employeeCode?: string;
  fullName: string;
  orgUnitId?: number | null;
  password?: string;
  roleCode: RoleCode;
  username: string;
}

export interface UpdateUserRolePayload {
  dataScope: DataScope;
  roleCode: RoleCode;
  scopeOrgUnitId?: number | null;
}

export interface UpdateUserPayload {
  dataScope?: DataScope;
  email?: string;
  employeeCode?: string;
  fullName: string;
  orgUnitId?: number | null;
  roleCode: RoleCode;
  scopeOrgUnitId?: number | null;
}

export interface PageResult<T> {
  content: T[];
  first: boolean;
  last: boolean;
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

export async function getUsers(page = 0, size = 100): Promise<PageResult<User>> {
  return await apiRequest<PageResult<User>>(`/users?page=${page}&size=${size}`, {
    method: "GET",
  });
}

export async function getUserById(id: number): Promise<User> {
  return await apiRequest<User>(`/users/${id}`, {
    method: "GET",
  });
}

export async function createUser(payload: CreateUserPayload): Promise<User> {
  return await apiRequest<User>("/users", {
    body: JSON.stringify(payload),
    method: "POST",
  });
}

export async function updateUser(id: number, payload: UpdateUserPayload): Promise<User> {
  return await apiRequest<User>(`/users/${id}`, {
    body: JSON.stringify(payload),
    method: "PUT",
  });
}

export async function updateUserRole(id: number, payload: UpdateUserRolePayload): Promise<User> {
  return await apiRequest<User>(`/users/${id}/role`, {
    body: JSON.stringify(payload),
    method: "PUT",
  });
}

export async function toggleUserStatus(id: number, lock: boolean): Promise<User> {
  return await apiRequest<User>(`/users/${id}/status?lock=${lock}`, {
    method: "PATCH",
  });
}
