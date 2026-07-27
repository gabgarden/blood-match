package bloodmatch.domain.repositories;

import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.Optional;

public interface BloodCenterRepositoryInterface {

  Optional<BloodCenter> findByPartyId(DomainID partyId);

  void save(BloodCenter bloodCenter);
}
