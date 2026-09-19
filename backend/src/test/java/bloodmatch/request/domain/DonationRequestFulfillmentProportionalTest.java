package bloodmatch.request.domain;

import bloodmatch.donation.domain.Donation;
import bloodmatch.donation.domain.DonationRepositoryInterface;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.request.domain.DonationRequestRepositoryInterface;
import bloodmatch.request.domain.Urgency;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.request.domain.DonationRequestFulfillmentStatusRecord;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cenários do teto proporcional e da segunda passagem FIFO.
 *
 * Regras cobertas:
 * - na 1ª passagem cada request só recebe até o teto proporcional à sua janela
 * - ao bater o teto, a doação segue para a próxima request compatível
 * - as doações que ninguém absorveu voltam em FIFO até a meta cheia
 * - o total alocado é o mesmo do FIFO puro: o teto só muda quem recebe
 *
 * O helper imita o repositório (ativas + pool) e chama {@code fill(lista)}, igual
 * ao {@link DonationRequestFulfillmentFifoTest}.
 */
class DonationRequestFulfillmentProportionalTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);
  private final DonationRequestRepositoryInterface requestRepository =
      mock(DonationRequestRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository =
      mock(DonationRepositoryInterface.class);
  private final DonationRequestFulfillmentService service =
      new DonationRequestFulfillmentService(requestRepository, donationRepository);

  @Test
  void capsTheOldestRequestSoASisterCloserToItsDeadlineIsNotStarved() {
    BloodCenter center = center("A");
    // janela de 41 dias, 10 decorridos, meta 10 → teto 10 * 10/41 = 2.4390
    DonationRequest longHorizon = request(1, center, "A+", 10, TODAY.minusDays(10), TODAY.plusDays(31));
    // janela de 7 dias, 4 decorridos, meta 4 → teto 4 * 4/7 = 2.2857
    DonationRequest nearDeadline = request(2, center, "A+", 4, TODAY.minusDays(4), TODAY.plusDays(3));

    var result = calculate(List.of(longHorizon, nearDeadline), donations(8, center, TODAY.minusDays(1)));

    // 1ª passagem: 2.4390 para longHorizon, 2.2857 para nearDeadline (total 4.7247).
    // 2ª passagem: as 3.2753 sobras voltam para a mais antiga (2.4390 + 3.2753 = 5.7143).
    assertFulfilled(result, longHorizon, new BigDecimal("5.7143"), false);
    assertFulfilled(result, nearDeadline, new BigDecimal("2.2857"), false);
  }

  @Test
  void doesNotCapARequestOnItsLimitDay() {
    BloodCenter center = center("A");
    // janela de 25 dias, 5 decorridos, meta 4 → teto 4 * 5/25 = 0.8000
    DonationRequest longHorizon = request(1, center, "A+", 4, TODAY.minusDays(5), TODAY.plusDays(20));
    // último dia da janela: a meta é liberada inteira
    DonationRequest onItsLimitDay = request(2, center, "A+", 2, TODAY.minusDays(2), TODAY);

    var result = calculate(List.of(longHorizon, onItsLimitDay), donations(3, center, TODAY));

    assertFulfilled(result, longHorizon, new BigDecimal("1.0000"), false);
    assertFulfilled(result, onItsLimitDay, new BigDecimal("2.0000"), true);
  }

  @Test
  void givesNothingToABrandNewRequestInTheFirstPassAndReturnsLeftoversToTheOldest() {
    BloodCenter center = center("A");
    // janela de 10 dias, 1 decorrido, meta 5 → teto 5 * 1/10 = 0.5000
    DonationRequest yesterday = request(1, center, "A+", 5, TODAY.minusDays(1), TODAY.plusDays(9));
    // criada hoje: nenhuma fração da janela passou → teto 0
    DonationRequest createdToday = request(2, center, "A+", 5, TODAY, TODAY.plusDays(10));

    var result = calculate(List.of(yesterday, createdToday), donations(4, center, TODAY));

    assertFulfilled(result, yesterday, new BigDecimal("4.0000"), false);
    assertFulfilled(result, createdToday, BigDecimal.ZERO, false);
  }

  @Test
  void reachesEveryGoalWhenDonationsAreAbundant() {
    BloodCenter center = center("A");
    DonationRequest capped = request(1, center, "A+", 2, TODAY.minusDays(1), TODAY.plusDays(9));
    DonationRequest createdToday = request(2, center, "A+", 1, TODAY, TODAY.plusDays(10));

    var result = calculate(List.of(capped, createdToday), donations(5, center, TODAY));

    // com pool suficiente o resultado é idêntico ao do FIFO puro
    assertFulfilled(result, capped, new BigDecimal("2.0000"), true);
    assertFulfilled(result, createdToday, new BigDecimal("1.0000"), true);
  }

  @Test
  void doesNotLeaveBagsIdleWhenEveryCapIsSaturated() {
    BloodCenter center = center("A");
    DonationRequest first = request(1, center, "A+", 6, TODAY.minusDays(1), TODAY.plusDays(11));
    DonationRequest second = request(2, center, "A+", 6, TODAY.minusDays(1), TODAY.plusDays(11));

    var result = calculate(List.of(first, second), donations(7, center, TODAY));

    // tetos somam 1 (0.5 cada); as sobras voltam na 2ª passagem totalizando 7
    assertEquals(0, new BigDecimal("7.0000").compareTo(
        result.get(first.getId()).fulfilledBloodBags()
            .add(result.get(second.getId()).fulfilledBloodBags())));
  }

  private Map<DomainID, DonationRequestFulfillmentStatusRecord> calculate(
      List<DonationRequest> requests,
      List<Donation> donations) {
    List<DonationRequest> active = requests.stream()
        .filter(DonationRequest::isActive)
        .filter(request -> !request.isExpired(TODAY))
        .toList();

    when(requestRepository.findActiveRequestsByOrganizationIds(anyList(), eq(TODAY)))
        .thenReturn(active);

    LocalDate from = TODAY;
    for (DonationRequest request : active) {
      if (request.getDateRequested().isBefore(from)) {
        from = request.getDateRequested();
      }
    }
    when(donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(
            anyList(), eq(from), eq(TODAY)))
        .thenReturn(donations);

    return service.fill(requests, TODAY);
  }

  private void assertFulfilled(
      Map<DomainID, DonationRequestFulfillmentStatusRecord> result,
      DonationRequest request,
      Number expectedBags,
      boolean expectedGoalReached) {
    DonationRequestFulfillmentStatusRecord status = result.get(request.getId());
    BigDecimal expected = new BigDecimal(expectedBags.toString());
    assertEquals(0, expected.compareTo(status.fulfilledBloodBags()));
    if (expectedGoalReached) assertTrue(status.goalReached());
    else assertFalse(status.goalReached());
  }

  private DonationRequest request(
      long id,
      BloodCenter center,
      String neededBloodType,
      int goal,
      LocalDate requestedAt,
      LocalDate limit) {
    return DonationRequest.reconstitute(
        id(id), requester(), center, BloodType.of(neededBloodType), goal,
        requestedAt, limit, true, Urgency.MEDIUM, null, null);
  }

  /** Pool de doações O- na mesma data, com ids sequenciais para ordem estável. */
  private List<Donation> donations(int amount, BloodCenter center, LocalDate date) {
    List<Donation> donations = new ArrayList<>();
    for (int index = 1; index <= amount; index++) {
      donations.add(Donation.reconstitute(id(100L + index), donor("O-"), null, date, null, center));
    }
    return donations;
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
