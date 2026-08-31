package bloodmatch.domain.roles.person.donor;

import bloodmatch.domain.party.Person;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
