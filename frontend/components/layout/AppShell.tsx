"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useLayoutEffect, useRef, useState, type ReactNode } from "react";

import { Icon, type IconName } from "@/components/ui/Icon";
import { Logo } from "@/components/ui/Logo";
import { clearDemoSession, readDemoSession } from "@/lib/demo-session";

const navigation: ReadonlyArray<{ href: string; icon: IconName; label: string }> = [
  { href: "/users", icon: "users", label: "Tài khoản" },
  { href: "/organization", icon: "organization", label: "Cây tổ chức" },
  { href: "/access", icon: "access", label: "Phân quyền" },
];

export function AppShell({ children }: Readonly<{ children: ReactNode }>) {
  const pathname = usePathname();
  const router = useRouter();
  const [isNavigationOpen, setIsNavigationOpen] = useState(false);
  const [isDesktopNavigation, setIsDesktopNavigation] = useState(false);
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
    <div className="app-shell">
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
        className={isNavigationOpen ? "side-nav is-open" : "side-nav"}
        inert={isDesktopNavigation || isNavigationOpen ? undefined : true}
        onKeyDown={(event) => {
          if (event.key === "Escape") {
            closeNavigation();
          }
        }}
      >
        <div className="side-nav__brand">
          <Logo size={36} theme="dark" variant="full" />
        </div>

        <nav aria-label="Điều hướng chính" className="side-nav__links">
          {navigation.map((item) => {
            const isCurrent = pathname === item.href;
            return (
              <Link
                aria-current={isCurrent ? "page" : undefined}
                className={isCurrent ? "side-nav__link is-current" : "side-nav__link"}
                href={item.href}
                key={item.href}
                onClick={() => closeNavigation(false)}
                ref={item.href === navigation[0]?.href ? firstNavigationLinkRef : undefined}
              >
                <Icon name={item.icon} />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="side-nav__footer">
          <div className="session-card">
            <span aria-hidden="true" className="avatar avatar--small">{user.fullName.slice(0, 1)}</span>
            <div>
              <strong>{user.fullName}</strong>
              <span>{user.roleName} · {user.roleCode}</span>
            </div>
          </div>
          <button className="side-nav__logout" onClick={handleLogout} type="button">
            <Icon name="logout" />
            <span>Đăng xuất</span>
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
