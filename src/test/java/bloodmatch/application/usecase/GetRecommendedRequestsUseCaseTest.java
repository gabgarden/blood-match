package bloodmatch.application.usecase;

import bloodmatch.application.usecase.donationrequest.recommendations.GetRecommendedRequestsUseCase;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
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
  private final GetRecommendedRequestsUseCase useCase = new GetRecommendedRequestsUseCase(
      donorRepository,
      donationRequestRepository);

  @Test
  void shouldNotRecommendRequestsWhenDonorIsNotEligible() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();

    Donor donor = createDonor(currentDate);
    donor.registerDonation(currentDate.minusMonths(1), currentDate);

    DonationRequest request = createRequest(currentDate);

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequestsForDonor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(request));
    when(donationRequestRepository.findActiveRequestsByBloodCenterIds(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(request));

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(donorId, currentDate);

    assertEquals(List.of(), result);
  }

  @Test
  void shouldRecommendEligibleRequestsWithOutstandingGoal() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();

    Donor donor = createDonor(currentDate);
    DonationRequest request = createRequest(currentDate);
    request.setFulfilledBloodBags(0);

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequestsForDonor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(request));

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
    request.setFulfilledBloodBags(1);

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequestsForDonor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(request));

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(donorId, currentDate);

    assertEquals(List.of(), result);
  }

  @Test
  void shouldSortByDistanceThenUrgencyThenDeadline() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();

    Donor donor = createDonor(currentDate);
    donor.getPerson().changeAddress(new Address("Donor Street", "Sao Paulo", "SP", "01001000", -23.5505, -46.6333));

    DonationRequest closer = createRequest(currentDate);
    closer.getBloodCenter().getOrganization().changeAddress(new Address("Closer", "Sao Paulo", "SP", "02002000", -23.5510, -46.6330));
    DonationRequest farther = createRequest(currentDate);
    farther.getBloodCenter().getOrganization().changeAddress(new Address("Farther", "Sao Paulo", "SP", "03003000", -23.6500, -46.7300));

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequestsForDonor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(farther, closer));

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(donorId, currentDate);

    assertEquals(2, result.size());
    assertEquals(closer.getId().getValue().toString(), result.get(0).requestId());
    assertEquals(farther.getId().getValue().toString(), result.get(1).requestId());
  }

  @Test
  void shouldReturnNoRecommendationsWhenTheDatabaseFindsNoRequestWithinTheMaximumDistance() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();
    Donor donor = createDonor(currentDate);
    donor.getPerson().changeAddress(new Address("Street", "Sao Paulo", "SP", "01001000", -23.5505, -46.6333));

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequestsForDonor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(donorId, currentDate);

    assertEquals(List.of(), result);
  }

  private Donor createDonor(LocalDate currentDate) {
    Person donorPerson = new Person(
        "Donor Person",
        new PhoneNumber("11988887777"),
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
      new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1995, 1, 1)));

    BloodCenter bloodCenter = new BloodCenter(new Organization(
        "Blood Center",
      new PhoneNumber("1133334444"),
        new CNPJ("12345678000100")));

    return DonationRequest.create(
        requester,
        bloodCenter,
        BloodType.of("A+"),
        1,
        currentDate.plusDays(10),
        currentDate,
        Urgency.MEDIUM,
        null);
  }
}
