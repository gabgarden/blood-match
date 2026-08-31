package bloodmatch.domain.shared.valueObjects;

import org.junit.jupiter.api.Test;

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

}
