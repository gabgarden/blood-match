package bloodmatch.application.usecase;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase;
import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase.Input;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetDonationRequestsByPartyIdUseCaseTest {

  private final DonationRequestRepositoryInterface donationRequestRepository =
      mock(DonationRequestRepositoryInterface.class);
  private final GetDonationRequestsByPartyIdUseCase useCase = new GetDonationRequestsByPartyIdUseCase(
      donationRequestRepository);

  @Test
  void shouldReturnRequestsOrderedByDateRequestedDesc() {
    LocalDate now = LocalDate.of(2026, 4, 23);
    DomainID userId = DomainID.generate();

    DonationRequest older = createRequest(now.minusDays(3));
    DonationRequest newer = createRequest(now.minusDays(1));

    when(donationRequestRepository.findByRequesterPartyId(userId)).thenReturn(List.of(older, newer));

    List<GetDonationRequestsByPartyIdUseCase.OutputItem> result =
        useCase.execute(new Input(userId.getValue().toString()), now);

    assertEquals(2, result.size());
    assertEquals(newer.getId().getValue().toString(), result.get(0).requestId());
    assertEquals(older.getId().getValue().toString(), result.get(1).requestId());
    assertEquals(1, result.get(0).goalBloodBags());
    assertEquals(0, result.get(0).fulfilledBloodBags());
    assertEquals(1, result.get(0).remainingBloodBags());
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

    List<GetDonationRequestsByPartyIdUseCase.OutputItem> result =
        useCase.execute(new Input(userId.getValue().toString()), now);

    assertEquals(1, result.size());
    assertEquals(0, result.get(0).fulfilledBloodBags());
    assertEquals(1, result.get(0).remainingBloodBags());
    assertEquals(false, result.get(0).active());
    assertEquals(false, result.get(0).expired());
  }

  @Test
  void shouldExposeExpiredAndFulfillmentBooleansInsteadOfASituation() {
    LocalDate currentDate = LocalDate.of(2026, 4, 23);
    DomainID userId = DomainID.generate();
    DonationRequest expired = createRequest(currentDate.minusDays(12));
    expired.setFulfilledBloodBags(1);

    when(donationRequestRepository.findByRequesterPartyId(userId)).thenReturn(List.of(expired));

    GetDonationRequestsByPartyIdUseCase.OutputItem result =
        useCase.execute(new Input(userId.getValue().toString()), currentDate).get(0);

    assertEquals(true, result.active());
    assertEquals(true, result.expired());
    assertEquals(1, result.fulfilledBloodBags());
    assertEquals(true, result.goalReached());
  }

  @Test
  void shouldThrowWhenUserIdIsNull() {
    assertThrows(ValidationException.class, () -> useCase.execute(new Input(null)));
  }

  private DonationRequest createRequest(LocalDate dateRequested) {
    Requester requester = new Requester(new Person(
        "Requester Person",
        new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1)));

    BloodCenter bloodCenter = new BloodCenter(new Organization(
        "Blood Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100")));

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
}
