package bloodmatch.application.usecase;

import bloodmatch.application.usecase.donationrequest.recommendations.GetRecommendedRequestsUseCase;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Recomendações com o {@link DonationRequestFulfillmentService} real.
 * Ordenação e mapeamento de DTO continuam em {@link GetRecommendedRequestsUseCaseTest}.
 */
class GetRecommendedRequestsFulfillmentContractTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);

  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final DonationRequestRepositoryInterface requestRepository =
      mock(DonationRequestRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository =
      mock(DonationRepositoryInterface.class);
  private final GetRecommendedRequestsUseCase useCase = new GetRecommendedRequestsUseCase(
      donorRepository,
      requestRepository,
      new DonationRequestFulfillmentService(requestRepository, donationRepository));

  @Test
  void omitsRequestWhenTheRealFulfillmentSnapshotHasReachedTheGoal() {
    Donor donor = donor();
    BloodCenter center = center();
    DonationRequest request = request(1, center, TODAY.minusDays(1), 1);

    when(donorRepository.findByPartyId(donor.getPerson().getId())).thenReturn(Optional.of(donor));
    when(requestRepository.findActiveRequestsForDonor(any(), any(), anyDouble(), eq(TODAY)))
        .thenReturn(List.of(request));
    when(requestRepository.findActiveRequestsByOrganizationIds(anyList(), eq(TODAY)))
        .thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(anyList(), any(), any()))
        .thenReturn(List.of(donation(1, center, TODAY)));

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(
        new GetRecommendedRequestsUseCase.Input(donor.getPerson().getId().getValue().toString()),
        TODAY);

    assertEquals(List.of(), result);
  }

  private DonationRequest request(long id, BloodCenter center, LocalDate requestedAt, int goal) {
    return DonationRequest.reconstitute(
        id(id),
        new Requester(new Person(
            "Requester",
            new PhoneNumber("11999990000"),
            new CPF("12345678901"),
            LocalDate.of(1990, 1, 1))),
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
            new Person("Other Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
            BloodType.of("O-"),
            70.0),
        null,
        date,
        null,
        center);
  }

  private Donor donor() {
    return new Donor(
        new Person("Donor", new PhoneNumber("11977776666"), new CPF("11144477735"), TODAY.minusYears(30)),
        BloodType.of("O-"),
        75.0);
  }

  private BloodCenter center() {
    return new BloodCenter(new Organization(
        "Blood Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100")));
  }

  private DomainID id(long value) {
    return new DomainID(new UUID(0, value));
  }
}
