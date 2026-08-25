"use client";

import { useRef, type FormEvent, type RefObject } from "react";

import { FormField } from "@/components/ui/FormField";
import { OrgUnitCombobox, type OrgUnitOption } from "@/components/ui/OrgUnitCombobox";
import { DEMO_ROLES } from "@/src/mocks/hrm";
import type { User } from "@/src/types/hrm";

import { AuthorizationFields, type AuthorizationDraft, type AuthorizationErrors } from "@/features/users/AuthorizationFields";

export interface UserAccountDraft extends AuthorizationDraft {
  employeeCode: string;
  fullName: string;
  orgUnitId: string;
  password: string;
  username: string;
}

export type UserAccountErrors = AuthorizationErrors & Partial<Record<"employeeCode" | "fullName" | "orgUnitId" | "password" | "username", string>>;

interface UserAccountFormProps {
  errors: UserAccountErrors;
  formId: string;
  identity?: Pick<User, "fullName" | "orgUnitName" | "username">;
  initialFocusRef?: (element: HTMLElement | null) => void;
  mode: "create" | "edit";
  onChange: (key: keyof UserAccountDraft, value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  orgUnitOptions: readonly OrgUnitOption[];
  submitRef?: RefObject<HTMLButtonElement | null>;
  value: UserAccountDraft;
}

export function UserAccountForm({
  errors,
  formId,
  identity,
  initialFocusRef,
  mode,
  onChange,
  onSubmit,
  orgUnitOptions,
  submitRef,
  value,
}: UserAccountFormProps) {
  const employeeCodeRef = useRef<HTMLInputElement>(null);
  const usernameRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);
  const roleRef = useRef<HTMLSelectElement>(null);
  const orgUnitRef = useRef<HTMLButtonElement>(null);

  function advanceOnEnter(event: React.KeyboardEvent<HTMLElement>, nextElement: HTMLElement | null) {
    if (event.key === "Enter" && !event.nativeEvent.isComposing) {
      event.preventDefault();
      nextElement?.focus();
    }
  }

  function assignCreateFocus(element: HTMLInputElement | null) {
    initialFocusRef?.(element);
  }

  if (mode === "edit") {
    return (
      <form className="form" id={formId} noValidate onSubmit={onSubmit}>
        <div className="account-summary">
          <strong>{identity?.fullName}</strong>
          <span>{identity?.username} · {identity?.orgUnitName ?? "Chưa gán đơn vị"}</span>
        </div>
        <AuthorizationFields
          errors={errors}
          idPrefix="edit-user"
          initialRoleFocusRef={(element) => initialFocusRef?.(element)}
          onChange={(key, nextValue) => onChange(key, nextValue)}
          orgUnitOptions={orgUnitOptions}
          value={value}
        />
      </form>
    );
  }

  return (
    <form className="form" id={formId} noValidate onSubmit={onSubmit}>
      <div className="form-grid form-grid--two">
        <FormField error={errors.fullName} id="create-user-full-name" label="Họ tên">
          <input
            aria-describedby="create-user-full-name-message"
            aria-invalid={Boolean(errors.fullName)}
            aria-required="true"
            className="input"
            id="create-user-full-name"
            onChange={(event) => onChange("fullName", event.target.value)}
            onKeyDown={(event) => advanceOnEnter(event, employeeCodeRef.current)}
            ref={assignCreateFocus}
            required
            value={value.fullName}
          />
        </FormField>
        <FormField error={errors.employeeCode} id="create-user-employee-code" label="Mã nhân viên">
          <input
            aria-describedby="create-user-employee-code-message"
            aria-invalid={Boolean(errors.employeeCode)}
            aria-required="true"
            className="input"
            id="create-user-employee-code"
            onChange={(event) => onChange("employeeCode", event.target.value)}
            onKeyDown={(event) => advanceOnEnter(event, usernameRef.current)}
            placeholder="vd. EMP-001"
            ref={employeeCodeRef}
            required
            value={value.employeeCode}
          />
        </FormField>
      </div>

      <div className="form-grid form-grid--two">
        <FormField error={errors.username} id="create-user-username" label="Tên đăng nhập">
          <input
            aria-describedby="create-user-username-message"
            aria-invalid={Boolean(errors.username)}
            aria-required="true"
            autoComplete="username"
            className="input"
            id="create-user-username"
            onChange={(event) => onChange("username", event.target.value)}
            onKeyDown={(event) => advanceOnEnter(event, passwordRef.current)}
            ref={usernameRef}
            required
            value={value.username}
          />
        </FormField>
        <FormField error={errors.password} id="create-user-password" label="Mật khẩu">
          <input
            aria-describedby="create-user-password-message"
            aria-invalid={Boolean(errors.password)}
            aria-required="true"
            autoComplete="new-password"
            className="input"
            id="create-user-password"
            onChange={(event) => onChange("password", event.target.value)}
            onKeyDown={(event) => advanceOnEnter(event, roleRef.current)}
            ref={passwordRef}
            required
            type="password"
            value={value.password}
          />
        </FormField>
      </div>

      <div className="form-grid form-grid--two">
        <FormField error={errors.roleCode} id="create-user-role" label="Role">
          <select
            aria-describedby="create-user-role-message"
            aria-invalid={Boolean(errors.roleCode)}
            aria-required="true"
            className="select"
            id="create-user-role"
            onChange={(event) => onChange("roleCode", event.target.value)}
            onKeyDown={(event) => advanceOnEnter(event, orgUnitRef.current)}
            ref={roleRef}
            required
            value={value.roleCode}
          >
            <option value="">Chọn role</option>
            {DEMO_ROLES.map((role) => <option key={role.code} value={role.code}>{role.code} · {role.name}</option>)}
          </select>
        </FormField>
        <FormField error={errors.orgUnitId} id="create-user-org-unit" label="Đơn vị tổ chức">
          <OrgUnitCombobox
            ariaDescribedBy="create-user-org-unit-message"
            ariaInvalid={Boolean(errors.orgUnitId)}
            id="create-user-org-unit"
            onChange={(nextValue) => onChange("orgUnitId", nextValue)}
            onEnter={() => submitRef?.current?.focus()}
            onKeyboardSelect={() => submitRef?.current?.focus()}
            options={orgUnitOptions}
            placeholder="Chọn đơn vị tổ chức"
            ref={orgUnitRef}
            value={value.orgUnitId}
          />
        </FormField>
      </div>
    </form>
  );
}
