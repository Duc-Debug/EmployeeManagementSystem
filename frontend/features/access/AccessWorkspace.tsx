"use client";

import { useMemo, useState } from "react";

import { PageHeader } from "@/components/layout/PageHeader";
import { RoleBadge, ScopeBadge, StatusBadge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { Icon } from "@/components/ui/Icon";
import { flattenOrgTree } from "@/lib/organization";
import { DEMO_ORG_UNIT_TREE, DEMO_ROLES, DEMO_USERS } from "@/src/mocks/hrm";
import type { RoleCode, User } from "@/src/types/hrm";

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
  const [searchQuery, setSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<string>("ALL");
  const [toastMessage, setToastMessage] = useState<string>("");

  const orgUnits = useMemo(() => flattenOrgTree(DEMO_ORG_UNIT_TREE), []);
  const orgUnitOptions = useMemo(() => orgUnits.map((u) => ({
    depth: u.level,
    id: u.id,
    unitCode: u.unitCode,
    unitName: u.unitName,
    unitType: u.unitType,
  })), [orgUnits]);

  // Selected Role details
  const activeRoleDetails = useMemo(() => {
    return DEMO_ROLES.find((r) => r.code === draft.roleCode);
  }, [draft.roleCode]);

  // Filtered Users List
  const filteredUsers = useMemo(() => {
    const q = searchQuery.trim().toLocaleLowerCase("vi");
    return users.filter((user) => {
      const matchQuery = !q || [user.fullName, user.username, user.email, user.roleName, user.roleCode]
        .filter(Boolean)
        .some((val) => val!.toLocaleLowerCase("vi").includes(q));

      const matchRole = roleFilter === "ALL" || user.roleCode === roleFilter;

      return matchQuery && matchRole;
    });
  }, [roleFilter, searchQuery, users]);

  // KPI Metrics
  const stats = useMemo(() => {
    const total = users.length;
    const adminExec = users.filter((u) => u.roleCode === "VT-01" || u.roleCode === "VT-06").length;
    const managers = users.filter((u) => u.roleCode === "VT-02" || u.roleCode === "VT-03").length;
    const specialists = users.filter((u) => u.roleCode === "VT-04" || u.roleCode === "VT-05").length;
    return { adminExec, managers, specialists, total };
  }, [users]);

  function selectUser(userId: number) {
    const user = users.find((item) => item.id === userId);
    if (!user) {
      return;
    }

    setSelectedUserId(userId);
    setDraft(toAccessDraft(user));
    setErrors({});
  }

  function showToast(msg: string) {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(""), 4000);
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
          roleCode: selectedRole.code as RoleCode,
          roleName: selectedRole.name,
          scopeOrgUnitId,
        }
        : user
    )));
    showToast(`Đã cập nhật vai trò ${selectedRole.name} cho ${selectedUser.fullName}.`);
    setErrors({});
  }

  return (
    <div className="workspace-stack">
      <PageHeader
        description="Thiết lập vai trò nghiệp vụ (VT-01 → VT-06) và phạm vi dữ liệu (Data Scope) cho từng tài khoản."
        title="Phân quyền hệ thống"
      />

      {toastMessage && (
        <div className="access-toast" role="alert">
          <Icon name="check" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* KPI Stats Cards */}
      <section aria-label="Thống kê phân quyền" className="kpi-grid">
        <div className="kpi-card is-active">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Tổng tài khoản</span>
            <span className="kpi-card__icon kpi-card__icon--indigo">
              <Icon name="users" />
            </span>
          </div>
          <div className="kpi-card__val">{stats.total}</div>
          <div className="kpi-card__desc">Toàn hệ thống</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Quản trị & Giám đốc</span>
            <span className="kpi-card__icon kpi-card__icon--purple">
              <Icon name="shield" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--purple">{stats.adminExec}</div>
          <div className="kpi-card__desc">VT-01, VT-06</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Quản lý Dự án & Nguồn lực</span>
            <span className="kpi-card__icon kpi-card__icon--emerald">
              <Icon name="branch" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--emerald">{stats.managers}</div>
          <div className="kpi-card__desc">VT-02, VT-03</div>
        </div>

        <div className="kpi-card">
          <div className="kpi-card__header">
            <span className="kpi-card__label">Nhân sự & Chuyên môn</span>
            <span className="kpi-card__icon kpi-card__icon--rose">
              <Icon name="user" />
            </span>
          </div>
          <div className="kpi-card__val kpi-card__val--rose">{stats.specialists}</div>
          <div className="kpi-card__desc">VT-04, VT-05</div>
        </div>
      </section>

      {/* Main Access Layout */}
      <div className="access-layout">
        {/* Left: User Selection List */}
        <section aria-labelledby="access-user-list-title" className="data-panel access-user-list-panel">
          <div className="data-panel__header">
            <div>
              <h2 id="access-user-list-title">Danh sách tài khoản</h2>
              <p>{filteredUsers.length} / {users.length} tài khoản</p>
            </div>
          </div>

          <div className="data-panel__body access-user-list__body">
            {/* Search Box */}
            <div className="search-field">
              <Icon name="search" />
              <input
                aria-label="Tìm tài khoản phân quyền"
                className="input"
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Tìm theo tên, email, vai trò..."
                type="search"
                value={searchQuery}
              />
              {searchQuery && (
                <button aria-label="Xóa tìm kiếm" className="search-clear-btn" onClick={() => setSearchQuery("")} type="button">
                  <Icon name="close" />
                </button>
              )}
            </div>

            {/* Role Filter Chips */}
            <div className="access-role-chips">
              <button
                className={`access-chip ${roleFilter === "ALL" ? "is-active" : ""}`}
                onClick={() => setRoleFilter("ALL")}
                type="button"
              >
                Tất cả ({users.length})
              </button>
              {DEMO_ROLES.map((r) => {
                const count = users.filter((u) => u.roleCode === r.code).length;
                if (count === 0) return null;
                return (
                  <button
                    className={`access-chip ${roleFilter === r.code ? "is-active" : ""}`}
                    key={r.code}
                    onClick={() => setRoleFilter(r.code)}
                    type="button"
                  >
                    {r.code} ({count})
                  </button>
                );
              })}
            </div>

            {/* Users Scroll List */}
            <div className="access-user-list__items">
              {filteredUsers.map((user) => {
                const isSelected = user.id === selectedUser?.id;
                return (
                  <button
                    aria-current={isSelected ? "true" : undefined}
                    className={`access-user-card ${isSelected ? "is-selected" : ""}`}
                    key={user.id}
                    onClick={() => selectUser(user.id)}
                    type="button"
                  >
                    <span aria-hidden="true" className="avatar avatar--small avatar--gradient">
                      {user.fullName.slice(0, 1)}
                    </span>
                    <div className="access-user-card__copy">
                      <div className="access-user-card__top">
                        <strong>{user.fullName}</strong>
                        <StatusBadge status={user.status} />
                      </div>
                      <small>@{user.username} {user.email ? `· ${user.email}` : ""}</small>
                      <div className="access-user-card__badges">
                        <RoleBadge code={user.roleCode} name={user.roleName} />
                        <ScopeBadge scope={user.dataScope} />
                      </div>
                    </div>
                  </button>
                );
              })}

              {filteredUsers.length === 0 && (
                <EmptyState
                  action={<button className="button button--secondary" onClick={() => { setSearchQuery(""); setRoleFilter("ALL"); }} type="button">Xóa bộ lọc</button>}
                  icon="search"
                  message="Không tìm thấy tài khoản phù hợp với điều kiện lọc."
                  title="Không có kết quả"
                />
              )}
            </div>
          </div>
        </section>

        {/* Right: Permissions Editor & Matrix */}
        {selectedUser ? (
          <section aria-labelledby="access-editor-title" className="data-panel access-editor-panel">
            {/* User Profile Header */}
            <div className="access-editor__hero">
              <div className="access-editor__hero-main">
                <span aria-hidden="true" className="avatar avatar--large avatar--gradient">
                  {selectedUser.fullName.slice(0, 1)}
                </span>
                <div className="access-editor__hero-info">
                  <div className="access-editor__title-row">
                    <h2 id="access-editor-title">{selectedUser.fullName}</h2>
                    <StatusBadge status={selectedUser.status} />
                  </div>
                  <div className="access-editor__meta-row">
                    <span>@{selectedUser.username}</span>
                    {selectedUser.email && <span>· {selectedUser.email}</span>}
                    <span>· {selectedUser.orgUnitName ?? "Đơn vị gốc"}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="data-panel__body access-editor__body">
              {/* Form Config */}
              <form className="form access-form" noValidate onSubmit={saveAccess}>
                <div className="access-section-title">
                  <Icon name="shield" />
                  <span>Cấu hình Vai trò & Phạm vi truy cập</span>
                </div>

                <AuthorizationFields
                  errors={errors}
                  idPrefix="access"
                  onChange={(key, value) => {
                    setDraft((currentDraft) => ({ ...currentDraft, [key]: value }));
                    setErrors((currentErrors) => ({ ...currentErrors, [key]: undefined }));
                  }}
                  orgUnitOptions={orgUnitOptions}
                  value={draft}
                />

                <div className="form-actions access-form-actions">
                  <button className="button button--primary" type="submit">
                    <Icon name="check" />
                    <span>Lưu quyền truy cập</span>
                  </button>
                </div>
              </form>

              {/* Permissions Matrix Detail Card for Selected Role */}
              {activeRoleDetails && (
                <div className="role-spec-card">
                  <div className="role-spec-card__header">
                    <div className="role-spec-card__title">
                      <Icon name="access" />
                      <span>Ma trận thẩm quyền: {activeRoleDetails.code} · {activeRoleDetails.name}</span>
                    </div>
                  </div>

                  <div className="role-spec-grid">
                    <div className="role-spec-item">
                      <span className="role-spec-item__label">🎯 Mục tiêu vai trò</span>
                      <p className="role-spec-item__val">{activeRoleDetails.goal}</p>
                    </div>

                    <div className="role-spec-item">
                      <span className="role-spec-item__label">🛡️ Thẩm quyền chức năng</span>
                      <p className="role-spec-item__val">{activeRoleDetails.permissions}</p>
                    </div>

                    <div className="role-spec-item">
                      <span className="role-spec-item__label">📊 Dữ liệu được phép xem</span>
                      <p className="role-spec-item__val">{activeRoleDetails.scopeData}</p>
                    </div>

                    <div className="role-spec-item role-spec-item--warning">
                      <span className="role-spec-item__label">⛔ Giới hạn không được phép</span>
                      <p className="role-spec-item__val">{activeRoleDetails.limitations}</p>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </section>
        ) : (
          <EmptyState icon="user" message="Chọn một tài khoản từ danh sách bên trái để cấu hình phân quyền." title="Chưa chọn tài khoản" />
        )}
      </div>
    </div>
  );
}
