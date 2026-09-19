package bloodmatch.request.application;

import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.request.application.GetDonationRequestsByPartyIdUseCase;
import bloodmatch.request.application.GetDonationRequestsByPartyIdUseCase.Input;
import bloodmatch.donation.domain.Donation;
import bloodmatch.donation.domain.DonationRepositoryInterface;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.request.domain.Urgency;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.request.domain.DonationRequestRepositoryInterface;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.request.domain.DonationRequestFulfillmentService;
import bloodmatch.request.domain.DonationRequestFulfillmentStatusRecord;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetDonationRequestsByPartyIdUseCaseTest {

  private final DonationRequestRepositoryInterface donationRequestRepository =
      mock(DonationRequestRepositoryInterface.class);
  private final DonationRequestFulfillmentService fulfillmentService =
      mock(DonationRequestFulfillmentService.class);
  private final GetDonationRequestsByPartyIdUseCase useCase = new GetDonationRequestsByPartyIdUseCase(
      donationRequestRepository,
      fulfillmentService);

  @Test
  void shouldReturnRequestsOrderedByDateRequestedDesc() {
    LocalDate now = LocalDate.of(2026, 4, 23);
    DomainID userId = DomainID.generate();

    DonationRequest older = createRequest(now.minusDays(3));
    DonationRequest newer = createRequest(now.minusDays(1));

    when(donationRequestRepository.findByRequesterPartyId(userId)).thenReturn(List.of(older, newer));
    when(fulfillmentService.fill(anyList(), any())).thenReturn(Map.of());

    List<GetDonationRequestsByPartyIdUseCase.OutputItem> result =
        useCase.execute(new Input(userId.getValue().toString()), now);

    assertEquals(2, result.size());
    assertEquals(newer.getId().getValue().toString(), result.get(0).requestId());
    assertEquals(older.getId().getValue().toString(), result.get(1).requestId());
    assertEquals(1, result.get(0).goalBloodBags());
    assertBags(0, result.get(0).fulfilledBloodBags());
    assertBags(1, result.get(0).remainingBloodBags());
    assertEquals(false, result.get(0).goalReached());
    assertEquals(true, result.get(0).active());
    assertEquals(false, result.get(0).expired());
    assertEquals("MEDIUM", result.get(0).urgency());
  }

  @Test
  void shouldReturnCancelledRequestWithZeroProgress() {
    LocalDate now = LocalDate.of(2026, 4, 23);
    DomainID userId = DomainID.generate();
    DonationRequest cancelled = createRequest(now.minusDays(1));
    cancelled.close();

    when(donationRequestRepository.findByRequesterPartyId(userId)).thenReturn(List.of(cancelled));
    when(fulfillmentService.fill(anyList(), any())).thenReturn(Map.of());

    List<GetDonationRequestsByPartyIdUseCase.OutputItem> result =
        useCase.execute(new Input(userId.getValue().toString()), now);

    assertEquals(1, result.size());
    assertBags(0, result.get(0).fulfilledBloodBags());
    assertBags(1, result.get(0).remainingBloodBags());
    assertEquals(false, result.get(0).active());
    assertEquals(false, result.get(0).expired());
  }

  @Test
  void shouldExposeExpiredAndFulfillmentBooleansInsteadOfASituation() {
    LocalDate currentDate = LocalDate.of(2026, 4, 23);
    DomainID userId = DomainID.generate();
    DonationRequest expired = createRequest(currentDate.minusDays(12));

    when(donationRequestRepository.findByRequesterPartyId(userId)).thenReturn(List.of(expired));
    when(fulfillmentService.fill(anyList(), any())).thenReturn(
        Map.of(expired.getId(), new DonationRequestFulfillmentStatusRecord(1, true)));

    GetDonationRequestsByPartyIdUseCase.OutputItem result =
        useCase.execute(new Input(userId.getValue().toString()), currentDate).get(0);

    assertEquals(true, result.active());
    assertEquals(true, result.expired());
    assertBags(1, result.fulfilledBloodBags());
    assertEquals(true, result.goalReached());
  }

  @Test
  void shouldThrowWhenUserIdIsNull() {
    assertThrows(ValidationException.class, () -> useCase.execute(new Input(null)));
  }

  @Test
  void shouldExposeFifoSnapshotFromTheRealFulfillmentService() {
    LocalDate now = LocalDate.of(2026, 4, 23);
    DomainID userId = DomainID.generate();
    BloodCenter center = bloodCenter();
    DonationRequest older = createRequest(now.minusDays(3), center);
    DonationRequest newer = createRequest(now.minusDays(1), center);

    DonationRequestRepositoryInterface requests = mock(DonationRequestRepositoryInterface.class);
    DonationRepositoryInterface donations = mock(DonationRepositoryInterface.class);
    GetDonationRequestsByPartyIdUseCase listing = new GetDonationRequestsByPartyIdUseCase(
        requests,
        new DonationRequestFulfillmentService(requests, donations));

    when(requests.findByRequesterPartyId(userId)).thenReturn(List.of(older, newer));
    when(requests.findActiveRequestsByOrganizationIds(anyList(), any())).thenReturn(List.of(older, newer));
    when(donations.findCompletedDonationsByOrganizationIdsAndDateRange(anyList(), any(), any()))
        .thenReturn(List.of(completedDonation(center, now)));

    List<GetDonationRequestsByPartyIdUseCase.OutputItem> result =
        listing.execute(new Input(userId.getValue().toString()), now);

    assertEquals(newer.getId().getValue().toString(), result.get(0).requestId());
    assertBags("0.1000", result.get(0).fulfilledBloodBags());
    assertEquals(false, result.get(0).goalReached());
    assertEquals(older.getId().getValue().toString(), result.get(1).requestId());
    assertBags("0.9000", result.get(1).fulfilledBloodBags());
    assertEquals(false, result.get(1).goalReached());
  }

  private static void assertBags(Object expected, BigDecimal actual) {
    assertEquals(0, new BigDecimal(expected.toString()).compareTo(actual));
  }

  private DonationRequest createRequest(LocalDate dateRequested) {
    return createRequest(dateRequested, bloodCenter());
  }

  private DonationRequest createRequest(LocalDate dateRequested, BloodCenter bloodCenter) {
    Requester requester = new Requester(new Person(
        "Requester Person",
        new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1)));

    return DonationRequest.create(
        requester,
        bloodCenter,
        BloodType.of("A+"),
        1,
        dateRequested.plusDays(10),
        dateRequested,
        Urgency.MEDIUM,
        null);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization(
        "Blood Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100")));
  }

  private Donation completedDonation(BloodCenter center, LocalDate date) {
    return Donation.reconstitute(
        new DomainID(new UUID(0, 1)),
        new Donor(
            new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
            BloodType.of("O-"),
            70.0),
        null,
        date,
        null,
        center);
  }
}
