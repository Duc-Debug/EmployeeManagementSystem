"use client";

import { apiRequest } from "../api-client";

export interface RoleResponse {
  id: number;
  code: string;
  name: string;
  description?: string;
}

export async function getRoles(): Promise<RoleResponse[]> {
  const roles = await apiRequest<RoleResponse[]>("/roles");
  return Array.isArray(roles)
    ? roles.filter((r) => r && r.code?.toUpperCase().replace(/_/g, '-') !== 'VT-07')
    : [];
}
