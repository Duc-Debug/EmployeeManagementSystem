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
  return apiRequest<SkillResponse[]>(`/skills${qs}`);
}

export async function getSkillGroups(): Promise<SkillGroupResponse[]> {
  return apiRequest<SkillGroupResponse[]>("/skills/groups");
}

export async function createSkill(payload: {
  name: string;
  description?: string;
  groupId: number;
}): Promise<SkillResponse> {
  return apiRequest<SkillResponse>("/skills", {
    method: "POST",
    body: JSON.stringify(payload),
  });
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
  return apiRequest<SkillResponse>(`/skills/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deactivateSkill(id: number): Promise<SkillResponse> {
  return apiRequest<SkillResponse>(`/skills/${id}/deactivate`, {
    method: "PATCH",
  });
}

export async function mergeSkills(payload: {
  sourceSkillId: number;
  targetSkillId: number;
}): Promise<SkillResponse> {
  return apiRequest<SkillResponse>("/skills/merge", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function createSkillGroup(payload: {
  name: string;
  description?: string;
}): Promise<SkillGroupResponse> {
  return apiRequest<SkillGroupResponse>("/skills/groups", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/* ── Personal Employee Skill APIs ────────────────────────── */

export async function getMySkills(): Promise<EmployeeSkillResponse[]> {
  const res = await apiRequest<ApiResponse<EmployeeSkillResponse[]>>("/employees/me/skills");
  return res.data || [];
}

export async function declareMySkill(payload: {
  skillId: number;
  proficiencyLevel: number;
  yearsOfExperience: number;
}): Promise<EmployeeSkillResponse> {
  const res = await apiRequest<ApiResponse<EmployeeSkillResponse>>("/employees/me/skills", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  return res.data;
}

export async function deleteMySkill(skillId: number): Promise<void> {
  await apiRequest<ApiResponse<void>>(`/employees/me/skills/${skillId}`, {
    method: "DELETE",
  });
}

/* ── Skill Approval APIs (RM / VT-03, Admin / VT-06) ─────── */

export async function getPendingSkills(keyword?: string): Promise<PendingEmployeeSkillItem[]> {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
  const res = await apiRequest<ApiResponse<PendingEmployeeSkillItem[]>>(`/employee-skills/pending${query}`);
  return res.data || [];
}

export async function approveSkill(
  id: number,
  payload: {
    adjustedProficiencyLevel?: number;
    reviewNotes?: string;
  }
): Promise<EmployeeSkillResponse> {
  const res = await apiRequest<ApiResponse<EmployeeSkillResponse>>(`/employee-skills/${id}/approve`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  return res.data;
}

export async function rejectSkill(id: number, rejectionReason?: string): Promise<EmployeeSkillResponse> {
  const res = await apiRequest<ApiResponse<EmployeeSkillResponse>>(`/employee-skills/${id}/reject`, {
    method: "PUT",
    body: JSON.stringify({ rejectionReason }),
  });
  return res.data;
}

/* ── Department Skill Matrix API ─────────────────────────── */

export async function getDepartmentSkillMatrix(
  orgUnitId: number
): Promise<DepartmentSkillMatrixResponse> {
  const res = await apiRequest<ApiResponse<DepartmentSkillMatrixResponse>>(
    `/skills/matrix?orgUnitId=${orgUnitId}`
  );
  return res.data;
}
