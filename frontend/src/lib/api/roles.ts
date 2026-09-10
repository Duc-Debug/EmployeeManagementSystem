"use client";

import { apiRequest } from "../api-client";

export interface RoleResponse {
  id: number;
  code: string;
  name: string;
  description?: string;
}

export async function getRoles(): Promise<RoleResponse[]> {
  return apiRequest<RoleResponse[]>("/roles");
}
