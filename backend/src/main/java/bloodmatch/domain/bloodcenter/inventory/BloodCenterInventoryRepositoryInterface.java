package bloodmatch.domain.bloodcenter.inventory;

import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.List;
import java.util.Optional;

public interface BloodCenterInventoryRepositoryInterface {

  void save(BloodCenterInventory inventory);

  Optional<BloodCenterInventory> findByOrganizationId(DomainID organizationId);

  List<BloodCenterInventory> findAll();
}
