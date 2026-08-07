package bloodmatch.application.shared;

import bloodmatch.application.exception.ForbiddenException;
import bloodmatch.domain.shared.valueObjects.DomainID;

public final class PartyOwnership {

  private PartyOwnership() {
  }

  /**
   * Ensures the authenticated actor owns the resource.
   * Pass {@code null} as {@code actorPartyId} to signal SYSTEM_ADMIN bypass.
   */
  public static void requireSameParty(DomainID resourcePartyId, String actorPartyId) {
    if (resourcePartyId == null) {
      throw new ForbiddenException("Forbidden");
    }
    requireSameParty(resourcePartyId.getValue().toString(), actorPartyId);
  }

  public static void requireSameParty(String resourcePartyId, String actorPartyId) {
    if (actorPartyId == null) {
      return;
    }
    if (resourcePartyId == null || !resourcePartyId.equals(actorPartyId)) {
      throw new ForbiddenException("Forbidden");
    }
  }
}
