package bloodmatch.domain.roles;

import bloodmatch.domain.party.Party;
import bloodmatch.domain.shared.entity.DomainObject;

public abstract class PartyRole<T extends Party> extends DomainObject {

  private T party;

  protected PartyRole(T party) {

    if (party == null)
      throw new IllegalArgumentException("Party cannot be null");

    this.party = party;
    this.id = party.getId();
  }

  protected T getParty() {
    return party;
  }

}