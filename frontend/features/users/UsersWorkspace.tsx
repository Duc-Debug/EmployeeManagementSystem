"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { RoleBadge, ScopeBadge, StatusBadge } from "@/components/ui/Badge";
import { Dialog } from "@/components/ui/Dialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
import { flattenOrgTree } from "@/lib/organization";
import { DEMO_ROLES } from "@/src/mocks/hrm";
import type { OrgUnitTreeNode, User } from "@/src/types/hrm";
import { createUser, getUsers, toggleUserStatus, updateUserRole } from "@/lib/api/users";
import { getOrgTree } from "@/lib/api/org-units";
import { ApiError } from "@/lib/api-client";

import { type UserAccountDraft, type UserAccountErrors, UserAccountForm } from "@/features/users/UserAccountForm";

type UserEditorState = { mode: "create" } | { mode: "edit"; userId: number } | null;

const EMPTY_DRAFT: UserAccountDraft = {
  dataScope: "SELF",
  email: "",
  employeeCode: "",
  fullName: "",
  orgUnitId: "",
  password: "",
  roleCode: "",
  scopeOrgUnitId: "",
  status: "ACTIVE",
  username: "",
};

function toEditDraft(user: User): UserAccountDraft {
  return {
    ...EMPTY_DRAFT,
    dataScope: user.dataScope,
    email: user.email || `${user.username}@company.com`,
    employeeCode: user.employeeId ? `EMP-00${user.employeeId}` : "EMP-001",
    fullName: user.fullName || user.username || "",
    orgUnitId: user.orgUnitId ? String(user.orgUnitId) : "",
    roleCode: user.roleCode,
    scopeOrgUnitId: user.scopeOrgUnitId ? String(user.scopeOrgUnitId) : "",
    status: user.status,
    username: user.username,
  };
}

export function UsersWorkspace() {
  const [users, setUsers] = useState<User[]>([]);
  const [rawTree, setRawTree] = useState<OrgUnitTreeNode[]>([]);
  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [editor, setEditor] = useState<UserEditorState>(null);
  const [lockTarget, setLockTarget] = useState<User | null>(null);
  const [draft, setDraft] = useState<UserAccountDraft>(EMPTY_DRAFT);
  const [errors, setErrors] = useState<UserAccountErrors>({});
  const [announcement, setAnnouncement] = useState("");
  const editorFocusRef = useRef<HTMLElement>(null);
  const submitRef = useRef<HTMLButtonElement>(null);
  const lockCancelRef = useRef<HTMLButtonElement>(null);
  const setEditorFocus = useCallback((element: HTMLElement | null) => {
    editorFocusRef.current = element;
  }, []);

  const [fetchError, setFetchError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  useEffect(() => {
    let ignore = false;
    Promise.all([
      getUsers(0, 100),
      getOrgTree(),
    ])
      .then(([usersPage, tree]) => {
        if (!ignore) {
          setUsers(usersPage?.content || []);
          setRawTree(tree || []);
          setFetchError(null);
        }
      })
      .catch((err) => {
        if (!ignore) {
          console.error("Lỗi nạp dữ liệu từ Backend:", err);
          setFetchError(err instanceof Error ? err.message : "Không thể tải dữ liệu từ máy chủ.");
        }
      });

    return () => {
      ignore = true;
    };
  }, [reloadTick]);

  const orgUnits = useMemo(() => flattenOrgTree(rawTree), [rawTree]);
  const orgUnitOptions = useMemo(() => orgUnits.map((orgUnit) => ({
    depth: orgUnit.level,
    id: orgUnit.id,
    unitCode: orgUnit.unitCode,
    unitName: orgUnit.unitName,
    unitType: orgUnit.unitType,
  })), [orgUnits]);

  const editingUser = editor?.mode === "edit" ? users.find((user) => user.id === editor.userId) : undefined;
  
  // KPI Stats
  const stats = useMemo(() => {
    const total = users.length;
    const active = users.filter((user) => user.status === "ACTIVE").length;
    const locked = users.filter((user) => user.status === "LOCKED").length;
    const admins = users.filter((user) => user.roleCode === "VT-06").length;
    return { active, admins, locked, total };
  }, [users]);

  const filteredUsers = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("vi");
    return users.filter((user) => {
      const searchMatches = !normalizedQuery || [user.fullName || "", user.username || "", user.email ?? "", user.orgUnitName ?? ""]
        .some((value) => value.toLocaleLowerCase("vi").includes(normalizedQuery));
      const roleMatches = roleFilter === "ALL" || user.roleCode === roleFilter;
      const statusMatches = statusFilter === "ALL" || user.status === statusFilter;
      return searchMatches && roleMatches && statusMatches;
    });
  }, [query, roleFilter, statusFilter, users]);

  function openCreateDialog() {
    setDraft(EMPTY_DRAFT);
    setErrors({});
    setEditor({ mode: "create" });
  }

  function openEditDialog(user: User) {
    setDraft(toEditDraft(user));
    setErrors({});
    setEditor({ mode: "edit", userId: user.id });
  }

  function closeEditor() {
    setEditor(null);
    setErrors({});
    setDraft(EMPTY_DRAFT);
  }

  function updateDraft<Key extends keyof UserAccountDraft>(key: Key, value: UserAccountDraft[Key]) {
    setDraft((currentDraft) => ({ ...currentDraft, [key]: value }));
    setErrors((currentErrors) => ({ ...currentErrors, [key]: undefined }));
  }

  function validateDraft(): boolean {
    const nextErrors: UserAccountErrors = {};
    if (!editor) {
      return false;
    }

    if (!draft.fullName.trim()) nextErrors.fullName = "Họ tên là bắt buộc.";
    if (!draft.email.trim()) {
      nextErrors.email = "Email là bắt buộc.";
    } else if (!draft.email.includes("@")) {
      nextErrors.email = "Email không đúng định dạng.";
    }

    if (editor.mode === "create") {
      if (!draft.employeeCode.trim()) nextErrors.employeeCode = "Mã nhân viên là bắt buộc.";
      if (draft.username.trim().length < 3) nextErrors.username = "Tên đăng nhập cần có ít nhất 3 ký tự.";
      if (!draft.password || draft.password.length < 6) nextErrors.password = "Mật khẩu cần có ít nhất 6 ký tự.";
      if (!draft.orgUnitId) nextErrors.orgUnitId = "Hãy chọn đơn vị tổ chức.";
    }

    if (editor.mode === "edit") {
      if (draft.password && draft.password.length < 6) nextErrors.password = "Mật khẩu mới phải có ít nhất 6 ký tự.";
    }

    if (!draft.roleCode) nextErrors.roleCode = "Hãy chọn vai trò (Role).";
    if (draft.dataScope === "ORGANIZATION_BRANCH" && !draft.scopeOrgUnitId) {
      nextErrors.scopeOrgUnitId = "Hãy chọn đơn vị tổ chức áp dụng.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function saveUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editor || !validateDraft()) {
      return;
    }

    const selectedRole = DEMO_ROLES.find((role) => role.code === draft.roleCode);
    if (!selectedRole) {
      setErrors({ roleCode: "Hãy chọn role hợp lệ." });
      return;
    }

    const selectedOrgUnit = orgUnits.find((orgUnit) => orgUnit.id === Number(draft.orgUnitId));

    if (editor.mode === "create") {
      if (!selectedOrgUnit) {
        setErrors({ orgUnitId: "Hãy chọn đơn vị tổ chức hợp lệ." });
        return;
      }

      try {
        const created = await createUser({
          email: draft.email.trim() || undefined,
          employeeCode: draft.employeeCode.trim() || undefined,
          fullName: draft.fullName.trim(),
          orgUnitId: selectedOrgUnit.id,
          password: draft.password,
          roleCode: selectedRole.code,
          username: draft.username.trim(),
        });
        setUsers((currentUsers) => [created, ...currentUsers]);
        setAnnouncement(`Đã tạo tài khoản ${created.fullName}.`);
        closeEditor();
      } catch (err) {
        if (err instanceof ApiError) {
          setErrors({ username: err.message });
        }
      }
      return;
    }

    if (!editingUser) {
      return;
    }

    const scopeOrgUnitId = draft.dataScope === "ORGANIZATION_BRANCH" ? Number(draft.scopeOrgUnitId) : null;
    try {
      const updated = await updateUserRole(editingUser.id, {
        dataScope: selectedRole.code === "VT-06" ? "COMPANY" : draft.dataScope,
        roleCode: selectedRole.code,
        scopeOrgUnitId,
      });
      setUsers((currentUsers) => currentUsers.map((u) => u.id === editingUser.id ? updated : u));
      setAnnouncement(`Đã cập nhật tài khoản ${editingUser.fullName}.`);
      closeEditor();
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors({ roleCode: err.message });
      }
    }
  }

  function requestLock(user: User) {
    setLockTarget(user);
  }

  async function confirmLock() {
    if (!lockTarget) {
      return;
    }

    try {
      const updated = await toggleUserStatus(lockTarget.id, true);
      setUsers((currentUsers) => currentUsers.map((u) => u.id === lockTarget.id ? updated : u));
      setAnnouncement(`Đã khóa tài khoản ${lockTarget.fullName}.`);
    } catch {
      // ignore
    }
    setLockTarget(null);
  }

  async function unlockUser(user: User) {
    try {
      const updated = await toggleUserStatus(user.id, false);
      setUsers((currentUsers) => currentUsers.map((item) => (
        item.id === user.id ? updated : item
      )));
      setAnnouncement(`Đã mở khóa tài khoản ${user.fullName}.`);
    } catch {
      // ignore
    }
  }

  const editorMode = editor?.mode;
  const editorTitle = editorMode === "edit" ? `Chỉnh sửa tài khoản: ${editingUser?.fullName ?? ""}` : "Tạo tài khoản mới";

  return (
    <div className="workspace-stack">
      <PageHeader
        actions={
          <button className="button button--primary create-user-cta" onClick={openCreateDialog} type="button">
            <Icon name="plus" />
            <span>Tạo tài khoản</span>
          </button>
        }
        description="Quản trị danh sách người dùng, phân cấp đơn vị và phạm vi truy cập dữ liệu."
        title="Quản lý tài khoản nhân sự"
      />

      {fetchError && (
        <div className="notice notice--error" style={{ marginBottom: "1rem" }}>
          <Icon name="alert" />
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", width: "100%" }}>
            <span>{fetchError}</span>
            <button className="button button--secondary" onClick={() => setReloadTick((t) => t + 1)} type="button" style={{ padding: "0.25rem 0.75rem", fontSize: "0.875rem" }}>
              Thử lại
            </button>
          </div>
        </div>
      )}

      {announcement ? <p aria-live="polite" className="sr-only">{announcement}</p> : null}

      {/* KPI Stats Cards */}
      <section aria-label="Thống kê tài khoản" className="kpi-grid">
        <button
          className={`kpi-card ${statusFilter === "ALL" && roleFilter === "ALL" ? "is-active" : ""}`}
          onClick={() => {
            setStatusFilter("ALL");
            setRoleFilter("ALL");
          }}
          type="button"
        >
          <div className="kpi-card__header">
            <span className="kpi-card__label">Tổng tài khoản</span>
            <span className="kpi-card__icon kpi-card__icon--indigo">
              <Icon name="users" />
            </span>
          </div>
          <div className="kpi-card__val">{stats.total}</div>
          <div className="kpi-card__desc">Toàn hệ thống</div>
        </button>

        <button
          className={`kpi-card ${statusFilter === "ACTIVE" ? "is-active" : ""}`}
          onClick={() => setStatusFilter((prev) => (prev === "ACTIVE" ? "ALL" : "ACTIVE"))}
          type="button"
        >
          <div className="kpi-card__header">
            <span className="kpi-card__label">Đang hoạt động</span>
            <span className="kpi-card__icon kpi-card__icon--emerald">
              <Icon name="check" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--emerald">{stats.active}</div>
          <div className="kpi-card__desc">Sẵn sàng đăng nhập</div>
        </button>

        <button
          className={`kpi-card ${statusFilter === "LOCKED" ? "is-active" : ""}`}
          onClick={() => setStatusFilter((prev) => (prev === "LOCKED" ? "ALL" : "LOCKED"))}
          type="button"
        >
          <div className="kpi-card__header">
            <span className="kpi-card__label">Đã khóa</span>
            <span className="kpi-card__icon kpi-card__icon--rose">
              <Icon name="lock" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--rose">{stats.locked}</div>
          <div className="kpi-card__desc">Tạm ngưng quyền truy cập</div>
        </button>

        <button
          className={`kpi-card ${roleFilter === "VT-06" ? "is-active" : ""}`}
          onClick={() => setRoleFilter((prev) => (prev === "VT-06" ? "ALL" : "VT-06"))}
          type="button"
        >
          <div className="kpi-card__header">
            <span className="kpi-card__label">Quản trị viên (VT-06)</span>
            <span className="kpi-card__icon kpi-card__icon--purple">
              <Icon name="shield" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--purple">{stats.admins}</div>
          <div className="kpi-card__desc">DataScope: Toàn công ty</div>
        </button>
      </section>

      {/* Main Data Panel */}
      <section aria-labelledby="users-table-title" className="data-panel">
        <div className="data-panel__header">
          <div>
            <h2 id="users-table-title">Danh sách tài khoản nhân sự</h2>
            <p>Hiển thị {filteredUsers.length} trên tổng số {users.length} tài khoản</p>
          </div>
        </div>

        <div className="data-panel__body">
          <div className="filter-toolbar">
            <div className="select-field">
              <label className="sr-only" htmlFor="role-filter">Lọc theo role</label>
              <select className="select" id="role-filter" onChange={(event) => setRoleFilter(event.target.value)} value={roleFilter}>
                <option value="ALL">Tất cả role</option>
                {DEMO_ROLES.map((role) => <option key={role.code} value={role.code}>{role.code} · {role.name}</option>)}
              </select>
            </div>

            <div className="select-field">
              <label className="sr-only" htmlFor="status-filter">Lọc theo trạng thái</label>
              <select className="select" id="status-filter" onChange={(event) => setStatusFilter(event.target.value)} value={statusFilter}>
                <option value="ALL">Tất cả trạng thái</option>
                <option value="ACTIVE">Hoạt động</option>
                <option value="LOCKED">Đã khóa</option>
              </select>
            </div>

            {(query || roleFilter !== "ALL" || statusFilter !== "ALL") && (
              <button
                className="button button--secondary button--compact filter-reset-btn"
                onClick={() => {
                  setQuery("");
                  setRoleFilter("ALL");
                  setStatusFilter("ALL");
                }}
                type="button"
              >
                <Icon name="close" />
                <span>Đặt lại lọc</span>
              </button>
            )}

            {/* Search Box on the Right */}
            <div className="search-field">
              <Icon name="search" />
              <label className="sr-only" htmlFor="user-search">Tìm tài khoản</label>
              <input
                className="input"
                id="user-search"
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Tìm theo tên, email, username, mã NV..."
                type="search"
                value={query}
              />
              {query && (
                <button
                  aria-label="Xóa từ khóa tìm kiếm"
                  className="search-clear-btn"
                  onClick={() => setQuery("")}
                  type="button"
                >
                  <Icon name="close" />
                </button>
              )}
            </div>
          </div>

          {filteredUsers.length === 0 ? (
            <EmptyState
              action={
                <button
                  className="button button--secondary"
                  onClick={() => {
                    setQuery("");
                    setRoleFilter("ALL");
                    setStatusFilter("ALL");
                  }}
                  type="button"
                >
                  Xóa bộ lọc
                </button>
              }
              icon="search"
              message="Không có tài khoản nào khớp với điều kiện tìm kiếm hiện tại."
              title="Không tìm thấy tài khoản"
            />
          ) : (
            <>
              <div className="data-table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th scope="col">Tài khoản</th>
                      <th scope="col">Email</th>
                      <th scope="col">Vai trò (Role)</th>
                      <th scope="col">Đơn vị tổ chức</th>
                      <th scope="col">Phạm vi dữ liệu</th>
                      <th scope="col">Trạng thái</th>
                      <th scope="col" style={{ textAlign: "right", width: "8.5rem" }}>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.map((user) => (
                      <UserTableRow
                        key={user.id}
                        onEdit={openEditDialog}
                        onRequestLock={requestLock}
                        onUnlock={unlockUser}
                        user={user}
                      />
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="mobile-record-list">
                {filteredUsers.map((user) => (
                  <UserRecordCard
                    key={user.id}
                    onEdit={openEditDialog}
                    onRequestLock={requestLock}
                    onUnlock={unlockUser}
                    user={user}
                  />
                ))}
              </div>
            </>
          )}
        </div>
      </section>

      {/* Create / Edit User Dialog */}
      <Dialog
        className="dialog--user-form"
        description={editorMode === "edit" ? "Cập nhật thông tin nhân sự, đơn vị công tác và cấu hình phân quyền." : "Điền thông tin tài khoản và cấu hình phân quyền ban đầu."}
        footer={
          <>
            <button className="button button--secondary" onClick={closeEditor} type="button">
              Hủy
            </button>
            <button className="button button--primary" form="user-account-form" ref={submitRef} type="submit">
              {editorMode === "edit" ? "Lưu thay đổi" : "Tạo tài khoản"}
            </button>
          </>
        }
        initialFocusRef={editorFocusRef}
        onClose={closeEditor}
        open={Boolean(editor)}
        preventBackdropClose={true}
        title={editorTitle}
      >
        {editorMode ? (
          <UserAccountForm
            errors={errors}
            formId="user-account-form"
            identity={editingUser}
            initialFocusRef={setEditorFocus}
            mode={editorMode}
            onChange={updateDraft}
            onSubmit={saveUser}
            orgUnitOptions={orgUnitOptions}
            submitRef={submitRef}
            value={draft}
          />
        ) : null}
      </Dialog>

      {/* Lock User Confirmation Dialog */}
      <Dialog
        className="dialog--compact"
        description="Xác nhận trước khi khóa quyền truy cập của tài khoản này."
        footer={
          <>
            <button className="button button--secondary" onClick={() => setLockTarget(null)} ref={lockCancelRef} type="button">
              Hủy
            </button>
            <button className="button button--danger" onClick={confirmLock} type="button">
              Xác nhận khóa
            </button>
          </>
        }
        initialFocusRef={lockCancelRef}
        onClose={() => setLockTarget(null)}
        open={Boolean(lockTarget)}
        preventBackdropClose={true}
        title="Khóa tài khoản người dùng"
      >
        {lockTarget ? (
          <div className="dialog-confirmation">
            <div className="lock-warning-card">
              <div className="lock-warning-card__icon">
                <Icon name="alert" />
              </div>
              <div className="lock-warning-card__info">
                <strong>{lockTarget.fullName || lockTarget.username}</strong>
                <span>@{lockTarget.username} · {lockTarget.email ?? `${lockTarget.username}@company.com`}</span>
              </div>
            </div>
            <p className="dialog-confirmation__text">
              Tài khoản này sẽ bị thu hồi phiên làm việc và không thể đăng nhập vào hệ thống cho đến khi được mở khóa lại.
            </p>
          </div>
        ) : null}
      </Dialog>
    </div>
  );
}

interface UserActionProps {
  onEdit: (user: User) => void;
  onRequestLock: (user: User) => void;
  onUnlock: (user: User) => void;
  user: User;
}

function UserActions({ onEdit, onRequestLock, onUnlock, user }: UserActionProps) {
  const isActive = user.status === "ACTIVE";
  return (
    <div className="table-actions">
      <button className="table-action table-action--edit" onClick={() => onEdit(user)} title="Chỉnh sửa tài khoản" type="button">
        <Icon name="settings" />
        <span>Sửa</span>
      </button>
      <button
        className={`table-action ${isActive ? "table-action--danger" : "table-action--unlock"}`}
        onClick={() => (isActive ? onRequestLock(user) : onUnlock(user))}
        title={isActive ? "Khóa tài khoản" : "Mở khóa tài khoản"}
        type="button"
      >
        <Icon name={isActive ? "lock" : "unlock"} />
        <span>{isActive ? "Khóa" : "Mở"}</span>
      </button>
    </div>
  );
}

function UserTableRow({ onEdit, onRequestLock, onUnlock, user }: UserActionProps) {
  const displayName = user.fullName || user.username || "Người dùng";
  const displayEmail = user.email || `${user.username}@company.com`;
  return (
    <tr className="user-table-row">
      <td>
        <div className="table-person">
          <span aria-hidden="true" className="avatar avatar--medium avatar--gradient">
            {displayName.slice(0, 1).toUpperCase()}
          </span>
          <div className="table-person__copy">
            <strong>{displayName}</strong>
            <div className="table-person__meta">
              <span className="table-person__username">@{user.username}</span>
            </div>
          </div>
        </div>
      </td>
      <td>
        <span className="table-email">{displayEmail}</span>
      </td>
      <td>
        <RoleBadge code={user.roleCode} name={user.roleName} />
      </td>
      <td>
        <div className="table-org-cell">
          <Icon name="organization" />
          <span>{user.orgUnitName ?? "Chưa gán đơn vị"}</span>
        </div>
      </td>
      <td>
        <ScopeBadge scope={user.dataScope} />
      </td>
      <td>
        <StatusBadge status={user.status} />
      </td>
      <td style={{ textAlign: "right" }}>
        <UserActions onEdit={onEdit} onRequestLock={onRequestLock} onUnlock={onUnlock} user={user} />
      </td>
    </tr>
  );
}

function UserRecordCard({ onEdit, onRequestLock, onUnlock, user }: UserActionProps) {
  const displayName = user.fullName || user.username || "Người dùng";
  const displayEmail = user.email || `${user.username}@company.com`;
  return (
    <article className="record-card">
      <div className="record-card__header">
        <div className="table-person">
          <span aria-hidden="true" className="avatar avatar--small avatar--gradient">
            {displayName.slice(0, 1).toUpperCase()}
          </span>
          <div className="table-person__copy">
            <strong>{displayName}</strong>
            <div className="table-person__meta">
              <span className="table-person__username">@{user.username}</span>
            </div>
          </div>
        </div>
        <StatusBadge status={user.status} />
      </div>
      <dl className="record-card__facts">
        <div><dt>Email</dt><dd>{displayEmail}</dd></div>
        <div><dt>Vai trò</dt><dd><RoleBadge code={user.roleCode} name={user.roleName} /></dd></div>
        <div><dt>Phạm vi</dt><dd><ScopeBadge scope={user.dataScope} /></dd></div>
        <div><dt>Đơn vị</dt><dd>{user.orgUnitName ?? "Chưa gán đơn vị"}</dd></div>
      </dl>
      <div className="record-card__footer">
        <UserActions onEdit={onEdit} onRequestLock={onRequestLock} onUnlock={onUnlock} user={user} />
      </div>
    </article>
  );
}
