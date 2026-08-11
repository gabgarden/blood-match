// Prefer 127.0.0.1 over localhost to avoid IPv6 (::1) connection issues on Windows.
export const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || "http://127.0.0.1:8080";
