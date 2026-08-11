package bloodmatch.domain.repositories;

import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.Optional;

public interface RequesterRepositoryInterface {

  Optional<Requester> findByPartyId(DomainID partyId);

  void save(Requester requester);
}
