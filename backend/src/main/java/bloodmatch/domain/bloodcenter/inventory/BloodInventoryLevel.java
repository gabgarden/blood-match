package bloodmatch.domain.bloodcenter.inventory;

import bloodmatch.domain.shared.valueObjects.BloodType;

public record BloodInventoryLevel(BloodType bloodType, int percentage) {

  public BloodInventoryLevel {
    if (bloodType == null) {
      throw new IllegalArgumentException("Blood type cannot be null");
    }
    percentage = Math.max(0, Math.min(100, percentage));
  }

  public String label() {
    if (percentage < 30) {
      return "Crítico";
    }
    if (percentage <= 70) {
      return "Alerta";
    }
    return "Adequado";
  }
}
