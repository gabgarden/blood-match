package bloodmatch.domain.roles.requester;

import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.Optional;

public interface RequesterRepositoryInterface {

  Optional<Requester> findByPartyId(DomainID partyId);

  void save(Requester requester);
}
