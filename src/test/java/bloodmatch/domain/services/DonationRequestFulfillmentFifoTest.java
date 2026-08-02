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
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonationRequestFulfillmentFifoTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);
  private final DonationRequestFulfillmentService service = new DonationRequestFulfillmentService();

  @Test
  void assignsDonationToTheOldestCompatibleRequestAtTheSameBloodCenter() {
    BloodCenter center = center("A");
    DonationRequest oldest = request(1, center, "A+", 1, TODAY.minusDays(2), TODAY.plusDays(2), true);
    DonationRequest newest = request(2, center, "A+", 1, TODAY.minusDays(1), TODAY.plusDays(2), true);

    var result = calculate(List.of(newest, oldest), List.of(donation(1, center, "O-", TODAY)));

    assertFulfilled(result, oldest, 1, true);
    assertFulfilled(result, newest, 0, false);
  }

  @Test
  void skipsAnOlderRequestWhenItsBloodTypeIsIncompatible() {
    BloodCenter center = center("A");
    DonationRequest incompatibleOldest = request(1, center, "O-", 1, TODAY.minusDays(2), TODAY.plusDays(2), true);
    DonationRequest compatibleNewest = request(2, center, "A+", 1, TODAY.minusDays(1), TODAY.plusDays(2), true);

    var result = calculate(List.of(incompatibleOldest, compatibleNewest), List.of(donation(1, center, "A+", TODAY)));

    assertFulfilled(result, incompatibleOldest, 0, false);
    assertFulfilled(result, compatibleNewest, 1, true);
  }

  @Test
  void fillsTheOldestGoalBeforeMovingToTheNextCompatibleRequest() {
    BloodCenter center = center("A");
    DonationRequest oldest = request(1, center, "A+", 2, TODAY.minusDays(2), TODAY.plusDays(2), true);
    DonationRequest next = request(2, center, "A+", 2, TODAY.minusDays(1), TODAY.plusDays(2), true);
    List<Donation> donations = List.of(
        donation(1, center, "O-", TODAY),
        donation(2, center, "O-", TODAY),
        donation(3, center, "O-", TODAY));

    var result = calculate(List.of(next, oldest), donations);

    assertFulfilled(result, oldest, 2, true);
    assertFulfilled(result, next, 1, false);
  }

  @Test
  void consumesEachDonationOnlyOnce() {
    BloodCenter center = center("A");
    DonationRequest first = request(1, center, "A+", 1, TODAY.minusDays(2), TODAY.plusDays(2), true);
    DonationRequest second = request(2, center, "A+", 1, TODAY.minusDays(1), TODAY.plusDays(2), true);

    var result = calculate(List.of(first, second), List.of(donation(1, center, "O-", TODAY)));

    assertEquals(1, result.get(first.getId()).fulfilledBloodBags()
        + result.get(second.getId()).fulfilledBloodBags());
  }

  @Test
  void neverAllocatesDonationToARequestFromAnotherBloodCenter() {
    BloodCenter firstCenter = center("A");
    BloodCenter secondCenter = center("B");
    DonationRequest firstRequest = request(1, firstCenter, "A+", 1, TODAY.minusDays(1), TODAY.plusDays(2), true);
    DonationRequest secondRequest = request(2, secondCenter, "A+", 1, TODAY.minusDays(1), TODAY.plusDays(2), true);

    var result = calculate(
        List.of(firstRequest, secondRequest),
        List.of(donation(1, firstCenter, "O-", TODAY)));

    assertFulfilled(result, firstRequest, 1, true);
    assertFulfilled(result, secondRequest, 0, false);
  }

  @Test
  void allocatesIndependentlyForEachBloodCenter() {
    BloodCenter firstCenter = center("A");
    BloodCenter secondCenter = center("B");
    DonationRequest firstRequest = request(1, firstCenter, "A+", 1, TODAY.minusDays(1), TODAY.plusDays(2), true);
    DonationRequest secondRequest = request(2, secondCenter, "B+", 1, TODAY.minusDays(1), TODAY.plusDays(2), true);

    var result = calculate(
        List.of(secondRequest, firstRequest),
        List.of(donation(2, secondCenter, "O-", TODAY), donation(1, firstCenter, "O-", TODAY)));

    assertFulfilled(result, firstRequest, 1, true);
    assertFulfilled(result, secondRequest, 1, true);
  }

  @Test
  void acceptsDonationsOnTheRequestStartAndLimitDates() {
    BloodCenter center = center("A");
    DonationRequest request = request(1, center, "A+", 2, TODAY.minusDays(2), TODAY, true);

    var result = calculate(
        List.of(request),
        List.of(donation(1, center, "O-", TODAY.minusDays(2)), donation(2, center, "O-", TODAY)));

    assertFulfilled(result, request, 2, true);
  }

  @Test
  void ignoresDonationsBeforeTheRequestAndAfterItsLimit() {
    BloodCenter center = center("A");
    DonationRequest request = request(1, center, "A+", 2, TODAY.minusDays(1), TODAY.plusDays(1), true);

    var result = calculate(
        List.of(request),
        List.of(donation(1, center, "O-", TODAY.minusDays(2)), donation(2, center, "O-", TODAY.plusDays(2))));

    assertFulfilled(result, request, 0, false);
  }

  @Test
  void ignoresInactiveAndExpiredRequests() {
    BloodCenter center = center("A");
    DonationRequest inactive = request(1, center, "A+", 1, TODAY.minusDays(2), TODAY.plusDays(2), false);
    DonationRequest expired = request(2, center, "A+", 1, TODAY.minusDays(2), TODAY.minusDays(1), true);

    var result = calculate(
        List.of(inactive, expired),
        List.of(donation(1, center, "O-", TODAY), donation(2, center, "O-", TODAY)));

    assertFulfilled(result, inactive, 0, false);
    assertFulfilled(result, expired, 0, false);
  }

  @Test
  void ignoresPendingAndCancelledDonations() {
    BloodCenter center = center("A");
    DonationRequest request = request(1, center, "A+", 2, TODAY.minusDays(1), TODAY.plusDays(1), true);
    Donation pending = Donation.reconstitute(id(1), donor("O-"), TODAY, center, false, true, false);
    Donation cancelled = Donation.reconstitute(id(2), donor("O-"), TODAY, center, false, false, true);

    var result = calculate(List.of(request), List.of(pending, cancelled));

    assertFulfilled(result, request, 0, false);
  }

  @Test
  void sortsRequestsAndDonationsInsteadOfTrustingTheInputOrder() {
    BloodCenter center = center("A");
    DonationRequest oldest = request(1, center, "A+", 1, TODAY.minusDays(2), TODAY.plusDays(2), true);
    DonationRequest newest = request(2, center, "A+", 1, TODAY.minusDays(1), TODAY.plusDays(2), true);

    var result = calculate(
        List.of(newest, oldest),
        List.of(donation(2, center, "O-", TODAY), donation(1, center, "O-", TODAY.minusDays(1))));

    assertFulfilled(result, oldest, 1, true);
    assertFulfilled(result, newest, 1, true);
  }

  @Test
  void resolvesRequestsCreatedOnTheSameDayByTheirIdDeterministically() {
    BloodCenter center = center("A");
    DonationRequest lowerId = request(1, center, "A+", 1, TODAY, TODAY.plusDays(1), true);
    DonationRequest higherId = request(2, center, "A+", 1, TODAY, TODAY.plusDays(1), true);

    var result = calculate(List.of(higherId, lowerId), List.of(donation(1, center, "O-", TODAY)));

    assertFulfilled(result, lowerId, 1, true);
    assertFulfilled(result, higherId, 0, false);
  }

  private java.util.Map<DomainID, DonationRequestFulfillmentStatusRecord> calculate(
      List<DonationRequest> requests,
      List<Donation> donations) {
    return service.calculate(requests, donations, TODAY);
  }

  private void assertFulfilled(
      java.util.Map<DomainID, DonationRequestFulfillmentStatusRecord> result,
      DonationRequest request,
      int expectedBags,
      boolean expectedGoalReached) {
    DonationRequestFulfillmentStatusRecord status = result.get(request.getId());
    assertEquals(expectedBags, status.fulfilledBloodBags());
    if (expectedGoalReached) assertTrue(status.goalReached());
    else assertFalse(status.goalReached());
  }

  private DonationRequest request(
      long id,
      BloodCenter center,
      String neededBloodType,
      int goal,
      LocalDate requestedAt,
      LocalDate limit,
      boolean active) {
    return DonationRequest.reconstitute(
        id(id), requester(), center, BloodType.of(neededBloodType), goal,
      requestedAt, limit, active, Urgency.MEDIUM, null, 0, null);
  }

  private Donation donation(long id, BloodCenter center, String donorBloodType, LocalDate date) {
    return Donation.reconstitute(id(id), donor(donorBloodType), date, center, true, false, false);
  }

  private BloodCenter center(String suffix) {
    return new BloodCenter(new Organization("Center " + suffix, new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }

  private Requester requester() {
    return new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)));
  }

  private Donor donor(String bloodType) {
    return new Donor(
        new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
        BloodType.of(bloodType),
        70.0);
  }

  private DomainID id(long value) {
    return new DomainID(new UUID(0, value));
  }
}
