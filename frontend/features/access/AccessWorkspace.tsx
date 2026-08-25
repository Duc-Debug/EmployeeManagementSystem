"use client";

import { useMemo, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { StatusBadge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { flattenOrgTree } from "@/lib/organization";
import { DEMO_ORG_UNIT_TREE, DEMO_ROLES, DEMO_USERS } from "@/src/mocks/hrm";
import type { User } from "@/src/types/hrm";

import { AuthorizationFields, type AuthorizationDraft, type AuthorizationErrors } from "@/features/users/AuthorizationFields";

function toAccessDraft(user: User): AuthorizationDraft {
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
  const [draft, setDraft] = useState<AuthorizationDraft>(() => toAccessDraft(DEMO_USERS[0]));
  const [errors, setErrors] = useState<AuthorizationErrors>({});
  const [announcement, setAnnouncement] = useState("");
  const orgUnits = useMemo(() => flattenOrgTree(DEMO_ORG_UNIT_TREE), []);

  function selectUser(userId: number) {
    const user = users.find((item) => item.id === userId);
    if (!user) {
      return;
    }

    setSelectedUserId(userId);
    setDraft(toAccessDraft(user));
    setErrors({});
  }

  function saveAccess(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedUser) {
      return;
    }

    if (draft.dataScope === "ORGANIZATION_BRANCH" && !draft.scopeOrgUnitId) {
      setErrors({ scopeOrgUnitId: "Hãy chọn đơn vị tổ chức áp dụng." });
      return;
    }

    const selectedRole = DEMO_ROLES.find((role) => role.code === draft.roleCode);
    const selectedOrgUnit = orgUnits.find((orgUnit) => orgUnit.id === Number(draft.scopeOrgUnitId));
    if (!selectedRole) {
      setErrors({ roleCode: "Hãy chọn role hợp lệ." });
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
    setAnnouncement(`Đã cập nhật quyền truy cập cho ${selectedUser.fullName}.`);
    setErrors({});
  }

  return (
    <div className="workspace-stack">
      <PageHeader
        description="Thiết lập vai trò và phạm vi truy cập cho từng tài khoản."
        title="Phân quyền"
      />

      {announcement ? <p aria-live="polite" className="sr-only">{announcement}</p> : null}

      <div className="access-layout">
        <section aria-labelledby="access-user-list-title" className="data-panel access-user-list">
          <div className="data-panel__header">
            <div>
              <h2 id="access-user-list-title">Chọn tài khoản</h2>
              <p>{users.length} tài khoản</p>
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
                <p>Chỉnh sửa vai trò và phạm vi truy cập</p>
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

              <form className="form access-form" noValidate onSubmit={saveAccess}>
                <AuthorizationFields
                  errors={errors}
                  idPrefix="access"
                  onChange={(key, value) => {
                    setDraft((currentDraft) => ({ ...currentDraft, [key]: value }));
                    setErrors((currentErrors) => ({ ...currentErrors, [key]: undefined }));
                  }}
                  orgUnitOptions={orgUnits.map((orgUnit) => ({ depth: orgUnit.level, id: orgUnit.id, unitCode: orgUnit.unitCode, unitName: orgUnit.unitName }))}
                  value={draft}
                />

                <div className="form-actions">
                  <button className="button button--primary" type="submit">Lưu quyền truy cập</button>
                </div>
              </form>

            </div>
          </section>
        ) : <EmptyState icon="user" message="Chọn một tài khoản để xem và chỉnh sửa quyền truy cập." title="Chưa chọn tài khoản" />}
      </div>
    </div>
  );
}
