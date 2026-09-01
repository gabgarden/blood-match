package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.bloodcenter.inventory.BloodCenterInventory;
import bloodmatch.domain.bloodcenter.inventory.BloodInventoryLevel;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "blood_center_inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BloodCenterInventorySchema {

  @Id
  private String organizationId;
  @Version
  private Long version;
  private List<LevelDocument> levels;
  private LocalDateTime updatedAt;

  public BloodCenterInventorySchema(BloodCenterInventory inventory) {
    if (inventory == null) {
      throw new IllegalArgumentException("Inventory cannot be null");
    }
    this.organizationId = inventory.getOrganizationId().getValue().toString();
    this.version = inventory.getVersion();
    this.updatedAt = inventory.getUpdatedAt();
    this.levels = inventory.getLevels().stream()
        .map(level -> new LevelDocument(level.bloodType().getType(), level.percentage()))
        .toList();
  }

  public BloodCenterInventory toDomain() {
    DomainID orgId = new DomainID(UUID.fromString(this.organizationId));
    List<BloodInventoryLevel> domainLevels = new ArrayList<>();
    if (this.levels != null) {
      for (LevelDocument level : this.levels) {
        if (level == null || level.bloodType == null) {
          continue;
        }
        domainLevels.add(new BloodInventoryLevel(BloodType.of(level.bloodType), level.percentage));
      }
    }
    return BloodCenterInventory.reconstitute(orgId, domainLevels, this.updatedAt, this.version);
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LevelDocument {
    private String bloodType;
    private int percentage;
  }
}
