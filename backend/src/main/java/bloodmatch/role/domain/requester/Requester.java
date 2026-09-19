package bloodmatch.role.domain.requester;

import bloodmatch.role.domain.PartyRole;
import bloodmatch.shared.domain.valueObjects.DomainID;

import bloodmatch.party.domain.Party;

public class Requester extends PartyRole<Party> {

  public Requester(Party party) {
    super(party);
  }

  protected Requester(Party party, DomainID id) {
    super(party, id);
  }

  public static Requester reconstitute(Party party, DomainID id) {
    return reconstitute(party, id, null);
  }

  public static Requester reconstitute(Party party, DomainID id, Long version) {
    Requester requester = new Requester(party, id);
    requester.setVersion(version);
    return requester;
  }

}