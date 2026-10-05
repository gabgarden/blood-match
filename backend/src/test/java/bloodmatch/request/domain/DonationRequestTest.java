package bloodmatch.request.domain;

import bloodmatch.donation.domain.Donation;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    assertEquals(0, new BigDecimal("3.0000").compareTo(request.proportionalGoalAt(requestedAt.plusDays(3))));
  }

  @Test
  void releasesNothingOnTheDayTheRequestWasCreated() {
    assertEquals(0, BigDecimal.ZERO.compareTo(requestWith(10, 10).proportionalGoalAt(requestedAt)));
  }

  @Test
  void releasesTheWholeGoalOnTheLimitDay() {
    assertEquals(0, BigDecimal.valueOf(10).compareTo(requestWith(10, 10).proportionalGoalAt(requestedAt.plusDays(10))));
  }

  @Test
  void keepsTheProportionalGoalFractionalAsBigDecimal() {
    // meta 1 em janela de 10 dias, 1 dia decorrido: mantém 0.1000 quebrado em BigDecimal
    assertEquals(0, new BigDecimal("0.1000").compareTo(requestWith(1, 10).proportionalGoalAt(requestedAt.plusDays(1))));
    // meta 4 em janela de 7 dias, 4 dias decorridos: 16/7 ≈ 2.2857
    assertEquals(0, new BigDecimal("2.2857").compareTo(requestWith(4, 7).proportionalGoalAt(requestedAt.plusDays(4))));
  }

  @Test
  void releasesTheWholeGoalWhenTheWindowLastsASingleDay() {
    assertEquals(0, BigDecimal.valueOf(5).compareTo(requestWith(5, 0).proportionalGoalAt(requestedAt)));
  }

  @Test
  void proportionalGoalRequiresNonNullDate() {
    DonationRequest request = requestWith(2, 5);

    assertThrows(IllegalArgumentException.class, () -> request.proportionalGoalAt(null));
  }

  // --- Validações de Construtor e Reconstituição ---

  @Test
  void shouldRejectNullOrInvalidConstructorArguments() {
    Requester requester = new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)));
    BloodType bloodType = BloodType.of("A+");

    assertThrows(IllegalArgumentException.class, () -> DonationRequest.create(null, bloodCenter, bloodType, 5, dateLimit, requestedAt, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.create(requester, null, bloodType, 5, dateLimit, requestedAt, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.create(requester, bloodCenter, null, 5, dateLimit, requestedAt, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.create(requester, bloodCenter, bloodType, 0, dateLimit, requestedAt, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.create(requester, bloodCenter, bloodType, -1, dateLimit, requestedAt, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.create(requester, bloodCenter, bloodType, 5, null, requestedAt, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.create(requester, bloodCenter, bloodType, 5, dateLimit, null, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.create(requester, bloodCenter, bloodType, 5, dateLimit, requestedAt, null, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.create(requester, bloodCenter, bloodType, 5, requestedAt.minusDays(1), requestedAt, Urgency.MEDIUM, null));
  }

  @Test
  void shouldRejectNullOrInvalidReconstituteArguments() {
    Requester requester = new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)));
    BloodType bloodType = BloodType.of("A+");
    DomainID id = nextId();

    assertThrows(IllegalArgumentException.class, () -> DonationRequest.reconstitute(null, requester, bloodCenter, bloodType, 5, requestedAt, dateLimit, true, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.reconstitute(id, null, bloodCenter, bloodType, 5, requestedAt, dateLimit, true, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.reconstitute(id, requester, null, bloodType, 5, requestedAt, dateLimit, true, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.reconstitute(id, requester, bloodCenter, null, 5, requestedAt, dateLimit, true, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.reconstitute(id, requester, bloodCenter, bloodType, 0, requestedAt, dateLimit, true, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.reconstitute(id, requester, bloodCenter, bloodType, 5, null, dateLimit, true, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.reconstitute(id, requester, bloodCenter, bloodType, 5, requestedAt, null, true, Urgency.MEDIUM, null));
    assertThrows(IllegalArgumentException.class, () -> DonationRequest.reconstitute(id, requester, bloodCenter, bloodType, 5, requestedAt, dateLimit, true, null, null));
  }

  @Test
  void shouldSupportReconstituteWithVersionAndGetters() {
    Requester requester = new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)));
    BloodType bloodType = BloodType.of("A+");
    DomainID id = nextId();

    DonationRequest reconstituted = DonationRequest.reconstitute(id, requester, bloodCenter, bloodType, 10, requestedAt, dateLimit, true, Urgency.CRITICAL, "Patient John", 3L);

    assertEquals(id, reconstituted.getId());
    assertEquals(requester, reconstituted.getRequester());
    assertEquals(bloodCenter, reconstituted.getBloodCenter());
    assertEquals(bloodType, reconstituted.getBloodTypeNeeded());
    assertEquals(10, reconstituted.getGoalBloodBags());
    assertEquals(requestedAt, reconstituted.getDateRequested());
    assertEquals(dateLimit, reconstituted.getDateLimit());
    assertEquals(Urgency.CRITICAL, reconstituted.getUrgency());
    assertEquals("Patient John", reconstituted.getDirectedTo());
    assertEquals(3L, reconstituted.getVersion());
    assertTrue(reconstituted.isActive());

    // Reconstitute without version overload
    DonationRequest withoutVersion = DonationRequest.reconstitute(id, requester, bloodCenter, bloodType, 10, requestedAt, dateLimit, true, Urgency.CRITICAL, "Patient John");
    assertEquals(id, withoutVersion.getId());
    assertNull(withoutVersion.getVersion());
  }

  @Test
  void shouldThrowWhenClosingAlreadyClosedRequest() {
    DonationRequest request = request(BloodType.of("A+"));
    request.close();
    assertFalse(request.isActive());
    assertThrows(IllegalStateException.class, request::close);
  }

  @Test
  void shouldSupportDefaultCreateAndIsExpired() {
    Requester requester = new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)));
    DonationRequest req = DonationRequest.create(requester, bloodCenter, BloodType.of("O+"), 3, LocalDate.now().plusDays(2), Urgency.LOW, "Patient");
    assertFalse(req.isExpired());
    assertThrows(IllegalArgumentException.class, () -> req.isExpired(null));
  }

  @Test
  void shouldVerifyCanBeFulfilledBy() {
    DonationRequest request = request(BloodType.of("A+"));

    assertThrows(IllegalArgumentException.class, () -> request.canBeFulfilledBy(null, requestedAt));
    assertThrows(IllegalArgumentException.class, () -> request.canBeFulfilledBy(BloodType.of("A+"), null));

    assertTrue(request.canBeFulfilledBy(BloodType.of("O-"), requestedAt));
    assertTrue(request.canBeFulfilledBy(BloodType.of("A+"), requestedAt));
    assertFalse(request.canBeFulfilledBy(BloodType.of("B+"), requestedAt));

    // Inactive request cannot be fulfilled
    request.close();
    assertFalse(request.canBeFulfilledBy(BloodType.of("O-"), requestedAt));

    // Expired request cannot be fulfilled
    DonationRequest activeReq = request(BloodType.of("A+"));
    assertFalse(activeReq.canBeFulfilledBy(BloodType.of("O-"), dateLimit.plusDays(1)));

    // Default canBeFulfilledBy overload
    DonationRequest futureReq = DonationRequest.create(
        new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
        bloodCenter, BloodType.of("A+"), 2, LocalDate.now().plusDays(5), Urgency.MEDIUM, null);
    assertTrue(futureReq.canBeFulfilledBy(BloodType.of("A+")));

    // proportionalGoalAt when elapsedDays > windowDays
    assertEquals(0, BigDecimal.valueOf(2).compareTo(futureReq.proportionalGoalAt(LocalDate.now().plusDays(10))));
  }

  @Test
  void shouldUpdateGoalBloodBags() {
    DonationRequest request = request(BloodType.of("A+"));
    request.setGoalBloodBags(15);
    assertEquals(15, request.getGoalBloodBags());

    assertThrows(IllegalArgumentException.class, () -> request.setGoalBloodBags(0));
    assertThrows(IllegalArgumentException.class, () -> request.setGoalBloodBags(-5));
  }

  @Test
  void shouldUpdateDateLimit() {
    DonationRequest request = request(BloodType.of("A+"));
    LocalDate newLimit = LocalDate.now().plusDays(10);
    request.setDateLimit(newLimit);
    assertEquals(newLimit, request.getDateLimit());

    assertThrows(IllegalArgumentException.class, () -> request.setDateLimit(null));
    assertThrows(IllegalArgumentException.class, () -> request.setDateLimit(LocalDate.now().minusDays(1)));
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
