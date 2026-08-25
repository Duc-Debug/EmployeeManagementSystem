"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { Icon } from "@/components/ui/Icon";
import { DEFAULT_DEMO_SESSION, saveDemoSession } from "@/lib/demo-session";

export function LoginForm() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!username.trim() || !password) {
      setError("Tên đăng nhập và mật khẩu là bắt buộc. Hãy điền đủ hai trường để tiếp tục.");
      return;
    }

    setError(
      "Dịch vụ đăng nhập chưa được kết nối trong bản UI này. Mở giao diện demo để xem các màn hình quản trị bằng dữ liệu minh họa.",
    );
  }

  function openDemo() {
    saveDemoSession(DEFAULT_DEMO_SESSION);
    router.push("/users");
  }

  return (
    <div className="login-page">
      <aside className="login-aside" aria-label="Giới thiệu hệ thống">
        <div className="login-aside__content">
          <span className="brand-mark">EM</span>
          <div className="login-aside__rule" />
          <h1>Quản trị nhân sự, theo cấu trúc rõ ràng.</h1>
          <p>Quản lý tài khoản, cơ cấu tổ chức và phạm vi truy cập trong cùng một không gian làm việc.</p>
        </div>
      </aside>

      <main className="login-main">
        <section aria-labelledby="login-title" className="login-card">
          <div className="login-card__brand">
            <span aria-hidden="true" className="brand-mark">EM</span>
            <span>
              <strong>Employee Management</strong>
              <small>Hệ thống quản lý nhân sự</small>
            </span>
          </div>

          <div className="login-card__intro">
            <h2 id="login-title">Đăng nhập</h2>
            <p>Nhập thông tin tài khoản để truy cập không gian quản trị của bạn.</p>
          </div>

          <form className="form login-form" noValidate onSubmit={handleSubmit}>
            {error ? (
              <div aria-live="polite" className="notice notice--error" role="alert">
                <Icon name="alert" />
                <span>{error}</span>
              </div>
            ) : null}

            <div className="field-group">
              <label htmlFor="username">Tên đăng nhập</label>
              <input
                autoComplete="username"
                className="input"
                id="username"
                onChange={(event) => setUsername(event.target.value)}
                placeholder="vd. nguyenvana"
                required
                value={username}
              />
            </div>

            <div className="field-group">
              <label htmlFor="password">Mật khẩu</label>
              <input
                autoComplete="current-password"
                className="input"
                id="password"
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Nhập mật khẩu"
                required
                type="password"
                value={password}
              />
            </div>

            <button className="button button--primary login-form__submit" type="submit">
              Đăng nhập
              <Icon name="arrowRight" />
            </button>
          </form>

          <div className="login-card__demo">
            <strong>Giao diện đang dùng dữ liệu minh họa.</strong>
            <p>Chức năng đăng nhập thật sẽ gọi API khi lớp session/BFF được bổ sung.</p>
            <button className="button button--secondary" onClick={openDemo} type="button">
              Mở giao diện demo
              <Icon name="arrowRight" />
            </button>
          </div>
        </section>
      </main>
    </div>
  );
}
