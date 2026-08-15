import axios from "axios";
import { API_BASE_URL } from "../config/api";
import type { AuthSession, LoginCredentials, LoginResponse } from "../types/auth";
import { translateKnownApiMessage } from "../utils/apiError";

export type ConfirmEmailResponse = {
  message: string;
  email: string | null;
};

export type ResendConfirmationResponse = {
  message: string;
};

const AUTH_SESSION_KEY = "bloodmatch.auth.session";
const POST_LOGIN_NOTICE_KEY = "bloodmatch.auth.notice";

type JwtPayload = {
  exp?: number;
};

function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) {
      return null;
    }

    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const normalized = `${base64}${"=".repeat((4 - (base64.length % 4)) % 4)}`;
    return JSON.parse(atob(normalized)) as JwtPayload;
  } catch {
    return null;
  }
}

function decodeJwtExp(token: string): number | null {
  const payload = decodeJwtPayload(token);
  return typeof payload?.exp === "number" ? payload.exp * 1000 : null;
}

function buildSession(data: LoginResponse): AuthSession {
  const now = Date.now();
  const expFromToken = decodeJwtExp(data.accessToken);
  const expFromApi = typeof data.expiresIn === "number" ? now + data.expiresIn : null;

  return {
    tokenType: data.tokenType || "Bearer",
    accessToken: data.accessToken,
    expiresAt: expFromToken ?? expFromApi ?? now + 60 * 60 * 1000,
    partyId: data.partyId,
    roles: Array.isArray(data.roles) ? data.roles : [],
  };
}

function saveSession(session: AuthSession): void {
  localStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(session));
}

function readSession(): AuthSession | null {
  const raw = localStorage.getItem(AUTH_SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    localStorage.removeItem(AUTH_SESSION_KEY);
    return null;
  }
}

function clearSession(): void {
  localStorage.removeItem(AUTH_SESSION_KEY);
}

export const authService = {
  async login(credentials: LoginCredentials): Promise<AuthSession> {
    // Evita reenviar token antigo/inválido em qualquer fluxo paralelo.
    clearSession();

    const { data } = await axios.post<LoginResponse>(`${API_BASE_URL}/auth/login`, credentials, {
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!data?.accessToken || typeof data.accessToken !== "string") {
      throw new Error("Resposta de login inválida: accessToken ausente.");
    }

    if (!data?.partyId || typeof data.partyId !== "string") {
      throw new Error("Resposta de login inválida: partyId ausente.");
    }

    const session = buildSession(data);
    saveSession(session);

    return session;
  },

  logout(): void {
    clearSession();
  },

  getSession(): AuthSession | null {
    return readSession();
  },

  isAuthenticated(): boolean {
    const session = readSession();
    if (!session) {
      return false;
    }

    if (Date.now() >= session.expiresAt) {
      clearSession();
      return false;
    }

    return true;
  },

  getAccessToken(): string | null {
    if (!this.isAuthenticated()) {
      return null;
    }

    return readSession()?.accessToken ?? null;
  },

  getRoles(): string[] {
    if (!this.isAuthenticated()) {
      return [];
    }

    return readSession()?.roles ?? [];
  },

  getPartyId(): string | null {
    if (!this.isAuthenticated()) {
      return null;
    }

    return readSession()?.partyId ?? null;
  },

  consumePostLoginNotice(): string | null {
    const notice = sessionStorage.getItem(POST_LOGIN_NOTICE_KEY);
    if (!notice) {
      return null;
    }

    sessionStorage.removeItem(POST_LOGIN_NOTICE_KEY);
    return notice;
  },

  setPostLoginNotice(message: string): void {
    sessionStorage.setItem(POST_LOGIN_NOTICE_KEY, message);
  },

  async confirmEmail(token: string): Promise<ConfirmEmailResponse> {
    const { data } = await axios.post<{ message?: string; email?: string }>(
      `${API_BASE_URL}/auth/confirm-email`,
      { token },
      { headers: { "Content-Type": "application/json" } },
    );
    return {
      message: translateKnownApiMessage(data?.message?.trim() || "Email confirmed"),
      email: typeof data?.email === "string" ? data.email : null,
    };
  },

  async resendConfirmation(email: string): Promise<ResendConfirmationResponse> {
    const { data } = await axios.post<{ message?: string }>(
      `${API_BASE_URL}/auth/resend-confirmation`,
      { email },
      { headers: { "Content-Type": "application/json" } },
    );
    return {
      message: translateKnownApiMessage(
        data?.message?.trim() || "If the email is registered and pending confirmation, a new message was sent.",
      ),
    };
  },
};
