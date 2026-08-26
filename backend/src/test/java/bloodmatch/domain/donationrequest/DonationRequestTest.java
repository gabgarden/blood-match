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
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonationRequestTest {

  private static final AtomicLong IDS = new AtomicLong(1);
  private final LocalDate requestedAt = LocalDate.of(2026, 4, 10);
  private final LocalDate dateLimit = requestedAt.plusDays(5);
  private final BloodCenter bloodCenter = new BloodCenter(
      new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));

  @Test
  void shouldExpireOnlyAfterTheDeadline() {
    DonationRequest request = request(BloodType.of("A+"));

    assertFalse(request.isExpired(dateLimit));
    assertTrue(request.isExpired(dateLimit.plusDays(1)));
  }

  @Test
  void acceptsCompletedCompatibleDonationWithinInclusivePeriod() {
    DonationRequest request = request(BloodType.of("A+"));

    assertTrue(request.acceptsDonation(completed("O-", requestedAt), requestedAt));
    assertTrue(request.acceptsDonation(completed("O-", requestedAt.plusDays(2)), requestedAt.plusDays(2)));
    assertTrue(request.acceptsDonation(completed("O-", dateLimit), dateLimit));
  }

  @Test
  void rejectsDonationBeforeRequestOrAfterLimit() {
    DonationRequest request = request(BloodType.of("A+"));

    assertFalse(request.acceptsDonation(completed("O-", requestedAt.minusDays(1)), requestedAt));
    assertFalse(request.acceptsDonation(completed("O-", dateLimit.plusDays(1)), dateLimit.plusDays(1)));
  }

  @Test
  void rejectsIncompatibleBloodTypeEvenWhenCompletedAndInWindow() {
    DonationRequest request = request(BloodType.of("O-"));

    assertFalse(request.acceptsDonation(completed("A+", requestedAt.plusDays(1)), requestedAt.plusDays(1)));
  }

  @Test
  void rejectsPendingAndCancelledDonations() {
    DonationRequest request = request(BloodType.of("A+"));
    Donation pending = Donation.reconstitute(
        nextId(), donor("O-"), requestedAt.plusDays(1), null, null, bloodCenter);
    Donation cancelled = Donation.reconstitute(
        nextId(), donor("O-"), requestedAt.plusDays(1), null, requestedAt.plusDays(1), bloodCenter);

    assertFalse(request.acceptsDonation(pending, requestedAt.plusDays(1)));
    assertFalse(request.acceptsDonation(cancelled, requestedAt.plusDays(1)));
  }

  @Test
  void rejectsWhenRequestIsInactive() {
    DonationRequest request = request(BloodType.of("A+"));
    Donation donation = completed("O-", requestedAt.plusDays(1));

    request.close();

    assertFalse(request.acceptsDonation(donation, requestedAt.plusDays(1)));
  }

  @Test
  void rejectsExpiredRequestEvenIfDonationDateWasInsideOriginalWindow() {
    DonationRequest request = request(BloodType.of("A+"));
    Donation withinOriginalWindow = completed("O-", requestedAt.plusDays(1));

    assertFalse(request.acceptsDonation(withinOriginalWindow, dateLimit.plusDays(1)));
  }

  @Test
  void acceptsDonationRequiresNonNullArguments() {
    DonationRequest request = request(BloodType.of("A+"));
    Donation donation = completed("O-", requestedAt.plusDays(1));

    assertThrows(IllegalArgumentException.class, () -> request.acceptsDonation(null, requestedAt));
    assertThrows(IllegalArgumentException.class, () -> request.acceptsDonation(donation, null));
  }

  // --- Teto proporcional ---

  @Test
  void releasesTheGoalProportionallyToTheElapsedWindow() {
    // janela de 10 dias, meta 10: no 3º dia só 3/10 da meta está liberada
    DonationRequest request = requestWith(10, 10);

    assertEquals(3, request.proportionalGoalAt(requestedAt.plusDays(3)));
  }

  @Test
  void releasesNothingOnTheDayTheRequestWasCreated() {
    assertEquals(0, requestWith(10, 10).proportionalGoalAt(requestedAt));
  }

  @Test
  void releasesTheWholeGoalOnTheLimitDay() {
    assertEquals(10, requestWith(10, 10).proportionalGoalAt(requestedAt.plusDays(10)));
  }

  @Test
  void roundsTheProportionalGoalUpSoSmallGoalsAreNotStuckAtZero() {
    // meta 1 em janela de 10 dias: com floor ficaria em 0 até o último dia
    assertEquals(1, requestWith(1, 10).proportionalGoalAt(requestedAt.plusDays(1)));
    // meta 4 em janela de 7 dias, 4 dias decorridos: ceil(16/7)
    assertEquals(3, requestWith(4, 7).proportionalGoalAt(requestedAt.plusDays(4)));
  }

  @Test
  void releasesTheWholeGoalWhenTheWindowLastsASingleDay() {
    assertEquals(5, requestWith(5, 0).proportionalGoalAt(requestedAt));
  }

  @Test
  void proportionalGoalRequiresNonNullDate() {
    DonationRequest request = requestWith(2, 5);

    assertThrows(IllegalArgumentException.class, () -> request.proportionalGoalAt(null));
  }

  private DonationRequest requestWith(int goal, int windowDays) {
    return DonationRequest.create(
        new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
        bloodCenter,
        BloodType.of("A+"),
        goal,
        requestedAt.plusDays(windowDays),
        requestedAt,
        Urgency.MEDIUM,
        null);
  }

  private DonationRequest request(BloodType type) {
    return DonationRequest.create(
        new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
        bloodCenter,
        type,
        2,
        dateLimit,
        requestedAt,
        Urgency.MEDIUM,
        null);
  }

  private Donation completed(String bloodType, LocalDate date) {
    return Donation.reconstitute(nextId(), donor(bloodType), null, date, null, bloodCenter);
  }

  private Donor donor(String bloodType) {
    return new Donor(
        new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
        BloodType.of(bloodType),
        70.0);
  }

  private DomainID nextId() {
    return new DomainID(new UUID(0, IDS.getAndIncrement()));
  }
}
