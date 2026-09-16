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
  data?: unknown;

  constructor(message: string, status: number, data?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
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
      if (payload && typeof payload === "object") {
        const p = payload as Record<string, unknown>;
        if (typeof p.message === "string" && p.message.trim().length > 0) {
          errorMessage = p.message;
        } else if (typeof p.error === "string" && p.error.trim().length > 0) {
          errorMessage = p.error;
        } else if (typeof p.code === "string" && p.code.trim().length > 0) {
          errorMessage = `[${p.code}] ${errorMessage}`;
        }
      } else if (typeof payload === "string" && payload.trim().length > 0) {
        if (payload.includes("<!DOCTYPE") || payload.includes("<html")) {
          if (response.status === 403) errorMessage = "Bạn không có quyền truy cập dữ liệu này (403 Forbidden).";
          else if (response.status === 404) errorMessage = "Không tìm thấy dữ liệu yêu cầu (404 Not Found).";
          else if (response.status === 500) errorMessage = "Máy chủ xảy ra lỗi nội bộ (500 Internal Server Error).";
        } else {
          errorMessage = payload;
        }
      }

      if (response.status === 403 && errorMessage === "Access Denied") {
        errorMessage = "Tài khoản hiện tại không có quyền xem hoặc thao tác trên kịch bản này.";
      }
      if (errorMessage === "An unexpected error occurred.") {
        errorMessage = response.status >= 500
          ? "Máy chủ gặp lỗi khi xử lý dữ liệu. Vui lòng thử lại hoặc liên hệ quản trị viên."
          : `Yêu cầu thất bại với mã lỗi ${response.status}`;
      }

      throw new ApiError(errorMessage, response.status, payload);
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
    // Preserve request cancellation so callers can silently ignore stale requests.
    if (error instanceof DOMException && error.name === "AbortError") {
      throw error;
    }
    if (error instanceof TypeError && error.message.includes("fetch")) {
      throw new ApiError(
        "Không thể kết nối đến máy chủ Backend (http://localhost:8080). Vui lòng kiểm tra máy chủ đã được khởi động chưa.",
        0
      );
    }
    const msg = error instanceof Error ? error.message : String(error);
    throw new ApiError(
      msg && msg !== "An unexpected error occurred." ? msg : "Đã xảy ra lỗi không xác định khi tải dữ liệu.",
      500
    );
  }
}
