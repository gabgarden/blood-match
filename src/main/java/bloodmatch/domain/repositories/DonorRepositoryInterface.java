package bloodmatch.domain.repositories;

import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.List;
import java.util.Optional;

public interface DonorRepositoryInterface {

  Optional<Donor> findByPartyId(DomainID partyId);

  List<Donor> findAll();

  void save(Donor donor);
}
