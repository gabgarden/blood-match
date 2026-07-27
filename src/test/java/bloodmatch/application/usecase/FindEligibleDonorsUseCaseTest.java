package bloodmatch.application.usecase;

import bloodmatch.application.usecase.donationrequest.FindEligibleDonorsUseCase;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.matching.DonorMatchingService;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.interfaces.DonorRecommendationPolicyInterface;

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

  private DonationRequest createDonationRequest(LocalDate currentDate) {
    Person requesterParty = new Person(
        "Requester Person",
        new CPF("12345678901"),
        LocalDate.of(1995, 1, 1));
    Requester requester = new Requester(requesterParty);

    Organization bloodCenterParty = new Organization(
        "Main Blood Center",
        new CNPJ("12345678000100"));
    BloodCenter bloodCenter = new BloodCenter(bloodCenterParty);

    return DonationRequest.create(
        requester,
        bloodCenter,
        BloodType.of("A+"),
      1,
        currentDate.plusDays(10),
      currentDate,
      Urgency.MEDIUM);
  }

  private Donor createDonor(LocalDate currentDate, String cpf) {
    return new Donor(
                new Person(
            "Donor Person",
            new CPF(cpf),
            currentDate.minusYears(30)),
        BloodType.of("O-"),
        75.0);
  }
}