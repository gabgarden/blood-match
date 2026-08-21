package bloodmatch.application.usecase;

import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase;
import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase.Input;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lista do requisitante com o {@link DonationRequestFulfillmentService} real.
 * Os testes de DTO/ordenação continuam em {@link GetDonationRequestsByPartyIdUseCaseTest}.
 */
class GetDonationRequestsByPartyIdFulfillmentContractTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);

  private final DonationRequestRepositoryInterface requestRepository =
      mock(DonationRequestRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository =
      mock(DonationRepositoryInterface.class);
  private final GetDonationRequestsByPartyIdUseCase useCase =
      new GetDonationRequestsByPartyIdUseCase(
          requestRepository,
          new DonationRequestFulfillmentService(requestRepository, donationRepository));

  @Test
  void listsFifoAllocationFromTheRealFulfillmentSnapshot() {
    BloodCenter center = center();
    Requester requester = requester();
    DonationRequest older = request(1, requester, center, TODAY.minusDays(2), 1);
    DonationRequest newer = request(2, requester, center, TODAY.minusDays(1), 1);

    when(requestRepository.findByRequesterPartyId(requester.getParty().getId()))
        .thenReturn(List.of(older, newer));
    when(requestRepository.findActiveRequestsByOrganizationIds(anyList(), eq(TODAY)))
        .thenReturn(List.of(older, newer));
    when(donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(anyList(), any(), any()))
        .thenReturn(List.of(donation(1, center, TODAY)));

    List<GetDonationRequestsByPartyIdUseCase.OutputItem> result =
        useCase.execute(new Input(requester.getParty().getId().getValue().toString()), TODAY);

    assertEquals(2, result.size());
    assertEquals(newer.getId().getValue().toString(), result.get(0).requestId());
    assertEquals(0, result.get(0).fulfilledBloodBags());
    assertEquals(false, result.get(0).goalReached());
    assertEquals(older.getId().getValue().toString(), result.get(1).requestId());
    assertEquals(1, result.get(1).fulfilledBloodBags());
    assertEquals(true, result.get(1).goalReached());
  }

  private DonationRequest request(
      long id,
      Requester requester,
      BloodCenter center,
      LocalDate requestedAt,
      int goal) {
    return DonationRequest.reconstitute(
        id(id),
        requester,
        center,
        BloodType.of("A+"),
        goal,
        requestedAt,
        TODAY.plusDays(5),
        true,
        Urgency.MEDIUM,
        null,
        null);
  }

  private Donation donation(long id, BloodCenter center, LocalDate date) {
    return Donation.reconstitute(
        id(id),
        new Donor(
            new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
            BloodType.of("O-"),
            70.0),
        null,
        date,
        null,
        center);
  }

  private Requester requester() {
    return new Requester(new Person(
        "Requester",
        new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1)));
  }

  private BloodCenter center() {
    return new BloodCenter(new Organization(
        "Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100")));
  }

  private DomainID id(long value) {
    return new DomainID(new UUID(0, value));
  }
}
