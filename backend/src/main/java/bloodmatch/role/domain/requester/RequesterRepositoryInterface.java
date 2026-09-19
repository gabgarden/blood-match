package bloodmatch.role.domain.requester;

import bloodmatch.shared.domain.valueObjects.DomainID;

import java.util.Optional;

public interface RequesterRepositoryInterface {

  Optional<Requester> findByPartyId(DomainID partyId);

  void save(Requester requester);
}
