package bloodmatch.interfaces.rest.shared;

import bloodmatch.application.exception.ForbiddenException;
import bloodmatch.infra.security.JwtAuthenticationFilter.JwtPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthenticatedPartySupport {

  private AuthenticatedPartySupport() {
  }

  public static void requireSamePartyOrAdmin(String partyId) {
    Authentication authentication = requireAuthentication();
    JwtPrincipal principal = requirePrincipal(authentication);

    if (isSystemAdmin(authentication)) {
      return;
    }

    if (partyId == null || !partyId.equals(principal.partyId())) {
      throw new ForbiddenException("Forbidden");
    }
  }

  /**
   * Party id used for resource ownership checks in use cases.
   * Returns {@code null} when the caller is SYSTEM_ADMIN (bypass).
   */
  public static String actorPartyIdForOwnership() {
    Authentication authentication = requireAuthentication();
    JwtPrincipal principal = requirePrincipal(authentication);

    if (isSystemAdmin(authentication)) {
      return null;
    }

    return principal.partyId();
  }

  public static boolean isSystemAdmin() {
    return isSystemAdmin(requireAuthentication());
  }

  private static Authentication requireAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new ForbiddenException("Forbidden");
    }
    return authentication;
  }

  private static JwtPrincipal requirePrincipal(Authentication authentication) {
    if (!(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
      throw new ForbiddenException("Forbidden");
    }
    return principal;
  }

  private static boolean isSystemAdmin(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("SYSTEM_ADMIN"::equals);
  }
}
