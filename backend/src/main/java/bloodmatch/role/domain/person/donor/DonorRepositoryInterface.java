package bloodmatch.role.domain.person.donor;

import bloodmatch.shared.domain.valueObjects.DomainID;

import java.util.List;
import java.util.Optional;

public interface DonorRepositoryInterface {

  Optional<Donor> findByPartyId(DomainID partyId);

  List<Donor> findAll();

  void save(Donor donor);
}
