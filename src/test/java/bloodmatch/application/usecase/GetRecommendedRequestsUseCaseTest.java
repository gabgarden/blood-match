package bloodmatch.application.usecase;

import bloodmatch.application.usecase.donationrequest.recommendations.GetRecommendedRequestsUseCase;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetRecommendedRequestsUseCaseTest {

  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final DonationRequestRepositoryInterface donationRequestRepository = mock(DonationRequestRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final GetRecommendedRequestsUseCase useCase = new GetRecommendedRequestsUseCase(
      donorRepository,
      donationRequestRepository,
      donationRepository,
      new DonationRequestFulfillmentService());

  @Test
  void shouldNotRecommendRequestsWhenDonorIsNotEligible() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();

    Donor donor = createDonor(currentDate);
    donor.registerDonation(currentDate.minusMonths(1), currentDate);

    DonationRequest request = createRequest(currentDate);

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequests()).thenReturn(List.of(request));

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(donorId, currentDate);

    assertEquals(List.of(), result);
  }

  @Test
  void shouldRecommendEligibleRequestsWithOutstandingGoal() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();

    Donor donor = createDonor(currentDate);
    DonationRequest request = createRequest(currentDate);

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequests()).thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(donorId, currentDate);

    assertEquals(1, result.size());
    assertEquals(0, result.get(0).fulfilledBloodBags());
    assertEquals(false, result.get(0).goalReached());
  }

  @Test
  void shouldNotRecommendRequestsWhenGoalIsAlreadyReached() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();

    Donor donor = createDonor(currentDate);
    DonationRequest request = createRequest(currentDate);

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequests()).thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(
        Donation.registerExternalDonation(donor, currentDate, request.getBloodCenter(), currentDate)));

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(donorId, currentDate);

    assertEquals(List.of(), result);
  }

  private Donor createDonor(LocalDate currentDate) {
    Person donorPerson = new Person(
        "Donor Person",
        new CPF("98765432100"),
        currentDate.minusYears(30));

    return new Donor(
        donorPerson,
        BloodType.of("O-"),
        75.0);
  }

  private DonationRequest createRequest(LocalDate currentDate) {
    Requester requester = new Requester(new Person(
        "Requester Person",
        new CPF("12345678901"),
        LocalDate.of(1995, 1, 1)));

    BloodCenter bloodCenter = new BloodCenter(new Organization(
        "Blood Center",
        new CNPJ("12345678000100")));

    return DonationRequest.create(
        requester,
        bloodCenter,
        BloodType.of("A+"),
        1,
        currentDate.plusDays(10),
        currentDate,
        Urgency.MEDIUM);
  }
}
