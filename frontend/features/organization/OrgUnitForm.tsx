"use client";

import { useMemo, useRef, type FormEvent, type KeyboardEvent } from "react";

import { FormField } from "@/components/ui/FormField";
import { OrgUnitCombobox, type OrgUnitOption } from "@/components/ui/OrgUnitCombobox";
import { ManagerCombobox, type ManagerOption } from "@/components/ui/ManagerCombobox";
import type { OrgUnitType } from "@/src/types/hrm";

export interface OrgUnitDraft {
  description: string;
  managerId: string;
  parentId: string;
  unitCode: string;
  unitName: string;
  unitType: OrgUnitType;
}

export type OrgUnitDraftErrors = Partial<Record<keyof OrgUnitDraft, string>>;

export function getSuitableManagerRoles(unitType: OrgUnitType): Set<string> {
  switch (unitType) {
    case "COMPANY":
      // Cấp 1 (Công ty): Tổng Giám đốc / Giám đốc điều hành -> VT-01 (Ban giám đốc) hoặc VT-06 (Quản trị viên)
      return new Set(["VT-01", "VT-06"]);
    case "CENTER":
      // Cấp 2 (Khối / Trung tâm): Giám đốc khối -> VT-03 (Quản lý nguồn lực)
      return new Set(["VT-03"]);
    case "DEPARTMENT":
      // Cấp 3 (Phòng ban): Trưởng phòng -> VT-03 (Quản lý nguồn lực) hoặc VT-05 (Nhân sự nếu là phòng HR)
      return new Set(["VT-03", "VT-05"]);
    case "TEAM":
      // Cấp 4 (Nhóm chuyên môn): Trưởng nhóm -> VT-03 (Quản lý nguồn lực) hoặc VT-02 (Quản lý dự án)
      return new Set(["VT-03", "VT-02"]);
    default:
      return new Set(["VT-01", "VT-03", "VT-05", "VT-06"]);
  }
}

export function getManagerRoleHint(unitType: OrgUnitType): string {
  switch (unitType) {
    case "COMPANY":
      return "Cấp 1 (Công ty): Gán Ban giám đốc (VT-01) - Tổng Giám đốc / Giám đốc điều hành";
    case "CENTER":
      return "Cấp 2 (Khối / Trung tâm): Gán Quản lý nguồn lực (VT-03) - Giám đốc khối";
    case "DEPARTMENT":
      return "Cấp 3 (Phòng ban): Gán Quản lý nguồn lực (VT-03) - Trưởng phòng";
    case "TEAM":
      return "Cấp 4 (Nhóm chuyên môn): Gán Quản lý nguồn lực (VT-03) - Trưởng nhóm hoặc Quản lý dự án (VT-02)";
    default:
      return "Chọn nhân sự có vai trò quản lý phù hợp";
  }
}

interface OrgUnitFormProps {
  errors: OrgUnitDraftErrors;
  formId: string;
  initialFocusRef?: (element: HTMLElement | null) => void;
  managerOptions: readonly ManagerOption[];
  mode: "create" | "edit";
  onChange: <Key extends keyof OrgUnitDraft>(key: Key, value: OrgUnitDraft[Key]) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  parentOptions: readonly OrgUnitOption[];
  value: OrgUnitDraft;
}

export function OrgUnitForm({
  errors,
  formId,
  initialFocusRef,
  managerOptions,
  mode,
  onChange,
  onSubmit,
  parentOptions,
  value,
}: OrgUnitFormProps) {
  const nameRef = useRef<HTMLInputElement>(null);
  const typeRef = useRef<HTMLSelectElement>(null);
  const parentRef = useRef<HTMLButtonElement>(null);
  const descriptionRef = useRef<HTMLTextAreaElement>(null);

  const suitableManagerOptions = useMemo(() => {
    const allowedRoles = getSuitableManagerRoles(value.unitType);
    return managerOptions.filter((opt) => allowedRoles.has(opt.roleCode));
  }, [managerOptions, value.unitType]);

  function advanceOnEnter(event: KeyboardEvent<HTMLElement>, nextElement: HTMLElement | null) {
    if (event.key === "Enter" && !event.nativeEvent.isComposing) {
      event.preventDefault();
      nextElement?.focus();
    }
  }

  function assignCodeFocus(element: HTMLInputElement | null) {
    initialFocusRef?.(element);
  }

  function assignNameFocus(element: HTMLInputElement | null) {
    nameRef.current = element;
    if (mode === "edit") {
      initialFocusRef?.(element);
    }
  }

  function handleTypeChange(nextType: OrgUnitType) {
    onChange("unitType", nextType);
    // If the currently selected manager is not allowed for the new unitType, clear selection
    if (value.managerId) {
      const currentMgr = managerOptions.find((m) => String(m.employeeId) === value.managerId);
      if (currentMgr && !getSuitableManagerRoles(nextType).has(currentMgr.roleCode)) {
        onChange("managerId", "");
      }
    }
  }

  return (
    <form className="form" id={formId} noValidate onSubmit={onSubmit}>
      <div className="form-grid form-grid--two">
        {mode === "create" ? (
          <FormField error={errors.unitCode} id="org-unit-code" label="Mã đơn vị">
            <input
              aria-describedby="org-unit-code-message"
              aria-invalid={Boolean(errors.unitCode)}
              aria-required="true"
              className="input"
              id="org-unit-code"
              maxLength={50}
              onChange={(event) => onChange("unitCode", event.target.value)}
              onKeyDown={(event) => advanceOnEnter(event, nameRef.current)}
              placeholder="vd. P-KYTHUAT"
              ref={assignCodeFocus}
              required
              value={value.unitCode}
            />
          </FormField>
        ) : (
          <div className="field-group">
            <span className="field-label">Mã đơn vị</span>
            <p className="readonly-value">{value.unitCode}</p>
            <p className="field-hint"> </p>
          </div>
        )}

        <FormField error={errors.unitName} id="org-unit-name" label="Tên đơn vị">
          <input
            aria-describedby="org-unit-name-message"
            aria-invalid={Boolean(errors.unitName)}
            aria-required="true"
            className="input"
            id="org-unit-name"
            maxLength={255}
            onChange={(event) => onChange("unitName", event.target.value)}
            onKeyDown={(event) => advanceOnEnter(event, typeRef.current)}
            ref={assignNameFocus}
            required
            value={value.unitName}
          />
        </FormField>
      </div>

      <div className="form-grid form-grid--two">
        <FormField error={errors.unitType} id="org-unit-type" label="Loại đơn vị">
          <select
            aria-describedby="org-unit-type-message"
            aria-invalid={Boolean(errors.unitType)}
            className="select"
            id="org-unit-type"
            onChange={(event) => handleTypeChange(event.target.value as OrgUnitType)}
            onKeyDown={(event) => advanceOnEnter(event, parentRef.current)}
            ref={typeRef}
            value={value.unitType}
          >
            <option value="COMPANY">Công ty</option>
            <option value="CENTER">Khối / Trung tâm</option>
            <option value="DEPARTMENT">Phòng ban</option>
            <option value="TEAM">Nhóm chuyên môn</option>
          </select>
        </FormField>

        <FormField error={errors.parentId} hint={mode === "create" ? "Để trống để tạo đơn vị gốc." : undefined} id="org-unit-parent" label="Đơn vị cha">
          <OrgUnitCombobox
            allowClear={mode === "create"}
            ariaDescribedBy="org-unit-parent-message"
            ariaInvalid={Boolean(errors.parentId)}
            id="org-unit-parent"
            onChange={(nextValue) => onChange("parentId", nextValue)}
            onEnter={() => descriptionRef.current?.focus()}
            onKeyboardSelect={() => descriptionRef.current?.focus()}
            options={parentOptions}
            placeholder={mode === "create" ? "Chọn đơn vị cha" : "Đơn vị gốc"}
            ref={parentRef}
            value={value.parentId}
          />
        </FormField>
      </div>

      <FormField
        error={errors.managerId}
        hint={getManagerRoleHint(value.unitType)}
        id="org-unit-manager"
        label="Người quản lý (Manager)"
      >
        <ManagerCombobox
          allowClear={true}
          ariaDescribedBy="org-unit-manager-message"
          ariaInvalid={Boolean(errors.managerId)}
          id="org-unit-manager"
          onChange={(nextValue) => onChange("managerId", nextValue)}
          options={suitableManagerOptions}
          placeholder="Chọn người quản lý đơn vị (Không bắt buộc)"
          value={value.managerId}
        />
      </FormField>

      <FormField error={errors.description} id="org-unit-description" label="Mô tả">
        <textarea
          aria-describedby="org-unit-description-message"
          aria-invalid={Boolean(errors.description)}
          className="textarea"
          id="org-unit-description"
          maxLength={2000}
          onChange={(event) => onChange("description", event.target.value)}
          placeholder="Mô tả ngắn về đơn vị"
          ref={descriptionRef}
          value={value.description}
        />
      </FormField>
    </form>
  );
}
