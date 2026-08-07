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
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
      throw new ForbiddenException("Forbidden");
    }

    boolean isAdmin = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("SYSTEM_ADMIN"::equals);

    if (!isAdmin && !partyId.equals(principal.partyId())) {
      throw new ForbiddenException("Forbidden");
    }
  }
}
