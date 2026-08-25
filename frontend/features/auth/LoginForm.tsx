"use client";

import { useRef, useState } from "react";

import { FormField } from "@/components/ui/FormField";
import { Icon } from "@/components/ui/Icon";

type LoginErrors = Partial<Record<"password" | "username", string>>;

export function LoginForm() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<LoginErrors>({});
  const [submitError, setSubmitError] = useState("");
  const usernameRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);

  function updateUsername(value: string) {
    setUsername(value);
    setErrors((currentErrors) => ({ ...currentErrors, username: undefined }));
    setSubmitError("");
  }

  function updatePassword(value: string) {
    setPassword(value);
    setErrors((currentErrors) => ({ ...currentErrors, password: undefined }));
    setSubmitError("");
  }

  function submitLogin() {
    const nextErrors: LoginErrors = {};
    if (!username.trim() || !password) {
      if (!username.trim()) nextErrors.username = "Tên đăng nhập là bắt buộc.";
      if (!password) nextErrors.password = "Mật khẩu là bắt buộc.";
      setErrors(nextErrors);
      window.requestAnimationFrame(() => {
        if (nextErrors.username) {
          usernameRef.current?.focus();
        } else {
          passwordRef.current?.focus();
        }
      });
      return;
    }

    setSubmitError("Không thể đăng nhập vào lúc này. Vui lòng thử lại sau.");
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    submitLogin();
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
            {submitError ? (
              <div aria-live="polite" className="notice notice--error" role="alert">
                <Icon name="alert" />
                <span>{submitError}</span>
              </div>
            ) : null}

            <FormField error={errors.username} id="username" label="Tên đăng nhập">
              <input
                aria-describedby="username-message"
                aria-invalid={Boolean(errors.username)}
                aria-required="true"
                autoComplete="username"
                className="input"
                id="username"
                onChange={(event) => updateUsername(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" && !event.nativeEvent.isComposing) {
                    event.preventDefault();
                    passwordRef.current?.focus();
                  }
                }}
                placeholder="vd. nguyenvana"
                ref={usernameRef}
                required
                value={username}
              />
            </FormField>

            <FormField error={errors.password} id="password" label="Mật khẩu">
              <input
                aria-describedby="password-message"
                aria-invalid={Boolean(errors.password)}
                aria-required="true"
                autoComplete="current-password"
                className="input"
                id="password"
                onChange={(event) => updatePassword(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" && !event.nativeEvent.isComposing) {
                    event.preventDefault();
                    submitLogin();
                  }
                }}
                placeholder="Nhập mật khẩu"
                ref={passwordRef}
                required
                type="password"
                value={password}
              />
            </FormField>

            <button className="button button--primary login-form__submit" type="submit">
              Đăng nhập
              <Icon name="arrowRight" />
            </button>
          </form>
        </section>
      </main>
    </div>
  );
}
