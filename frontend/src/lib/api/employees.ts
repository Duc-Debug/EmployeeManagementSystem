"use client";

import { apiRequest } from "../api-client";

export interface EmployeeProfile {
  id: number;
  userId?: number;
  orgUnitId: number;
  orgUnitName: string;
  employeeCode: string;
  fullName: string;
  email?: string;
  professionalRole?: string;
  startDate?: string;
  contractEndDate?: string;
  standardHoursPerWeek: number;
  isOutsourced?: boolean;
  providerName?: string;
  version: number;
}

export interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export async function getEmployees(page: number = 1, size: number = 50): Promise<PageResult<EmployeeProfile>> {
  return await apiRequest<PageResult<EmployeeProfile>>(`/employees?page=${page}&size=${size}`, {
    method: "GET",
  });
}

/**
 * Tải toàn bộ danh sách nhân viên sử dụng phân trang size <= 50 an toàn
 */
export async function fetchAllEmployees(
  fetchPage: (page: number, size: number) => Promise<PageResult<EmployeeProfile>> = getEmployees
): Promise<EmployeeProfile[]> {
  const pageSize = 50;
  const allEmployees: EmployeeProfile[] = [];
  let page = 1;

  while (true) {
    const result = await fetchPage(page, pageSize);
    const items = result?.content || [];
    allEmployees.push(...items);
    const totalPages = result?.totalPages || 1;

    if (items.length === 0 || allEmployees.length >= (result?.totalElements || 0) || page >= totalPages) {
      break;
    }
    page++;
  }

  return allEmployees;
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

export interface DeclareOutsourcedEmployeePayload {
  orgUnitId: number;
  employeeCode?: string;
  fullName: string;
  providerName: string;
  professionalRole?: string;
  startDate: string;
  contractEndDate: string;
  standardHoursPerWeek?: number;
  skillIds?: number[];
}

export interface OutsourcedEmployeeResult {
  id: number;
  orgUnitId: number;
  employeeCode: string;
  fullName: string;
  providerName: string;
  professionalRole?: string;
  startDate: string;
  contractEndDate: string;
  standardHoursPerWeek: number;
  isOutsourced: boolean;
  version: number;
}

export async function declareOutsourcedEmployee(
  payload: DeclareOutsourcedEmployeePayload
): Promise<OutsourcedEmployeeResult> {
  return await apiRequest<OutsourcedEmployeeResult>(`/employees/outsourced`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}


