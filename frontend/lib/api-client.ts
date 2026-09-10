"use client";

import { clearAuthSession, getAuthToken } from "./auth-session";

const metaEnv = typeof import.meta !== "undefined"
  ? (import.meta as unknown as { env?: Record<string, string> }).env
  : undefined;

export const API_BASE_URL =
  metaEnv?.VITE_API_BASE_URL ||
  (typeof window !== "undefined" ? "/api/v1" : "http://localhost:8080/api/v1");

export class ApiError extends Error {
  status: number;
  errorCode?: string;
  data?: unknown;

  constructor(message: string, status: number, data?: unknown, errorCode?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
    this.errorCode = errorCode;
  }
}

export async function apiRequest<T = unknown>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = endpoint.startsWith("http")
    ? endpoint
    : `${API_BASE_URL}${endpoint.startsWith("/") ? endpoint : `/${endpoint}`}`;

  const token = getAuthToken();
  const headers = new Headers(options.headers || {});

  if (!headers.has("Content-Type") && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }

  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });

    // Handle 401 Unauthorized for expired or invalid session
    if (response.status === 401) {
      if (
        typeof window !== "undefined" &&
        !window.location.pathname.startsWith("/login")
      ) {
        clearAuthSession();
        window.location.href = "/login";
      }
    }

    let payload: unknown;
    const contentType = response.headers.get("Content-Type");
    if (contentType && contentType.includes("application/json")) {
      payload = await response.json();
    } else {
      payload = await response.text();
    }

    if (!response.ok) {
      let errorMessage = `Yêu cầu thất bại với mã lỗi ${response.status}`;
      let errorCode: string | undefined;

      if (payload && typeof payload === "object") {
        if ("errorCode" in payload && typeof (payload as any).errorCode === "string") {
          errorCode = (payload as any).errorCode;
        } else if ("code" in payload && typeof (payload as any).code === "string") {
          errorCode = (payload as any).code;
        }

        if ("message" in payload && typeof (payload as any).message === "string") {
          errorMessage = (payload as any).message;
        }
      }

      // Map standard business error codes to human-readable Vietnamese messages
      if (errorCode === "DUPLICATE_EMAIL") {
        errorMessage = "Email đã tồn tại";
      } else if (errorCode === "DUPLICATE_USERNAME") {
        errorMessage = "Tên đăng nhập đã tồn tại";
      }

      throw new ApiError(errorMessage, response.status, payload, errorCode);
    }

    // If payload is wrapped in Spring Boot ApiResponse format { data: ..., success: true }
    if (payload && typeof payload === "object" && "data" in payload && "success" in payload) {
      return (payload as { data: T }).data;
    }

    return payload as T;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    if (error instanceof TypeError && error.message.includes("fetch")) {
      throw new ApiError(
        "Không thể kết nối đến máy chủ Backend (http://localhost:8080). Vui lòng kiểm tra máy chủ đã được khởi động chưa.",
        0
      );
    }
    throw new ApiError(
      error instanceof Error ? error.message : "Đã xảy ra lỗi không xác định.",
      500
    );
  }
}
