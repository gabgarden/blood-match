package bloodmatch.domain.donation;

import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonationTest {

  private final LocalDate currentDate = LocalDate.of(2026, 4, 23);

  @Test
  void shouldCreatePendingDonationFromIntendedDate() {
    Donation donation = Donation.create(
        donor(), bloodCenter(), currentDate.plusDays(2), null, currentDate);

    assertTrue(donation.isPending());
    assertFalse(donation.isCompleted());
    assertEquals(currentDate.plusDays(2), donation.getIntendedDate());
    assertNull(donation.getDonationDate());
    assertEquals("PENDING", donation.status());
  }

  @Test
  void shouldCreateCompletedDonationFromActualDate() {
    Donation donation = Donation.create(
        donor(), bloodCenter(), null, currentDate.minusDays(1), currentDate);

    assertTrue(donation.isCompleted());
    assertFalse(donation.isPending());
    assertEquals(currentDate.minusDays(1), donation.getDonationDate());
    assertNull(donation.getIntendedDate());
    assertEquals("COMPLETED", donation.status());
  }

  @Test
  void shouldRejectCreateWhenBothOrNeitherDateIsProvided() {
    assertThrows(IllegalArgumentException.class, () -> Donation.create(
        donor(), bloodCenter(), currentDate, currentDate, currentDate));
    assertThrows(IllegalArgumentException.class, () -> Donation.create(
        donor(), bloodCenter(), null, null, currentDate));
  }

  @Test
  void shouldCompletePendingDonationWithoutErasingIntendedDate() {
    LocalDate intendedDate = currentDate.plusDays(2);
    Donation donation = Donation.create(donor(), bloodCenter(), intendedDate, null, currentDate);

    donation.complete(currentDate, currentDate);

    assertTrue(donation.isCompleted());
    assertFalse(donation.isPending());
    assertEquals(currentDate, donation.getDonationDate());
    assertEquals(intendedDate, donation.getIntendedDate());
  }

  @Test
  void shouldCancelOnlyPendingDonation() {
    Donation donation = Donation.create(donor(), bloodCenter(), currentDate, null, currentDate);

    donation.cancel(currentDate);

    assertTrue(donation.isCancelled());
    assertFalse(donation.isPending());
    assertEquals(currentDate, donation.getCancelledAt());
    assertThrows(IllegalStateException.class, () -> donation.cancel(currentDate));
  }

  @Test
  void shouldRejectInvalidReconstitutedDates() {
    assertThrows(IllegalArgumentException.class, () -> Donation.reconstitute(
        DomainID.generate(), donor(), null, currentDate, currentDate, bloodCenter()));
    assertThrows(IllegalArgumentException.class, () -> Donation.reconstitute(
        DomainID.generate(), donor(), null, null, null, bloodCenter()));
  }

  private Donor donor() {
    return new Donor(new Person("Donor", new PhoneNumber("11988887777"), new CPF("12345678901"), currentDate.minusYears(30)),
        BloodType.of("O-"), 70.0);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }
}
