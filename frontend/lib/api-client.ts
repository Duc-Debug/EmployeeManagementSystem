"use client";

import { clearAuthSession, getAuthToken } from "./auth-session";

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api/v1";

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

    // Handle 401 Unauthorized and 403 Forbidden for protected routes
    if (response.status === 401 || response.status === 403) {
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
      if (payload && typeof payload === "object" && "message" in payload && typeof payload.message === "string") {
        errorMessage = payload.message;
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
