"use client";

import { apiRequest } from "../api-client";

export interface PendingEmployeeSkill {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  orgUnitId: number;
  orgUnitName: string;
  skillId: number;
  skillCode: string;
  skillName: string;
  skillCategory: string;
  proficiencyLevel: number;
  yearsOfExperience: number;
  status: string;
  createdAt: string;
}

export interface PendingEmployeeSkillPage {
  content: PendingEmployeeSkill[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export async function getPendingEmployeeSkills(keyword = "", page = 0, size = 100): Promise<PendingEmployeeSkillPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (keyword.trim()) params.set("keyword", keyword.trim());
  return apiRequest<PendingEmployeeSkillPage>(`/employee-skills/pending?${params.toString()}`);
}

export async function approveEmployeeSkill(id: number): Promise<void> {
  await apiRequest(`/employee-skills/${id}/approve`, {
    method: "PUT",
    body: JSON.stringify({}),
  });
}
