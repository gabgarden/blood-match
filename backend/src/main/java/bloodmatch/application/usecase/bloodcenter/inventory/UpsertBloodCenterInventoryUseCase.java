package bloodmatch.application.usecase.bloodcenter.inventory;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.application.shared.PartyOwnership;
import bloodmatch.domain.bloodcenter.inventory.BloodCenterInventory;
import bloodmatch.domain.bloodcenter.inventory.BloodCenterInventoryRepositoryInterface;
import bloodmatch.domain.bloodcenter.inventory.BloodInventoryLevel;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UpsertBloodCenterInventoryUseCase {

  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final BloodCenterInventoryRepositoryInterface inventoryRepository;

  public UpsertBloodCenterInventoryUseCase(
      BloodCenterRepositoryInterface bloodCenterRepository,
      BloodCenterInventoryRepositoryInterface inventoryRepository) {
    this.bloodCenterRepository = bloodCenterRepository;
    this.inventoryRepository = inventoryRepository;
  }

  public BloodCenterInventoryOutput execute(Input input) {
    return execute(input, LocalDateTime.now());
  }

  public BloodCenterInventoryOutput execute(Input input, LocalDateTime updatedAt) {
    if (input == null) {
      throw new ValidationException("Request body cannot be null");
    }
    if (updatedAt == null) {
      throw new ValidationException("updatedAt cannot be null");
    }

    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");
    PartyOwnership.requireSameParty(organizationId, input.actorPartyId());

    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    List<BloodInventoryLevel> items = parseItems(input.items());
    BloodCenterInventory inventory = inventoryRepository.findByOrganizationId(organizationId)
        .orElse(null);
    if (inventory == null) {
      inventory = BloodCenterInventory.create(organizationId, items, updatedAt);
    } else {
      inventory.replaceLevels(items, updatedAt);
    }

    inventoryRepository.save(inventory);
    return BloodCenterInventoryOutput.from(bloodCenter, inventory);
  }

  private static List<BloodInventoryLevel> parseItems(List<ItemInput> itemInputs) {
    Map<String, Integer> byType = new LinkedHashMap<>();
    for (BloodType type : BloodType.all()) {
      byType.put(type.getType(), 0);
    }

    if (itemInputs != null) {
      for (ItemInput itemInput : itemInputs) {
        if (itemInput == null) {
          throw new ValidationException("items cannot contain null");
        }
        if (itemInput.bloodType() == null || itemInput.bloodType().isBlank()) {
          throw new ValidationException("bloodType cannot be blank");
        }

        BloodType bloodType;
        try {
          bloodType = BloodType.of(itemInput.bloodType());
        } catch (IllegalArgumentException ex) {
          throw new ValidationException("Invalid blood type");
        }

        int percentage = itemInput.percentage() == null ? 0 : itemInput.percentage();
        byType.put(bloodType.getType(), Math.max(0, Math.min(100, percentage)));
      }
    }

    List<BloodInventoryLevel> items = new ArrayList<>();
    for (BloodType type : BloodType.all()) {
      items.add(new BloodInventoryLevel(type, byType.get(type.getType())));
    }
    return items;
  }

  public record Input(String organizationId, List<ItemInput> items, String actorPartyId) {
  }

  public record ItemInput(String bloodType, Integer percentage) {
  }
}
