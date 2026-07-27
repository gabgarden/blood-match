package bloodmatch.domain.services;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.services.records.DonationRequestFulfillmentStatusRecord;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonationRequestFulfillmentServiceTest {

  private final LocalDate currentDate = LocalDate.of(2026, 4, 23);
  private final DonationRequestFulfillmentService service = new DonationRequestFulfillmentService();

  @Test
  void shouldDistributeEachDonationOnlyOnceUsingRequestFifo() {
    BloodCenter center = bloodCenter();
    DonationRequest oldest = request(center, currentDate.minusDays(2), 1);
    DonationRequest newest = request(center, currentDate.minusDays(1), 1);
    Donation donation = Donation.registerExternalDonation(donor(), currentDate, center, currentDate);

    Map<?, DonationRequestFulfillmentStatusRecord> result = service.calculate(
        List.of(newest, oldest), List.of(donation), currentDate);

    assertEquals(1, result.get(oldest.getId()).fulfilledBloodBags());
    assertTrue(result.get(oldest.getId()).goalReached());
    assertEquals(0, result.get(newest.getId()).fulfilledBloodBags());
    assertFalse(result.get(newest.getId()).goalReached());
  }

  @Test
  void shouldIgnoreDonationsForExpiredRequests() {
    BloodCenter center = bloodCenter();
    DonationRequest expired = request(center, currentDate.minusDays(5), 1);
    Donation donation = Donation.registerExternalDonation(donor(), currentDate.minusDays(4), center, currentDate);

    Map<?, DonationRequestFulfillmentStatusRecord> result = service.calculate(
        List.of(expired), List.of(donation), currentDate);

    assertEquals(0, result.get(expired.getId()).fulfilledBloodBags());
  }

  private DonationRequest request(BloodCenter center, LocalDate requestedAt, int goal) {
    return DonationRequest.create(
        new Requester(new Person("Requester", new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
        center, BloodType.of("A+"), goal, requestedAt.plusDays(2), requestedAt, Urgency.MEDIUM);
  }

  private Donor donor() {
    return new Donor(
        new Person("Donor", new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
        BloodType.of("O-"), 70.0);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Center", new CNPJ("12345678000100")));
  }
}
