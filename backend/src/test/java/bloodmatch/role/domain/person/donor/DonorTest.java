package bloodmatch.role.domain.person.donor;

import bloodmatch.party.domain.Person;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonorTest {

  @Test
  void constructorShouldSetWeightUpdatedAtToToday() {
    Donor donor = newDonor(70.0);

    assertEquals(LocalDate.now(), donor.getWeightUpdatedAt());
  }

  @Test
  void updateProfileShouldRefreshWeightUpdatedAtOnlyWhenWeightChanges() {
    Donor donor = newDonor(70.0);
    LocalDate original = donor.getWeightUpdatedAt();

    donor.updateProfile(BloodType.of("A+"), 70.0);
    assertEquals(original, donor.getWeightUpdatedAt());

    donor.updateProfile(BloodType.of("A+"), 72.5);
    assertEquals(LocalDate.now(), donor.getWeightUpdatedAt());
    assertEquals(72.5, donor.getWeight());
  }

  @Test
  void reconstituteShouldKeepPersistedWeightUpdatedAt() {
    LocalDate stored = LocalDate.now().minusDays(10);
    Donor donor = Donor.reconstitute(
        person(),
        BloodType.of("O+"),
        70.0,
        null,
        null,
        DomainID.generate(),
        stored);

    assertEquals(stored, donor.getWeightUpdatedAt());
  }

  @Test
  void reconstituteShouldUseTodayWhenWeightUpdatedAtIsNull() {
    Donor donor = Donor.reconstitute(
        person(),
        BloodType.of("O+"),
        70.0,
        null,
        null,
        DomainID.generate(),
        null);

    assertEquals(LocalDate.now(), donor.getWeightUpdatedAt());
  }

  @Test
  void shouldRejectInvalidConstructorArguments() {
    Person p = person();
    BloodType bt = BloodType.of("O+");
    DomainID id = DomainID.generate();

    assertThrows(IllegalArgumentException.class, () -> new Donor(p, null, 70.0));
    assertThrows(IllegalArgumentException.class, () -> new Donor(p, bt, 49.9));
    assertThrows(IllegalArgumentException.class, () -> new Donor(p, null, 70.0, id));
    assertThrows(IllegalArgumentException.class, () -> new Donor(p, bt, 49.9, id));
  }

  @Test
  void shouldValidateCanDonateTo() {
    Donor oMinus = new Donor(person(), BloodType.of("O-"), 70.0);
    assertTrue(oMinus.canDonateTo(BloodType.of("A+")));
    assertTrue(oMinus.canDonateTo(BloodType.of("AB+")));

    Donor aPlus = new Donor(person(), BloodType.of("A+"), 70.0);
    assertFalse(aPlus.canDonateTo(BloodType.of("O+")));
  }

  @Test
  void shouldCheckEligibilityByAgeAndInterval() {
    LocalDate today = LocalDate.of(2026, 4, 1);

    // Too young: 15 years old
    Person young = new Person("Young", new PhoneNumber("11999990000"), new CPF("11122233344"), today.minusYears(15));
    Donor youngDonor = new Donor(young, BloodType.of("A+"), 60.0);
    assertFalse(youngDonor.isEligibleToDonate(today));

    // Too old: 70 years old
    Person old = new Person("Old", new PhoneNumber("11999990000"), new CPF("11122233344"), today.minusYears(70));
    Donor oldDonor = new Donor(old, BloodType.of("A+"), 60.0);
    assertFalse(oldDonor.isEligibleToDonate(today));

    // Valid age: 25 years old, never donated
    Person valid = new Person("Valid", new PhoneNumber("11999990000"), new CPF("11122233344"), today.minusYears(25));
    Donor validDonor = new Donor(valid, BloodType.of("A+"), 60.0);
    assertTrue(validDonor.isEligibleToDonate(today));

    // Valid age, donated recently (1 month ago, interval is typically 2-3 months)
    validDonor.registerDonation(today.minusMonths(1), today);
    assertFalse(validDonor.isEligibleToDonate(today));

    // Valid age, donated long ago (6 months ago)
    validDonor.registerDonation(today.minusMonths(6), today);
    assertTrue(validDonor.isEligibleToDonate(today));
  }

  @Test
  void shouldValidateRegisterDonation() {
    Donor donor = newDonor(70.0);
    LocalDate today = LocalDate.of(2026, 4, 1);

    assertThrows(IllegalArgumentException.class, () -> donor.registerDonation(null, today));
    assertThrows(IllegalArgumentException.class, () -> donor.registerDonation(today, null));
    assertThrows(IllegalArgumentException.class, () -> donor.registerDonation(today.plusDays(1), today));

    donor.registerDonation(today.minusDays(5));
    assertEquals(today.minusDays(5), donor.getLastDonationDate());
  }

  @Test
  void shouldValidateUpdateBloodTypeAndWeight() {
    Donor donor = newDonor(70.0);

    assertThrows(IllegalArgumentException.class, () -> donor.updateBloodType(null));
    donor.updateBloodType(BloodType.of("AB-"));
    assertEquals(BloodType.of("AB-"), donor.getBloodType());

    assertThrows(IllegalArgumentException.class, () -> donor.updateWeight(40.0));
    donor.updateWeight(75.0);
    assertEquals(75.0, donor.getWeight());

    assertThrows(IllegalArgumentException.class, () -> donor.updateProfile(null, 75.0));
    assertThrows(IllegalArgumentException.class, () -> donor.updateProfile(BloodType.of("A+"), 45.0));
  }

  @Test
  void shouldValidateMaxRecommendationDistance() {
    Donor donor = newDonor(70.0);
    assertEquals(Donor.DEFAULT_MAX_RECOMMENDATION_DISTANCE_KM, donor.getMaxRecommendationDistanceKm());

    assertThrows(IllegalArgumentException.class, () -> donor.updateMaxRecommendationDistanceKm(0));
    assertThrows(IllegalArgumentException.class, () -> donor.updateMaxRecommendationDistanceKm(-5.0));

    donor.updateMaxRecommendationDistanceKm(50.0);
    assertEquals(50.0, donor.getMaxRecommendationDistanceKm());
  }

  @Test
  void shouldSupportReconstituteWithAllFields() {
    Person p = person();
    DomainID id = DomainID.generate();
    LocalDate lastDonation = LocalDate.of(2026, 1, 15);
    LocalDate weightUpdated = LocalDate.of(2026, 2, 1);

    Donor reconstituted = Donor.reconstitute(
        p, BloodType.of("B+"), 65.0, lastDonation, 45.0, id, weightUpdated, 2L);

    assertEquals(p, reconstituted.getPerson());
    assertEquals(BloodType.of("B+"), reconstituted.getBloodType());
    assertEquals(65.0, reconstituted.getWeight());
    assertEquals(lastDonation, reconstituted.getLastDonationDate());
    assertEquals(45.0, reconstituted.getMaxRecommendationDistanceKm());
    assertEquals(id, reconstituted.getId());
    assertEquals(weightUpdated, reconstituted.getWeightUpdatedAt());
    assertEquals(2L, reconstituted.getVersion());
  }

  private static Donor newDonor(double weight) {
    return new Donor(person(), BloodType.of("O+"), weight);
  }

  private static Person person() {
    return new Person(
        "Donor",
        new PhoneNumber("11988887777"),
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1));
  }
}
