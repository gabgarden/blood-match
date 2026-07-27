package bloodmatch.domain.roles;

import bloodmatch.domain.party.Party;
import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.DomainID;

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