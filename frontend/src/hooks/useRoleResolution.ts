import { useEffect, useState } from "react";

const ROLE_RESOLUTION_DELAY_MS = 900;

export function useRoleResolution(roles: string[]): boolean {
  const [isResolvingRoles, setIsResolvingRoles] = useState(roles.length === 0);

  useEffect(() => {
    if (roles.length > 0) {
      setIsResolvingRoles(false);
      return;
    }

    setIsResolvingRoles(true);
    const timer = window.setTimeout(() => {
      setIsResolvingRoles(false);
    }, ROLE_RESOLUTION_DELAY_MS);

    return () => {
      window.clearTimeout(timer);
    };
  }, [roles]);

  return isResolvingRoles;
}