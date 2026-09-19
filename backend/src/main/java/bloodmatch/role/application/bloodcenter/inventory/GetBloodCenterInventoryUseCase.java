package bloodmatch.role.application.bloodcenter.inventory;

import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.application.shared.DomainIdParser;
import bloodmatch.role.domain.bloodcenter.inventory.BloodCenterInventory;
import bloodmatch.role.domain.bloodcenter.inventory.BloodCenterInventoryRepositoryInterface;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.shared.domain.valueObjects.DomainID;
import org.springframework.stereotype.Service;

@Service
public class GetBloodCenterInventoryUseCase {

  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final BloodCenterInventoryRepositoryInterface inventoryRepository;

  public GetBloodCenterInventoryUseCase(
      BloodCenterRepositoryInterface bloodCenterRepository,
      BloodCenterInventoryRepositoryInterface inventoryRepository) {
    this.bloodCenterRepository = bloodCenterRepository;
    this.inventoryRepository = inventoryRepository;
  }

  public BloodCenterInventoryOutput execute(Input input) {
    if (input == null) {
      throw new ValidationException("Input cannot be null");
    }

    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");
    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    BloodCenterInventory inventory = inventoryRepository.findByOrganizationId(organizationId).orElse(null);
    return BloodCenterInventoryOutput.from(bloodCenter, inventory);
  }

  public record Input(String organizationId) {
  }
}
