"use client";

import { useRouter } from "next/navigation";
import { useRef, useState } from "react";

import { Dialog } from "@/components/ui/Dialog";
import { FormField } from "@/components/ui/FormField";
import { Icon } from "@/components/ui/Icon";
import { Logo } from "@/components/ui/Logo";
import { saveDemoSession } from "@/lib/demo-session";
import { DEMO_USERS } from "@/src/mocks/hrm";

type LoginErrors = Partial<Record<"password" | "username", string>>;

export function LoginForm() {
  const router = useRouter();
  const [username, setUsername] = useState("minh.anh");
  const [password, setPassword] = useState("admin@123");
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [errors, setErrors] = useState<LoginErrors>({});
  const [submitError, setSubmitError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  // Forgot password state
  const [isForgotPasswordOpen, setIsForgotPasswordOpen] = useState(false);
  const [resetEmail, setResetEmail] = useState("");
  const [resetStatus, setResetStatus] = useState<"idle" | "sending" | "sent">("idle");
  const [resetError, setResetError] = useState("");

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

  function handleSelectQuickAccount(targetUsername: string) {
    const matched = DEMO_USERS.find((u) => u.username === targetUsername);
    if (matched) {
      setUsername(matched.username);
      setPassword("password123");
      setErrors({});
      setSubmitError("");
    }
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

    setIsLoading(true);
    setSubmitError("");

    // Simulate login request (matching backend /api/v1/auth/login)
    setTimeout(() => {
      setIsLoading(false);
      const matchedUser = DEMO_USERS.find(
        (u) => u.username.toLowerCase() === username.trim().toLowerCase()
      );

      if (matchedUser) {
        if (matchedUser.status === "LOCKED") {
          setSubmitError("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
          return;
        }

        saveDemoSession({
          fullName: matchedUser.fullName,
          roleCode: matchedUser.roleCode,
          roleName: matchedUser.roleName,
          username: matchedUser.username,
        });
      } else {
        // Fallback for custom usernames in prototype
        saveDemoSession({
          fullName: username.trim(),
          roleCode: "VT-06",
          roleName: "Quản trị viên",
          username: username.trim(),
        });
      }

      router.push("/users");
    }, 600);
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    submitLogin();
  }

  function handleForgotPasswordSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!resetEmail.trim() || !resetEmail.includes("@")) {
      setResetError("Vui lòng nhập địa chỉ email hợp lệ.");
      return;
    }
    setResetStatus("sending");
    setResetError("");

    setTimeout(() => {
      setResetStatus("sent");
    }, 800);
  }

  return (
    <div className="login-page">
      <aside className="login-aside" aria-label="Giới thiệu hệ thống">
        <div className="login-aside__background-glow" />
        <div className="login-aside__content">
          <div className="login-aside__header">
            <Logo size={42} theme="dark" variant="full" />
          </div>

          <div className="login-aside__hero">
            <h1>Quản trị nhân sự & Phân quyền doanh nghiệp</h1>
            <p>Kiểm soát toàn diện cơ cấu tổ chức, phân quyền phạm vi dữ liệu đa cấp và bảo mật tài khoản tập trung.</p>
          </div>

          {/* Enterprise Feature Cards */}
          <div className="login-feature-grid">
            <div className="login-feature-card">
              <div className="login-feature-card__icon">
                <Icon name="organization" />
              </div>
              <div className="login-feature-card__text">
                <strong>Sơ đồ cây tổ chức đa cấp</strong>
                <span>Quản lý Công ty, Khối, Phòng ban & Nhóm trực quan</span>
              </div>
            </div>

            <div className="login-feature-card">
              <div className="login-feature-card__icon">
                <Icon name="shield" />
              </div>
              <div className="login-feature-card__text">
                <strong>Data Scope linh hoạt</strong>
                <span>Phân quyền theo Toàn công ty, Đơn vị hoặc Cá nhân</span>
              </div>
            </div>

            <div className="login-feature-card">
              <div className="login-feature-card__icon">
                <Icon name="sparkles" />
              </div>
              <div className="login-feature-card__text">
                <strong>Tối ưu trải nghiệm</strong>
                <span>Giao diện tốc độ cao, bảo mật token & kiểm soát phiên</span>
              </div>
            </div>
          </div>

          <div className="login-aside__footer">
            <span>© 2026 NexusHRM Enterprise System · All rights reserved.</span>
          </div>
        </div>
      </aside>

      <main className="login-main">
        <section aria-labelledby="login-title" className="login-card">
          <div className="login-card__brand">
            <Logo size={44} theme="light" variant="full" />
          </div>

          <div className="login-card__intro">
            <h2 id="login-title">Chào mừng trở lại</h2>
            <p>Vui lòng nhập thông tin xác thực để bắt đầu phiên làm việc.</p>
          </div>

          {/* Quick Demo Account Selector */}
          <div className="quick-accounts">
            <div className="quick-accounts__header">
              <Icon name="sparkles" />
              <span className="quick-accounts__title">Tài khoản thử nghiệm nhanh:</span>
            </div>
            <div className="quick-accounts__chips">
              {DEMO_USERS.map((user) => (
                <button
                  key={user.id}
                  type="button"
                  onClick={() => handleSelectQuickAccount(user.username)}
                  className={`quick-account-chip ${username === user.username ? "is-selected" : ""}`}
                  title={`${user.fullName} (${user.roleName})`}
                >
                  <Icon name={user.roleCode === "VT-06" ? "access" : "user"} />
                  <span>{user.username}</span>
                  <small>· {user.roleName}</small>
                </button>
              ))}
            </div>
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
                disabled={isLoading}
                id="username"
                onChange={(event) => updateUsername(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" && !event.nativeEvent.isComposing) {
                    event.preventDefault();
                    passwordRef.current?.focus();
                  }
                }}
                placeholder="vd. minh.anh"
                ref={usernameRef}
                required
                value={username}
              />
            </FormField>

            <FormField error={errors.password} id="password" label="Mật khẩu">
              <div className="input-action-wrapper">
                <input
                  aria-describedby="password-message"
                  aria-invalid={Boolean(errors.password)}
                  aria-required="true"
                  autoComplete="current-password"
                  className="input"
                  disabled={isLoading}
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
                  type={showPassword ? "text" : "password"}
                  value={password}
                />
                <button
                  type="button"
                  className="input-action-btn"
                  onClick={() => setShowPassword(!showPassword)}
                  title={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                  aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                >
                  <Icon name={showPassword ? "eyeOff" : "eye"} />
                </button>
              </div>
            </FormField>

            <div className="login-form__options">
              <label>
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                />
                <span>Ghi nhớ đăng nhập</span>
              </label>

              <button
                type="button"
                className="forgot-password-btn"
                onClick={() => {
                  setIsForgotPasswordOpen(true);
                  setResetStatus("idle");
                  setResetEmail("");
                  setResetError("");
                }}
              >
                <Icon name="lock" />
                <span>Quên mật khẩu?</span>
              </button>
            </div>

            <button
              className="button button--primary login-form__submit"
              disabled={isLoading}
              type="submit"
            >
              {isLoading ? (
                <>
                  <Icon name="spinner" />
                  Đang đăng nhập...
                </>
              ) : (
                <>
                  Đăng nhập
                  <Icon name="arrowRight" />
                </>
              )}
            </button>
          </form>
        </section>
      </main>

      {/* Forgot Password Dialog */}
      <Dialog
        open={isForgotPasswordOpen}
        onClose={() => setIsForgotPasswordOpen(false)}
        title="Khôi phục mật khẩu"
        description="Nhập địa chỉ email đăng ký tài khoản của bạn để nhận liên kết đặt lại mật khẩu."
      >
        {resetStatus === "sent" ? (
          <div className="empty-state">
            <div className="notice notice--success" style={{ width: "100%", justifyContent: "center" }}>
              <Icon name="check" />
              <span>Đã gửi hướng dẫn khôi phục mật khẩu đến <strong>{resetEmail}</strong>. Vui lòng kiểm tra hộp thư!</span>
            </div>
            <button
              type="button"
              className="button button--primary"
              style={{ marginTop: "1rem" }}
              onClick={() => setIsForgotPasswordOpen(false)}
            >
              Đã hiểu & Quay lại đăng nhập
            </button>
          </div>
        ) : (
          <form className="form" onSubmit={handleForgotPasswordSubmit}>
            {resetError ? (
              <div className="notice notice--error">
                <Icon name="alert" />
                <span>{resetError}</span>
              </div>
            ) : null}

            <FormField error={resetError} id="reset-email" label="Email đăng ký">
              <input
                id="reset-email"
                type="email"
                className="input"
                placeholder="vd. user@company.com"
                value={resetEmail}
                onChange={(e) => {
                  setResetEmail(e.target.value);
                  setResetError("");
                }}
                required
              />
            </FormField>

            <div className="dialog__actions" style={{ display: "flex", justifyContent: "flex-end", gap: "0.5rem", marginTop: "1rem" }}>
              <button
                type="button"
                className="button button--ghost"
                onClick={() => setIsForgotPasswordOpen(false)}
              >
                Hủy
              </button>
              <button
                type="submit"
                className="button button--primary"
                disabled={resetStatus === "sending"}
              >
                {resetStatus === "sending" ? "Đang gửi..." : "Gửi liên kết khôi phục"}
              </button>
            </div>
          </form>
        )}
      </Dialog>
    </div>
  );
}
