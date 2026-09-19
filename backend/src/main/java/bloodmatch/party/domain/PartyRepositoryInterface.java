package bloodmatch.party.domain;

import bloodmatch.shared.domain.valueObjects.DomainID;

import java.util.Optional;

public interface PartyRepositoryInterface {

  Optional<Party> findById(DomainID partyId);

  void save(Party party);
}
