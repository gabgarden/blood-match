import axios from "axios";
import { authService } from "../services/authService";
import { API_BASE_URL } from "../config/api";

function getAppPath(pathname: string): string {
  const basePath = import.meta.env.BASE_URL.endsWith("/") ? import.meta.env.BASE_URL : `${import.meta.env.BASE_URL}/`;
  return `${basePath}${pathname.replace(/^\/+/, "")}`;
}

function isAuthPublicPath(): boolean {
  const pathname = window.location.pathname.replace(/\/+$/, "");
  const loginPath = getAppPath("login").replace(/\/+$/, "");
  const registerPath = getAppPath("register").replace(/\/+$/, "");
  const checkEmailPath = getAppPath("register/check-email").replace(/\/+$/, "");
  const confirmEmailPath = getAppPath("confirm-email").replace(/\/+$/, "");
  return (
    pathname === loginPath ||
    pathname === registerPath ||
    pathname === checkEmailPath ||
    pathname === confirmEmailPath
  );
}

function shouldForceLogoutOn401(error: unknown): boolean {
  const response = (error as { response?: { data?: { error?: unknown }; status?: number }; config?: { headers?: unknown } })
    ?.response;

  if (response?.status !== 401) {
    return false;
  }

  const errStr = typeof response.data?.error === "string" ? response.data.error.trim() : "";
  return errStr === "Invalid or expired token";
}

/** Cliente para rotas públicas — nunca envia Bearer nem faz logout global. */
export const publicApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const accessToken = authService.getAccessToken();

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (shouldForceLogoutOn401(error)) {
      authService.logout();
      authService.setPostLoginNotice("Sessão expirada. Faça login novamente.");
      window.dispatchEvent(new Event("bloodmatch:session-expired"));

      if (!isAuthPublicPath()) {
        window.location.replace(getAppPath("login"));
      }
    }

    return Promise.reject(error);
  },
);
