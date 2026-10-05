package bloodmatch.donation.domain;

import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
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
  void shouldValidateDateCombinationsOnReconstitute() {
    DomainID id = DomainID.generate();
    Donor d = donor();
    BloodCenter bc = bloodCenter();

    // Completed and cancelled
    assertThrows(IllegalArgumentException.class, () -> Donation.reconstitute(id, d, currentDate, currentDate, currentDate, bc));
    // Cancelled without intendedDate
    assertThrows(IllegalArgumentException.class, () -> Donation.reconstitute(id, d, null, null, currentDate, bc));
    // Neither intended nor donation date
    assertThrows(IllegalArgumentException.class, () -> Donation.reconstitute(id, d, null, null, null, bc));
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
    assertThrows(IllegalArgumentException.class, () -> Donation.reconstitute(
        DomainID.generate(), donor(), null, null, currentDate, bloodCenter()));
  }

  @Test
  void shouldRejectNullArgumentsOnCreate() {
    LocalDate future = currentDate.plusDays(1);
    assertThrows(IllegalArgumentException.class, () -> Donation.create(null, bloodCenter(), future, null, currentDate));
    assertThrows(IllegalArgumentException.class, () -> Donation.create(donor(), null, future, null, currentDate));
    assertThrows(IllegalArgumentException.class, () -> Donation.create(donor(), bloodCenter(), future, null, null));
  }

  @Test
  void shouldRejectPastIntendedDateAndFutureDonationDate() {
    LocalDate past = currentDate.minusDays(1);
    LocalDate future = currentDate.plusDays(1);

    assertThrows(IllegalArgumentException.class, () -> Donation.create(donor(), bloodCenter(), past, null, currentDate));
    assertThrows(IllegalArgumentException.class, () -> Donation.create(donor(), bloodCenter(), null, future, currentDate));
    assertThrows(IllegalArgumentException.class, () -> Donation.create(donor(), bloodCenter(), null, past, java.time.LocalTime.of(10, 0), currentDate));
  }

  @Test
  void shouldCreateWithExpectedTime() {
    java.time.LocalTime time = java.time.LocalTime.of(14, 30);
    Donation donation = Donation.create(donor(), bloodCenter(), currentDate.plusDays(1), null, time, currentDate);

    assertEquals(time, donation.getExpectedTime());
    assertEquals(currentDate.plusDays(1), donation.getReferenceDate());
  }

  @Test
  void shouldRejectNullArgumentsOnReconstitute() {
    DomainID id = DomainID.generate();
    Donor d = donor();
    BloodCenter bc = bloodCenter();

    assertThrows(IllegalArgumentException.class, () -> Donation.reconstitute(null, d, currentDate, null, null, bc));
    assertThrows(IllegalArgumentException.class, () -> Donation.reconstitute(id, null, currentDate, null, null, bc));
    assertThrows(IllegalArgumentException.class, () -> Donation.reconstitute(id, d, currentDate, null, null, null));
  }

  @Test
  void shouldSupportReconstituteOverloadsAndGetters() {
    DomainID id = DomainID.generate();
    Donor d = donor();
    BloodCenter bc = bloodCenter();
    java.time.LocalTime time = java.time.LocalTime.of(9, 15);

    Donation d1 = Donation.reconstitute(id, d, currentDate, null, null, bc, time);
    assertEquals(time, d1.getExpectedTime());

    Donation d2 = Donation.reconstitute(id, d, null, currentDate, null, bc, time, 2L);
    assertEquals(2L, d2.getVersion());
    assertEquals(currentDate, d2.getReferenceDate());
    assertEquals(bc, d2.getBloodCenter());
    assertEquals(d, d2.getDonor());
  }

  @Test
  void shouldValidateCompleteParametersAndState() {
    Donation donation = Donation.create(donor(), bloodCenter(), currentDate.plusDays(1), null, currentDate);

    assertThrows(IllegalArgumentException.class, () -> donation.complete(null, currentDate));
    assertThrows(IllegalArgumentException.class, () -> donation.complete(currentDate, null));
    assertThrows(IllegalArgumentException.class, () -> donation.complete(currentDate.plusDays(1), currentDate));

    donation.complete(currentDate, currentDate);
    assertThrows(IllegalStateException.class, () -> donation.complete(currentDate, currentDate));
  }

  @Test
  void shouldReschedulePendingDonation() {
    Donation donation = Donation.create(donor(), bloodCenter(), currentDate.plusDays(1), null, currentDate);
    LocalDate newDate = currentDate.plusDays(5);
    java.time.LocalTime newTime = java.time.LocalTime.of(11, 0);

    assertThrows(IllegalArgumentException.class, () -> donation.reschedule(null, currentDate));
    assertThrows(IllegalArgumentException.class, () -> donation.reschedule(newDate, null));
    assertThrows(IllegalArgumentException.class, () -> donation.reschedule(currentDate.minusDays(1), currentDate));

    donation.reschedule(newDate, currentDate);
    assertEquals(newDate, donation.getIntendedDate());

    donation.reschedule(newDate.plusDays(1), currentDate, newTime);
    assertEquals(newDate.plusDays(1), donation.getIntendedDate());
    assertEquals(newTime, donation.getExpectedTime());

    donation.cancel(currentDate);
    assertThrows(IllegalStateException.class, () -> donation.reschedule(newDate.plusDays(2), currentDate));
  }

  @Test
  void shouldValidateCancelParameters() {
    Donation donation = Donation.create(donor(), bloodCenter(), currentDate.plusDays(1), null, currentDate);
    assertThrows(IllegalArgumentException.class, () -> donation.cancel(null));

    donation.cancel(currentDate);
    assertEquals(currentDate, donation.getCancelledAt());
    assertTrue(donation.isCancelled());
    assertEquals("CANCELLED", donation.status());
    assertThrows(IllegalStateException.class, () -> donation.cancel(currentDate));
  }

  private Donor donor() {
    return new Donor(new Person("Donor", new PhoneNumber("11988887777"), new CPF("12345678901"), currentDate.minusYears(30)),
        BloodType.of("O-"), 70.0);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }
}
