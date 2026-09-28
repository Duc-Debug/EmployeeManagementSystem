import { FormField } from "@/components/ui/FormField";
import { OrgUnitCombobox, type OrgUnitOption } from "@/components/ui/OrgUnitCombobox";
import { DEMO_ROLES } from "@/src/mocks/hrm";
import type { DataScope } from "@/src/types/hrm";
import { getDefaultDataScopeForRole } from "@/lib/role-data-scope";

export interface AuthorizationDraft {
  dataScope: DataScope;
  roleCode: string;
  scopeOrgUnitId: string;
  syncOrgScope?: boolean;
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
  const roleId = `${idPrefix}-role`;
  const dataScopeId = `${idPrefix}-data-scope`;
  const scopeOrgUnitId = `${idPrefix}-scope-org-unit`;

  function handleRoleChange(roleCode: string) {
    onChange("roleCode", roleCode);
    const dataScope = getDefaultDataScopeForRole(roleCode);
    if (dataScope) {
      onChange("dataScope", dataScope);
    }
    if (roleCode === "VT-03") {
      onChange("syncOrgScope", true);
    } else if (dataScope !== "ORGANIZATION_BRANCH") {
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
        <FormField error={errors.dataScope} hint="Tự động xác định theo vai trò." id={dataScopeId} label="Phạm vi dữ liệu">
          <select
            aria-describedby={`${dataScopeId}-message`}
            aria-invalid={Boolean(errors.dataScope)}
            className="select"
            disabled={true}
            id={dataScopeId}
            value={value.dataScope}
          >
            {dataScopeOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </FormField>
      </div>

      {value.roleCode === "VT-03" && (
        <div style={{ marginBottom: "0.75rem" }}>
          <label style={{ display: "inline-flex", alignItems: "center", gap: "0.5rem", cursor: "pointer", fontSize: "0.875rem", fontWeight: 500 }}>
            <input
              type="checkbox"
              checked={value.syncOrgScope !== false}
              onChange={(e) => {
                const checked = e.target.checked;
                onChange("syncOrgScope", checked);
                if (checked) {
                  onChange("scopeOrgUnitId", "");
                }
              }}
            />
            <span>Áp dụng phạm vi quản lý theo đơn vị trực thuộc</span>
          </label>
        </div>
      )}

      {value.dataScope === "ORGANIZATION_BRANCH" && value.syncOrgScope === false ? (
        <FormField error={errors.scopeOrgUnitId} hint="Chọn đơn vị tổ chức áp dụng." id={scopeOrgUnitId} label="Đơn vị tổ chức áp dụng">
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
