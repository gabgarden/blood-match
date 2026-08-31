import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { FullPageLoading } from "../components/ui";
import { useRoleResolution } from "../hooks/useRoleResolution";
import { resolvePostLoginPath } from "./roleRouting";

export function RequireAuth({ children }: { children: ReactNode }) {
  const location = useLocation();
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <FullPageLoading message="Validando sessão..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}

export function PublicOnlyRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading, roles } = useAuth();
  const isResolvingRoles = useRoleResolution(roles);

  if (isLoading) {
    return <FullPageLoading message="Validando sessão..." />;
  }

  if (isAuthenticated && isResolvingRoles) {
    return <FullPageLoading message="Carregando permissões..." />;
  }

  if (isAuthenticated) {
    return <Navigate to={resolvePostLoginPath(roles)} replace />;
  }

  return <>{children}</>;
}

export function RoleBasedHomeRedirect() {
  const { roles } = useAuth();
  const isResolvingRoles = useRoleResolution(roles);

  if (isResolvingRoles) {
    return <FullPageLoading message="Carregando permissões..." />;
  }

  return <Navigate to={resolvePostLoginPath(roles)} replace />;
}

export function RequireRoles({
  acceptedRoles,
  children,
  fallback,
}: {
  acceptedRoles: string[];
  children: ReactNode;
  fallback: ReactNode;
}) {
  const { roles } = useAuth();
  const hasAccess = acceptedRoles.some((role) => roles.includes(role));

  if (!hasAccess) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
}
