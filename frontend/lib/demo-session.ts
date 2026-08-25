export interface DemoSession {
  fullName: string;
  username: string;
  roleCode: string;
  roleName: string;
}

export const DEMO_SESSION_STORAGE_KEY = "employee-management-demo-session";

export const DEFAULT_DEMO_SESSION: DemoSession = {
  fullName: "Người dùng demo",
  username: "demo.admin",
  roleCode: "VT-06",
  roleName: "Quản trị viên",
};

export function readDemoSession(): DemoSession {
  if (typeof window === "undefined") {
    return DEFAULT_DEMO_SESSION;
  }

  const storedSession = window.sessionStorage.getItem(DEMO_SESSION_STORAGE_KEY);
  if (!storedSession) {
    return DEFAULT_DEMO_SESSION;
  }

  try {
    return { ...DEFAULT_DEMO_SESSION, ...JSON.parse(storedSession) } as DemoSession;
  } catch {
    return DEFAULT_DEMO_SESSION;
  }
}

export function saveDemoSession(session: DemoSession) {
  window.sessionStorage.setItem(DEMO_SESSION_STORAGE_KEY, JSON.stringify(session));
}

export function clearDemoSession() {
  window.sessionStorage.removeItem(DEMO_SESSION_STORAGE_KEY);
}
