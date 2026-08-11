function normalizeRole(role: string): string {
  return role.trim().toUpperCase();
}

export function hasRequesterRole(roles: string[]): boolean {
  return roles.some((role) => normalizeRole(role) === "REQUESTER");
}

export function hasDonorRole(roles: string[]): boolean {
  return roles.some((role) => normalizeRole(role) === "DONOR");
}

export function hasBloodCenterRole(roles: string[]): boolean {
  return roles.some((role) => normalizeRole(role) === "BLOOD_CENTER");
}

export function hasAdminRole(roles: string[]): boolean {
  return roles.some((role) => normalizeRole(role) === "SYSTEM_ADMIN");
}

export function isRequesterOnly(roles: string[]): boolean {
  return hasRequesterRole(roles) && !hasDonorRole(roles) && !hasAdminRole(roles);
}

export function resolvePostLoginPath(roles: string[]): string {
  if (isRequesterOnly(roles)) {
    return "/requests";
  }

  if (hasDonorRole(roles) || hasAdminRole(roles)) {
    return "/dashboard";
  }

  if (hasRequesterRole(roles)) {
    return "/requests";
  }

  if (hasBloodCenterRole(roles)) {
    return "/dashboard";
  }

  return "/dashboard";
}
