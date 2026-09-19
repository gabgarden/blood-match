package bloodmatch.role.domain;

import bloodmatch.party.domain.Party;
import bloodmatch.shared.domain.entity.DomainObject;
import bloodmatch.shared.domain.valueObjects.DomainID;

public abstract class PartyRole<T extends Party> extends DomainObject {

  private T party;

  protected PartyRole(T party) {
    this(party, DomainID.generate());
  }

  protected PartyRole(T party, DomainID id) {
    if (party == null)
      throw new IllegalArgumentException("Party cannot be null");
    if (id == null)
      throw new IllegalArgumentException("Role id cannot be null");

    this.party = party;
    this.id = id;
  }

  public T getParty() {
    return party;
  }

}