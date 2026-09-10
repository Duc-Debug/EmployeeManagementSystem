"use client";

import { apiRequest } from "../api-client";

export interface SkillResponse {
  id: number;
  name: string;
  description?: string;
  groupId: number;
  groupName: string;
  status: "ACTIVE" | "INACTIVE";
  statusDisplayName?: string;
  version: number;
}

export interface SkillGroupResponse {
  id: number;
  name: string;
  description?: string;
  status: "ACTIVE" | "INACTIVE";
  statusDisplayName?: string;
  skillCount: number;
  version: number;
}

export interface EmployeeSkillResponse {
  id: number;
  employeeId: number;
  skillId: number;
  skillCode?: string;
  skillName: string;
  skillCategory?: string;
  proficiencyLevel: number;
  yearsOfExperience: number;
  status: "PENDING" | "APPROVED" | "REJECTED";
  statusDisplayName?: string;
  reviewNotes?: string;
  version?: number;
}

export interface PendingEmployeeSkillItem {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  orgUnitId?: number;
  orgUnitName?: string;
  skillId: number;
  skillCode?: string;
  skillName: string;
  skillCategory?: string;
  proficiencyLevel: number;
  yearsOfExperience: number;
  status: string;
  createdAt?: string;
}

export interface SkillMatrixSkillHeader {
  id: number;
  code: string;
  name: string;
  category: string;
  employeeCount: number;
  singlePersonRisk: boolean;
}

export interface SkillMatrixCell {
  skillId: number;
  proficiencyLevel?: number;
  yearsOfExperience?: number;
  reviewNotes?: string;
}

export interface SkillMatrixRow {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  professionalRole?: string;
  skills: Record<string | number, SkillMatrixCell>;
}

export interface SkillMatrixSummary {
  totalEmployees: number;
  totalSkills: number;
  singlePersonRiskSkillCount: number;
  unstaffedSkillCount: number;
}

export interface DepartmentSkillMatrixResponse {
  orgUnitId: number;
  orgUnitCode: string;
  orgUnitName: string;
  skills: SkillMatrixSkillHeader[];
  rows: SkillMatrixRow[];
  summary: SkillMatrixSummary;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

function unwrapData<T>(res: any): T {
  if (res && typeof res === "object" && "data" in res && "success" in res) {
    return res.data as T;
  }
  return res as T;
}

function unwrapList<T>(res: any): T[] {
  if (Array.isArray(res)) return res as T[];
  if (res && Array.isArray(res.data)) return res.data as T[];
  return [];
}

/* ── Standard Skill Catalog APIs ─────────────────────────── */

export async function getSkills(params?: {
  groupId?: number;
  status?: string;
  keyword?: string;
}): Promise<SkillResponse[]> {
  const query = new URLSearchParams();
  if (params?.groupId) query.append("groupId", String(params.groupId));
  if (params?.status) query.append("status", params.status);
  if (params?.keyword) query.append("keyword", params.keyword);

  const qs = query.toString() ? `?${query.toString()}` : "";
  const res = await apiRequest<any>(`/skills${qs}`);
  return unwrapList<SkillResponse>(res);
}

export async function getSkillGroups(): Promise<SkillGroupResponse[]> {
  const res = await apiRequest<any>("/skills/groups");
  return unwrapList<SkillGroupResponse>(res);
}

export async function createSkill(payload: {
  name: string;
  description?: string;
  groupId: number;
}): Promise<SkillResponse> {
  const res = await apiRequest<any>("/skills", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return unwrapData<SkillResponse>(res);
}

export async function updateSkill(
  id: number,
  payload: {
    name: string;
    description?: string;
    groupId: number;
    version: number;
  }
): Promise<SkillResponse> {
  const res = await apiRequest<any>(`/skills/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  return unwrapData<SkillResponse>(res);
}

export async function deactivateSkill(id: number): Promise<SkillResponse> {
  const res = await apiRequest<any>(`/skills/${id}/deactivate`, {
    method: "PATCH",
  });
  return unwrapData<SkillResponse>(res);
}

export async function mergeSkills(payload: {
  sourceSkillId: number;
  targetSkillId: number;
}): Promise<SkillResponse> {
  const res = await apiRequest<any>("/skills/merge", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return unwrapData<SkillResponse>(res);
}

export async function createSkillGroup(payload: {
  name: string;
  description?: string;
}): Promise<SkillGroupResponse> {
  const res = await apiRequest<any>("/skills/groups", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return unwrapData<SkillGroupResponse>(res);
}

/* ── Personal Employee Skill APIs ────────────────────────── */

export async function getMySkills(): Promise<EmployeeSkillResponse[]> {
  const res = await apiRequest<any>("/employees/me/skills");
  return unwrapList<EmployeeSkillResponse>(res);
}

export async function declareMySkill(payload: {
  skillId: number;
  proficiencyLevel: number;
  yearsOfExperience: number;
}): Promise<EmployeeSkillResponse> {
  const res = await apiRequest<any>("/employees/me/skills", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return unwrapData<EmployeeSkillResponse>(res);
}

export async function updateMySkill(
  skillId: number,
  payload: {
    skillId: number;
    proficiencyLevel: number;
    yearsOfExperience: number;
  }
): Promise<EmployeeSkillResponse> {
  const res = await apiRequest<any>(`/employees/me/skills/${skillId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  return unwrapData<EmployeeSkillResponse>(res);
}

export async function deleteMySkill(skillId: number): Promise<void> {
  await apiRequest<ApiResponse<void>>(`/employees/me/skills/${skillId}`, {
    method: "DELETE",
  });
}

/* ── Skill Approval APIs (RM / VT-03, Admin / VT-06) ─────── */

export async function getPendingSkills(keyword?: string): Promise<PendingEmployeeSkillItem[]> {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
  const res = await apiRequest<any>(`/employee-skills/pending${query}`);
  return unwrapList<PendingEmployeeSkillItem>(res);
}

export async function approveSkill(
  id: number,
  payload: {
    adjustedProficiencyLevel?: number;
    reviewNotes?: string;
  }
): Promise<EmployeeSkillResponse> {
  const res = await apiRequest<any>(`/employee-skills/${id}/approve`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  return unwrapData<EmployeeSkillResponse>(res);
}

export async function rejectSkill(id: number, rejectionReason?: string): Promise<EmployeeSkillResponse> {
  const res = await apiRequest<any>(`/employee-skills/${id}/reject`, {
    method: "PUT",
    body: JSON.stringify({ rejectionReason }),
  });
  return unwrapData<EmployeeSkillResponse>(res);
}

/* ── Department Skill Matrix API ─────────────────────────── */

export async function getDepartmentSkillMatrix(
  orgUnitId: number
): Promise<DepartmentSkillMatrixResponse> {
  const res = await apiRequest<any>(
    `/skills/matrix?orgUnitId=${orgUnitId}`
  );
  return unwrapData<DepartmentSkillMatrixResponse>(res);
}

/* ── Resource Search by Skill & Availability API (VT-02, VT-03, VT-06) ── */

export interface WeeklyAvailabilityBar {
  year: number;
  weekNumber: number;
  standardHours: number;
  netAvailableHours: number;
  totalAllocatedHours: number;
  remainingHours: number;
}

export interface ResourceSearchResultItem {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  orgUnitId: number;
  orgUnitName: string;
  jobTitle?: string;
  skillId: number;
  skillName: string;
  proficiencyLevel: number;
  yearsOfExperience: number;
  weeklyAvailabilities: WeeklyAvailabilityBar[];
  totalRemainingHours: number;
}

export async function searchResourcesBySkill(params: {
  skillId: number;
  minProficiencyLevel?: number;
  orgUnitId?: number;
  fromYear: number;
  fromWeek: number;
  toYear: number;
  toWeek: number;
}): Promise<ResourceSearchResultItem[]> {
  const query = new URLSearchParams();
  query.append("skillId", String(params.skillId));
  if (params.minProficiencyLevel) query.append("minProficiencyLevel", String(params.minProficiencyLevel));
  if (params.orgUnitId) query.append("orgUnitId", String(params.orgUnitId));
  query.append("fromYear", String(params.fromYear));
  query.append("fromWeek", String(params.fromWeek));
  query.append("toYear", String(params.toYear));
  query.append("toWeek", String(params.toWeek));

  const res = await apiRequest<any>(`/allocations/search?${query.toString()}`);
  return unwrapList<ResourceSearchResultItem>(res);
}

