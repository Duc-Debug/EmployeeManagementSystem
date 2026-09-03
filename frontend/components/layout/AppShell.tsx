"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useLayoutEffect, useMemo, useRef, useState, type ReactNode } from "react";

import { Dialog } from "@/components/ui/Dialog";
import { FormField } from "@/components/ui/FormField";
import { Icon, type IconName } from "@/components/ui/Icon";
import { Logo } from "@/components/ui/Logo";
import { clearAuthSession, getAuthToken, getStoredUser, useAuthUser } from "@/lib/auth-session";
import { getCurrentUser, changePassword, logout } from "@/lib/api/auth";
import { ApiError } from "@/lib/api-client";

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
      { href: "/skills", icon: "shield", label: "Khai báo kỹ năng" },
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
    desc: "Cấu trúc 4 cấp đơn vị tổ chức đã được đồng bộ thành công.",
    icon: "organization",
    id: "notif-1",
    time: "5 phút trước",
    title: "Cơ cấu tổ chức",
    unread: true,
  },
  {
    desc: "Hệ thống NexusHRM hoạt động ở chế độ kết nối dữ liệu trực tiếp.",
    icon: "shield",
    id: "notif-2",
    time: "25 phút trước",
    title: "Bảo mật hệ thống",
    unread: false,
  },
];

export function AppShell({ children }: Readonly<{ children: ReactNode }>) {
  const pathname = usePathname();
  const router = useRouter();
  const [isAuthChecking, setIsAuthChecking] = useState(true);
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
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  const menuTriggerRef = useRef<HTMLButtonElement>(null);
  const firstNavigationLinkRef = useRef<HTMLAnchorElement>(null);
  const notifDropdownRef = useRef<HTMLDivElement>(null);
  const topbarUserDropdownRef = useRef<HTMLDivElement>(null);
  const sidebarUserDropdownRef = useRef<HTMLDivElement>(null);
  const authUser = useAuthUser();

  const userNavSections = useMemo(() => {
    return navSections
      .map((section) => ({
        ...section,
        items: section.items.filter((item) => {
          // VT-06 (Admin) has access to all management modules
          if (authUser?.roleCode === "VT-06") {
            return true;
          }
          // VT-04 (Specialist/Employee) has access to skills declaration
          if (authUser?.roleCode === "VT-04" && item.href === "/skills") {
            return true;
          }
          // Non-admin roles have view access to organization tree
          return item.href === "/organization" || item.href === "/skills";
        }),
      }))
      .filter((section) => section.items.length > 0);
  }, [authUser?.roleCode]);

  // Auth Guard: Enforce login if token is missing & sync profile
  useEffect(() => {
    let isMounted = true;
    const token = getAuthToken();

    if (!token) {
      router.replace("/login");
      return;
    }

    getCurrentUser()
      .then(() => {
        if (isMounted) setIsAuthChecking(false);
      })
      .catch((err) => {
        const storedUser = getStoredUser();
        if (storedUser && !(err instanceof ApiError && (err.status === 401 || err.status === 403))) {
          if (isMounted) setIsAuthChecking(false);
        } else {
          clearAuthSession();
          router.replace("/login");
        }
      });

    return () => {
      isMounted = false;
    };
  }, [router]);

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
    logout();
  }

  function handleMarkAllAsRead() {
    setNotifications((prev) => prev.map((n) => ({ ...n, unread: false })));
  }

  function handleNotificationClick(id: string) {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, unread: false } : n)),
    );
  }

  async function handleChangePasswordSubmit(e: React.FormEvent) {
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

    try {
      setIsChangingPassword(true);
      await changePassword({
        confirmPassword,
        currentPassword: oldPassword,
        newPassword,
      });
      setChangePasswordSuccess(true);
      setTimeout(() => {
        setIsChangePasswordOpen(false);
        setChangePasswordSuccess(false);
        setOldPassword("");
        setNewPassword("");
        setConfirmPassword("");
        setIsChangingPassword(false);
      }, 1500);
    } catch (err) {
      setIsChangingPassword(false);
      if (err instanceof ApiError) {
        setChangePasswordError(err.message);
      } else {
        setChangePasswordError("Đổi mật khẩu thất bại. Vui lòng thử lại.");
      }
    }
  }

  const unreadCount = notifications.filter((n) => n.unread).length;

  if (isAuthChecking && !authUser) {
    return (
      <div
        className="auth-loading-screen"
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          minHeight: "100vh",
          gap: "1rem",
          color: "var(--color-text-secondary, #6b7280)",
        }}
      >
        <Icon name="spinner" />
        <span>Đang xác thực phiên làm việc...</span>
      </div>
    );
  }

  if (!authUser) {
    return null;
  }

  const user = authUser;

  return (
    <div className="app-shell">
      <button
        aria-label="Đóng menu điều hướng"
        className={`side-nav-backdrop ${isNavigationOpen ? "is-visible" : ""}`}
        onClick={() => closeNavigation(false)}
        type="button"
      />

      <aside
        aria-label="Thanh điều hướng bên cạnh"
        className={`side-nav ${isNavigationOpen ? "is-open" : ""} ${isCollapsed ? "side-nav--collapsed" : ""}`}
      >
        <div className="side-nav__header">
          <div className="side-nav__brand">
            <Logo
              size={isCollapsed ? 32 : 36}
              theme="light"
              variant={isCollapsed ? "mark" : "full"}
            />
          </div>

          <button
            aria-label={isCollapsed ? "Mở rộng sidebar" : "Thu gọn sidebar"}
            className="icon-button side-nav__toggle-btn"
            onClick={() => setIsCollapsed(!isCollapsed)}
            title={isCollapsed ? "Mở rộng sidebar" : "Thu gọn sidebar"}
            type="button"
          >
            <Icon name="menu" />
          </button>
        </div>

        <nav aria-label="Thanh điều hướng chính" className="side-nav__menu">
          {userNavSections.map((section, sectionIndex) => (
            <div className="side-nav__section" key={section.title}>
              {!isCollapsed && <span className="side-nav__section-title">{section.title}</span>}
              <ul className="side-nav__links" style={{ listStyle: "none", margin: 0, padding: 0 }}>
                {section.items.map((item, itemIndex) => {
                  const isActive = pathname === item.href;
                  return (
                    <li key={item.href} style={{ listStyle: "none", margin: 0, padding: 0 }}>
                      <Link
                        aria-current={isActive ? "page" : undefined}
                        className={`side-nav__link ${isActive ? "is-current" : ""}`}
                        href={item.href}
                        onClick={() => {
                          if (!isDesktopNavigation) {
                            closeNavigation();
                          }
                        }}
                        ref={sectionIndex === 0 && itemIndex === 0 ? firstNavigationLinkRef : undefined}
                        title={isCollapsed ? item.label : undefined}
                      >
                        <span className="side-nav__link-icon">
                          <Icon name={item.icon} />
                        </span>
                        {!isCollapsed && <span className="side-nav__link-label">{item.label}</span>}
                        {item.badge && !isCollapsed && <span className="side-nav__link-badge">{item.badge}</span>}
                      </Link>
                    </li>
                  );
                })}
              </ul>
            </div>
          ))}
        </nav>

        {/* Sidebar Footer User Info & Menu Container */}
        <div className="side-nav__footer dropdown-container" ref={sidebarUserDropdownRef}>
          <button
            aria-expanded={isSidebarUserMenuOpen}
            aria-haspopup="true"
            aria-label="Tùy chọn tài khoản"
            className={`session-card ${isSidebarUserMenuOpen ? "is-active" : ""}`}
            onClick={() => {
              setIsSidebarUserMenuOpen((prev) => !prev);
              setIsNotificationOpen(false);
              setIsTopbarUserMenuOpen(false);
            }}
            type="button"
          >
            <div className="session-card__avatar">
              <span aria-hidden="true" className="avatar avatar--small">{(user.fullName || user.username || "U").slice(0, 1).toUpperCase()}</span>
              <span className="session-card__status-dot" />
            </div>
            {!isCollapsed && (
              <div className="session-card__info">
                <strong>{user.fullName || user.username}</strong>
                <span className="role-chip">{user.roleName}</span>
              </div>
            )}
          </button>

          {isSidebarUserMenuOpen && (
            <div className="dropdown-menu user-dropdown side-nav__user-dropdown">
              <div className="user-dropdown__header">
                <div className="user-dropdown__avatar">
                  <span className="avatar avatar--large">{(user.fullName || user.username || "U").slice(0, 1).toUpperCase()}</span>
                  <span className="user-dropdown__online" />
                </div>
                <div className="user-dropdown__details">
                  <strong>{user.fullName || user.username}</strong>
                  <span className="user-dropdown__username">@{user.username}</span>
                  <span className="role-chip">{user.roleName} ({user.roleCode})</span>
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
                placeholder="Tìm kiếm nhanh nhân sự, đơn vị, quyền hạn..."
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
                          <div className="notification-item__icon">
                            <Icon name={notif.icon} />
                          </div>
                          <div className="notification-item__content">
                            <div className="notification-item__title">
                              <span>{notif.title}</span>
                              <time>{notif.time}</time>
                            </div>
                            <p className="notification-item__desc">{notif.desc}</p>
                          </div>
                        </button>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>

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
                <span aria-hidden="true" className="avatar avatar--small">{(user.fullName || user.username || "U").slice(0, 1).toUpperCase()}</span>
                <div className="topbar-user__info">
                  <strong>{user.fullName || user.username}</strong>
                  <span className="topbar-user__role">{user.roleName}</span>
                </div>
              </button>

              {isTopbarUserMenuOpen && (
                <div className="dropdown-menu user-dropdown">
                  <div className="user-dropdown__header">
                    <div className="user-dropdown__avatar">
                      <span className="avatar avatar--large">{(user.fullName || user.username || "U").slice(0, 1).toUpperCase()}</span>
                      <span className="user-dropdown__online" />
                    </div>
                    <div className="user-dropdown__details">
                      <strong>{user.fullName || user.username}</strong>
                      <span className="user-dropdown__username">@{user.username}</span>
                      <span className="role-chip">{user.roleName} ({user.roleCode})</span>
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
            <button className="button button--primary" disabled={isChangingPassword} form="change-password-form" type="submit">
              {isChangingPassword ? "Đang lưu..." : "Cập nhật mật khẩu"}
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
