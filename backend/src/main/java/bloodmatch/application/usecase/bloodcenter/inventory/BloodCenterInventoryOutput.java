package bloodmatch.application.usecase.bloodcenter.inventory;

import bloodmatch.domain.bloodcenter.inventory.BloodCenterInventory;
import bloodmatch.domain.bloodcenter.inventory.BloodInventoryLevel;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.shared.valueObjects.Address;

import java.time.LocalDateTime;
import java.util.List;

public record BloodCenterInventoryOutput(
    String organizationId,
    String name,
    String city,
    String state,
    LocalDateTime updatedAt,
    List<ItemOutput> items) {

  public record ItemOutput(String bloodType, int percentage, String label) {
  }

  public static BloodCenterInventoryOutput from(BloodCenter bloodCenter, BloodCenterInventory inventory) {
    Organization organization = bloodCenter.getOrganization();
    Address address = organization.getAddress();
    return new BloodCenterInventoryOutput(
        organization.getId().getValue().toString(),
        organization.getName(),
        address == null ? null : address.getCity(),
        address == null ? null : address.getState(),
        inventory == null ? null : inventory.getUpdatedAt(),
        BloodCenterInventory.levelsOrDefaults(inventory).stream()
            .map(BloodCenterInventoryOutput::toItem)
            .toList());
  }

  private static ItemOutput toItem(BloodInventoryLevel level) {
    return new ItemOutput(level.bloodType().getType(), level.percentage(), level.label());
  }
}
