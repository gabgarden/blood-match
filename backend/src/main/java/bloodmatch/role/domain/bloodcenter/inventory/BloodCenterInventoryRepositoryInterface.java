package bloodmatch.role.domain.bloodcenter.inventory;

import bloodmatch.shared.domain.valueObjects.DomainID;

import java.util.List;
import java.util.Optional;

public interface BloodCenterInventoryRepositoryInterface {

  void save(BloodCenterInventory inventory);

  Optional<BloodCenterInventory> findByOrganizationId(DomainID organizationId);

  List<BloodCenterInventory> findAll();
}
