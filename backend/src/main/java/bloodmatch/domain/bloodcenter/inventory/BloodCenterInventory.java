package bloodmatch.domain.bloodcenter.inventory;

import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BloodCenterInventory extends DomainObject {

  private DomainID organizationId;
  private List<BloodInventoryLevel> levels;
  private LocalDateTime updatedAt;

  private BloodCenterInventory() {
  }

  public static BloodCenterInventory create(
      DomainID organizationId,
      List<BloodInventoryLevel> levels,
      LocalDateTime updatedAt) {
    BloodCenterInventory inventory = new BloodCenterInventory();
    inventory.setId(requireOrganizationId(organizationId));
    inventory.organizationId = organizationId;
    inventory.levels = normalizeLevels(levels);
    inventory.updatedAt = requireUpdatedAt(updatedAt);
    return inventory;
  }

  public static BloodCenterInventory reconstitute(
      DomainID organizationId,
      List<BloodInventoryLevel> levels,
      LocalDateTime updatedAt) {
    return reconstitute(organizationId, levels, updatedAt, null);
  }

  public static BloodCenterInventory reconstitute(
      DomainID organizationId,
      List<BloodInventoryLevel> levels,
      LocalDateTime updatedAt,
      Long version) {
    BloodCenterInventory inventory = new BloodCenterInventory();
    inventory.setId(requireOrganizationId(organizationId));
    inventory.organizationId = organizationId;
    inventory.levels = levels == null ? List.of() : List.copyOf(levels);
    inventory.updatedAt = updatedAt;
    inventory.setVersion(version);
    return inventory;
  }

  public void replaceLevels(List<BloodInventoryLevel> levels, LocalDateTime updatedAt) {
    this.levels = normalizeLevels(levels);
    this.updatedAt = requireUpdatedAt(updatedAt);
  }

  public DomainID getOrganizationId() {
    return organizationId;
  }

  public List<BloodInventoryLevel> getLevels() {
    return levels == null ? List.of() : List.copyOf(levels);
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public List<BloodInventoryLevel> normalizedLevels() {
    Map<String, Integer> byType = new HashMap<>();
    for (BloodInventoryLevel level : getLevels()) {
      byType.put(level.bloodType().getType(), level.percentage());
    }

    List<BloodInventoryLevel> normalized = new ArrayList<>();
    for (BloodType type : BloodType.all()) {
      int percentage = byType.getOrDefault(type.getType(), 0);
      normalized.add(new BloodInventoryLevel(type, percentage));
    }
    return List.copyOf(normalized);
  }

  public static List<BloodInventoryLevel> defaultLevels() {
    return BloodType.all().stream()
        .map(type -> new BloodInventoryLevel(type, 0))
        .toList();
  }

  public static List<BloodInventoryLevel> levelsOrDefaults(BloodCenterInventory inventory) {
    if (inventory == null) {
      return defaultLevels();
    }
    return inventory.normalizedLevels();
  }

  private static DomainID requireOrganizationId(DomainID organizationId) {
    if (organizationId == null) {
      throw new IllegalArgumentException("organizationId cannot be null");
    }
    return organizationId;
  }

  private static LocalDateTime requireUpdatedAt(LocalDateTime updatedAt) {
    if (updatedAt == null) {
      throw new IllegalArgumentException("updatedAt cannot be null");
    }
    return updatedAt;
  }

  private static List<BloodInventoryLevel> normalizeLevels(List<BloodInventoryLevel> levels) {
    Map<String, Integer> byType = new HashMap<>();
    for (BloodType type : BloodType.all()) {
      byType.put(type.getType(), 0);
    }
    if (levels != null) {
      for (BloodInventoryLevel level : levels) {
        if (level == null) {
          throw new IllegalArgumentException("levels cannot contain null");
        }
        byType.put(level.bloodType().getType(), level.percentage());
      }
    }
    List<BloodInventoryLevel> normalized = new ArrayList<>();
    for (BloodType type : BloodType.all()) {
      normalized.add(new BloodInventoryLevel(type, byType.get(type.getType())));
    }
    return List.copyOf(normalized);
  }
}
