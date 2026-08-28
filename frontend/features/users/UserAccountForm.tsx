import { useState, type FormEvent, type RefObject } from "react";
 
import { FormField } from "@/components/ui/FormField";
import { Icon } from "@/components/ui/Icon";
import { OrgUnitCombobox, type OrgUnitOption } from "@/components/ui/OrgUnitCombobox";
import { DEMO_ROLES } from "@/src/mocks/hrm";
import type { DataScope, User, UserStatus } from "@/src/types/hrm";

import { type AuthorizationDraft, type AuthorizationErrors } from "@/features/users/AuthorizationFields";

export interface UserAccountDraft extends AuthorizationDraft {
  email: string;
  employeeCode: string;
  fullName: string;
  orgUnitId: string;
  password?: string;
  status: UserStatus;
  username: string;
}

export type UserAccountErrors = AuthorizationErrors &
  Partial<Record<"email" | "employeeCode" | "fullName" | "orgUnitId" | "password" | "status" | "username", string>>;

interface UserAccountFormProps {
  errors: UserAccountErrors;
  formId: string;
  identity?: Pick<User, "fullName" | "orgUnitName" | "username">;
  initialFocusRef?: (element: HTMLElement | null) => void;
  mode: "create" | "edit";
  onChange: <Key extends keyof UserAccountDraft>(key: Key, value: UserAccountDraft[Key]) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  orgUnitOptions: readonly OrgUnitOption[];
  submitRef?: RefObject<HTMLButtonElement | null>;
  value: UserAccountDraft;
}

const dataScopeOptions: ReadonlyArray<{ label: string; value: DataScope }> = [
  { label: "Toàn công ty", value: "COMPANY" },
  { label: "Theo đơn vị", value: "ORGANIZATION_BRANCH" },
  { label: "Cá nhân", value: "SELF" },
];

export function UserAccountForm({
  errors,
  formId,
  initialFocusRef,
  mode,
  onChange,
  onSubmit,
  orgUnitOptions,
  value,
}: UserAccountFormProps) {
  const [showPassword, setShowPassword] = useState(false);
  const isSystemAdmin = value.roleCode === "VT-06";

  function handleRoleChange(roleCode: string) {
    onChange("roleCode", roleCode);
    if (roleCode === "VT-06") {
      onChange("dataScope", "COMPANY");
      onChange("scopeOrgUnitId", "");
    }
  }

  return (
    <form className="form form--user-editor" id={formId} noValidate onSubmit={onSubmit}>
      {/* Block 1: Thông tin cơ bản */}
      <div className="form-section-title">
        <span>1. Thông tin cá nhân & Tài khoản</span>
      </div>

      <div className="form-grid form-grid--two">
        <FormField error={errors.fullName} id="user-full-name" label="Họ và tên">
          <input
            aria-invalid={Boolean(errors.fullName)}
            className="input"
            id="user-full-name"
            onChange={(event) => onChange("fullName", event.target.value)}
            placeholder="vd. Nguyễn Văn A"
            ref={(el) => {
              if (mode === "create") initialFocusRef?.(el);
            }}
            required
            type="text"
            value={value.fullName}
          />
        </FormField>

        <FormField error={errors.email} id="user-email" label="Email liên hệ / Khôi phục">
          <input
            aria-invalid={Boolean(errors.email)}
            className="input"
            id="user-email"
            onChange={(event) => onChange("email", event.target.value)}
            placeholder="vd. van.a@company.com"
            required
            type="email"
            value={value.email}
          />
        </FormField>
      </div>

      <div className="form-grid form-grid--two">
        <FormField
          error={errors.employeeCode}
          hint={mode === "create" ? "Tự động sinh mã chuẩn EMP-xxx hoặc có thể tự nhập" : undefined}
          id="user-employee-code"
          label="Mã nhân viên"
        >
          <input
            aria-invalid={Boolean(errors.employeeCode)}
            className="input"
            id="user-employee-code"
            onChange={(event) => onChange("employeeCode", event.target.value)}
            placeholder="vd. EMP-001"
            required
            type="text"
            value={value.employeeCode}
          />
        </FormField>

        <FormField error={errors.username} id="user-username" label="Tên đăng nhập">
          <input
            aria-invalid={Boolean(errors.username)}
            autoComplete="username"
            className="input"
            disabled={mode === "edit"}
            id="user-username"
            onChange={(event) => onChange("username", event.target.value)}
            placeholder="vd. van.a"
            required
            type="text"
            value={value.username}
          />
        </FormField>
      </div>

      <div className="form-grid form-grid--two">
        <FormField
          error={errors.password}
          hint={mode === "edit" ? "Để trống nếu giữ nguyên mật khẩu cũ" : undefined}
          id="user-password"
          label={mode === "create" ? "Mật khẩu khởi tạo" : "Mật khẩu mới"}
        >
          <div className="input-action-wrapper">
            <input
              aria-invalid={Boolean(errors.password)}
              autoComplete="new-password"
              className="input"
              id="user-password"
              onChange={(event) => onChange("password", event.target.value)}
              placeholder={mode === "create" ? "Tối thiểu 6 ký tự" : "Nhập nếu muốn đổi"}
              required={mode === "create"}
              type={showPassword ? "text" : "password"}
              value={value.password ?? ""}
            />
            <button
              type="button"
              className="input-action-btn"
              onClick={() => setShowPassword(!showPassword)}
              title={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
              aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            >
              <Icon name={showPassword ? "eyeOff" : "eye"} />
            </button>
          </div>
        </FormField>

        <FormField error={errors.orgUnitId} id="user-org-unit" label="Đơn vị tổ chức trực thuộc">
          <OrgUnitCombobox
            ariaInvalid={Boolean(errors.orgUnitId)}
            id="user-org-unit"
            onChange={(nextValue) => onChange("orgUnitId", nextValue)}
            options={orgUnitOptions}
            placeholder="Chọn phòng ban / đơn vị công tác"
            value={value.orgUnitId}
          />
        </FormField>
      </div>

      {/* Block 2: Phân quyền & Phạm vi dữ liệu */}
      <div className="form-section-title" style={{ marginTop: "0.5rem" }}>
        <span>2. Phân quyền & Phạm vi dữ liệu</span>
      </div>

      <div className="form-grid form-grid--two">
        <FormField error={errors.roleCode} id="user-role" label="Vai trò">
          <select
            aria-invalid={Boolean(errors.roleCode)}
            className="select"
            id="user-role"
            onChange={(event) => handleRoleChange(event.target.value)}
            value={value.roleCode}
          >
            <option value="">Chọn vai trò</option>
            {DEMO_ROLES.map((role) => (
              <option key={role.code} value={role.code}>
                {role.code} · {role.name}
              </option>
            ))}
          </select>
        </FormField>

        <FormField
          error={errors.dataScope}
          hint={isSystemAdmin ? "Quản trị viên (VT-06) tự động áp dụng toàn công ty." : undefined}
          id="user-data-scope"
          label="Phạm vi dữ liệu"
        >
          <select
            aria-invalid={Boolean(errors.dataScope)}
            className="select"
            disabled={isSystemAdmin}
            id="user-data-scope"
            onChange={(event) => {
              const dataScope = event.target.value as DataScope;
              onChange("dataScope", dataScope);
              if (dataScope !== "ORGANIZATION_BRANCH") {
                onChange("scopeOrgUnitId", "");
              }
            }}
            value={value.dataScope}
          >
            {dataScopeOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </FormField>
      </div>

      <div className="form-grid form-grid--two">
        <FormField error={errors.status} id="user-status" label="Trạng thái hoạt động">
          <select
            className="select"
            id="user-status"
            onChange={(event) => onChange("status", event.target.value as UserStatus)}
            value={value.status}
          >
            <option value="ACTIVE">Hoạt động</option>
            <option value="LOCKED">Đã khóa</option>
          </select>
        </FormField>

        {value.dataScope === "ORGANIZATION_BRANCH" ? (
          <FormField
            error={errors.scopeOrgUnitId}
            hint="Dữ liệu sẽ được giới hạn trong cây đơn vị đã chọn."
            id="user-scope-org-unit"
            label="Đơn vị tổ chức áp dụng"
          >
            <OrgUnitCombobox
              ariaInvalid={Boolean(errors.scopeOrgUnitId)}
              id="user-scope-org-unit"
              onChange={(nextValue) => onChange("scopeOrgUnitId", nextValue)}
              options={orgUnitOptions}
              placeholder="Chọn đơn vị áp dụng"
              value={value.scopeOrgUnitId}
            />
          </FormField>
        ) : null}
      </div>
    </form>
  );
}
