package bloodmatch.domain.donationrequest;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonationRequestTest {

  private final LocalDate requestedAt = LocalDate.of(2026, 4, 10);

  @Test
  void shouldExpireOnlyAfterTheDeadline() {
    DonationRequest request = request(BloodType.of("A+"), requestedAt.plusDays(2));

    assertFalse(request.isExpired(requestedAt.plusDays(2)));
    assertTrue(request.isExpired(requestedAt.plusDays(3)));
  }

  @Test
  void shouldAcceptOnlyCompletedCompatibleDonationsWithinRequestPeriod() {
    DonationRequest request = request(BloodType.of("A+"), requestedAt.plusDays(5));
    Donor compatibleDonor = donor(BloodType.of("O-"));

    Donation withinPeriod = Donation.registerExternalDonation(
        compatibleDonor, requestedAt.plusDays(1), request.getBloodCenter(), requestedAt.plusDays(1));
    Donation beforeRequest = Donation.registerExternalDonation(
        compatibleDonor, requestedAt.minusDays(1), request.getBloodCenter(), requestedAt);

    assertTrue(request.acceptsDonation(withinPeriod, requestedAt.plusDays(1)));
    assertFalse(request.acceptsDonation(beforeRequest, requestedAt));
    request.close();
    assertFalse(request.acceptsDonation(withinPeriod, requestedAt.plusDays(1)));
  }

  private DonationRequest request(BloodType type, LocalDate dateLimit) {
    return DonationRequest.create(
        new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
        new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100"))),
        type, 2, dateLimit, requestedAt, Urgency.MEDIUM, null);
  }

  private Donor donor(BloodType type) {
    return new Donor(
        new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)), type, 70.0);
  }
}
