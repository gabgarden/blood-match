package bloodmatch.application.usecase.bloodcenter.inventory;

import bloodmatch.domain.bloodcenter.inventory.BloodCenterInventory;
import bloodmatch.domain.bloodcenter.inventory.BloodCenterInventoryRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GetBloodCentersInventoryOverviewUseCase {

  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final BloodCenterInventoryRepositoryInterface inventoryRepository;

  public GetBloodCentersInventoryOverviewUseCase(
      BloodCenterRepositoryInterface bloodCenterRepository,
      BloodCenterInventoryRepositoryInterface inventoryRepository) {
    this.bloodCenterRepository = bloodCenterRepository;
    this.inventoryRepository = inventoryRepository;
  }

  public List<BloodCenterInventoryOutput> execute() {
    Map<String, BloodCenterInventory> inventoriesByOrg = inventoryRepository.findAll().stream()
        .collect(Collectors.toMap(
            inventory -> inventory.getOrganizationId().getValue().toString(),
            inventory -> inventory,
            (left, right) -> left));

    List<BloodCenter> bloodCenters = bloodCenterRepository.findAll();
    return bloodCenters.stream()
        .map(center -> {
          DomainID organizationId = center.getOrganization().getId();
          BloodCenterInventory inventory = inventoriesByOrg.get(organizationId.getValue().toString());
          return BloodCenterInventoryOutput.from(center, inventory);
        })
        .toList();
  }
}
