"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useLayoutEffect, useRef, useState, type ReactNode } from "react";

import { Icon, type IconName } from "@/components/ui/Icon";
import { Logo } from "@/components/ui/Logo";
import { clearDemoSession, readDemoSession } from "@/lib/demo-session";

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
      { href: "/users", icon: "users", label: "Tài khoản nhân sự", badge: "Live" },
      { href: "/organization", icon: "organization", label: "Sơ đồ cây tổ chức" },
      { href: "/access", icon: "access", label: "Ma trận phân quyền" },
    ],
  },
];

export function AppShell({ children }: Readonly<{ children: ReactNode }>) {
  const pathname = usePathname();
  const router = useRouter();
  const [isNavigationOpen, setIsNavigationOpen] = useState(false);
  const [isDesktopNavigation, setIsDesktopNavigation] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(false);
  const menuTriggerRef = useRef<HTMLButtonElement>(null);
  const firstNavigationLinkRef = useRef<HTMLAnchorElement>(null);

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

  const user = readDemoSession();

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

        <div className="side-nav__footer">
          <div className="session-card" title={`${user.fullName} (${user.roleName})`}>
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
          </div>
          <button className="side-nav__logout" onClick={handleLogout} title="Đăng xuất" type="button">
            <Icon name="logout" />
            <span className="side-nav__logout-text">Đăng xuất</span>
          </button>
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
                type="text"
                placeholder="Tìm kiếm nhanh nhân sự, đơn vị, quyền hạn... (Ctrl + K)"
                aria-label="Tìm kiếm nhanh"
                className="topbar-search__input"
                readOnly
              />
            </div>
          </div>

          <div className="dashboard-topbar__right">
            <button type="button" className="icon-button notification-btn" title="Thông báo" aria-label="Thông báo">
              <Icon name="bell" />
              <span className="notification-indicator" />
            </button>

            <div className="topbar-divider" />

            <div className="topbar-user">
              <span aria-hidden="true" className="avatar avatar--small">{user.fullName.slice(0, 1)}</span>
              <div className="topbar-user__info">
                <strong>{user.fullName}</strong>
                <span className="topbar-user__role">{user.roleName}</span>
              </div>
            </div>
          </div>
        </header>

        <main className="dashboard-content">{children}</main>
      </div>
    </div>
  );
}
