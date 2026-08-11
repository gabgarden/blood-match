package bloodmatch.domain.repositories;

import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.List;
import java.util.Optional;

public interface BloodCenterRepositoryInterface {

  Optional<BloodCenter> findByPartyId(DomainID partyId);

  void save(BloodCenter bloodCenter);

  /**
   * Case-insensitive partial name search over organizations that have a blood-center role.
   */
  List<BloodCenterDirectoryEntry> searchByOrganizationName(String query, int limit);
}
