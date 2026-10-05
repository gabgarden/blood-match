package bloodmatch.shared.domain.valueObjects;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BloodTypeTest {

  @Test
  void shouldCreateValidBloodType() {

    BloodType type = BloodType.of("A+");

    assertEquals("A+", type.getType());
  }

  @Test
  void shouldIgnoreCaseWhenCreatingBloodType() {

    BloodType type = BloodType.of("o-");

    assertEquals("O-", type.getType());
  }

  @Test
  void shouldThrowExceptionWhenBloodTypeIsNull() {

    assertThrows(
        IllegalArgumentException.class,
        () -> BloodType.of(null));
  }

  @Test
  void shouldThrowExceptionForInvalidBloodType() {

    assertThrows(
        IllegalArgumentException.class,
        () -> BloodType.of("X"));
  }

  @Test
  void shouldThrowExceptionWhenReceiverBloodTypeIsNull() {

    BloodType donor = BloodType.of("O-");

    assertThrows(
        IllegalArgumentException.class,
        () -> donor.canDonateTo(null));
  }

  @Test
  void oNegativeShouldDonateToEveryone() {

    BloodType donor = BloodType.of("O-");

    assertTrue(donor.canDonateTo(BloodType.of("O-")));
    assertTrue(donor.canDonateTo(BloodType.of("O+")));
    assertTrue(donor.canDonateTo(BloodType.of("A-")));
    assertTrue(donor.canDonateTo(BloodType.of("A+")));
    assertTrue(donor.canDonateTo(BloodType.of("B-")));
    assertTrue(donor.canDonateTo(BloodType.of("B+")));
    assertTrue(donor.canDonateTo(BloodType.of("AB-")));
    assertTrue(donor.canDonateTo(BloodType.of("AB+")));
  }

  @Test
  void oPositiveShouldDonateOnlyToPositiveTypes() {

    BloodType donor = BloodType.of("O+");

    assertTrue(donor.canDonateTo(BloodType.of("O+")));
    assertTrue(donor.canDonateTo(BloodType.of("A+")));
    assertTrue(donor.canDonateTo(BloodType.of("B+")));
    assertTrue(donor.canDonateTo(BloodType.of("AB+")));

    assertFalse(donor.canDonateTo(BloodType.of("A-")));
  }

  @Test
  void aPositiveShouldDonateOnlyToAPositiveAndABPositive() {

    BloodType donor = BloodType.of("A+");

    assertTrue(donor.canDonateTo(BloodType.of("A+")));
    assertTrue(donor.canDonateTo(BloodType.of("AB+")));

    assertFalse(donor.canDonateTo(BloodType.of("B+")));
  }

  @Test
  void abPositiveShouldDonateOnlyToAbPositive() {

    BloodType donor = BloodType.of("AB+");

    assertTrue(donor.canDonateTo(BloodType.of("AB+")));

    assertFalse(donor.canDonateTo(BloodType.of("A+")));
    assertFalse(donor.canDonateTo(BloodType.of("O+")));
  }

  @Test
  void aNegativeShouldDonateToAAndABTypes() {
    BloodType donor = BloodType.of("A-");

    assertTrue(donor.canDonateTo(BloodType.of("A-")));
    assertTrue(donor.canDonateTo(BloodType.of("A+")));
    assertTrue(donor.canDonateTo(BloodType.of("AB-")));
    assertTrue(donor.canDonateTo(BloodType.of("AB+")));

    assertFalse(donor.canDonateTo(BloodType.of("B+")));
    assertFalse(donor.canDonateTo(BloodType.of("O+")));
  }

  @Test
  void bNegativeShouldDonateToBAndABTypes() {
    BloodType donor = BloodType.of("B-");

    assertTrue(donor.canDonateTo(BloodType.of("B-")));
    assertTrue(donor.canDonateTo(BloodType.of("B+")));
    assertTrue(donor.canDonateTo(BloodType.of("AB-")));
    assertTrue(donor.canDonateTo(BloodType.of("AB+")));

    assertFalse(donor.canDonateTo(BloodType.of("A+")));
    assertFalse(donor.canDonateTo(BloodType.of("O-")));
  }

  @Test
  void bPositiveShouldDonateOnlyToBPositiveAndABPositive() {
    BloodType donor = BloodType.of("B+");

    assertTrue(donor.canDonateTo(BloodType.of("B+")));
    assertTrue(donor.canDonateTo(BloodType.of("AB+")));

    assertFalse(donor.canDonateTo(BloodType.of("B-")));
    assertFalse(donor.canDonateTo(BloodType.of("A+")));
  }

  @Test
  void abNegativeShouldDonateOnlyToABNegativeAndABPositive() {
    BloodType donor = BloodType.of("AB-");

    assertTrue(donor.canDonateTo(BloodType.of("AB-")));
    assertTrue(donor.canDonateTo(BloodType.of("AB+")));

    assertFalse(donor.canDonateTo(BloodType.of("A-")));
    assertFalse(donor.canDonateTo(BloodType.of("B-")));
  }

  @Test
  void shouldReturnAllBloodTypes() {
    assertEquals(8, BloodType.all().size());
  }

  @Test
  void shouldReturnCompatibleRecipientTypes() {
    BloodType oMinus = BloodType.of("O-");
    assertEquals(8, oMinus.getCompatibleRecipientTypes().size());

    BloodType abPlus = BloodType.of("AB+");
    assertEquals(List.of("AB+"), abPlus.getCompatibleRecipientTypes());
  }

  @Test
  void shouldVerifyEqualsAndHashCode() {
    BloodType a1 = BloodType.of("A+");
    BloodType a2 = BloodType.of("A+");
    BloodType b = BloodType.of("B+");

    assertEquals(a1, a1);
    assertEquals(a1, a2);
    assertEquals(a1.hashCode(), a2.hashCode());
    assertNotEquals(a1, b);
    assertNotEquals(a1, null);
    assertNotEquals(a1, "A+");
  }
}
