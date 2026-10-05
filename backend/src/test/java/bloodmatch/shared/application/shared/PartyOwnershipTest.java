package bloodmatch.shared.application.shared;

import bloodmatch.shared.application.exception.ForbiddenException;
import bloodmatch.shared.domain.valueObjects.DomainID;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartyOwnershipTest {

  @Test
  void shouldAllowWhenActorPartyIdIsNull() {
    DomainID resourcePartyId = DomainID.generate();
    // null actorPartyId means SYSTEM_ADMIN bypass
    assertDoesNotThrow(() -> PartyOwnership.requireSameParty(resourcePartyId, null));
    assertDoesNotThrow(() -> PartyOwnership.requireSameParty(resourcePartyId.getValue().toString(), null));
  }

  @Test
  void shouldAllowWhenActorMatchesResource() {
    UUID uuid = UUID.randomUUID();
    DomainID resourcePartyId = new DomainID(uuid);
    String actorId = uuid.toString();

    assertDoesNotThrow(() -> PartyOwnership.requireSameParty(resourcePartyId, actorId));
    assertDoesNotThrow(() -> PartyOwnership.requireSameParty(actorId, actorId));
  }

  @Test
  void shouldThrowForbiddenWhenMismatchOrResourceNull() {
    DomainID resourcePartyId = DomainID.generate();
    String otherActorId = UUID.randomUUID().toString();

    assertThrows(ForbiddenException.class, () -> PartyOwnership.requireSameParty(resourcePartyId, otherActorId));
    assertThrows(ForbiddenException.class, () -> PartyOwnership.requireSameParty((DomainID) null, otherActorId));
    assertThrows(ForbiddenException.class, () -> PartyOwnership.requireSameParty((String) null, otherActorId));
    assertThrows(ForbiddenException.class, () -> PartyOwnership.requireSameParty(resourcePartyId.getValue().toString(), otherActorId));
  }
}
