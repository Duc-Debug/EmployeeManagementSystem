"use client";

import { apiRequest } from "../api-client";

export interface Skill {
  category?: string;
  code: string;
  description?: string;
  id: number;
  name: string;
}

export interface EmployeeSkill {
  createdAt?: string;
  employeeId: number;
  id: number;
  proficiencyLevel: number;
  skillCategory?: string;
  skillCode?: string;
  skillId: number;
  skillName?: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  yearsOfExperience: number;
}

export interface DeclareSkillPayload {
  proficiencyLevel: number;
  skillId: number;
  yearsOfExperience: number;
}

/**
 * 1. API Lấy danh mục kỹ năng chuẩn trong hệ thống (Cho Dropdown Select)
 * Endpoint: GET /api/v1/skills/catalog (hoặc mock catalog)
 */
export async function getSkillCatalog(): Promise<Skill[]> {
  try {
    return await apiRequest<Skill[]>("/skills/catalog", {
      method: "GET",
    });
  } catch (err) {
    // Trả về danh mục kỹ năng mặc định nếu endpoint catalog chưa bật
    return [
      { id: 1, code: "JAVA", name: "Java", category: "Backend" },
      { id: 2, code: "REACT", name: "React.js", category: "Frontend" },
      { id: 3, code: "SPRING", name: "Spring Boot", category: "Backend" },
      { id: 4, code: "MYSQL", name: "MySQL Database", category: "Database" },
      { id: 5, code: "PYTHON", name: "Python", category: "Backend" },
      { id: 6, code: "DOCKER", name: "Docker & DevOps", category: "DevOps" },
    ];
  }
}

/**
 * 2. API Lấy danh sách kỹ năng cá nhân của nhân viên đang đăng nhập
 * Endpoint: GET /api/v1/employees/me/skills
 */
export async function getMySkills(): Promise<EmployeeSkill[]> {
  try {
    return await apiRequest<EmployeeSkill[]>("/employees/me/skills", {
      method: "GET",
    });
  } catch (err) {
    return [];
  }
}

/**
 * 3. API Khai báo kỹ năng mới
 * Endpoint: POST /api/v1/employees/me/skills
 */
export async function declareSkill(payload: DeclareSkillPayload): Promise<EmployeeSkill> {
  return await apiRequest<EmployeeSkill>("/employees/me/skills", {
    body: JSON.stringify(payload),
    method: "POST",
  });
}
