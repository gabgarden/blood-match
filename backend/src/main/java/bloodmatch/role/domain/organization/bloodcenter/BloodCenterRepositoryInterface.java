package bloodmatch.role.domain.organization.bloodcenter;

import bloodmatch.shared.domain.valueObjects.DomainID;

import java.util.List;
import java.util.Optional;

public interface BloodCenterRepositoryInterface {

  Optional<BloodCenter> findByPartyId(DomainID partyId);

  void save(BloodCenter bloodCenter);

  List<BloodCenter> findAll();

  /**
   * Case-insensitive partial name search over organizations that have a blood-center role.
   */
  List<BloodCenterDirectoryEntry> searchByOrganizationName(String query, int limit);
}
