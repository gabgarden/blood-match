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
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
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
    DonationRequest oldest = request(center, currentDate.minusDays(2), currentDate.plusDays(5), 1);
    DonationRequest newest = request(center, currentDate.minusDays(1), currentDate.plusDays(5), 1);
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
    DonationRequest expired = request(center, currentDate.minusDays(5), currentDate.minusDays(1), 1);
    Donation donation = Donation.registerExternalDonation(donor(), currentDate.minusDays(4), center, currentDate);

    Map<?, DonationRequestFulfillmentStatusRecord> result = service.calculate(
        List.of(expired), List.of(donation), currentDate);

    assertEquals(0, result.get(expired.getId()).fulfilledBloodBags());
  }

  @Test
  void shouldStopAtRequestGoalAndCarryOverToTheNextCompatibleRequest() {
    BloodCenter center = bloodCenter();
    DonationRequest first = request(center, currentDate.minusDays(3), currentDate.plusDays(5), 1);
    DonationRequest second = request(center, currentDate.minusDays(2), currentDate.plusDays(5), 2);
    DonationRequest third = request(center, currentDate.minusDays(1), currentDate.plusDays(5), 1);

    List<Donation> donations = List.of(
        Donation.registerExternalDonation(donor(), currentDate.minusDays(2), center, currentDate),
        Donation.registerExternalDonation(donor(), currentDate.minusDays(1), center, currentDate),
        Donation.registerExternalDonation(donor(), currentDate, center, currentDate));

    Map<?, DonationRequestFulfillmentStatusRecord> result = service.calculate(
        List.of(third, second, first), donations, currentDate);

    assertEquals(1, result.get(first.getId()).fulfilledBloodBags());
    assertTrue(result.get(first.getId()).goalReached());
    assertEquals(2, result.get(second.getId()).fulfilledBloodBags());
    assertTrue(result.get(second.getId()).goalReached());
    assertEquals(0, result.get(third.getId()).fulfilledBloodBags());
    assertFalse(result.get(third.getId()).goalReached());
  }

  @Test
  void shouldSynchronizePersistedFulfillmentCountersOnRequests() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(center, currentDate.minusDays(2), currentDate.plusDays(5), 1);
    Donation donation = Donation.registerExternalDonation(donor(), currentDate, center, currentDate);

    service.synchronize(List.of(request), List.of(donation), currentDate);

    assertEquals(1, request.getFulfilledBloodBags());
    assertTrue(request.isGoalReached());
  }

    private DonationRequest request(BloodCenter center, LocalDate requestedAt, LocalDate limit, int goal) {
    return DonationRequest.create(
        new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
      center, BloodType.of("A+"), goal, limit, requestedAt, Urgency.MEDIUM, null);
  }

  private Donor donor() {
    return new Donor(
        new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
        BloodType.of("O-"), 70.0);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }
}
