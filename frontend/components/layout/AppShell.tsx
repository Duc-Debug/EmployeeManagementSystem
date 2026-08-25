"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from "react";

import { Dialog } from "@/components/ui/Dialog";
import { FormField } from "@/components/ui/FormField";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Logo } from "@/components/ui/Logo";
import { clearDemoSession, readDemoSession, saveDemoSession, type DemoSession } from "@/lib/demo-session";
import { DEMO_USERS } from "@/src/mocks/hrm";

interface NavItem {
  badge?: string;
  href: string;
  icon: IconName;
  label: string;
}

interface NavSection {
  items: ReadonlyArray<NavItem>;
  title: string;
}

const navSections: ReadonlyArray<NavSection> = [
  {
    title: "Phân hệ nghiệp vụ",
    items: [
      { href: "/users", icon: "users", label: "Tài khoản nhân sự" },
      { href: "/organization", icon: "organization", label: "Sơ đồ cây tổ chức" },
      { href: "/access", icon: "access", label: "Phân quyền hệ thống" },
    ],
  },
];

interface NotificationItem {
  desc: string;
  icon: IconName;
  id: string;
  time: string;
  title: string;
  unread: boolean;
}

const INITIAL_NOTIFICATIONS: NotificationItem[] = [
  {
    desc: "Tài khoản minh.anh đã được cấu hình DataScope: Toàn công ty.",
    icon: "shield",
    id: "notif-1",
    time: "5 phút trước",
    title: "Phân quyền cập nhật",
    unread: true,
  },
  {
    desc: "Cấu trúc 4 cấp đơn vị tổ chức đã được đồng bộ thành công.",
    icon: "organization",
    id: "notif-2",
    time: "25 phút trước",
    title: "Cơ cấu tổ chức",
    unread: true,
  },
  {
    desc: "Hệ thống NexusHRM đã cập nhật giao diện Modern SaaS mới.",
    icon: "sparkles",
    id: "notif-3",
    time: "2 giờ trước",
    title: "Nâng cấp giao diện",
    unread: false,
  },
];

export function AppShell({ children }: Readonly<{ children: ReactNode }>) {
  const pathname = usePathname();
  const router = useRouter();
  const [isNavigationOpen, setIsNavigationOpen] = useState(false);
  const [isDesktopNavigation, setIsDesktopNavigation] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [isTopbarUserMenuOpen, setIsTopbarUserMenuOpen] = useState(false);
  const [isSidebarUserMenuOpen, setIsSidebarUserMenuOpen] = useState(false);
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>(INITIAL_NOTIFICATIONS);

  // Change password form state
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [changePasswordSuccess, setChangePasswordSuccess] = useState(false);
  const [changePasswordError, setChangePasswordError] = useState("");

  const menuTriggerRef = useRef<HTMLButtonElement>(null);
  const firstNavigationLinkRef = useRef<HTMLAnchorElement>(null);
  const notifDropdownRef = useRef<HTMLDivElement>(null);
  const topbarUserDropdownRef = useRef<HTMLDivElement>(null);
  const sidebarUserDropdownRef = useRef<HTMLDivElement>(null);

  const [user, setUser] = useState<DemoSession>(() => {
    if (typeof window === "undefined") {
      return DEFAULT_USER_SESSION;
    }
    return readDemoSession();
  });

  useLayoutEffect(() => {
    const desktopQuery = window.matchMedia("(min-width: 60rem)");
    const syncNavigationMode = () => {
      setIsDesktopNavigation(desktopQuery.matches);
      if (desktopQuery.matches) {
        setIsNavigationOpen(false);
      }
    };

    syncNavigationMode();
    desktopQuery.addEventListener("change", syncNavigationMode);
    return () => desktopQuery.removeEventListener("change", syncNavigationMode);
  }, []);

  useLayoutEffect(() => {
    if (!isNavigationOpen || isDesktopNavigation) {
      return;
    }

    const frame = window.requestAnimationFrame(() => firstNavigationLinkRef.current?.focus());
    return () => window.cancelAnimationFrame(frame);
  }, [isDesktopNavigation, isNavigationOpen]);

  // Handle clicking outside to close popups
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      const target = event.target as Node;
      if (notifDropdownRef.current && !notifDropdownRef.current.contains(target)) {
        setIsNotificationOpen(false);
      }
      if (topbarUserDropdownRef.current && !topbarUserDropdownRef.current.contains(target)) {
        setIsTopbarUserMenuOpen(false);
      }
      if (sidebarUserDropdownRef.current && !sidebarUserDropdownRef.current.contains(target)) {
        setIsSidebarUserMenuOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setIsNotificationOpen(false);
        setIsTopbarUserMenuOpen(false);
        setIsSidebarUserMenuOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, []);

  function closeNavigation(restoreFocus = true) {
    setIsNavigationOpen(false);
    if (restoreFocus && !isDesktopNavigation) {
      window.requestAnimationFrame(() => menuTriggerRef.current?.focus());
    }
  }

  function handleLogout() {
    clearDemoSession();
    router.push("/login");
  }

  function handleSwitchUser(demoUsername: string) {
    const selected = DEMO_USERS.find((u) => u.username === demoUsername);
    if (selected) {
      const newSession: DemoSession = {
        fullName: selected.fullName,
        roleCode: selected.roleCode,
        roleName: selected.roleName,
        username: selected.username,
      };
      saveDemoSession(newSession);
      setUser(newSession);
      setIsTopbarUserMenuOpen(false);
      setIsSidebarUserMenuOpen(false);
      window.location.reload();
    }
  }

  function handleMarkAllAsRead() {
    setNotifications((prev) => prev.map((n) => ({ ...n, unread: false })));
  }

  function handleNotificationClick(id: string) {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, unread: false } : n)),
    );
  }

  function handleChangePasswordSubmit(e: React.FormEvent) {
    e.preventDefault();
    setChangePasswordError("");
    if (!oldPassword) {
      setChangePasswordError("Vui lòng nhập mật khẩu hiện tại.");
      return;
    }
    if (newPassword.length < 6) {
      setChangePasswordError("Mật khẩu mới phải có tối thiểu 6 ký tự.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setChangePasswordError("Mật khẩu xác nhận không trùng khớp.");
      return;
    }

    setChangePasswordSuccess(true);
    setTimeout(() => {
      setIsChangePasswordOpen(false);
      setChangePasswordSuccess(false);
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    }, 1500);
  }

  const unreadCount = notifications.filter((n) => n.unread).length;

  return (
    <div className={`app-shell ${isCollapsed ? "app-shell--collapsed" : ""}`}>
      {isNavigationOpen ? (
        <button
          aria-label="Đóng menu điều hướng"
          className="side-nav-backdrop is-visible"
          onClick={() => closeNavigation()}
          type="button"
        />
      ) : null}
      <aside
        aria-hidden={isDesktopNavigation || isNavigationOpen ? undefined : true}
        className={`side-nav ${isCollapsed ? "side-nav--collapsed" : ""} ${isNavigationOpen ? "is-open" : ""}`}
        inert={isDesktopNavigation || isNavigationOpen ? undefined : true}
        onKeyDown={(event) => {
          if (event.key === "Escape") {
            closeNavigation();
          }
        }}
      >
        <div className="side-nav__header">
          <div className="side-nav__brand">
            <Logo size={34} theme="light" variant={isCollapsed ? "mark" : "full"} />
          </div>
          <button
            aria-label={isCollapsed ? "Mở rộng thanh điều hướng" : "Thu gọn thanh điều hướng"}
            className="side-nav__toggle-btn"
            onClick={() => setIsCollapsed(!isCollapsed)}
            title={isCollapsed ? "Mở rộng thanh điều hướng" : "Thu gọn thanh điều hướng"}
            type="button"
          >
            <Icon name="menu" />
          </button>
        </div>

        <nav aria-label="Điều hướng chính" className="side-nav__menu">
          {navSections.map((section, sIdx) => (
            <div className="side-nav__section" key={section.title}>
              <span className="side-nav__section-title">{section.title}</span>
              <div className="side-nav__links">
                {section.items.map((item, itemIdx) => {
                  const isCurrent = pathname === item.href;
                  return (
                    <Link
                      aria-current={isCurrent ? "page" : undefined}
                      className={isCurrent ? "side-nav__link is-current" : "side-nav__link"}
                      href={item.href}
                      key={item.href}
                      onClick={() => closeNavigation(false)}
                      ref={sIdx === 0 && itemIdx === 0 ? firstNavigationLinkRef : undefined}
                      title={item.label}
                    >
                      <span className="side-nav__link-icon">
                        <Icon name={item.icon} />
                      </span>
                      <span className="side-nav__link-label">{item.label}</span>
                      {item.badge ? (
                        <span className="side-nav__link-badge">{item.badge}</span>
                      ) : null}
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* Sidebar Footer with interactive user dropdown */}
        <div className="side-nav__footer dropdown-container sidebar-user-dropdown-container" ref={sidebarUserDropdownRef}>
          <button
            className={`session-card session-card--interactive ${isSidebarUserMenuOpen ? "is-active" : ""}`}
            onClick={() => {
              setIsSidebarUserMenuOpen((prev) => !prev);
              setIsTopbarUserMenuOpen(false);
              setIsNotificationOpen(false);
            }}
            title={`${user.fullName} (${user.roleName}) - Nhấp để xem tùy chọn`}
            type="button"
          >
            <div className="session-card__avatar">
              <span aria-hidden="true" className="avatar avatar--small">{user.fullName.slice(0, 1)}</span>
              <span className="session-card__status-dot" />
            </div>
            <div className="session-card__info">
              <strong>{user.fullName}</strong>
              <div className="session-card__role">
                <span className="role-chip">{user.roleName}</span>
              </div>
            </div>
          </button>

          {isSidebarUserMenuOpen && (
            <div className="dropdown-menu user-dropdown sidebar-user-dropdown">
              <div className="user-dropdown__header">
                <div className="user-dropdown__avatar">
                  <span className="avatar avatar--large">{user.fullName.slice(0, 1)}</span>
                  <span className="user-dropdown__online" />
                </div>
                <div className="user-dropdown__details">
                  <strong>{user.fullName}</strong>
                  <span className="user-dropdown__username">@{user.username}</span>
                  <span className="role-chip">{user.roleName} ({user.roleCode})</span>
                </div>
              </div>

              <div className="user-dropdown__section">
                <span className="user-dropdown__section-title">Chuyển tài khoản thử nghiệm:</span>
                <div className="user-dropdown__quick-users">
                  {DEMO_USERS.map((demo) => (
                    <button
                      className={`quick-user-item ${demo.username === user.username ? "is-selected" : ""}`}
                      key={demo.id}
                      onClick={() => handleSwitchUser(demo.username)}
                      type="button"
                    >
                      <span className="avatar avatar--xs">{demo.fullName.slice(0, 1)}</span>
                      <div className="quick-user-item__info">
                        <strong>{demo.fullName}</strong>
                        <span>{demo.roleName}</span>
                      </div>
                      {demo.username === user.username && <Icon name="check" />}
                    </button>
                  ))}
                </div>
              </div>

              <div className="user-dropdown__menu-items">
                <button
                  className="user-dropdown__item"
                  onClick={() => {
                    setIsSidebarUserMenuOpen(false);
                    setIsChangePasswordOpen(true);
                  }}
                  type="button"
                >
                  <Icon name="lock" />
                  <span>Đổi mật khẩu</span>
                </button>
                <button
                  className="user-dropdown__item user-dropdown__item--logout"
                  onClick={handleLogout}
                  type="button"
                >
                  <Icon name="logout" />
                  <span>Đăng xuất</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </aside>

      <div
        aria-hidden={isNavigationOpen && !isDesktopNavigation ? true : undefined}
        className="dashboard-frame"
        inert={isNavigationOpen && !isDesktopNavigation ? true : undefined}
      >
        <header className="dashboard-topbar">
          <div className="dashboard-topbar__left">
            <button
              aria-expanded={isNavigationOpen}
              aria-label="Mở menu điều hướng"
              className="icon-button mobile-menu-btn"
              onClick={() => setIsNavigationOpen(true)}
              ref={menuTriggerRef}
              type="button"
            >
              <Icon name="menu" />
            </button>
            <div className="topbar-search">
              <Icon name="search" />
              <input
                aria-label="Tìm kiếm nhanh"
                className="topbar-search__input"
                placeholder="Tìm kiếm nhanh nhân sự, đơn vị, quyền hạn... (Ctrl + K)"
                readOnly
                type="text"
              />
            </div>
          </div>

          <div className="dashboard-topbar__right">
            {/* Notification Dropdown Container */}
            <div className="dropdown-container" ref={notifDropdownRef}>
              <button
                aria-expanded={isNotificationOpen}
                aria-haspopup="true"
                aria-label="Xem thông báo"
                className={`icon-button notification-btn ${isNotificationOpen ? "is-active" : ""}`}
                onClick={() => {
                  setIsNotificationOpen((prev) => !prev);
                  setIsTopbarUserMenuOpen(false);
                  setIsSidebarUserMenuOpen(false);
                }}
                title={unreadCount > 0 ? `Bạn có ${unreadCount} thông báo mới` : "Không có thông báo mới"}
                type="button"
              >
                <Icon name="bell" />
                {unreadCount > 0 && <span className="notification-indicator is-unread" />}
              </button>

              {isNotificationOpen && (
                <div className="dropdown-menu notification-dropdown">
                  <div className="dropdown-header">
                    <div className="dropdown-header__title">
                      <strong>Thông báo hệ thống</strong>
                      {unreadCount > 0 && <span className="badge-count">{unreadCount} mới</span>}
                    </div>
                    {unreadCount > 0 && (
                      <button
                        className="dropdown-action-btn"
                        onClick={handleMarkAllAsRead}
                        type="button"
                      >
                        Đã đọc tất cả
                      </button>
                    )}
                  </div>

                  <div className="notification-list">
                    {notifications.length === 0 ? (
                      <div className="notification-empty">Không có thông báo nào.</div>
                    ) : (
                      notifications.map((notif) => (
                        <button
                          className={`notification-item ${notif.unread ? "is-unread" : ""}`}
                          key={notif.id}
                          onClick={() => handleNotificationClick(notif.id)}
                          type="button"
                        >
                          <span className="notification-item__icon">
                            <Icon name={notif.icon} />
                          </span>
                          <div className="notification-item__content">
                            <div className="notification-item__title">
                              <strong>{notif.title}</strong>
                              {notif.unread && <span className="unread-dot" />}
                            </div>
                            <p>{notif.desc}</p>
                            <time>{notif.time}</time>
                          </div>
                        </button>
                      ))
                    )}
                  </div>

                  <div className="dropdown-footer">
                    <span>Hệ thống NexusHRM v2.4</span>
                  </div>
                </div>
              )}
            </div>

            <div className="topbar-divider" />

            {/* Topbar User Profile Dropdown Container */}
            <div className="dropdown-container" ref={topbarUserDropdownRef}>
              <button
                aria-expanded={isTopbarUserMenuOpen}
                aria-haspopup="true"
                aria-label="Tùy chọn tài khoản"
                className={`topbar-user topbar-user--interactive ${isTopbarUserMenuOpen ? "is-active" : ""}`}
                onClick={() => {
                  setIsTopbarUserMenuOpen((prev) => !prev);
                  setIsNotificationOpen(false);
                  setIsSidebarUserMenuOpen(false);
                }}
                type="button"
              >
                <span aria-hidden="true" className="avatar avatar--small">{user.fullName.slice(0, 1)}</span>
                <div className="topbar-user__info">
                  <strong>{user.fullName}</strong>
                  <span className="topbar-user__role">{user.roleName}</span>
                </div>
              </button>

              {isTopbarUserMenuOpen && (
                <div className="dropdown-menu user-dropdown">
                  <div className="user-dropdown__header">
                    <div className="user-dropdown__avatar">
                      <span className="avatar avatar--large">{user.fullName.slice(0, 1)}</span>
                      <span className="user-dropdown__online" />
                    </div>
                    <div className="user-dropdown__details">
                      <strong>{user.fullName}</strong>
                      <span className="user-dropdown__username">@{user.username}</span>
                      <span className="role-chip">{user.roleName} ({user.roleCode})</span>
                    </div>
                  </div>

                  <div className="user-dropdown__section">
                    <span className="user-dropdown__section-title">Chuyển tài khoản thử nghiệm:</span>
                    <div className="user-dropdown__quick-users">
                      {DEMO_USERS.map((demo) => (
                        <button
                          className={`quick-user-item ${demo.username === user.username ? "is-selected" : ""}`}
                          key={demo.id}
                          onClick={() => handleSwitchUser(demo.username)}
                          type="button"
                        >
                          <span className="avatar avatar--xs">{demo.fullName.slice(0, 1)}</span>
                          <div className="quick-user-item__info">
                            <strong>{demo.fullName}</strong>
                            <span>{demo.roleName}</span>
                          </div>
                          {demo.username === user.username && <Icon name="check" />}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="user-dropdown__menu-items">
                    <button
                      className="user-dropdown__item"
                      onClick={() => {
                        setIsTopbarUserMenuOpen(false);
                        setIsChangePasswordOpen(true);
                      }}
                      type="button"
                    >
                      <Icon name="lock" />
                      <span>Đổi mật khẩu</span>
                    </button>
                    <button
                      className="user-dropdown__item user-dropdown__item--logout"
                      onClick={handleLogout}
                      type="button"
                    >
                      <Icon name="logout" />
                      <span>Đăng xuất</span>
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="dashboard-content">{children}</main>
      </div>

      {/* Change Password Dialog (Compact & Balanced Layout) */}
      <Dialog
        className="dialog--compact"
        footer={
          <>
            <button
              className="button button--secondary"
              onClick={() => {
                setIsChangePasswordOpen(false);
                setChangePasswordError("");
              }}
              type="button"
            >
              Hủy
            </button>
            <button className="button button--primary" form="change-password-form" type="submit">
              Cập nhật mật khẩu
            </button>
          </>
        }
        onClose={() => {
          setIsChangePasswordOpen(false);
          setChangePasswordError("");
        }}
        open={isChangePasswordOpen}
        preventBackdropClose={true}
        title="Đổi mật khẩu tài khoản"
      >
        <form
          className={`form form--compact ${changePasswordError ? "form--shake" : ""}`}
          id="change-password-form"
          onSubmit={handleChangePasswordSubmit}
        >
          {changePasswordSuccess && (
            <div className="notice notice--success">
              <Icon name="check" />
              <span>Đổi mật khẩu thành công!</span>
            </div>
          )}

          {changePasswordError && (
            <div className="notice notice--error notice--shake" role="alert">
              <Icon name="alert" />
              <span>{changePasswordError}</span>
            </div>
          )}

          <FormField
            error={changePasswordError && !oldPassword ? "Vui lòng nhập mật khẩu cũ" : undefined}
            id="oldPassword"
            label="Mật khẩu hiện tại"
          >
            <input
              className={`input ${changePasswordError && !oldPassword ? "input--error" : ""}`}
              id="oldPassword"
              onChange={(e) => {
                setOldPassword(e.target.value);
                setChangePasswordError("");
              }}
              placeholder="Nhập mật khẩu cũ"
              required
              type="password"
              value={oldPassword}
            />
          </FormField>

          <FormField
            error={changePasswordError && newPassword.length < 6 ? "Tối thiểu 6 ký tự" : undefined}
            id="newPassword"
            label="Mật khẩu mới (tối thiểu 6 ký tự)"
          >
            <input
              className={`input ${changePasswordError && newPassword.length < 6 ? "input--error" : ""}`}
              id="newPassword"
              onChange={(e) => {
                setNewPassword(e.target.value);
                setChangePasswordError("");
              }}
              placeholder="Nhập mật khẩu mới"
              required
              type="password"
              value={newPassword}
            />
          </FormField>

          <FormField
            error={changePasswordError && newPassword !== confirmPassword ? "Mật khẩu xác nhận không khớp" : undefined}
            id="confirmPassword"
            label="Xác nhận mật khẩu mới"
          >
            <input
              className={`input ${changePasswordError && newPassword !== confirmPassword ? "input--error" : ""}`}
              id="confirmPassword"
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                setChangePasswordError("");
              }}
              placeholder="Nhập lại mật khẩu mới"
              required
              type="password"
              value={confirmPassword}
            />
          </FormField>
        </form>
      </Dialog>
    </div>
  );
}

const DEFAULT_USER_SESSION: DemoSession = {
  fullName: "Nguyễn Minh Anh",
  roleCode: "VT-06",
  roleName: "Quản trị viên",
  username: "minh.anh",
};
