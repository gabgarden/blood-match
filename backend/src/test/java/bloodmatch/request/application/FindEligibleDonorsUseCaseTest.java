package bloodmatch.request.application;

import bloodmatch.request.application.FindEligibleDonorsUseCase;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.request.domain.Urgency;
import bloodmatch.role.domain.matching.DonorMatchingService;
import bloodmatch.party.domain.Person;
import bloodmatch.request.domain.DonationRequestRepositoryInterface;
import bloodmatch.role.domain.person.donor.DonorRepositoryInterface;
import bloodmatch.party.domain.Organization;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import bloodmatch.request.domain.DonorRecommendationPolicyInterface;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FindEligibleDonorsUseCaseTest {

  private final DonationRequestRepositoryInterface donationRequestRepository = mock(
      DonationRequestRepositoryInterface.class);
    private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final DonorMatchingService donorMatchingService = mock(DonorMatchingService.class);
  private final DonorRecommendationPolicyInterface recommendationPolicy = mock(
      DonorRecommendationPolicyInterface.class);

  @Test
  void shouldReturnEligibleDonorsWhenPolicyIsNull() {
    FindEligibleDonorsUseCase useCase = new FindEligibleDonorsUseCase(
        donationRequestRepository,
        donorRepository,
        donorMatchingService);

    LocalDate currentDate = LocalDate.of(2026, 3, 16);
    DomainID requestId = DomainID.generate();
    DomainID donorId = DomainID.generate();

    DonationRequest request = createDonationRequest(currentDate);
    Donor donor = createDonor(currentDate, "98765432100");

    List<Donor> eligible = List.of(donor);

    when(donationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.of(donor));
    when(donorMatchingService.findEligibleDonors(request, List.of(donor), currentDate)).thenReturn(eligible);

    List<Donor> result = useCase.execute(requestId, List.of(donorId), currentDate);

    assertSame(eligible, result);
  }

  @Test
  void shouldApplyRecommendationPolicyAfterMatching() {
    FindEligibleDonorsUseCase useCase = new FindEligibleDonorsUseCase(
        donationRequestRepository,
        donorRepository,
        donorMatchingService,
        recommendationPolicy);

    LocalDate currentDate = LocalDate.of(2026, 3, 16);
    DomainID requestId = DomainID.generate();
    DomainID donorId1 = DomainID.generate();
    DomainID donorId2 = DomainID.generate();

    DonationRequest request = createDonationRequest(currentDate);
    Donor donor1 = createDonor(currentDate, "98765432100");
    Donor donor2 = createDonor(currentDate, "12312312399");

    when(donationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(donorRepository.findByPartyId(donorId1)).thenReturn(Optional.of(donor1));
    when(donorRepository.findByPartyId(donorId2)).thenReturn(Optional.of(donor2));
    when(donorMatchingService.findEligibleDonors(request, List.of(donor1, donor2), currentDate))
        .thenReturn(List.of(donor1, donor2));
    when(recommendationPolicy.isSatisfiedBy(donor1, request)).thenReturn(true);
    when(recommendationPolicy.isSatisfiedBy(donor2, request)).thenReturn(false);

    List<Donor> result = useCase.execute(requestId, List.of(donorId1, donorId2), currentDate);

    assertEquals(1, result.size());
    assertSame(donor1, result.get(0));
  }

  @Test
  void shouldRejectNullConstructorArgs() {
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new FindEligibleDonorsUseCase(null, donorRepository, donorMatchingService));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new FindEligibleDonorsUseCase(donationRequestRepository, null, donorMatchingService));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new FindEligibleDonorsUseCase(donationRequestRepository, donorRepository, null));
  }

  @Test
  void shouldValidateExecutionArgs() {
    FindEligibleDonorsUseCase useCase = new FindEligibleDonorsUseCase(donationRequestRepository, donorRepository, donorMatchingService);
    LocalDate today = LocalDate.now();
    DomainID validId = DomainID.generate();

    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, List.of(), today));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.execute(validId, null, today));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.execute(validId, List.of(), null));
  }

  @Test
  void shouldThrowWhenRequestOrDonorMissingOrNullIdInList() {
    FindEligibleDonorsUseCase useCase = new FindEligibleDonorsUseCase(donationRequestRepository, donorRepository, donorMatchingService);
    LocalDate today = LocalDate.now();
    DomainID reqId = DomainID.generate();
    DomainID donorId = DomainID.generate();

    when(donationRequestRepository.findById(reqId)).thenReturn(Optional.empty());
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.execute(reqId, List.of(donorId), today));

    DonationRequest request = createDonationRequest(today);
    when(donationRequestRepository.findById(reqId)).thenReturn(Optional.of(request));

    // null donor id in list
    List<DomainID> listWithNull = new java.util.ArrayList<>();
    listWithNull.add(null);
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.execute(reqId, listWithNull, today));

    // donor not found
    when(donorRepository.findByPartyId(donorId)).thenReturn(Optional.empty());
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> useCase.execute(reqId, List.of(donorId), today));
  }

  private DonationRequest createDonationRequest(LocalDate currentDate) {
    Person requesterParty = new Person(
        "Requester Person",
        new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1995, 1, 1));
    Requester requester = new Requester(requesterParty);

    Organization bloodCenterParty = new Organization(
        "Main Blood Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100"));
    BloodCenter bloodCenter = new BloodCenter(bloodCenterParty);

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

  private Donor createDonor(LocalDate currentDate, String cpf) {
    return new Donor(
        new Person(
            "Donor Person",
            new PhoneNumber("11988887777"),
            new CPF(cpf),
            currentDate.minusYears(30)),
        BloodType.of("O-"),
        75.0);
  }
}