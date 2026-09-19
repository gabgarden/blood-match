package bloodmatch.request.application;

import bloodmatch.request.application.recommendations.GetRecommendedRequestsUseCase;
import bloodmatch.donation.domain.Donation;
import bloodmatch.donation.domain.DonationRepositoryInterface;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.request.domain.Urgency;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.request.domain.DonationRequestRepositoryInterface;
import bloodmatch.role.domain.person.donor.DonorRepositoryInterface;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.request.domain.DonationRequestFulfillmentService;
import bloodmatch.request.domain.DonationRequestFulfillmentStatusRecord;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.Address;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetRecommendedRequestsUseCaseTest {

  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final DonationRequestRepositoryInterface donationRequestRepository = mock(DonationRequestRepositoryInterface.class);
  private final DonationRequestFulfillmentService fulfillmentService = mock(DonationRequestFulfillmentService.class);
  private final GetRecommendedRequestsUseCase useCase = new GetRecommendedRequestsUseCase(
      donorRepository,
      donationRequestRepository,
      fulfillmentService);

  @Test
  void shouldNotRecommendRequestsWhenDonorIsNotEligible() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();

    Donor donor = createDonor(currentDate);
    donor.registerDonation(currentDate.minusMonths(1), currentDate);

    DonationRequest request = createRequest(currentDate);

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequestsForDonor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(request));
    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(
        new GetRecommendedRequestsUseCase.Input(donorId.getValue().toString()), currentDate);

    assertEquals(List.of(), result);
  }

  @Test
  void shouldRecommendRequestsWhenDonorIsNotEligibleAndIncludeNonEligibleIsTrue() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();

    Donor donor = createDonor(currentDate);
    donor.registerDonation(currentDate.minusMonths(1), currentDate);

    DonationRequest request = createRequest(currentDate);

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequestsForDonor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(request));
    when(fulfillmentService.fill(anyList(), any())).thenReturn(Map.of());

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(
        new GetRecommendedRequestsUseCase.Input(donorId.getValue().toString(), true), currentDate);

    assertEquals(1, result.size());
    assertEquals("Blood Center", result.get(0).bloodCenterName());
  }

  @Test
  void shouldRecommendEligibleRequestsWithOutstandingGoal() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();

    Donor donor = createDonor(currentDate);
    DonationRequest request = createRequest(currentDate);

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequestsForDonor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(request));
    when(fulfillmentService.fill(anyList(), any())).thenReturn(
        Map.of(request.getId(), new DonationRequestFulfillmentStatusRecord(0, false)));

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(
        new GetRecommendedRequestsUseCase.Input(donorId.getValue().toString()), currentDate);

    assertEquals(1, result.size());
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).fulfilledBloodBags()));
    assertEquals(false, result.get(0).goalReached());
    assertEquals(request.getBloodCenter().getOrganization().getId().getValue().toString(),
        result.get(0).organizationId());
    assertEquals("Blood Center", result.get(0).bloodCenterName());
  }

  @Test
  void shouldNotRecommendRequestsWhenGoalIsAlreadyReached() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();

    Donor donor = createDonor(currentDate);
    DonationRequest request = createRequest(currentDate);

    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donationRequestRepository.findActiveRequestsForDonor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(request));
    when(fulfillmentService.fill(anyList(), any())).thenReturn(
        Map.of(request.getId(), new DonationRequestFulfillmentStatusRecord(1, true)));

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(
        new GetRecommendedRequestsUseCase.Input(donorId.getValue().toString()), currentDate);

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
    when(fulfillmentService.fill(anyList(), any())).thenReturn(Map.of());

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(
        new GetRecommendedRequestsUseCase.Input(donorId.getValue().toString()), currentDate);

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

    List<GetRecommendedRequestsUseCase.OutputItem> result = useCase.execute(
        new GetRecommendedRequestsUseCase.Input(donorId.getValue().toString()), currentDate);

    assertEquals(List.of(), result);
  }

  @Test
  void shouldOmitRequestWhenRealFulfillmentSnapshotAlreadyReachedTheGoal() {
    LocalDate currentDate = LocalDate.of(2026, 4, 17);
    DomainID donorId = DomainID.generate();
    Donor donor = createDonor(currentDate);
    DonationRequest request = createRequest(currentDate);

    DonationRequestRepositoryInterface requests = mock(DonationRequestRepositoryInterface.class);
    DonationRepositoryInterface donations = mock(DonationRepositoryInterface.class);
    DonorRepositoryInterface donors = mock(DonorRepositoryInterface.class);
    GetRecommendedRequestsUseCase recommendations = new GetRecommendedRequestsUseCase(
        donors,
        requests,
        new DonationRequestFulfillmentService(requests, donations));

    when(donors.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(requests.findActiveRequestsForDonor(any(), any(), anyDouble(), any())).thenReturn(List.of(request));
    when(requests.findActiveRequestsByOrganizationIds(anyList(), any())).thenReturn(List.of(request));
    when(donations.findCompletedDonationsByOrganizationIdsAndDateRange(anyList(), any(), any()))
        .thenReturn(List.of(completedDonation(request.getBloodCenter(), currentDate)));

    List<GetRecommendedRequestsUseCase.OutputItem> result = recommendations.execute(
        new GetRecommendedRequestsUseCase.Input(donorId.getValue().toString()), currentDate);

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
