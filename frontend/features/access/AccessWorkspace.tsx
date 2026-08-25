"use client";

import { useMemo, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { ScopeBadge, StatusBadge, getScopeLabel } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
import { flattenOrgTree } from "@/lib/organization";
import { DEMO_ORG_UNIT_TREE, DEMO_READ_ONLY_EFFECTIVE_PERMISSIONS, DEMO_ROLES, DEMO_USERS } from "@/src/mocks/hrm";
import type { DataScope, User } from "@/src/types/hrm";

interface AccessDraft {
  dataScope: DataScope;
  roleCode: string;
  scopeOrgUnitId: string;
}

const scopeOptions: ReadonlyArray<{ description: string; scope: DataScope; title: string }> = [
  { scope: "COMPANY", title: "Toàn công ty", description: "Phạm vi áp dụng trên toàn bộ dữ liệu theo quyết định của backend." },
  { scope: "ORGANIZATION_BRANCH", title: "Theo đơn vị", description: "Chỉ định một Org Unit làm phạm vi truy cập." },
  { scope: "SELF", title: "Cá nhân", description: "Giới hạn vào dữ liệu thuộc chính người dùng." },
];

function toAccessDraft(user: User): AccessDraft {
  return {
    dataScope: user.dataScope,
    roleCode: user.roleCode,
    scopeOrgUnitId: user.scopeOrgUnitId ? String(user.scopeOrgUnitId) : "",
  };
}

export function AccessWorkspace() {
  const [users, setUsers] = useState<User[]>(() => DEMO_USERS.map((user) => ({ ...user })));
  const [selectedUserId, setSelectedUserId] = useState<number>(DEMO_USERS[0]?.id ?? 0);
  const selectedUser = users.find((user) => user.id === selectedUserId) ?? users[0];
  const [draft, setDraft] = useState<AccessDraft>(() => toAccessDraft(DEMO_USERS[0]));
  const [error, setError] = useState("");
  const [announcement, setAnnouncement] = useState("");
  const orgUnits = useMemo(() => flattenOrgTree(DEMO_ORG_UNIT_TREE), []);

  function selectUser(userId: number) {
    const user = users.find((item) => item.id === userId);
    if (!user) {
      return;
    }

    setSelectedUserId(userId);
    setDraft(toAccessDraft(user));
    setError("");
  }

  function saveAccess(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedUser) {
      return;
    }

    if (draft.dataScope === "ORGANIZATION_BRANCH" && !draft.scopeOrgUnitId) {
      setError("Phạm vi theo đơn vị cần một Org Unit được chọn.");
      return;
    }

    const selectedRole = DEMO_ROLES.find((role) => role.code === draft.roleCode);
    const selectedOrgUnit = orgUnits.find((orgUnit) => orgUnit.id === Number(draft.scopeOrgUnitId));
    if (!selectedRole) {
      setError("Role đã chọn không hợp lệ.");
      return;
    }

    const scopeOrgUnitId = draft.dataScope === "ORGANIZATION_BRANCH" ? selectedOrgUnit?.id ?? null : null;
    setUsers((currentUsers) => currentUsers.map((user) => (
      user.id === selectedUser.id
        ? {
          ...user,
          dataScope: draft.dataScope,
          roleCode: selectedRole.code,
          roleName: selectedRole.name,
          scopeOrgUnitId,
        }
        : user
    )));
    setAnnouncement(`Đã cập nhật quyền truy cập minh họa cho ${selectedUser.fullName}.`);
    setError("");
  }

  return (
    <div className="workspace-stack">
      <PageHeader
        description="Role trả lời “được làm gì”; Data Scope trả lời “được thao tác trên dữ liệu nào”. Hai lớp này được hiển thị và chỉnh sửa riêng."
        title="Phân quyền"
      />

      {announcement ? <p aria-live="polite" className="sr-only">{announcement}</p> : null}

      <div className="access-layout">
        <section aria-labelledby="access-user-list-title" className="data-panel access-user-list">
          <div className="data-panel__header">
            <div>
              <h2 id="access-user-list-title">Chọn tài khoản</h2>
              <p>Dữ liệu minh họa</p>
            </div>
          </div>
          <div className="access-user-list__items">
            {users.map((user) => (
              <button
                aria-current={user.id === selectedUser?.id ? "true" : undefined}
                className={user.id === selectedUser?.id ? "access-user-card is-selected" : "access-user-card"}
                key={user.id}
                onClick={() => selectUser(user.id)}
                type="button"
              >
                <span aria-hidden="true" className="avatar avatar--small">{user.fullName.slice(0, 1)}</span>
                <span className="access-user-card__copy">
                  <strong>{user.fullName}</strong>
                  <small>{user.username}</small>
                </span>
                <StatusBadge status={user.status} />
              </button>
            ))}
          </div>
        </section>

        {selectedUser ? (
          <section aria-labelledby="access-editor-title" className="data-panel access-editor">
            <div className="data-panel__header">
              <div>
                <h2 id="access-editor-title">Quyền truy cập</h2>
                <p>Cập nhật độc lập role và phạm vi dữ liệu</p>
              </div>
              <StatusBadge status={selectedUser.status} />
            </div>
            <div className="data-panel__body workspace-stack">
              <div className="access-subject">
                <span aria-hidden="true" className="avatar avatar--large">{selectedUser.fullName.slice(0, 1)}</span>
                <div>
                  <h3>{selectedUser.fullName}</h3>
                  <p>{selectedUser.username} · {selectedUser.orgUnitName ?? "Chưa gán đơn vị"}</p>
                </div>
              </div>

              <div className="notice">
                <Icon name="access" />
                <span>Role là vị trí được gán cho tài khoản. Data Scope là giới hạn dữ liệu; UI không coi hai khái niệm này là một.</span>
              </div>

              <form className="form access-form" noValidate onSubmit={saveAccess}>
                <div className="field-group">
                  <label htmlFor="access-role">Role</label>
                  <select className="select" id="access-role" onChange={(event) => setDraft((currentDraft) => ({ ...currentDraft, roleCode: event.target.value }))} value={draft.roleCode}>
                    {DEMO_ROLES.map((role) => <option key={role.code} value={role.code}>{role.code} · {role.name}</option>)}
                  </select>
                  <p className="field-hint">Role code sẽ được lấy từ backend khi có catalog endpoint.</p>
                </div>

                <fieldset className="scope-fieldset">
                  <legend>Data Scope</legend>
                  <div className="scope-options">
                    {scopeOptions.map((option) => {
                      const checked = draft.dataScope === option.scope;
                      return (
                        <label className={checked ? "scope-option is-checked" : "scope-option"} key={option.scope}>
                          <input
                            checked={checked}
                            name="data-scope"
                            onChange={() => setDraft((currentDraft) => ({ ...currentDraft, dataScope: option.scope, scopeOrgUnitId: option.scope === "ORGANIZATION_BRANCH" ? currentDraft.scopeOrgUnitId : "" }))}
                            type="radio"
                            value={option.scope}
                          />
                          <span className="scope-option__copy">
                            <strong>{option.title}</strong>
                            <small>{option.description}</small>
                          </span>
                        </label>
                      );
                    })}
                  </div>
                </fieldset>

                {draft.dataScope === "ORGANIZATION_BRANCH" ? (
                  <div className="field-group">
                    <label htmlFor="access-org-unit">Org Unit được truy cập</label>
                    <select aria-invalid={Boolean(error)} className="select" id="access-org-unit" onChange={(event) => setDraft((currentDraft) => ({ ...currentDraft, scopeOrgUnitId: event.target.value }))} value={draft.scopeOrgUnitId}>
                      <option value="">Chọn một Org Unit</option>
                      {orgUnits.map((orgUnit) => <option key={orgUnit.id} value={orgUnit.id}>{orgUnit.unitCode} · {orgUnit.unitName}</option>)}
                    </select>
                    <p className={error ? "field-error" : "field-hint"}>{error || "Backend hiện nhận đúng một scopeOrgUnitId cho phạm vi theo đơn vị."}</p>
                  </div>
                ) : null}

                <div className="form-actions">
                  <button className="button button--primary" type="submit">Lưu quyền truy cập</button>
                </div>
              </form>

              <section aria-labelledby="effective-permissions-title" className="effective-permissions">
                <div className="detail-section__heading">
                  <div>
                    <h3 id="effective-permissions-title">Quyền hiệu lực</h3>
                    <p>Read-only · nguồn backend</p>
                  </div>
                  <ScopeBadge scope={draft.dataScope} />
                </div>
                <EmptyState
                  icon="document"
                  message={DEMO_READ_ONLY_EFFECTIVE_PERMISSIONS.note}
                  title="Chưa có endpoint quyền hiệu lực"
                />
                <p className="field-hint">Phạm vi đang chọn: {getScopeLabel(draft.dataScope)}. Không hiển thị permission mapping khi backend chưa trả dữ liệu chính thức.</p>
              </section>
            </div>
          </section>
        ) : <EmptyState icon="user" message="Chọn một tài khoản để xem và chỉnh sửa quyền truy cập minh họa." title="Chưa chọn tài khoản" />}
      </div>
    </div>
  );
}
