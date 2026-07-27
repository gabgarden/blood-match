package bloodmatch.application.usecase;

import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetDonationRequestsByPartyIdUseCaseTest {

  private final DonationRequestRepositoryInterface donationRequestRepository = mock(DonationRequestRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final GetDonationRequestsByPartyIdUseCase useCase = new GetDonationRequestsByPartyIdUseCase(
      donationRequestRepository,
      donationRepository,
      new DonationRequestFulfillmentService());

  @Test
  void shouldReturnRequestsOrderedByDateRequestedDesc() {
    LocalDate now = LocalDate.of(2026, 4, 23);
    DomainID userId = DomainID.generate();

    DonationRequest older = createRequest(now.minusDays(3));
    DonationRequest newer = createRequest(now.minusDays(1));

    when(donationRequestRepository.findByRequesterPartyId(userId)).thenReturn(List.of(older, newer));
    when(donationRequestRepository.findActiveRequests()).thenReturn(List.of(older, newer));
    when(donationRepository.findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());

    List<GetDonationRequestsByPartyIdUseCase.OutputItem> result = useCase.execute(userId, now);

    assertEquals(2, result.size());
    assertEquals(newer.getId().getValue().toString(), result.get(0).requestId());
    assertEquals(older.getId().getValue().toString(), result.get(1).requestId());
    assertEquals(1, result.get(0).goalBloodBags());
    assertEquals(0, result.get(0).fulfilledBloodBags());
    assertEquals(1, result.get(0).remainingBloodBags());
    assertEquals(false, result.get(0).goalReached());
    assertEquals(true, result.get(0).active());
    assertEquals(false, result.get(0).expired());
    verify(donationRepository).findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(
        List.of(
            older.getBloodCenter().getOrganization().getId(),
            newer.getBloodCenter().getOrganization().getId()));
  }

  @Test
  void shouldReturnCancelledRequestWithZeroProgress() {
    LocalDate now = LocalDate.of(2026, 4, 23);
    DomainID userId = DomainID.generate();
    DonationRequest cancelled = createRequest(now.minusDays(1));
    cancelled.close();

    when(donationRequestRepository.findByRequesterPartyId(userId)).thenReturn(List.of(cancelled));
    when(donationRequestRepository.findActiveRequests()).thenReturn(List.of());
    when(donationRepository.findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());

    List<GetDonationRequestsByPartyIdUseCase.OutputItem> result = useCase.execute(userId, now);

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
    Donor donor = createDonor(currentDate);
    Donation donation = Donation.registerExternalDonation(
        donor, expired.getDateRequested().plusDays(1), expired.getBloodCenter(), currentDate);

    when(donationRequestRepository.findByRequesterPartyId(userId)).thenReturn(List.of(expired));
    when(donationRequestRepository.findActiveRequests()).thenReturn(List.of(expired));
    when(donationRepository.findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of(donation));

    GetDonationRequestsByPartyIdUseCase.OutputItem result = useCase.execute(userId, currentDate).get(0);

    assertEquals(true, result.active());
    assertEquals(true, result.expired());
    assertEquals(0, result.fulfilledBloodBags());
    assertEquals(false, result.goalReached());
  }

  @Test
  void shouldThrowWhenUserIdIsNull() {
    assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
  }

  private DonationRequest createRequest(LocalDate dateRequested) {
    Requester requester = new Requester(new Person(
        "Requester Person",
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1)));

    BloodCenter bloodCenter = new BloodCenter(new Organization(
        "Blood Center",
        new CNPJ("12345678000100")));

    return DonationRequest.create(
        requester,
        bloodCenter,
        BloodType.of("A+"),
      1,
        dateRequested.plusDays(10),
        dateRequested,
        Urgency.MEDIUM);
  }

  private Donor createDonor(LocalDate currentDate) {
    return new Donor(
        new Person("Donor Person", new CPF("98765432100"), currentDate.minusYears(30)),
        BloodType.of("O-"),
        75.0);
  }
}
