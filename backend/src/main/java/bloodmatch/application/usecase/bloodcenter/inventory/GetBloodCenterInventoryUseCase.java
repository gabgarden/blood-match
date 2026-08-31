package bloodmatch.application.usecase.bloodcenter.inventory;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.bloodcenter.inventory.BloodCenterInventory;
import bloodmatch.domain.bloodcenter.inventory.BloodCenterInventoryRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
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
