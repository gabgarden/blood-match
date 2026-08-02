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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonationTest {

  private final LocalDate currentDate = LocalDate.of(2026, 4, 23);

  @Test
  void shouldCompletePendingDonationAndReplaceExpectedDate() {
    Donation donation = Donation.createPending(donor(), currentDate.plusDays(2), bloodCenter(), currentDate);

    donation.complete(currentDate, currentDate);

    assertTrue(donation.isCompleted());
    assertFalse(donation.isPending());
    assertFalse(donation.isCancelled());
    assertEquals(currentDate, donation.getDonationDate());
  }

  @Test
  void shouldCancelOnlyPendingDonation() {
    Donation donation = Donation.createPending(donor(), currentDate, bloodCenter(), currentDate);

    donation.cancel();

    assertTrue(donation.isCancelled());
    assertFalse(donation.isPending());
    assertThrows(IllegalStateException.class, donation::cancel);
  }

  @Test
  void shouldRejectInvalidReconstitutedStatusFlags() {
    assertThrows(IllegalArgumentException.class, () -> Donation.reconstitute(
        DomainID.generate(), donor(), currentDate, bloodCenter(), true, true, false));
  }

  private Donor donor() {
    return new Donor(new Person("Donor", new PhoneNumber("11988887777"), new CPF("12345678901"), currentDate.minusYears(30)),
        BloodType.of("O-"), 70.0);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }
}
