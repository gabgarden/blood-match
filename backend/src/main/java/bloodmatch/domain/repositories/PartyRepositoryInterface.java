package bloodmatch.domain.repositories;

import bloodmatch.domain.party.Party;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.Optional;

public interface PartyRepositoryInterface {

  Optional<Party> findById(DomainID partyId);

  void save(Party party);
}
