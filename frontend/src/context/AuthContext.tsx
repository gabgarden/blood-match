import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { authService } from "../services/authService";
import { completePendingProfiles, clearPendingProfiles } from "../services/pendingProfiles";
import type { AuthSession, LoginCredentials } from "../types/auth";

type AuthContextValue = {
  session: AuthSession | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  roles: string[];
  partyId: string | null;
  login: (credentials: LoginCredentials) => Promise<AuthSession>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() =>
    authService.isAuthenticated() ? authService.getSession() : null,
  );
  const [isLoading] = useState(false);

  useEffect(() => {
    const handleSessionExpired = () => {
      setSession(null);
    };

    window.addEventListener("bloodmatch:session-expired", handleSessionExpired);
    return () => {
      window.removeEventListener("bloodmatch:session-expired", handleSessionExpired);
    };
  }, []);

  const login = useCallback(async (credentials: LoginCredentials) => {
    const nextSession = await authService.login(credentials);
    setSession(nextSession);

    try {
      const didCreateRoles = await completePendingProfiles(nextSession.partyId);
      if (!didCreateRoles) {
        return nextSession;
      }

      const refreshedSession = await authService.login(credentials);
      clearPendingProfiles();
      setSession(refreshedSession);
      return refreshedSession;
    } catch (error) {
      throw error;
    }
  }, []);

  const logout = useCallback(() => {
    authService.logout();
    setSession(null);
  }, []);

  const value = useMemo<AuthContextValue>(() => {
    return {
      session,
      isLoading,
      isAuthenticated: !!session && authService.isAuthenticated(),
      roles: session?.roles ?? [],
      partyId: session?.partyId ?? null,
      login,
      logout,
    };
  }, [session, isLoading, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}
