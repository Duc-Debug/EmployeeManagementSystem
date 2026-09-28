"use client";

import { type FormEvent, type RefObject } from "react";
import { FormField } from "@/components/ui/FormField";

export interface SkillItemOption {
  category?: string;
  code: string;
  id: number;
  name: string;
}

export interface SkillDeclarationDraft {
  proficiencyLevel: number;
  skillId: string;
  yearsOfExperience: string;
}

export interface SkillDeclarationErrors {
  proficiencyLevel?: string;
  skillId?: string;
  yearsOfExperience?: string;
}

interface SkillDeclarationFormProps {
  errors: SkillDeclarationErrors;
  formId: string;
  initialFocusRef?: (element: HTMLElement | null) => void;
  onChange: <Key extends keyof SkillDeclarationDraft>(key: Key, value: SkillDeclarationDraft[Key]) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  skillCatalog: SkillItemOption[];
  submitRef?: RefObject<HTMLButtonElement | null>;
  value: SkillDeclarationDraft;
}

export function SkillDeclarationForm({
  errors,
  formId,
  initialFocusRef,
  onChange,
  onSubmit,
  skillCatalog,
  value,
}: SkillDeclarationFormProps) {
  return (
    <form className="form" id={formId} onSubmit={onSubmit}>
      {/* 1. Select Skill from Catalog */}
      <FormField error={errors.skillId} id="skillId" label="Chọn Kỹ năng chuyên môn (*)">
        <select
          className={`select ${errors.skillId ? "select--error" : ""}`}
          id="skillId"
          onChange={(e) => onChange("skillId", e.target.value)}
          ref={initialFocusRef}
          required
          value={value.skillId}
        >
          <option value="">-- Chọn kỹ năng từ danh mục --</option>
          {skillCatalog.map((skill) => (
            <option key={skill.id} value={String(skill.id)}>
              {skill.name}{skill.category ? ` · ${skill.category}` : ""}
            </option>
          ))}
        </select>
      </FormField>

      {/* 2. Proficiency Level Rating (1 -> 5 Stars / Level Buttons) */}
      <FormField error={errors.proficiencyLevel} id="proficiencyLevel" label="Mức độ thành thạo (*)">
        <div className="rating-select-group" style={{ display: "flex", gap: "0.5rem", marginTop: "0.25rem" }}>
          {[1, 2, 3, 4, 5].map((lvl) => {
            const labels: Record<number, string> = {
              1: "1 - Cơ bản",
              2: "2 - Khá",
              3: "3 - Thành thạo",
              4: "4 - Giỏi",
              5: "5 - Chuyên gia",
            };
            const isSelected = value.proficiencyLevel === lvl;
            return (
              <button
                key={lvl}
                className={`button ${isSelected ? "button--primary" : "button--secondary"}`}
                onClick={() => onChange("proficiencyLevel", lvl)}
                style={{
                  flex: 1,
                  padding: "0.5rem 0.25rem",
                  fontSize: "0.8125rem",
                  fontWeight: isSelected ? "bold" : "normal",
                  textAlign: "center",
                }}
                type="button"
              >
                {"★".repeat(lvl)}
                <div style={{ fontSize: "0.7rem", opacity: 0.85, marginTop: "0.125rem" }}>
                  {labels[lvl]}
                </div>
              </button>
            );
          })}
        </div>
      </FormField>

      {/* 3. Years of Experience */}
      <FormField error={errors.yearsOfExperience} id="yearsOfExperience" label="Số năm kinh nghiệm (*)">
        <div style={{ position: "relative", display: "flex", alignItems: "center" }}>
          <input
            className={`input ${errors.yearsOfExperience ? "input--error" : ""}`}
            id="yearsOfExperience"
            max="50"
            min="0"
            onChange={(e) => onChange("yearsOfExperience", e.target.value)}
            placeholder="Ví dụ: 3.5"
            required
            step="0.5"
            type="number"
            value={value.yearsOfExperience}
          />
          <span style={{ position: "absolute", right: "0.75rem", color: "var(--color-text-secondary, #6b7280)", fontSize: "0.875rem", pointerEvents: "none" }}>
            năm
          </span>
        </div>
      </FormField>
    </form>
  );
}
