import { FormField } from "@/components/ui/FormField";
import { OrgUnitCombobox, type OrgUnitOption } from "@/components/ui/OrgUnitCombobox";
import { DEMO_ROLES } from "@/src/mocks/hrm";
import type { DataScope } from "@/src/types/hrm";

export interface AuthorizationDraft {
  dataScope: DataScope;
  roleCode: string;
  scopeOrgUnitId: string;
}

export type AuthorizationErrors = Partial<Record<keyof AuthorizationDraft, string>>;

interface AuthorizationFieldsProps {
  errors: AuthorizationErrors;
  idPrefix: string;
  initialRoleFocusRef?: (element: HTMLSelectElement | null) => void;
  onChange: <Key extends keyof AuthorizationDraft>(key: Key, value: AuthorizationDraft[Key]) => void;
  orgUnitOptions: readonly OrgUnitOption[];
  value: AuthorizationDraft;
}

const dataScopeOptions: ReadonlyArray<{ label: string; value: DataScope }> = [
  { label: "Toàn công ty", value: "COMPANY" },
  { label: "Theo đơn vị", value: "ORGANIZATION_BRANCH" },
  { label: "Cá nhân", value: "SELF" },
];

export function AuthorizationFields({ errors, idPrefix, initialRoleFocusRef, onChange, orgUnitOptions, value }: AuthorizationFieldsProps) {
  const isSystemAdmin = value.roleCode === "VT-06";
  const roleId = `${idPrefix}-role`;
  const dataScopeId = `${idPrefix}-data-scope`;
  const scopeOrgUnitId = `${idPrefix}-scope-org-unit`;

  function handleRoleChange(roleCode: string) {
    onChange("roleCode", roleCode);
    if (roleCode === "VT-06") {
      onChange("dataScope", "COMPANY");
      onChange("scopeOrgUnitId", "");
    }
  }

  return (
    <>
      <div className="form-grid form-grid--two">
        <FormField error={errors.roleCode} id={roleId} label="Vai trò">
          <select
            aria-describedby={`${roleId}-message`}
            aria-invalid={Boolean(errors.roleCode)}
            className="select"
            id={roleId}
            onChange={(event) => handleRoleChange(event.target.value)}
            ref={initialRoleFocusRef}
            value={value.roleCode}
          >
            <option value="">Chọn vai trò</option>
            {DEMO_ROLES.map((role) => <option key={role.code} value={role.code}>{role.code} · {role.name}</option>)}
          </select>
        </FormField>
        <FormField error={errors.dataScope} hint={isSystemAdmin ? "Quản trị viên áp dụng cho toàn công ty." : undefined} id={dataScopeId} label="Phạm vi dữ liệu">
          <select
            aria-describedby={`${dataScopeId}-message`}
            aria-invalid={Boolean(errors.dataScope)}
            className="select"
            disabled={isSystemAdmin}
            id={dataScopeId}
            onChange={(event) => {
              const dataScope = event.target.value as DataScope;
              onChange("dataScope", dataScope);
              if (dataScope !== "ORGANIZATION_BRANCH") {
                onChange("scopeOrgUnitId", "");
              }
            }}
            value={value.dataScope}
          >
            {dataScopeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </FormField>
      </div>

      {value.dataScope === "ORGANIZATION_BRANCH" ? (
        <FormField error={errors.scopeOrgUnitId} hint="Chọn đơn vị tổ chức áp dụng." id={scopeOrgUnitId} label="Đơn vị tổ chức">
          <OrgUnitCombobox
            ariaDescribedBy={`${scopeOrgUnitId}-message`}
            ariaInvalid={Boolean(errors.scopeOrgUnitId)}
            id={scopeOrgUnitId}
            onChange={(nextValue) => onChange("scopeOrgUnitId", nextValue)}
            options={orgUnitOptions}
            placeholder="Chọn đơn vị tổ chức"
            value={value.scopeOrgUnitId}
          />
        </FormField>
      ) : null}
    </>
  );
}
