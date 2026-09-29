import test from "node:test";
import assert from "node:assert/strict";
import { can, hasRole } from "../lib/permissions.ts";
import {
  clearAuthSession,
  getAuthToken,
  getStoredUser,
  setAuthToken,
  setStoredUser,
} from "../lib/auth-session.ts";
import { apiRequest, ApiError } from "../lib/api-client.ts";
import { logout } from "../lib/api/auth.ts";

// Setup mock browser localStorage and sessionStorage in Node test environment
const mockLocalStorageStore = new Map();
const mockSessionStorageStore = new Map();

globalThis.localStorage = {
  getItem: (key) => mockLocalStorageStore.get(key) ?? null,
  setItem: (key, val) => mockLocalStorageStore.set(key, String(val)),
  removeItem: (key) => mockLocalStorageStore.delete(key),
  clear: () => mockLocalStorageStore.clear(),
};

globalThis.sessionStorage = {
  getItem: (key) => mockSessionStorageStore.get(key) ?? null,
  setItem: (key, val) => mockSessionStorageStore.set(key, String(val)),
  removeItem: (key) => mockSessionStorageStore.delete(key),
  clear: () => mockSessionStorageStore.clear(),
};

globalThis.window = {
  location: {
    pathname: "/dashboard/overview",
    href: "/dashboard/overview",
  },
};

test("Security Test 1 & 6: Client permission tampering does not bypass backend authorization", async () => {
  mockLocalStorageStore.clear();

  // 1. Initial state: standard specialist employee (VT-04)
  const initialUser = {
    id: 101,
    username: "dev_user",
    roleCode: "VT-04",
    roleName: "Chuyên viên phát triển",
    dataScope: "SELF",
    permissions: ["WORK_LOG_READ", "WORK_LOG_CREATE"],
  };

  setAuthToken("valid-low-privilege-jwt");
  setStoredUser(initialUser);

  assert.equal(can("DATA_BACKUP_MANAGE"), false);
  assert.equal(hasRole("VT-06"), false);

  // 2. Attacker modifies localStorage in browser DevTools to claim ADMIN permissions
  const tamperedUser = {
    ...initialUser,
    roleCode: "VT-06",
    roleName: "Quản trị viên tối cao",
    permissions: ["DATA_BACKUP_MANAGE", "USER_DELETE", "ADMIN"],
  };
  mockLocalStorageStore.set("nexushrm_auth_user", JSON.stringify(tamperedUser));

  // Client helper reflects tampered state for UI ONLY
  assert.equal(can("DATA_BACKUP_MANAGE"), true);
  assert.equal(hasRole("VT-06"), true);

  // 3. Privileged request to backend: backend verifies JWT signature & database permissions, rejecting with 403
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.ok(options.headers.get("Authorization").includes("valid-low-privilege-jwt"));
    return {
      ok: false,
      status: 403,
      headers: {
        get: (h) => (h.toLowerCase() === "content-type" ? "application/json" : null),
      },
      json: async () => ({
        success: false,
        message: "Truy cập bị từ chối: Tài khoản không có quyền thực hiện thao tác quản trị.",
      }),
    };
  };

  try {
    await assert.rejects(
      async () => {
        await apiRequest("/backups/create", { method: "POST" });
      },
      (err) => {
        assert.ok(err instanceof ApiError);
        assert.equal(err.status, 403);
        assert.ok(err.message.includes("Truy cập bị từ chối"));
        return true;
      }
    );
  } finally {
    globalThis.fetch = originalFetch;
    mockLocalStorageStore.clear();
  }
});

test("Security Test 2: JWT is NEVER stored in localStorage (mitigating XSS token theft)", () => {
  mockLocalStorageStore.clear();
  setAuthToken("super-secret-jwt-token");

  // In-memory accessor returns the active token
  assert.equal(getAuthToken(), "super-secret-jwt-token");

  // localStorage must NEVER contain the secret JWT token
  assert.equal(mockLocalStorageStore.get("nexushrm_auth_token"), undefined);
  assert.equal(mockLocalStorageStore.get("accessToken"), undefined);
  assert.equal(mockLocalStorageStore.get("token"), undefined);
});

test("Security Test 3: clearAuthSession and logout purges ALL auth tokens and credentials", async () => {
  mockLocalStorageStore.clear();
  mockSessionStorageStore.clear();

  // Populate state
  setAuthToken("jwt-token-xyz");
  mockLocalStorageStore.set("nexushrm_auth_user", JSON.stringify({ id: 1, username: "admin" }));
  mockLocalStorageStore.set("accessToken", "legacy-token-xyz");
  mockLocalStorageStore.set("currentUser", JSON.stringify({ id: 1, username: "admin" }));
  mockLocalStorageStore.set("token", "legacy-token-2");
  mockSessionStorageStore.set("demo-session", "demo-data");

  assert.equal(getAuthToken(), "jwt-token-xyz");
  assert.notEqual(getStoredUser(), null);

  clearAuthSession();

  // Assert all sensitive credential keys are purged
  assert.equal(mockLocalStorageStore.get("nexushrm_auth_token"), undefined);
  assert.equal(mockLocalStorageStore.get("nexushrm_auth_user"), undefined);
  assert.equal(mockLocalStorageStore.get("accessToken"), undefined);
  assert.equal(mockLocalStorageStore.get("currentUser"), undefined);
  assert.equal(mockLocalStorageStore.get("token"), undefined);
  assert.equal(mockSessionStorageStore.get("demo-session"), undefined);

  assert.equal(getAuthToken(), null);
  assert.equal(getStoredUser(), null);
});

test("Security Test: logout notifies backend /auth/logout to blacklist token", async () => {
  mockLocalStorageStore.clear();
  setAuthToken("active-user-jwt");
  setStoredUser({ id: 99, username: "logout_test" });

  let backendLogoutCalled = false;
  let sentAuthHeader = null;

  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    if (url.includes("/auth/logout")) {
      backendLogoutCalled = true;
      sentAuthHeader = options.headers.get("Authorization");
      return {
        ok: true,
        status: 200,
        headers: { get: () => "application/json" },
        json: async () => ({ success: true, message: "Đăng xuất thành công" }),
      };
    }
    return originalFetch(url, options);
  };

  try {
    await logout();
    assert.equal(backendLogoutCalled, true);
    assert.equal(sentAuthHeader, "Bearer active-user-jwt");
    assert.equal(getAuthToken(), null);
    assert.equal(getStoredUser(), null);
  } finally {
    globalThis.fetch = originalFetch;
    mockLocalStorageStore.clear();
  }
});

test("Security Test 4: Expired token 401 response purges local session and redirects", async () => {
  mockLocalStorageStore.clear();
  setAuthToken("expired-jwt-token");
  setStoredUser({ id: 12, username: "expired_user" });

  let redirectDestination = null;
  globalThis.window.location = {
    pathname: "/dashboard/projects",
    set href(val) {
      redirectDestination = val;
    },
    get href() {
      return redirectDestination;
    },
  };

  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => ({
    ok: false,
    status: 401,
    headers: { get: () => "application/json" },
    json: async () => ({ success: false, message: "Bạn cần đăng nhập để truy cập" }),
  });

  try {
    await assert.rejects(async () => {
      await apiRequest("/projects");
    });

    // Session must be purged
    assert.equal(getAuthToken(), null);
    assert.equal(getStoredUser(), null);
    assert.equal(redirectDestination, "/login");
  } finally {
    globalThis.fetch = originalFetch;
    mockLocalStorageStore.clear();
  }
});
