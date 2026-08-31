package bloodmatch.domain.bloodcenter.inventory;

import bloodmatch.domain.shared.valueObjects.BloodType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BloodInventoryLevelTest {

  @Test
  void mapsPercentageToStockLabel() {
    assertEquals("Crítico", new BloodInventoryLevel(BloodType.of("O-"), 0).label());
    assertEquals("Crítico", new BloodInventoryLevel(BloodType.of("O-"), 29).label());
    assertEquals("Alerta", new BloodInventoryLevel(BloodType.of("O+"), 30).label());
    assertEquals("Alerta", new BloodInventoryLevel(BloodType.of("A+"), 70).label());
    assertEquals("Adequado", new BloodInventoryLevel(BloodType.of("A+"), 71).label());
    assertEquals("Adequado", new BloodInventoryLevel(BloodType.of("AB+"), 100).label());
  }

  @Test
  void clampsPercentageBetweenZeroAndOneHundred() {
    assertEquals(0, new BloodInventoryLevel(BloodType.of("O+"), -10).percentage());
    assertEquals(100, new BloodInventoryLevel(BloodType.of("O+"), 140).percentage());
  }
}
