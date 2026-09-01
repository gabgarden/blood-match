package bloodmatch.domain.roles.requester;

import bloodmatch.domain.roles.PartyRole;
import bloodmatch.domain.shared.valueObjects.DomainID;

import bloodmatch.domain.party.Party;

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