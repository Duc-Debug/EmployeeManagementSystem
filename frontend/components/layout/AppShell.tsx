"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState, type ReactNode } from "react";

import { Icon, type IconName } from "@/components/ui/Icon";
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

  function closeNavigation() {
    setIsNavigationOpen(false);
  }

  function handleLogout() {
    clearDemoSession();
    router.push("/login");
  }

  const user = readDemoSession();

  return (
    <div className="app-shell">
      <button
        aria-label="Đóng menu điều hướng"
        className={isNavigationOpen ? "side-nav-backdrop is-visible" : "side-nav-backdrop"}
        onClick={closeNavigation}
        type="button"
      />
      <aside className={isNavigationOpen ? "side-nav is-open" : "side-nav"}>
        <div className="side-nav__brand">
          <span aria-hidden="true" className="brand-mark">EM</span>
          <span>
            <strong>Employee</strong>
            <small>Management System</small>
          </span>
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
                onClick={closeNavigation}
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
          <p className="side-nav__mode">Giao diện demo · Mock data</p>
        </div>
      </aside>

      <div className="dashboard-frame">
        <header className="mobile-bar">
          <button
            aria-expanded={isNavigationOpen}
            aria-label="Mở menu điều hướng"
            className="icon-button"
            onClick={() => setIsNavigationOpen(true)}
            type="button"
          >
            <Icon name="menu" />
          </button>
          <span className="mobile-bar__brand">Employee Management</span>
          <span aria-hidden="true" className="avatar avatar--small">{user.fullName.slice(0, 1)}</span>
        </header>
        <main className="dashboard-content">{children}</main>
      </div>
    </div>
  );
}
