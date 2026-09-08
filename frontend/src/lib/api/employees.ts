"use client";

import { apiRequest } from "../api-client";

export interface EmployeeProfile {
  id: number;
  userId: number;
  orgUnitId: number;
  orgUnitName: string;
  employeeCode: string;
  fullName: string;
  professionalRole?: string;
  startDate?: string;
  contractEndDate?: string;
  standardHoursPerWeek: number;
  version: number;
}

export interface UpdateEmployeeProfilePayload {
  version: number;
  orgUnitId: number;
  fullName: string;
  professionalRole?: string;
  startDate?: string;
  contractEndDate?: string;
  standardHoursPerWeek: number;
}

export async function getEmployeeProfile(id: number): Promise<EmployeeProfile> {
  return await apiRequest<EmployeeProfile>(`/employees/${id}`, {
    method: "GET",
  });
}

export async function getEmployeeProfileByUserId(userId: number): Promise<EmployeeProfile> {
  return await apiRequest<EmployeeProfile>(`/employees/by-user/${userId}`, {
    method: "GET",
  });
}

export async function updateEmployeeProfile(
  id: number,
  payload: UpdateEmployeeProfilePayload
): Promise<EmployeeProfile> {
  return await apiRequest<EmployeeProfile>(`/employees/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export interface CreateEmployeeProfilePayload {
  userId: number;
  orgUnitId: number;
  employeeCode: string;
  fullName: string;
  professionalRole?: string;
  startDate?: string;
  contractEndDate?: string;
  standardHoursPerWeek: number;
}

export async function createEmployeeProfile(
  payload: CreateEmployeeProfilePayload
): Promise<EmployeeProfile> {
  return await apiRequest<EmployeeProfile>(`/employees`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

